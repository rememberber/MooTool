package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ColumnEditEngineTest {
    @Test
    fun insertsAcrossShortLinesAndPadsVisualColumns() {
        val inserted = ColumnEditEngine.insert("ab\n\nxyz", 0, 2, 2, "Q", 4)
        assertEquals(listOf("abQ", "  Q", "xyQz"), ColumnEditEngine.splitKeepLast(inserted))
    }

    @Test
    fun pastesRectangleAndDeletesTheSameRange() {
        val pasted = ColumnEditEngine.paste("one\ntwo\nthree", 0, 1, "A\nB\nC", 4)
        assertEquals("oAne\ntBwo\ntChree", pasted)
        val deleted = ColumnEditEngine.delete(pasted, 0, 2, 1, 2, 4)
        assertEquals("one\ntwo\nthree", deleted)
    }

    @Test
    fun typesIntoEachLineLikeElectronQuickNote() {
        val typed = ColumnEditEngine.replace("aa\nbb", ColumnRange(0, 1, 0, 0), "x", 2)
        assertEquals("xaa\nxbb", typed)
    }

    @Test
    fun expandsTabsWhenMeasuringVisualColumns() {
        assertEquals(4, ColumnEditEngine.visualColumn("\tA", 1, 4))
        assertEquals(1, ColumnEditEngine.indexOfVisualColumn("\tA", 4, 4))
    }

    @Test
    fun doesNotSplitEmojiOrCombiningMarks() {
        val smile = "a\uD83D\uDE00b"
        val inserted = ColumnEditEngine.insertAtVisualColumn(smile, 2, "X", 4)
        assertEquals("a\uD83D\uDE00Xb", inserted)
        val combining = ColumnEditEngine.delete("e\u0301x", 0, 0, 0, 1, 4)
        assertEquals("x", combining)
    }

    @Test
    fun treatsIdeographsAsTwoColumnsAndDeletesWholeGlyph() {
        assertEquals(2, ColumnEditEngine.visualColumn("中a", 1, 4))
        val deleted = ColumnEditEngine.delete("中a\n中a", 0, 1, 0, 2, 4)
        assertEquals("a\na", deleted)
        val extracted = ColumnEditEngine.extract("中a\nxy", ColumnRange(0, 1, 0, 2), 4)
        assertEquals("中\nxy", extracted)
    }

    @Test
    fun singleLinePasteGoesToOneRowAndBlockPasteAddsLines() {
        assertEquals("aXbc", ColumnEditEngine.paste("abc", 0, 1, "X", 4))
        val block = ColumnEditEngine.paste("a\nb", 0, 1, "1\n2\n3", 4)
        assertEquals("a1\nb2\n 3", block)
    }

    @Test
    fun pastePadsShorterLinesToTheVisualColumn() {
        val pasted = ColumnEditEngine.paste("ab\nc", 0, 4, "X\nY", 4)
        assertEquals("ab  X\nc   Y", pasted)
    }

    @Test
    fun pasteIntoSelectionRepeatsSingleLineAcrossRowsAndClearsRange() {
        val filled = ColumnEditEngine.pasteIntoSelection("aa\nbb\ncc", ColumnRange(0, 2, 1, 1), "Z", 4)
        assertEquals("aZa\nbZb\ncZc", filled)
        val replaced = ColumnEditEngine.pasteIntoSelection("abcd\nefgh", ColumnRange(0, 1, 1, 3), "X\nY", 4)
        assertEquals("aXd\neYh", replaced)
    }
}
