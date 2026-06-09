package com.luoboduner.moo.tool.util.translator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for BingTranslatorUtil
 */
public class BingTranslatorUtilTest {

    private final BingTranslatorUtil translator = new BingTranslatorUtil();

    @Test
    public void testTranslateEmptyString() {
        String result = translator.translate("", "auto", "zh-CN");
        assertEquals("", result);
    }

    @Test
    public void testTranslateNull() {
        String result = translator.translate(null, "auto", "zh-CN");
        assertEquals("", result);
    }

    @Test
    public void testParseSessionFromPage() {
        String page = "<script>var _G={IG:\"CE86760F79114B048DE00FB3AB4D0B12\"};" +
                "params_AbusePreventionHelper = [1781010085450,\"McgxNajxoyup0SKv8fPExQtAQfGc1Gv1\",3600000];</script>";

        BingTranslatorUtil.BingSession session = BingTranslatorUtil.parseSessionFromPage(page);

        assertEquals("CE86760F79114B048DE00FB3AB4D0B12", session.ig);
        assertEquals(1781010085450L, session.key);
        assertEquals("McgxNajxoyup0SKv8fPExQtAQfGc1Gv1", session.token);
        assertTrue(session.expireAt > System.currentTimeMillis());
    }

    @Test
    public void testLanguageCodeConversion() {
        assertEquals("auto-detect", translator.convertToBingLanguageCode("auto"));
        assertEquals("zh-Hans", translator.convertToBingLanguageCode("zh-CN"));
        assertEquals("zh-Hant", translator.convertToBingLanguageCode("cht"));
        assertEquals("ja", translator.convertToBingLanguageCode("jp"));
        assertEquals("ko", translator.convertToBingLanguageCode("kor"));
        assertEquals("fr", translator.convertToBingLanguageCode("fra"));
        assertEquals("vi", translator.convertToBingLanguageCode("vie"));
    }

    @Test
    public void testLanguageCodeConversionDoesNotCrash() {
        String result = translator.translate("test", "zh-CN", "en");
        assertNotNull(result);
    }
}
