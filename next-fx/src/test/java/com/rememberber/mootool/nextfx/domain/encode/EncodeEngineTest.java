package com.rememberber.mootool.nextfx.domain.encode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncodeEngineTest {

    @Test
    void roundTripsUnicodeAndUtf8Hex() {
        assertThat(EncodeEngine.fromUnicode(EncodeEngine.toUnicode("Moo 工具 🚀"))).isEqualTo("Moo 工具 🚀");
        assertThat(EncodeEngine.hexToText(EncodeEngine.textToHex("你好 Moo"))).isEqualTo("你好 Moo");
        assertThat(EncodeEngine.toUnicode("Moo 工具 🚀")).contains("\\u5de5").contains("\\ud83d").contains("\\ude80");
    }

    @Test
    void roundTripsUrlTextInUtf8AndGb2312() {
        assertThat(EncodeEngine.urlDecode(
                EncodeEngine.urlEncode("你好 a/b", EncodeEngine.UrlCharset.UTF_8),
                EncodeEngine.UrlCharset.UTF_8
        )).isEqualTo("你好 a/b");
        assertThat(EncodeEngine.urlEncode("你好 a/b", EncodeEngine.UrlCharset.UTF_8))
                .isEqualTo("%E4%BD%A0%E5%A5%BD%20a%2Fb");
        assertThat(EncodeEngine.urlDecode(
                EncodeEngine.urlEncode("编码测试", EncodeEngine.UrlCharset.GB2312),
                EncodeEngine.UrlCharset.GB2312
        )).isEqualTo("编码测试");
    }

    @Test
    void convertsUnicodeCodePointsToAndFromDecimalOrHexAsciiLists() {
        assertThat(EncodeEngine.textToAscii("A中", EncodeEngine.AsciiFormat.DECIMAL)).isEqualTo("65 20013");
        assertThat(EncodeEngine.textToAscii("A中", EncodeEngine.AsciiFormat.HEX)).isEqualTo("41 4E2D");
        assertThat(EncodeEngine.asciiToText("65 4E2D")).isEqualTo("A中");
        assertThat(EncodeEngine.convert(
                EncodeEngine.Tab.ASCII,
                EncodeEngine.Direction.REVERSE,
                "65,0x4E2D",
                EncodeEngine.UrlCharset.UTF_8,
                EncodeEngine.AsciiFormat.DECIMAL
        )).isEqualTo("A中");
    }

    @Test
    void rejectsInvalidHexAndCodePoints() {
        assertThatThrownBy(() -> EncodeEngine.hexToText("zzz"))
                .isInstanceOf(EncodeException.class)
                .satisfies(error -> assertThat(((EncodeException) error).messageKey()).isEqualTo("encode.error.invalidHex"));
        assertThatThrownBy(() -> EncodeEngine.asciiToText("1114112"))
                .isInstanceOf(EncodeException.class)
                .satisfies(error -> assertThat(((EncodeException) error).messageKey()).isEqualTo("encode.error.invalidCodePoint"));
    }
}
