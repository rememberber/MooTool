package com.rememberber.mootool.nextfx.domain.encode;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encode/decode algorithms aligned with Electron {@code encodeTools.ts}.
 */
public final class EncodeEngine {

    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([\\da-fA-F]{4})");
    private static final Pattern HEX_PAIR = Pattern.compile("[\\da-fA-F]{2}");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,;]+");
    private static final Charset GB2312 = Charset.forName("GB2312");

    public enum Tab {
        UNICODE,
        URL,
        HEX,
        ASCII;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Tab fromId(String id) {
            if (id == null) {
                return UNICODE;
            }
            return switch (id.toLowerCase(Locale.ROOT)) {
                case "url" -> URL;
                case "hex" -> HEX;
                case "ascii" -> ASCII;
                default -> UNICODE;
            };
        }
    }

    public enum Direction {
        FORWARD,
        REVERSE
    }

    public enum UrlCharset {
        UTF_8("utf-8", StandardCharsets.UTF_8),
        GB2312("gb2312", EncodeEngine.GB2312);

        private final String id;
        private final Charset charset;

        UrlCharset(String id, Charset charset) {
            this.id = id;
            this.charset = charset;
        }

        public String id() {
            return id;
        }

        public Charset charset() {
            return charset;
        }

        public static UrlCharset fromId(String id) {
            if (id == null) {
                return UTF_8;
            }
            return "gb2312".equalsIgnoreCase(id) ? GB2312 : UTF_8;
        }
    }

    public enum AsciiFormat {
        DECIMAL,
        HEX;

        public static AsciiFormat fromId(String id) {
            return "hex".equalsIgnoreCase(id) ? HEX : DECIMAL;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private EncodeEngine() {
    }

    public static String convert(Tab tab, Direction direction, String value, UrlCharset charset, AsciiFormat asciiFormat) {
        String input = value == null ? "" : value;
        return switch (tab) {
            case UNICODE -> direction == Direction.FORWARD ? toUnicode(input) : fromUnicode(input);
            case URL -> direction == Direction.FORWARD ? urlEncode(input, charset) : urlDecode(input, charset);
            case HEX -> direction == Direction.FORWARD ? textToHex(input) : hexToText(input);
            case ASCII -> direction == Direction.FORWARD ? textToAscii(input, asciiFormat) : asciiToText(input);
        };
    }

    public static String toUnicode(String value) {
        StringBuilder out = new StringBuilder();
        value.codePoints().forEach(codePoint -> {
            if (codePoint <= 0x7f) {
                out.appendCodePoint(codePoint);
            } else if (codePoint <= 0xffff) {
                out.append("\\u").append(String.format("%04x", codePoint));
            } else {
                int offset = codePoint - 0x10000;
                int high = 0xd800 + (offset >> 10);
                int low = 0xdc00 + (offset & 0x3ff);
                out.append("\\u").append(Integer.toHexString(high));
                out.append("\\u").append(Integer.toHexString(low));
            }
        });
        return out.toString();
    }

    public static String fromUnicode(String value) {
        Matcher matcher = UNICODE_ESCAPE.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    String.valueOf((char) Integer.parseInt(matcher.group(1), 16))));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static String urlEncode(String value, UrlCharset charset) {
        byte[] bytes = value.getBytes(charset.charset());
        StringBuilder out = new StringBuilder(bytes.length * 3);
        for (byte raw : bytes) {
            int unsigned = raw & 0xff;
            if (isUnreserved(unsigned)) {
                out.append((char) unsigned);
            } else {
                out.append('%');
                out.append(String.format("%02X", unsigned));
            }
        }
        return out.toString();
    }

    public static String urlDecode(String value, UrlCharset charset) {
        String source = value.replace('+', ' ');
        List<Byte> bytes = new ArrayList<>();
        for (int index = 0; index < source.length(); ) {
            if (source.charAt(index) == '%'
                    && index + 2 < source.length()
                    && HEX_PAIR.matcher(source.substring(index + 1, index + 3)).matches()) {
                bytes.add((byte) Integer.parseInt(source.substring(index + 1, index + 3), 16));
                index += 3;
            } else {
                for (byte raw : String.valueOf(source.charAt(index)).getBytes(charset.charset())) {
                    bytes.add(raw);
                }
                index += 1;
            }
        }
        byte[] array = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            array[i] = bytes.get(i);
        }
        return new String(array, charset.charset());
    }

    public static String textToHex(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte raw : bytes) {
            out.append(String.format("%02x", raw & 0xff));
        }
        return out.toString();
    }

    public static String hexToText(String value) {
        String normalized = value.replaceAll("[\\s:_-]+", "");
        if (normalized.isEmpty() || normalized.length() % 2 != 0 || !normalized.matches("[\\da-fA-F]+")) {
            throw new EncodeException("encode.error.invalidHex", "");
        }
        byte[] bytes = new byte[normalized.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(normalized.substring(i * 2, i * 2 + 2), 16);
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new EncodeException("encode.error.invalidHex", exception.getMessage());
        }
    }

    public static String textToAscii(String value, AsciiFormat format) {
        StringBuilder out = new StringBuilder();
        value.codePoints().forEach(code -> {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(format == AsciiFormat.HEX
                    ? Integer.toHexString(code).toUpperCase(Locale.ROOT)
                    : Integer.toString(code));
        });
        return out.toString();
    }

    public static String asciiToText(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String part : TOKEN_SPLIT.split(trimmed)) {
            if (part.isEmpty()) {
                continue;
            }
            boolean hex = part.regionMatches(true, 0, "0x", 0, 2) || part.matches("(?i).*[a-f].*");
            String normalized = part.replaceFirst("(?i)^0x", "");
            int codePoint;
            try {
                codePoint = Integer.parseInt(normalized, hex ? 16 : 10);
            } catch (NumberFormatException exception) {
                throw new EncodeException("encode.error.invalidCodePoint", part);
            }
            if (codePoint < 0 || codePoint > 0x10ffff) {
                throw new EncodeException("encode.error.invalidCodePoint", part);
            }
            out.appendCodePoint(codePoint);
        }
        return out.toString();
    }

    private static boolean isUnreserved(int unsignedByte) {
        return (unsignedByte >= 0x41 && unsignedByte <= 0x5a)
                || (unsignedByte >= 0x61 && unsignedByte <= 0x7a)
                || (unsignedByte >= 0x30 && unsignedByte <= 0x39)
                || unsignedByte == 0x2d
                || unsignedByte == 0x2e
                || unsignedByte == 0x5f
                || unsignedByte == 0x7e;
    }
}
