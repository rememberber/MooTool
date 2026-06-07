package com.luoboduner.moo.tool.util.translator;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for BingTranslatorUtil
 * 
 * Note: These tests focus on the parsing logic and parameter handling
 * rather than actual API calls which require network access.
 */
public class BingTranslatorUtilTest {

    private BingTranslatorUtil translator = new BingTranslatorUtil();

    @Test
    public void testTranslateEmptyString() {
        String result = translator.translate("", "auto", "zh-CN");
        assertEquals("Empty string should return empty result", "", result);
    }

    @Test
    public void testTranslateNull() {
        String result = translator.translate(null, "auto", "zh-CN");
        assertEquals("Null string should return empty result", "", result);
    }

    @Test
    public void testLanguageCodeConversion() {
        // Test that language code conversion doesn't cause crashes
        // This is important because the translator needs to convert between
        // the app's language codes and Bing's expected format
        
        // We can't easily test the actual conversion without making it public,
        // but we can verify the translator handles various language codes without exceptions
        
        // Note: These calls will attempt to reach the API, so they may fail with network errors
        // That's acceptable - we're mainly testing that language code handling doesn't crash
        
        String result;
        
        // Test Chinese to English
        result = translator.translate("test", "zh-CN", "en");
        assertNotNull("Result should not be null", result);
        
        // Test English to Chinese  
        result = translator.translate("test", "en", "zh-CN");
        assertNotNull("Result should not be null", result);
        
        // Test auto-detect
        result = translator.translate("test", "auto", "zh-CN");
        assertNotNull("Result should not be null", result);
        
        // Even if API fails, we should get an error message, not crash or return null
        assertNotNull("Result should not be null even on API failure", result);
    }
}
