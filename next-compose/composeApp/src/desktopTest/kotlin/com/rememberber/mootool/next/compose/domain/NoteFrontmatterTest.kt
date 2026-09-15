package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteFrontmatterTest {
    @Test
    fun serializesJavaCompatibleQuotedNumbers() {
        val metadata = NoteMetadata(
            title = "API ideas",
            syntax = "text/plain",
            fontName = "PingFang SC",
            fontSize = 15,
            lineSpacing = 1.0,
            color = "default",
            lineWrap = true,
            createdAt = "2026-09-14T00:00:00Z",
            modifiedAt = "2026-09-14T00:00:00Z"
        )
        val raw = NoteFrontmatter.serialize(metadata, "# Hello\nneedle")
        assertTrue(raw.startsWith("---\n"))
        assertTrue(raw.contains("font_name: PingFang SC"))
        assertTrue(raw.contains("font_size: \"15\""))
        assertTrue(raw.contains("line_spacing: \"1.0\""))
        assertTrue(raw.contains("line_wrap: \"1\""))
        val parsed = NoteFrontmatter.parse(raw, "fallback")
        assertEquals("# Hello\nneedle", parsed.content)
        assertEquals("PingFang SC", parsed.metadata.fontName)
        assertEquals(15, parsed.metadata.fontSize)
        assertEquals(1.0, parsed.metadata.lineSpacing)
    }

    @Test
    fun readsLegacyFrontmatterWithoutExposingYamlAsBody() {
        val raw = "---\ntitle: Legacy title\nsyntax: text/markdown\nline_spacing: \"1.6\"\nline_wrap: \"0\"\n---\nlegacy body"
        val parsed = NoteFrontmatter.parse(raw, "Legacy")
        assertEquals("legacy body", parsed.content)
        assertEquals("Legacy title", parsed.metadata.title)
        assertEquals(1.6, parsed.metadata.lineSpacing)
        assertFalse(parsed.metadata.lineWrap)
        assertEquals("text/markdown", parsed.metadata.syntax)
    }

    @Test
    fun mapsSyntaxToElectronCompatibleExtensions() {
        assertEquals("Work/API ideas.md", NoteFrontmatter.withNoteExtension("Work/API ideas.txt", "text/markdown"))
        assertEquals("Config.json", NoteFrontmatter.withNoteExtension("Config.md", "application/json"))
        assertEquals("text/markdown", NoteFrontmatter.syntaxForExtension("md"))
    }
}

class GitIgnoreMatcherTest {
    @Test
    fun hidesDirectoryPatternsAndFileGlobs() {
        val matcher = GitIgnoreMatcher.parse("Private/\n*.tmp\nignored.json\n")
        assertTrue(matcher.ignores("Private", directory = true))
        assertTrue(matcher.ignores("Private/Secret.txt"))
        assertTrue(matcher.ignores("scratch.tmp"))
        assertTrue(matcher.ignores("ignored.json"))
        assertFalse(matcher.ignores("Legacy.txt"))
    }
}
