package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolsExportWiringPresentationTest {
    @Test
    fun defaultHostExportFileName_sanitizesProfileName() {
        assertEquals("Prod-Hosts.txt", ToolsExportWiringPresentation.defaultHostExportFileName(" Prod Hosts "))
        assertEquals("hosts.txt", ToolsExportWiringPresentation.defaultHostExportFileName("   "))
    }

    @Test
    fun defaultMergePdfPath_usesExportDirectoryWhenSet() {
        val dir = System.getProperty("java.io.tmpdir")
        val path = ToolsExportWiringPresentation.defaultMergePdfPath(dir)
        assertTrue(path.endsWith("merge.pdf"))
        assertEquals("$dir/merge.pdf".replace("//", "/"), path.replace("//", "/"))
    }

    @Test
    fun defaultSvgPath_buildsUnderExportRoot() {
        val dir = System.getProperty("java.io.tmpdir")
        val path = ToolsExportWiringPresentation.defaultSvgPath(dir, "photo.png")
        assertTrue(path.endsWith("photo.svg"))
    }
}
