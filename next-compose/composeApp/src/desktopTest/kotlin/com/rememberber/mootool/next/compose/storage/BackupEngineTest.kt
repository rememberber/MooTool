package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.model.AppSettings
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupEngineTest {
    @Test
    fun exportsAndRestoresSettingsDatabaseAndVault() {
        val root = Files.createTempDirectory("mootool-compose-backup-src")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        SettingsRepository(directories).save(AppSettings.Default.copy(general = AppSettings.Default.general.copy(language = "en-US")))
        AppDatabase(directories).use { database ->
            HistoryRepository(database).save("quickNote", "save", "hello", "body", "")
        }
        val note = directories.quickNoteVault.resolve("hello.md")
        note.writeText("# hello")
        val zip = Files.createTempDirectory("mootool-compose-backup-zip").resolve("backup.zip")
        val exported = BackupEngine.export(directories, zip)
        assertEquals(ProductIdentity.PRODUCT_ID, exported.manifest.productId)
        assertTrue(exported.manifest.files.any { it.path.endsWith("settings.json") })
        assertTrue(exported.manifest.files.any { it.path.contains("hello.md") })
        val preview = BackupEngine.preview(zip)
        assertEquals(exported.manifest.files.size, preview.files.size)

        SettingsRepository(directories).save(AppSettings.Default.copy(general = AppSettings.Default.general.copy(language = "zh-CN")))
        note.writeText("# changed")
        AppDatabase(directories).use { database ->
            HistoryRepository(database).save("quickNote", "save", "other", "x", "")
        }

        val restored = BackupEngine.restore(zip, directories)
        assertTrue(Files.exists(restored.safetyBackup))
        assertTrue(SettingsRepository(directories).load().general.language == "en-US")
        assertEquals("# hello", note.readText())
        AppDatabase(directories).use { database ->
            val items = HistoryRepository(database).list("quickNote")
            assertTrue(items.any { it.summary == "hello" })
            assertTrue(items.none { it.summary == "other" })
        }
    }

    @Test
    fun rejectsPathEscapeAndWrongProduct() {
        val root = Files.createTempDirectory("mootool-compose-backup-bad")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        SettingsRepository(directories).save(AppSettings.Default)
        val zip = Files.createTempDirectory("mootool-compose-backup-bad-zip").resolve("evil.zip")
        ZipOutputStream(Files.newOutputStream(zip)).use { stream ->
            stream.putNextEntry(ZipEntry("../secret.txt"))
            stream.write("nope".toByteArray())
            stream.closeEntry()
        }
        val escaped = assertFailsWith<BackupException> { BackupEngine.preview(zip) }
        assertEquals(BackupErrorCode.PATH_ESCAPE, escaped.code)

        val missing = Files.createTempDirectory("mootool-compose-backup-missing").resolve("empty.zip")
        ZipOutputStream(Files.newOutputStream(missing)).use { stream ->
            stream.putNextEntry(ZipEntry("readme.txt"))
            stream.write("x".toByteArray())
            stream.closeEntry()
        }
        val invalid = assertFailsWith<BackupException> { BackupEngine.preview(missing) }
        assertEquals(BackupErrorCode.INVALID_ARCHIVE, invalid.code)
    }
}
