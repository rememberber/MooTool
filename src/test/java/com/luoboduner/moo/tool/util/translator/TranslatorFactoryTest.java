package com.luoboduner.moo.tool.util.translator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslatorFactoryTest {

    @Test
    public void testParseTypeDefaultsToGoogle() {
        assertEquals(TranslatorFactory.TranslatorType.GOOGLE, TranslatorFactory.parseType(null));
        assertEquals(TranslatorFactory.TranslatorType.GOOGLE, TranslatorFactory.parseType(""));
        assertEquals(TranslatorFactory.TranslatorType.GOOGLE, TranslatorFactory.parseType("UNKNOWN"));
        assertEquals(TranslatorFactory.TranslatorType.GOOGLE, TranslatorFactory.parseType("MICROSOFT"));
    }

    @Test
    public void testParseTypeRecognizesBing() {
        assertEquals(TranslatorFactory.TranslatorType.BING, TranslatorFactory.parseType("BING"));
    }
}
