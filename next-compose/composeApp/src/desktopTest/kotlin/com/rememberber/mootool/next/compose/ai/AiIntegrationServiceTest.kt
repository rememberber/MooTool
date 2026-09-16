package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.nio.file.Files

class AiIntegrationServiceTest {
    @Test
    fun mcpInstallPreviewAndUninstallRoundTrip() {
        val productRoot = Files.createTempDirectory("mootool-ai-product-")
        val clientHome = Files.createTempDirectory("mootool-ai-client-home-")
        try {
            val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
            val settings = SettingsRepository(directories).also { it.load() }
            val database = AppDatabase(directories)
            val container = AppContainer(
                directories = directories,
                settingsRepository = settings,
                database = database,
                history = HistoryRepository(database),
                migrationRows = LegacyMigrationRowRepository(database),
                sessions = SessionStore(database)
            )
            val service = AiIntegrationService(container, testUserHome = clientHome).apply {
                connectionVerifier = { }
            }
            val configPath = clientHome.resolve(".claude.json")
            val receiptPath = directories.configRoot
                .resolve("ai-integration")
                .resolve("claude-code.json")
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            assertTrue(preview.files.any { it.path == configPath.toString() && it.action == AiFileAction.Create })
            val installed = service.install(preview.id)
            assertTrue(configPath.exists())
            assertTrue(configPath.readText().contains("mootool"))
            assertTrue(receiptPath.exists())
            assertTrue(receiptPath.readText().contains("mcp"))
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.ClaudeCode).mcp)
            assertTrue(installed.paths.contains(configPath.toString()))
            val uninstallPreview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Uninstall)
            )
            val uninstall = service.install(uninstallPreview.id)
            assertEquals(null, AiClientConfigMerge.readServer(configPath.readText(), false))
            assertTrue(!receiptPath.exists())
            assertEquals(AiComponentState.NotInstalled, service.getStatus(AiClient.ClaudeCode).mcp)
            assertTrue(uninstall.backups.isNotEmpty())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun detectsUserMcpConfigEditsAsConflict() {
        val productRoot = Files.createTempDirectory("mootool-ai-conflict-")
        val clientHome = Files.createTempDirectory("mootool-ai-conflict-home-")
        try {
            val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
            val settings = SettingsRepository(directories).also { it.load() }
            val database = AppDatabase(directories)
            val container = AppContainer(
                directories = directories,
                settingsRepository = settings,
                database = database,
                history = HistoryRepository(database),
                migrationRows = LegacyMigrationRowRepository(database),
                sessions = SessionStore(database)
            )
            val service = AiIntegrationService(container, testUserHome = clientHome).apply {
                connectionVerifier = { }
            }
            val configPath = clientHome.resolve(".claude.json")
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            service.install(preview.id)
            val mapper = ObjectMapper()
            val root = mapper.readTree(configPath.readText())
            val args = root.path("mcpServers").path("mootool").path("args") as ArrayNode
            args.add("--user-option")
            val tampered = mapper.writeValueAsString(root)
            configPath.writeText(tampered)

            assertEquals(AiComponentState.Conflict, service.getStatus(AiClient.ClaudeCode).mcp)

            val installAgain = AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            val uninstall = AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Uninstall)
            assertFailsWith<IllegalArgumentException> {
                service.preview(installAgain)
            }
            assertFailsWith<IllegalArgumentException> {
                service.preview(uninstall)
            }
            assertEquals(tampered, configPath.readText())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun previewDoesNotWriteClientConfiguration() {
        val productRoot = Files.createTempDirectory("mootool-ai-preview-readonly-")
        val clientHome = Files.createTempDirectory("mootool-ai-preview-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".claude.json")
            val original = """{"custom":true}"""
            configPath.writeText(original)
            service.preview(AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install))
            assertEquals(original, configPath.readText())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun installRejectsStaleConfigurationSincePreview() {
        val productRoot = Files.createTempDirectory("mootool-ai-stale-")
        val clientHome = Files.createTempDirectory("mootool-ai-stale-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".claude.json")
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            configPath.writeText("""{"updatedByClaude":true}""")
            assertFailsWith<IllegalArgumentException> {
                service.install(preview.id)
            }
            assertEquals("""{"updatedByClaude":true}""", configPath.readText())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun installPropagatesConnectionVerifierFailure() {
        val productRoot = Files.createTempDirectory("mootool-ai-conn-fail-")
        val clientHome = Files.createTempDirectory("mootool-ai-conn-home-")
        try {
            val service = aiService(productRoot, clientHome) {
                throw IllegalStateException("Cannot start runtime")
            }
            val configPath = clientHome.resolve(".cursor").resolve("mcp.json")
            val preview = service.preview(
                AiInstallRequest(AiClient.Cursor, AiInstallMode.Mcp, AiOperation.Install)
            )
            assertFailsWith<IllegalStateException> {
                service.install(preview.id)
            }
            assertFalse(configPath.exists())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsSymlinkConfigurationFile() {
        val productRoot = Files.createTempDirectory("mootool-ai-symlink-")
        val clientHome = Files.createTempDirectory("mootool-ai-symlink-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val target = clientHome.resolve("secret.json")
            target.writeText("{}")
            val link = clientHome.resolve(".claude.json")
            Files.createSymbolicLink(link, target)
            assertFailsWith<IllegalArgumentException> {
                service.preview(AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install))
            }
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun cursorRejectsSkillOnlyPreview() {
        val productRoot = Files.createTempDirectory("mootool-ai-cursor-skill-")
        val clientHome = Files.createTempDirectory("mootool-ai-cursor-home-")
        try {
            val service = aiService(productRoot, clientHome)
            assertFailsWith<IllegalArgumentException> {
                service.preview(AiInstallRequest(AiClient.Cursor, AiInstallMode.Skill, AiOperation.Install))
            }
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun claudeSkillOnlyInstallDoesNotCreateMcpConfig() {
        val productRoot = Files.createTempDirectory("mootool-ai-skill-only-")
        val clientHome = Files.createTempDirectory("mootool-ai-skill-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".claude.json")
            val skillPath = clientHome.resolve(".claude").resolve("skills").resolve("mootool").resolve("SKILL.md")
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Skill, AiOperation.Install)
            )
            service.install(preview.id)
            assertFalse(configPath.exists())
            assertTrue(skillPath.exists())
            assertTrue(skillPath.readText().contains("MooTool"))
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun mcpNeedsRepairWhenRuntimeLaunchChangesAndReinstallFixes() {
        val productRoot = Files.createTempDirectory("mootool-ai-repair-mcp-")
        val clientHome = Files.createTempDirectory("mootool-ai-repair-mcp-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val javaCommand = Path.of(System.getProperty("java.home"), "bin", "java").toString()
            val launchOld = McpLaunch(javaCommand, listOf("--mcp", "access-old"))
            val launchNew = McpLaunch(javaCommand, listOf("--mcp", "access-new"))
            service.testLaunch = launchOld
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            service.install(preview.id)
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.ClaudeCode).mcp)
            service.testLaunch = launchNew
            assertEquals(AiComponentState.NeedsRepair, service.getStatus(AiClient.ClaudeCode).mcp)
            val repair = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            service.install(repair.id)
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.ClaudeCode).mcp)
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun codexSkillNeedsRepairWhenManagedFileMissing() {
        val productRoot = Files.createTempDirectory("mootool-ai-repair-skill-")
        val clientHome = Files.createTempDirectory("mootool-ai-repair-skill-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val skillPath = clientHome.resolve(".agents").resolve("skills").resolve("mootool").resolve("SKILL.md")
            service.install(
                service.preview(AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Install)).id
            )
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.Codex).skill)
            skillPath.deleteIfExists()
            assertEquals(AiComponentState.NeedsRepair, service.getStatus(AiClient.Codex).skill)
            service.install(
                service.preview(AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Install)).id
            )
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.Codex).skill)
            assertTrue(skillPath.exists())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun uninstallPreservesUserFilesInSkillDirectory() {
        val productRoot = Files.createTempDirectory("mootool-ai-uninstall-skill-")
        val clientHome = Files.createTempDirectory("mootool-ai-uninstall-skill-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val skillDir = clientHome.resolve(".agents").resolve("skills").resolve("mootool")
            val skillPath = skillDir.resolve("SKILL.md")
            val userResource = skillDir.resolve("my-resource.txt")
            service.install(
                service.preview(AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Install)).id
            )
            userResource.writeText("user resource")
            val configPath = clientHome.resolve(".codex").resolve("config.toml")
            val withComment = "# keep my comment\nmodel = \"user-model\"\n${configPath.readText()}"
            configPath.writeText(withComment)
            service.install(
                service.preview(
                    AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Uninstall)
                ).id
            )
            assertFalse(skillPath.exists())
            assertTrue(userResource.exists())
            assertEquals("user resource", userResource.readText())
            assertTrue(configPath.readText().startsWith("# keep my comment"))
            assertEquals(null, AiClientConfigMerge.readServer(configPath.readText(), true))
            assertEquals(AiComponentState.NotInstalled, service.getStatus(AiClient.Codex).skill)
            assertEquals(AiComponentState.NotInstalled, service.getStatus(AiClient.Codex).mcp)
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun codexBothPreviewOmitsPrivateTomlAndInstallBacksUp() {
        val productRoot = Files.createTempDirectory("mootool-ai-codex-preview-privacy-")
        val clientHome = Files.createTempDirectory("mootool-ai-codex-privacy-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".codex").resolve("config.toml")
            configPath.parent.createDirectories()
            val original = "# personal settings\nmodel = \"private-model\"\n"
            configPath.writeText(original)
            val preview = service.preview(
                AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Install)
            )
            val serialized = buildString {
                append(preview.configuration)
                preview.files.forEach { file -> append(file.path).append(file.content) }
            }
            assertFalse(serialized.contains("private-model"), serialized)
            assertEquals(original, configPath.readText())
            assertTrue(preview.files.any { it.path == configPath.toString() })
            assertTrue(preview.files.any { it.path.endsWith("SKILL.md") })
            val installed = service.install(preview.id)
            assertEquals(1, installed.backups.size)
            assertEquals(original, Path.of(installed.backups[0]).readText())
            val again = service.preview(
                AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Install)
            )
            assertTrue(again.files.all { it.action == AiFileAction.Unchanged })
            assertTrue(service.install(again.id).backups.isEmpty())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun mcpInstallBacksUpExistingClientConfig() {
        val productRoot = Files.createTempDirectory("mootool-ai-backup-")
        val clientHome = Files.createTempDirectory("mootool-ai-backup-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".claude.json")
            val original = """{"custom":true}"""
            configPath.writeText(original)
            val installed = service.install(
                service.preview(
                    AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
                ).id
            )
            assertEquals(1, installed.backups.size)
            assertEquals(original, Path.of(installed.backups[0]).readText())
            assertTrue(configPath.readText().contains("mootool"))
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun codexMcpInstallPreservesTomlAndIdempotentPreview() {
        val productRoot = Files.createTempDirectory("mootool-ai-codex-mcp-")
        val clientHome = Files.createTempDirectory("mootool-ai-codex-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".codex").resolve("config.toml")
            configPath.parent.createDirectories()
            val original = "# personal settings\nmodel = \"private-model\"\n"
            configPath.writeText(original)
            val preview = service.preview(
                AiInstallRequest(AiClient.Codex, AiInstallMode.Mcp, AiOperation.Install)
            )
            assertTrue(preview.files.any { it.path == configPath.toString() })
            service.install(preview.id)
            val merged = configPath.readText()
            assertTrue(merged.startsWith(original))
            assertTrue(merged.contains("mcp_servers.mootool"))
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.Codex).mcp)
            val again = service.preview(
                AiInstallRequest(AiClient.Codex, AiInstallMode.Mcp, AiOperation.Install)
            )
            assertTrue(
                again.files.filter { it.path == configPath.toString() }.all { it.action == AiFileAction.Unchanged }
            )
            val uninstallPreview = service.preview(
                AiInstallRequest(AiClient.Codex, AiInstallMode.Mcp, AiOperation.Uninstall)
            )
            service.install(uninstallPreview.id)
            val afterUninstall = configPath.readText()
            assertTrue(afterUninstall.startsWith(original))
            assertEquals(null, AiClientConfigMerge.readServer(afterUninstall, true))
            assertEquals(AiComponentState.NotInstalled, service.getStatus(AiClient.Codex).mcp)
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun codexBothInstallsMcpTomlAndSkill() {
        val productRoot = Files.createTempDirectory("mootool-ai-codex-both-")
        val clientHome = Files.createTempDirectory("mootool-ai-codex-both-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".codex").resolve("config.toml")
            val skillPath = clientHome.resolve(".agents").resolve("skills").resolve("mootool").resolve("SKILL.md")
            val preview = service.preview(
                AiInstallRequest(AiClient.Codex, AiInstallMode.Both, AiOperation.Install)
            )
            assertTrue(preview.files.any { it.path == configPath.toString() })
            assertTrue(preview.files.any { it.path == skillPath.toString() })
            service.install(preview.id)
            assertTrue(configPath.readText().contains("mcp_servers.mootool"))
            assertTrue(skillPath.exists())
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.Codex).mcp)
            assertEquals(AiComponentState.Installed, service.getStatus(AiClient.Codex).skill)
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun installRollsBackEarlierWritesWhenLaterFileFails() {
        val productRoot = Files.createTempDirectory("mootool-ai-rollback-")
        val clientHome = Files.createTempDirectory("mootool-ai-rollback-home-")
        try {
            val service = aiService(productRoot, clientHome)
            val configPath = clientHome.resolve(".claude.json")
            val skillPath = clientHome.resolve(".claude").resolve("skills").resolve("mootool").resolve("SKILL.md")
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Both, AiOperation.Install)
            )
            service.installPutVerifier = { path, _ ->
                if (path == skillPath) throw IllegalStateException("Disk write failed")
            }
            assertFailsWith<IllegalStateException> {
                service.install(preview.id)
            }
            assertFalse(configPath.exists())
            assertFalse(skillPath.exists())
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun installRejectsConcurrentInstallation() {
        val productRoot = Files.createTempDirectory("mootool-ai-concurrent-install-")
        val clientHome = Files.createTempDirectory("mootool-ai-concurrent-home-")
        try {
            val hold = java.util.concurrent.CountDownLatch(1)
            val started = java.util.concurrent.CountDownLatch(1)
            val service = aiService(productRoot, clientHome) {
                started.countDown()
                hold.await()
            }
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            val thread = Thread { service.install(preview.id) }
            thread.start()
            started.await()
            assertFailsWith<IllegalArgumentException> {
                service.install(preview.id)
            }
            hold.countDown()
            thread.join(10_000)
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    @Test
    fun installRejectsExpiredPreview() {
        val productRoot = Files.createTempDirectory("mootool-ai-expired-")
        val clientHome = Files.createTempDirectory("mootool-ai-client-expired-")
        try {
            val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
            val settings = SettingsRepository(directories).also { it.load() }
            val database = AppDatabase(directories)
            val container = AppContainer(
                directories = directories,
                settingsRepository = settings,
                database = database,
                history = HistoryRepository(database),
                migrationRows = LegacyMigrationRowRepository(database),
                sessions = SessionStore(database)
            )
            val service = AiIntegrationService(container, testUserHome = clientHome).apply {
                connectionVerifier = { }
            }
            val preview = service.preview(
                AiInstallRequest(AiClient.ClaudeCode, AiInstallMode.Mcp, AiOperation.Install)
            )
            assertFailsWith<IllegalArgumentException> {
                service.install("missing-preview-id")
            }
            service.install(preview.id)
            assertFailsWith<IllegalArgumentException> {
                service.install(preview.id)
            }
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }

    private fun aiService(
        productRoot: java.nio.file.Path,
        clientHome: java.nio.file.Path,
        connectionVerifier: (() -> Unit)? = { },
    ): AiIntegrationService {
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        return AiIntegrationService(container, testUserHome = clientHome).apply {
            this.connectionVerifier = connectionVerifier
        }
    }
}
