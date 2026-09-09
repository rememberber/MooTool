package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import ua_parser.Parser

@Serializable
data class UaResult(
    val browser: String,
    val browserVersion: String,
    val engine: String,
    val engineVersion: String,
    val os: String,
    val osVersion: String,
    val deviceType: String,
    val deviceBrand: String,
    val deviceModel: String,
    val mobile: Boolean,
    val bot: Boolean
)

class UaException(val code: String, message: String) : RuntimeException(message)

object UaEngine {
    const val UNKNOWN = "Unknown"

    val presets: List<Pair<String, String>> = listOf(
        "Chrome (Windows)" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Chrome (macOS)" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Firefox (Windows)" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
        "Safari (iPhone)" to "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
        "Chrome (Android)" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
        "curl" to "curl/8.7.1"
    )

    private val parser: Parser by lazy { Parser() }
    private val botPattern = Regex("bot|crawler|spider|slurp|bingpreview|headless|facebookexternalhit", RegexOption.IGNORE_CASE)
    private val mobilePattern = Regex("mobile|android|iphone|ipad", RegexOption.IGNORE_CASE)
    private val chromeVersion = Regex("""(?:Chrome|CriOS|Edg(?:e|A|iOS)?|OPR|Opera)/([\d.]+)""")
    private val firefoxVersion = Regex("""(?:Firefox|FxiOS)/([\d.]+)""")
    private val webkitVersion = Regex("""AppleWebKit/([\d.]+)""")

    fun parse(value: String): UaResult {
        val source = value.trim()
        if (source.isEmpty()) throw UaException("empty", "empty")
        val client = parser.parse(source)
        val browser = namedBrowser(client.userAgent?.family)
        val os = namedOs(client.os?.family)
        val deviceFamily = named(client.device?.family)
        val bot = botPattern.containsMatchIn(source)
        val mobileHint = mobilePattern.containsMatchIn(source)
        val deviceType = when {
            bot -> "bot"
            deviceFamily.equals("iPhone", true) || deviceFamily.equals("iPad", true) ->
                if (deviceFamily.equals("iPad", true)) "tablet" else "mobile"
            mobileHint -> "mobile"
            else -> "desktop"
        }
        val engine = detectEngine(source, browser)
        return UaResult(
            browser = browser,
            browserVersion = version(client.userAgent?.major, client.userAgent?.minor, client.userAgent?.patch),
            engine = engine.first,
            engineVersion = engine.second,
            os = os,
            osVersion = version(client.os?.major, client.os?.minor, client.os?.patch),
            deviceType = deviceType,
            deviceBrand = brandFor(deviceFamily),
            deviceModel = if (deviceFamily == UNKNOWN) UNKNOWN else deviceFamily,
            mobile = deviceType == "mobile" || deviceType == "tablet" || mobileHint,
            bot = bot
        )
    }

    private fun detectEngine(source: String, browser: String): Pair<String, String> {
        val blinkBrowser = browser.contains("Chrome", true) || browser.contains("Edge", true) ||
            browser.equals("Opera", true) || browser.equals("Chromium", true) ||
            browser.equals("Chrome Mobile", true)
        val firefox = browser.contains("Firefox", true)
        val safari = browser.equals("Safari", true) || browser.equals("Mobile Safari", true)
        return when {
            blinkBrowser || (source.contains("Chrome/", true) && source.contains("Safari/")) ->
                "Blink" to (chromeVersion.find(source)?.groupValues?.get(1) ?: UNKNOWN)
            firefox || source.contains("Firefox/", true) ->
                "Gecko" to (firefoxVersion.find(source)?.groupValues?.get(1) ?: UNKNOWN)
            safari || (source.contains("Safari/", true) && !source.contains("Chrome/", true) && !source.contains("Chromium/", true)) ->
                "WebKit" to (webkitVersion.find(source)?.groupValues?.get(1) ?: UNKNOWN)
            else -> UNKNOWN to UNKNOWN
        }
    }

    private fun namedBrowser(value: String?): String {
        val name = named(value)
        return when {
            name.equals("Mobile Safari", true) -> "Safari"
            name.equals("Chrome Mobile", true) || name.equals("Chrome Mobile iOS", true) -> "Chrome"
            else -> name
        }
    }

    private fun namedOs(value: String?): String {
        val name = named(value)
        return if (name.equals("iPhone OS", true) || name.equals("iPadOS", true)) "iOS" else name
    }

    private fun named(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.isEmpty() || trimmed.equals("Other", true)) UNKNOWN else trimmed
    }

    private fun version(major: String?, minor: String?, patch: String?): String {
        val parts = listOfNotNull(major, minor, patch).filter { it.isNotBlank() }
        return if (parts.isEmpty()) UNKNOWN else parts.joinToString(".")
    }

    private fun brandFor(deviceFamily: String): String = when {
        deviceFamily.contains("iPhone", true) || deviceFamily.contains("iPad", true) || deviceFamily.contains("iPod", true) -> "Apple"
        deviceFamily.contains("Pixel", true) -> "Google"
        else -> UNKNOWN
    }
}
