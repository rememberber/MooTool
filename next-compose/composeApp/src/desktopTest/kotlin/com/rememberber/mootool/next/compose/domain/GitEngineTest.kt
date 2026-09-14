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
        } finally {
            root.toFile().deleteRecursively()
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

    private fun assumeGit() {
        Assume.assumeTrue("git CLI not installed", GitEngine.detect(isolateConfig = true).available)
    }
}
