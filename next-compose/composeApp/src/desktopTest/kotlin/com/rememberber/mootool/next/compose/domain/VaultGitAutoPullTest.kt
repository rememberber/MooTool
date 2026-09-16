package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultGitAutoPullTest {
    private fun status(
        changes: Int = 0,
        conflicts: Int = 0,
        merging: Boolean = false,
        remote: String = "https://example.com/r.git",
    ) = GitStatus(
        available = true,
        repository = true,
        remote = remote,
        changes = List(changes) { GitChange(path = "file$it.json", status = "M", conflict = false) },
        conflicts = conflicts,
        merging = merging,
    )

    @Test
    fun allowsPullWhenRepositoryClean() {
        assertTrue(VaultGitAutoPull.mayPullCleanWorkingTree(status()))
    }

    @Test
    fun skipsWhenLocalChangesOrConflictOrMerge() {
        assertFalse(VaultGitAutoPull.mayPullCleanWorkingTree(status(changes = 1)))
        assertFalse(VaultGitAutoPull.mayPullCleanWorkingTree(status(conflicts = 1)))
        assertFalse(VaultGitAutoPull.mayPullCleanWorkingTree(status(merging = true)))
        assertFalse(VaultGitAutoPull.mayPullCleanWorkingTree(status(remote = "")))
    }

    @Test
    fun notifiesVaultAfterPullOnSuccessOrMergeState() {
        val clean = status()
        assertTrue(VaultGitAutoPull.shouldNotifyVaultAfterPull(GitActionResult(true, "ok"), clean))
        assertTrue(
            VaultGitAutoPull.shouldNotifyVaultAfterPull(
                GitActionResult(false, "failed"),
                status(merging = true),
            ),
        )
        assertFalse(
            VaultGitAutoPull.shouldNotifyVaultAfterPull(
                GitActionResult(false, "failed"),
                clean,
            ),
        )
    }
}
