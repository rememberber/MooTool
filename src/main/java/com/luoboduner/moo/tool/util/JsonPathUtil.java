package com.luoboduner.moo.tool.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * JSON Path 取值与结果格式化。
 */
public final class JsonPathUtil {

    private JsonPathUtil() {
    }

    public static Object getByPath(String jsonText, String jsonPath) {
        String trimmedPath = StringUtils.trimToEmpty(jsonPath);
        if (StringUtils.isBlank(trimmedPath)) {
            throw new IllegalArgumentException("JSON Path cannot be empty");
        }
        if (StringUtils.isBlank(jsonText)) {
            throw new IllegalArgumentException("JSON text cannot be empty");
        }
        return JSONUtil.getByPath(JSONUtil.parse(jsonText), trimmedPath);
    }

    public static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return JSONUtil.toJsonPrettyStr(value);
        }
        if (value instanceof String || value instanceof Boolean || value instanceof Number) {
            return formatPrimitiveJsonValue(value);
        }
        return String.valueOf(value);
    }

    public static String getFormattedValue(String jsonText, String jsonPath) {
        return formatValue(getByPath(jsonText, jsonPath));
    }

    private static String formatPrimitiveJsonValue(Object value) {
        JSONArray wrapper = new JSONArray();
        wrapper.add(value);
        String json = wrapper.toString();
        return json.substring(1, json.length() - 1);
    }
}
