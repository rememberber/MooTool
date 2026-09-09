package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.storage.TranslationStore
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TranslationEngineTest {
    @AfterTest
    fun reset() {
        TranslationEngine.resetForTests()
    }

    @Test
    fun splitsLongTextWithoutLosingContentOrSurrogates() {
        assertFailsWith<TranslationException> { TranslationEngine.splitTranslationText("text", 1) }
        val text = "${"a".repeat(8)}\n${"b".repeat(8)}"
        val chunks = TranslationEngine.splitTranslationText(text, 10)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 10 })

        val words = "hello ".repeat(10)
        val wordChunks = TranslationEngine.splitTranslationText(words, 20)
        assertEquals(words, wordChunks.joinToString(""))
        assertTrue(wordChunks.dropLast(1).all { it.last().isWhitespace() })

        val emoji = "${"a".repeat(9)}😀${"b".repeat(9)}"
        val emojiChunks = TranslationEngine.splitTranslationText(emoji, 10)
        assertEquals(emoji, emojiChunks.joinToString(""))
        assertTrue(emojiChunks.all { it.length <= 10 })
        assertEquals("a".repeat(9), emojiChunks[0])
        assertTrue(emojiChunks[1].startsWith("😀"))
    }

    @Test
    fun convertsLegacyLanguageCodesAndNormalizesNames() {
        val common = mapOf(
            "wyw" to "lzh", "jp" to "ja", "kor" to "ko", "fra" to "fr", "spa" to "es", "ara" to "ar",
            "bul" to "bg", "est" to "et", "dan" to "da", "fin" to "fi", "rom" to "ro", "slo" to "sl",
            "swe" to "sv", "vie" to "vi"
        )
        for ((legacy, provider) in common) {
            assertEquals(provider, TranslationEngine.googleLanguage(legacy))
            assertEquals(provider, TranslationEngine.bingLanguage(legacy, false))
        }
        assertEquals("zh-TW", TranslationEngine.googleLanguage("cht"))
        assertEquals("zh-Hant", TranslationEngine.bingLanguage("cht", false))
        assertEquals("zh-Hans", TranslationEngine.bingLanguage("zh-CN", false))
        assertEquals("auto-detect", TranslationEngine.bingLanguage("auto", true))
        assertEquals("en", TranslationEngine.normalizeLanguageCode("English", "auto"))
        assertEquals("en", TranslationEngine.normalizeLanguageCode("英语", "auto"))
        assertEquals("en", TranslationEngine.normalizeLanguageCode("英語", "auto"))
        assertEquals("rom", TranslationEngine.normalizeLanguageCode("Romanian", "auto"))
        assertEquals("zh-CN", TranslationEngine.normalizeLanguageCode("auto", "zh-CN", allowAuto = false))
        assertEquals("auto" to "en", TranslationEngine.normalizeLanguagePair("English", "en"))
        assertEquals(listOf(TranslationProvider.Google, TranslationProvider.Bing), TranslationEngine.translationProviderOrder(TranslationProvider.Google, false))
        assertEquals(listOf(TranslationProvider.Bing, TranslationProvider.Google), TranslationEngine.translationProviderOrder(TranslationProvider.Google, true))
        assertEquals(listOf(TranslationProvider.Bing, TranslationProvider.Google), TranslationEngine.translationProviderOrder(TranslationProvider.Bing, false))
        assertEquals(listOf(TranslationProvider.Google, TranslationProvider.Bing), TranslationEngine.translationProviderOrder(TranslationProvider.Bing, true))
        assertEquals("en", TranslationEngine.alternateTargetLanguage("zh-CN"))
        assertEquals("zh-CN" to "en", TranslationEngine.exchangedLanguages("auto", "zh-CN"))
    }

    @Test
    fun localServerCoversChunksFallbackCancelAndWords() {
        val googleHits = AtomicInteger(0)
        val bingPageHits = AtomicInteger(0)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/google") { exchange ->
            val current = concurrent.incrementAndGet()
            maxConcurrent.updateAndGet { current.coerceAtLeast(it) }
            try {
                googleHits.incrementAndGet()
                val q = queryParam(exchange.requestURI.rawQuery, "q")
                if (q.contains("FAIL")) {
                    exchange.sendResponseHeaders(500, -1)
                    exchange.close()
                    return@createContext
                }
                if (q.startsWith("AAAA")) Thread.sleep(120)
                val payload = """[[["G:$q","$q",null,null,1]]]"""
                val bytes = payload.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.write(bytes)
                exchange.close()
            } finally {
                concurrent.decrementAndGet()
            }
        }
        server.createContext("/fail-google") { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        server.createContext("/translator") { exchange ->
            bingPageHits.incrementAndGet()
            val html = """IG:"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" params_AbusePreventionHelper = [1,"tok",600000]"""
            val bytes = html.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.createContext("/ttranslatev3") { exchange ->
            val body = exchange.requestBody.readBytes().decodeToString()
            val text = formParam(body, "text")
            val payload = """[{"translations":[{"text":"B:$text"}]}]"""
            val bytes = payload.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        val hangEntered = CountDownLatch(1)
        server.createContext("/hang-google") { exchange ->
            hangEntered.countDown()
            try {
                Thread.sleep(8_000)
            } catch (_: InterruptedException) {
            }
            val bytes = """[[["late","x",null,null,1]]]""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        server.start()
        try {
            val origin = "http://127.0.0.1:${server.address.port}"
            val google = TranslationEndpoints("$origin/google", "$origin/translator", "$origin/ttranslatev3")
            val hello = TranslationEngine.translate(
                TranslationInput("t-hello", "hello", "en", "zh-CN", TranslationProvider.Google, 5_000),
                endpointsOverride = google
            )
            assertTrue(hello.ok)
            assertEquals("G:hello", hello.text)
            assertEquals("google", hello.provider)
            assertFalse(hello.fallbackUsed)

            val longText = "A".repeat(1_800) + "B".repeat(200)
            val chunked = TranslationEngine.translate(
                TranslationInput("t-chunks", longText, "en", "zh-CN", TranslationProvider.Google, 5_000),
                endpointsOverride = google
            )
            assertTrue(chunked.ok)
            assertEquals("G:" + "A".repeat(1_800) + "G:" + "B".repeat(200), chunked.text)
            assertTrue(maxConcurrent.get() >= 1)
            assertTrue(googleHits.get() >= 3)

            val fallbackEndpoints = TranslationEndpoints("$origin/fail-google", "$origin/translator", "$origin/ttranslatev3")
            TranslationEngine.providerCooldownMs = 60_000
            var now = 10L
            TranslationEngine.clock = { now }
            val fallback = TranslationEngine.translate(
                TranslationInput("t-fallback", "hello", "en", "zh-CN", TranslationProvider.Google, 5_000),
                endpointsOverride = fallbackEndpoints
            )
            assertTrue(fallback.ok)
            assertEquals("B:hello", fallback.text)
            assertEquals("bing", fallback.provider)
            assertTrue(fallback.fallbackUsed)
            assertEquals(1, bingPageHits.get())

            val skipped = TranslationEngine.translate(
                TranslationInput("t-skip", "again", "en", "zh-CN", TranslationProvider.Google, 5_000),
                endpointsOverride = fallbackEndpoints
            )
            assertTrue(skipped.ok)
            assertEquals("bing", skipped.provider)
            assertTrue(skipped.fallbackUsed)
            assertEquals(1, bingPageHits.get())
            now = 70_000L

            val tooLong = TranslationEngine.translate(
                TranslationInput("t-long", "x".repeat(TranslationEngine.MAX_TEXT_UNITS + 1), "en", "zh-CN")
            )
            assertEquals(TranslationErrorCode.INVALID_REQUEST, tooLong.errorCode)

            val hang = TranslationEndpoints("$origin/hang-google", "$origin/translator", "$origin/ttranslatev3")
            val cancelId = "t-cancel"
            val cancelled = java.util.concurrent.CompletableFuture<TranslationResult>()
            Thread {
                cancelled.complete(
                    TranslationEngine.translate(
                        TranslationInput(cancelId, "hello", "en", "zh-CN", TranslationProvider.Google, 12_000),
                        endpointsOverride = hang
                    )
                )
            }.start()
            assertTrue(hangEntered.await(2, TimeUnit.SECONDS))
            assertTrue(TranslationEngine.cancel(cancelId))
            val cancelResult = cancelled.get(4, TimeUnit.SECONDS)
            assertEquals(TranslationErrorCode.ABORTED, cancelResult.errorCode)

            val timed = TranslationEngine.translate(
                TranslationInput("t-timeout", "hello", "en", "zh-CN", TranslationProvider.Google, 1_000),
                endpointsOverride = hang
            )
            assertEquals(TranslationErrorCode.TIMEOUT, timed.errorCode)

            val root = Files.createTempDirectory("mootool-compose-translation-")
            val directories = AppDirectories(root, root.resolve("data"), root.resolve("cache"), root.resolve("logs"))
            directories.ensureCreated()
            val store = TranslationStore(directories)
            val saved = store.saveWord(null, "hello", "你好", "en", "zh-CN", "note")
            val again = store.saveWord(null, "hello", "您好", "en", "zh-CN", "updated")
            assertEquals(saved.id, again.id)
            assertEquals("您好", again.targetText)
            assertEquals(1, store.listWords().size)
            assertEquals(1, store.listWords("您好").size)
            store.saveHistory("hello", "你好", "en", "zh-CN", "google")
            store.saveHistory("bye", "再见", "en", "zh-CN", "bing")
            assertEquals(listOf("bye", "hello"), store.listHistory().map { it.sourceText })
            store.deleteHistory(store.listHistory().first().id)
            assertEquals(1, store.listHistory().size)
            store.clearHistory()
            assertTrue(store.listHistory().isEmpty())
            store.deleteWord(saved.id)
            assertTrue(store.listWords().isEmpty())
        } finally {
            server.stop(0)
        }
    }
}

private fun queryParam(rawQuery: String?, name: String): String {
    return rawQuery.orEmpty().split("&").map { it.split("=", limit = 2) }.firstOrNull { it[0] == name }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        .orEmpty()
}

private fun formParam(body: String, name: String): String {
    return body.split("&").map { it.split("=", limit = 2) }.firstOrNull { it[0] == name }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        .orEmpty()
}
