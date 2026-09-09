package com.rememberber.mootool.next.compose.domain

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

enum class TranslationProvider { Google, Bing }

enum class TranslationErrorCode { ABORTED, TIMEOUT, NETWORK, INVALID_REQUEST }

enum class TranslationTab { Translate, Words, History }

data class TranslationEndpoints(
    val googleTranslateUrl: String = "https://translate.googleapis.com/translate_a/single",
    val bingPageUrl: String = "https://cn.bing.com/translator",
    val bingTranslateUrl: String = "https://cn.bing.com/ttranslatev3"
) {
    companion object {
        val Production: TranslationEndpoints = TranslationEndpoints()
    }
}

data class TranslationInput(
    val requestId: String,
    val text: String,
    val sourceLang: String,
    val targetLang: String,
    val preferredProvider: TranslationProvider = TranslationProvider.Google,
    val timeoutMs: Int = 15_000
)

@Serializable
data class TranslationResult(
    val requestId: String,
    val ok: Boolean,
    val text: String,
    val provider: String = "",
    val fallbackUsed: Boolean = false,
    val errorCode: TranslationErrorCode? = null,
    val statusText: String = ""
)

class TranslationException(val code: TranslationErrorCode, message: String) : RuntimeException(message)

private data class BingSession(
    val ig: String,
    val key: String,
    val token: String,
    val expiresAt: Long,
    var requestCount: Int
)

object TranslationEngine {
    const val MAX_TEXT_UNITS = 50_000
    const val GOOGLE_CHUNK_SIZE = 1_800
    const val GOOGLE_CHUNK_CONCURRENCY = 3
    const val HISTORY_LIMIT = 500
    const val USER_AGENT = "Mozilla/5.0 (MooTool Next) AppleWebKit/537.36 Chrome/138 Safari/537.36"
    private const val CONNECT_TIMEOUT_MS = 5_000L
    private const val BODY_TIMEOUT_MS = 10_000L
    private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    val LANGUAGE_CODES: List<String> = listOf(
        "auto", "zh-CN", "en", "yue", "wyw", "jp", "kor", "fra", "spa", "th", "ara", "ru", "pt", "de", "it",
        "el", "nl", "pl", "bul", "est", "dan", "fin", "cs", "rom", "slo", "swe", "hu", "cht", "vie"
    )
    private val googleLanguageMap = mapOf(
        "wyw" to "lzh",
        "jp" to "ja",
        "kor" to "ko",
        "fra" to "fr",
        "spa" to "es",
        "ara" to "ar",
        "bul" to "bg",
        "est" to "et",
        "dan" to "da",
        "fin" to "fi",
        "rom" to "ro",
        "slo" to "sl",
        "swe" to "sv",
        "cht" to "zh-TW",
        "vie" to "vi"
    )
    private val bingLanguageMap = mapOf(
        "zh-CN" to "zh-Hans",
        "cht" to "zh-Hant",
        "wyw" to "lzh",
        "jp" to "ja",
        "kor" to "ko",
        "fra" to "fr",
        "spa" to "es",
        "ara" to "ar",
        "bul" to "bg",
        "est" to "et",
        "dan" to "da",
        "fin" to "fi",
        "rom" to "ro",
        "slo" to "sl",
        "swe" to "sv",
        "vie" to "vi"
    )
    private val languageAliases: Map<String, String> = buildMap {
        fun add(code: String, vararg aliases: String) {
            put(code.lowercase(), code)
            aliases.forEach { put(it.lowercase(), code) }
        }
        add("auto", "自动检测", "Detect language", "自動検出", "auto-detect")
        add("zh-CN", "中文（简体）", "Chinese (Simplified)", "中国語（簡体字）", "zh", "zh-Hans")
        add("en", "英语", "English", "英語")
        add("yue", "粤语", "Cantonese", "広東語")
        add("wyw", "文言文", "Classical Chinese", "漢文", "lzh")
        add("jp", "日语", "Japanese", "日本語", "ja")
        add("kor", "韩语", "Korean", "韓国語", "ko")
        add("fra", "法语", "French", "フランス語", "fr")
        add("spa", "西班牙语", "Spanish", "スペイン語", "es")
        add("th", "泰语", "Thai", "タイ語")
        add("ara", "阿拉伯语", "Arabic", "アラビア語", "ar")
        add("ru", "俄语", "Russian", "ロシア語")
        add("pt", "葡萄牙语", "Portuguese", "ポルトガル語")
        add("de", "德语", "German", "ドイツ語")
        add("it", "意大利语", "Italian", "イタリア語")
        add("el", "希腊语", "Greek", "ギリシャ語")
        add("nl", "荷兰语", "Dutch", "オランダ語")
        add("pl", "波兰语", "Polish", "ポーランド語")
        add("bul", "保加利亚语", "Bulgarian", "ブルガリア語", "bg")
        add("est", "爱沙尼亚语", "Estonian", "エストニア語", "et")
        add("dan", "丹麦语", "Danish", "デンマーク語", "da")
        add("fin", "芬兰语", "Finnish", "フィンランド語", "fi")
        add("cs", "捷克语", "Czech", "チェコ語")
        add("rom", "罗马尼亚语", "Romanian", "ルーマニア語", "ro")
        add("slo", "斯洛文尼亚语", "Slovenian", "スロベニア語", "sl")
        add("swe", "瑞典语", "Swedish", "スウェーデン語", "sv")
        add("hu", "匈牙利语", "Hungarian", "ハンガリー語")
        add("cht", "繁体中文", "Chinese (Traditional)", "中国語（繁体字）", "zh-TW", "zh-Hant")
        add("vie", "越南语", "Vietnamese", "ベトナム語", "vi")
    }
    private val naturalBreak = Regex("[\\s.!?。！？,，;；:：]")
    private val bingIg = Regex("""IG:"([A-F0-9]{32})"""")
    private val bingAbuse = Regex("""params_AbusePreventionHelper\s*=\s*\[(\d+),"([^"]+)",(\d+)\]""")
    private val mapper = ObjectMapper()
    private val calls = ConcurrentHashMap<String, MutableList<okhttp3.Call>>()
    private val abortReasons = ConcurrentHashMap<String, TranslationErrorCode>()
    private val timeoutTasks = ConcurrentHashMap<String, Future<*>>()
    private val providerCooldownUntil = ConcurrentHashMap<TranslationProvider, Long>()
    private val bingLock = Any()
    @Volatile private var bingSession: BingSession? = null
    @Volatile var endpoints: TranslationEndpoints = TranslationEndpoints.Production
    @Volatile var providerCooldownMs: Long = 10 * 60 * 1_000L
    @Volatile var clock: () -> Long = { System.currentTimeMillis() }
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "translation-timeout").apply { isDaemon = true }
    }

    fun parseProvider(value: String): TranslationProvider =
        if (value.equals("bing", ignoreCase = true)) TranslationProvider.Bing else TranslationProvider.Google

    fun clampTimeout(value: Int): Int = value.coerceIn(1_000, 120_000)

    fun googleLanguage(code: String): String = googleLanguageMap[code] ?: code.ifBlank { "auto" }

    fun bingLanguage(code: String, source: Boolean): String {
        if (code.isBlank() || code == "auto") return if (source) "auto-detect" else "zh-Hans"
        return bingLanguageMap[code] ?: code
    }

    fun normalizeLanguageCode(value: String?, fallback: String, allowAuto: Boolean = true): String {
        val code = languageAliases[value?.trim()?.lowercase().orEmpty()] ?: return fallback
        return if (!allowAuto && code == "auto") fallback else code
    }

    fun normalizeLanguagePair(source: String?, target: String?): Pair<String, String> {
        val targetLang = normalizeLanguageCode(target, "zh-CN", allowAuto = false)
        val sourceLang = normalizeLanguageCode(source, "auto")
        return (if (sourceLang == targetLang) "auto" else sourceLang) to targetLang
    }

    fun alternateTargetLanguage(code: String): String = if (code == "zh-CN") "en" else "zh-CN"

    fun nextSourceLanguages(selectedSource: String, currentTarget: String): Pair<String, String> {
        val source = normalizeLanguageCode(selectedSource, "auto")
        val target = if (source != "auto" && source == currentTarget) alternateTargetLanguage(source) else currentTarget
        return source to target
    }

    fun nextTargetLanguage(selectedTarget: String, currentSource: String): String {
        val target = normalizeLanguageCode(selectedTarget, "zh-CN", allowAuto = false)
        return if (target == currentSource) alternateTargetLanguage(target) else target
    }

    fun exchangedLanguages(source: String, target: String): Pair<String, String> =
        target to if (source == "auto") "en" else source

    fun translationProviderOrder(preferred: TranslationProvider, skipPreferred: Boolean): List<TranslationProvider> {
        val alternate = if (preferred == TranslationProvider.Google) TranslationProvider.Bing else TranslationProvider.Google
        return if (skipPreferred) listOf(alternate, preferred) else listOf(preferred, alternate)
    }

    fun splitTranslationText(text: String, maxLength: Int = GOOGLE_CHUNK_SIZE): List<String> {
        if (maxLength < 2) throw TranslationException(TranslationErrorCode.INVALID_REQUEST, "Invalid translation chunk size")
        if (text.length <= maxLength) return listOf(text)
        val chunks = ArrayList<String>()
        var offset = 0
        while (offset < text.length) {
            var end = minOf(offset + maxLength, text.length)
            if (end < text.length) {
                val minimumNaturalBreak = offset + maxLength / 2
                var cursor = end
                while (cursor > minimumNaturalBreak) {
                    if (naturalBreak.containsMatchIn(text[cursor - 1].toString())) {
                        end = cursor
                        break
                    }
                    cursor -= 1
                }
                val previousCodeUnit = text[end - 1].code
                val nextCodeUnit = text[end].code
                if (previousCodeUnit in 0xD800..0xDBFF && nextCodeUnit in 0xDC00..0xDFFF) {
                    end -= 1
                }
            }
            chunks += text.substring(offset, end)
            offset = end
        }
        return chunks
    }

    fun translate(
        input: TranslationInput,
        proxy: HttpProxyConfig = HttpProxyConfig(),
        endpointsOverride: TranslationEndpoints = endpoints
    ): TranslationResult {
        if (input.text.length > MAX_TEXT_UNITS) {
            return TranslationResult(
                requestId = input.requestId,
                ok = false,
                text = "",
                errorCode = TranslationErrorCode.INVALID_REQUEST,
                statusText = "Text exceeds $MAX_TEXT_UNITS units"
            )
        }
        if (input.text.isEmpty()) {
            return TranslationResult(requestId = input.requestId, ok = true, text = "")
        }
        val timeoutMs = clampTimeout(input.timeoutMs)
        begin(input.requestId, timeoutMs)
        val previousEndpoints = endpoints
        endpoints = endpointsOverride
        try {
            val preferred = input.preferredProvider
            val order = translationProviderOrder(preferred, shouldSkipProvider(preferred))
            var lastError: Exception? = null
            for (provider in order) {
                checkAborted(input.requestId)
                try {
                    val text = translateWith(provider, input, proxy)
                    markProviderSuccess(provider)
                    return TranslationResult(
                        requestId = input.requestId,
                        ok = true,
                        text = text,
                        provider = provider.name.lowercase(),
                        fallbackUsed = provider != preferred
                    )
                } catch (error: Exception) {
                    lastError = error
                    if (isAbort(error, input.requestId)) throw error
                    markProviderFailure(provider)
                }
            }
            throw lastError ?: TranslationException(TranslationErrorCode.NETWORK, "Translation failed")
        } catch (error: Exception) {
            val code = classify(error, input.requestId)
            return TranslationResult(
                requestId = input.requestId,
                ok = false,
                text = "",
                errorCode = code,
                statusText = error.message ?: code.name
            )
        } finally {
            endpoints = previousEndpoints
            finish(input.requestId)
        }
    }

    fun cancel(requestId: String): Boolean {
        abortReasons.putIfAbsent(requestId, TranslationErrorCode.ABORTED)
        val active = calls.remove(requestId).orEmpty()
        active.forEach { it.cancel() }
        timeoutTasks.remove(requestId)?.cancel(false)
        return active.isNotEmpty() || abortReasons.containsKey(requestId)
    }

    fun resetForTests() {
        calls.values.flatten().forEach { runCatching { it.cancel() } }
        calls.clear()
        abortReasons.clear()
        timeoutTasks.values.forEach { it.cancel(false) }
        timeoutTasks.clear()
        providerCooldownUntil.clear()
        bingSession = null
        endpoints = TranslationEndpoints.Production
        providerCooldownMs = 10 * 60 * 1_000L
        clock = { System.currentTimeMillis() }
    }

    private fun translateWith(provider: TranslationProvider, input: TranslationInput, proxy: HttpProxyConfig): String {
        return if (provider == TranslationProvider.Google) {
            translateGoogle(input, proxy)
        } else {
            translateBing(input, proxy)
        }
    }

    private fun translateGoogle(input: TranslationInput, proxy: HttpProxyConfig): String {
        val chunks = splitTranslationText(input.text, GOOGLE_CHUNK_SIZE)
        if (chunks.size == 1) return translateGoogleChunk(chunks[0], input, proxy)
        return mapPool(chunks, GOOGLE_CHUNK_CONCURRENCY) { chunk -> translateGoogleChunk(chunk, input, proxy) }.joinToString("")
    }

    private fun translateGoogleChunk(chunk: String, input: TranslationInput, proxy: HttpProxyConfig): String {
        val url = endpoints.googleTranslateUrl.toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", googleLanguage(input.sourceLang))
            .addQueryParameter("tl", googleLanguage(input.targetLang))
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", chunk)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        execute(input.requestId, request, proxy).use { response ->
            if (!response.isSuccessful) throw TranslationException(TranslationErrorCode.NETWORK, "Google HTTP ${response.code}")
            val payload = mapper.readTree(readLimited(response))
            val parts = payload.get(0) ?: throw TranslationException(TranslationErrorCode.NETWORK, "Google returned no translation")
            val translated = parts.joinToString("") { node -> node.path(0).asText("") }
            if (translated.isEmpty()) throw TranslationException(TranslationErrorCode.NETWORK, "Google returned no translation")
            return translated
        }
    }

    private fun translateBing(input: TranslationInput, proxy: HttpProxyConfig): String {
        val session = getBingSession(input.requestId, proxy)
        session.requestCount += 2
        val page = endpoints.bingPageUrl.toHttpUrl()
        val origin = originOf(page)
        val url = endpoints.bingTranslateUrl.toHttpUrl().newBuilder()
            .addQueryParameter("isVertical", "1")
            .addQueryParameter("IG", session.ig)
            .addQueryParameter("IID", "translator.5026.${session.requestCount}")
            .build()
        val body = FormBody.Builder()
            .add("fromLang", bingLanguage(input.sourceLang, true))
            .add("to", bingLanguage(input.targetLang, false))
            .add("text", input.text)
            .add("token", session.token)
            .add("key", session.key)
            .add("tryFetchingGenderDebiasedTranslations", "true")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Origin", origin)
            .header("Referer", endpoints.bingPageUrl)
            .post(body)
            .build()
        execute(input.requestId, request, proxy).use { response ->
            if (!response.isSuccessful) throw TranslationException(TranslationErrorCode.NETWORK, "Bing HTTP ${response.code}")
            val payload = mapper.readTree(readLimited(response))
            val translated = payload.get(0)?.get("translations")?.get(0)?.get("text")?.asText()
            if (translated.isNullOrEmpty()) throw TranslationException(TranslationErrorCode.NETWORK, "Bing returned no translation")
            return translated
        }
    }

    private fun getBingSession(requestId: String, proxy: HttpProxyConfig): BingSession {
        synchronized(bingLock) {
            val cached = bingSession
            if (cached != null && cached.expiresAt > clock()) return cached
            val request = Request.Builder()
                .url(endpoints.bingPageUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            execute(requestId, request, proxy).use { response ->
                if (!response.isSuccessful) throw TranslationException(TranslationErrorCode.NETWORK, "Bing session HTTP ${response.code}")
                val html = String(readLimited(response), Charsets.UTF_8)
                val ig = bingIg.find(html)?.groupValues?.get(1)
                    ?: throw TranslationException(TranslationErrorCode.NETWORK, "Bing session token unavailable")
                val abuse = bingAbuse.find(html)
                    ?: throw TranslationException(TranslationErrorCode.NETWORK, "Bing session token unavailable")
                val next = BingSession(
                    ig = ig,
                    key = abuse.groupValues[1],
                    token = abuse.groupValues[2],
                    expiresAt = clock() + abuse.groupValues[3].toLong() - 60_000L,
                    requestCount = 0
                )
                bingSession = next
                return next
            }
        }
    }

    private fun <T> mapPool(items: List<T>, concurrency: Int, mapper: (T) -> String): List<String> {
        val results = arrayOfNulls<String>(items.size)
        val failure = AtomicReference<Exception?>(null)
        val next = AtomicInteger(0)
        val workers = minOf(concurrency.coerceAtLeast(1), items.size)
        val threads = (0 until workers).map {
            Thread {
                while (true) {
                    if (failure.get() != null) return@Thread
                    val index = next.getAndIncrement()
                    if (index >= items.size) return@Thread
                    try {
                        results[index] = mapper(items[index])
                    } catch (error: Exception) {
                        failure.compareAndSet(null, error)
                    }
                }
            }.also { it.start() }
        }
        threads.forEach { it.join() }
        failure.get()?.let { throw it }
        return results.map { it ?: throw TranslationException(TranslationErrorCode.NETWORK, "Incomplete Google chunks") }
    }

    private fun execute(requestId: String, request: Request, proxy: HttpProxyConfig): Response {
        checkAborted(requestId)
        val remaining = remainingTimeout(requestId)
        if (remaining <= 0) throw TranslationException(TranslationErrorCode.TIMEOUT, "TIMEOUT")
        val call = buildClient(remaining, proxy).newCall(request)
        register(requestId, call)
        try {
            return call.execute()
        } catch (error: Exception) {
            throw classifyToException(error, requestId, call)
        }
    }

    private fun buildClient(remainingMs: Long, proxy: HttpProxyConfig): OkHttpClient {
        val connect = minOf(CONNECT_TIMEOUT_MS, remainingMs).coerceAtLeast(1)
        val read = minOf(BODY_TIMEOUT_MS, remainingMs).coerceAtLeast(1)
        val builder = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .callTimeout(remainingMs.coerceAtLeast(1), TimeUnit.MILLISECONDS)
            .connectTimeout(connect, TimeUnit.MILLISECONDS)
            .readTimeout(read, TimeUnit.MILLISECONDS)
            .writeTimeout(read, TimeUnit.MILLISECONDS)
        if (proxy.enabled) {
            val host = proxy.host.trim()
            val port = proxy.port.trim().toIntOrNull()
            if (host.isEmpty() || port == null || port !in 1..65535) {
                throw TranslationException(TranslationErrorCode.INVALID_REQUEST, "Invalid proxy settings")
            }
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            if (proxy.username.isNotBlank()) {
                val credential = okhttp3.Credentials.basic(proxy.username, proxy.password)
                builder.proxyAuthenticator { _, response ->
                    response.request.newBuilder().header("Proxy-Authorization", credential).build()
                }
            }
        }
        return builder.build()
    }

    private fun register(requestId: String, call: okhttp3.Call) {
        calls.getOrPut(requestId) { java.util.Collections.synchronizedList(ArrayList()) }.add(call)
        if (abortReasons.containsKey(requestId)) call.cancel()
    }

    private fun begin(requestId: String, timeoutMs: Int) {
        cancel(requestId)
        abortReasons.remove(requestId)
        timeoutTasks[requestId] = scheduler.schedule({
            abortReasons.putIfAbsent(requestId, TranslationErrorCode.TIMEOUT)
            calls.remove(requestId).orEmpty().forEach { it.cancel() }
        }, timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        remainingStarted[requestId] = System.nanoTime() to timeoutMs.toLong()
    }

    private val remainingStarted = ConcurrentHashMap<String, Pair<Long, Long>>()

    private fun remainingTimeout(requestId: String): Long {
        val started = remainingStarted[requestId] ?: return 1
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started.first)
        return started.second - elapsed
    }

    private fun finish(requestId: String) {
        timeoutTasks.remove(requestId)?.cancel(false)
        calls.remove(requestId)
        remainingStarted.remove(requestId)
        abortReasons.remove(requestId)
    }

    private fun checkAborted(requestId: String) {
        val reason = abortReasons[requestId] ?: return
        throw TranslationException(reason, reason.name)
    }

    private fun shouldSkipProvider(provider: TranslationProvider): Boolean {
        val until = providerCooldownUntil[provider] ?: return false
        return until > clock()
    }

    private fun markProviderFailure(provider: TranslationProvider) {
        providerCooldownUntil[provider] = clock() + providerCooldownMs
    }

    private fun markProviderSuccess(provider: TranslationProvider) {
        providerCooldownUntil.remove(provider)
    }

    private fun isAbort(error: Exception, requestId: String): Boolean {
        if (error is TranslationException && error.code in setOf(TranslationErrorCode.ABORTED, TranslationErrorCode.TIMEOUT)) return true
        return abortReasons[requestId] != null
    }

    private fun classify(error: Exception, requestId: String): TranslationErrorCode {
        if (error is TranslationException) return error.code
        abortReasons[requestId]?.let { return it }
        val timedOut = error is InterruptedIOException || error.message?.contains("timeout", ignoreCase = true) == true
        if (timedOut) return TranslationErrorCode.TIMEOUT
        if (error.message?.contains("Canceled", ignoreCase = true) == true) return TranslationErrorCode.ABORTED
        return TranslationErrorCode.NETWORK
    }

    private fun classifyToException(error: Exception, requestId: String, call: okhttp3.Call): TranslationException {
        if (error is TranslationException) return error
        abortReasons[requestId]?.let { return TranslationException(it, it.name) }
        val timedOut = error is InterruptedIOException || error.message?.contains("timeout", ignoreCase = true) == true
        if (timedOut) return TranslationException(TranslationErrorCode.TIMEOUT, "TIMEOUT")
        if (call.isCanceled() || error.message?.contains("Canceled", ignoreCase = true) == true) {
            return TranslationException(TranslationErrorCode.ABORTED, "ABORTED")
        }
        return TranslationException(TranslationErrorCode.NETWORK, error.message ?: "NETWORK")
    }

    private fun readLimited(response: Response): ByteArray {
        val declared = response.header("Content-Length")?.toLongOrNull() ?: 0L
        if (declared > MAX_RESPONSE_BYTES) throw TranslationException(TranslationErrorCode.NETWORK, "RESPONSE_TOO_LARGE")
        val body = response.body ?: return ByteArray(0)
        val source = body.source()
        val buffer = Buffer()
        while (!source.exhausted()) {
            source.read(buffer, 8_192)
            if (buffer.size > MAX_RESPONSE_BYTES) throw TranslationException(TranslationErrorCode.NETWORK, "RESPONSE_TOO_LARGE")
        }
        return buffer.readByteArray()
    }

    private fun originOf(url: HttpUrl): String {
        val defaultPort = if (url.scheme == "https") 443 else 80
        val port = if (url.port == defaultPort) "" else ":${url.port}"
        return "${url.scheme}://${url.host}$port"
    }
}
