package com.rememberber.mootool.next.compose.domain

/**
 * Vault Git merge 产品主窗 §B 走查：冲突文件选中、ours/theirs、继续合并。
 * 与 `prepare-git-merge-conflict-evidence.sh` / `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge` 一致。
 */
object GitMergeProductFlowPresentation {
    const val EVIDENCE_CONFLICT_FILE = "conflict.json"

    fun autoSelectConflictPath(
        merging: Boolean,
        conflicts: Int,
        changes: List<Pair<String, Boolean>>,
        currentSelected: String,
    ): String? {
        if (!merging || conflicts <= 0) return null
        val conflictPaths = changes.filter { it.second }.map { it.first }
        if (currentSelected.isNotBlank() && conflictPaths.contains(currentSelected)) return null
        return conflictPaths.firstOrNull()
    }

    fun showResolveActions(merging: Boolean, selectedConflict: Boolean): Boolean =
        merging && selectedConflict

    fun productFlowHintKey(merging: Boolean, conflicts: Int, selectedConflict: Boolean): String? {
        if (!merging || conflicts <= 0) return null
        return if (selectedConflict) "git.mergeProductFlowResolve" else "git.mergeProductFlowSelect"
    }

    fun evidenceReady(merging: Boolean, conflicts: Int, unmergedPaths: List<String>): Boolean =
        merging && conflicts > 0 && unmergedPaths.isNotEmpty()

    fun showProductFlowPanel(merging: Boolean, conflicts: Int): Boolean =
        merging && conflicts > 0

    fun resolveActionsEnabled(
        merging: Boolean,
        selectedConflict: Boolean,
        busy: Boolean,
    ): Boolean = !busy && showResolveActions(merging, selectedConflict)
}
