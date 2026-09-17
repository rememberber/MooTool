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

    @Test
    fun formatsMarkdownLikePrettierSamples() {
        val messy =
            "# Title\n\n- item1\n-   item2\n\nparagraph   with   spaces  \n\n## Sub\n\n1. one\n2. two"
        assertEquals(
            "# Title\n\n- item1\n- item2\n\nparagraph with spaces\n\n## Sub\n\n1. one\n2. two\n",
            CodeEditorSurfaceFormatEngine.formatMarkdown(messy),
        )
    }

    @Test
    fun insertsBlankLineAfterHeadingBeforeParagraph() {
        assertEquals(
            "# Title\n\nparagraph\n",
            CodeEditorSurfaceFormatEngine.formatMarkdown("# Title\nparagraph\n"),
        )
    }

    @Test
    fun preservesFencedCodeInternalSpacing() {
        val input = "# Title\n\n```\ncode   here\n```\n\npara   x"
        assertEquals(
            "# Title\n\n```\ncode   here\n```\n\npara x\n",
            CodeEditorSurfaceFormatEngine.formatMarkdown(input),
        )
    }

    @Test
    fun normalizesBlockquoteSpacing() {
        assertEquals(
            "> quote line\n> second\n",
            CodeEditorSurfaceFormatEngine.formatMarkdown("> quote   line\n>   second"),
        )
    }

    @Test
    fun documentFormatEngineRoutesMarkdown() {
        assertEquals(
            "# Hello\n\n- one\n",
            DocumentFormatEngine.format("# Hello\n\n-   one", "text/markdown", "MySQL", 2),
        )
    }
}
