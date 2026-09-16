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
        if (selectionEnd > selectionStart) {
            val selected = content.substring(selectionStart, selectionEnd)
            val inner = findAll(selected, query, options)
            if (inner.size == 1 && inner[0].start == 0 && inner[0].end == selected.length) {
                val expanded = expandReplacement(replacement, options.regex)
                val updated = content.replaceRange(selectionStart, selectionEnd, expanded)
                return updated to FindMatch(selectionStart, selectionStart + expanded.length)
            }
        }
        val match = findNext(content, query, options, fromIndex, true) ?: return content to null
        val expanded = expandReplacement(replacement, options.regex)
        val updated = content.replaceRange(match.start, match.end, expanded)
        return updated to FindMatch(match.start, match.start + expanded.length)
    }

    fun replaceAll(
        content: String,
        query: String,
        replacement: String,
        options: FindReplaceOptions
    ): Pair<String, Int> {
        val matches = findAll(content, query, options)
        if (matches.isEmpty()) return content to 0
        val expanded = expandReplacement(replacement, options.regex)
        val builder = StringBuilder()
        var cursor = 0
        for (match in matches) {
            builder.append(content, cursor, match.start)
            builder.append(expanded)
            cursor = match.end
        }
        builder.append(content, cursor, content.length)
        return builder.toString() to matches.size
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
