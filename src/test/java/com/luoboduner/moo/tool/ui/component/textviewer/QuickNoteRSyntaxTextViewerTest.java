package com.luoboduner.moo.tool.ui.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickNoteRSyntaxTextViewerTest {

    @Test
    void appliesAndClampsLineSpacingFactor() {
        FlatLaf.registerCustomDefaultsSource("themes");
        FlatLightLaf.setup();
        QuickNoteRSyntaxTextViewer viewer = new QuickNoteRSyntaxTextViewer();
        int defaultLineHeight = viewer.getLineHeight();

        viewer.setLineSpacingFactor(1.6f);
        assertEquals(1.6f, viewer.getLineSpacingFactor());
        assertEquals(Math.round(defaultLineHeight * 1.6f), viewer.getLineHeight());

        viewer.setLineSpacingFactor(3.0f);
        assertEquals(2.0f, viewer.getLineSpacingFactor());

        viewer.setLineSpacingFactor(0.5f);
        assertEquals(1.0f, viewer.getLineSpacingFactor());

        viewer.setLineSpacingFactor(Float.NaN);
        assertEquals(1.0f, viewer.getLineSpacingFactor());
    }
}
