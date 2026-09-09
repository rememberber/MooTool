package com.rememberber.mootool.next.compose.domain

import com.fasterxml.jackson.databind.ObjectMapper
import java.text.Collator
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

enum class QuickReplaceAction {
    Trim,
    RemoveBlankLines,
    RemoveTabs,
    ScientificToNormal,
    NormalToScientific,
    ThousandsToNormal,
    NormalToThousands,
    UnderscoreToCamel,
    CamelToUnderscore,
    Uppercase,
    Lowercase,
    LinesToComma,
    LinesToSingleQuoted,
    LinesToDoubleQuoted,
    CommaToLines,
    TabsToLines,
    ClearNewlines,
    DeduplicateLines,
    DeduplicateWithCount,
    Escape,
    Unescape,
    ReverseLines,
    SortAscending,
    SortDescending
}

object QuickReplaceEngine {
    val actions: List<QuickReplaceAction> = QuickReplaceAction.entries
    private val numberPattern = Regex("[-+]?(?:\\d[\\d,]*\\.?\\d*|\\.\\d+)(?:e[-+]?\\d+)?", RegexOption.IGNORE_CASE)
    private val thousandsPattern = Regex("(?<=\\d),(?=\\d{3}(?:\\D|$))")
    private val collator: Collator = Collator.getInstance(Locale.ROOT)
    private val json = ObjectMapper()

    fun parse(id: String): QuickReplaceAction =
        actions.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: error("Unknown quick replace action: $id")

    fun run(input: String, action: QuickReplaceAction): String {
        val lines = normalizeLines(input)
        return when (action) {
            QuickReplaceAction.Trim -> lines.joinToString("\n") { it.trim() }
            QuickReplaceAction.RemoveBlankLines -> lines.filter { it.trim().isNotEmpty() }.joinToString("\n")
            QuickReplaceAction.RemoveTabs -> input.replace("\t", "")
            QuickReplaceAction.ScientificToNormal -> replaceNumbers(input) { scientificToNormal(it) }
            QuickReplaceAction.NormalToScientific -> replaceNumbers(input) { toExponential(it.replace(",", "").toDouble()) }
            QuickReplaceAction.ThousandsToNormal -> thousandsPattern.replace(input, "")
            QuickReplaceAction.NormalToThousands -> replaceNumbers(input) { addThousandsSeparators(it) }
            QuickReplaceAction.UnderscoreToCamel ->
                Regex("_([a-zA-Z0-9])").replace(input) { match -> match.groupValues[1].uppercase(Locale.ROOT) }
            QuickReplaceAction.CamelToUnderscore ->
                Regex("([a-z0-9])([A-Z])").replace(input, "$1_$2").lowercase(Locale.ROOT)
            QuickReplaceAction.Uppercase -> input.uppercase(Locale.getDefault())
            QuickReplaceAction.Lowercase -> input.lowercase(Locale.getDefault())
            QuickReplaceAction.LinesToComma -> nonEmptyLines(lines).joinToString(",")
            QuickReplaceAction.LinesToSingleQuoted ->
                nonEmptyLines(lines).joinToString(",") { "'${it.replace("'", "\\'")}'" }
            QuickReplaceAction.LinesToDoubleQuoted ->
                nonEmptyLines(lines).joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
            QuickReplaceAction.CommaToLines ->
                input.split(',').map { unquote(it.trim()) }.filter { it.isNotEmpty() }.joinToString("\n")
            QuickReplaceAction.TabsToLines -> input.split('\t').joinToString("\n")
            QuickReplaceAction.ClearNewlines -> lines.joinToString("")
            QuickReplaceAction.DeduplicateLines -> LinkedHashSet(lines).joinToString("\n")
            QuickReplaceAction.DeduplicateWithCount -> {
                val counts = LinkedHashMap<String, Int>()
                for (line in lines) counts[line] = (counts[line] ?: 0) + 1
                counts.entries.joinToString("\n") { "${it.key}\t${it.value}" }
            }
            QuickReplaceAction.Escape -> jsonEscape(input)
            QuickReplaceAction.Unescape -> jsonUnescape(input)
            QuickReplaceAction.ReverseLines -> lines.asReversed().joinToString("\n")
            QuickReplaceAction.SortAscending -> lines.sortedWith { left, right -> collator.compare(left, right) }.joinToString("\n")
            QuickReplaceAction.SortDescending -> lines.sortedWith { left, right -> collator.compare(right, left) }.joinToString("\n")
        }
    }

    fun stats(content: String): Triple<Int, Int, Int> {
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        val trimmed = content.trim()
        val words = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
        val lines = if (normalized.isEmpty()) 0 else normalized.split('\n').size
        return Triple(content.length, words, lines)
    }

    private fun normalizeLines(value: String): List<String> =
        value.replace("\r\n", "\n").replace('\r', '\n').split('\n')

    private fun nonEmptyLines(lines: List<String>): List<String> =
        lines.map { it.trim() }.filter { it.isNotEmpty() }

    private fun replaceNumbers(input: String, transform: (String) -> String): String =
        numberPattern.replace(input) { match ->
            val value = match.value
            val parsed = value.replace(",", "").toDoubleOrNull()
            if (parsed != null && parsed.isFinite()) transform(value) else value
        }

    private fun scientificToNormal(value: String): String {
        val normalized = value.replace(",", "")
        if (!normalized.contains('e', ignoreCase = true)) return normalized
        val parts = normalized.lowercase(Locale.ROOT).split('e', limit = 2)
        val coefficient = parts[0]
        val exponent = parts.getOrNull(1)?.toIntOrNull() ?: return normalized
        val negative = coefficient.startsWith('-')
        val unsigned = coefficient.removePrefix("-").removePrefix("+")
        val split = unsigned.split('.', limit = 2)
        val integer = split[0]
        val fraction = split.getOrNull(1).orEmpty()
        val digits = integer + fraction
        val decimalIndex = integer.length + exponent
        val result = when {
            decimalIndex <= 0 -> "0." + "0".repeat(-decimalIndex) + digits
            decimalIndex >= digits.length -> digits + "0".repeat(decimalIndex - digits.length)
            else -> digits.substring(0, decimalIndex) + "." + digits.substring(decimalIndex)
        }
        return if (negative) "-$result" else result
    }

    private fun addThousandsSeparators(value: String): String {
        val normalized = value.replace(",", "")
        if (normalized.contains('e', ignoreCase = true)) return normalized
        val parts = normalized.split('.', limit = 2)
        val integer = parts[0]
        val fraction = parts.getOrNull(1)
        val sign = if (integer.startsWith('-') || integer.startsWith('+')) integer.first().toString() else ""
        val digits = if (sign.isEmpty()) integer else integer.substring(1)
        val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
        return sign + grouped + if (fraction == null) "" else ".$fraction"
    }

    private fun toExponential(n: Double): String {
        if (n == 0.0) return "0e+0"
        val sign = if (n < 0) "-" else ""
        val abs = abs(n)
        var exp = floor(log10(abs)).toInt()
        var mantissa = abs / 10.0.pow(exp.toDouble())
        if (mantissa >= 10.0) {
            mantissa /= 10.0
            exp += 1
        }
        val mantissaText = mantissa.toString().trimEnd('0').trimEnd('.')
        val expSign = if (exp >= 0) "+" else ""
        return "$sign${mantissaText}e$expSign$exp"
    }

    private fun unquote(value: String): String {
        if (value.length >= 2 &&
            ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith('\'') && value.endsWith('\'')))
        ) {
            return value.substring(1, value.length - 1)
        }
        return value
    }

    private fun jsonEscape(input: String): String = buildString {
        for (ch in input) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
    }

    private fun jsonUnescape(input: String): String =
        json.readValue("\"" + input.replace("\"", "\\\"") + "\"", String::class.java)
}
