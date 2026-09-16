package com.rememberber.mootool.next.compose.ai

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.tomlj.Toml
import org.tomlj.TomlTable

object AiClientConfigMerge {
    private const val SERVER_NAME = "mootool"

    private val jsonMapper: ObjectMapper = ObjectMapper().apply {
        factory.enable(JsonParser.Feature.ALLOW_COMMENTS)
        factory.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA)
        factory.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    }

    fun readServer(source: String?, toml: Boolean): Any? {
        if (toml) {
            val table = parseToml(source ?: "")
            val servers = table.getTable("mcp_servers") ?: return null
            return servers.toMap()[SERVER_NAME]
        }
        val root = parseJsonRoot(source ?: "{}")
        val servers = root.get("mcpServers") ?: return null
        if (!servers.isObject) return null
        return servers.get(SERVER_NAME)
    }

    fun mergeToml(source: String, launch: McpLaunch): String {
        val parsed = parseToml(source)
        val servers = parsed.getTable("mcp_servers")
        if (servers != null && servers.get(SERVER_NAME) != null) {
            if (launchTable(servers, SERVER_NAME) == launch) return source
            throw IllegalArgumentException(
                "An MCP server named mootool already exists with different settings. Rename or remove that entry before installing."
            )
        }
        val block = formatTomlMcpBlock(launch)
        val separator = if (source.isEmpty() || source.endsWith("\n")) "" else "\n"
        val result = "$source$separator\n$block"
        parseToml(result)
        return result
    }

    fun manageServer(source: String, toml: Boolean, desired: Any?, owned: Any? = null): String {
        val existing = readServer(source.ifEmpty { null }, toml)
        if (existing == null) {
            return when {
                desired == null -> source
                toml -> mergeToml(source, normalizeLaunch(desired, true) as McpLaunch)
                else -> mergeJson(source.ifEmpty { "{}" }, normalizeLaunch(desired, false) as ClaudeMcpLaunch, true)
            }
        }
        if (desired != null && serverEquals(existing, desired, toml)) {
            return if (toml) mergeToml(source, normalizeLaunch(desired, true) as McpLaunch)
            else mergeJson(source, normalizeLaunch(desired, false) as ClaudeMcpLaunch, true)
        }
        if (owned == null || !serverEquals(existing, owned, toml)) {
            throw IllegalArgumentException(
                "MooTool configuration was changed outside the installer. Preserve or rename that entry before continuing."
            )
        }
        val without = if (toml) {
            removeTomlServer(source, normalizeLaunch(owned, true) as McpLaunch)
        } else {
            removeJsonServer(source, normalizeLaunch(owned, false) as ClaudeMcpLaunch)
        }
        if (desired == null) return without
        return if (toml) mergeToml(without, normalizeLaunch(desired, true) as McpLaunch)
        else mergeJson(without, normalizeLaunch(desired, false) as ClaudeMcpLaunch, true)
    }

    fun serverEquals(existing: Any?, expected: Any?, toml: Boolean): Boolean {
        if (toml) {
            val left = normalizeLaunch(existing, true) as? McpLaunch
            val right = normalizeLaunch(expected, true) as? McpLaunch
            return left == right
        }
        val left = normalizeLaunch(existing, false) as? ClaudeMcpLaunch
        val right = normalizeLaunch(expected, false) as? ClaudeMcpLaunch
        return left != null && right != null && left.toMap() == right.toMap()
    }

    fun mergeJson(source: String, launch: ClaudeMcpLaunch, allowComments: Boolean): String {
        if (!allowComments && (source.contains("//") || source.contains("/*"))) {
            throw IllegalArgumentException("Invalid client JSON configuration")
        }
        val root = parseJsonRoot(source)
        if (!root.isObject) throw IllegalArgumentException("Invalid client JSON configuration")
        val servers = root.get("mcpServers")
        if (servers != null && !servers.isObject) throw IllegalArgumentException("mcpServers must be an object")
        if (servers != null && servers.has(SERVER_NAME)) {
            if (serverEquals(servers.get(SERVER_NAME), launch, false)) return source
            val existing = jsonMapper.convertValue(servers.get(SERVER_NAME), Map::class.java)
            val desired = launch.toMap()
            if (existing == desired) return source
            throw IllegalArgumentException(
                "An MCP server named mootool already exists with different settings. Rename or remove that entry before installing."
            )
        }
        if (source.contains("//") || source.contains("/*") || source.contains(",\n") && source.contains(",}")) {
            return insertJsonServerEntry(source, launch)
        }
        val next = (root.deepCopy() as ObjectNode)
        val serverNode = next.with("mcpServers") as ObjectNode
        serverNode.set<JsonNode>(SERVER_NAME, jsonMapper.valueToTree(launch.toMap()))
        val eol = if (source.contains("\r\n")) "\r\n" else "\n"
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(next).replace("\n", eol)
    }

    private fun insertJsonServerEntry(source: String, launch: ClaudeMcpLaunch): String {
        val key = "\"mcpServers\""
        val index = source.indexOf(key)
        if (index < 0) {
            val trimmed = source.trimEnd().removeSuffix("}")
            val comma = if (trimmed.endsWith("{")) "" else ","
            val entry = jsonMapper.writeValueAsString(launch.toMap()).prependIndent("    ")
            return "$trimmed$comma\n  \"mcpServers\": {\n    \"$SERVER_NAME\": $entry\n  }\n}"
        }
        val open = source.indexOf('{', index)
        if (open < 0) throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        val close = findMatchingBrace(source, open)
        val inner = source.substring(open + 1, close).trim()
        val snippet = jsonMapper.writeValueAsString(launch.toMap())
        val insertion = if (inner.isEmpty()) {
            "\n    \"$SERVER_NAME\": $snippet\n  "
        } else {
            val suffix = if (inner.endsWith(",")) "" else ","
            "\n    \"$SERVER_NAME\": $snippet$suffix\n  "
        }
        return source.substring(0, close) + insertion + source.substring(close)
    }

    private fun findMatchingBrace(source: String, open: Int): Int {
        var depth = 0
        var inString = false
        var escape = false
        for (i in open until source.length) {
            val ch = source[i]
            if (inString) {
                if (escape) escape = false
                else if (ch == '\\') escape = true
                else if (ch == '"') inString = false
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        throw IllegalArgumentException("Invalid client JSON configuration")
    }

    private fun parseToml(source: String): TomlTable {
        return try {
            Toml.parse(source)
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "Cannot safely merge this TOML configuration. Check its syntax and inline mcp_servers tables, then refresh the preview."
            )
        }
    }

    private fun parseJsonRoot(source: String): JsonNode {
        return try {
            jsonMapper.readTree(source.ifBlank { "{}" })
        } catch (error: JsonParseException) {
            if (error.message?.contains("duplicate", ignoreCase = true) == true) {
                throw IllegalArgumentException("Duplicate JSON configuration keys must be resolved before installation")
            }
            throw IllegalArgumentException("Invalid client JSON configuration")
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid client JSON configuration")
        }
    }

    private fun launchTable(servers: TomlTable, name: String): McpLaunch? {
        val table = servers.getTable(name) ?: return null
        val command = table.getString("command") ?: return null
        val args = table.getArray("args")?.toList()?.map { it.toString().trim('"') } ?: emptyList()
        val envTable = table.getTable("env")
        val env = envTable?.toMap()?.mapValues { it.value.toString() } ?: emptyMap()
        return McpLaunch(command, args, env)
    }

    private fun formatTomlMcpBlock(launch: McpLaunch): String {
        val lines = mutableListOf<String>()
        lines += "[mcp_servers.$SERVER_NAME]"
        lines += "command = ${tomlString(launch.command)}"
        lines += "args = [${launch.args.joinToString(", ") { tomlString(it) }}]"
        if (launch.env.isNotEmpty()) {
            lines += ""
            lines += "[mcp_servers.$SERVER_NAME.env]"
            launch.env.forEach { (key, value) -> lines += "$key = ${tomlString(value)}" }
        }
        return lines.joinToString("\n")
    }

    private fun tomlString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    private fun removeTomlServer(source: String, owned: McpLaunch): String {
        val block = formatTomlMcpBlock(owned)
        val at = source.indexOf(block)
        if (at < 0 || source.indexOf(block, at + 1) >= 0) {
            throw IllegalArgumentException(
                "The managed TOML block was reformatted. Preserve or rename it before continuing."
            )
        }
        var next = source.removeRange(at, at + block.length)
        if (next.endsWith("\n\n")) next = next.dropLast(1)
        parseToml(next)
        if (readServer(next, true) != null) {
            throw IllegalArgumentException("Cannot preserve the existing TOML configuration")
        }
        if (!tomlConfigsEqualExceptMootool(source, next)) {
            throw IllegalArgumentException("Cannot preserve the existing TOML configuration")
        }
        return next
    }

    private fun removeJsonServer(source: String, owned: ClaudeMcpLaunch): String {
        mergeJson(source, owned, true)
        val removed = removeJsonProperty(source, listOf("mcpServers", SERVER_NAME))
        if (readServer(removed, false) != null) {
            throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        }
        if (!withoutMootoolJson(source).equals(withoutMootoolJson(removed))) {
            throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        }
        return removed
    }

    private fun tomlConfigsEqualExceptMootool(left: String, right: String): Boolean {
        val leftParsed = parseToml(left)
        val rightParsed = parseToml(right)
        if (leftParsed.getString("model") != rightParsed.getString("model")) return false
        val leftServers = leftParsed.getTable("mcp_servers")
        val rightServers = rightParsed.getTable("mcp_servers")
        val leftOthers = leftServers?.toMap()?.keys?.filter { it != SERVER_NAME }?.sorted() ?: emptyList()
        val rightOthers = rightServers?.toMap()?.keys?.filter { it != SERVER_NAME }?.sorted() ?: emptyList()
        if (leftOthers != rightOthers) return false
        return leftOthers.all { name ->
            launchTable(leftServers!!, name) == launchTable(rightServers!!, name)
        }
    }

    private fun withoutMootoolJson(source: String): JsonNode {
        val root = parseJsonRoot(source).deepCopy() as ObjectNode
        val servers = root.get("mcpServers") as? ObjectNode
        servers?.remove(SERVER_NAME)
        if (servers != null && !servers.fields().hasNext()) root.remove("mcpServers")
        return root
    }

    private fun removeJsonProperty(source: String, path: List<String>): String {
        require(path.size >= 2)
        val parent = path.first()
        val child = path.last()
        val parentKey = "\"$parent\""
        val parentIndex = source.indexOf(parentKey)
        if (parentIndex < 0) throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        val open = source.indexOf('{', parentIndex)
        if (open < 0) throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        val close = findMatchingBrace(source, open)
        val inner = source.substring(open + 1, close)
        val childKey = "\"$child\""
        val childIndex = inner.indexOf(childKey)
        if (childIndex < 0) throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        val valueStart = inner.indexOf('{', childIndex)
        if (valueStart < 0) throw IllegalArgumentException("Cannot preserve the existing JSON configuration")
        val valueEnd = findMatchingBrace(inner, valueStart)
        var removeStart = childIndex
        while (removeStart > 0 && inner[removeStart - 1].isWhitespace()) removeStart--
        if (removeStart > 0 && inner[removeStart - 1] == ',') removeStart--
        var removeEnd = valueEnd + 1
        while (removeEnd < inner.length && inner[removeEnd].isWhitespace()) removeEnd++
        if (removeEnd < inner.length && inner[removeEnd] == ',') removeEnd++
        val newInner = inner.removeRange(removeStart, removeEnd)
        return source.substring(0, open + 1) + newInner + source.substring(close)
    }

    private fun normalizeLaunch(value: Any?, toml: Boolean): Any? {
        return when (value) {
            null -> null
            is McpLaunch -> value
            is ClaudeMcpLaunch -> value
            is ReceiptMcp -> if (toml) {
                McpLaunch(value.command, value.args, value.env)
            } else {
                ClaudeMcpLaunch(value.type ?: "stdio", value.command, value.args, value.env)
            }
            is TomlTable -> launchTableFromToml(value)
            is JsonNode -> claudeLaunchFromMap(jsonMapper.convertValue(value, Map::class.java) as Map<*, *>)
            else -> null
        }
    }

    private fun claudeLaunchFromMap(map: Map<*, *>): ClaudeMcpLaunch? {
        val command = map["command"]?.toString() ?: return null
        val args = (map["args"] as? List<*>)?.map { it.toString() } ?: emptyList()
        val env = (map["env"] as? Map<*, *>)?.entries?.associate { (key, value) ->
            key.toString() to value.toString()
        } ?: emptyMap()
        val type = map["type"]?.toString()?.takeIf { it.isNotBlank() } ?: "stdio"
        return ClaudeMcpLaunch(type = type, command = command, args = args, env = env)
    }

    private fun launchTableFromToml(table: TomlTable): McpLaunch? {
        val command = table.getString("command") ?: return null
        val args = table.getArray("args")?.toList()?.map { it.toString().trim('"') } ?: emptyList()
        val envTable = table.getTable("env")
        val env = envTable?.toMap()?.mapValues { it.value.toString() } ?: emptyMap()
        return McpLaunch(command, args, env)
    }
}
