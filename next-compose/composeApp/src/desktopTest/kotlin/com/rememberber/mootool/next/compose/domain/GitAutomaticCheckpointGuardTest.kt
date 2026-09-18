package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Assume

/** merge/rebase 或冲突期自动检查点跳过（对齐 Electron `performAutomaticCheckpoint`）。 */
class GitAutomaticCheckpointGuardTest {
    @Test
    fun automaticCheckpointSkippedWhileMergeConflictsRemain() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-checkpoint-guard-")
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
            val headBefore = GitEngine.history(root, isolateConfig = true).first().hash
            val checkpoint = GitEngine.automaticCheckpoint(
                root,
                "Must not commit during conflict",
                identity,
                isolateConfig = true,
            )
            assertTrue(checkpoint.success, checkpoint.message)
            assertTrue(
                checkpoint.message.contains("checkpoint skipped", ignoreCase = true) ||
                    checkpoint.message.contains("merge", ignoreCase = true),
                checkpoint.message,
            )
            assertEquals(headBefore, GitEngine.history(root, isolateConfig = true).first().hash)
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
