package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.storage.HttpCollectionStore
import com.sun.net.httpserver.HttpServer
import org.junit.Assume
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpEngineTest {
    @Test
    fun parsesCurlAndRoundTripsImportantFields() {
        val request = HttpEngine.parseCurl(
            """curl 'https://example.com/api?q=1' -X POST -H 'Content-Type: application/json' -H 'X-Test: yes' -b 'sid=abc; mode=dark' --data-raw '{"ok":true}'"""
        )
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("https://example.com/api?q=1", request.url)
        assertEquals("application/json", request.bodyType)
        assertEquals("""{"ok":true}""", request.body)
        assertEquals(2, request.headers.size)
        assertEquals(listOf("sid", "mode"), request.cookies.map { it.name })
        val parsed = HttpEngine.parseCurl(HttpEngine.toCurl(request))
        assertEquals(HttpMethod.POST, parsed.method)
        assertEquals("https://example.com/api?q=1", parsed.url)
        assertEquals("""{"ok":true}""", parsed.body)
        assertTrue(parsed.headers.any { it.name == "Accept" || it.name == "Content-Type" || it.name == "X-Test" })
    }

    @Test
    fun curlRoundTripMatchesElectronHttpToolsAcceptAndBody() {
        val source = HttpEngine.parseCurl(
            """curl https://example.com -H 'Accept: application/json' --data-raw 'hello world'""",
        )
        assertEquals(HttpMethod.POST, source.method)
        assertEquals("https://example.com", source.url)
        assertEquals("hello world", source.body)
        assertEquals("application/json", source.headers.first().value)
        val parsed = HttpEngine.parseCurl(HttpEngine.toCurl(source))
        assertEquals(HttpMethod.POST, parsed.method)
        assertEquals("https://example.com", parsed.url)
        assertEquals("hello world", parsed.body)
        assertTrue(parsed.headers.any { it.name == "Accept" && it.value == "application/json" })
    }

    @Test
    fun frozenSemanticsKeepOriginalQueryAndDuplicateParams() {
        val params = listOf(
            HttpEngine.pair("q", "2"),
            HttpEngine.pair("q", "3"),
            HttpEngine.pair("off", "x", enabled = false)
        )
        val getUrl = HttpEngine.buildRequestUrl("https://example.com/api?q=1", HttpMethod.GET, params)
        assertTrue(getUrl.startsWith("https://example.com/api?"))
        assertTrue(getUrl.contains("q=1"))
        assertTrue(getUrl.contains("q=2"))
        assertTrue(getUrl.contains("q=3"))
        assertFalse(getUrl.contains("off="))
        val post = HttpEngine.prepare(
            HttpEngine.emptyDraft().copy(
                method = HttpMethod.POST,
                url = "https://example.com/submit",
                params = listOf(HttpEngine.pair("a", "1"), HttpEngine.pair("a", "2"))
            )
        )
        assertEquals("https://example.com/submit", post.url)
        assertEquals("a=1&a=2", post.body?.decodeToString())
        assertTrue(post.headers.any { it.first.equals("Content-Type", true) && it.second == "application/x-www-form-urlencoded" })
        val withBody = HttpEngine.prepare(
            HttpEngine.emptyDraft().copy(
                method = HttpMethod.POST,
                url = "https://example.com/submit?keep=1",
                params = listOf(HttpEngine.pair("a", "1")),
                body = """{"n":1}""",
                bodyType = "application/json"
            )
        )
        assertEquals("https://example.com/submit?keep=1", withBody.url)
        assertEquals("""{"n":1}""", withBody.body?.decodeToString())
        val duplicates = HttpEngine.prepare(
            HttpEngine.emptyDraft().copy(
                url = "https://example.com",
                headers = listOf(HttpEngine.pair("X-Dup", "one"), HttpEngine.pair("X-Dup", "two"))
            )
        )
        assertEquals(listOf("one", "two"), duplicates.headers.filter { it.first == "X-Dup" }.map { it.second })
    }

    @Test
    fun localServerCoversStatusRedirectTimeoutCancelAndLimit() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/echo") { exchange ->
            val query = exchange.requestURI.rawQuery.orEmpty()
            val incoming = exchange.requestBody.readBytes().decodeToString()
            val header = exchange.requestHeaders.getFirst("X-Test").orEmpty()
            val payload = "method=${exchange.requestMethod};query=$query;body=$incoming;x=$header"
            val bytes = payload.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.createContext("/missing") { exchange ->
            val bytes = "nope".toByteArray()
            exchange.sendResponseHeaders(404, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.createContext("/redir") { exchange ->
            exchange.responseHeaders.add("Location", "/echo?from=redir")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/slow") { exchange ->
            Thread.sleep(1_500)
            val bytes = "late".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.createContext("/big") { exchange ->
            val bytes = ByteArray(HttpEngine.MAX_RESPONSE_BYTES + 8) { 'a'.code.toByte() }
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.start()
        try {
            val origin = "http://127.0.0.1:${server.address.port}"
            val echo = HttpEngine.send(
                HttpEngine.emptyDraft().copy(
                    method = HttpMethod.POST,
                    url = "$origin/echo?keep=1",
                    params = listOf(HttpEngine.pair("a", "1")),
                    headers = listOf(HttpEngine.pair("X-Test", "yes")),
                    body = """{"ok":true}""",
                    bodyType = "application/json"
                ),
                requestId = "echo-1",
                timeoutMs = 5_000
            )
            assertEquals(200, echo.status)
            assertTrue(echo.ok)
            assertTrue(echo.body.contains("keep=1"))
            assertFalse(echo.body.contains("a=1"))
            assertTrue(echo.body.contains("""{"ok":true}"""))
            assertTrue(echo.body.contains("x=yes"))

            val notFound = HttpEngine.send(
                HttpEngine.emptyDraft().copy(url = "$origin/missing"),
                requestId = "missing-1",
                timeoutMs = 5_000
            )
            assertEquals(404, notFound.status)
            assertFalse(notFound.ok)
            assertEquals("nope", notFound.body.trim())

            val redirected = HttpEngine.send(
                HttpEngine.emptyDraft().copy(url = "$origin/redir"),
                requestId = "redir-1",
                timeoutMs = 5_000
            )
            assertEquals(200, redirected.status)
            assertTrue(redirected.body.contains("from=redir"))

            val timedOut = HttpEngine.send(
                HttpEngine.emptyDraft().copy(url = "$origin/slow"),
                requestId = "slow-1",
                timeoutMs = 1_000
            )
            assertEquals(HttpErrorCode.TIMEOUT, timedOut.errorCode)
            assertEquals(0, timedOut.status)

            val hangEntered = java.util.concurrent.CountDownLatch(1)
            server.createContext("/hang") { exchange ->
                hangEntered.countDown()
                try {
                    Thread.sleep(8_000)
                } catch (_: InterruptedException) {
                }
                val bytes = "late".toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.write(bytes)
                exchange.close()
            }
            val cancelId = "cancel-1"
            val cancelled = java.util.concurrent.CompletableFuture<HttpResponseResult>()
            Thread {
                cancelled.complete(
                    HttpEngine.send(HttpEngine.emptyDraft().copy(url = "$origin/hang"), cancelId, 12_000)
                )
            }.start()
            assertTrue(hangEntered.await(2, java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(HttpEngine.cancel(cancelId))
            val cancelResult = cancelled.get(4, java.util.concurrent.TimeUnit.SECONDS)
            assertEquals(HttpErrorCode.ABORTED, cancelResult.errorCode)

            val tooLarge = HttpEngine.send(
                HttpEngine.emptyDraft().copy(url = "$origin/big"),
                requestId = "big-1",
                timeoutMs = 5_000
            )
            assertEquals(HttpErrorCode.RESPONSE_TOO_LARGE, tooLarge.errorCode)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun collectionStoreRoundTripStaysInComposeDataDir() {
        val root = Files.createTempDirectory("compose-http-store-")
        val directories = AppDirectories(root, root.resolve("data"), root.resolve("cache"), root.resolve("logs"))
        directories.ensureCreated()
        val store = HttpCollectionStore(directories)
        val saved = store.save(HttpEngine.emptyDraft("ping").copy(url = "https://example.com/ping", method = HttpMethod.GET))
        val updated = store.save(saved.draft.copy(name = "ping-2", url = "https://example.com/v2"))
        assertEquals(saved.id, updated.id)
        assertEquals("ping-2", store.get(saved.id)?.draft?.name)
        assertEquals(1, store.list("ping").size)
        store.delete(saved.id)
        assertTrue(store.list().isEmpty())
        val stored = directories.dataRoot.resolve("http").resolve("requests.json").readText()
        assertFalse(stored.contains("t_msg_http"))
    }

    @Test
    fun failAndCancelKeepPreviousResponseLabel() {
        val ok = sampleResponse(ok = true, status = 200, body = "hello")
        val aborted = sampleResponse(ok = false, status = 0, body = "", error = HttpErrorCode.ABORTED)
        val timeout = sampleResponse(ok = false, status = 0, body = "", error = HttpErrorCode.TIMEOUT)
        val tooLarge = sampleResponse(ok = false, status = 200, body = "partial", error = HttpErrorCode.RESPONSE_TOO_LARGE)
        assertEquals(ok, HttpEngine.usableResponse(ok, null))
        assertEquals(ok, HttpEngine.usableResponse(aborted, ok))
        assertTrue(HttpEngine.showPreviousLabel(sending = true, current = ok, previous = ok))
        assertTrue(HttpEngine.showPreviousLabel(sending = false, current = aborted, previous = ok))
        assertTrue(HttpEngine.showPreviousLabel(sending = false, current = timeout, previous = ok))
        assertFalse(HttpEngine.showPreviousLabel(sending = false, current = ok, previous = ok))
        assertFalse(HttpEngine.showPreviousLabel(sending = false, current = tooLarge, previous = ok))
        assertEquals(ok, HttpEngine.visibleResponse(sending = false, current = aborted, previous = ok))
        assertEquals(tooLarge, HttpEngine.visibleResponse(sending = false, current = tooLarge, previous = ok))
        assertEquals(ok, HttpEngine.visibleResponse(sending = true, current = null, previous = ok))
    }

    @Test
    fun responseFindReadsTabPayloadAndWrapsIndex() {
        val result = sampleResponse(true, 200, "alpha").copy(headers = "X-Test: 1", cookies = "sid=1")
        assertEquals("alpha", HttpResponseFind.payload(result, HttpResponseTab.Body))
        assertEquals("X-Test: 1", HttpResponseFind.payload(result, HttpResponseTab.Headers))
        assertEquals("sid=1", HttpResponseFind.payload(result, HttpResponseTab.Cookies))
        assertEquals("", HttpResponseFind.payload(null, HttpResponseTab.Body))
        val matches = FindReplace.findAll("one two one", "one", FindReplaceOptions())
        assertEquals(2, matches.size)
        assertEquals(1, HttpResponseFind.nextIndex(matches.size, 0, forward = true))
        assertEquals(0, HttpResponseFind.nextIndex(matches.size, 1, forward = true))
        assertEquals(1, HttpResponseFind.nextIndex(matches.size, 0, forward = false))
        assertEquals(0, HttpResponseFind.nextIndex(0, 3, forward = true))
        val spans = HttpResponseFind.spans("one two one".length, matches, 1)
        assertEquals(2, spans.size)
        assertFalse(spans[0].current)
        assertTrue(spans[1].current)
        assertEquals(8, spans[1].start)
        val unsetSpans = HttpResponseFind.spans("one two one".length, matches, HttpResponseFind.FIND_INDEX_UNSET)
        assertEquals(2, unsetSpans.size)
        assertFalse(unsetSpans.any { it.current })
        assertEquals(emptyList(), HttpResponseFind.spans(3, emptyList(), 0))
    }

    @Test
    fun localServerEchoesRequestCookiesAndResponseSetCookie() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/cookie") { exchange ->
            val incoming = exchange.requestHeaders.getFirst("Cookie").orEmpty()
            exchange.responseHeaders.add("Set-Cookie", "sid=abc; Path=/")
            exchange.responseHeaders.add("Set-Cookie", "mode=dark; Path=/")
            val payload = "cookie=$incoming"
            val bytes = payload.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.start()
        try {
            val origin = "http://127.0.0.1:${server.address.port}"
            val result = HttpEngine.send(
                HttpEngine.emptyDraft().copy(
                    url = "$origin/cookie",
                    cookies = listOf(
                        HttpEngine.cookie("token", "secret"),
                        HttpEngine.cookie("off", "x", enabled = false),
                    ),
                ),
                requestId = "cookie-echo",
                timeoutMs = 5_000,
            )
            assertEquals(200, result.status)
            assertTrue(result.body.contains("token=secret"))
            assertFalse(result.body.contains("off="))
            assertTrue(result.cookies.contains("sid=abc"))
            assertTrue(result.cookies.contains("mode=dark"))
            val roundTrip = HttpEngine.parseCurl(HttpEngine.toCurl(HttpEngine.emptyDraft().copy(
                url = "$origin/cookie",
                cookies = listOf(HttpEngine.cookie("token", "secret")),
            )))
            assertEquals("token=secret", roundTrip.cookies.first().let { "${it.name}=${it.value}" })
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun postBodyWithEmbeddedNullBytesRoundTripsOnLocalServer() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/raw") { exchange ->
            val incoming = exchange.requestBody.readBytes()
            val summary = incoming.joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val payload = "len=${incoming.size};hex=$summary"
            val bytes = payload.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.start()
        try {
            val origin = "http://127.0.0.1:${server.address.port}"
            val body = "\u0000ab"
            val prepared = HttpEngine.prepare(
                HttpEngine.emptyDraft().copy(
                    method = HttpMethod.POST,
                    url = "$origin/raw",
                    body = body,
                    bodyType = "application/octet-stream",
                )
            )
            assertTrue(prepared.body!!.contentEquals(body.toByteArray(StandardCharsets.UTF_8)))
            val result = HttpEngine.send(
                HttpEngine.emptyDraft().copy(
                    method = HttpMethod.POST,
                    url = "$origin/raw",
                    body = body,
                    bodyType = "application/octet-stream",
                ),
                requestId = "raw-post",
                timeoutMs = 5_000,
            )
            assertEquals(200, result.status)
            assertTrue(result.body.contains("len=3"))
            assertTrue(result.body.contains("hex=006162"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun decodeBodyTreatsMultipartResponsesAsText() {
        val boundary = "----MooTool"
        val payload = """
            --$boundary
            Content-Disposition: form-data; name="field"

            hello
            --$boundary--
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val (text, binary) = HttpEngine.decodeBody(payload, "multipart/form-data; boundary=$boundary")
        assertFalse(binary)
        assertTrue(text.contains("hello"))
        assertTrue(text.contains(boundary))
    }

    @Test
    fun parseCurlDataUrlencodeDecodesBodyAndSetsFormContentType() {
        val request = HttpEngine.parseCurl("curl 'https://example.com/form' --data-urlencode 'q=hello%20world'")
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("https://example.com/form", request.url)
        assertEquals("q=hello world", request.body)
        assertEquals("application/x-www-form-urlencoded", request.bodyType)
        val withHeader = HttpEngine.parseCurl(
            "curl https://example.com/form -H 'Content-Type: text/plain' --data-urlencode 'a%3D1'"
        )
        assertEquals("a=1", withHeader.body)
        assertEquals("text/plain", withHeader.bodyType)
    }

    @Test
    fun parseCurlDataBinaryPreservesPayloadAndDefaultsToPost() {
        val payload = "\u0000ab"
        val request = HttpEngine.parseCurl("curl https://example.com/upload --data-binary '${payload.replace("'", "'\"'\"'")}'")
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("https://example.com/upload", request.url)
        assertEquals(payload, request.body)
        val roundTrip = HttpEngine.parseCurl(HttpEngine.toCurl(request))
        assertEquals(payload, roundTrip.body)
    }

    @Test
    fun binaryResponseKeepsOriginalBytesForDownload() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0)
        val (text, binary) = HttpEngine.decodeBody(png, "image/png")
        assertTrue(binary)
        assertTrue(text.startsWith("[binary 5 bytes]"))
        val result = sampleResponse(true, 200, text).copy(binary = true, bodyBytes = png)
        assertTrue(HttpEngine.downloadBytes(result).contentEquals(png))
        val json = HttpEngine.decodeBody("""{"ok":true}""".toByteArray(), "application/json")
        assertFalse(json.second)
        assertEquals(null, HttpEngine.downloadBytes(sampleResponse(true, 200, json.first)))
        assertTrue(HttpEngine.decodeBody(byteArrayOf(1, 2, 3), "application/pdf").second)
    }

    @Test
    fun publicSmokeAllowlistPermitsHttpBinAndLocalhostOnly() {
        assertTrue(HttpEngine.isPublicSmokeUrlAllowed("https://httpbin.org/get"))
        assertTrue(HttpEngine.isPublicSmokeUrlAllowed("http://127.0.0.1:8080/echo"))
        assertTrue(HttpEngine.isPublicSmokeUrlAllowed("http://localhost/health"))
        assertFalse(HttpEngine.isPublicSmokeUrlAllowed("https://example.com/"))
        assertFalse(HttpEngine.isPublicSmokeUrlAllowed("ftp://httpbin.org/get"))
        assertEquals(HttpEngine.DEFAULT_PUBLIC_SMOKE_URL, HttpEngine.resolvePublicSmokeUrl())
    }

    @Test
    fun optionalHttpBinPublicGetSmoke() {
        Assume.assumeTrue(
            "Set MOOTOOL_HTTP_PUBLIC_SMOKE=1 to run httpbin/localhost smoke",
            System.getenv("MOOTOOL_HTTP_PUBLIC_SMOKE") == "1",
        )
        val url = HttpEngine.resolvePublicSmokeUrl()
        val result = HttpEngine.send(
            HttpEngine.emptyDraft().copy(url = url, method = HttpMethod.GET),
            requestId = "public-smoke",
            timeoutMs = 15_000,
        )
        Assume.assumeTrue(
            "Network unavailable; record as 未测 in acceptance",
            result.ok || result.errorCode != HttpErrorCode.NETWORK,
        )
        assertTrue(result.ok, "status=${result.status} error=${result.errorCode}")
        assertTrue(result.body.contains("httpbin.org") || result.body.contains("mootool-next-compose"))
    }

    @Test
    fun bodyAndResponseSyntaxFollowMime() {
        val json = org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JSON
        val xml = org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_XML
        val html = org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_HTML
        val js = org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        val none = org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_NONE
        assertEquals(json, HttpEngine.syntaxForMime("application/json"))
        assertEquals(xml, HttpEngine.syntaxForMime("application/xml; charset=utf-8"))
        assertEquals(xml, HttpEngine.syntaxForMime("text/xml"))
        assertEquals(html, HttpEngine.syntaxForMime("text/html"))
        assertEquals(js, HttpEngine.syntaxForMime("application/javascript"))
        assertEquals(none, HttpEngine.syntaxForMime("text/plain"))
        val result = sampleResponse(true, 200, """{"ok":true}""").copy(headers = "Content-Type: application/json\nX-Test: 1")
        assertEquals(json, HttpEngine.syntaxForResponse(result, HttpResponseTab.Body))
        assertEquals(none, HttpEngine.syntaxForResponse(result, HttpResponseTab.Headers))
        assertEquals(none, HttpEngine.syntaxForResponse(result, HttpResponseTab.Cookies))
        assertEquals("", HttpEngine.contentTypeFromHeaders("X-Test: 1"))
        assertEquals("application/json; charset=utf-8", HttpEngine.contentTypeFromHeaders("content-type: application/json; charset=utf-8"))
    }
}

private fun sampleResponse(
    ok: Boolean,
    status: Int,
    body: String,
    error: HttpErrorCode? = null
): HttpResponseResult = HttpResponseResult(
    requestId = "req",
    ok = ok,
    status = status,
    statusText = if (ok) "OK" else "ERR",
    url = "https://example.com",
    durationMs = 12,
    body = body,
    headers = "",
    cookies = "",
    errorCode = error
)
