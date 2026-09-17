package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

/**
 * 锁定 rebase 冲突期间 pull / commit / continue / abort 行为（对齐 Electron `VaultGitService`）。
 */
class GitRebaseGuardTest {
    @Test
    fun pullBlockedWhileRebaseInProgress() {
        assumeGit()
        val (root, _) = rebaseConflictFixture()
        try {
            val pull = GitEngine.pull(root, isolateConfig = true)
            assertFalse(pull.success)
            assertTrue(pull.message.contains("merge", ignoreCase = true), pull.message)
            assertFalse(pull.message.contains("remote", ignoreCase = true), pull.message)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun commitBlockedWhileRebaseConflictsRemain() {
        assumeGit()
        val (root, identity) = rebaseConflictFixture()
        try {
            val blocked = GitEngine.commit(root, "Must not commit", identity, isolateConfig = true)
            assertFalse(blocked.success)
            assertTrue(blocked.message.contains("merge", ignoreCase = true), blocked.message)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun continueRejectedWhileRebaseConflictsRemain() {
        assumeGit()
        val (root, identity) = rebaseConflictFixture()
        try {
            val continued = GitEngine.continueOperation(root, identity, isolateConfig = true)
            assertFalse(continued.success, continued.message)
            assertTrue(continued.message.contains("conflict", ignoreCase = true), continued.message)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun abortEndsRebaseWithUnresolvedConflicts() {
        assumeGit()
        val (root, _) = rebaseConflictFixture()
        try {
            val during = GitEngine.status(root, isolateConfig = true)
            assertEquals("rebase", during.operation)
            assertTrue(during.conflicts >= 1)
            val aborted = GitEngine.abortMerge(root, isolateConfig = true)
            assertTrue(aborted.success, aborted.message)
            val done = GitEngine.status(root, isolateConfig = true)
            assertEquals("none", done.operation)
            assertEquals(0, done.conflicts)
            assertFalse(done.merging)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun rebaseConflictFixture(): Pair<Path, GitIdentity> {
        val root = Files.createTempDirectory("mootool-compose-git-rebase-guard-")
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
        return root to identity
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
