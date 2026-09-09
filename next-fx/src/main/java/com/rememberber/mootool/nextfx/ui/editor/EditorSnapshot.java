package com.rememberber.mootool.nextfx.ui.editor;

public record EditorSnapshot(
        String documentId,
        long revision,
        String text,
        int caret,
        int anchor,
        int firstVisibleParagraph,
        double horizontalOffset,
        boolean wrap
) {
}
