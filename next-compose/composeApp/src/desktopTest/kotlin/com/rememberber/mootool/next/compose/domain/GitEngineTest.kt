package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
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
            val vault = Files.createDirectories(parent.resolve("vault"))
            val nested = GitEngine.status(vault, isolateConfig = true)
            assertTrue(nested.available)
            assertFalse(nested.repository)
            val initialized = GitEngine.init(vault, identity, isolateConfig = true)
            assertTrue(initialized.success, initialized.message)
            val ownRepo = GitEngine.status(vault, isolateConfig = true)
            assertTrue(ownRepo.repository)
        } finally {
            parent.toFile().deleteRecursively()
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
}
