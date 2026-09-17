package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostWiringPresentationTest {
    @Test
    fun applyConfirmRequiresContentAndIdle() {
        assertTrue(HostWiringPresentation.canOpenApplyConfirm("hosts", applying = false))
        assertFalse(HostWiringPresentation.canOpenApplyConfirm(" ", applying = false))
        assertFalse(HostWiringPresentation.canOpenApplyConfirm("hosts", applying = true))
    }
}
