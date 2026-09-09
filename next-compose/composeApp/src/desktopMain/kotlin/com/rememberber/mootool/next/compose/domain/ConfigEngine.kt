package com.rememberber.mootool.next.compose.domain

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.representer.Representer
import java.util.LinkedHashMap

class ConfigException(val code: String, message: String) : RuntimeException(message)

data class YamlValidation(val valid: Boolean, val message: String)

object ConfigEngine {
    const val SAMPLE_PROPERTIES = "server.port=8080\napp.name=MooTool\napp.locales[0]=zh-CN\napp.locales[1]=en-US"
    const val SAMPLE_YAML = "app:\n  name: MooTool\n  enabled: true\n"

    private val unicodeEscape = Regex("""\\u([\da-fA-F]{4})""")
    private val escapedSeparator = Regex("""\\([:= ])""")
    private val pathToken = Regex("""([^\[\]]+)|\[(\d+)\]""")

    fun propertiesToYaml(source: String): String {
        val root = LinkedHashMap<String, Any?>()
        for (rawLine in source.split(Regex("\r?\n"))) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue
            val separator = findSeparator(rawLine)
            val key = (if (separator < 0) rawLine else rawLine.substring(0, separator)).trim()
            val value = (if (separator < 0) "" else rawLine.substring(separator + 1)).trim()
            if (key.isEmpty()) continue
            assignPath(root, tokenizePath(key), decodeProperty(value))
        }
        return dumpYaml(root, indent = 4)
    }

    fun yamlToProperties(source: String): String {
        val value = loadYaml(source)
        if (value == null || value !is Map<*, *>) {
            throw ConfigException("root", "YAML root must be an object")
        }
        val lines = ArrayList<String>()
        flattenYaml(value, "", lines)
        return lines.joinToString("\n")
    }

    fun formatYaml(source: String): String {
        if (source.trim().isEmpty()) return ""
        val loaded = loadYaml(source)
        return when (loaded) {
            null -> ""
            is Map<*, *> -> dumpYaml(loaded, indent = 2)
            else -> dumpYaml(loaded, indent = 2)
        }.trimEnd()
    }

    fun validateYaml(source: String): YamlValidation {
        if (source.trim().isEmpty()) return YamlValidation(true, "")
        return try {
            loadYaml(source)
            YamlValidation(true, "")
        } catch (error: ConfigException) {
            YamlValidation(false, error.message ?: "invalid")
        }
    }

    private fun loadYaml(source: String): Any? {
        return try {
            parser().load<Any?>(source)
        } catch (error: YAMLException) {
            throw ConfigException("syntax", error.message ?: "invalid YAML")
        }
    }

    private fun dumpYaml(value: Any?, indent: Int): String {
        val options = dumperOptions(indent)
        val yaml = Yaml(options)
        return if (value is Map<*, *>) {
            val typed = LinkedHashMap<String, Any?>()
            for ((key, item) in value) typed[key.toString()] = item
            yaml.dumpAsMap(typed)
        } else {
            yaml.dump(value)
        }
    }

    private fun parser(): Yaml {
        val loader = LoaderOptions().apply {
            codePointLimit = 1_048_576
            maxAliasesForCollections = 50
            allowRecursiveKeys = false
        }
        val options = dumperOptions(2)
        return Yaml(SafeConstructor(loader), Representer(options), options, loader)
    }

    private fun dumperOptions(indent: Int): DumperOptions = DumperOptions().apply {
        this.indent = indent.coerceAtLeast(2)
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
        isPrettyFlow = false
        isAllowUnicode = true
        width = Integer.MAX_VALUE
        isExplicitStart = false
        isExplicitEnd = false
    }

    private fun findSeparator(line: String): Int {
        var escaped = false
        for (index in line.indices) {
            val char = line[index]
            if (!escaped && (char == '=' || char == ':')) return index
            escaped = !escaped && char == '\\'
            if (char != '\\') escaped = false
        }
        return -1
    }

    private fun tokenizePath(key: String): List<Any> =
        key.split('.').flatMap { part ->
            pathToken.findAll(part).map { match ->
                val index = match.groupValues[2]
                if (index.isEmpty()) match.groupValues[1] else index.toInt()
            }
        }

    private fun assignPath(root: MutableMap<String, Any?>, tokens: List<Any>, value: String) {
        if (tokens.isEmpty()) return
        var current: Any = root
        tokens.forEachIndexed { index, token ->
            val last = index == tokens.lastIndex
            if (last) {
                writeLeaf(current, token, value)
                return
            }
            val nextIsArray = tokens[index + 1] is Int
            current = childContainer(current, token, nextIsArray)
        }
    }

    private fun writeLeaf(current: Any, token: Any, value: String) {
        when {
            current is MutableList<*> && token is Int -> {
                val list = mutableAnyList(current)
                ensureSize(list, token)
                val existing = list[token]
                if (existing is MutableMap<*, *> || existing is MutableList<*>) {
                    throw ConfigException("conflict", "Cannot overwrite nested value at index $token with a scalar")
                }
                list[token] = value
            }
            current is MutableMap<*, *> && token is String -> {
                val map = mutableAnyMap(current)
                val existing = map[token]
                if (existing is MutableMap<*, *> || existing is MutableList<*>) {
                    throw ConfigException("conflict", "Cannot overwrite object/array '$token' with a scalar")
                }
                map[token] = value
            }
            else -> throw ConfigException("conflict", "Cannot assign '$token' on ${current::class.simpleName}")
        }
    }

    private fun childContainer(current: Any, token: Any, nextIsArray: Boolean): Any {
        when {
            current is MutableList<*> && token is Int -> {
                val list = mutableAnyList(current)
                ensureSize(list, token)
                val existing = list[token]
                return when {
                    existing == null -> {
                        val created: Any = if (nextIsArray) ArrayList<Any?>() else LinkedHashMap<String, Any?>()
                        list[token] = created
                        created
                    }
                    nextIsArray && existing is MutableList<*> -> existing
                    !nextIsArray && existing is MutableMap<*, *> -> existing
                    else -> throw ConfigException("conflict", "Type conflict at index $token")
                }
            }
            current is MutableMap<*, *> && token is String -> {
                val map = mutableAnyMap(current)
                val existing = map[token]
                return when {
                    existing == null -> {
                        val created: Any = if (nextIsArray) ArrayList<Any?>() else LinkedHashMap<String, Any?>()
                        map[token] = created
                        created
                    }
                    nextIsArray && existing is MutableList<*> -> existing
                    !nextIsArray && existing is MutableMap<*, *> -> existing
                    else -> throw ConfigException("conflict", "Type conflict at '$token'")
                }
            }
            else -> throw ConfigException("conflict", "Cannot nest '$token' on ${current::class.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mutableAnyList(value: MutableList<*>): MutableList<Any?> = value as MutableList<Any?>

    @Suppress("UNCHECKED_CAST")
    private fun mutableAnyMap(value: MutableMap<*, *>): MutableMap<String, Any?> = value as MutableMap<String, Any?>

    private fun ensureSize(list: MutableList<Any?>, index: Int) {
        while (list.size <= index) list.add(null)
    }

    private fun flattenYaml(value: Any?, prefix: String, lines: MutableList<String>) {
        when (value) {
            is List<*> -> {
                val scalar = value.all { item -> item == null || item !is Map<*, *> && item !is List<*> }
                if (scalar) {
                    lines += "$prefix=${value.joinToString(",") { propertyValue(it) }}"
                } else {
                    value.forEachIndexed { index, item -> flattenYaml(item, "$prefix[$index]", lines) }
                }
            }
            is Map<*, *> -> {
                for ((key, item) in value) {
                    val next = if (prefix.isEmpty()) key.toString() else "$prefix.$key"
                    flattenYaml(item, next, lines)
                }
            }
            else -> lines += "$prefix=${propertyValue(value)}"
        }
    }

    private fun propertyValue(value: Any?): String =
        (value?.toString() ?: " ").replace("\\", "\\\\").replace("\n", "\\n")

    private fun decodeProperty(value: String): String =
        value
            .replace(unicodeEscape) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace(escapedSeparator, "$1")
}
