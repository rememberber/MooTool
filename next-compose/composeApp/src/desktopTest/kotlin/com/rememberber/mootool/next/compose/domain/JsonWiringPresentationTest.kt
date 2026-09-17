package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonWiringPresentationTest {
    private val t = JsonTranslator { key, params -> Translator(AppLanguage.ZhCN).t(key, params) }

    @Test
    fun runValidateIdleWhenBlank() {
        assertEquals(JsonStatus.Kind.Idle, JsonWiringPresentation.runValidate("", t).kind)
    }

    @Test
    fun runQuickFormatPrettyPrints() {
        val outcome = JsonWiringPresentation.runQuickFormat("""{"a":1}""", t, 2)
        assertTrue(outcome is JsonWiringPresentation.TransformOutcome.Success)
        assertTrue((outcome as JsonWiringPresentation.TransformOutcome.Success).output.contains("\n"))
    }

    @Test
    fun runQueryPathReturnsValue() {
        val input = """{"x":571}"""
        val outcome = JsonWiringPresentation.runQueryPath(input, "$.x", t)
        assertTrue(outcome is JsonWiringPresentation.TransformOutcome.Success)
        assertTrue((outcome as JsonWiringPresentation.TransformOutcome.Success).output.contains("571"))
    }

    @Test
    fun runQueryPathFailsOnInvalidPathSyntax() {
        val outcome = JsonWiringPresentation.runQueryPath("{}", "$.[", t)
        assertTrue(outcome is JsonWiringPresentation.TransformOutcome.Failure)
    }

    @Test
    fun runReadImportAndWriteExportRoundTrip() {
        val dir = File.createTempFile("json-io-", ".dir").apply { delete(); mkdirs() }
        try {
            val source = File(dir, "in.json")
            source.writeText("""{"a":576}""")
            val read = JsonWiringPresentation.runReadImportFile(source)
            assertTrue(read is JsonWiringPresentation.ImportOutcome.Success)
            assertEquals("""{"a":576}""", (read as JsonWiringPresentation.ImportOutcome.Success).content)
            val target = File(dir, "out.json")
            val write = JsonWiringPresentation.runWriteExportFile(target, """{"b":1}""")
            assertTrue(write is JsonWiringPresentation.WriteExportOutcome.Success)
            assertEquals("""{"b":1}""", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }
}
