package com.rememberber.mootool.next.compose.domain

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
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

enum class GitDiffPreview { Text, Binary, TooLarge }

data class GitFileDiff(
    val path: String,
    val originalPath: String? = null,
    val status: String = "",
    val before: String = "",
    val after: String = "",
    val preview: GitDiffPreview = GitDiffPreview.Text
)

private data class GitContentPreview(
    val text: String = "",
    val state: String = "missing"
)

internal data class GitCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

object GitEngine {
    const val TIMEOUT_MS = 30_000L
    const val REMOTE_TIMEOUT_MS = 120_000L
    const val MAX_DIFF_PREVIEW_BYTES = 512 * 1024
    private const val STALE_INDEX_LOCK_MS = 5 * 60 * 1_000L
    private const val INDEX_LOCK_RETRY_MS = 250L
    private const val INDEX_LOCK_STABILITY_MS = 100L
    private val INDEX_LOCK_FAILURE = Regex("""unable to create [^\r\n]*index\.lock['"]?: file exists""", RegexOption.IGNORE_CASE)
    private val COMMIT_HASH = Regex("^[0-9a-f]{7,40}$", RegexOption.IGNORE_CASE)
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

    fun fileDiffs(root: Path, path: String? = null, commit: String? = null, isolateConfig: Boolean = false): List<GitFileDiff> = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository) return@locked emptyList()
        val normalizedCommit = commit?.trim().orEmpty()
        if (normalizedCommit.isNotEmpty()) {
            if (!COMMIT_HASH.matches(normalizedCommit)) return@locked emptyList()
            return@locked commitDiffFiles(root, normalizedCommit, path, isolateConfig)
        }
        val pathArg = path?.trim().orEmpty()
        val wanted = normalizeGitPath(path)
        if (pathArg.isNotEmpty() && wanted == null) {
            throw IllegalArgumentException("Invalid Git path")
        }
        val changes = if (wanted == null) current.changes else current.changes.filter { it.path == wanted }
        if (wanted != null && changes.isEmpty()) {
            return@locked emptyList()
        }
        changes.map { change ->
            val before = readBlobPreview(root, "HEAD", change.originalPath ?: change.path, isolateConfig)
            val after = readWorkingPreview(root, change.path)
            toFileDiff(change, before, after)
        }
    }

    fun normalizeGitRemote(value: String): String {
        val remote = value.trim()
        if (remote.isEmpty()) return ""
        if (remote.length > 2048 || remote.any { it == '\r' || it == '\n' || it == '\u0000' }) {
            throw IllegalArgumentException("Invalid Git remote")
        }
        if (!GIT_REMOTE_PREFIX.containsMatchIn(remote)) {
            throw IllegalArgumentException("Invalid Git remote")
        }
        return remote
    }

    fun setRemote(root: Path, url: String, isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository) return@locked GitActionResult(false, "Git repository is not initialized in the Vault root")
        val remote = try {
            normalizeGitRemote(url)
        } catch (error: IllegalArgumentException) {
            return@locked GitActionResult(false, error.message ?: "Invalid Git remote")
        }
        val exists = run(listOf("remote", "get-url", "origin"), root, isolateConfig).exitCode == 0
        val result = when {
            remote.isEmpty() && exists -> run(listOf("remote", "remove", "origin"), root, isolateConfig)
            remote.isEmpty() -> return@locked GitActionResult(true, "Remote is already removed")
            exists -> run(listOf("remote", "set-url", "origin", remote), root, isolateConfig)
            else -> run(listOf("remote", "add", "origin", remote), root, isolateConfig)
        }
        if (result.exitCode == 0) GitActionResult(true, result.stdout.trim().ifBlank { "Done" }) else failure(result)
    }

    fun fetch(root: Path, isolateConfig: Boolean = false, token: String = ""): GitActionResult = locked(root) {
        requireRemote(root, isolateConfig) { run(listOf("fetch", "--prune", "origin"), root, isolateConfig, token, REMOTE_TIMEOUT_MS) }
    }

    fun pull(root: Path, isolateConfig: Boolean = false, token: String = ""): GitActionResult = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository) return@locked GitActionResult(false, "Git repository is not initialized")
        if (current.merging) return@locked GitActionResult(false, "Finish or abort the current merge/rebase before pulling")
        if (current.remote.isBlank()) return@locked GitActionResult(false, "No origin remote is configured")
        val result = run(listOf("pull", "--no-rebase", "origin"), root, isolateConfig, token, REMOTE_TIMEOUT_MS)
        if (result.exitCode == 0) GitActionResult(true, result.stdout.trim().ifBlank { result.stderr.trim().ifBlank { "Done" } })
        else failure(result)
    }

    fun push(root: Path, isolateConfig: Boolean = false, token: String = ""): GitActionResult = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository) return@locked GitActionResult(false, "Git repository is not initialized")
        if (current.merging) {
            return@locked GitActionResult(false, "Finish or abort the current merge/rebase before pushing")
        }
        if (current.conflicts > 0) {
            return@locked GitActionResult(false, "Resolve all conflicts before pushing")
        }
        requireRemote(root, isolateConfig) { run(listOf("push", "-u", "origin", "HEAD"), root, isolateConfig, token, REMOTE_TIMEOUT_MS) }
    }

    fun discard(root: Path, path: String, isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val normalized = path.trim()
        if (normalized.isEmpty()) return@locked GitActionResult(false, "Git path is required")
        val current = status(root, isolateConfig)
        val change = current.changes.find { it.path == normalized } ?: return@locked GitActionResult(true, "No changes to discard")
        if (change.status == "??") {
            val cleaned = run(listOf("clean", "-f", "--", normalized), root, isolateConfig)
            return@locked if (cleaned.exitCode == 0) GitActionResult(true, cleaned.stdout.trim().ifBlank { "Done" }) else failure(cleaned)
        }
        val paths = listOfNotNull(change.originalPath, normalized)
        var restored = run(listOf("restore", "--staged", "--worktree", "--source=HEAD", "--") + paths, root, isolateConfig)
        if (restored.exitCode != 0) {
            run(listOf("reset", "HEAD", "--") + paths, root, isolateConfig)
            restored = run(listOf("checkout", "HEAD", "--") + paths, root, isolateConfig)
        }
        if (restored.exitCode != 0 && change.status.contains('A')) {
            val cleaned = run(listOf("clean", "-f", "--", normalized), root, isolateConfig)
            return@locked if (cleaned.exitCode == 0) GitActionResult(true, cleaned.stdout.trim().ifBlank { "Done" }) else failure(cleaned)
        }
        if (restored.exitCode == 0) GitActionResult(true, restored.stdout.trim().ifBlank { "Done" }) else failure(restored)
    }

    private fun commitDiffFiles(root: Path, commit: String, path: String?, isolateConfig: Boolean): List<GitFileDiff> {
        val parentResult = run(listOf("rev-parse", "--verify", "$commit^"), root, isolateConfig)
        val parent = if (parentResult.exitCode == 0) parentResult.stdout.trim().ifBlank { null } else null
        val wanted = normalizeGitPath(path)
        val pathArgs = if (wanted == null) emptyList() else listOf("--", wanted)
        val listed = if (parent != null) {
            run(listOf("diff", "--name-status", "-z", "-M", "-C", parent, commit) + pathArgs, root, isolateConfig)
        } else {
            run(listOf("diff-tree", "--root", "--no-commit-id", "--name-status", "-z", "-r", "-M", "-C", commit) + pathArgs, root, isolateConfig)
        }
        if (listed.exitCode != 0) return emptyList()
        return parseNameStatus(listed.stdout).map { change ->
            val before = if (parent != null) {
                readBlobPreview(root, parent, change.originalPath ?: change.path, isolateConfig)
            } else {
                GitContentPreview()
            }
            val after = readBlobPreview(root, commit, change.path, isolateConfig)
            toFileDiff(change, before, after)
        }
    }

    private fun readBlobPreview(root: Path, ref: String, path: String, isolateConfig: Boolean): GitContentPreview {
        val spec = "$ref:$path"
        val sizeResult = run(listOf("cat-file", "-s", spec), root, isolateConfig)
        if (sizeResult.exitCode != 0) return GitContentPreview()
        val size = sizeResult.stdout.trim().toLongOrNull() ?: return GitContentPreview()
        if (size > MAX_DIFF_PREVIEW_BYTES) return GitContentPreview(state = "too-large")
        val content = run(listOf("cat-file", "blob", spec), root, isolateConfig)
        if (content.exitCode != 0) return GitContentPreview()
        if (content.stdout.indexOf('\u0000') >= 0) return GitContentPreview(state = "binary")
        return GitContentPreview(content.stdout, "text")
    }

    private fun readWorkingPreview(root: Path, path: String): GitContentPreview {
        val vaultRoot = root.toAbsolutePath().normalize().let { if (it.exists()) it.toRealPath() else it }
        val file = vaultRoot.resolve(path).normalize()
        if (!file.startsWith(vaultRoot) || !Files.isRegularFile(file)) return GitContentPreview()
        val size = Files.size(file)
        if (size > MAX_DIFF_PREVIEW_BYTES) return GitContentPreview(state = "too-large")
        val bytes = Files.readAllBytes(file)
        if (bytes.any { it == 0.toByte() }) return GitContentPreview(state = "binary")
        return GitContentPreview(bytes.toString(Charsets.UTF_8), "text")
    }

    private fun toFileDiff(change: GitChange, before: GitContentPreview, after: GitContentPreview): GitFileDiff {
        val preview = when {
            before.state == "too-large" || after.state == "too-large" -> GitDiffPreview.TooLarge
            before.state == "binary" || after.state == "binary" -> GitDiffPreview.Binary
            else -> GitDiffPreview.Text
        }
        return GitFileDiff(
            path = change.path,
            originalPath = change.originalPath,
            status = change.status,
            before = before.text,
            after = after.text,
            preview = preview
        )
    }

    internal fun parseNameStatus(output: String): List<GitChange> {
        val records = output.split('\u0000')
        val changes = mutableListOf<GitChange>()
        var index = 0
        while (index < records.size) {
            val record = records[index++]
            if (record.isEmpty()) continue
            val separator = record.indexOf('\t')
            val status = if (separator >= 0) record.substring(0, separator) else record
            val inlinePath = if (separator >= 0) record.substring(separator + 1) else ""
            val renamed = status.startsWith('R') || status.startsWith('C')
            val originalPathValue = if (renamed) inlinePath.ifBlank { records.getOrNull(index++) } else null
            val pathValue = if (renamed) records.getOrNull(index++) else inlinePath.ifBlank { records.getOrNull(index++) }
            val path = normalizeGitPath(pathValue) ?: continue
            changes += GitChange(path, normalizeGitPath(originalPathValue), status, conflict = false)
        }
        return changes
    }

    internal fun normalizeGitPath(value: String?): String? {
        if (value.isNullOrEmpty()) return null
        val normalized = value.trim().replace('\\', '/')
        if (normalized.startsWith('/') || normalized.contains('\u0000') || normalized.length > 512) return null
        if (normalized.split('/').any { it.isEmpty() || it == "." || it == ".." }) return null
        return normalized
    }

    fun abortMerge(root: Path, isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val merge = run(listOf("merge", "--abort"), root, isolateConfig)
        if (merge.exitCode == 0) return@locked GitActionResult(true, merge.stdout.trim().ifBlank { "Done" })
        val rebase = run(listOf("rebase", "--abort"), root, isolateConfig)
        if (rebase.exitCode == 0) GitActionResult(true, rebase.stdout.trim().ifBlank { "Done" }) else failure(rebase)
    }

    fun resolveConflict(root: Path, path: String, strategy: String, isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val normalized = path.trim()
        if (normalized.isEmpty()) return@locked GitActionResult(false, "Git path is required")
        if (strategy != "ours" && strategy != "theirs") return@locked GitActionResult(false, "Invalid conflict strategy")
        val current = status(root, isolateConfig)
        if (current.changes.none { it.path == normalized && it.conflict }) {
            return@locked GitActionResult(false, "Git path is not conflicted")
        }
        val checkout = run(listOf("checkout", "--$strategy", "--", normalized), root, isolateConfig)
        if (checkout.exitCode != 0) return@locked failure(checkout)
        val add = run(listOf("add", "--", normalized), root, isolateConfig)
        if (add.exitCode == 0) GitActionResult(true, add.stdout.trim().ifBlank { "Done" }) else failure(add)
    }

    fun automaticCheckpoint(
        root: Path,
        message: String,
        identity: GitIdentity = GitIdentity(),
        isolateConfig: Boolean = false,
        token: String = ""
    ): GitActionResult = locked(root) {
        var current = status(root, isolateConfig)
        if (!current.available) return@locked GitActionResult(true, "Git is unavailable; checkpoint skipped")
        if (!current.repository) {
            val initialized = init(root, identity, isolateConfig)
            if (!initialized.success) return@locked initialized
            current = status(root, isolateConfig)
        }
        if (current.merging || current.conflicts > 0) {
            return@locked GitActionResult(true, "Merge/rebase in progress; checkpoint skipped")
        }
        var result = GitActionResult(true, "No changes to commit")
        if (current.changes.isNotEmpty()) {
            result = commitLocked(root, message, identity, isolateConfig)
            if (!result.success) return@locked result
        }
        if (current.remote.isNotBlank() && (current.changes.isNotEmpty() || current.ahead > 0)) {
            return@locked push(root, isolateConfig, token)
        }
        result
    }

    fun continueOperation(root: Path, identity: GitIdentity = GitIdentity(), isolateConfig: Boolean = false): GitActionResult = locked(root) {
        val current = status(root, isolateConfig)
        if (!current.repository || current.operation == "none") return@locked GitActionResult(false, "No merge or rebase is in progress")
        if (current.conflicts > 0) return@locked GitActionResult(false, "Resolve all conflicts before continuing")
        val result = if (current.operation == "rebase") {
            run(identityArgs(identity) + listOf("-c", "core.editor=true", "rebase", "--continue"), root, isolateConfig)
        } else {
            run(identityArgs(identity) + listOf("commit", "--no-edit"), root, isolateConfig)
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

    private fun requireRemote(root: Path, isolateConfig: Boolean, block: () -> GitCommandResult): GitActionResult {
        val current = status(root, isolateConfig)
        if (!current.repository) return GitActionResult(false, "Git repository is not initialized")
        if (current.remote.isBlank()) return GitActionResult(false, "No origin remote is configured")
        val result = block()
        return if (result.exitCode == 0) GitActionResult(true, result.stdout.trim().ifBlank { result.stderr.trim().ifBlank { "Done" } })
        else failure(result)
    }

    private fun failure(result: GitCommandResult): GitActionResult =
        GitActionResult(false, result.stderr.trim().ifBlank { result.stdout.trim().ifBlank { "Git command failed" } })

    private val GIT_REMOTE_PREFIX = Regex("^(https?://|ssh://|git://|git@|file://)", RegexOption.IGNORE_CASE)

    private val windows: Boolean
        get() = System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)

    private fun <T> locked(root: Path, block: () -> T): T {
        val lock = locks.getOrPut(root.toAbsolutePath().normalize().pathString) { Any() }
        synchronized(lock) { return block() }
    }

    internal fun run(
        args: List<String>,
        cwd: Path?,
        isolateConfig: Boolean,
        token: String = "",
        timeoutMs: Long = TIMEOUT_MS
    ): GitCommandResult {
        var result = executeRun(args, cwd, isolateConfig, token, timeoutMs)
        if (cwd == null || !isIndexLockFailure(result)) return result
        Thread.sleep(INDEX_LOCK_RETRY_MS)
        result = executeRun(args, cwd, isolateConfig, token, timeoutMs)
        if (!isIndexLockFailure(result)) return result
        val quarantined = quarantineStaleIndexLock(cwd, isolateConfig)
        if (quarantined == null) return result
        try {
            return executeRun(args, cwd, isolateConfig, token, timeoutMs)
        } finally {
            Files.deleteIfExists(quarantined)
        }
    }

    private fun isIndexLockFailure(result: GitCommandResult): Boolean =
        INDEX_LOCK_FAILURE.containsMatchIn("${result.stderr}\n${result.stdout}")

    private fun quarantineStaleIndexLock(root: Path, isolateConfig: Boolean): Path? {
        val lockPath = resolveIndexLockPath(root, isolateConfig) ?: return null
        if (!lockPath.isRegularFile()) return null
        val initial = Files.getLastModifiedTime(lockPath)
        if (System.currentTimeMillis() - initial.toMillis() < STALE_INDEX_LOCK_MS) return null
        if (gitIndexLockHasOpenHandle(lockPath)) return null
        Thread.sleep(INDEX_LOCK_STABILITY_MS)
        if (!lockPath.isRegularFile()) return null
        val stable = Files.getLastModifiedTime(lockPath)
        if (initial != stable) return null
        val quarantined = lockPath.resolveSibling(
            "${lockPath.name}.mootool-stale-${System.currentTimeMillis()}-${ProcessHandle.current().pid()}",
        )
        return try {
            Files.move(lockPath, quarantined)
            quarantined
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveIndexLockPath(root: Path, isolateConfig: Boolean): Path? {
        val result = executeRun(listOf("rev-parse", "--git-path", "index.lock"), root, isolateConfig)
        if (result.exitCode != 0) return null
        val relative = result.stdout.trim()
        if (relative.isEmpty()) return null
        val lockPath = root.resolve(relative).normalize()
        return if (lockPath.name == "index.lock") lockPath else null
    }

    private fun gitIndexLockHasOpenHandle(lockPath: Path): Boolean {
        val executable = when {
            System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true) -> "/usr/sbin/lsof"
            System.getProperty("os.name").orEmpty().contains("Linux", ignoreCase = true) -> "lsof"
            else -> return false
        }
        return try {
            val process = ProcessBuilder(listOf(executable, "-t", "--", lockPath.pathString))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(2, TimeUnit.SECONDS)
            output.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    private fun executeRun(
        args: List<String>,
        cwd: Path?,
        isolateConfig: Boolean,
        token: String = "",
        timeoutMs: Long = TIMEOUT_MS
    ): GitCommandResult {
        val askPass = if (token.isNotBlank()) writeAskPass() else null
        val builder = ProcessBuilder(listOf("git") + args)
        cwd?.let { builder.directory(it.toFile()) }
        val env = builder.environment()
        env["GIT_TERMINAL_PROMPT"] = "0"
        env["GIT_OPTIONAL_LOCKS"] = "0"
        if (isolateConfig) {
            val empty = if (windows) "NUL" else "/dev/null"
            env["GIT_CONFIG_GLOBAL"] = empty
            env["GIT_CONFIG_SYSTEM"] = empty
        }
        if (askPass != null) {
            env["GIT_ASKPASS"] = askPass.toAbsolutePath().toString()
            env["SSH_ASKPASS"] = askPass.toAbsolutePath().toString()
            env["MOOTOOL_COMPOSE_GIT_TOKEN"] = token
        }
        return try {
            val process = builder.start()
            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            val outThread = Thread { process.inputStream.copyTo(stdout) }
            val errThread = Thread { process.errorStream.copyTo(stderr) }
            outThread.start()
            errThread.start()
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
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
        } finally {
            askPass?.let { Files.deleteIfExists(it) }
        }
    }

    private fun writeAskPass(): Path {
        val script = Files.createTempFile("mootool-compose-askpass-", if (windows) ".cmd" else ".sh")
        if (windows) {
            script.writeText("@echo off\r\necho %MOOTOOL_COMPOSE_GIT_TOKEN%\r\n")
        } else {
            script.writeText("#!/bin/sh\nprintf '%s\\n' \"\$MOOTOOL_COMPOSE_GIT_TOKEN\"\n")
            script.toFile().setExecutable(true, true)
        }
        return script
    }
}
