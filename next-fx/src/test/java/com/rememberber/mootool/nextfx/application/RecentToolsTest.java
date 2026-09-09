package com.rememberber.mootool.nextfx.application;

import com.rememberber.mootool.nextfx.domain.ToolId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentToolsTest {

    @Test
    void keepsFiveMostRecentAndSkipsHome() {
        List<ToolId> recent = List.of();
        recent = RecentTools.push(recent, ToolId.MOOTOOL);
        assertThat(recent).isEmpty();
        recent = RecentTools.push(recent, ToolId.JSON);
        recent = RecentTools.push(recent, ToolId.HTTP);
        recent = RecentTools.push(recent, ToolId.JSON);
        recent = RecentTools.push(recent, ToolId.REGEX);
        recent = RecentTools.push(recent, ToolId.CRON);
        recent = RecentTools.push(recent, ToolId.PDF);
        recent = RecentTools.push(recent, ToolId.IMAGE);
        assertThat(recent).containsExactly(ToolId.IMAGE, ToolId.PDF, ToolId.CRON, ToolId.REGEX, ToolId.JSON);
        assertThat(recent).doesNotContain(ToolId.HTTP);
    }
}
