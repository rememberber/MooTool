package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.RuntimeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeRunWiringPresentationTest {
    @Test
    fun pathsFromMapsRuntimeSettings() {
        val settings = RuntimeSettings(javaPath = "/j", groovyPath = "/g", pythonPath = "/p", nodePath = "/n")
        val paths = CodeRunWiringPresentation.pathsFrom(settings)
        assertEquals("/j", paths.java)
        assertEquals("/n", paths.node)
    }

    @Test
    fun availableCountAndRunGuard() {
        val statuses = listOf(
            CodeRuntimeStatus(CodeRuntime.Java, true, "java", "17"),
            CodeRuntimeStatus(CodeRuntime.Python, false, "python3", ""),
        )
        assertEquals(1, CodeRunWiringPresentation.availableCount(statuses))
        assertTrue(CodeRunWiringPresentation.canRun(statuses.first()))
        assertFalse(CodeRunWiringPresentation.showConfigureBanner(statuses.first()))
        assertFalse(CodeRunWiringPresentation.canRun(statuses[1]))
        assertTrue(CodeRunWiringPresentation.showConfigureBanner(statuses[1]))
        assertTrue(CodeRunWiringPresentation.canRun(null))
    }

    @Test
    fun parseRunArgumentsUsesEngine() {
        val parsed = CodeRunWiringPresentation.parseRunArguments("""--name "Moo"""")
        assertTrue(parsed is CodeRunWiringPresentation.ArgumentsOutcome.Success)
        assertEquals(listOf("--name", "Moo"), (parsed as CodeRunWiringPresentation.ArgumentsOutcome.Success).arguments)
    }
}
