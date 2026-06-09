package com.luoboduner.moo.tool.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TranslationWordBookUtilTest {

    @Test
    public void testPreviewTextShort() {
        assertEquals("hello", TranslationWordBookUtil.previewText("hello", 10));
    }

    @Test
    public void testPreviewTextTruncatesLongText() {
        String result = TranslationWordBookUtil.previewText("abcdefghijklmnopqrstuvwxyz", 10);
        assertTrue(result.endsWith("..."));
        assertEquals(13, result.length());
    }

    @Test
    public void testPreviewTextReplacesNewlines() {
        assertEquals("line one line two", TranslationWordBookUtil.previewText("line one\nline two", 40));
    }
}
