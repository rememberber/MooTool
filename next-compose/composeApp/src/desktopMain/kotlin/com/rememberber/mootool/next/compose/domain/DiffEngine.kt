package com.rememberber.mootool.next.compose.domain

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import kotlinx.serialization.Serializable

@Serializable
enum class DiffSegmentType { Insert, Delete, Change }

@Serializable
enum class UnifiedSpanType {
    AddLine, DeleteLine, HunkLine, HeaderLine, AddCharacter, DeleteCharacter, ChangeCharacter
}

@Serializable
data class DiffSegment(
    val type: DiffSegmentType,
    val leftStart: Int,
    val leftEnd: Int,
    val rightStart: Int,
    val rightEnd: Int,
    val wholeLine: Boolean
)

@Serializable
data class UnifiedSpan(
    val start: Int,
    val end: Int,
    val type: UnifiedSpanType
)

@Serializable
data class UnifiedDiffView(
    val text: String,
    val lineSpans: List<UnifiedSpan>,
    val characterSpans: List<UnifiedSpan>,
    val characterEventCount: Int
)

@Serializable
data class DiffResult(
    val leftText: String,
    val rightText: String,
    val segments: List<DiffSegment>,
    val unified: String,
    val unifiedView: UnifiedDiffView,
    val added: Int,
    val removed: Int,
    val changed: Int
)

private data class PatchDelta<T>(
    val type: DiffSegmentType,
    val sourcePosition: Int,
    val targetPosition: Int,
    val source: List<T>,
    val target: List<T>
)

object DiffEngine {
    fun compare(left: String, right: String, ignoreWhitespace: Boolean): DiffResult {
        val leftLines = splitLines(left)
        val rightLines = splitLines(right)
        val patch = buildPatch(leftLines, rightLines)
        val segments = buildUiSegments(left, right, patch, ignoreWhitespace)
        val unifiedView = buildUnifiedView(patch, leftLines, ignoreWhitespace)
        var added = 0
        var removed = 0
        var changed = 0
        for (delta in patch) {
            when (delta.type) {
                DiffSegmentType.Insert -> added += delta.target.size
                DiffSegmentType.Delete -> removed += delta.source.size
                DiffSegmentType.Change -> {
                    val paired = minOf(delta.source.size, delta.target.size)
                    changed += paired
                    removed += maxOf(0, delta.source.size - paired)
                    added += maxOf(0, delta.target.size - paired)
                }
            }
        }
        return DiffResult(left, right, segments, unifiedView.text, unifiedView, added, removed, changed)
    }

    internal fun splitLines(text: String): List<String> =
        text.split(Regex("\\r\\n|[\\n\\u000B\\u000C\\r\\u0085\\u2028\\u2029]"))

    private fun <T> buildPatch(source: List<T>, target: List<T>): List<PatchDelta<T>> {
        return DiffUtils.diff(source, target).deltas.map { delta ->
            val type = when (delta.type) {
                DeltaType.INSERT -> DiffSegmentType.Insert
                DeltaType.DELETE -> DiffSegmentType.Delete
                else -> DiffSegmentType.Change
            }
            PatchDelta(type, delta.source.position, delta.target.position, delta.source.lines, delta.target.lines)
        }
    }

    private fun buildUiSegments(
        left: String,
        right: String,
        patch: List<PatchDelta<String>>,
        ignoreWhitespace: Boolean
    ): List<DiffSegment> {
        val leftLineStarts = computeLineStartOffsets(left)
        val rightLineStarts = computeLineStartOffsets(right)
        val segments = ArrayList<DiffSegment>()
        for (delta in patch) {
            when (delta.type) {
                DiffSegmentType.Delete -> delta.source.forEachIndexed { index, line ->
                    if (ignoreWhitespace && isAllWhitespace(line)) return@forEachIndexed
                    val start = safeLineStart(leftLineStarts, delta.sourcePosition + index)
                    segments += DiffSegment(DiffSegmentType.Delete, start, start + line.length, -1, -1, true)
                }
                DiffSegmentType.Insert -> delta.target.forEachIndexed { index, line ->
                    if (ignoreWhitespace && isAllWhitespace(line)) return@forEachIndexed
                    val start = safeLineStart(rightLineStarts, delta.targetPosition + index)
                    segments += DiffSegment(DiffSegmentType.Insert, -1, -1, start, start + line.length, true)
                }
                DiffSegmentType.Change -> {
                    val paired = minOf(delta.source.size, delta.target.size)
                    for (index in 0 until paired) {
                        val leftLine = delta.source[index]
                        val rightLine = delta.target[index]
                        val leftLineStart = safeLineStart(leftLineStarts, delta.sourcePosition + index)
                        val rightLineStart = safeLineStart(rightLineStarts, delta.targetPosition + index)
                        for (characterDelta in buildPatch(leftLine.toList(), rightLine.toList())) {
                            val leftStart = leftLineStart + characterDelta.sourcePosition
                            val leftEnd = leftStart + characterDelta.source.size
                            val rightStart = rightLineStart + characterDelta.targetPosition
                            val rightEnd = rightStart + characterDelta.target.size
                            val before = safeSlice(leftLine, characterDelta.sourcePosition, characterDelta.source.size)
                            val after = safeSlice(rightLine, characterDelta.targetPosition, characterDelta.target.size)
                            when (characterDelta.type) {
                                DiffSegmentType.Delete -> if (!ignoreWhitespace || !isAllWhitespace(before)) {
                                    segments += DiffSegment(DiffSegmentType.Delete, leftStart, leftEnd, -1, -1, false)
                                }
                                DiffSegmentType.Insert -> if (!ignoreWhitespace || !isAllWhitespace(after)) {
                                    segments += DiffSegment(DiffSegmentType.Insert, -1, -1, rightStart, rightEnd, false)
                                }
                                DiffSegmentType.Change -> if (!ignoreWhitespace || !equalsIgnoringWhitespace(before, after)) {
                                    segments += DiffSegment(DiffSegmentType.Change, leftStart, leftEnd, rightStart, rightEnd, false)
                                }
                            }
                        }
                    }
                    for (index in paired until delta.source.size) {
                        val line = delta.source[index]
                        if (ignoreWhitespace && isAllWhitespace(line)) continue
                        val start = safeLineStart(leftLineStarts, delta.sourcePosition + index)
                        segments += DiffSegment(DiffSegmentType.Delete, start, start + line.length, -1, -1, true)
                    }
                    for (index in paired until delta.target.size) {
                        val line = delta.target[index]
                        if (ignoreWhitespace && isAllWhitespace(line)) continue
                        val start = safeLineStart(rightLineStarts, delta.targetPosition + index)
                        segments += DiffSegment(DiffSegmentType.Insert, -1, -1, start, start + line.length, true)
                    }
                }
            }
        }
        return segments
    }

    private fun buildUnifiedView(
        patch: List<PatchDelta<String>>,
        originalLines: List<String>,
        ignoreWhitespace: Boolean
    ): UnifiedDiffView {
        val lines = generateUnifiedDiffLines(originalLines, patch, 3)
        val text = lines.joinToString("\n")
        val lineSpans = ArrayList<UnifiedSpan>()
        val characterSpans = ArrayList<UnifiedSpan>()
        var characterEventCount = 0
        var offset = 0
        val deletedStarts = ArrayList<Int>()
        val deletedTexts = ArrayList<String>()
        val addedStarts = ArrayList<Int>()
        val addedTexts = ArrayList<String>()

        fun flushCharacterSpans() {
            characterEventCount += addIntralineSpans(deletedStarts, deletedTexts, addedStarts, addedTexts, characterSpans, ignoreWhitespace)
            deletedStarts.clear()
            deletedTexts.clear()
            addedStarts.clear()
            addedTexts.clear()
        }

        lines.forEachIndexed { index, line ->
            val lineStart = offset
            val lineEnd = lineStart + line.length
            when {
                line.startsWith("@@") -> {
                    flushCharacterSpans()
                    lineSpans += UnifiedSpan(lineStart, lineEnd, UnifiedSpanType.HunkLine)
                }
                line.startsWith("---") || line.startsWith("+++") -> {
                    lineSpans += UnifiedSpan(lineStart, lineEnd, UnifiedSpanType.HeaderLine)
                }
                line.startsWith("+") -> {
                    val content = line.substring(1)
                    if (!ignoreWhitespace || !isAllWhitespace(content)) {
                        lineSpans += UnifiedSpan(lineStart, lineEnd, UnifiedSpanType.AddLine)
                    }
                    addedStarts += lineStart
                    addedTexts += content
                }
                line.startsWith("-") -> {
                    val content = line.substring(1)
                    if (!ignoreWhitespace || !isAllWhitespace(content)) {
                        lineSpans += UnifiedSpan(lineStart, lineEnd, UnifiedSpanType.DeleteLine)
                    }
                    deletedStarts += lineStart
                    deletedTexts += content
                }
            }
            offset = if (index < lines.lastIndex) lineEnd + 1 else lineEnd
        }
        flushCharacterSpans()
        return UnifiedDiffView(text, lineSpans, characterSpans, characterEventCount)
    }

    private fun generateUnifiedDiffLines(
        originalLines: List<String>,
        patch: List<PatchDelta<String>>,
        contextSize: Int
    ): List<String> {
        if (patch.isEmpty()) return emptyList()
        val lines = ArrayList<String>()
        lines += "--- old"
        lines += "+++ new"
        var currentGroup = arrayListOf(patch[0])
        var previous = patch[0]
        for (index in 1 until patch.size) {
            val next = patch[index]
            val closeEnough = previous.sourcePosition + previous.source.size + contextSize >= next.sourcePosition - contextSize
            if (closeEnough) currentGroup += next
            else {
                lines += processUnifiedGroup(originalLines, currentGroup, contextSize)
                currentGroup = arrayListOf(next)
            }
            previous = next
        }
        lines += processUnifiedGroup(originalLines, currentGroup, contextSize)
        return lines
    }

    private fun processUnifiedGroup(
        originalLines: List<String>,
        deltas: List<PatchDelta<String>>,
        contextSize: Int
    ): List<String> {
        val output = ArrayList<String>()
        var originalTotal = 0
        var revisedTotal = 0
        var current = deltas[0]
        val originalStart = maxOf(1, current.sourcePosition + 1 - contextSize)
        val revisedStart = maxOf(1, current.targetPosition + 1 - contextSize)
        val contextStart = maxOf(0, current.sourcePosition - contextSize)
        for (line in contextStart until current.sourcePosition) {
            output += " ${originalLines[line]}"
            originalTotal += 1
            revisedTotal += 1
        }
        output += deltaText(current)
        originalTotal += current.source.size
        revisedTotal += current.target.size
        for (index in 1 until deltas.size) {
            val next = deltas[index]
            val intermediateStart = current.sourcePosition + current.source.size
            for (line in intermediateStart until next.sourcePosition) {
                output += " ${originalLines[line]}"
                originalTotal += 1
                revisedTotal += 1
            }
            output += deltaText(next)
            originalTotal += next.source.size
            revisedTotal += next.target.size
            current = next
        }
        val trailingStart = current.sourcePosition + current.source.size
        val trailingEnd = minOf(originalLines.size, trailingStart + contextSize)
        for (line in trailingStart until trailingEnd) {
            output += " ${originalLines[line]}"
            originalTotal += 1
            revisedTotal += 1
        }
        output.add(0, "@@ -$originalStart,$originalTotal +$revisedStart,$revisedTotal @@")
        return output
    }

    private fun deltaText(delta: PatchDelta<String>): List<String> =
        delta.source.map { "-$it" } + delta.target.map { "+$it" }

    private fun addIntralineSpans(
        deletedStarts: List<Int>,
        deletedTexts: List<String>,
        addedStarts: List<Int>,
        addedTexts: List<String>,
        output: MutableList<UnifiedSpan>,
        ignoreWhitespace: Boolean
    ): Int {
        var events = 0
        val pairs = minOf(deletedTexts.size, addedTexts.size)
        for (index in 0 until pairs) {
            val before = deletedTexts[index]
            val after = addedTexts[index]
            val beforeBase = deletedStarts[index] + 1
            val afterBase = addedStarts[index] + 1
            for (delta in buildPatch(before.toList(), after.toList())) {
                val beforeText = safeSlice(before, delta.sourcePosition, delta.source.size)
                val afterText = safeSlice(after, delta.targetPosition, delta.target.size)
                when (delta.type) {
                    DiffSegmentType.Delete -> {
                        if (ignoreWhitespace && isAllWhitespace(beforeText)) continue
                        output += UnifiedSpan(beforeBase + delta.sourcePosition, beforeBase + delta.sourcePosition + delta.source.size, UnifiedSpanType.DeleteCharacter)
                    }
                    DiffSegmentType.Insert -> {
                        if (ignoreWhitespace && isAllWhitespace(afterText)) continue
                        output += UnifiedSpan(afterBase + delta.targetPosition, afterBase + delta.targetPosition + delta.target.size, UnifiedSpanType.AddCharacter)
                    }
                    DiffSegmentType.Change -> {
                        if (ignoreWhitespace && equalsIgnoringWhitespace(beforeText, afterText)) continue
                        if (beforeText.isNotEmpty()) {
                            output += UnifiedSpan(beforeBase + delta.sourcePosition, beforeBase + delta.sourcePosition + delta.source.size, UnifiedSpanType.ChangeCharacter)
                        }
                        if (afterText.isNotEmpty()) {
                            output += UnifiedSpan(afterBase + delta.targetPosition, afterBase + delta.targetPosition + delta.target.size, UnifiedSpanType.ChangeCharacter)
                        }
                    }
                }
                events += 1
            }
        }
        return events
    }

    private fun computeLineStartOffsets(text: String): List<Int> {
        val starts = ArrayList<Int>()
        starts += 0
        for (index in text.indices) {
            if (text[index] == '\n') starts += index + 1
        }
        return starts
    }

    private fun safeLineStart(starts: List<Int>, lineIndex: Int): Int {
        if (lineIndex < 0) return 0
        if (lineIndex >= starts.size) return starts.last()
        return starts[lineIndex]
    }

    private fun safeSlice(value: String, position: Int, length: Int): String {
        val start = position.coerceIn(0, value.length)
        val end = (position + length).coerceIn(start, value.length)
        return value.substring(start, end)
    }

    private fun isAllWhitespace(value: String): Boolean = value.all { Character.isWhitespace(it) }

    private fun equalsIgnoringWhitespace(left: String, right: String): Boolean {
        var leftIndex = 0
        var rightIndex = 0
        while (leftIndex < left.length && rightIndex < right.length) {
            val leftChar = left[leftIndex]
            val rightChar = right[rightIndex]
            if (Character.isWhitespace(leftChar)) {
                leftIndex += 1
                continue
            }
            if (Character.isWhitespace(rightChar)) {
                rightIndex += 1
                continue
            }
            if (leftChar != rightChar) return false
            leftIndex += 1
            rightIndex += 1
        }
        while (leftIndex < left.length) {
            if (!Character.isWhitespace(left[leftIndex])) return false
            leftIndex += 1
        }
        while (rightIndex < right.length) {
            if (!Character.isWhitespace(right[rightIndex])) return false
            rightIndex += 1
        }
        return true
    }
}
