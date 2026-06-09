package com.luoboduner.moo.tool.util.translator;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoogleTranslatorUtilTest {

    @Test
    public void testSplitTextKeepsShortTextAsSingleChunk() {
        List<String> chunks = GoogleTranslatorUtil.splitText("hello");
        assertEquals(1, chunks.size());
        assertEquals("hello", chunks.get(0));
    }

    @Test
    public void testSplitTextSplitsLongParagraph() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            longText.append('a');
        }

        List<String> chunks = GoogleTranslatorUtil.splitText(longText.toString());
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().mapToInt(String::length).max().orElse(0) <= 1800);
    }

    @Test
    public void testSplitTextPreservesNewlinesWhenPossible() {
        String text = "line one\nline two\nline three";
        List<String> chunks = GoogleTranslatorUtil.splitText(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }
}
