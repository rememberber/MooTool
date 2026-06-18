package com.luoboduner.moo.tool.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonPathUtilTest {

    private static final String JSON = "{\"name\":\"test\",\"arr\":[{\"id\":1}],\"num\":42,\"flag\":true,\"nil\":null,\"a.b\":2}";

    @Test
    public void shouldFormatPrimitiveValues() {
        assertEquals("\"test\"", JsonPathUtil.formatValue("test"));
        assertEquals("\"a\\\"b\\n\"", JsonPathUtil.formatValue("a\"b\n"));
        assertEquals("42", JsonPathUtil.formatValue(42));
        assertEquals("true", JsonPathUtil.formatValue(true));
        assertEquals("null", JsonPathUtil.formatValue(null));
    }

    @Test
    public void shouldFormatObjectAndArrayValues() {
        String objectResult = JsonPathUtil.getFormattedValue(JSON, "$.arr[0]");
        assertTrue(objectResult.contains("\"id\""));
        assertTrue(objectResult.contains("1"));

        String arrayResult = JsonPathUtil.getFormattedValue(JSON, "$.arr");
        assertTrue(arrayResult.startsWith("["));
        assertTrue(arrayResult.contains("\"id\""));
    }

    @Test
    public void shouldGetValueByPath() {
        assertEquals("\"test\"", JsonPathUtil.getFormattedValue(JSON, "$.name"));
        assertEquals("2", JsonPathUtil.getFormattedValue(JSON, "$['a.b']"));
        assertEquals("null", JsonPathUtil.getFormattedValue(JSON, "$.nil"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankPath() {
        JsonPathUtil.getByPath(JSON, "  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankJson() {
        JsonPathUtil.getByPath("  ", "$.name");
    }
}
