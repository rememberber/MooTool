package com.luoboduner.moo.tool.util;

/**
 * ASCII 字符码转换工具
 */
public class AsciiUtil {

    public static final String FORMAT_DECIMAL = "十进制";
    public static final String FORMAT_HEX = "十六进制";

    private AsciiUtil() {
    }

    public static String toAscii(String text, String format) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        boolean hex = FORMAT_HEX.equals(format);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (hex) {
                sb.append(Integer.toHexString(codePoint).toUpperCase());
            } else {
                sb.append(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return sb.toString();
    }

    public static String fromAscii(String ascii) {
        if (ascii == null || ascii.trim().isEmpty()) {
            return "";
        }
        String[] parts = ascii.trim().split("[\\s,;]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            int codePoint = parseCodePoint(part);
            sb.appendCodePoint(codePoint);
        }
        return sb.toString();
    }

    private static int parseCodePoint(String part) {
        String value = part.trim();
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.parseInt(value.substring(2), 16);
        }
        if (value.matches("(?i)[0-9a-f]+") && value.matches(".*[a-fA-F].*")) {
            return Integer.parseInt(value, 16);
        }
        return Integer.parseInt(value, 10);
    }
}
