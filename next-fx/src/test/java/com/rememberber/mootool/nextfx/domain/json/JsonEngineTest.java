package com.rememberber.mootool.nextfx.domain.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonEngineTest {

    @Test
    void formatsAndCompressesWithoutChangingValue() {
        String input = "{\"name\":\"MooTool\",\"items\":[1,2]}";
        String formatted = JsonEngine.format(input, 2);
        assertThat(formatted).contains("\"name\"").contains("MooTool").contains("\n");
        assertThat(JsonEngine.compress(formatted)).isEqualTo(input);
    }

    @Test
    void reportsIdleValidAndInvalidInput() {
        assertThat(JsonEngine.validate("").kind()).isEqualTo(JsonStatus.Kind.IDLE);
        assertThat(JsonEngine.validate("[]")).isEqualTo(JsonStatus.valid("Array"));
        assertThat(JsonEngine.validate("{").kind()).isEqualTo(JsonStatus.Kind.ERROR);
    }

    @Test
    void sortsKeysAndDetectsDuplicatesBeforeMapCollapse() {
        String input = "{\"z\":{\"B\":1,\"a\":2},\"A\":0}";
        assertThat(JsonEngine.formatAdvanced(input, new JsonFormatOptions(2, true, true, true)))
                .contains("\"A\"")
                .contains("\"a\"")
                .contains("\"B\"");
        assertThat(JsonEngine.findDuplicateKeys("{\"a\":1,\"A\":2,\"child\":{\"x\":1,\"x\":2}}", true))
                .containsExactly("$.A", "$.child.x");
    }

    @Test
    void preservesIntegerLargerThanIeeeDouble() {
        String input = "{\"id\":9007199254740993}";
        assertThat(JsonEngine.compress(input)).contains("9007199254740993");
        assertThat(JsonEngine.format(input, 2)).contains("9007199254740993");
    }

    @Test
    void convertsJsonAndXml() {
        String xml = JsonEngine.jsonToXml("{\"name\":\"MooTool\",\"enabled\":true}");
        assertThat(xml).contains("<name>MooTool</name>");
        assertThat(JsonEngine.compress(JsonEngine.xmlToJson("<tool><name>MooTool</name><enabled>true</enabled></tool>")))
                .isEqualTo("{\"tool\":{\"name\":\"MooTool\",\"enabled\":true}}");
    }

    @Test
    void queriesAndEnumeratesJsonPaths() {
        String input = "{\"store\":{\"books\":[{\"title\":\"One\"},{\"title\":\"Two\"}]}}";
        assertThat(JsonEngine.queryJsonPath(input, "$.store.books[1].title")).isEqualTo("\"Two\"");
        assertThat(JsonEngine.listPaths(input).stream().map(JsonPathEntry::path))
                .contains("$.store.books[0].title");
    }

    @Test
    void swapsObjectKeysAndValues() {
        assertThat(JsonEngine.compress(JsonEngine.swapKeysAndValues("{\"first\":\"one\",\"second\":2}")))
                .isEqualTo("{\"one\":\"first\",\"2\":\"second\"}");
    }

    @Test
    void convertsJavaBeanFields() {
        String json = JsonEngine.javaBeanToJson("public class User { private String name; private int age; private List<String> tags; }");
        assertThat(JsonEngine.compress(json)).isEqualTo("{\"name\":\"\",\"age\":0,\"tags\":[]}");
        String source = JsonEngine.jsonToJavaBean("{\"name\":\"MooTool\",\"profile\":{\"active\":true}}", "ToolConfig");
        assertThat(source).contains("public class ToolConfig");
        assertThat(source).contains("private Profile profile;");
        assertThat(source).contains("public static class Profile");
        assertThat(source.trim()).endsWith("}");
    }

    @Test
    void formatsThreeMegabyteObjectWithoutDroppingKeys() {
        StringBuilder builder = new StringBuilder("{\"blob\":\"");
        while (builder.length() < 3_100_000) {
            builder.append("abcdefghij");
        }
        builder.append("\",\"id\":9007199254740993}");
        String input = builder.toString();
        assertThat(input.length()).isGreaterThan(3_000_000);
        String formatted = JsonEngine.format(input, 2);
        assertThat(formatted).contains("9007199254740993");
        assertThat(JsonEngine.validate(formatted).kind()).isEqualTo(JsonStatus.Kind.VALID);
        assertThat(JsonEngine.compress(formatted).length()).isGreaterThan(3_000_000);
    }

    @Test
    void emptyInputFailsWithStableKey() {
        assertThatThrownBy(() -> JsonEngine.format("  ", 2))
                .isInstanceOf(JsonException.class)
                .extracting(error -> ((JsonException) error).messageKey())
                .isEqualTo("json.error.empty");
    }
}
