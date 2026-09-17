package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** 对照 Electron `codeEditorFormatting.test.ts` 非 Prettier 路径与 JS 样本。 */
class CodeEditorSurfaceFormatEngineTest {
    @Test
    fun formatsJavascriptLikeElectronSample() {
        assertEquals(
            "const value = { ready: true };",
            CodeEditorSurfaceFormatEngine.formatJavascript("const value={ready:true}"),
        )
    }

    @Test
    fun trimsPlainTextTrailingWhitespace() {
        assertEquals(
            "first\nsecond",
            CodeEditorSurfaceFormatEngine.trimTrailingWhitespace("first  \nsecond\t\n"),
        )
    }

    @Test
    fun formatsPythonByExpandingTabs() {
        assertEquals(
            "    def greet():\n        print(\"moo\")",
            CodeEditorSurfaceFormatEngine.formatPython("\tdef greet():  \n\t\tprint(\"moo\")\t\n", 4),
        )
    }

    @Test
    fun formatsTypescriptLikeJavascriptSurface() {
        assertEquals(
            "type Item = { id: number };",
            CodeEditorSurfaceFormatEngine.formatTypescript("type Item={id:number}"),
        )
    }

    @Test
    fun documentFormatEngineRoutesJavascriptAndPython() {
        assertEquals(
            "const value = { ready: true };",
            DocumentFormatEngine.format("const value={ready:true}", "text/javascript", "MySQL", 2),
        )
        assertEquals(
            "    def greet():\n        print(\"moo\")",
            DocumentFormatEngine.format("\tdef greet():  \n\t\tprint(\"moo\")\t\n", "text/python", "MySQL", 4),
        )
    }
}
