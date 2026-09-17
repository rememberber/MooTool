package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

/** Vault Git discard during merge conflict（对齐 Electron `VaultGitService.discard`，冲突文件可单独还原）。 */
class GitDiscardDuringMergeTest {
    @Test
    fun discardRestoresTrackedConflictMarkdownDuringMerge() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-discard-merge-md-")
        try {
            val identity = GitIdentity("Test User", "test@local")
            assertTrue(GitEngine.init(root, identity, isolateConfig = true).success)
            root.resolve("notes/conflict.md").parent?.let { Files.createDirectories(it) }
            root.resolve("notes/conflict.md").writeText("base")
            assertTrue(GitEngine.commit(root, "Base", identity, isolateConfig = true).success)
            val baseBranch = GitEngine.status(root, isolateConfig = true).branch
            git(root, "checkout", "-b", "other")
            root.resolve("notes/conflict.md").writeText("other")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Other")
            git(root, "checkout", baseBranch)
            root.resolve("notes/conflict.md").writeText("base-branch")
            git(root, "add", "--all")
            git(root, "commit", "-m", "Base branch")
            assertTrue(kotlin.runCatching { git(root, "merge", "other") }.isFailure)
            val status = GitEngine.status(root, isolateConfig = true)
            assertTrue(status.conflicts >= 1)
            val discarded = GitEngine.discard(root, "notes/conflict.md", isolateConfig = true)
            assertTrue(discarded.success, discarded.message)
            val after = GitEngine.status(root, isolateConfig = true)
            assertFalse(after.changes.any { it.path == "notes/conflict.md" && it.status.contains('U') })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun discardRestoresTrackedConflictFileDuringMerge() {
        assumeGit()
        val root = Files.createTempDirectory("mootool-compose-git-discard-merge-")
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
            val discarded = GitEngine.discard(root, "conflict.json", isolateConfig = true)
            assertTrue(discarded.success, discarded.message)
            val after = GitEngine.status(root, isolateConfig = true)
            assertFalse(after.changes.any { it.path == "conflict.json" && it.status.contains('U') })
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
