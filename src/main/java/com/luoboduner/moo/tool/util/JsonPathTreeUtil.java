package com.luoboduner.moo.tool.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * 将 JSON 构建为树形结构，并生成 Hutool 兼容的 JSON Path。
 */
public final class JsonPathTreeUtil {

    private static final int MAX_DISPLAY_LENGTH = 80;

    private JsonPathTreeUtil() {
    }

    public static DefaultTreeModel buildTreeModel(String jsonText) {
        Object parsed = JSONUtil.parse(jsonText);
        DefaultMutableTreeNode root = buildNode(parsed, "$", "(root)");
        return new DefaultTreeModel(root);
    }

    public static DefaultMutableTreeNode buildNode(Object value, String jsonPath, String label) {
        JsonPathNode data = new JsonPathNode(label, jsonPath, value);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
        appendChildren(node, value, jsonPath);
        return node;
    }

    private static void appendChildren(DefaultMutableTreeNode parentNode, Object value, String parentPath) {
        if (value instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object childValue = jsonObject.get(key);
                String childPath = appendObjectPath(parentPath, key);
                String display = formatObjectChildLabel(key, childValue);
                DefaultMutableTreeNode childNode = buildNode(childValue, childPath, display);
                parentNode.add(childNode);
            }
        } else if (value instanceof JSONArray jsonArray) {
            for (int i = 0; i < jsonArray.size(); i++) {
                Object childValue = jsonArray.get(i);
                String childPath = parentPath + "[" + i + "]";
                String display = formatArrayChildLabel(i, childValue);
                DefaultMutableTreeNode childNode = buildNode(childValue, childPath, display);
                parentNode.add(childNode);
            }
        }
    }

    static String appendObjectPath(String parentPath, String key) {
        if (needsBracketNotation(key)) {
            return parentPath + "['" + escapeKeyForPath(key) + "']";
        }
        return parentPath + "." + key;
    }

    static boolean needsBracketNotation(String key) {
        if (key == null || key.isEmpty()) {
            return true;
        }
        if (Character.isDigit(key.charAt(0))) {
            return true;
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return true;
            }
        }
        return false;
    }

    static String escapeKeyForPath(String key) {
        return key.replace("\\", "\\\\").replace("'", "\\'");
    }

    static String formatObjectChildLabel(String key, Object value) {
        return key + ": " + formatValueSummary(value);
    }

    static String formatArrayChildLabel(int index, Object value) {
        return "[" + index + "]: " + formatValueSummary(value);
    }

    static String formatValueSummary(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof JSONObject jsonObject) {
            return "{...} (" + jsonObject.size() + " keys)";
        }
        if (value instanceof JSONArray jsonArray) {
            return "[...] (" + jsonArray.size() + " items)";
        }
        if (value instanceof String str) {
            return "\"" + truncate(str) + "\"";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        return truncate(String.valueOf(value));
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_DISPLAY_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_DISPLAY_LENGTH) + "...";
    }

    public static final class JsonPathNode {
        private final String displayText;
        private final String jsonPath;
        private final Object value;

        public JsonPathNode(String displayText, String jsonPath, Object value) {
            this.displayText = displayText;
            this.jsonPath = jsonPath;
            this.value = value;
        }

        public String getDisplayText() {
            return displayText;
        }

        public String getJsonPath() {
            return jsonPath;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }
}
