package com.rememberber.mootool.next.compose.ai

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 对照 Electron `next/electron/mcp/server.test.ts` 工具调用语义。 */
class MooToolMcpToolsTest {
    @Test
    fun mirrorsElectronMcpToolHappyPaths() {
        val formatted = MooToolMcpTools.call(
            "mootool_json_format",
            mapOf("text" to """{"b":2,"a":1}""", "sortKeys" to true, "spaces" to 0),
        )
        assertFalse(formatted.isError)
        assertEquals("""{"a":1,"b":2}""", formatted.text)

        val encode = MooToolMcpTools.call(
            "mootool_encode",
            mapOf("text" to "Moo 中文🐮", "format" to "base64", "direction" to "encode"),
        )
        assertFalse(encode.isError)
        val encoded = encode.text
        val decode = MooToolMcpTools.call(
            "mootool_encode",
            mapOf("text" to encoded, "format" to "base64", "direction" to "decode"),
        )
        assertFalse(decode.isError)
        assertEquals("Moo 中文🐮", decode.text)

        val ts = MooToolMcpTools.call(
            "mootool_timestamp",
            mapOf(
                "text" to "1970-01-01 08:00:00",
                "direction" to "to-timestamp",
                "zone" to "Asia/Shanghai",
            ),
        )
        assertFalse(ts.isError)
        assertEquals("0", ts.text)

        val diff = MooToolMcpTools.call(
            "mootool_diff",
            mapOf("left" to "one\n", "right" to "two\n"),
        )
        assertFalse(diff.isError)

        val hash = MooToolMcpTools.call("mootool_hash", mapOf("text" to "abc"))
        assertFalse(hash.isError)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash.text,
        )

        val uuids = MooToolMcpTools.call("mootool_uuid", mapOf("count" to 3))
        assertFalse(uuids.isError)
        val list = Json.decodeFromString<List<String>>(uuids.text)
        assertEquals(3, list.size)
        assertEquals(3, list.toSet().size)
        assertTrue(list[0].matches(Regex("^[0-9a-f-]{14}4[0-9a-f-]{21}$")))
    }

    @Test
    fun mirrorsElectronMcpToolValidationFailures() {
        assertTrue(MooToolMcpTools.call("missing", emptyMap()).isError)
        assertTrue(
            MooToolMcpTools.call(
                "mootool_json_format",
                mapOf("text" to """{"a":1,"a":2}"""),
            ).isError,
        )
        assertTrue(
            MooToolMcpTools.call(
                "mootool_encode",
                mapOf("text" to "invalid!", "format" to "base64", "direction" to "decode"),
            ).isError,
        )
        assertTrue(
            MooToolMcpTools.call(
                "mootool_json_query",
                mapOf("text" to "[1,2]", "path" to "$[?(@ > 1)]"),
            ).isError,
        )
        assertTrue(
            MooToolMcpTools.call(
                "mootool_uuid",
                mapOf("count" to 101),
            ).isError,
        )
        assertTrue(
            MooToolMcpTools.call(
                "mootool_diff",
                mapOf("left" to "x".repeat(8001), "right" to ""),
            ).isError,
        )
        assertTrue(
            MooToolMcpTools.call(
                "mootool_hash",
                mapOf("text" to 123),
            ).isError,
        )
        assertTrue(
            MooToolMcpTools.call(
                "mootool_hash",
                mapOf("text" to "a", "path" to "/etc/hosts"),
            ).isError,
        )
    }

    @Test
    fun timestampToLocalDetectsThirteenDigitMillisForMcp() {
        val result = MooToolMcpTools.call(
            "mootool_timestamp",
            mapOf(
                "text" to "1704067200000",
                "direction" to "to-local",
                "unit" to "second",
                "zone" to "UTC",
            ),
        )
        assertFalse(result.isError)
        assertEquals("2024-01-01 00:00:00", result.text)
    }

    @Test
    fun timestampRejectsLocalTimeThatFailsRoundTrip() {
        val result = MooToolMcpTools.call(
            "mootool_timestamp",
            mapOf(
                "text" to "2024-03-10 24:00:00",
                "direction" to "to-timestamp",
                "zone" to "UTC",
            ),
        )
        assertTrue(result.isError)
    }

    @Test
    fun encodeUrlRoundTripViaMcp() {
        val encoded = MooToolMcpTools.call(
            "mootool_encode",
            mapOf("text" to "a b", "format" to "url", "direction" to "encode", "charset" to "utf-8"),
        )
        assertFalse(encoded.isError)
        val decoded = MooToolMcpTools.call(
            "mootool_encode",
            mapOf("text" to encoded.text, "format" to "url", "direction" to "decode", "charset" to "utf-8"),
        )
        assertFalse(decoded.isError)
        assertEquals("a b", decoded.text)
    }

    @Test
    fun protobufWireDecodesHexViaMcp() {
        val result = MooToolMcpTools.call(
            "mootool_protobuf_wire",
            mapOf("text" to "0801", "format" to "hex"),
        )
        assertFalse(result.isError)
        assertTrue(result.text.contains("field=1"))
    }

    @Test
    fun jsonQueryReturnsArrayMatchesForValuesPath() {
        val result = MooToolMcpTools.call(
            "mootool_json_query",
            mapOf("text" to """{"values":[1,2]}""", "path" to "$.values[*]"),
        )
        assertFalse(result.isError)
        assertTrue(result.text.contains("1"))
        assertTrue(result.text.contains("2"))
    }
}
