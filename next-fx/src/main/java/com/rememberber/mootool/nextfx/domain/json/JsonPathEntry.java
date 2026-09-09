package com.rememberber.mootool.nextfx.domain.json;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonPathEntry(String path, String label, JsonNode value, int depth) {
}
