package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** 对齐 Electron `codeEditorLanguage.test.ts`。 */
class TextCodeEditorLanguageTest {
    @Test
    fun mapsMimeTypesUsedByQuickNoteAndHttpBodies() {
        assertEquals(TextCodeEditorLanguage.Json, TextCodeEditorLanguages.resolve("application/json"))
        assertEquals(TextCodeEditorLanguage.Json, TextCodeEditorLanguages.resolve("application/problem+json"))
        assertEquals(TextCodeEditorLanguage.Markdown, TextCodeEditorLanguages.resolve("text/markdown"))
        assertEquals(TextCodeEditorLanguage.Typescript, TextCodeEditorLanguages.resolve("text/typescript"))
        assertEquals(TextCodeEditorLanguage.Xml, TextCodeEditorLanguages.resolve("application/xml"))
        assertEquals(TextCodeEditorLanguage.Html, TextCodeEditorLanguages.resolve("text/html"))
    }

    @Test
    fun acceptsShortRuntimeNamesAndFallsBackToText() {
        assertEquals(TextCodeEditorLanguage.Javascript, TextCodeEditorLanguages.resolve("node"))
        assertEquals(TextCodeEditorLanguage.Python, TextCodeEditorLanguages.resolve("py"))
        assertEquals(TextCodeEditorLanguage.Yaml, TextCodeEditorLanguages.resolve("yml"))
        assertEquals(TextCodeEditorLanguage.Text, TextCodeEditorLanguages.resolve("unknown/type"))
        assertEquals(TextCodeEditorLanguage.Text, TextCodeEditorLanguages.resolve(null))
    }

    @Test
    fun resolvesFromGitDiffPathExtension() {
        assertEquals(TextCodeEditorLanguage.Json, TextCodeEditorLanguages.resolveFromPath("vault/doc.json"))
        assertEquals(TextCodeEditorLanguage.Java, TextCodeEditorLanguages.resolveFromPath("src/Main.java"))
    }
}
