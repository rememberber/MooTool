package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteListEngineTest {
    @Test
    fun prefixesWholeLinesAroundSelectionLikeElectron() {
        val text = "alpha\nbeta\ngamma"
        val bullet = NoteListEngine.prefixSelectedLines(text, 6, 10, NoteListEngine.Prefix.Bullet)
        assertEquals("alpha\n- beta\ngamma", bullet.text)
        val numbered = NoteListEngine.prefixSelectedLines("one\ntwo", 0, 7, NoteListEngine.Prefix.Numbered)
        assertEquals("1. one\n2. two", numbered.text)
        val empty = NoteListEngine.prefixSelectedLines("", 0, 0, NoteListEngine.Prefix.Bullet)
        assertEquals("- ", empty.text)
    }
}

class NoteColorsTest {
    @Test
    fun keepsElectronSwatchesAndDefaultDoesNotTintTree() {
        assertEquals("coral", NoteColors.normalize("Coral"))
        assertEquals("default", NoteColors.normalize("unknown"))
        assertFalse(NoteColors.tintsTree("default"))
        assertTrue(NoteColors.tintsTree("blue"))
        assertEquals(0xFF4F83CC.toInt(), NoteColors.argb("blue"))
    }
}
