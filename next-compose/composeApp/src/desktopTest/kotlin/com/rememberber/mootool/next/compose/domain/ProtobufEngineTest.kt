package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProtobufEngineTest {
    private val personProto =
        "syntax = \"proto3\"; message Person { string name = 1; int32 age = 2; repeated string tags = 3; }"

    @Test
    fun convertsJsonToHexAndBackLikeElectron() {
        val json = """{"name":"Moo","age":25,"tags":["desktop","tool"]}"""
        val hex = ProtobufEngine.jsonToProtobuf(personProto, "Person", json, ProtobufBinaryFormat.Hex)
        assertTrue(hex.matches(Regex("^[0-9a-f]+$")))
        val roundTrip = ProtobufEngine.protobufToJson(personProto, "Person", hex, ProtobufBinaryFormat.Hex)
        assertTrue(roundTrip.contains("Moo"))
        assertTrue(roundTrip.contains("\"age\": 25") || roundTrip.contains("\"age\":25"))
        assertTrue(roundTrip.contains("desktop") && roundTrip.contains("tool"))
        val base64 = ProtobufEngine.convertBinary(hex, ProtobufBinaryFormat.Hex, ProtobufBinaryFormat.Base64)
        assertEquals(hex, ProtobufEngine.convertBinary(base64, ProtobufBinaryFormat.Base64, ProtobufBinaryFormat.Hex))
    }

    @Test
    fun decodesWireFieldsWithoutADefinition() {
        val hex = ProtobufEngine.jsonToProtobuf(personProto, "Person", """{"name":"Moo","age":25}""", ProtobufBinaryFormat.Hex)
        val output = ProtobufEngine.decodeWire(hex, ProtobufBinaryFormat.Hex)
        assertTrue(output.contains("field=1"))
        assertTrue(output.contains("value=\"Moo\""))
        assertTrue(output.contains("field=2"))
        assertTrue(output.contains("value=25"))
    }

    @Test
    fun formatsCompactProtoDefinitions() {
        assertTrue(
            ProtobufEngine.formatProtoDefinition(personProto).contains("message Person {\n  string name = 1;")
        )
    }

    @Test
    fun handlesNestedMapOneofInt64AndBytes() {
        val proto = """
            syntax = "proto3";
            message Envelope {
              message Nested { string title = 1; }
              Nested nested = 1;
              map<string, int32> counts = 2;
              oneof payload { string text = 3; bytes raw = 4; }
              int64 big = 5;
            }
        """.trimIndent()
        val json = """{"nested":{"title":"Moo"},"counts":{"a":1},"text":"hi","big":"9007199254740993"}"""
        val hex = ProtobufEngine.jsonToProtobuf(proto, "Envelope", json, ProtobufBinaryFormat.Hex)
        val back = ProtobufEngine.protobufToJson(proto, "Envelope", hex, ProtobufBinaryFormat.Hex)
        assertTrue(back.contains("Moo"))
        assertTrue(back.contains("9007199254740993"))
        assertTrue(back.contains("hi"))
    }

    @Test
    fun reportsCompileAndBinaryErrorsWithoutChangingContract() {
        val compile = assertFailsWith<ProtobufException> {
            ProtobufEngine.jsonToProtobuf("not a proto", "Person", "{}", ProtobufBinaryFormat.Hex)
        }
        assertEquals("compile", compile.code)
        val missing = assertFailsWith<ProtobufException> {
            ProtobufEngine.jsonToProtobuf(personProto, "Unknown", "{}", ProtobufBinaryFormat.Hex)
        }
        assertEquals("missing-message", missing.code)
        val hex = assertFailsWith<ProtobufException> {
            ProtobufEngine.parseBinary("zz", ProtobufBinaryFormat.Hex)
        }
        assertEquals("invalid-hex", hex.code)
        val truncated = assertFailsWith<ProtobufException> {
            ProtobufEngine.decodeWire("0a03", ProtobufBinaryFormat.Hex)
        }
        assertEquals("wire", truncated.code)
    }
}
