package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickNoteFrontmatterTest {

    @Test
    void parseAndSerializeRoundTrip() {
        String raw = """
                ---
                title: 测试笔记
                syntax: text/markdown
                font_name: 等线
                font_size: "14"
                color: default
                line_wrap: "1"
                created_at: 2024-01-01 10:00:00
                modified_at: 2024-01-02 11:00:00
                ---
                正文第一行
                正文第二行
                """;

        QuickNoteFrontmatter.ParsedNote parsed = QuickNoteFrontmatter.parse(raw);
        assertEquals("测试笔记", parsed.getString("title"));
        assertEquals("text/markdown", parsed.getString("syntax"));
        assertEquals("14", parsed.getString("font_size"));
        assertTrue(parsed.getBody().contains("正文第一行"));

        Map<String, Object> metadata = new LinkedHashMap<>(parsed.getMetadata());
        String serialized = QuickNoteFrontmatter.serialize(metadata, parsed.getBody().trim());
        QuickNoteFrontmatter.ParsedNote again = QuickNoteFrontmatter.parse(serialized);
        assertEquals("测试笔记", again.getString("title"));
        assertEquals(parsed.getBody().trim(), again.getBody().trim());
    }

    @Test
    void parsePlainTextWithoutFrontmatter() {
        QuickNoteFrontmatter.ParsedNote parsed = QuickNoteFrontmatter.parse("plain body");
        assertEquals("plain body", parsed.getBody());
        assertTrue(parsed.getMetadata().isEmpty());
    }
}
