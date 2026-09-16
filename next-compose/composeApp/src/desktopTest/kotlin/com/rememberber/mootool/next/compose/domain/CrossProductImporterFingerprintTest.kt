package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossProductImporterFingerprintTest {
    @Test
    fun applySkipsWhenLegacyElectronFingerprintAlreadyRecorded() {
        val directories = AppPaths.resolve(createTempDirectory("import-fp-").toString()).also { it.ensureCreated() }
        val preview = ImportPreview(
            sourceKind = "java-sqlite",
            fingerprint = "compose-content-fingerprint",
            notes = 0,
            jsonItems = 0,
            customGroups = 0,
            databaseFound = false,
            configFound = false,
            warnings = emptyList(),
            noteFiles = emptyList(),
            jsonFiles = emptyList(),
            groups = emptyList(),
            sqliteNotes = emptyList(),
            sqliteJson = emptyList(),
            legacyElectronFingerprint = "electron-legacy-fingerprint"
        )
        val result = CrossProductImporter.apply(
            preview,
            NoteVault(directories),
            JsonVault(directories),
            importedFingerprints = setOf("electron-legacy-fingerprint")
        )
        assertEquals(0, result.importedNotes)
        assertEquals(0, result.importedJson)
        assertTrue(CrossProductImporter.importAlreadyRecorded(preview, setOf("electron-legacy-fingerprint")))
    }
}
