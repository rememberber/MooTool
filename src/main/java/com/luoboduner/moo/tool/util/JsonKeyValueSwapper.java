package com.luoboduner.moo.tool.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class JsonKeyValueSwapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String swapKeysAndValues(String jsonString) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        ObjectNode swappedNode = objectMapper.createObjectNode();
        swapKeysAndValues(rootNode, swappedNode);
        return objectMapper.writeValueAsString(swappedNode);
    }

    private static void swapKeysAndValues(JsonNode originalNode, ObjectNode swappedNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = originalNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();
            if (value.isObject()) {
                ObjectNode nestedSwappedNode = objectMapper.createObjectNode();
                swapKeysAndValues(value, nestedSwappedNode);
                swappedNode.set(value.asText(), nestedSwappedNode);
            } else {
                swappedNode.put(value.asText(), key);
            }
        }
    }

}