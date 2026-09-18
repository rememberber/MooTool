package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtobufWiringPresentationTest {
    @Test
    fun jsonTabRequiresAllFields() {
        assertFalse(ProtobufWiringPresentation.canJsonToBinary("syntax", "", "{}"))
        assertTrue(ProtobufWiringPresentation.canJsonToBinary("syntax", "Msg", "{}"))
    }

    @Test
    fun runJsonToBinaryUsesEngine() {
        val proto = """
            syntax = "proto3";
            message Person { string name = 1; int32 age = 2; }
        """.trimIndent()
        val outcome = ProtobufWiringPresentation.runJsonToBinary(
            proto,
            "Person",
            """{"name":"Moo","age":1}""",
            ProtobufBinaryFormat.Hex,
        )
        assertTrue(outcome is ProtobufWiringPresentation.OperationOutcome.Success)
    }

    @Test
    fun copyPayloadPrefersConvertBase64() {
        assertEquals(
            "YQ==",
            ProtobufWiringPresentation.copyPayload("convert", wireOutput = "", base64 = "YQ==", hex = "61", binary = ""),
        )
    }

    @Test
    fun toolbarActionEnabledMatchesGuards() {
        assertTrue(ProtobufWiringPresentation.jsonToBinaryActionEnabled("p", "M", "{}"))
        assertFalse(ProtobufWiringPresentation.decodeWireActionEnabled("  "))
        assertTrue(ProtobufWiringPresentation.copyPayloadActionEnabled("wire", wireOutput = "1", base64 = "", hex = "", binary = ""))
        assertFalse(ProtobufWiringPresentation.copyPayloadActionEnabled("json", wireOutput = "", base64 = "", hex = "", binary = ""))
    }

    @Test
    fun shouldToastOperationFailure() {
        assertTrue(ProtobufWiringPresentation.shouldToastOperationFailure(IllegalStateException()))
    }
}
