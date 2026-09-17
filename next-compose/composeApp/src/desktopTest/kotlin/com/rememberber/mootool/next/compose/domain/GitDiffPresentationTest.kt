package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitDiffPresentationTest {
    @Test
    fun previewMessageKeyForNonTextDiffs() {
        assertEquals("git.diffBinary", GitDiffPresentation.previewMessageKey(GitDiffPreview.Binary))
        assertEquals("git.diffTooLarge", GitDiffPresentation.previewMessageKey(GitDiffPreview.TooLarge))
        assertEquals(null, GitDiffPresentation.previewMessageKey(GitDiffPreview.Text))
    }

    @Test
    fun languageFromRenamedPathUsesOriginalExtension() {
        val diff = GitFileDiff(
            path = "c.md",
            originalPath = "b.java",
            status = "R100",
            before = "",
            after = "",
        )
        assertEquals(TextCodeEditorLanguage.Java, GitDiffPresentation.languageForFile(diff))
    }

    @Test
    fun filePickerAndSideBySideFlags() {
        assertFalse(GitDiffPresentation.showFilePicker(1))
        assertTrue(GitDiffPresentation.showFilePicker(2))
        assertTrue(GitDiffPresentation.showSideBySide(GitDiffPreview.Text))
        assertFalse(GitDiffPresentation.showSideBySide(GitDiffPreview.Binary))
    }

    @Test
    fun rstaSyntaxFromGitDiffPath() {
        val diff = GitFileDiff(path = "src/App.ts", before = "", after = "")
        assertEquals(
            org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT,
            GitDiffPresentation.rstaSyntaxForFile(diff),
        )
    }
}
