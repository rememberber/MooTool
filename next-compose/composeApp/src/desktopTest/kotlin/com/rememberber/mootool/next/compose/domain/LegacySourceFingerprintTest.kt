package com.rememberber.mootool.next.compose.domain

import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LegacySourceFingerprintTest {
    @Test
    fun fingerprintIsDeterministicForLegacyLayout() {
        val root = createTempDirectory("legacy-fp-")
        root.resolve("config").createDirectories()
        root.resolve("config/config.setting").writeText("[func.quickNote]\nquickNoteVaultPath=quick-notes\n")
        root.resolve("MooTool.db").writeText("sqlite-placeholder")
        root.resolve("quick-notes").createDirectories()
        root.resolve("quick-notes/note.md").writeText("# hello")
        root.resolve("json-beauty").createDirectories()
        root.resolve("json-beauty/sample.json").writeText("{}")

        val paths = LegacyJavaDataPaths.resolve(root, LegacyJavaSettings.parse(root.resolve("config/config.setting").readText()))
        val inputs = listOf(
            paths.databasePath,
            paths.configPath,
            paths.quickNoteVaultPath,
            paths.jsonVaultPath
        )
        val first = LegacySourceFingerprint.fingerprint(root, inputs)
        val second = LegacySourceFingerprint.fingerprint(root, inputs)
        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun inspectExposesLegacyElectronFingerprint() {
        val source = createTempDirectory("legacy-fp-inspect-")
        source.resolve("config").createDirectories()
        source.resolve("config/config.setting").writeText("")
        source.resolve("MooTool.db").writeText("x")
        val preview = CrossProductImporter.inspect(source)
        assertNotNull(preview.legacyElectronFingerprint)
        assertEquals(64, preview.legacyElectronFingerprint!!.length)
    }
}
