package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val t = JsonTranslator { key, params ->
    var message = when (key) {
        "json.valid.idle" -> "idle"
        "json.valid.ok" -> "valid ${params["type"]}"
        "json.valid.error" -> "invalid"
        "json.error.empty" -> "empty"
        "json.error.notString" -> "not string"
        "json.error.duplicateKeys" -> "duplicates ${params["paths"]}"
        "json.error.objectRequired" -> "object required"
        "json.error.emptyJavaBean" -> "empty bean"
        "json.error.noJavaFields" -> "no fields"
        "json.error.emptyXml" -> "empty xml"
        "json.error.emptyPath" -> "empty path"
        else -> key
    }
    params.forEach { (name, value) -> message = message.replace("{$name}", value) }
    message
}

class JsonEngineTest {
    @Test
    fun formatsAndCompressesWithoutChangingValue() {
        val input = """{"name":"MooTool","items":[1,2]}"""
        assertEquals("{\n  \"name\": \"MooTool\",\n  \"items\": [\n    1,\n    2\n  ]\n}", JsonEngine.format(input, t, 2))
        assertEquals(input, JsonEngine.compress(JsonEngine.format(input, t), t))
    }

    @Test
    fun preservesLargeIntegerLiteral() {
        val input = """{"id":9007199254740993}"""
        assertTrue(JsonEngine.format(input, t).contains("9007199254740993"))
        assertEquals(input, JsonEngine.compress(input, t).replace(" ", ""))
    }

    @Test
    fun reportsIdleValidAndInvalid() {
        assertEquals(JsonStatus.Kind.Idle, JsonEngine.validate("", t).kind)
        assertEquals("valid Array", JsonEngine.validate("[]", t).message)
        assertEquals(JsonStatus.Kind.Error, JsonEngine.validate("{", t).kind)
    }

    @Test
    fun sortsKeysAndDetectsDuplicates() {
        val input = """{"z":{"B":1,"a":2},"A":0}"""
        assertEquals(
            "{\n  \"A\": 0,\n  \"z\": {\n    \"a\": 2,\n    \"B\": 1\n  }\n}",
            JsonEngine.formatAdvanced(input, t, JsonFormatOptions(2, sortKeys = true, ignoreCase = true, checkDuplicateKeys = true))
        )
        assertEquals(listOf("$.A", "$.child.x"), JsonEngine.findDuplicateKeys("""{"a":1,"A":2,"child":{"x":1,"x":2}}""", true))
    }

    @Test
    fun convertsJsonAndXml() {
        val xml = JsonEngine.jsonToXml("""{"name":"MooTool","enabled":true}""", t)
        assertTrue(xml.contains("<name>MooTool</name>"))
        val json = JsonEngine.xmlToJson("<tool><name>MooTool</name><enabled>true</enabled></tool>", t)
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("MooTool"))
    }

    @Test
    fun queriesAndEnumeratesPaths() {
        val input = """{"store":{"books":[{"title":"One"},{"title":"Two"}]}}"""
        assertEquals("\"Two\"", JsonEngine.queryPath(input, "$.store.books[1].title", t))
        val titles = JsonEngine.queryPath(input, "$.store.books[*].title", t)
        assertTrue(titles.contains("One"))
        assertTrue(JsonEngine.listPaths(input, t).map { it.path }.contains("$.store.books[0].title"))
    }

    @Test
    fun swapsKeysAndConvertsJavaBean() {
        val swapped = JsonEngine.swapKeysAndValues("""{"first":"one","second":2}""", t)
        assertTrue(swapped.contains("\"one\""))
        val bean = JsonEngine.javaBeanToJson("public class User { private String name; private int age; private List<String> tags; }", t)
        assertTrue(bean.contains("\"name\""))
        val source = JsonEngine.jsonToJavaBean("""{"name":"MooTool","profile":{"active":true}}""", t, "ToolConfig")
        assertTrue(source.contains("public class ToolConfig"))
        assertTrue(source.contains("private Profile profile;"))
        assertTrue(source.contains("public static class Profile"))
    }
}
