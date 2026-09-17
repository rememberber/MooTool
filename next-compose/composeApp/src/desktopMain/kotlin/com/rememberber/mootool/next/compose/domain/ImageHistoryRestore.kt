package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord

/** F23 图片历史恢复：输出路径与库内资源名。 */
object ImageHistoryRestore {
    data class RestorePlan(
        val lastOutputs: List<String>,
        val preferredAsset: String?,
    )

    fun plan(item: HistoryRecord, availableAssetNames: Set<String>): RestorePlan {
        val outputs = item.output.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val preferred = resolvePreferredAsset(outputs, item.input, availableAssetNames)
        return RestorePlan(lastOutputs = outputs, preferredAsset = preferred)
    }

    fun resolvePreferredAsset(outputs: List<String>, input: String, availableAssetNames: Set<String>): String? {
        val candidates = (outputs + input.split(',', '，', '\n').map { it.trim() }.filter { it.isNotEmpty() })
        return candidates.firstOrNull { name -> availableAssetNames.contains(name) }
    }
}
