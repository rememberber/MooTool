package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

class GitEngineTest {
    @Test
    fun parsesPorcelainStatusAndBranch() {
        val parsed = GitEngine.parsePorcelain("## main...origin/main [ahead 2, behind 1]\u0000 M note.md\u0000?? new.md\u0000R  after.md\u0000before.md\u0000UU conflict.md\u0000")
        assertEquals("## main...origin/main [ahead 2, behind 1]", parsed.branchLine)
        assertEquals("main", GitEngine.parseBranch(parsed.branchLine))
        assertEquals(4, parsed.changes.size)
        assertEquals(" M", parsed.changes[0].status)
        assertEquals("note.md", parsed.changes[0].path)
        assertFalse(parsed.changes[0].conflict)
        assertEquals("??", parsed.changes[1].status)
        assertEquals("new.md", parsed.changes[1].path)
        assertEquals("R ", parsed.changes[2].status)
        assertEquals("after.md", parsed.changes[2].path)
        assertEquals("before.md", parsed.changes[2].originalPath)
        assertTrue(parsed.changes[3].conflict)
        assertEquals("feature", GitEngine.parseBranch("## No commits yet on feature"))
        assertEquals("HEAD", GitEngine.parseBranch("## HEAD (no branch)"))
    }

    @Test
    fun mirrorsElectronVaultGitInitCommitDiffHistory() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-electron-parity-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertFalse(GitEngine.status(root, isolateConfig = true).repository)
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            assertTrue(GitEngine.status(root, isolateConfig = true).branch.isNotBlank())
            root.resolve("sample.json").writeText("""{"value":1}\n""")
            val untracked = GitEngine.status(root, isolateConfig = true).changes.single()
            assertEquals("sample.json", untracked.path)
            assertEquals("??", untracked.status.trim())
            assertTrue(GitEngine.commit(root, "Add sample", identity, isolateConfig = true).success)
            val commit = GitEngine.history(root, isolateConfig = true).first()
            assertEquals("Add sample", commit.message)
            assertTrue(commit.author.isNotBlank())
            val firstCommitDiff = GitEngine.fileDiffs(root, commit = commit.hash, isolateConfig = true).single()
            assertEquals("sample.json", firstCommitDiff.path)
            assertEquals("", firstCommitDiff.before)
            assertEquals("""{"value":1}\n""", firstCommitDiff.after)
            assertEquals(GitDiffPreview.Text, firstCommitDiff.preview)
            root.resolve("sample.json").writeText("""{"value":2}\n""")
            val working = GitEngine.fileDiffs(root, "sample.json", isolateConfig = true).single()
            assertEquals("""{"value":1}\n""", working.before)
            assertEquals("""{"value":2}\n""", working.after)
            root.resolve("notes.txt").writeText("new note\n")
            assertTrue(GitEngine.commit(root, "Update sample", identity, isolateConfig = true).success)
            val updated = GitEngine.history(root, isolateConfig = true).first()
            val committed = GitEngine.fileDiffs(root, commit = updated.hash, isolateConfig = true)
            assertEquals("""{"value":1}\n""", committed.first { it.path == "sample.json" }.before)
            assertEquals("""{"value":2}\n""", committed.first { it.path == "sample.json" }.after)
            assertEquals("", committed.first { it.path == "notes.txt" }.before)
            assertEquals("new note\n", committed.first { it.path == "notes.txt" }.after)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun pullBlockedWhileMergeInProgress() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-pull-merge-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("conflict.json").writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(root, "Base", identity, isolateConfig = true).success)
            val baseBranch = GitEngine.status(root, isolateConfig = true).branch
            git(root, "checkout", "-b", "other")
            root.resolve("conflict.json").writeText("""{"side":"other"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Other")
            git(root, "checkout", baseBranch)
            root.resolve("conflict.json").writeText("""{"side":"base-branch"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Base branch")
            assertTrue(runCatching { git(root, "merge", "other") }.isFailure)
            val pull = GitEngine.pull(root, isolateConfig = true)
            assertFalse(pull.success)
            assertTrue(pull.message.contains("merge", ignoreCase = true), pull.message)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsCommitWhileMergeInProgress() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-commit-merge-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("conflict.json").writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(root, "Base", identity, isolateConfig = true).success)
            val baseBranch = GitEngine.status(root, isolateConfig = true).branch
            git(root, "checkout", "-b", "other")
            root.resolve("conflict.json").writeText("""{"side":"other"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Other")
            git(root, "checkout", baseBranch)
            root.resolve("conflict.json").writeText("""{"side":"base-branch"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Base branch")
            assertTrue(runCatching { git(root, "merge", "other") }.isFailure)
            val blocked = GitEngine.commit(root, "Must not commit", identity, isolateConfig = true)
            assertFalse(blocked.success)
            assertTrue(blocked.message.contains("merge", ignoreCase = true), blocked.message)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun truncatesCommitMessageToThreeHundredCharacters() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-commit-trim-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("note.txt").writeText("x")
            val longMessage = "m".repeat(320)
            assertTrue(GitEngine.commit(root, longMessage, identity, isolateConfig = true).success)
            val saved = GitEngine.history(root, isolateConfig = true).first().message
            assertEquals(300, saved.length)
            assertEquals("m".repeat(300), saved)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun initWritesGitignoreAndIgnoresDsStore() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-ignore-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            assertTrue(root.resolve(".gitignore").readText().contains(".DS_Store"))
            root.resolve(".DS_Store").writeText("ignored")
            assertTrue(GitEngine.status(root, isolateConfig = true).changes.isEmpty())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun initCommitAndHistoryInTemporaryDirectory() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            val initialized = GitEngine.init(root, identity, isolateConfig = true)
            assertTrue(initialized.success, initialized.message)
            val afterInit = GitEngine.status(root, isolateConfig = true)
            assertTrue(afterInit.available)
            assertTrue(afterInit.repository)
            assertTrue(afterInit.changes.isEmpty(), afterInit.changes.toString())
            root.resolve("note.md").writeText("hello")
            val dirty = GitEngine.status(root, isolateConfig = true)
            assertTrue(dirty.changes.any { it.path == "note.md" })
            val missingMessage = GitEngine.commit(root, "   ", identity, isolateConfig = true)
            assertFalse(missingMessage.success)
            val committed = GitEngine.commit(root, "add note", identity, isolateConfig = true)
            assertTrue(committed.success, committed.message)
            val clean = GitEngine.status(root, isolateConfig = true)
            assertTrue(clean.changes.isEmpty(), clean.changes.toString())
            val history = GitEngine.history(root, isolateConfig = true)
            assertTrue(history.any { it.message == "add note" }, history.toString())
            assertTrue(history.any { it.message.contains("Initial") }, history.toString())
            val unchanged = GitEngine.commit(root, "again", identity, isolateConfig = true)
            assertTrue(unchanged.success)
            assertEquals("No changes to commit", unchanged.message)
            val remote = GitEngine.setRemote(root, "https://example.invalid/vault.git", isolateConfig = true)
            assertTrue(remote.success, remote.message)
            assertEquals("https://example.invalid/vault.git", GitEngine.status(root, isolateConfig = true).remote)
            val missingRemote = GitEngine.pull(root, isolateConfig = true)
            assertFalse(missingRemote.success)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun pullLeavesMergeConflictWhenHistoriesDiverge() {
        assumeGit()
        val bare = Files.createTempDirectory("mootool-compose-git-pull-bare-")
        val upstream = Files.createTempDirectory("mootool-compose-git-pull-up-")
        val local = Files.createTempDirectory("mootool-compose-git-pull-local-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            git(bare, "init", "--bare")
            val url = bare.toAbsolutePath().toUri().toString()
            assertTrue(GitEngine.init(upstream, identity, isolateConfig = true).success)
            upstream.resolve("conflict.json").writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(upstream, "Base", identity, isolateConfig = true).success)
            assertTrue(GitEngine.setRemote(upstream, url, isolateConfig = true).success)
            assertTrue(GitEngine.push(upstream, isolateConfig = true).success)
            git(local, "clone", url, ".")
            upstream.resolve("conflict.json").writeText("""{"side":"remote"}""")
            assertTrue(GitEngine.commit(upstream, "Remote", identity, isolateConfig = true).success)
            assertTrue(GitEngine.push(upstream, isolateConfig = true).success)
            local.resolve("conflict.json").writeText("""{"side":"local"}""")
            assertTrue(GitEngine.commit(local, "Local", identity, isolateConfig = true).success)
            val pulled = GitEngine.pull(local, isolateConfig = true)
            assertFalse(pulled.success, pulled.message)
            val status = GitEngine.status(local, isolateConfig = true)
            assertEquals("merge", status.operation)
            assertTrue(status.merging)
            assertTrue(status.conflicts >= 1)
            val resolved = GitEngine.resolveConflict(local, "conflict.json", "theirs", isolateConfig = true)
            assertTrue(resolved.success, resolved.message)
            assertEquals(0, GitEngine.status(local, isolateConfig = true).conflicts)
            val continued = GitEngine.continueOperation(local, identity, isolateConfig = true)
            assertTrue(continued.success, continued.message)
            val done = GitEngine.status(local, isolateConfig = true)
            assertEquals("none", done.operation)
            assertFalse(done.merging)
            assertTrue(local.resolve("conflict.json").readText().contains("remote"))
        } finally {
            bare.toFile().deleteRecursively()
            upstream.toFile().deleteRecursively()
            local.toFile().deleteRecursively()
        }
    }

    @Test
    fun pushFailsWhenRemoteIsAheadWithoutPull() {
        assumeGit()
        val bare = Files.createTempDirectory("mootool-compose-git-push-bare-")
        val peerA = Files.createTempDirectory("mootool-compose-git-push-a-")
        val peerB = Files.createTempDirectory("mootool-compose-git-push-b-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            git(bare, "init", "--bare")
            val url = bare.toAbsolutePath().toUri().toString()
            git(peerA, "clone", url, ".")
            git(peerB, "clone", url, ".")
            peerA.resolve("shared.json").writeText("""{"from":"a"}""")
            git(peerA, "add", "--all")
            git(peerA, "commit", "-m", "A")
            assertTrue(GitEngine.push(peerA, isolateConfig = true).success)
            peerB.resolve("shared.json").writeText("""{"from":"b"}""")
            git(peerB, "add", "--all")
            git(peerB, "commit", "-m", "B")
            val rejected = GitEngine.push(peerB, isolateConfig = true)
            assertFalse(rejected.success, rejected.message)
            assertTrue(
                rejected.message.contains("rejected", ignoreCase = true) ||
                    rejected.message.contains("non-fast-forward", ignoreCase = true) ||
                    rejected.message.contains("fetch first", ignoreCase = true),
                rejected.message,
            )
        } finally {
            bare.toFile().deleteRecursively()
            peerA.toFile().deleteRecursively()
            peerB.toFile().deleteRecursively()
        }
    }

    @Test
    fun discardsChangesAndPushesToLocalRemote() {
        assumeGit()
        val work = Files.createTempDirectory("mootool-compose-git-work-")
        val remote = Files.createTempDirectory("mootool-compose-git-remote-")
        val clone = Files.createTempDirectory("mootool-compose-git-clone-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(work, identity, isolateConfig = true).success)
            work.resolve("tracked.md").writeText("keep")
            assertTrue(GitEngine.commit(work, "add tracked", identity, isolateConfig = true).success)
            work.resolve("tracked.md").writeText("dirty")
            work.resolve("scratch.md").writeText("tmp")
            val discarded = GitEngine.discard(work, "tracked.md", isolateConfig = true)
            assertTrue(discarded.success, discarded.message)
            assertEquals("keep", Files.readString(work.resolve("tracked.md")))
            val cleaned = GitEngine.discard(work, "scratch.md", isolateConfig = true)
            assertTrue(cleaned.success, cleaned.message)
            assertFalse(Files.exists(work.resolve("scratch.md")))
            val bare = GitEngine.run(listOf("init", "--bare"), remote, isolateConfig = true)
            assertEquals(0, bare.exitCode, bare.stderr + bare.stdout)
            val url = remote.toAbsolutePath().toUri().toString()
            assertTrue(GitEngine.setRemote(work, url, isolateConfig = true).success)
            val pushed = GitEngine.push(work, isolateConfig = true)
            assertTrue(pushed.success, pushed.message)
            val cloned = GitEngine.run(listOf("clone", url, "."), clone, isolateConfig = true)
            assertEquals(0, cloned.exitCode, cloned.stderr + cloned.stdout)
            assertTrue(Files.exists(clone.resolve("tracked.md")))
            work.resolve("later.md").writeText("from work")
            assertTrue(GitEngine.commit(work, "later", identity, isolateConfig = true).success)
            assertTrue(GitEngine.push(work, isolateConfig = true).success)
            val pulled = GitEngine.pull(clone, isolateConfig = true)
            assertTrue(pulled.success, pulled.message)
            assertEquals("from work", Files.readString(clone.resolve("later.md")))
        } finally {
            work.toFile().deleteRecursively()
            remote.toFile().deleteRecursively()
            clone.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsParentRepositoryUntilVaultRootIsInitialized() {
        assumeGit()
        val parent = Files.createTempDirectory("mootool-compose-git-parent-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(parent, identity, isolateConfig = true).success)
            parent.resolve("tracked.txt").writeText("tracked\n")
            git(parent, "add", "tracked.txt")
            git(parent, "commit", "-m", "Parent base")
            val vault = Files.createDirectories(parent.resolve("vault"))
            parent.resolve("outside-unrelated.txt").writeText("outside\n")
            vault.resolve("inside.json").writeText("{}\n")
            val nested = GitEngine.status(vault, isolateConfig = true)
            assertTrue(nested.available)
            assertFalse(nested.repository)
            val initialized = GitEngine.init(vault, identity, isolateConfig = true)
            assertTrue(initialized.success, initialized.message)
            val ownRepo = GitEngine.status(vault, isolateConfig = true)
            assertTrue(ownRepo.repository)
            val parentStatus = git(parent, "status", "--porcelain=v1")
            assertTrue(parentStatus.contains("outside-unrelated.txt"), parentStatus)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun fileDiffsMatchesElectronPathRules() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-path-rules-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            assertTrue(GitEngine.fileDiffs(root, "valid..name.json", isolateConfig = true).isEmpty())
            assertTrue(
                runCatching { GitEngine.fileDiffs(root, "../outside", isolateConfig = true) }.isFailure,
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun automaticCheckpointInitializesRepositoryOnFirstUse() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-checkpoint-init-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            root.resolve("first-note.txt").writeText("Saved automatically\n")
            assertFalse(GitEngine.status(root, isolateConfig = true).repository)
            val checkpoint = GitEngine.automaticCheckpoint(root, "Automatic checkpoint", identity, isolateConfig = true)
            assertTrue(checkpoint.success, checkpoint.message)
            val after = GitEngine.status(root, isolateConfig = true)
            assertTrue(after.repository)
            assertTrue(after.changes.isEmpty(), after.changes.toString())
            assertTrue(
                GitEngine.history(root, isolateConfig = true).any { it.message.contains("Initial") },
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun automaticCheckpointPushesWhenRemoteConfigured() {
        assumeGit()
        val work = Files.createTempDirectory("mootool-compose-git-checkpoint-push-")
        val bare = Files.createTempDirectory("mootool-compose-git-checkpoint-bare-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            git(bare, "init", "--bare")
            assertTrue(GitEngine.init(work, identity, isolateConfig = true).success)
            val url = bare.toAbsolutePath().toUri().toString()
            assertTrue(GitEngine.setRemote(work, url, isolateConfig = true).success)
            work.resolve("checkpoint.json").writeText("""{"saved":true}""")
            val checkpoint = GitEngine.automaticCheckpoint(work, "Automatic checkpoint", identity, isolateConfig = true)
            assertTrue(checkpoint.success, checkpoint.message)
            val remoteLog = git(bare, "log", "--all", "--pretty=%s")
            assertTrue(remoteLog.contains("Automatic checkpoint"), remoteLog)
            assertEquals(0, GitEngine.status(work, isolateConfig = true).ahead)
        } finally {
            work.toFile().deleteRecursively()
            bare.toFile().deleteRecursively()
        }
    }

    @Test
    fun unicodePathsWorkForStatusDiffDiscardAndRename() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-unicode-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            val original = "中文随手记.txt"
            val renamed = "重命名后的随手记.txt"
            root.resolve(original).writeText("第一版\n")
            assertTrue(GitEngine.commit(root, "Add unicode note", identity, isolateConfig = true).success)
            root.resolve(original).writeText("第二版\n")
            val status = GitEngine.status(root, isolateConfig = true)
            assertEquals(original, status.changes.first().path)
            val files = GitEngine.fileDiffs(root, original, isolateConfig = true)
            assertEquals("第一版", files.single().before.trim())
            assertEquals("第二版", files.single().after.trim())
            assertTrue(GitEngine.discard(root, original, isolateConfig = true).success)
            git(root, "mv", original, renamed)
            val renamedChange = GitEngine.status(root, isolateConfig = true).changes.first()
            assertEquals(renamed, renamedChange.path)
            assertEquals(original, renamedChange.originalPath)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun fileDiffShowsWorkingTreeBeforeAndAfter() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-diff-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("note.md").writeText("hello")
            assertTrue(GitEngine.commit(root, "add note", identity, isolateConfig = true).success)
            root.resolve("note.md").writeText("hello world")
            val files = GitEngine.fileDiffs(root, "note.md", isolateConfig = true)
            assertEquals(1, files.size)
            assertEquals("hello", files[0].before.trim())
            assertEquals("hello world", files[0].after.trim())
            assertEquals(GitDiffPreview.Text, files[0].preview)
            val untracked = root.resolve("new.md")
            untracked.writeText("fresh")
            val added = GitEngine.fileDiffs(root, "new.md", isolateConfig = true)
            assertEquals("", added.single().before)
            assertEquals("fresh", added.single().after.trim())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun commitDiffListsAllChangedFiles() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-files-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("a.md").writeText("a1")
            root.resolve("b.md").writeText("b1")
            assertTrue(GitEngine.commit(root, "first", identity, isolateConfig = true).success)
            root.resolve("a.md").writeText("a2")
            root.resolve("b.md").writeText("b2")
            assertTrue(GitEngine.commit(root, "second", identity, isolateConfig = true).success)
            val history = GitEngine.history(root, isolateConfig = true)
            val latest = history.first()
            val files = GitEngine.fileDiffs(root, commit = latest.hash, isolateConfig = true)
            assertEquals(2, files.size)
            assertEquals(setOf("a.md", "b.md"), files.map { it.path }.toSet())
            val a = files.first { it.path == "a.md" }
            assertEquals("a1", a.before.trim())
            assertEquals("a2", a.after.trim())
            assertEquals("b.md", GitDiffSelection.selected(files, "b.md")?.path)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolvesAndAbortsMergeConflicts() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-merge-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("conflict.json").writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(root, "Base", identity, isolateConfig = true).success)
            val baseBranch = GitEngine.status(root, isolateConfig = true).branch
            git(root, "checkout", "-b", "other")
            root.resolve("conflict.json").writeText("""{"side":"other"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Other")
            git(root, "checkout", baseBranch)
            root.resolve("conflict.json").writeText("""{"side":"base-branch"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Base branch")
            assertTrue(runCatching { git(root, "merge", "other") }.isFailure)
            val conflicted = GitEngine.status(root, isolateConfig = true)
            assertTrue(conflicted.conflicts >= 1)
            assertTrue(conflicted.merging)
            val conflictedHead = GitEngine.history(root, isolateConfig = true).first().hash
            val checkpoint = GitEngine.automaticCheckpoint(
                root,
                "Must not commit conflicts",
                identity,
                isolateConfig = true
            )
            assertTrue(checkpoint.success, checkpoint.message)
            assertEquals(conflictedHead, GitEngine.history(root, isolateConfig = true).first().hash)
            val resolved = GitEngine.resolveConflict(root, "conflict.json", "ours", isolateConfig = true)
            assertTrue(resolved.success, resolved.message)
            assertEquals(0, GitEngine.status(root, isolateConfig = true).conflicts)
            val aborted = GitEngine.abortMerge(root, isolateConfig = true)
            assertTrue(aborted.success, aborted.message)
            val done = GitEngine.status(root, isolateConfig = true)
            assertEquals(0, done.conflicts)
            assertFalse(done.merging)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun continuesMergeAfterConflictResolved() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-merge-continue-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("conflict.json").writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(root, "Base", identity, isolateConfig = true).success)
            val baseBranch = GitEngine.status(root, isolateConfig = true).branch
            git(root, "checkout", "-b", "other")
            root.resolve("conflict.json").writeText("""{"side":"other"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Other")
            git(root, "checkout", baseBranch)
            root.resolve("conflict.json").writeText("""{"side":"base-branch"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Base branch")
            assertTrue(runCatching { git(root, "merge", "other") }.isFailure)
            val during = GitEngine.status(root, isolateConfig = true)
            assertEquals("merge", during.operation)
            assertTrue(during.merging)
            assertTrue(during.conflicts >= 1)
            val resolved = GitEngine.resolveConflict(root, "conflict.json", "theirs", isolateConfig = true)
            assertTrue(resolved.success, resolved.message)
            assertEquals(0, GitEngine.status(root, isolateConfig = true).conflicts)
            assertTrue(GitEngine.status(root, isolateConfig = true).merging)
            val continued = GitEngine.continueOperation(root, identity, isolateConfig = true)
            assertTrue(continued.success, continued.message)
            val done = GitEngine.status(root, isolateConfig = true)
            assertEquals("none", done.operation)
            assertEquals(0, done.conflicts)
            assertFalse(done.merging)
            assertTrue(root.resolve("conflict.json").readText().contains("other"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun continuesRebaseAfterConflictResolved() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-rebase-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("conflict.json").writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(root, "Base", identity, isolateConfig = true).success)
            val baseBranch = GitEngine.status(root, isolateConfig = true).branch
            git(root, "checkout", "-b", "feature")
            root.resolve("conflict.json").writeText("""{"side":"feature"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Feature")
            git(root, "checkout", baseBranch)
            root.resolve("conflict.json").writeText("""{"side":"main"}""")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Main")
            git(root, "checkout", "feature")
            assertTrue(runCatching { git(root, "rebase", baseBranch) }.isFailure)
            val during = GitEngine.status(root, isolateConfig = true)
            assertEquals("rebase", during.operation)
            assertTrue(during.merging)
            assertTrue(during.conflicts >= 1)
            val resolved = GitEngine.resolveConflict(root, "conflict.json", "theirs", isolateConfig = true)
            assertTrue(resolved.success, resolved.message)
            val afterResolve = GitEngine.status(root, isolateConfig = true)
            assertEquals(0, afterResolve.conflicts)
            assertTrue(afterResolve.merging)
            val continued = GitEngine.continueOperation(root, identity, isolateConfig = true)
            assertTrue(continued.success, continued.message)
            val done = GitEngine.status(root, isolateConfig = true)
            assertEquals("none", done.operation)
            assertEquals(0, done.conflicts)
            assertFalse(done.merging)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsUnsafeGitRemotes() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-remote-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            assertFalse(GitEngine.setRemote(root, "javascript:alert(1)", isolateConfig = true).success)
            assertFalse(GitEngine.setRemote(root, "/etc/passwd", isolateConfig = true).success)
            val bare = Files.createTempDirectory("mootool-compose-git-remote-bare-")
            try {
                git(bare, "init", "--bare")
                val url = bare.toAbsolutePath().toUri().toString()
                assertTrue(GitEngine.setRemote(root, url, isolateConfig = true).success)
                assertTrue(GitEngine.setRemote(root, "", isolateConfig = true).success)
            } finally {
                bare.toFile().deleteRecursively()
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun repairsStaleIndexLockAndRetriesCommit() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-stale-lock-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("stale-lock.txt").writeText("Recovered\n")
            val lockPath = root.resolve(".git/index.lock")
            lockPath.writeText("")
            val stale = FileTime.from(Instant.now().minusSeconds(10 * 60))
            Files.setLastModifiedTime(lockPath, stale)
            val committed = GitEngine.commit(root, "Recover stale lock", identity, isolateConfig = true)
            assertTrue(committed.success, committed.message)
            assertFalse(Files.exists(lockPath))
            val gitDir = root.resolve(".git")
            val leftoverQuarantine = gitDir.toFile().listFiles()?.any { it.name.startsWith("index.lock.mootool-stale-") } == true
            assertFalse(leftoverQuarantine)
            assertEquals("Recover stale lock", git(root, "log", "-1", "--pretty=%s").trim())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun doesNotQuarantineIndexLockHeldOpenByThisProcess() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-held-lock-")
        var lockStream: java.io.InputStream? = null
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("held-lock.txt").writeText("Keep held lock\n")
            val lockPath = root.resolve(".git/index.lock")
            lockPath.writeText("")
            val stale = FileTime.from(Instant.now().minusSeconds(10 * 60))
            Files.setLastModifiedTime(lockPath, stale)
            lockStream = lockPath.toFile().inputStream()
            val blocked = GitEngine.commit(root, "Must stay blocked", identity, isolateConfig = true)
            assertFalse(blocked.success)
            assertTrue(Files.exists(lockPath))
        } finally {
            lockStream?.close()
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun recentIndexLockBlocksCommit() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-active-lock-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("active-lock.txt").writeText("Keep lock\n")
            val lockPath = root.resolve(".git/index.lock")
            lockPath.writeText("")
            val blocked = GitEngine.commit(root, "Must stay blocked", identity, isolateConfig = true)
            assertFalse(blocked.success)
            assertTrue(blocked.message.contains("index.lock", ignoreCase = true), blocked.message)
            assertTrue(Files.exists(lockPath))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun serializesConcurrentCommitsForSameVaultRoot() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-concurrent-")
        val pool = Executors.newFixedThreadPool(2)
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("concurrent.txt").writeText("One change\n")
            val start = CountDownLatch(1)
            val done = CountDownLatch(2)
            val results = java.util.concurrent.ConcurrentLinkedQueue<GitActionResult>()
            repeat(2) { index ->
                pool.submit {
                    start.await()
                    results.add(
                        GitEngine.commit(
                            root,
                            if (index == 0) "First checkpoint" else "Second checkpoint",
                            identity,
                            isolateConfig = true,
                        ),
                    )
                    done.countDown()
                }
            }
            start.countDown()
            assertTrue(done.await(30, TimeUnit.SECONDS))
            assertEquals(2, results.size)
            assertTrue(results.all { it.success }, results.map { it.message }.toString())
            val log = git(root, "log", "--pretty=%s", "--", "concurrent.txt").trim().lines().filter { it.isNotBlank() }
            assertEquals(1, log.size, log.toString())
        } finally {
            pool.shutdownNow()
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun parseNameStatusAndRejectsPathTraversal() {
        val renamed = GitEngine.parseNameStatus("R100\u0000old.md\u0000new.md\u0000")
        assertEquals("new.md", renamed.single().path)
        assertEquals("old.md", renamed.single().originalPath)
        assertEquals(null, GitEngine.normalizeGitPath("../secret"))
        assertEquals(null, GitEngine.normalizeGitPath("/etc/passwd"))
        assertEquals("folder/note.md", GitEngine.normalizeGitPath("folder/note.md"))
    }

    private fun assumeGit() {
        Assume.assumeTrue("git CLI not installed", GitEngine.detect(isolateConfig = true).available)
    }

    private fun git(root: Path, vararg args: String): String {
        val process = ProcessBuilder(listOf("git") + args)
            .directory(root.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code != 0) throw IllegalStateException("git ${args.joinToString(" ")} failed ($code): $output")
        return output
    }
}
