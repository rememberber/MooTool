package com.rememberber.mootool.nextfx.domain.editor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnEditsTest {

    @Test
    void insertsAcrossShortLinesAndPadsVisualColumns() {
        String text = "ab\n\nxyz";
        String inserted = ColumnEdits.insert(text, 0, 2, 2, "Q", 4);
        assertThat(inserted.split("\n", -1)).containsExactly("abQ", "  Q", "xyQz");
    }

    @Test
    void pastesRectangleAndUndoesByDeletingTheSameRange() {
        String text = "one\ntwo\nthree";
        String pasted = ColumnEdits.paste(text, 0, 1, "A\nB\nC", 4);
        assertThat(pasted).isEqualTo("oAne\ntBwo\ntChree");
        String deleted = ColumnEdits.delete(pasted, 0, 2, 1, 2, 4);
        assertThat(deleted).isEqualTo("one\ntwo\nthree");
    }

    @Test
    void expandsTabsWhenMeasuringVisualColumns() {
        assertThat(ColumnEdits.visualColumn("\tA", 1, 4)).isEqualTo(4);
    }
}
