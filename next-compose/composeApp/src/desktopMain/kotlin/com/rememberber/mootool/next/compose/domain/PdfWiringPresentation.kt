package com.rememberber.mootool.next.compose.domain

import java.nio.file.Path

/** F24 任务列表/拆分合并守卫（可单测，对齐 Electron PDF 工具栏启用条件）。 */
object PdfWiringPresentation {
    fun taskCount(tab: PdfTab, splitCount: Int, mergeCount: Int): Int =
        when (tab) {
            PdfTab.Split -> splitCount
            PdfTab.Merge -> mergeCount
        }

    fun limitReached(tab: PdfTab, splitCount: Int, mergeCount: Int): Boolean =
        taskCount(tab, splitCount, mergeCount) >= PdfEngine.MAX_TASKS

    fun canAddTask(busy: Boolean, tab: PdfTab, splitCount: Int, mergeCount: Int): Boolean =
        !busy && !limitReached(tab, splitCount, mergeCount)

    fun ingestRemainingSlots(tab: PdfTab, splitCount: Int, mergeCount: Int): Int =
        (PdfEngine.MAX_TASKS - taskCount(tab, splitCount, mergeCount)).coerceAtLeast(0)

    fun canStartSplit(busy: Boolean, selectedCount: Int): Boolean = !busy && selectedCount > 0

    fun canStartMerge(busy: Boolean, selectedCount: Int): Boolean = !busy && selectedCount >= 2

    sealed interface InspectOutcome {
        data class Success(val info: PdfFileInfo) : InspectOutcome
        data class Failure(val error: Throwable) : InspectOutcome
    }

    fun inspectFile(path: Path): InspectOutcome =
        runCatching { PdfEngine.inspect(path) }.fold(
            onSuccess = { InspectOutcome.Success(it) },
            onFailure = { InspectOutcome.Failure(it) },
        )

    sealed interface JobOutcome {
        data class Success(val value: PdfOperationResult) : JobOutcome
        data class Failure(val error: Throwable) : JobOutcome
    }

    fun runSplit(
        tasks: List<PdfEngine.SplitTask>,
        cancelled: () -> Boolean,
    ): JobOutcome =
        runCatching { PdfEngine.split(tasks, cancelled) }.fold(
            onSuccess = { JobOutcome.Success(it) },
            onFailure = { JobOutcome.Failure(it) },
        )

    fun runMerge(
        sources: List<PdfEngine.MergeSource>,
        output: Path,
        cancelled: () -> Boolean,
    ): JobOutcome =
        runCatching { PdfEngine.merge(sources, output, cancelled) }.fold(
            onSuccess = { JobOutcome.Success(it) },
            onFailure = { JobOutcome.Failure(it) },
        )
}
