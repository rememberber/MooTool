package com.rememberber.mootool.next.compose.ai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path

object VaultMcpTools {
    private val json = Json { encodeDefaults = true }

    val toolNames: List<String> = listOf(
        "mootool_notes_search",
        "mootool_notes_read",
        "mootool_json_documents_search",
        "mootool_json_documents_read"
    )

    fun isVaultTool(name: String): Boolean = toolNames.contains(name)

    fun call(name: String, arguments: Map<String, Any?>, accessFile: Path?): McpToolResult {
        return try {
            val service = VaultMcpReadService(accessFile)
            val text = when (name) {
                "mootool_notes_search" -> {
                    val args = searchArgs(arguments)
                    json.encodeToString(
                        service.search(VaultMcpKind.Notes, args.query, args.limit, args.offset)
                    )
                }
                "mootool_json_documents_search" -> {
                    val args = searchArgs(arguments)
                    json.encodeToString(
                        service.search(VaultMcpKind.Json, args.query, args.limit, args.offset)
                    )
                }
                "mootool_notes_read" -> {
                    val args = readArgs(arguments)
                    json.encodeToString(
                        service.read(VaultMcpKind.Notes, args.path, args.offset, args.length)
                    )
                }
                "mootool_json_documents_read" -> {
                    val args = readArgs(arguments)
                    json.encodeToString(
                        service.read(VaultMcpKind.Json, args.path, args.offset, args.length)
                    )
                }
                else -> throw IllegalArgumentException("Unknown vault tool: $name")
            }
            McpToolResult(text = text, isError = false)
        } catch (error: Exception) {
            McpToolResult(text = error.message ?: error.toString(), isError = true)
        }
    }

    private data class VaultSearchArgs(val query: String, val limit: Int, val offset: Int)

    private data class VaultReadArgs(val path: String, val offset: Int, val length: Int)

    /** 对齐 Electron `vaultTools.ts` 中 `searchSchema` / `readSchema`（Zod strict 边界，不静默钳制）。 */
    private fun searchArgs(arguments: Map<String, Any?>): VaultSearchArgs {
        val query = optionalString(arguments, "query", maxLength = 200, default = "")
        val limit = boundedInt(arguments, "limit", default = 20, min = 1, max = 50)
        val offset = boundedInt(arguments, "offset", default = 0, min = 0, max = 2000)
        return VaultSearchArgs(query, limit, offset)
    }

    private fun readArgs(arguments: Map<String, Any?>): VaultReadArgs {
        val path = requiredString(arguments, "path", maxLength = 1000)
        val offset = boundedInt(arguments, "offset", default = 0, min = 0, max = 2_000_000)
        val length = boundedInt(arguments, "length", default = 20_000, min = 1, max = 50_000)
        return VaultReadArgs(path, offset, length)
    }

    private fun optionalString(
        args: Map<String, Any?>,
        key: String,
        maxLength: Int,
        default: String,
    ): String {
        val value = args[key] ?: return default
        val text = when (value) {
            is String -> value
            else -> throw IllegalArgumentException("Invalid $key")
        }
        if (text.length > maxLength) throw IllegalArgumentException("Invalid $key")
        return text
    }

    private fun requiredString(args: Map<String, Any?>, key: String, maxLength: Int): String {
        val value = args[key] ?: throw IllegalArgumentException("Missing $key")
        val text = when (value) {
            is String -> value
            else -> throw IllegalArgumentException("Invalid $key")
        }
        if (text.isBlank() || text.length > maxLength) throw IllegalArgumentException("Invalid $key")
        return text
    }

    private fun boundedInt(
        args: Map<String, Any?>,
        key: String,
        default: Int,
        min: Int,
        max: Int,
    ): Int {
        if (!args.containsKey(key) || args[key] == null) return default
        val parsed = parseIntStrict(args[key], key)
        if (parsed !in min..max) throw IllegalArgumentException("Invalid $key")
        return parsed
    }

    private fun parseIntStrict(value: Any?, key: String): Int = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> if (value % 1.0 == 0.0) value.toInt() else throw IllegalArgumentException("Invalid $key")
        is Float -> if (value % 1f == 0f) value.toInt() else throw IllegalArgumentException("Invalid $key")
        is String -> value.toIntOrNull() ?: throw IllegalArgumentException("Invalid $key")
        else -> throw IllegalArgumentException("Invalid $key")
    }
}
