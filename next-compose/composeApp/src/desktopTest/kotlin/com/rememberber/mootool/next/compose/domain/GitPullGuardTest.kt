package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

/** 锁定 [GitEngine.pull] 在 merge 中优先于 remote 校验（DIFF-447）。 */
class GitPullGuardTest {
    @Test
    fun mergeInProgressRejectedBeforeMissingRemoteMessage() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-pull-guard-")
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
            assertTrue(kotlin.runCatching { git(root, "merge", "other") }.isFailure)
            val pull = GitEngine.pull(root, isolateConfig = true)
            assertFalse(pull.success)
            assertTrue(pull.message.contains("merge", ignoreCase = true), pull.message)
            assertFalse(pull.message.contains("remote", ignoreCase = true), pull.message)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun assumeGit() {
        Assume.assumeTrue("git CLI not installed", GitEngine.detect(isolateConfig = true).available)
    }

    private fun git(root: java.nio.file.Path, vararg args: String): String {
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
