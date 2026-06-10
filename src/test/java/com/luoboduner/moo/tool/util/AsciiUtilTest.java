package com.luoboduner.moo.tool.util;

import org.junit.Assert;
import org.junit.Test;

public class AsciiUtilTest {

    @Test
    public void toAsciiDecimal() {
        Assert.assertEquals("72 101 108 108 111", AsciiUtil.toAscii("Hello", AsciiUtil.FORMAT_DECIMAL));
    }

    @Test
    public void toAsciiHex() {
        Assert.assertEquals("48 65 6C 6C 6F", AsciiUtil.toAscii("Hello", AsciiUtil.FORMAT_HEX));
    }

    @Test
    public void fromAsciiDecimal() {
        Assert.assertEquals("Hello", AsciiUtil.fromAscii("72 101 108 108 111"));
    }

    @Test
    public void fromAsciiHex() {
        Assert.assertEquals("Hello", AsciiUtil.fromAscii("48 65 6C 6C 6F"));
    }

    @Test
    public void fromAsciiWithPrefix() {
        Assert.assertEquals("A", AsciiUtil.fromAscii("0x41"));
    }

    @Test
    public void emptyInput() {
        Assert.assertEquals("", AsciiUtil.toAscii("", AsciiUtil.FORMAT_DECIMAL));
        Assert.assertEquals("", AsciiUtil.fromAscii(""));
    }
}
