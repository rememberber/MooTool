package com.rememberber.mootool.next.compose.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import java.io.StringWriter
import java.math.BigDecimal
import java.util.Locale
import java.util.TreeMap

object JsonEngine {
    private val factory: JsonFactory = JsonFactory.builder()
        .streamReadConstraints(
            StreamReadConstraints.builder()
                .maxNestingDepth(512)
                .maxStringLength(20 * 1024 * 1024)
                .maxNumberLength(10_000)
                .build()
        )
        .build()

    private val mapper: ObjectMapper = ObjectMapper(factory).apply {
        nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
    }

    private val jsonPathConfig: Configuration = Configuration.builder()
        .jsonProvider(JacksonJsonNodeJsonProvider(mapper))
        .mappingProvider(JacksonMappingProvider(mapper))
        .options(Option.ALWAYS_RETURN_LIST)
        .build()

    fun validate(input: String, t: JsonTranslator): JsonStatus {
        if (input.isBlank()) {
            return JsonStatus(JsonStatus.Kind.Idle, t.t("json.valid.idle"))
        }
        return try {
            val type = describeType(parsePreserving(input, t))
            JsonStatus(JsonStatus.Kind.Valid, t.t("json.valid.ok", mapOf("type" to type)))
        } catch (error: JsonException) {
            JsonStatus(JsonStatus.Kind.Error, error.message ?: t.t("json.valid.error"), error.line, error.column)
        } catch (error: Exception) {
            JsonStatus(JsonStatus.Kind.Error, error.message ?: t.t("json.valid.error"))
        }
    }

    fun format(input: String, t: JsonTranslator, spaces: Int = 2): String =
        rewrite(input, t, pretty = true, spaces = spaces, sortKeys = false, ignoreCase = false)

    fun compress(input: String, t: JsonTranslator): String =
        rewrite(input, t, pretty = false, spaces = 0, sortKeys = false, ignoreCase = false)

    fun formatAdvanced(input: String, t: JsonTranslator, options: JsonFormatOptions): String {
        if (options.checkDuplicateKeys) {
            val duplicates = findDuplicateKeys(input, options.ignoreCase)
            if (duplicates.isNotEmpty()) {
                throw JsonException(t.t("json.error.duplicateKeys", mapOf("paths" to duplicates.joinToString(", "))))
            }
        }
        return rewrite(input, t, pretty = true, spaces = options.spaces, sortKeys = options.sortKeys, ignoreCase = options.ignoreCase)
    }

    fun findDuplicateKeys(input: String, ignoreCase: Boolean = false): List<String> {
        mapper.readTree(input)
        return DuplicateKeyParser(input, ignoreCase).parse()
    }

    fun escapeJsonString(input: String): String = mapper.writeValueAsString(input)

    fun unescapeJsonString(input: String, t: JsonTranslator): String {
        val node = parsePreserving(input, t)
        if (!node.isTextual) throw JsonException(t.t("json.error.notString"))
        return node.textValue()
    }

    fun escapeJavaString(input: String): String = buildString(input.length + 8) {
        for (ch in input) {
            when (ch) {
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
    }

    fun unescapeJsonText(input: String): String {
        val quoted = "\"" + input.replace("\"", "\\\"") + "\""
        return mapper.readValue(quoted, String::class.java)
    }

    fun jsonToXml(input: String, t: JsonTranslator): String {
        val node = parsePreserving(input, t)
        return XmlRenderer.render(mapOf("root" to nodeToPlain(node)))
    }

    fun xmlToJson(input: String, t: JsonTranslator): String {
        if (input.isBlank()) throw JsonException(t.t("json.error.emptyXml"))
        val parsed = SimpleXmlParser.parse(input)
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed)
    }

    fun swapKeysAndValues(input: String, t: JsonTranslator): String {
        val node = parsePreserving(input, t)
        if (!node.isObject) throw JsonException(t.t("json.error.objectRequired"))
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(swapObject(node))
    }

    fun queryPath(input: String, path: String, t: JsonTranslator): String {
        if (path.isBlank()) throw JsonException(t.t("json.error.emptyPath"))
        val document = parsePreserving(input, t)
        val result = JsonPath.using(jsonPathConfig).parse(document).read<JsonNode>(path.trim())
        if (result == null || result.isMissingNode || (result.isArray && result.isEmpty)) {
            return "undefined"
        }
        val value = if (result.isArray && result.size() == 1) result[0] else result
        return if (value.isTextual) mapper.writeValueAsString(value.textValue())
        else mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
    }

    fun listPaths(input: String, t: JsonTranslator): List<JsonPathEntry> {
        val entries = ArrayList<JsonPathEntry>()
        collectPaths(parsePreserving(input, t), "$", "$", 0, entries)
        return entries
    }

    fun javaBeanToJson(input: String, t: JsonTranslator): String {
        if (input.isBlank()) throw JsonException(t.t("json.error.emptyJavaBean"))
        val result = LinkedHashMap<String, Any?>()
        val fieldPattern = Regex(
            """^(?:(?:public|protected|private)\s+)?(?:(?:static|final|transient|volatile)\s+)*([\w$.<>?, \[\]]+?)\s+(\w+)\s*(?:=.*)?$"""
        )
        for (statement in input.split(';')) {
            val boundary = maxOf(statement.lastIndexOf('{'), statement.lastIndexOf('}'))
            val match = fieldPattern.matchEntire(statement.substring(boundary + 1).trim()) ?: continue
            val type = match.groupValues[1].trim()
            val name = match.groupValues[2]
            if (name == "serialVersionUID") continue
            result[name] = mockJavaValue(type)
        }
        if (result.isEmpty()) throw JsonException(t.t("json.error.noJavaFields"))
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
    }

    fun jsonToJavaBean(input: String, t: JsonTranslator, rootClassName: String = "Root"): String {
        val node = parsePreserving(input, t)
        if (!node.isObject) throw JsonException(t.t("json.error.objectRequired"))
        return JavaBeanEmitter.emit(toPascalCase(rootClassName), node, 0, true)
    }

    private fun parsePreserving(input: String, t: JsonTranslator): JsonNode {
        if (input.isBlank()) throw JsonException(t.t("json.error.empty"))
        return try {
            factory.createParser(input).use { parser ->
                parser.nextToken()
                readNode(parser)
            }
        } catch (error: JsonParseException) {
            throw JsonException(
                error.originalMessage ?: t.t("json.valid.error"),
                error.location?.lineNr,
                error.location?.columnNr
            )
        }
    }

    private fun readNode(parser: JsonParser): JsonNode {
        return when (parser.currentToken) {
            JsonToken.START_OBJECT -> {
                val node = mapper.createObjectNode()
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    val name = parser.currentName
                    parser.nextToken()
                    node.set<JsonNode>(name, readNode(parser))
                }
                node
            }
            JsonToken.START_ARRAY -> {
                val node = mapper.createArrayNode()
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    node.add(readNode(parser))
                }
                node
            }
            JsonToken.VALUE_STRING -> mapper.nodeFactory.textNode(parser.text)
            JsonToken.VALUE_TRUE -> mapper.nodeFactory.booleanNode(true)
            JsonToken.VALUE_FALSE -> mapper.nodeFactory.booleanNode(false)
            JsonToken.VALUE_NULL -> mapper.nodeFactory.nullNode()
            JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT ->
                DecimalNode(BigDecimal(parser.text))
            else -> throw JsonParseException(parser, "Unexpected token ${parser.currentToken}")
        }
    }

    private fun rewrite(
        input: String,
        t: JsonTranslator,
        pretty: Boolean,
        spaces: Int,
        sortKeys: Boolean,
        ignoreCase: Boolean
    ): String {
        val node = parsePreserving(input, t)
        val prepared = if (sortKeys) sortNode(node, ignoreCase) else node
        val writer = StringWriter()
        factory.createGenerator(writer).use { generator ->
            if (pretty) {
                generator.setPrettyPrinter(LiteralPrettyPrinter(spaces))
            }
            writeNode(generator, prepared)
        }
        return writer.toString()
    }

    private fun writeNode(generator: JsonGenerator, node: JsonNode) {
        when {
            node.isObject -> {
                generator.writeStartObject()
                node.fields().forEach { (key, value) ->
                    generator.writeFieldName(key)
                    writeNode(generator, value)
                }
                generator.writeEndObject()
            }
            node.isArray -> {
                generator.writeStartArray()
                node.forEach { writeNode(generator, it) }
                generator.writeEndArray()
            }
            node.isTextual -> generator.writeString(node.textValue())
            node.isBoolean -> generator.writeBoolean(node.booleanValue())
            node.isNull -> generator.writeNull()
            node.isNumber -> generator.writeRawValue(node.decimalValue().toPlainString())
            else -> generator.writeObject(node)
        }
    }

    private fun sortNode(node: JsonNode, ignoreCase: Boolean): JsonNode {
        return when {
            node.isArray -> {
                val array = mapper.createArrayNode()
                node.forEach { array.add(sortNode(it, ignoreCase)) }
                array
            }
            node.isObject -> {
                val comparator = if (ignoreCase) {
                    Comparator<String> { left, right -> left.compareTo(right, ignoreCase = true) }
                } else {
                    Comparator<String> { left, right -> left.compareTo(right) }
                }
                val sorted = TreeMap<String, JsonNode>(comparator)
                node.fields().forEach { (key, value) -> sorted[key] = sortNode(value, ignoreCase) }
                val result = mapper.createObjectNode()
                sorted.forEach { (key, value) -> result.set<JsonNode>(key, value) }
                result
            }
            else -> node
        }
    }

    private fun swapObject(node: JsonNode): ObjectNode {
        val result = mapper.createObjectNode()
        node.fields().forEach { (key, value) ->
            when {
                value.isObject -> result.set<JsonNode>(key, swapObject(value))
                value.isArray -> result.set<JsonNode>(mapper.writeValueAsString(value), mapper.nodeFactory.textNode(key))
                else -> {
                    val swappedKey = if (value.isTextual) value.textValue() else value.toString()
                    result.set<JsonNode>(swappedKey, mapper.nodeFactory.textNode(key))
                }
            }
        }
        return result
    }

    private fun collectPaths(node: JsonNode, path: String, label: String, depth: Int, entries: MutableList<JsonPathEntry>) {
        entries += JsonPathEntry(path, label, preview(node), depth)
        when {
            node.isArray -> node.forEachIndexed { index, child ->
                collectPaths(child, "$path[$index]", "[$index]", depth + 1, entries)
            }
            node.isObject -> node.fields().forEach { (key, child) ->
                val childPath = if (key.matches(Regex("^[a-zA-Z_$][\\w$]*$"))) "$path.$key" else "$path[${mapper.writeValueAsString(key)}]"
                collectPaths(child, childPath, key, depth + 1, entries)
            }
        }
    }

    private fun preview(node: JsonNode): String {
        val raw = when {
            node.isTextual -> mapper.writeValueAsString(node.textValue())
            node.isNumber -> node.decimalValue().toPlainString()
            node.isBoolean || node.isNull -> node.toString()
            node.isArray -> "[${node.size()}]"
            node.isObject -> "{${node.size()}}"
            else -> node.toString()
        }
        return if (raw.length > 80) raw.take(77) + "..." else raw
    }

    private fun describeType(node: JsonNode): String = when {
        node.isArray -> "Array"
        node.isObject -> "Object"
        node.isTextual -> "string"
        node.isNumber -> "number"
        node.isBoolean -> "boolean"
        node.isNull -> "null"
        else -> "value"
    }

    private fun nodeToPlain(node: JsonNode): Any? = when {
        node.isObject -> {
            val map = LinkedHashMap<String, Any?>()
            node.fields().forEach { (key, value) -> map[key] = nodeToPlain(value) }
            map
        }
        node.isArray -> node.map { nodeToPlain(it) }
        node.isTextual -> node.textValue()
        node.isBoolean -> node.booleanValue()
        node.isNumber -> node.decimalValue()
        else -> null
    }

    private fun mockJavaValue(type: String): Any? {
        val normalized = type.replace(" ", "")
        return when {
            normalized.endsWith("[]") || Regex("^(List|Set|Collection|Iterable)<").containsMatchIn(normalized) -> emptyList<Any>()
            Regex("^(Map|HashMap|LinkedHashMap)<").containsMatchIn(normalized) -> emptyMap<String, Any>()
            Regex("^(boolean|Boolean)$").matches(normalized) -> false
            Regex("^(byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double|BigDecimal|BigInteger)$").matches(normalized) -> 0
            Regex("^(char|Character|String|CharSequence)$").matches(normalized) -> ""
            else -> null
        }
    }

    private fun toPascalCase(value: String): String {
        val normalized = value.replace(Regex("[^a-zA-Z0-9]+(.)")) { it.groupValues[1].uppercase(Locale.ROOT) }
        val result = normalized.replaceFirstChar { it.uppercaseChar() }
        return when {
            result.isEmpty() -> "Root"
            result[0].isDigit() -> "Type$result"
            else -> result
        }
    }
}

private class LiteralPrettyPrinter(private val spaces: Int) : com.fasterxml.jackson.core.util.DefaultPrettyPrinter() {
    init {
        _objectFieldValueSeparatorWithSpaces = ": "
        indentArraysWith(FixedSpaceIndenter(spaces))
        indentObjectsWith(FixedSpaceIndenter(spaces))
    }
}

private class FixedSpaceIndenter(private val spaces: Int) : com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter {
    override fun isInline(): Boolean = false
    override fun writeIndentation(g: JsonGenerator, level: Int) {
        g.writeRaw('\n')
        repeat(level * spaces) { g.writeRaw(' ') }
    }
}

private class DuplicateKeyParser(private val source: String, private val ignoreCase: Boolean) {
    private var index = 0
    private val duplicates = ArrayList<String>()

    fun parse(): List<String> {
        parseValue("$")
        return duplicates
    }

    private fun parseValue(path: String) {
        skipWhitespace()
        when (source.getOrNull(index)) {
            '{' -> parseObject(path)
            '[' -> parseArray(path)
            '"' -> parseString()
            else -> parsePrimitive()
        }
    }

    private fun parseObject(path: String) {
        index++
        skipWhitespace()
        val keys = HashSet<String>()
        if (source.getOrNull(index) == '}') {
            index++
            return
        }
        while (index < source.length) {
            skipWhitespace()
            val key = parseString()
            val normalized = if (ignoreCase) key.lowercase(Locale.ROOT) else key
            val keyPath = if (key.matches(Regex("^[a-zA-Z_$][\\w$]*$"))) "$path.$key" else "$path[${JsonEngine.escapeJsonString(key)}]"
            if (!keys.add(normalized)) duplicates += keyPath
            skipWhitespace()
            index++
            parseValue(keyPath)
            skipWhitespace()
            if (source.getOrNull(index++) == '}') return
        }
    }

    private fun parseArray(path: String) {
        index++
        skipWhitespace()
        if (source.getOrNull(index) == ']') {
            index++
            return
        }
        var itemIndex = 0
        while (index < source.length) {
            parseValue("$path[${itemIndex++}]")
            skipWhitespace()
            if (source.getOrNull(index++) == ']') return
        }
    }

    private fun parseString(): String {
        val start = index++
        var escaped = false
        while (index < source.length) {
            val character = source[index++]
            if (escaped) escaped = false
            else if (character == '\\') escaped = true
            else if (character == '"') break
        }
        return ObjectMapper().readValue(source.substring(start, index), String::class.java)
    }

    private fun parsePrimitive() {
        while (index < source.length && source[index] !in " \t\r\n,}]") index++
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }
}

private object XmlRenderer {
    fun render(value: Any?): String = buildString { appendValue("root", value, 0) }

    private fun StringBuilder.appendValue(name: String, value: Any?, depth: Int) {
        when (value) {
            null -> Unit
            is Map<*, *> -> {
                indent(depth).append('<').append(name).append('>').append('\n')
                value.forEach { (key, child) -> appendValue(key.toString(), child, depth + 1) }
                indent(depth).append("</").append(name).append(">\n")
            }
            is List<*> -> value.forEach { appendValue(name, it, depth) }
            else -> {
                indent(depth).append('<').append(name).append('>')
                    .append(escape(value.toString()))
                    .append("</").append(name).append(">\n")
            }
        }
    }

    private fun StringBuilder.indent(depth: Int): StringBuilder {
        repeat(depth * 2) { append(' ') }
        return this
    }

    private fun escape(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                else -> append(ch)
            }
        }
    }
}

private object SimpleXmlParser {
    fun parse(input: String): Map<String, Any?> {
        if (input.contains("<!DOCTYPE", ignoreCase = true) || input.contains("<!ENTITY", ignoreCase = true)) {
            throw JsonException("XML external entity declarations are not allowed")
        }
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val document = factory.newDocumentBuilder().parse(input.byteInputStream())
        val root = document.documentElement
        return mapOf(root.tagName to nodeValue(root))
    }

    private fun nodeValue(node: org.w3c.dom.Element): Any? {
        val children = node.childNodes
        val elements = ArrayList<org.w3c.dom.Element>()
        val text = StringBuilder()
        for (index in 0 until children.length) {
            when (val child = children.item(index)) {
                is org.w3c.dom.Element -> elements += child
                is org.w3c.dom.Text -> text.append(child.nodeValue)
            }
        }
        if (elements.isEmpty()) {
            val raw = text.toString().trim()
            return coerce(raw)
        }
        val grouped = LinkedHashMap<String, Any?>()
        val counts = HashMap<String, Int>()
        for (element in elements) {
            counts[element.tagName] = (counts[element.tagName] ?: 0) + 1
        }
        for (element in elements) {
            val value = nodeValue(element)
            val existing = grouped[element.tagName]
            if (counts[element.tagName]!! > 1) {
                @Suppress("UNCHECKED_CAST")
                val list = (existing as? MutableList<Any?>) ?: arrayListOf<Any?>().also { grouped[element.tagName] = it }
                list += value
            } else {
                grouped[element.tagName] = value
            }
        }
        return grouped
    }

    private fun coerce(value: String): Any = when {
        value.equals("true", true) -> true
        value.equals("false", true) -> false
        value.toLongOrNull() != null -> value.toLong()
        value.toDoubleOrNull() != null -> value.toDouble()
        else -> value
    }
}

private object JavaBeanEmitter {
    fun emit(className: String, node: JsonNode, depth: Int, root: Boolean): String {
        val fields = ArrayList<String>()
        val children = ArrayList<Pair<String, JsonNode>>()
        val indent = "    ".repeat(depth)
        val bodyIndent = "    ".repeat(depth + 1)
        node.fields().forEach { (key, value) ->
            val fieldName = toJavaIdentifier(key)
            val type = inferType(key, value, children)
            fields += "${bodyIndent}private $type $fieldName;"
        }
        val declaration = if (root) "public class $className" else "public static class $className"
        val nested = children.joinToString("\n\n") { (name, child) -> emit(name, child, depth + 1, false) }
        val members = (fields + nested).joinToString("\n\n")
        return "$indent$declaration {\n$members\n$indent}"
    }

    private fun inferType(key: String, value: JsonNode, children: MutableList<Pair<String, JsonNode>>): String {
        return when {
            value.isNull -> "Object"
            value.isTextual -> "String"
            value.isBoolean -> "Boolean"
            value.isNumber && value.decimalValue().scale() <= 0 -> "Long"
            value.isNumber -> "Double"
            value.isArray -> {
                val first = value.firstOrNull { !it.isNull }
                when {
                    first == null -> "List<Object>"
                    first.isObject -> {
                        val name = toPascalCase(singularize(key))
                        children += name to first
                        "List<$name>"
                    }
                    else -> "List<${inferType(key, first, children)}>"
                }
            }
            value.isObject -> {
                val name = toPascalCase(key)
                children += name to value
                name
            }
            else -> "Object"
        }
    }

    private fun toPascalCase(value: String): String {
        val normalized = value.replace(Regex("[^a-zA-Z0-9]+(.)")) { it.groupValues[1].uppercase(Locale.ROOT) }
        val result = normalized.replaceFirstChar { it.uppercaseChar() }
        return if (result.isEmpty()) "Root" else if (result[0].isDigit()) "Type$result" else result
    }

    private fun toJavaIdentifier(value: String): String {
        val normalized = value.replace(Regex("[^a-zA-Z0-9_$]"), "_").ifEmpty { "value" }
        return if (normalized[0].isDigit()) "_$normalized" else normalized
    }

    private fun singularize(value: String): String = when {
        value.endsWith("ies") -> value.dropLast(3) + "y"
        value.endsWith("s") -> value.dropLast(1)
        else -> value
    }
}
