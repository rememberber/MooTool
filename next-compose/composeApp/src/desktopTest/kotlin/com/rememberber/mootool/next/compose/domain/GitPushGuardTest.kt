package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

/** 未解决冲突时 push 引擎拒绝（与 commit / Vault Git 面板 UI 一致，补充 DIFF-516 UI 禁用）。 */
class GitPushGuardTest {
    @Test
    fun pushBlockedWhileMergeConflictsRemain() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-push-guard-")
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
            val status = GitEngine.status(root, isolateConfig = true)
            assertTrue(status.conflicts >= 1)
            val pushed = GitEngine.push(root, isolateConfig = true)
            assertFalse(pushed.success)
            assertTrue(
                pushed.message.contains("merge", ignoreCase = true) ||
                    pushed.message.contains("conflict", ignoreCase = true),
                pushed.message,
            )
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
