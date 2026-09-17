package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class RandomWiringPresentationTest {
    @Test
    fun sessionRandomLengthClampsSettingsDefault() {
        assertEquals(1, RandomWiringPresentation.sessionRandomLength(0))
        assertEquals(4_096, RandomWiringPresentation.sessionRandomLength(99_999))
        assertEquals(16, RandomWiringPresentation.sessionRandomLength(16))
    }

    @Test
    fun persistedRandomLengthClampsSessionField() {
        assertEquals(1, RandomWiringPresentation.persistedRandomLength(0))
        assertEquals(4_096, RandomWiringPresentation.persistedRandomLength(8_192))
        assertEquals(32, RandomWiringPresentation.persistedRandomLength(32))
    }
}
