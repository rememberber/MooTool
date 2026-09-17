package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.domain.DiffEngine
import com.rememberber.mootool.next.compose.domain.EncodeEngine
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonFormatOptions
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.domain.ProtobufBinaryFormat
import com.rememberber.mootool.next.compose.domain.ProtobufEngine
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.domain.TimestampUnit
import com.rememberber.mootool.next.compose.domain.UrlCharset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.util.UUID

object MooToolMcpTools {
    private const val MCP_TEXT_MAX_CHARS = 100_000
    private const val MCP_TIMESTAMP_TEXT_MAX_CHARS = 100
    private const val MCP_JSON_PATH_MAX_CHARS = 1_000
    private const val MCP_DIFF_MAX_CHARS = 8_000
    private const val MCP_UUID_MAX_COUNT = 100
    private val base64Pattern = Regex("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")

    private val noopTranslator = object : JsonTranslator {
        override fun t(key: String, params: Map<String, String>): String = key
    }

    fun call(name: String, arguments: Map<String, Any?>): McpToolResult {
        return try {
            val text = when (name) {
                "mootool_json_format" -> jsonFormat(arguments)
                "mootool_json_query" -> jsonQuery(arguments)
                "mootool_encode" -> encode(arguments)
                "mootool_timestamp" -> timestamp(arguments)
                "mootool_diff" -> diff(arguments)
                "mootool_hash" -> hash(arguments)
                "mootool_uuid" -> uuid(arguments)
                "mootool_protobuf_wire" -> protobufWire(arguments)
                else -> throw IllegalArgumentException("Unknown MooTool tool: $name")
            }
            McpToolResult(text = text, isError = false)
        } catch (error: Exception) {
            McpToolResult(text = error.message ?: error.toString(), isError = true)
        }
    }

    fun toolNames(): List<String> = listOf(
        "mootool_json_format",
        "mootool_json_query",
        "mootool_encode",
        "mootool_timestamp",
        "mootool_diff",
        "mootool_hash",
        "mootool_uuid",
        "mootool_protobuf_wire",
    )

    private fun jsonFormat(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("text", "spaces", "sortKeys", "checkDuplicateKeys"))
        val input = requireTextArg(args, "text")
        val spaces = boundedIntArg(args, "spaces", default = 2, min = 0, max = 8)
        val sortKeys = boolArg(args, "sortKeys", false)
        val checkDuplicateKeys = boolArg(args, "checkDuplicateKeys", true)
        return JsonEngine.formatAdvanced(
            input,
            noopTranslator,
            JsonFormatOptions(spaces = spaces, sortKeys = sortKeys, checkDuplicateKeys = checkDuplicateKeys, ignoreCase = false)
        )
    }

    private fun jsonQuery(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("text", "path"))
        val input = requireTextArg(args, "text")
        val path = requireStringArg(args, "path", MCP_JSON_PATH_MAX_CHARS)
        if (path.contains("?(")) {
            throw IllegalArgumentException("JSONPath filter expressions are disabled for MCP tools")
        }
        return JsonEngine.queryPath(input, path, noopTranslator)
    }

    private fun encode(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("text", "format", "direction", "charset"))
        val input = requireTextArg(args, "text")
        val format = requireStringArg(args, "format")
        val direction = requireStringArg(args, "direction")
        val charset = when (optionalStringArg(args, "charset", "utf-8")) {
            "gb2312" -> UrlCharset.Gb2312
            else -> UrlCharset.Utf8
        }
        val encode = direction == "encode"
        return when (format) {
            "url" -> if (encode) EncodeEngine.urlEncode(input, charset) else EncodeEngine.urlDecode(input, charset)
            "hex" -> if (encode) EncodeEngine.textToHex(input) else EncodeEngine.hexToText(input)
            "unicode" -> if (encode) EncodeEngine.toUnicode(input) else EncodeEngine.fromUnicode(input)
            "base64" -> if (encode) {
                java.util.Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
            } else {
                if (!base64Pattern.matches(input)) throw IllegalArgumentException("Invalid Base64")
                String(java.util.Base64.getDecoder().decode(input), Charsets.UTF_8)
            }
            else -> throw IllegalArgumentException("Unsupported format")
        }
    }

    private fun timestamp(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("text", "direction", "unit", "zone"))
        val input = requireStringArg(args, "text", MCP_TIMESTAMP_TEXT_MAX_CHARS)
        val direction = requireStringArg(args, "direction")
        val unit = when (optionalStringArg(args, "unit", "second")) {
            "millisecond" -> TimestampUnit.Millisecond
            else -> TimestampUnit.Second
        }
        val zone = optionalStringArg(args, "zone", "UTC").take(100)
        return if (direction == "to-local") {
            mcpTimestampToLocal(input, unit, zone)
        } else {
            TimeEngine.localToTimestamp(input, unit, zone)
        }
    }

    /** MCP 对齐 Electron `timeTools.timestampToLocal`：13+ 位数字按毫秒解释（F18 UI 仍显式单位，见 DIFF-001）。 */
    private fun mcpTimestampToLocal(input: String, unit: TimestampUnit, zone: String): String {
        val normalized = input.trim()
        val detectedUnit = if (normalized.replace("-", "").length >= 13) TimestampUnit.Millisecond else unit
        return TimeEngine.timestampToLocal(input, detectedUnit, zone).localTime
    }

    private fun diff(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("left", "right", "ignoreWhitespace"))
        val left = requireStringArg(args, "left", MCP_DIFF_MAX_CHARS)
        val right = requireStringArg(args, "right", MCP_DIFF_MAX_CHARS)
        if (left.length > MCP_DIFF_MAX_CHARS || right.length > MCP_DIFF_MAX_CHARS) {
            throw IllegalArgumentException("Diff inputs are limited to $MCP_DIFF_MAX_CHARS characters each")
        }
        val ignoreWhitespace = boolArg(args, "ignoreWhitespace", false)
        val result = DiffEngine.compare(left, right, ignoreWhitespace)
        return Json.encodeToString(
            JsonObject(
                mapOf(
                    "unified" to JsonPrimitive(result.unified),
                    "added" to JsonPrimitive(result.added),
                    "removed" to JsonPrimitive(result.removed),
                    "changed" to JsonPrimitive(result.changed)
                )
            )
        )
    }

    private fun hash(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("text", "algorithm"))
        val input = requireTextArg(args, "text")
        val algorithm = optionalStringArg(args, "algorithm", "sha256")
        if (algorithm !in setOf("md5", "sha1", "sha256", "sha384", "sha512")) {
            throw IllegalArgumentException("Invalid algorithm")
        }
        val digest = MessageDigest.getInstance(algorithm.uppercase())
        return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun protobufWire(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("text", "format"))
        val input = requireStringArg(args, "text", MCP_TEXT_MAX_CHARS)
        val format = when (requireStringArg(args, "format")) {
            "hex" -> ProtobufBinaryFormat.Hex
            "base64" -> ProtobufBinaryFormat.Base64
            else -> throw IllegalArgumentException("Unsupported format")
        }
        return ProtobufEngine.decodeWire(input, format)
    }

    private fun uuid(args: Map<String, Any?>): String {
        requireKnownKeys(args, setOf("count"))
        val count = boundedIntArg(args, "count", default = 1, min = 1, max = MCP_UUID_MAX_COUNT)
        val values = List(count) { UUID.randomUUID().toString() }
        return Json.encodeToString(JsonElement.serializer(), JsonArray(values.map { JsonPrimitive(it) }))
    }

    private fun requireKnownKeys(args: Map<String, Any?>, allowed: Set<String>) {
        val unknown = args.keys.filter { it !in allowed }
        if (unknown.isNotEmpty()) throw IllegalArgumentException("Invalid arguments")
    }

    private fun requireTextArg(args: Map<String, Any?>, key: String): String =
        requireStringArg(args, key, MCP_TEXT_MAX_CHARS)

    private fun requireStringArg(args: Map<String, Any?>, key: String, maxLength: Int = MCP_TEXT_MAX_CHARS): String {
        val value = args[key] ?: throw IllegalArgumentException("Missing $key")
        val text = when (value) {
            is String -> value
            is JsonPrimitive -> if (value.isString) value.content else throw IllegalArgumentException("Invalid $key")
            else -> throw IllegalArgumentException("Invalid $key")
        }
        if (text.length > maxLength) throw IllegalArgumentException("Invalid $key")
        return text
    }

    private fun optionalStringArg(args: Map<String, Any?>, key: String, default: String): String {
        if (!args.containsKey(key) || args[key] == null) return default
        return requireStringArg(args, key)
    }

    private fun boundedIntArg(args: Map<String, Any?>, key: String, default: Int, min: Int, max: Int): Int {
        if (!args.containsKey(key) || args[key] == null) return default
        val parsed = when (val value = args[key]) {
            is Int -> value
            is Long -> value.toInt()
            is JsonPrimitive -> value.intOrNull ?: throw IllegalArgumentException("Invalid $key")
            is Number -> value.toInt()
            else -> throw IllegalArgumentException("Invalid $key")
        }
        if (parsed !in min..max) throw IllegalArgumentException("Invalid $key")
        return parsed
    }

    private fun boolArg(args: Map<String, Any?>, key: String, default: Boolean): Boolean =
        when (val value = args[key]) {
            null -> default
            is JsonPrimitive -> value.booleanOrNull ?: default
            is Boolean -> value
            else -> default
        }
}

data class McpToolResult(val text: String, val isError: Boolean)

private fun JsonArray(elements: List<JsonElement>): JsonElement =
    kotlinx.serialization.json.buildJsonArray { elements.forEach { add(it) } }
