package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageBoardWiringPresentationTest {
    @Test
    fun clipDelegatesToEngine() {
        val long = "x".repeat(MessageBoardEngine.MAX_LENGTH + 5)
        assertEquals(MessageBoardEngine.MAX_LENGTH, MessageBoardWiringPresentation.clippedMessage(long).length)
    }

    @Test
    fun wakeErrorWhenDisplayNotAwake() {
        assertTrue(MessageBoardWiringPresentation.wakeErrorIfNeeded(displayAwake = false))
        assertFalse(MessageBoardWiringPresentation.wakeErrorIfNeeded(displayAwake = true))
    }

    @Test
    fun presentationRequiresMessageAndWake() {
        assertFalse(MessageBoardWiringPresentation.canEnterPresentation("", displayAwake = true))
        assertFalse(MessageBoardWiringPresentation.canEnterPresentation("hi", displayAwake = false))
        assertTrue(MessageBoardWiringPresentation.canEnterPresentation("hi", displayAwake = true))
    }

    @Test
    fun shouldToastWakeFailureWhenNotAwake() {
        assertTrue(MessageBoardWiringPresentation.shouldToastWakeFailure(displayAwake = false))
        assertFalse(MessageBoardWiringPresentation.shouldToastWakeFailure(displayAwake = true))
    }
}
