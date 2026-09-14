package com.rememberber.mootool.next.compose.domain

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.writeText

data class GitIdentity(
    val name: String = "MooTool Next Compose",
    val email: String = "next-compose@local"
)

data class GitChange(
    val path: String,
    val originalPath: String? = null,
    val status: String,
    val conflict: Boolean
)

data class GitStatus(
    val available: Boolean,
    val repository: Boolean,
    val version: String = "",
    val branch: String = "",
    val remote: String = "",
    val ahead: Int = 0,
    val behind: Int = 0,
    val changes: List<GitChange> = emptyList(),
    val conflicts: Int = 0,
    val merging: Boolean = false,
    val operation: String = "none"
)

data class GitCommitInfo(
    val hash: String,
    val shortHash: String,
    val author: String,
    val date: String,
    val message: String
)

data class GitActionResult(
    val success: Boolean,
    val message: String
)

internal data class GitCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

object GitEngine {
    const val TIMEOUT_MS = 30_000L
    val defaultGitignore: String = ".DS_Store\n.idea/\n.vscode/\n*.tmp\n.migrated-from-db\n"
    private val locks = ConcurrentHashMap<String, Any>()

    fun identityFrom(username: String): GitIdentity = GitIdentity(
        name = username.trim().take(128).ifBlank { "MooTool Next Compose" },
        email = "next-compose@local"
    )

    fun detect(isolateConfig: Boolean = false): GitStatus {
        val version = run(listOf("--version"), cwd = null, isolateConfig = isolateConfig)
        if (version.exitCode != 0) return GitStatus(available = false, repository = false)
        return GitStatus(available = true, repository = false, version = version.stdout.trim())
    }

    fun status(root: Path, isolateConfig: Boolean = false): GitStatus = locked(root) {
        val detected = detect(isolateConfig)
        if (!detected.available) return@locked detected
        root.createDirectories()
        val top = run(listOf("rev-parse", "--show-toplevel"), root, isolateConfig)
        if (top.exitCode != 0) return@locked detected.copy(repository = false, version = detected.version)
        val vaultRoot = root.toAbsolutePath().normalize().let { if (it.exists()) it.toRealPath() else it }
        val topLevel = Path.of(top.stdout.trim()).let { if (it.exists()) it.toRealPath() else it.normalize() }
        if (vaultRoot != topLevel) return@locked detected.copy(repository = false, version = detected.version)
        val porcelain = run(
            listOf("status", "--porcelain=v1", "-z", "--branch", "--untracked-files=all"),
            root,
            isolateConfig
        )
        val parsed = parsePorcelain(porcelain.stdout)
        val remote = run(listOf("remote", "get-url", "origin"), root, isolateConfig)
        val merge = run(listOf("rev-parse", "-q", "--verify", "MERGE_HEAD"), root, isolateConfig)
        val rebaseMerge = gitPathExists(root, "rebase-merge", isolateConfig)
        val rebaseApply = gitPathExists(root, "rebase-apply", isolateConfig)
        val operation = when {
            rebaseMerge || rebaseApply -> "rebase"
            merge.exitCode == 0 -> "merge"
            else -> "none"
        }
        GitStatus(
            available = true,
            repository = true,
            version = detected.version,
            branch = parseBranch(parsed.branchLine),
            remote = if (remote.exitCode == 0) remote.stdout.trim() else "",
            ahead = Regex("ahead (\\d+)").find(parsed.branchLine)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            behind = Regex("behind (\\d+)").find(parsed.branchLine)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            changes = parsed.changes,
            conflicts = parsed.changes.count { it.conflict },
            merging = operation != "none",
            operation = operation
        )
    }

    fun init(root: Path, identity: GitIdentity = GitIdentity(), isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val detected = detect(isolateConfig)
        if (!detected.available) return@locked GitActionResult(false, "Git is not installed")
        if (status(root, isolateConfig).repository) return@locked GitActionResult(true, "Git repository is already initialized")
        root.createDirectories()
        val initialized = run(listOf("init"), root, isolateConfig)
        if (initialized.exitCode != 0) return@locked failure(initialized)
        val ignore = root.resolve(".gitignore")
        if (!ignore.exists()) ignore.writeText(defaultGitignore)
        val current = status(root, isolateConfig)
        if (current.changes.isEmpty()) return@locked GitActionResult(true, initialized.stdout.trim().ifBlank { "Done" })
        commitLocked(root, "Initial MooTool Vault setup", identity, isolateConfig)
    }

    fun commit(root: Path, message: String, identity: GitIdentity = GitIdentity(), isolateConfig: Boolean = false): GitActionResult =
        locked(root) { commitLocked(root, message, identity, isolateConfig) }

    fun history(root: Path, limit: Int = 50, isolateConfig: Boolean = false): List<GitCommitInfo> = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository) return@locked emptyList()
        val count = limit.coerceIn(1, 100)
        val result = run(
            listOf("log", "--max-count=$count", "--pretty=format:%H%x1f%h%x1f%an%x1f%aI%x1f%s"),
            root,
            isolateConfig
        )
        if (result.exitCode != 0) return@locked emptyList()
        result.stdout.lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split('\u001f')
            if (parts.size < 5) null
            else GitCommitInfo(parts[0], parts[1], parts[2], parts[3], parts.drop(4).joinToString("\u001f"))
        }.toList()
    }

    fun diff(root: Path, path: String? = null, isolateConfig: Boolean = false): String = locked(root) {
        val normalized = path?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return@locked run(listOf("diff", "--no-color"), root, isolateConfig).stdout.take(64 * 1024)
        }
        val tracked = run(listOf("diff", "--no-color", "--", normalized), root, isolateConfig)
        if (tracked.stdout.isNotBlank()) return@locked tracked.stdout.take(64 * 1024)
        val emptyBlob = if (windows) "NUL" else "/dev/null"
        val untracked = run(listOf("diff", "--no-color", "--no-index", "--", emptyBlob, normalized), root, isolateConfig)
        untracked.stdout.take(64 * 1024)
    }

    fun setRemote(root: Path, url: String, isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository) return@locked GitActionResult(false, "Git repository is not initialized in the Vault root")
        val remote = url.trim()
        val exists = run(listOf("remote", "get-url", "origin"), root, isolateConfig).exitCode == 0
        val result = when {
            remote.isEmpty() && exists -> run(listOf("remote", "remove", "origin"), root, isolateConfig)
            remote.isEmpty() -> return@locked GitActionResult(true, "Remote is already removed")
            exists -> run(listOf("remote", "set-url", "origin", remote), root, isolateConfig)
            else -> run(listOf("remote", "add", "origin", remote), root, isolateConfig)
        }
        if (result.exitCode == 0) GitActionResult(true, result.stdout.trim().ifBlank { "Done" }) else failure(result)
    }

    fun parsePorcelain(output: String): PorcelainParse {
        val records = output.split('\u0000')
        val branchLine = records.firstOrNull().orEmpty()
        val changes = mutableListOf<GitChange>()
        var index = 1
        while (index < records.size) {
            val record = records[index]
            index += 1
            if (record.length < 4) continue
            val code = record.substring(0, 2)
            val path = record.substring(3)
            val renamed = code.contains('R') || code.contains('C')
            val original = if (renamed && index < records.size) records[index++] else null
            val conflict = code.contains('U') || code == "AA" || code == "DD"
            changes += GitChange(path, original?.ifBlank { null }, code, conflict)
        }
        return PorcelainParse(branchLine, changes)
    }

    fun parseBranch(line: String): String {
        val value = line.replace(Regex("^##\\s*"), "")
        val empty = Regex("^(?:No commits yet on|Initial commit on)\\s+(.+)$").find(value)
        if (empty != null) return empty.groupValues[1].trim()
        if (value.startsWith("HEAD ")) return "HEAD"
        return value.split("...").first().trim().split(' ').firstOrNull().orEmpty().ifBlank { "HEAD" }
    }

    data class PorcelainParse(val branchLine: String, val changes: List<GitChange>)

    private fun commitLocked(root: Path, message: String, identity: GitIdentity, isolateConfig: Boolean): GitActionResult {
        val normalized = message.trim().take(300)
        if (normalized.isEmpty()) return GitActionResult(false, "Commit message is required")
        val current = status(root, isolateConfig)
        if (!current.available) return GitActionResult(false, "Git is not installed")
        if (!current.repository) return GitActionResult(false, "Git repository is not initialized")
        if (current.merging || current.conflicts > 0) {
            return GitActionResult(false, "Finish or abort the current merge/rebase before creating a checkpoint")
        }
        if (current.changes.isEmpty()) return GitActionResult(true, "No changes to commit")
        val add = run(listOf("add", "--all"), root, isolateConfig)
        if (add.exitCode != 0) return failure(add)
        val commit = run(
            identityArgs(identity) + listOf("commit", "-m", normalized),
            root,
            isolateConfig
        )
        return if (commit.exitCode == 0) GitActionResult(true, commit.stdout.trim().ifBlank { "Done" }) else failure(commit)
    }

    private fun identityArgs(identity: GitIdentity): List<String> = listOf(
        "-c", "user.name=${identity.name.ifBlank { "MooTool Next Compose" }}",
        "-c", "user.email=${identity.email.ifBlank { "next-compose@local" }}",
        "-c", "commit.gpgsign=false"
    )

    private fun gitPathExists(root: Path, name: String, isolateConfig: Boolean): Boolean {
        val result = run(listOf("rev-parse", "--git-path", name), root, isolateConfig)
        if (result.exitCode != 0 || result.stdout.trim().isEmpty()) return false
        val path = root.resolve(result.stdout.trim())
        return Files.exists(path)
    }

    private fun failure(result: GitCommandResult): GitActionResult =
        GitActionResult(false, result.stderr.trim().ifBlank { result.stdout.trim().ifBlank { "Git command failed" } })

    private val windows: Boolean
        get() = System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)

    private fun <T> locked(root: Path, block: () -> T): T {
        val lock = locks.getOrPut(root.toAbsolutePath().normalize().pathString) { Any() }
        synchronized(lock) { return block() }
    }

    internal fun run(args: List<String>, cwd: Path?, isolateConfig: Boolean): GitCommandResult {
        val builder = ProcessBuilder(listOf("git") + args)
        cwd?.let { builder.directory(it.toFile()) }
        val env = builder.environment()
        env["GIT_TERMINAL_PROMPT"] = "0"
        env["GIT_OPTIONAL_LOCKS"] = "0"
        if (isolateConfig) {
            val empty = if (System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)) "NUL" else "/dev/null"
            env["GIT_CONFIG_GLOBAL"] = empty
            env["GIT_CONFIG_SYSTEM"] = empty
        }
        return try {
            val process = builder.start()
            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            val outThread = Thread { process.inputStream.copyTo(stdout) }
            val errThread = Thread { process.errorStream.copyTo(stderr) }
            outThread.start()
            errThread.start()
            if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                outThread.join(1_000)
                errThread.join(1_000)
                return GitCommandResult(124, stdout.toString(Charsets.UTF_8), "Git timed out")
            }
            outThread.join()
            errThread.join()
            GitCommandResult(process.exitValue(), stdout.toString(Charsets.UTF_8), stderr.toString(Charsets.UTF_8))
        } catch (error: Exception) {
            GitCommandResult(127, "", error.message ?: "Git is not installed")
        }
    }
}
