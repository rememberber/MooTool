package com.rememberber.mootool.nextfx.domain.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON algorithms aligned with Electron {@code jsonTools.ts}, with BigInteger/BigDecimal
 * preservation (FX-D001) and streaming duplicate-key detection before Map collapse.
 */
public final class JsonEngine {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.withExactBigDecimals(true);
    private static final ObjectMapper MAPPER = createMapper();
    private static final Configuration JSON_PATH_CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider(MAPPER))
            .mappingProvider(new JacksonMappingProvider(MAPPER))
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();
    private static final Pattern JAVA_FIELD = Pattern.compile(
            "^(?:(?:public|protected|private)\\s+)?(?:(?:static|final|transient|volatile)\\s+)*([\\w$.<>?, \\[\\]]+?)\\s+(\\w+)\\s*(?:=.*)?$"
    );
    private static final Pattern SAFE_KEY = Pattern.compile("^[a-zA-Z_$][\\w$]*$");

    private JsonEngine() {
    }

    public static String format(String input, int spaces) {
        return write(parse(input), spaces);
    }

    public static String compress(String input) {
        return write(parse(input), 0);
    }

    public static String formatAdvanced(String input, JsonFormatOptions options) {
        if (options.checkDuplicateKeys()) {
            List<String> duplicates = findDuplicateKeys(input, options.ignoreCase());
            if (!duplicates.isEmpty()) {
                throw new JsonException("json.error.duplicateKeys", String.join(", ", duplicates));
            }
        }
        JsonNode parsed = parse(input);
        JsonNode value = options.sortKeys() ? sortKeys(parsed, options.ignoreCase()) : parsed;
        return write(value, options.spaces());
    }

    public static JsonStatus validate(String input) {
        if (input == null || input.isBlank()) {
            return JsonStatus.idle();
        }
        try {
            JsonNode value = parse(input);
            String type;
            if (value.isArray()) {
                type = "Array";
            } else if (value.isObject()) {
                type = "Object";
            } else if (value.isTextual()) {
                type = "string";
            } else if (value.isBoolean()) {
                type = "boolean";
            } else if (value.isNumber()) {
                type = "number";
            } else if (value.isNull()) {
                type = "null";
            } else {
                type = value.getNodeType().name().toLowerCase(Locale.ROOT);
            }
            return JsonStatus.valid(type);
        } catch (JsonException exception) {
            return JsonStatus.error(exception.getMessage());
        }
    }

    public static String escapeJsonString(String input) {
        try {
            return MAPPER.writeValueAsString(input == null ? "" : input);
        } catch (JsonProcessingException exception) {
            throw new JsonException("json.valid.error", exception.getOriginalMessage());
        }
    }

    public static String unescapeJsonString(String input) {
        JsonNode parsed = parse(input);
        if (!parsed.isTextual()) {
            throw new JsonException("json.error.notString", "");
        }
        return parsed.textValue();
    }

    public static String escapeJavaString(String input) {
        return (input == null ? "" : input)
                .replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }

    public static String unescapeJsonText(String input) {
        try {
            return MAPPER.readValue("\"" + (input == null ? "" : input).replace("\"", "\\\"") + "\"", String.class);
        } catch (JsonProcessingException exception) {
            throw new JsonException("json.valid.error", exception.getOriginalMessage());
        }
    }

    public static String jsonToXml(String input) {
        return toXml(parse(input), "root", 0).trim() + "\n";
    }

    public static String xmlToJson(String input) {
        if (input == null || input.isBlank()) {
            throw new JsonException("json.error.emptyXml", "");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(input)));
            document.getDocumentElement().normalize();
            Element root = document.getDocumentElement();
            ObjectNode wrapper = NODE_FACTORY.objectNode();
            wrapper.set(root.getNodeName(), elementContent(root));
            return write(wrapper, 2);
        } catch (JsonException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonException("json.valid.error", exception.getMessage());
        }
    }

    public static String queryJsonPath(String input, String path) {
        if (path == null || path.isBlank()) {
            throw new JsonException("json.error.emptyPath", "");
        }
        JsonNode document = parse(input);
        try {
            Object result = JsonPath.using(JSON_PATH_CONFIGURATION).parse(document).read(path.trim());
            return formatPathValue(result);
        } catch (PathNotFoundException exception) {
            return "undefined";
        }
    }

    public static String swapKeysAndValues(String input) {
        JsonNode value = parse(input);
        if (!value.isObject()) {
            throw new JsonException("json.error.objectRequired", "");
        }
        return write(swapObject(value), 2);
    }

    public static String javaBeanToJson(String input) {
        if (input == null || input.isBlank()) {
            throw new JsonException("json.error.emptyJavaBean", "");
        }
        ObjectNode result = NODE_FACTORY.objectNode();
        for (String statement : input.split(";")) {
            int brace = Math.max(statement.lastIndexOf('{'), statement.lastIndexOf('}'));
            String candidate = statement.substring(brace + 1).trim();
            Matcher matcher = JAVA_FIELD.matcher(candidate);
            if (!matcher.matches()) {
                continue;
            }
            String type = matcher.group(1).trim();
            String name = matcher.group(2);
            if ("serialVersionUID".equals(name)) {
                continue;
            }
            result.set(name, mockJavaValue(type));
        }
        if (result.isEmpty()) {
            throw new JsonException("json.error.noJavaFields", "");
        }
        return write(result, 2);
    }

    public static String jsonToJavaBean(String input, String rootClassName) {
        JsonNode value = parse(input);
        if (!value.isObject()) {
            throw new JsonException("json.error.objectRequired", "");
        }
        String className = toPascalCase(rootClassName == null || rootClassName.isBlank() ? "Root" : rootClassName);
        return buildJavaClass(className, value, 0, true);
    }

    public static List<String> findDuplicateKeys(String input, boolean ignoreCase) {
        parse(input);
        return new DuplicateKeyParser(input, ignoreCase).parse();
    }

    public static List<JsonPathEntry> listPaths(String input) {
        List<JsonPathEntry> entries = new ArrayList<>();
        collectPaths(parse(input), "$", "$", 0, entries);
        return List.copyOf(entries);
    }

    public static JsonNode parse(String input) {
        if (input == null || input.isBlank()) {
            throw new JsonException("json.error.empty", "");
        }
        try {
            return MAPPER.readTree(input);
        } catch (JsonProcessingException exception) {
            throw new JsonException("json.valid.error", exception.getOriginalMessage());
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setNodeFactory(NODE_FACTORY);
        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.getFactory().disable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        return mapper;
    }

    private static String write(JsonNode value, int spaces) {
        try {
            if (spaces <= 0) {
                return MAPPER.writeValueAsString(value);
            }
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter() {
                @Override
                public DefaultPrettyPrinter createInstance() {
                    return this;
                }
            };
            DefaultIndenter indenter = new DefaultIndenter(" ".repeat(spaces), "\n");
            printer.indentObjectsWith(indenter);
            printer.indentArraysWith(indenter);
            StringWriter writer = new StringWriter();
            try (JsonGenerator generator = MAPPER.createGenerator(writer)) {
                generator.setPrettyPrinter(printer);
                MAPPER.writeTree(generator, value);
            }
            return writer.toString();
        } catch (IOException exception) {
            throw new JsonException("json.valid.error", exception.getMessage());
        }
    }

    private static JsonNode sortKeys(JsonNode value, boolean ignoreCase) {
        if (value.isArray()) {
            ArrayNode array = NODE_FACTORY.arrayNode();
            for (JsonNode item : value) {
                array.add(sortKeys(item, ignoreCase));
            }
            return array;
        }
        if (!value.isObject()) {
            return value;
        }
        Comparator<String> comparator = ignoreCase
                ? String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())
                : Comparator.naturalOrder();
        ObjectNode sorted = NODE_FACTORY.objectNode();
        List<String> names = new ArrayList<>();
        value.fieldNames().forEachRemaining(names::add);
        names.sort(comparator);
        for (String name : names) {
            sorted.set(name, sortKeys(value.get(name), ignoreCase));
        }
        return sorted;
    }

    private static void collectPaths(JsonNode value, String path, String label, int depth, List<JsonPathEntry> entries) {
        entries.add(new JsonPathEntry(path, label, value, depth));
        if (value.isArray()) {
            int index = 0;
            for (JsonNode item : value) {
                collectPaths(item, path + "[" + index + "]", "[" + index + "]", depth + 1, entries);
                index++;
            }
            return;
        }
        if (value.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String childPath = SAFE_KEY.matcher(field.getKey()).matches()
                        ? path + "." + field.getKey()
                        : path + "[" + escapeJsonString(field.getKey()) + "]";
                collectPaths(field.getValue(), childPath, field.getKey(), depth + 1, entries);
            }
        }
    }

    private static ObjectNode swapObject(JsonNode value) {
        ObjectNode result = NODE_FACTORY.objectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode item = field.getValue();
            if (item.isObject()) {
                result.set(field.getKey(), swapObject(item));
                continue;
            }
            String swappedKey = item.isArray() ? write(item, 0) : stringifyScalar(item);
            result.put(swappedKey, field.getKey());
        }
        return result;
    }

    private static String stringifyScalar(JsonNode item) {
        if (item.isTextual()) {
            return item.textValue();
        }
        if (item.isNull()) {
            return "null";
        }
        if (item.isBoolean() || item.isNumber()) {
            return item.asText();
        }
        return write(item, 0);
    }

    private static String formatPathValue(Object value) {
        if (value == null) {
            return "undefined";
        }
        if (value instanceof String text) {
            return escapeJsonString(text);
        }
        if (value instanceof JsonNode node) {
            if (node.isMissingNode() || node.isNull() && node.asText() == null) {
                return write(node, 2);
            }
            if (node.isTextual()) {
                return escapeJsonString(node.textValue());
            }
            return write(node, 2);
        }
        JsonNode tree = MAPPER.valueToTree(value);
        if (tree.isTextual()) {
            return escapeJsonString(tree.textValue());
        }
        return write(tree, 2);
    }

    private static JsonNode mockJavaValue(String type) {
        String normalized = type.replace(" ", "");
        if (normalized.endsWith("[]") || normalized.matches("^(List|Set|Collection|Iterable)<.*")) {
            return NODE_FACTORY.arrayNode();
        }
        if (normalized.matches("^(Map|HashMap|LinkedHashMap)<.*")) {
            return NODE_FACTORY.objectNode();
        }
        if (normalized.matches("^(boolean|Boolean)$")) {
            return NODE_FACTORY.booleanNode(false);
        }
        if (normalized.matches("^(byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double|BigDecimal|BigInteger)$")) {
            return NODE_FACTORY.numberNode(0);
        }
        if (normalized.matches("^(char|Character|String|CharSequence)$")) {
            return NODE_FACTORY.textNode("");
        }
        return NODE_FACTORY.nullNode();
    }

    private static String buildJavaClass(String className, JsonNode value, int depth, boolean root) {
        List<String> fields = new ArrayList<>();
        List<String> children = new ArrayList<>();
        String indent = "    ".repeat(depth);
        String bodyIndent = "    ".repeat(depth + 1);
        Iterator<Map.Entry<String, JsonNode>> iterator = value.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> field = iterator.next();
            List<ChildClass> nested = new ArrayList<>();
            String type = inferJavaType(field.getKey(), field.getValue(), nested);
            fields.add(bodyIndent + "private " + type + " " + toJavaIdentifier(field.getKey()) + ";");
            for (ChildClass child : nested) {
                children.add(buildJavaClass(child.name, child.value, depth + 1, false));
            }
        }
        String declaration = root ? "public class " + className : "public static class " + className;
        List<String> members = new ArrayList<>(fields);
        members.addAll(children);
        return indent + declaration + " {\n" + String.join("\n\n", members) + "\n" + indent + "}";
    }

    private static String inferJavaType(String key, JsonNode value, List<ChildClass> childClasses) {
        if (value == null || value.isNull()) {
            return "Object";
        }
        if (value.isTextual()) {
            return "String";
        }
        if (value.isBoolean()) {
            return "Boolean";
        }
        if (value.isNumber()) {
            return value.isFloatingPointNumber() ? "Double" : "Long";
        }
        if (value.isArray()) {
            JsonNode first = null;
            for (JsonNode item : value) {
                if (!item.isNull()) {
                    first = item;
                    break;
                }
            }
            if (first == null) {
                return "List<Object>";
            }
            if (first.isObject()) {
                String name = toPascalCase(singularize(key));
                childClasses.add(new ChildClass(name, first));
                return "List<" + name + ">";
            }
            return "List<" + inferJavaType(key, first, childClasses) + ">";
        }
        if (value.isObject()) {
            String name = toPascalCase(key);
            childClasses.add(new ChildClass(name, value));
            return name;
        }
        return "Object";
    }

    private static String toPascalCase(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!Character.isLetterOrDigit(character)) {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(character) : character);
            upper = false;
        }
        String result = builder.toString();
        if (result.isEmpty()) {
            return "Root";
        }
        return Character.isDigit(result.charAt(0)) ? "Type" + result : result;
    }

    private static String toJavaIdentifier(String value) {
        String normalized = value.replaceAll("[^a-zA-Z0-9_$]", "_");
        if (normalized.isEmpty()) {
            normalized = "value";
        }
        return Character.isDigit(normalized.charAt(0)) ? "_" + normalized : normalized;
    }

    private static String singularize(String value) {
        if (value.endsWith("ies")) {
            return value.substring(0, value.length() - 3) + "y";
        }
        if (value.endsWith("s")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String toXml(JsonNode value, String tag, int depth) {
        String indent = "  ".repeat(depth);
        if (value.isObject()) {
            StringBuilder builder = new StringBuilder();
            builder.append(indent).append('<').append(tag).append(">\n");
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                builder.append(toXml(field.getValue(), sanitizeTag(field.getKey()), depth + 1));
            }
            builder.append(indent).append("</").append(tag).append(">\n");
            return builder.toString();
        }
        if (value.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : value) {
                builder.append(toXml(item, tag, depth));
            }
            return builder.toString();
        }
        if (value.isNull()) {
            return "";
        }
        return indent + "<" + tag + ">" + escapeXml(stringifyScalar(value)) + "</" + tag + ">\n";
    }

    private static JsonNode elementContent(Element element) {
        ObjectNode object = NODE_FACTORY.objectNode();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            object.put("@" + attribute.getNodeName(), attribute.getNodeValue());
        }
        NodeList children = element.getChildNodes();
        Map<String, List<JsonNode>> grouped = new LinkedHashMap<>();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                grouped.computeIfAbsent(child.getNodeName(), key -> new ArrayList<>()).add(elementContent((Element) child));
            } else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(child.getTextContent());
            }
        }
        String trimmed = text.toString().trim();
        if (grouped.isEmpty() && attributes.getLength() == 0) {
            return coerceXmlValue(trimmed);
        }
        for (Map.Entry<String, List<JsonNode>> entry : grouped.entrySet()) {
            if (entry.getValue().size() == 1) {
                object.set(entry.getKey(), unwrapSingle(entry.getValue().getFirst()));
            } else {
                ArrayNode array = NODE_FACTORY.arrayNode();
                for (JsonNode item : entry.getValue()) {
                    array.add(unwrapSingle(item));
                }
                object.set(entry.getKey(), array);
            }
        }
        if (!trimmed.isEmpty() && grouped.isEmpty()) {
            return object.size() == 0 ? coerceXmlValue(trimmed) : object;
        }
        return object.size() == 1 && attributes.getLength() == 0 ? object : object;
    }

    private static JsonNode unwrapSingle(JsonNode node) {
        return node;
    }

    private static JsonNode coerceXmlValue(String text) {
        if (text.isEmpty()) {
            return NODE_FACTORY.textNode("");
        }
        if ("true".equals(text) || "false".equals(text)) {
            return NODE_FACTORY.booleanNode(Boolean.parseBoolean(text));
        }
        try {
            if (text.contains(".") || text.contains("e") || text.contains("E")) {
                return NODE_FACTORY.numberNode(new BigDecimal(text));
            }
            return NODE_FACTORY.numberNode(new BigInteger(text));
        } catch (NumberFormatException ignored) {
            return NODE_FACTORY.textNode(text);
        }
    }

    private static String sanitizeTag(String name) {
        if (SAFE_KEY.matcher(name).matches()) {
            return name;
        }
        return name.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record ChildClass(String name, JsonNode value) {
    }

    static final class DuplicateKeyParser {
        private final String source;
        private final boolean ignoreCase;
        private final List<String> duplicates = new ArrayList<>();
        private int index;

        DuplicateKeyParser(String source, boolean ignoreCase) {
            this.source = source;
            this.ignoreCase = ignoreCase;
        }

        List<String> parse() {
            parseValue("$");
            return List.copyOf(duplicates);
        }

        private void parseValue(String path) {
            skipWhitespace();
            if (index >= source.length()) {
                return;
            }
            char token = source.charAt(index);
            if (token == '{') {
                parseObject(path);
            } else if (token == '[') {
                parseArray(path);
            } else if (token == '"') {
                parseString();
            } else {
                parsePrimitive();
            }
        }

        private void parseObject(String path) {
            index++;
            skipWhitespace();
            Set<String> keys = new java.util.HashSet<>();
            if (index < source.length() && source.charAt(index) == '}') {
                index++;
                return;
            }
            while (index < source.length()) {
                skipWhitespace();
                String key = parseString();
                String normalized = ignoreCase ? key.toLowerCase(Locale.ROOT) : key;
                String keyPath = SAFE_KEY.matcher(key).matches() ? path + "." + key : path + "[" + escapeJsonString(key) + "]";
                if (keys.contains(normalized)) {
                    duplicates.add(keyPath);
                }
                keys.add(normalized);
                skipWhitespace();
                if (index < source.length()) {
                    index++;
                }
                parseValue(keyPath);
                skipWhitespace();
                if (index >= source.length()) {
                    return;
                }
                char next = source.charAt(index++);
                if (next == '}') {
                    return;
                }
            }
        }

        private void parseArray(String path) {
            index++;
            skipWhitespace();
            if (index < source.length() && source.charAt(index) == ']') {
                index++;
                return;
            }
            int itemIndex = 0;
            while (index < source.length()) {
                parseValue(path + "[" + itemIndex++ + "]");
                skipWhitespace();
                if (index >= source.length()) {
                    return;
                }
                char next = source.charAt(index++);
                if (next == ']') {
                    return;
                }
            }
        }

        private String parseString() {
            int start = index++;
            boolean escaped = false;
            while (index < source.length()) {
                char character = source.charAt(index++);
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    break;
                }
            }
            try {
                return MAPPER.readValue(source.substring(start, index), String.class);
            } catch (JsonProcessingException exception) {
                throw new JsonException("json.valid.error", exception.getOriginalMessage());
            }
        }

        private void parsePrimitive() {
            while (index < source.length() && !Character.isWhitespace(source.charAt(index))) {
                char character = source.charAt(index);
                if (character == ',' || character == '}' || character == ']') {
                    break;
                }
                index++;
            }
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }
    }
}
