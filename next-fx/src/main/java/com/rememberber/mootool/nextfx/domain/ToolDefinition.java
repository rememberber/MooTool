package com.rememberber.mootool.nextfx.domain;

import java.util.List;
import java.util.Objects;

public record ToolDefinition(
        ToolId id,
        ToolGroupId groupId,
        String titleKey,
        List<String> keywords,
        String glyph,
        ToolStatus status,
        boolean supportsHistory,
        boolean supportsFavorites
) {
    public ToolDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(titleKey);
        keywords = List.copyOf(keywords);
        Objects.requireNonNull(glyph);
        Objects.requireNonNull(status);
    }
}
