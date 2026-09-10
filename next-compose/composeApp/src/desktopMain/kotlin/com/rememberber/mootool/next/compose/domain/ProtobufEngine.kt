package com.rememberber.mootool.next.compose.domain

import com.google.protobuf.CodedInputStream
import com.google.protobuf.DynamicMessage
import com.google.protobuf.InvalidProtocolBufferException
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.HexFormat

enum class ProtobufBinaryFormat { Hex, Base64 }

class ProtobufException(val code: String, message: String) : RuntimeException(message)

object ProtobufEngine {
    const val SAMPLE_PROTO = "syntax = \"proto3\";\n\nmessage Person {\n  string name = 1;\n  int32 age = 2;\n}"
    const val SAMPLE_JSON = "{\n  \"name\": \"MooTool\",\n  \"age\": 25\n}"
    const val SAMPLE_MESSAGE = "Person"
    const val MAX_BINARY_BYTES = 2_097_152

    private val hex = HexFormat.of()
    private val wireNames = arrayOf("Varint", "64-bit", "Length-delimited", "Start group", "End group", "32-bit")

    fun jsonToProtobuf(proto: String, messageName: String, json: String, format: ProtobufBinaryFormat): String {
        val descriptor = ProtoCompiler.descriptorFor(proto, messageName)
        val message = parseDynamic(descriptor, json)
        return formatBinary(message.toByteArray(), format)
    }

    fun protobufToJson(proto: String, messageName: String, input: String, format: ProtobufBinaryFormat): String {
        val descriptor = ProtoCompiler.descriptorFor(proto, messageName)
        val bytes = parseBinary(input, format)
        val message = try {
            DynamicMessage.parseFrom(descriptor, bytes)
        } catch (error: InvalidProtocolBufferException) {
            throw ProtobufException("binary", error.message ?: "Invalid protobuf payload")
        }
        return protobufJsonPrinter.print(message)
    }

    fun convertBinary(input: String, from: ProtobufBinaryFormat, to: ProtobufBinaryFormat): String =
        formatBinary(parseBinary(input, from), to)

    fun decodeWire(input: String, format: ProtobufBinaryFormat): String {
        val bytes = parseBinary(input, format)
        val reader = CodedInputStream.newInstance(bytes)
        reader.setSizeLimit(MAX_BINARY_BYTES)
        val lines = ArrayList<String>()
        var index = 0
        try {
            while (!reader.isAtEnd) {
                val tag = reader.readTag()
                if (tag == 0) break
                val field = tag ushr 3
                val wireType = tag and 7
                if (field == 0) throw ProtobufException("wire", "Invalid tag with field number 0")
                index += 1
                val value = readWireValue(reader, wireType)
                lines += "#$index  field=$field  wire_type=$wireType (${wireTypeName(wireType)})  value=$value"
            }
        } catch (error: ProtobufException) {
            throw error
        } catch (error: InvalidProtocolBufferException) {
            throw ProtobufException("wire", error.message ?: "Truncated or illegal wire data")
        }
        return if (lines.isEmpty()) "(empty data)" else lines.joinToString("\n")
    }

    fun formatProtoDefinition(proto: String): String {
        if (proto.trim().isEmpty()) return ""
        val tokens = tokenizeProto(proto)
        val lines = ArrayList<String>()
        var level = 0
        for (token in tokens) {
            if (token == "}") level = maxOf(0, level - 1)
            lines += "${"  ".repeat(level)}$token"
            if (token.endsWith("{")) level += 1
        }
        return lines.joinToString("\n")
    }

    fun parseBinary(input: String, format: ProtobufBinaryFormat): ByteArray {
        val value = input.replace(Regex("\\s+"), "")
        if (value.length / 2 > MAX_BINARY_BYTES || value.length > MAX_BINARY_BYTES * 2) {
            throw ProtobufException("too-large", "Binary input exceeds $MAX_BINARY_BYTES bytes")
        }
        return when (format) {
            ProtobufBinaryFormat.Base64 -> {
                if (!value.matches(Regex("^[A-Za-z0-9+/]*={0,2}$"))) {
                    throw ProtobufException("invalid-base64", "Invalid Base64 input")
                }
                try {
                    Base64.getDecoder().decode(value)
                } catch (_: IllegalArgumentException) {
                    throw ProtobufException("invalid-base64", "Invalid Base64 input")
                }
            }
            ProtobufBinaryFormat.Hex -> {
                if (!value.matches(Regex("(?:[0-9a-fA-F]{2})*"))) {
                    throw ProtobufException("invalid-hex", "Invalid Hex input")
                }
                if (value.isEmpty()) ByteArray(0) else hex.parseHex(value)
            }
        }
    }

    fun formatBinary(bytes: ByteArray, format: ProtobufBinaryFormat): String {
        if (bytes.size > MAX_BINARY_BYTES) {
            throw ProtobufException("too-large", "Binary output exceeds $MAX_BINARY_BYTES bytes")
        }
        return when (format) {
            ProtobufBinaryFormat.Base64 -> Base64.getEncoder().encodeToString(bytes)
            ProtobufBinaryFormat.Hex -> hex.formatHex(bytes)
        }
    }

    private fun readWireValue(reader: CodedInputStream, wireType: Int): String = when (wireType) {
        0 -> java.lang.Long.toUnsignedString(reader.readUInt64())
        1 -> "0x${readRawHex(reader, 8)}"
        2 -> formatLengthDelimited(reader.readByteArray())
        5 -> "0x${readRawHex(reader, 4)}"
        3, 4 -> {
            reader.skipField((1 shl 3) or wireType)
            "(group)"
        }
        else -> throw ProtobufException("wire", "Unsupported wire type: $wireType")
    }

    private fun readRawHex(reader: CodedInputStream, length: Int): String {
        val bytes = reader.readRawBytes(length)
        return hex.formatHex(bytes)
    }

    private fun formatLengthDelimited(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "\"\""
        val text = decodePrintableUtf8(bytes)
        return if (text == null) "bytes[${bytes.size}]=${hex.formatHex(bytes)}" else jsonQuote(text)
    }

    private fun decodePrintableUtf8(bytes: ByteArray): String? {
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val text = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            if (text.all { ch -> ch == '\n' || ch == '\r' || ch == '\t' || (ch.code >= 32) }) text else null
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun jsonQuote(value: String): String = buildString(value.length + 2) {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun wireTypeName(type: Int): String = wireNames.getOrNull(type) ?: "Unknown"

    private fun tokenizeProto(proto: String): List<String> {
        val tokens = ArrayList<String>()
        val current = StringBuilder()
        var quote = '\u0000'
        var escaped = false
        var lineComment = false
        fun flush() {
            val value = current.toString().trim()
            if (value.isNotEmpty()) tokens += value
            current.setLength(0)
        }
        var index = 0
        while (index < proto.length) {
            val char = proto[index]
            if (lineComment) {
                current.append(char)
                if (char == '\n') {
                    flush()
                    lineComment = false
                }
                index += 1
                continue
            }
            if (escaped) {
                current.append(char)
                escaped = false
                index += 1
                continue
            }
            if (char == '\\') {
                current.append(char)
                escaped = true
                index += 1
                continue
            }
            if (quote != '\u0000') {
                current.append(char)
                if (char == quote) quote = '\u0000'
                index += 1
                continue
            }
            if (char == '"' || char == '\'') {
                quote = char
                current.append(char)
                index += 1
                continue
            }
            if (char == '/' && index + 1 < proto.length && proto[index + 1] == '/') {
                lineComment = true
                current.append("//")
                index += 2
                continue
            }
            when (char) {
                '{' -> {
                    val trimmed = current.toString().trimEnd()
                    current.setLength(0)
                    current.append(trimmed).append(" {")
                    flush()
                }
                '}' -> {
                    flush()
                    tokens += "}"
                }
                ';' -> {
                    val trimmed = current.toString().trimEnd()
                    current.setLength(0)
                    current.append(trimmed).append(';')
                    flush()
                }
                '\n', '\r' -> flush()
                else -> current.append(char)
            }
            index += 1
        }
        flush()
        return tokens
    }
}
