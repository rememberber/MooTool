package com.rememberber.mootool.next.compose.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostWiringPresentationTest {
    @Test
    fun applyConfirmRequiresContentAndIdle() {
        assertTrue(HostWiringPresentation.canOpenApplyConfirm("hosts", applying = false))
        assertFalse(HostWiringPresentation.canOpenApplyConfirm(" ", applying = false))
        assertFalse(HostWiringPresentation.canOpenApplyConfirm("hosts", applying = true))
    }

    @Test
    fun profileAndFindActionEnabled() {
        assertFalse(HostWiringPresentation.saveProfileActionEnabled(dirty = false))
        assertTrue(HostWiringPresentation.saveProfileActionEnabled(dirty = true))
        assertFalse(HostWiringPresentation.exportProfileActionEnabled("  "))
        assertTrue(HostWiringPresentation.exportProfileActionEnabled("127.0.0.1 x"))
        assertTrue(HostWiringPresentation.copyProfileActionEnabled("", "hosts"))
        assertFalse(HostWiringPresentation.copyProfileActionEnabled("", "  "))
        assertFalse(HostWiringPresentation.deleteProfileActionEnabled(""))
        assertTrue(HostWiringPresentation.deleteProfileActionEnabled("p1"))
        assertFalse(HostWiringPresentation.findQueryActionEnabled(""))
        assertTrue(HostWiringPresentation.findQueryActionEnabled("q"))
        assertFalse(HostWiringPresentation.renameProfileActionEnabled("  "))
        assertTrue(HostWiringPresentation.renameProfileActionEnabled(" prod "))
        assertFalse(HostWiringPresentation.copySystemHostsActionEnabled(""))
        assertTrue(HostWiringPresentation.copySystemHostsActionEnabled("/etc/hosts"))
    }

    @Test
    fun contentSearchToggleAndFilteredEmpty() {
        assertFalse(HostWiringPresentation.canToggleContentSearch(applying = true))
        assertTrue(HostWiringPresentation.showFilteredEmpty(profileCount = 0, query = "prod"))
        assertFalse(HostWiringPresentation.showFilteredEmpty(profileCount = 0, query = "  "))
    }

    @Test
    fun runReadImportProfileReadsUtf8() {
        val file = File.createTempFile("host-import-", ".txt").apply {
            writeText("127.0.0.1 localhost\n")
            deleteOnExit()
        }
        val outcome = HostWiringPresentation.runReadImportProfile(file)
        assertTrue(outcome is HostWiringPresentation.ImportProfileOutcome.Success)
        outcome as HostWiringPresentation.ImportProfileOutcome.Success
        assertEquals("127.0.0.1 localhost\n", outcome.content)
        assertEquals(file.nameWithoutExtension, HostWiringPresentation.inferImportProfileName(file, "Untitled"))
    }

    @Test
    fun runWriteExportProfileWritesContent() {
        val file = File.createTempFile("host-export-", ".txt").apply { deleteOnExit() }
        val outcome = HostWiringPresentation.runWriteExportProfile(file, "127.0.0.1 dev.local")
        assertEquals(HostWiringPresentation.ExportProfileOutcome.Success, outcome)
        assertEquals("127.0.0.1 dev.local", file.readText())
    }

    @Test
    fun shouldToastFailures() {
        assertTrue(HostWiringPresentation.shouldToastOperationFailure(IllegalStateException()))
        assertTrue(HostWiringPresentation.shouldToastIoFailure(IllegalStateException()))
    }
}
