package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ProductIdentity
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.McpJsonDefaults
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class AiIntegrationService(
    private val container: AppContainer,
    internal val testUserHome: Path? = null,
    internal var connectionVerifier: (() -> Unit)? = null,
    /** 安装写盘前回调；仅 desktopTest 用于模拟中途失败。 */
    internal var installPutVerifier: ((Path, String?) -> Unit)? = null,
    /** 覆盖当前进程 launch（desktopTest 模拟 runtime 迁移）。 */
    internal var testLaunch: McpLaunch? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val prettyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val plans = mutableMapOf<String, InstallPlan>()
    private var installing = false

    private val stateDirectory: Path = McpLaunchResolver.stateDirectory(container.directories)
    private val accessFile: Path = McpLaunchResolver.accessFile(container.directories)
    private val launch: McpLaunch
        get() = testLaunch ?: McpLaunchResolver.clientLaunch(accessFile)

    fun getStatus(client: AiClient): AiIntegrationStatus {
        return runCatching {
            val receiptBundle = readReceiptBundle(client)
            val receipt = receiptBundle.receipt
            val desired = clientDesired(client)
            val configPath = configPath(client)
            val source = readOptional(configPath)
            val existing = AiClientConfigMerge.readServer(source, client == AiClient.Codex)
            var mcp = when {
                existing == null -> if (receipt.mcp != null) AiComponentState.NeedsRepair else AiComponentState.NotInstalled
                AiClientConfigMerge.serverEquals(existing, desired, client == AiClient.Codex) -> AiComponentState.Installed
                receipt.mcp != null && AiClientConfigMerge.serverEquals(existing, receipt.mcp, client == AiClient.Codex) ->
                    AiComponentState.NeedsRepair
                else -> AiComponentState.Conflict
            }
            if (source != null && mcp != AiComponentState.Conflict) {
                try {
                    AiClientConfigMerge.manageServer(
                        source,
                        client == AiClient.Codex,
                        desired,
                        receipt.mcp
                    )
                } catch (_: Exception) {
                    mcp = AiComponentState.Conflict
                }
            }
            val skill = if (client == AiClient.Cursor) AiComponentState.NotInstalled else skillState(client, receipt)
            AiIntegrationStatus(mcp = mcp, skill = skill, version = ProductIdentity.VERSION)
        }.getOrElse { AiIntegrationStatus(AiComponentState.Conflict, AiComponentState.Conflict, ProductIdentity.VERSION) }
    }

    fun preview(request: AiInstallRequest): AiInstallPreview {
        if (request.client == AiClient.Cursor && request.mode != AiInstallMode.Mcp) {
            throw IllegalArgumentException("Cursor currently supports MCP installation only.")
        }
        val uninstall = request.operation == AiOperation.Uninstall
        if (!uninstall) assertRuntime()
        val files = mutableListOf<FileChange>()
        val receiptBundle = readReceiptBundle(request.client)
        val receipt = receiptBundle.receipt
        var nextMcp = receipt.mcp
        var nextSkills = receipt.skills
        val desired = if (uninstall) null else clientDesired(request.client)
        if (request.mode != AiInstallMode.Skill) {
            val path = configPath(request.client)
            val before = readOptional(path)
            val after = if (before == null && uninstall) {
                null
            } else {
                AiClientConfigMerge.manageServer(
                    before ?: if (request.client == AiClient.Codex) "" else "{}",
                    request.client == AiClient.Codex,
                    desired,
                    receipt.mcp
                )
            }
            files += FileChange(path, before, after)
            nextMcp = if (uninstall) null else toReceiptMcp(desired!!)
        }
        if (request.mode != AiInstallMode.Mcp && request.client != AiClient.Cursor) {
            for ((name, expected) in skillFiles()) {
                val path = skillRoot(request.client).resolve(name)
                val before = readOptional(path)
                val owned = receipt.skills?.get(name)
                if (before != null && (if (uninstall) sha256(before) != owned else before != expected && sha256(before) != owned)) {
                    throw IllegalArgumentException(
                        "The MooTool skill was modified outside the installer: $path. Preserve it before continuing."
                    )
                }
                files += FileChange(path, before, if (uninstall) null else expected)
            }
            nextSkills = if (uninstall) {
                null
            } else {
                skillFiles().mapValues { (_, content) -> sha256(content) }
            }
        }
        val nextReceipt = AiInstallReceipt(
            schemaVersion = 1,
            appVersion = ProductIdentity.VERSION,
            mcp = nextMcp,
            skills = nextSkills
        )
        val receiptAfter = if (nextReceipt.mcp == null && nextReceipt.skills == null) {
            null
        } else {
            prettyJson.encodeToString(nextReceipt)
        }
        files += FileChange(
            receiptBundle.path,
            receiptBundle.source,
            receiptAfter,
            internal = true
        )
        val configuration = when (request.client) {
            AiClient.Codex -> tomlConfiguration(launch)
            else -> jsonConfiguration(McpLaunchResolver.claudeDesired(launch))
        }
        val id = UUID.randomUUID().toString()
        plans.entries.removeIf { it.value.expires < System.currentTimeMillis() }
        if (plans.size >= 20) plans.remove(plans.keys.first())
        plans[id] = InstallPlan(files, System.currentTimeMillis() + 600_000, uninstall)
        return AiInstallPreview(
            id = id,
            configuration = configuration,
            files = files.filter { !it.internal }.map { change ->
                AiInstallPreviewFile(
                    path = change.path.toString(),
                    action = when {
                        change.before == change.after -> AiFileAction.Unchanged
                        change.after == null -> AiFileAction.Delete
                        change.before == null -> AiFileAction.Create
                        else -> AiFileAction.Update
                    },
                    content = when {
                        change.after == null -> ""
                        change.path.toString().endsWith(".md") -> change.after
                        else -> configuration
                    }
                )
            }
        )
    }

    fun install(id: String): AiInstallResult {
        val plan = plans[id] ?: throw IllegalArgumentException("Installation preview expired. Refresh the preview.")
        if (plan.expires < System.currentTimeMillis()) throw IllegalArgumentException("Installation preview expired. Refresh the preview.")
        if (installing) throw IllegalArgumentException("An installation is already running.")
        installing = true
        val backups = mutableListOf<String>()
        val written = mutableListOf<FileChange>()
        try {
            plan.files.forEach { assertUnchanged(it) }
            if (!plan.uninstall) {
                connectionVerifier?.invoke() ?: testConnection()
            }
            plan.files.forEach { change ->
                if (change.before == change.after) return@forEach
                assertUnchanged(change)
                if (change.before != null && !change.internal) {
                    val backup = change.path.parent.resolve("${change.path.fileName}.mootool-backup-${UUID.randomUUID()}")
                    atomicWrite(backup, change.before)
                    backups += backup.toString()
                }
                installPutVerifier?.invoke(change.path, change.after)
                put(change.path, change.after)
                written += change
            }
            plan.files.forEach { change ->
                if (readOptional(change.path) != change.after) {
                    throw IllegalStateException("Installation verification failed: ${change.path}")
                }
            }
            plans.remove(id)
            return AiInstallResult(
                paths = plan.files.filter { !it.internal }.map { it.path.toString() },
                backups = backups
            )
        } catch (error: Exception) {
            val rollbackErrors = mutableListOf<String>()
            for (change in written.asReversed()) {
                try {
                    if (readOptional(change.path) != change.after) throw IllegalStateException("File changed after installation")
                    put(change.path, change.before)
                } catch (_: Exception) {
                    rollbackErrors += change.path.toString()
                }
            }
            if (rollbackErrors.isNotEmpty()) {
                throw IllegalStateException(
                    "Installation failed; restore these files from backups: ${rollbackErrors.joinToString(", ")}. Backups: ${backups.joinToString(", ")}"
                )
            }
            throw error
        } finally {
            installing = false
        }
    }

    fun getDataAccess(): AiDataAccess {
        val file = readOptional(accessFile) ?: return AiDataAccess()
        val parsed = json.decodeFromString<AccessFile>(file)
        return AiDataAccess(notes = parsed.notes, json = parsed.json)
    }

    fun setDataAccess(request: AiDataAccessRequest): AiDataAccess {
        val notesRoot = container.noteVault().root()
        val jsonRoot = container.jsonVault.root()
        if (request.notes && !notesRoot.isDirectory()) {
            throw IllegalArgumentException("The MooTool vault directory is unavailable")
        }
        if (request.json && !jsonRoot.isDirectory()) {
            throw IllegalArgumentException("The MooTool vault directory is unavailable")
        }
        val access = AiDataAccess(
            notes = if (request.notes) canonicalVaultPath(notesRoot) else null,
            json = if (request.json) canonicalVaultPath(jsonRoot) else null,
        )
        atomicWrite(accessFile, prettyJson.encodeToString(AccessFile(version = 1, notes = access.notes, json = access.json)))
        return access
    }

    /** 对齐 Electron `setDataAccess`：`access.json` 持久化 `realpath` 后的 Vault 根。 */
    private fun canonicalVaultPath(root: Path): String = root.toRealPath().toString()

    fun testConnection(): AiConnectionResult {
        assertRuntime()
        val current = launch
        val transport = StdioClientTransport(
            ServerParameters.builder(current.command).args(current.args).env(current.env).build(),
            McpJsonDefaults.getMapper()
        )
        val client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build()
        client.use {
            it.initialize()
            val tools = it.listTools().tools.map { tool -> tool.name }
            val required = MooToolMcpTools.toolNames() + VaultMcpTools.toolNames
            val missing = required.filter { name -> !tools.contains(name) }
            if (missing.isNotEmpty()) {
                throw IllegalStateException("MooTool MCP is missing tools: ${missing.joinToString(", ")}")
            }
            val sample = it.callTool(
                McpSchema.CallToolRequest.builder()
                    .name("mootool_json_format")
                    .arguments(mapOf("text" to """{"moo":true}""", "spaces" to 0))
                    .build()
            )
            val text = sample.content.firstOrNull()?.let { content ->
                if (content is McpSchema.TextContent) content.text else null
            }
            if (sample.isError || text != """{"moo":true}""") throw IllegalStateException("MooTool tool verification failed")
            return AiConnectionResult(serverName = "MooTool", tools = tools)
        }
    }

    private fun clientDesired(client: AiClient): Any = when (client) {
        AiClient.Codex -> launch
        else -> McpLaunchResolver.claudeDesired(launch)
    }

    private fun userHome(): Path = testUserHome ?: Path.of(System.getProperty("user.home"))

    private fun configPath(client: AiClient): Path {
        val home = userHome()
        return when (client) {
            AiClient.Codex -> home.resolve(".codex").resolve("config.toml")
            AiClient.Cursor -> home.resolve(".cursor").resolve("mcp.json")
            AiClient.ClaudeCode -> home.resolve(".claude.json")
        }
    }

    private fun skillRoot(client: AiClient): Path {
        val home = userHome()
        return when (client) {
            AiClient.Codex -> home.resolve(".agents").resolve("skills").resolve("mootool")
            else -> home.resolve(".claude").resolve("skills").resolve("mootool")
        }
    }

    private fun skillState(client: AiClient, receipt: AiInstallReceipt): AiComponentState {
        if (client == AiClient.Cursor) return AiComponentState.NotInstalled
        val states = skillFiles().keys.map { name ->
            val path = skillRoot(client).resolve(name)
            val actual = readOptional(path)
            val expected = skillFiles()[name]!!
            when {
                actual == null ->
                    if (receipt.skills?.containsKey(name) == true) AiComponentState.NeedsRepair else AiComponentState.NotInstalled
                actual == expected -> AiComponentState.Installed
                receipt.skills?.get(name) == sha256(actual) -> AiComponentState.NeedsRepair
                else -> AiComponentState.Conflict
            }
        }
        return when {
            states.contains(AiComponentState.Conflict) -> AiComponentState.Conflict
            states.contains(AiComponentState.NeedsRepair) ||
                (states.contains(AiComponentState.Installed) && states.contains(AiComponentState.NotInstalled)) ->
                AiComponentState.NeedsRepair
            states.all { it == AiComponentState.Installed } -> AiComponentState.Installed
            else -> AiComponentState.NotInstalled
        }
    }

    private fun skillFiles(): Map<String, String> = mapOf(
        "SKILL.md" to skillMarkdown(),
        "runtime.md" to runtimeMarkdown()
    )

    private fun skillMarkdown(): String =
        this::class.java.classLoader.getResource("mcp/SKILL.md")?.readText() ?: ""

    private fun runtimeMarkdown(): String {
        val current = launch
        val posix = current.args.joinToString(" ") { "'${it.replace("'", "'\\''")}'" }
        return "# Installed MooTool runtime\n\nRun:\n\n```sh\n${current.command} $posix --list\n```\n"
    }

    private data class ReceiptBundle(val path: Path, val source: String?, val receipt: AiInstallReceipt)

    private fun readReceiptBundle(client: AiClient): ReceiptBundle {
        val path = stateDirectory.resolve("${client.receiptFileName()}.json")
        val source = readOptional(path)
        val receipt = try {
            if (source == null) {
                AiInstallReceipt(appVersion = ProductIdentity.VERSION)
            } else {
                json.decodeFromString<AiInstallReceipt>(source)
            }
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "The MooTool installation record is damaged. Restore it from its backup before continuing."
            )
        }
        return ReceiptBundle(path, source, receipt)
    }

    private fun readOptional(path: Path): String? {
        if (!path.exists()) return null
        if (Files.isSymbolicLink(path)) {
            throw IllegalArgumentException("Expected a regular configuration file: $path")
        }
        if (!path.isRegularFile()) return null
        if (Files.size(path) > 4_000_000) throw IllegalArgumentException("Configuration is too large: $path")
        return path.readText()
    }

    private fun atomicWrite(path: Path, content: String) {
        path.parent?.createDirectories()
        val temp = path.parent.resolve("${path.fileName}.mootool-tmp-${UUID.randomUUID()}")
        try {
            temp.writeText(content)
            Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS)
        } finally {
            temp.deleteIfExists()
        }
    }

    private fun put(path: Path, content: String?) {
        if (content == null) path.deleteIfExists() else atomicWrite(path, content)
    }

    private fun assertUnchanged(change: FileChange) {
        if (readOptional(change.path) != change.before) {
            throw IllegalArgumentException("Configuration changed since preview. Refresh the preview: ${change.path}")
        }
    }

    private fun assertRuntime() {
        if (!Path.of(launch.command).exists()) throw IllegalStateException("MooTool runtime is unavailable")
    }

    private fun tomlConfiguration(launch: McpLaunch): String =
        "[mcp_servers.mootool]\ncommand = \"${launch.command}\"\n"

    private fun jsonConfiguration(launch: ClaudeMcpLaunch): String =
        AiClientConfigMerge.mergeJson("{}", launch, true)

    private fun toReceiptMcp(launch: Any): ReceiptMcp = when (launch) {
        is McpLaunch -> ReceiptMcp(command = launch.command, args = launch.args, env = launch.env)
        is ClaudeMcpLaunch -> ReceiptMcp(
            command = launch.command,
            args = launch.args,
            env = launch.env,
            type = launch.type
        )
        else -> throw IllegalArgumentException("Unsupported launch type")
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private data class FileChange(val path: Path, val before: String?, val after: String?, val internal: Boolean = false)
    private data class InstallPlan(val files: List<FileChange>, val expires: Long, val uninstall: Boolean)

    @Serializable
    private data class AccessFile(val version: Int = 1, val notes: String? = null, val json: String? = null)
}

private fun AiClient.receiptFileName(): String = when (this) {
    AiClient.Codex -> "codex"
    AiClient.ClaudeCode -> "claude-code"
    AiClient.Cursor -> "cursor"
}
