package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.storage.HttpCollectionStore
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
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
}
