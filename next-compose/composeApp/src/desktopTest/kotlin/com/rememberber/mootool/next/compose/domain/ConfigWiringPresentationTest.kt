package com.rememberber.mootool.next.compose.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigWiringPresentationTest {
    @Test
    fun convertDirectionsRequireSource() {
        assertFalse(ConfigWiringPresentation.canToYaml(""))
        assertTrue(ConfigWiringPresentation.canToYaml("a=b"))
        assertFalse(ConfigWiringPresentation.canToProperties(""))
        assertTrue(ConfigWiringPresentation.canToProperties("a: b"))
    }

    @Test
    fun runToYamlRoundTrip() {
        val outcome = ConfigWiringPresentation.runToYaml("a=b")
        assertTrue(outcome is ConfigWiringPresentation.ConvertOutcome.Success)
        val yaml = (outcome as ConfigWiringPresentation.ConvertOutcome.Success).output
        assertTrue(yaml.contains("a:"))
        val back = ConfigWiringPresentation.runToProperties(yaml)
        assertTrue(back is ConfigWiringPresentation.ConvertOutcome.Success)
        assertTrue((back as ConfigWiringPresentation.ConvertOutcome.Success).output.contains("a=b"))
    }

    @Test
    fun runValidateAndFormat() {
        assertTrue(ConfigWiringPresentation.runValidate("a: 1").valid)
        assertFalse(ConfigWiringPresentation.runValidate("a: [").valid)
        val formatted = ConfigWiringPresentation.runFormat("a: 1\nb: 2")
        assertTrue(formatted is ConfigWiringPresentation.ConvertOutcome.Success)
    }

    @Test
    fun runReadImportAndWriteExportRoundTrip() {
        val dir = File.createTempFile("config-io-", ".dir").apply { delete(); mkdirs() }
        try {
            val source = File(dir, "in.properties")
            source.writeText("x=y")
            val read = ConfigWiringPresentation.runReadImportFile(source)
            assertTrue(read is ConfigWiringPresentation.ImportOutcome.Success)
            assertEquals("x=y", (read as ConfigWiringPresentation.ImportOutcome.Success).content)
            val target = File(dir, "out.properties")
            val write = ConfigWiringPresentation.runWriteExportFile(target, "z=1")
            assertTrue(write is ConfigWiringPresentation.WriteExportOutcome.Success)
            assertEquals("z=1", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun runReadImportFileMissingFileFails() {
        val file = File.createTempFile("config-missing-", ".properties")
        file.delete()
        val outcome = ConfigWiringPresentation.runReadImportFile(file)
        assertTrue(outcome is ConfigWiringPresentation.ImportOutcome.Failure)
    }

    @Test
    fun runWriteExportFileMissingParentFails() {
        val dir = File.createTempFile("config-write-", ".dir").apply { delete() }
        val target = File(dir, "nested/out.properties")
        val outcome = ConfigWiringPresentation.runWriteExportFile(target, "a=b")
        assertTrue(outcome is ConfigWiringPresentation.WriteExportOutcome.Failure)
    }

    @Test
    fun shouldToastFailures() {
        assertTrue(ConfigWiringPresentation.shouldToastConvertFailure(IllegalStateException()))
        assertTrue(ConfigWiringPresentation.shouldToastIoFailure(IllegalStateException()))
    }
}
