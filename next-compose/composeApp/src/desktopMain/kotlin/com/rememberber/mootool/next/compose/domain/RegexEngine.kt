package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Serializable
data class RegexOptions(
    val global: Boolean = true,
    val ignoreCase: Boolean = false,
    val multiline: Boolean = false,
    val dotAll: Boolean = false
)

@Serializable
data class RegexMatch(
    val index: Int,
    val value: String,
    val groups: List<String>,
    val named: Map<String, String> = emptyMap()
)

@Serializable
data class RegexWorkerRequest(
    val pattern: String,
    val source: String,
    val options: RegexOptions = RegexOptions(),
    val maxMatches: Int = RegexEngine.DEFAULT_MAX_MATCHES
)

@Serializable
data class RegexWorkerResponse(
    val ok: Boolean,
    val matches: List<RegexMatch> = emptyList(),
    val code: String = "",
    val error: String = "",
    val engine: String = RegexEngine.ENGINE_NAME
)

class RegexException(val code: String, message: String) : RuntimeException(message)

data class CommonRegex(val id: String, val labelKey: String, val pattern: String)

object RegexEngine {
    const val ENGINE_NAME = "Java Pattern"
    const val DEFAULT_MAX_MATCHES = 10_000
    const val DEFAULT_TIMEOUT_MS = 2_000L

    val commonRegexes: List<CommonRegex> = listOf(
        CommonRegex("phone", "regex.common.phone", "1[3-9]\\d{9}"),
        CommonRegex("email", "regex.common.email", "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"),
        CommonRegex("domain", "regex.common.domain", "^((http:\\/\\/)|(https:\\/\\/))?([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(\\/)"),
        CommonRegex("ipv4", "regex.common.ipv4", "((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d))"),
        CommonRegex("account", "regex.common.account", "^[a-zA-Z][a-zA-Z0-9_]{4,15}$"),
        CommonRegex("htmlId", "regex.common.htmlId", "(?<=id=\")[\\s\\S]*?(?=\")"),
        CommonRegex("color", "regex.common.color", "#([a-fA-F0-9]{6})"),
        CommonRegex("jpg", "regex.common.jpg", "http[s:]{1,2}//[^\\s'\"<>]*?.jpg"),
        CommonRegex("magnet", "regex.common.magnet", "magnet:\\?xt=urn:btih:[0-9a-fA-F]{40,}"),
        CommonRegex("chinese", "regex.common.chinese", "^[\\u4e00-\\u9fa5]{0,}$"),
        CommonRegex("alnum", "regex.common.alnum", "^[A-Za-z0-9]+$"),
        CommonRegex("len3to20", "regex.common.len3to20", "^.{3,20}$"),
        CommonRegex("letters26", "regex.common.letters26", "^[A-Za-z]+$"),
        CommonRegex("wordUnderscore", "regex.common.wordUnderscore", "^\\w+$"),
        CommonRegex("cnEnNum", "regex.common.cnEnNum", "^[\\u4E00-\\u9FA5A-Za-z0-9_]+$"),
        CommonRegex("noSpecial", "regex.common.noSpecial", "[^%&',;=?\$\\x22]+"),
        CommonRegex("integer", "regex.common.integer", "^-?[1-9]\\d*$"),
        CommonRegex("positiveInt", "regex.common.positiveInt", "^[1-9]\\d*$"),
        CommonRegex("negativeInt", "regex.common.negativeInt", "^-[1-9]\\d*$"),
        CommonRegex("nonNegativeInt", "regex.common.nonNegativeInt", "^(?:[1-9]\\d*|0)$"),
        CommonRegex("float", "regex.common.float", "^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$")
    )

    fun match(pattern: String, source: String, options: RegexOptions, maxMatches: Int = DEFAULT_MAX_MATCHES): List<RegexMatch> {
        if (pattern.isEmpty()) return emptyList()
        val compiled = try {
            Pattern.compile(pattern, flags(options))
        } catch (error: PatternSyntaxException) {
            throw RegexException("invalid", error.description ?: error.message ?: "invalid")
        }
        val matcher = compiled.matcher(source)
        val results = ArrayList<RegexMatch>()
        var searchFrom = 0
        while (searchFrom <= source.length) {
            if (!matcher.find(searchFrom)) break
            if (results.size >= maxMatches) throw RegexException("limit", "limit")
            val named = namedGroups(matcher)
            val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
            results += RegexMatch(index = matcher.start(), value = matcher.group() ?: "", groups = groups, named = named)
            val end = matcher.end()
            searchFrom = if (end == matcher.start()) end + 1 else end
            if (!options.global) break
        }
        return results
    }

    fun flags(options: RegexOptions): Int {
        var flags = 0
        if (options.ignoreCase) flags = flags or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        if (options.multiline) flags = flags or Pattern.MULTILINE
        if (options.dotAll) flags = flags or Pattern.DOTALL
        return flags
    }

    private fun namedGroups(matcher: Matcher): Map<String, String> {
        val names = matcher.namedGroups() ?: return emptyMap()
        if (names.isEmpty()) return emptyMap()
        val values = LinkedHashMap<String, String>()
        names.forEach { (name, index) ->
            values[name] = matcher.group(index.toInt()) ?: ""
        }
        return values
    }
}
