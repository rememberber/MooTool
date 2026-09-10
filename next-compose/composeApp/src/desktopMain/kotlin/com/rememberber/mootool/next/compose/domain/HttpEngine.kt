package com.rememberber.mootool.next.compose.domain

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.Serializable
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

enum class HttpErrorCode { ABORTED, TIMEOUT, NETWORK, INVALID_REQUEST, RESPONSE_TOO_LARGE }

enum class HttpRequestTab { Params, Headers, Cookies, Body }

enum class HttpResponseTab { Body, Headers, Cookies }

@Serializable
data class HttpPair(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val value: String = "",
    val enabled: Boolean = true
)

@Serializable
data class HttpCookie(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val value: String = "",
    val domain: String = "",
    val path: String = "/",
    val expires: String = "",
    val enabled: Boolean = true
)

@Serializable
data class HttpRequestDraft(
    val id: String = "",
    val name: String = "Untitled",
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val params: List<HttpPair> = emptyList(),
    val headers: List<HttpPair> = emptyList(),
    val cookies: List<HttpCookie> = emptyList(),
    val body: String = "",
    val bodyType: String = "application/json"
)

@Serializable
data class HttpResponseResult(
    val requestId: String,
    val ok: Boolean,
    val status: Int,
    val statusText: String,
    val url: String,
    val durationMs: Long,
    val body: String,
    val headers: String,
    val cookies: String,
    val errorCode: HttpErrorCode? = null,
    val binary: Boolean = false
)

data class HttpProxyConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = ""
)

data class HttpPreparedRequest(
    val method: HttpMethod,
    val url: String,
    val headers: List<Pair<String, String>>,
    val body: ByteArray?,
    val contentType: String?
)

class HttpException(val code: HttpErrorCode, message: String) : Exception(message)

object HttpEngine {
    const val MAX_RESPONSE_BYTES = 10 * 1024 * 1024
    val BODY_TYPES = listOf(
        "application/json",
        "text/plain",
        "application/xml",
        "text/xml",
        "text/html",
        "application/javascript"
    )
    private val mapper = ObjectMapper()
    private val calls = ConcurrentHashMap<String, okhttp3.Call>()

    fun emptyDraft(name: String = "Untitled"): HttpRequestDraft = HttpRequestDraft(name = name)

    fun pair(name: String = "", value: String = "", enabled: Boolean = true): HttpPair =
        HttpPair(name = name, value = value, enabled = enabled)

    fun cookie(name: String = "", value: String = "", enabled: Boolean = true): HttpCookie =
        HttpCookie(name = name, value = value, enabled = enabled)

    fun clampTimeout(value: Int): Int = value.coerceIn(1_000, 120_000)

    fun enabledPairs(items: List<HttpPair>): List<HttpPair> =
        items.filter { it.enabled && it.name.isNotBlank() }

    fun enabledCookies(items: List<HttpCookie>): List<HttpCookie> =
        items.filter { it.enabled && it.name.isNotBlank() }

    fun prepare(draft: HttpRequestDraft): HttpPreparedRequest {
        if (draft.url.isBlank()) throw HttpException(HttpErrorCode.INVALID_REQUEST, "URL is required")
        val url = buildRequestUrl(draft.url, draft.method, draft.params)
        val headers = LinkedHashMap<String, MutableList<String>>()
        for (item in enabledPairs(draft.headers)) {
            headers.getOrPut(item.name) { mutableListOf() }.add(item.value)
        }
        val cookieHeader = enabledCookies(draft.cookies).joinToString("; ") { "${it.name}=${it.value}" }
        if (cookieHeader.isNotEmpty()) {
            headers.keys.filter { it.equals("Cookie", ignoreCase = true) }.forEach(headers::remove)
            headers["Cookie"] = mutableListOf(cookieHeader)
        }
        val headerList = headers.flatMap { (name, values) -> values.map { name to it } }.toMutableList()
        val bodyText: String?
        val contentType: String?
        if (draft.method == HttpMethod.GET || draft.method == HttpMethod.HEAD || draft.method == HttpMethod.OPTIONS) {
            bodyText = null
            contentType = null
        } else if (draft.body.isNotEmpty()) {
            bodyText = draft.body
            contentType = if (hasHeader(headerList, "content-type")) null else draft.bodyType.ifBlank { "text/plain" }
            if (contentType != null) headerList += "Content-Type" to contentType
        } else {
            val form = enabledPairs(draft.params)
            if (form.isEmpty()) {
                bodyText = null
                contentType = null
            } else {
                bodyText = form.joinToString("&") { "${formEncode(it.name)}=${formEncode(it.value)}" }
                contentType = if (hasHeader(headerList, "content-type")) null else "application/x-www-form-urlencoded"
                if (contentType != null) headerList += "Content-Type" to contentType
            }
        }
        return HttpPreparedRequest(
            method = draft.method,
            url = url,
            headers = headerList,
            body = bodyText?.toByteArray(StandardCharsets.UTF_8),
            contentType = contentType
        )
    }

    fun buildRequestUrl(raw: String, method: HttpMethod, params: List<HttpPair>): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) throw HttpException(HttpErrorCode.INVALID_REQUEST, "URL is required")
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val uri = try {
            URI(withScheme)
        } catch (_: Exception) {
            throw HttpException(HttpErrorCode.INVALID_REQUEST, "Invalid URL")
        }
        val extra = if (method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS) {
            enabledPairs(params).joinToString("&") { "${formEncode(it.name)}=${formEncode(it.value)}" }
        } else {
            ""
        }
        val query = listOfNotNull(uri.rawQuery, extra.takeIf { it.isNotEmpty() })
            .filter { it.isNotEmpty() }
            .joinToString("&")
        val scheme = uri.scheme ?: throw HttpException(HttpErrorCode.INVALID_REQUEST, "Invalid URL")
        val host = uri.host ?: throw HttpException(HttpErrorCode.INVALID_REQUEST, "Invalid URL")
        val path = uri.rawPath.ifEmpty { "/" }
        val port = if (uri.port >= 0) ":${uri.port}" else ""
        val userInfo = uri.rawUserInfo?.let { "$it@" }.orEmpty()
        val question = if (query.isEmpty()) "" else "?$query"
        val fragment = uri.rawFragment?.let { "#$it" }.orEmpty()
        return "$scheme://$userInfo$host$port$path$question$fragment"
    }

    fun parseCurl(command: String): HttpRequestDraft {
        val tokens = tokenizeCurl(command.trim())
        val curlIndex = tokens.indexOfFirst { it == "curl" || it.endsWith("/curl") }
        if (curlIndex < 0) throw HttpException(HttpErrorCode.INVALID_REQUEST, "A curl command is required")
        var method = HttpMethod.GET
        var url = ""
        var body = ""
        var bodyType = "application/json"
        val headers = mutableListOf<HttpPair>()
        val cookies = mutableListOf<HttpCookie>()
        var index = curlIndex + 1
        while (index < tokens.size) {
            val token = tokens[index]
            val next = tokens.getOrNull(index + 1)
            when {
                token in listOf("-X", "--request") && next != null -> {
                    method = parseMethod(next)
                    index += 2
                }
                token in listOf("-H", "--header") && next != null -> {
                    val split = next.indexOf(':')
                    val name = if (split >= 0) next.substring(0, split).trim() else next.trim()
                    val value = if (split >= 0) next.substring(split + 1).trim() else ""
                    headers += pair(name, value)
                    if (name.equals("content-type", ignoreCase = true) && value.isNotBlank()) bodyType = value
                    index += 2
                }
                token in listOf("-d", "--data", "--data-raw", "--data-binary", "--data-urlencode") && next != null -> {
                    body = next
                    if (method == HttpMethod.GET) method = HttpMethod.POST
                    index += 2
                }
                token in listOf("-b", "--cookie") && next != null -> {
                    next.split(';').forEach { item ->
                        val piece = item.trim()
                        if (piece.isEmpty()) return@forEach
                        val eq = piece.indexOf('=')
                        val name = if (eq >= 0) piece.substring(0, eq) else piece
                        val value = if (eq >= 0) piece.substring(eq + 1) else ""
                        if (name.isNotBlank()) cookies += cookie(name, value)
                    }
                    index += 2
                }
                token == "--url" && next != null -> {
                    url = next
                    index += 2
                }
                !token.startsWith("-") && url.isEmpty() -> {
                    url = token
                    index += 1
                }
                else -> index += 1
            }
        }
        if (url.isEmpty()) throw HttpException(HttpErrorCode.INVALID_REQUEST, "The curl command has no URL")
        return emptyDraft("Imported cURL").copy(method = method, url = url, headers = headers, cookies = cookies, body = body, bodyType = bodyType)
    }

    fun toCurl(draft: HttpRequestDraft): String {
        val parts = mutableListOf("curl", "-X", draft.method.name, shellQuote(draft.url))
        for (header in enabledPairs(draft.headers)) {
            parts += listOf("-H", shellQuote("${header.name}: ${header.value}"))
        }
        val cookieValue = enabledCookies(draft.cookies).joinToString("; ") { "${it.name}=${it.value}" }
        if (cookieValue.isNotEmpty()) parts += listOf("-b", shellQuote(cookieValue))
        if (draft.body.isNotEmpty()) parts += listOf("--data-raw", shellQuote(draft.body))
        return parts.joinToString(" ")
    }

    fun formatBody(body: String, bodyType: String): String {
        if (body.isBlank()) return body
        val type = bodyType.lowercase()
        if (type.contains("json")) {
            return runCatching { mapper.readTree(body).toPrettyString() }.getOrDefault(body)
        }
        return body
    }

    fun send(
        draft: HttpRequestDraft,
        requestId: String,
        timeoutMs: Int,
        proxy: HttpProxyConfig = HttpProxyConfig()
    ): HttpResponseResult {
        val started = System.currentTimeMillis()
        val timeout = clampTimeout(timeoutMs)
        val prepared = try {
            prepare(draft)
        } catch (error: HttpException) {
            return HttpResponseResult(
                requestId = requestId,
                ok = false,
                status = 0,
                statusText = error.message ?: "INVALID_REQUEST",
                url = draft.url,
                durationMs = System.currentTimeMillis() - started,
                body = "",
                headers = "",
                cookies = "",
                errorCode = error.code
            )
        }
        val client = try {
            buildClient(timeout, proxy)
        } catch (error: HttpException) {
            return HttpResponseResult(
                requestId = requestId,
                ok = false,
                status = 0,
                statusText = error.message ?: "INVALID_REQUEST",
                url = prepared.url,
                durationMs = System.currentTimeMillis() - started,
                body = "",
                headers = "",
                cookies = "",
                errorCode = error.code
            )
        }
        val request = buildOkHttpRequest(prepared)
        val call = client.newCall(request)
        calls[requestId]?.cancel()
        calls[requestId] = call
        return try {
            call.execute().use { response ->
                val bytes = readLimited(response)
                val headerText = formatResponseHeaders(response)
                val cookies = response.headers("Set-Cookie").joinToString("\n")
                val contentType = response.header("Content-Type").orEmpty()
                val (text, binary) = decodeBody(bytes, contentType)
                HttpResponseResult(
                    requestId = requestId,
                    ok = response.code in 200..299,
                    status = response.code,
                    statusText = response.message.ifBlank { response.code.toString() },
                    url = response.request.url.toString(),
                    durationMs = System.currentTimeMillis() - started,
                    body = if (binary) text else prettyJson(text),
                    headers = headerText,
                    cookies = cookies,
                    binary = binary
                )
            }
        } catch (error: Exception) {
            HttpResponseResult(
                requestId = requestId,
                ok = false,
                status = 0,
                statusText = error.message ?: error.javaClass.simpleName,
                url = prepared.url,
                durationMs = System.currentTimeMillis() - started,
                body = "",
                headers = "",
                cookies = "",
                errorCode = classify(error, call)
            )
        } finally {
            calls.remove(requestId, call)
        }
    }

    fun cancel(requestId: String): Boolean {
        val call = calls.remove(requestId) ?: return false
        call.cancel()
        return true
    }

    private fun buildOkHttpRequest(prepared: HttpPreparedRequest): Request {
        val builder = Request.Builder().url(prepared.url)
        for ((name, value) in prepared.headers) builder.addHeader(name, value)
        val hasContentType = prepared.headers.any { it.first.equals("Content-Type", ignoreCase = true) }
        val media = if (hasContentType) null else prepared.contentType?.toMediaTypeOrNull()
        val method = prepared.method.name
        when (prepared.method) {
            HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS -> builder.method(method, null)
            else -> builder.method(method, (prepared.body ?: ByteArray(0)).toRequestBody(media))
        }
        return builder.build()
    }

    private fun buildClient(timeoutMs: Int, proxy: HttpProxyConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .callTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        if (proxy.enabled) {
            val host = proxy.host.trim()
            val port = proxy.port.trim().toIntOrNull()
            if (host.isEmpty() || port == null || port !in 1..65535) {
                throw HttpException(HttpErrorCode.INVALID_REQUEST, "Invalid proxy settings")
            }
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            if (proxy.username.isNotBlank()) {
                val credential = Credentials.basic(proxy.username, proxy.password)
                builder.proxyAuthenticator { _, response ->
                    response.request.newBuilder().header("Proxy-Authorization", credential).build()
                }
            }
        }
        return builder.build()
    }

    private fun readLimited(response: Response): ByteArray {
        val declared = response.header("Content-Length")?.toLongOrNull() ?: 0L
        if (declared > MAX_RESPONSE_BYTES) throw HttpException(HttpErrorCode.RESPONSE_TOO_LARGE, "RESPONSE_TOO_LARGE")
        val body = response.body ?: return ByteArray(0)
        val source = body.source()
        val buffer = Buffer()
        while (!source.exhausted()) {
            source.read(buffer, 8_192)
            if (buffer.size > MAX_RESPONSE_BYTES) throw HttpException(HttpErrorCode.RESPONSE_TOO_LARGE, "RESPONSE_TOO_LARGE")
        }
        return buffer.readByteArray()
    }

    private fun decodeBody(bytes: ByteArray, contentType: String): Pair<String, Boolean> {
        val lower = contentType.lowercase()
        val binaryType = lower.startsWith("image/") || lower.startsWith("audio/") || lower.startsWith("video/") ||
            lower.contains("octet-stream") || lower.contains("application/pdf")
        if (binaryType || bytes.contains(0.toByte())) {
            val preview = bytes.take(64).joinToString(" ") { "%02x".format(it.toInt() and 0xff) }
            return "[binary ${bytes.size} bytes]\n$preview" to true
        }
        val charset = charsetOf(contentType) ?: StandardCharsets.UTF_8
        return String(bytes, charset) to false
    }

    private fun charsetOf(contentType: String): Charset? {
        val match = Regex("charset=([^;]+)", RegexOption.IGNORE_CASE).find(contentType) ?: return null
        return runCatching { Charset.forName(match.groupValues[1].trim().trim('"', '\'')) }.getOrNull()
    }

    private fun prettyJson(value: String): String =
        runCatching { mapper.readTree(value).toPrettyString() }.getOrDefault(value)

    private fun formatResponseHeaders(response: Response): String =
        response.headers.names().flatMap { name -> response.headers.values(name).map { "$name: $it" } }.joinToString("\n")

    private fun classify(error: Exception, call: okhttp3.Call): HttpErrorCode {
        if (error is HttpException) return error.code
        val timedOut = error is InterruptedIOException || error.message?.contains("timeout", ignoreCase = true) == true
        if (timedOut) return HttpErrorCode.TIMEOUT
        if (call.isCanceled() || error.message?.contains("Canceled", ignoreCase = true) == true) return HttpErrorCode.ABORTED
        return HttpErrorCode.NETWORK
    }

    private fun hasHeader(headers: List<Pair<String, String>>, name: String): Boolean =
        headers.any { it.first.equals(name, ignoreCase = true) }

    private fun parseMethod(value: String): HttpMethod =
        HttpMethod.entries.find { it.name.equals(value, ignoreCase = true) }
            ?: throw HttpException(HttpErrorCode.INVALID_REQUEST, "Unsupported HTTP method: $value")

    internal fun tokenizeCurl(value: String): List<String> {
        val tokens = ArrayList<String>()
        val token = StringBuilder()
        var quote: Char? = null
        var escaped = false
        for (character in value) {
            if (escaped) {
                token.append(character)
                escaped = false
                continue
            }
            if (character == '\\' && quote != '\'') {
                escaped = true
                continue
            }
            if (character == '\'' && quote != '"') {
                quote = if (quote == '\'') null else '\''
                continue
            }
            if (character == '"' && quote != '\'') {
                quote = if (quote == '"') null else '"'
                continue
            }
            if (character.isWhitespace() && quote == null) {
                if (token.isNotEmpty()) {
                    tokens += token.toString()
                    token.clear()
                }
                continue
            }
            token.append(character)
        }
        if (escaped || quote != null) throw HttpException(HttpErrorCode.INVALID_REQUEST, "Unterminated curl argument")
        if (token.isNotEmpty()) tokens += token.toString()
        return tokens
    }

    internal fun formEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("%20", "+")

    internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"
}
