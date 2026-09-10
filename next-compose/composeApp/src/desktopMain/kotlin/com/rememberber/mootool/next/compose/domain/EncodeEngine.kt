package com.rememberber.mootool.next.compose.domain

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

enum class EncodeTab { Unicode, Url, Hex, Ascii }

enum class UrlCharset { Utf8, Gb2312 }

enum class AsciiFormat { Decimal, Hex }

class EncodeException(val code: String, message: String) : RuntimeException(message)

object EncodeEngine {
    private val unicodeEscape = Regex("""\\u([0-9a-fA-F]{4})""")
    private val hexPair = Regex("..")
    private val asciiParts = Regex("""[\s,;]+""")
    private val gb2312: Charset = Charset.forName("GB2312")

    fun convert(
        tab: EncodeTab,
        forward: Boolean,
        value: String,
        charset: UrlCharset,
        asciiFormat: AsciiFormat
    ): String = when (tab) {
        EncodeTab.Unicode -> if (forward) toUnicode(value) else fromUnicode(value)
        EncodeTab.Url -> if (forward) urlEncode(value, charset) else urlDecode(value, charset)
        EncodeTab.Hex -> if (forward) textToHex(value) else hexToText(value)
        EncodeTab.Ascii -> if (forward) textToAscii(value, asciiFormat) else asciiToText(value)
    }

    fun toUnicode(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            index += Character.charCount(codePoint)
            when {
                codePoint <= 0x7f -> appendCodePoint(codePoint)
                codePoint <= 0xffff -> append("\\u").append(codePoint.toString(16).padStart(4, '0'))
                else -> {
                    val offset = codePoint - 0x10000
                    val high = 0xd800 + (offset shr 10)
                    val low = 0xdc00 + (offset and 0x3ff)
                    append("\\u").append(high.toString(16))
                    append("\\u").append(low.toString(16))
                }
            }
        }
    }

    fun fromUnicode(value: String): String = unicodeEscape.replace(value) { match ->
        Char(match.groupValues[1].toInt(16)).toString()
    }

    fun urlEncode(value: String, charset: UrlCharset): String {
        val bytes = encodeText(value, charsetOf(charset))
        return buildString(bytes.size * 3) {
            for (byte in bytes) {
                val unsigned = byte.toInt() and 0xff
                if (isUnreserved(unsigned)) append(unsigned.toChar())
                else append('%').append(unsigned.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }

    fun urlDecode(value: String, charset: UrlCharset): String {
        val encoding = charsetOf(charset)
        val source = value.replace('+', ' ')
        val bytes = ArrayList<Byte>(source.length)
        var index = 0
        while (index < source.length) {
            val current = source[index]
            if (current == '%' && index + 2 < source.length && isHexPair(source, index + 1)) {
                bytes += source.substring(index + 1, index + 3).toInt(16).toByte()
                index += 3
            } else {
                encodeText(current.toString(), encoding).forEach { bytes += it }
                index += 1
            }
        }
        return decodeText(bytes.toByteArray(), encoding)
    }

    fun textToHex(value: String): String =
        value.toByteArray(StandardCharsets.UTF_8).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

    fun hexToText(value: String): String {
        val normalized = value.replace(Regex("""[\s:_-]+"""), "")
        if (normalized.isEmpty() || normalized.length % 2 != 0 || !normalized.matches(Regex("""^[0-9a-fA-F]+$"""))) {
            throw EncodeException("invalid-hex", "invalid-hex")
        }
        val bytes = hexPair.findAll(normalized).map { it.value.toInt(16).toByte() }.toList().toByteArray()
        return decodeText(bytes, StandardCharsets.UTF_8)
    }

    fun textToAscii(value: String, format: AsciiFormat): String = buildString {
        var index = 0
        var first = true
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            index += Character.charCount(codePoint)
            if (!first) append(' ')
            first = false
            append(if (format == AsciiFormat.Hex) codePoint.toString(16).uppercase() else codePoint.toString())
        }
    }

    fun asciiToText(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.split(asciiParts).joinToString("") { part ->
            val hex = part.startsWith("0x", ignoreCase = true) || part.contains(Regex("[a-fA-F]"))
            val normalized = part.replace(Regex("^0x", RegexOption.IGNORE_CASE), "")
            val codePoint = normalized.toIntOrNull(if (hex) 16 else 10)
                ?: throw EncodeException("invalid-code-point", part)
            if (codePoint < 0 || codePoint > 0x10ffff) throw EncodeException("invalid-code-point", part)
            if (codePoint in 0xd800..0xdfff) Char(codePoint).toString()
            else String(Character.toChars(codePoint))
        }
    }

    private fun charsetOf(charset: UrlCharset): Charset = when (charset) {
        UrlCharset.Utf8 -> StandardCharsets.UTF_8
        UrlCharset.Gb2312 -> gb2312
    }

    private fun encodeText(value: String, charset: Charset): ByteArray {
        val encoder = charset.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            val buffer = encoder.encode(CharBuffer.wrap(value))
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        } catch (_: CharacterCodingException) {
            throw EncodeException("unmappable", "unmappable")
        }
    }

    private fun decodeText(bytes: ByteArray, charset: Charset): String {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            throw EncodeException("invalid-bytes", "invalid-bytes")
        }
    }

    private fun isUnreserved(byte: Int): Boolean {
        return (byte in 0x41..0x5a) || (byte in 0x61..0x7a) || (byte in 0x30..0x39) ||
            byte == 0x2d || byte == 0x2e || byte == 0x5f || byte == 0x7e
    }

    private fun isHexPair(source: String, start: Int): Boolean {
        val first = source[start]
        val second = source[start + 1]
        return first.isHex() && second.isHex()
    }

    private fun Char.isHex(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
