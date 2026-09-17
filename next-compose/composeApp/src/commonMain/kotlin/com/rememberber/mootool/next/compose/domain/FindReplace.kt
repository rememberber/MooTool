package com.rememberber.mootool.next.compose.domain

data class FindReplaceOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false
)

data class FindMatch(
    val start: Int,
    val end: Int
)

object FindReplace {
    fun findAll(content: String, query: String, options: FindReplaceOptions): List<FindMatch> {
        if (query.isEmpty()) return emptyList()
        val regex = buildRegex(query, options) ?: return emptyList()
        val matches = ArrayList<FindMatch>()
        var index = 0
        while (index <= content.length) {
            val match = regex.find(content, index) ?: break
            val text = match.value
            if (text.isEmpty()) {
                index = match.range.first + 1
                continue
            }
            matches += FindMatch(match.range.first, match.range.first + text.length)
            index = match.range.first + text.length
        }
        return matches
    }

    fun findNext(
        content: String,
        query: String,
        options: FindReplaceOptions,
        fromIndex: Int,
        forward: Boolean
    ): FindMatch? {
        val matches = findAll(content, query, options)
        if (matches.isEmpty()) return null
        return if (forward) {
            matches.firstOrNull { it.start >= fromIndex } ?: matches.first()
        } else {
            matches.lastOrNull { it.end <= fromIndex } ?: matches.last()
        }
    }

    fun replaceCurrent(
        content: String,
        query: String,
        replacement: String,
        options: FindReplaceOptions,
        fromIndex: Int,
        selectionStart: Int = -1,
        selectionEnd: Int = -1,
    ): Pair<String, FindMatch?> {
        if (query.isEmpty()) return content to null
        val pattern = buildRegex(query, options) ?: return content to null
        val replacementTemplate = expandReplacement(replacement, options.regex)
        if (selectionEnd > selectionStart) {
            val selected = content.substring(selectionStart, selectionEnd)
            val inner = findAll(selected, query, options)
            if (inner.size == 1 && inner[0].start == 0 && inner[0].end == selected.length) {
                val replacedSlice = applyReplacement(selected, replacementTemplate, pattern, options.regex)
                val updated = content.replaceRange(selectionStart, selectionEnd, replacedSlice)
                return updated to FindMatch(selectionStart, selectionStart + replacedSlice.length)
            }
        }
        val match = findNext(content, query, options, fromIndex, true) ?: return content to null
        val slice = content.substring(match.start, match.end)
        val replacedSlice = applyReplacement(slice, replacementTemplate, pattern, options.regex)
        val updated = content.replaceRange(match.start, match.end, replacedSlice)
        return updated to FindMatch(match.start, match.start + replacedSlice.length)
    }

    fun replaceAll(
        content: String,
        query: String,
        replacement: String,
        options: FindReplaceOptions
    ): Pair<String, Int> {
        val pattern = buildRegex(query, options) ?: return content to 0
        val matches = findAll(content, query, options)
        if (matches.isEmpty()) return content to 0
        val replacementTemplate = expandReplacement(replacement, options.regex)
        val builder = StringBuilder()
        var cursor = 0
        for (match in matches) {
            builder.append(content, cursor, match.start)
            val slice = content.substring(match.start, match.end)
            builder.append(applyReplacement(slice, replacementTemplate, pattern, options.regex))
            cursor = match.end
        }
        builder.append(content, cursor, content.length)
        return builder.toString() to matches.size
    }

    /** 对照 Electron `applyReplacement`：正则模式支持 `$1`/`$&`；非正则按字面替换。 */
    private fun applyReplacement(matchedText: String, replaceWith: String, pattern: Regex, regex: Boolean): String {
        if (matchedText.isEmpty()) return matchedText
        if (!regex) return replaceWith
        return pattern.replace(matchedText) { match -> substituteReplacementGroups(replaceWith, match) }
    }

    private fun substituteReplacementGroups(template: String, match: MatchResult): String =
        buildString(template.length) {
            var index = 0
            while (index < template.length) {
                if (template[index] == '$' && index + 1 < template.length) {
                    when (val marker = template[index + 1]) {
                        '&' -> {
                            append(match.value)
                            index += 2
                        }
                        in '0'..'9' -> {
                            var end = index + 2
                            while (end < template.length && template[end].isDigit()) end += 1
                            val group = template.substring(index + 1, end).toIntOrNull() ?: 0
                            append(match.groupValues.getOrElse(group) { "" })
                            index = end
                        }
                        else -> {
                            append(template[index])
                            index += 1
                        }
                    }
                } else {
                    append(template[index])
                    index += 1
                }
            }
        }

    private fun buildRegex(query: String, options: FindReplaceOptions): Regex? {
        val source = if (options.regex) query else Regex.escape(query)
        val wrapped = if (options.wholeWord) "\\b(?:$source)\\b" else source
        val flags = buildSet {
            if (!options.matchCase) add(RegexOption.IGNORE_CASE)
        }
        return runCatching { Regex(wrapped, flags) }.getOrNull()
    }

    private fun expandReplacement(value: String, regex: Boolean): String {
        if (!regex) return value
        return buildString(value.length) {
            var index = 0
            while (index < value.length) {
                val current = value[index]
                if (current == '\\' && index + 1 < value.length) {
                    when (value[index + 1]) {
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        '\\' -> append('\\')
                        else -> {
                            append(current)
                            append(value[index + 1])
                        }
                    }
                    index += 2
                } else {
                    append(current)
                    index += 1
                }
            }
        }
    }
}
