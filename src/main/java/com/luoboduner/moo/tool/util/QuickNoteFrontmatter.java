package com.luoboduner.moo.tool.util;

import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 随手记 txt 文件头部 YAML frontmatter 解析与序列化。
 * 元数据对编辑器不可见，仅用于持久化语法、字体、颜色等设置。
 */
public final class QuickNoteFrontmatter {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "\\A---\\r?\\n(.*?)\\r?\\n---\\r?\\n?", Pattern.DOTALL);

    private QuickNoteFrontmatter() {
    }

    public static class ParsedNote {
        private final Map<String, Object> metadata;
        private final String body;

        public ParsedNote(Map<String, Object> metadata, String body) {
            this.metadata = metadata != null ? metadata : Map.of();
            this.body = body != null ? body : "";
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public String getBody() {
            return body;
        }

        public String getString(String key) {
            Object value = metadata.get(key);
            return value == null ? null : String.valueOf(value);
        }
    }

    public static ParsedNote parse(String rawFileContent) {
        if (StringUtils.isBlank(rawFileContent)) {
            return new ParsedNote(Map.of(), "");
        }
        Matcher matcher = FRONTMATTER_PATTERN.matcher(rawFileContent);
        if (!matcher.find()) {
            return new ParsedNote(Map.of(), rawFileContent);
        }
        String yamlBlock = matcher.group(1);
        String body = rawFileContent.substring(matcher.end());
        Map<String, Object> metadata = loadYaml(yamlBlock);
        return new ParsedNote(metadata, body);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String yamlBlock) {
        if (StringUtils.isBlank(yamlBlock)) {
            return Map.of();
        }
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(yamlBlock);
        if (loaded instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    public static String serialize(Map<String, Object> metadata, String body) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        if (metadata != null) {
            ordered.putAll(metadata);
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);
        String yamlText = yaml.dump(ordered).trim();
        String normalizedBody = body == null ? "" : body;
        if (!normalizedBody.isEmpty() && !normalizedBody.startsWith("\n")) {
            normalizedBody = "\n" + normalizedBody;
        }
        return "---\n" + yamlText + "\n---" + normalizedBody;
    }

    public static Map<String, Object> defaultMetadata(String title) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", title);
        metadata.put("syntax", SyntaxConstants.SYNTAX_STYLE_NONE);
        metadata.put("font_name", "");
        metadata.put("font_size", "");
        metadata.put("color", "default");
        metadata.put("line_wrap", "0");
        metadata.put("created_at", SqliteUtil.nowDateForSqlite());
        metadata.put("modified_at", SqliteUtil.nowDateForSqlite());
        return metadata;
    }
}
