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
    fun copyPayloadPrefersConvertBase64() {
        assertEquals(
            "YQ==",
            ProtobufWiringPresentation.copyPayload("convert", wireOutput = "", base64 = "YQ==", hex = "61", binary = ""),
        )
    }
}
