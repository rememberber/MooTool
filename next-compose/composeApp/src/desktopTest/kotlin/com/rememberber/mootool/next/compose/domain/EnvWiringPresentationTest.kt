package com.rememberber.mootool.next.compose.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvWiringPresentationTest {
    @Test
    fun refreshBlockedWhileLoadingOrSaving() {
        assertFalse(EnvWiringPresentation.refreshEnabled(loading = true, saving = false))
        assertFalse(EnvWiringPresentation.refreshEnabled(loading = false, saving = true))
        assertTrue(EnvWiringPresentation.refreshEnabled(loading = false, saving = false))
    }

    @Test
    fun exportRequiresSnapshot() {
        assertFalse(EnvWiringPresentation.exportEnabled(hasSnapshot = false))
        assertTrue(EnvWiringPresentation.exportEnabled(hasSnapshot = true))
    }

    @Test
    fun saveEditorRequiresNonBlankKey() {
        assertFalse(EnvWiringPresentation.saveEditorEnabled(trimmedKey = "", saving = false))
        assertFalse(EnvWiringPresentation.saveEditorEnabled(trimmedKey = "A", saving = true))
        assertTrue(EnvWiringPresentation.saveEditorEnabled(trimmedKey = "A", saving = false))
    }

    @Test
    fun deleteBlockedWhileSaving() {
        assertFalse(EnvWiringPresentation.deleteRowEnabled(canDelete = true, saving = true))
        assertFalse(EnvWiringPresentation.confirmDeleteEnabled(saving = true))
        assertTrue(EnvWiringPresentation.deleteRowEnabled(canDelete = true, saving = false))
    }

    @Test
    fun shouldToastIoFailure() {
        assertTrue(EnvWiringPresentation.shouldToastIoFailure(IllegalStateException()))
    }

    @Test
    fun runWriteExportWritesUtf8() {
        val dir = File.createTempFile("env-export-", ".dir").apply { delete(); mkdirs() }
        try {
            val file = File(dir, "env.txt")
            val snapshot =
                EnvSnapshot(
                    process = emptyList(),
                    runtime = emptyList(),
                    user = listOf(EnvEntry("A", "1")),
                    system = emptyList(),
                    userFile = "/data/environment",
                    systemFile = "/etc/zshenv",
                    shellProfile = "/home/u/.zshenv",
                )
            val outcome = EnvWiringPresentation.runWriteExport(file, snapshot)
            assertTrue(outcome is EnvWiringPresentation.ExportOutcome.Success)
            assertTrue(file.readText().contains("A=1"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun shouldToastOperationFailure() {
        assertTrue(EnvWiringPresentation.shouldToastOperationFailure(IllegalStateException()))
    }
}
