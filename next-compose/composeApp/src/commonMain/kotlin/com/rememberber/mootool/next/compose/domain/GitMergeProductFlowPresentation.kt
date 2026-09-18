package com.rememberber.mootool.next.compose.domain

/**
 * Vault Git merge 产品主窗 §B 走查：冲突文件选中、ours/theirs、继续合并。
 * 与 `prepare-git-merge-conflict-evidence.sh` / `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge` 一致。
 */
object GitMergeProductFlowPresentation {
    const val EVIDENCE_CONFLICT_FILE = "conflict.json"

    /** 多文件冲突时优先选中证据脚本产物 `conflict.json`，便于 §B 产品走查稳定。 */
    fun preferredConflictSelectionPath(conflictPaths: List<String>): String? {
        if (conflictPaths.isEmpty()) return null
        if (conflictPaths.contains(EVIDENCE_CONFLICT_FILE)) return EVIDENCE_CONFLICT_FILE
        return conflictPaths.first()
    }

    fun autoSelectConflictPath(
        merging: Boolean,
        conflicts: Int,
        changes: List<Pair<String, Boolean>>,
        currentSelected: String,
    ): String? {
        if (!merging || conflicts <= 0) return null
        val conflictPaths = changes.filter { it.second }.map { it.first }
        if (currentSelected.isNotBlank() && conflictPaths.contains(currentSelected)) return null
        return preferredConflictSelectionPath(conflictPaths)
    }

    fun showResolveActions(merging: Boolean, selectedConflict: Boolean): Boolean =
        merging && selectedConflict

    fun productFlowHintKey(
        merging: Boolean,
        conflicts: Int,
        selectedConflict: Boolean,
        operation: String = "merge",
    ): String? {
        if (!merging || conflicts <= 0) return null
        return if (operation == "rebase") {
            if (selectedConflict) "git.rebaseProductFlowResolve" else "git.rebaseProductFlowSelect"
        } else {
            if (selectedConflict) "git.mergeProductFlowResolve" else "git.mergeProductFlowSelect"
        }
    }

    fun evidenceReady(merging: Boolean, conflicts: Int, unmergedPaths: List<String>): Boolean =
        merging && conflicts > 0 && unmergedPaths.isNotEmpty()

    fun showProductFlowPanel(merging: Boolean, conflicts: Int): Boolean =
        merging && conflicts > 0

    fun resolveActionsEnabled(
        merging: Boolean,
        selectedConflict: Boolean,
        busy: Boolean,
    ): Boolean =
        GitOperationPresentation.resolveConflictActionEnabled(busy) &&
            showResolveActions(merging, selectedConflict)

    /** 产品主窗 merge/rebase 走查：全部冲突已标记后继续（§B continue 钮）。 */
    fun mergeContinueActionEnabled(merging: Boolean, conflicts: Int, busy: Boolean): Boolean =
        GitOperationPresentation.continueActionEnabled(busy, merging, conflicts)
}
