package com.rememberber.mootool.nextfx.application;

import com.rememberber.mootool.nextfx.domain.ToolId;

import java.util.ArrayList;
import java.util.List;

public final class RecentTools {

    public static final int LIMIT = 5;

    private RecentTools() {
    }

    public static List<ToolId> push(List<ToolId> current, ToolId next) {
        if (next == null || next == ToolId.MOOTOOL) {
            return List.copyOf(current == null ? List.of() : current);
        }
        List<ToolId> updated = new ArrayList<>();
        updated.add(next);
        if (current != null) {
            for (ToolId id : current) {
                if (id != next && updated.size() < LIMIT) {
                    updated.add(id);
                }
            }
        }
        return List.copyOf(updated);
    }
}
