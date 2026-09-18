package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

/**
 * 锁定 `prepare-git-merge-conflict-evidence.sh` 与 §B 产品走查：
 * pull 分叉 → conflict.json 未合并 → 面板自动选中 → resolve → continue。
 */
class GitMergeProductEvidenceFlowTest {
    @Test
    fun pullConflictMatchesEvidenceScriptAndProductFlowPresentation() {
        assumeGit()
        val bare = Files.createTempDirectory("mootool-compose-git-evidence-bare-")
        val upstream = Files.createTempDirectory("mootool-compose-git-evidence-up-")
        val local = Files.createTempDirectory("mootool-compose-git-evidence-local-")
        try {
            val identity = GitIdentity("MooTool Evidence", "mootool-evidence@local")
            git(bare, "init", "--bare")
            val url = bare.toAbsolutePath().toUri().toString()
            assertTrue(GitEngine.init(upstream, identity, isolateConfig = true).success)
            upstream.resolve(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE).writeText("""{"side":"base"}""")
            assertTrue(GitEngine.commit(upstream, "Base", identity, isolateConfig = true).success)
            assertTrue(GitEngine.setRemote(upstream, url, isolateConfig = true).success)
            assertTrue(GitEngine.push(upstream, isolateConfig = true).success)
            git(local, "clone", url, ".")
            upstream.resolve(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE).writeText("""{"side":"remote"}""")
            assertTrue(GitEngine.commit(upstream, "Remote", identity, isolateConfig = true).success)
            assertTrue(GitEngine.push(upstream, isolateConfig = true).success)
            local.resolve(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE).writeText("""{"side":"local"}""")
            assertTrue(GitEngine.commit(local, "Local", identity, isolateConfig = true).success)
            val pulled = GitEngine.pull(local, isolateConfig = true)
            assertFalse(pulled.success, pulled.message)

            val status = GitEngine.status(local, isolateConfig = true)
            assertEquals("merge", status.operation)
            assertTrue(status.merging)
            assertTrue(status.conflicts >= 1)
            val unmerged = status.changes.filter { it.conflict }.map { it.path }
            assertTrue(unmerged.contains(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE))
            assertTrue(
                GitMergeProductFlowPresentation.evidenceReady(
                    merging = status.merging,
                    conflicts = status.conflicts,
                    unmergedPaths = unmerged,
                ),
            )
            val auto = GitMergeProductFlowPresentation.autoSelectConflictPath(
                merging = status.merging,
                conflicts = status.conflicts,
                changes = status.changes.map { it.path to it.conflict },
                currentSelected = "",
            )
            assertEquals(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE, auto)
            assertEquals(
                "git.mergeProductFlowResolve",
                GitMergeProductFlowPresentation.productFlowHintKey(
                    merging = true,
                    conflicts = 1,
                    selectedConflict = true,
                ),
                "§B auto-select conflict.json should show resolve walkthrough hint",
            )
            assertFalse(
                GitMergeProductFlowPresentation.mergeContinueActionEnabled(
                    merging = status.merging,
                    conflicts = status.conflicts,
                    busy = false,
                ),
            )
            assertFalse(
                GitVaultRemotePresentation.pushActionEnabled(
                    persistedRemote = status.remote,
                    merging = status.merging,
                    busy = false,
                    conflicts = status.conflicts,
                ),
                "push must stay disabled during merge §B while conflicts remain",
            )
            assertFalse(
                GitVaultRemotePresentation.pullActionEnabled(
                    persistedRemote = status.remote,
                    merging = status.merging,
                    busy = false,
                    conflicts = status.conflicts,
                ),
            )
            assertFalse(
                GitOperationPresentation.commitActionEnabled(
                    busy = false,
                    merging = status.merging,
                    conflicts = status.conflicts,
                    hasChanges = status.changes.isNotEmpty(),
                    messageTrimmed = "evidence commit",
                ),
                "commit must stay disabled during merge §B while conflicts remain",
            )
            assertTrue(
                GitVaultRemotePresentation.fetchActionEnabled(status.remote, busy = false),
                "fetch stays enabled during merge §B (Electron VaultGitDialog)",
            )
            val fetched = GitEngine.fetch(local, isolateConfig = true)
            assertTrue(fetched.success, fetched.message)

            val resolved = GitEngine.resolveConflict(
                local,
                GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE,
                "theirs",
                isolateConfig = true,
            )
            assertTrue(resolved.success, resolved.message)
            val afterResolve = GitEngine.status(local, isolateConfig = true)
            assertEquals(0, afterResolve.conflicts)
            assertTrue(
                GitMergeProductFlowPresentation.mergeContinueActionEnabled(
                    merging = afterResolve.merging,
                    conflicts = afterResolve.conflicts,
                    busy = false,
                ),
            )
            val continued = GitEngine.continueOperation(local, identity, isolateConfig = true)
            assertTrue(continued.success, continued.message)
            val done = GitEngine.status(local, isolateConfig = true)
            assertEquals("none", done.operation)
            assertFalse(done.merging)
            assertTrue(
                GitVaultRemotePresentation.pushActionEnabled(
                    persistedRemote = done.remote,
                    merging = done.merging,
                    busy = false,
                    conflicts = done.conflicts,
                ),
                "push re-enabled after merge completes",
            )
            assertTrue(
                GitVaultRemotePresentation.pullActionEnabled(
                    persistedRemote = done.remote,
                    merging = done.merging,
                    busy = false,
                    conflicts = done.conflicts,
                ),
                "pull re-enabled after merge completes",
            )
            assertTrue(local.resolve(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE).readText().contains("remote"))
        } finally {
            bare.toFile().deleteRecursively()
            upstream.toFile().deleteRecursively()
            local.toFile().deleteRecursively()
        }
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
