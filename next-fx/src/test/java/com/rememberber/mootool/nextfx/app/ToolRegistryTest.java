package com.rememberber.mootool.nextfx.app;

import com.rememberber.mootool.nextfx.domain.ToolGroupId;
import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.domain.ToolStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {

    @Test
    void keepsTwentySixStableIdsInSourceOrder() {
        assertThat(ToolId.values()).hasSize(26);
        assertThat(ToolRegistry.all()).hasSize(26);
        assertThat(ToolRegistry.all().getFirst().id()).isEqualTo(ToolId.MOOTOOL);
        assertThat(ToolRegistry.all().getLast().id()).isEqualTo(ToolId.HARDWARE);
        assertThat(ToolRegistry.GROUP_ORDER).containsExactly(
                ToolGroupId.TEXT, ToolGroupId.DEV, ToolGroupId.NETWORK,
                ToolGroupId.ENCODE, ToolGroupId.DAILY, ToolGroupId.SYSTEM
        );
    }

    @Test
    void historyAndFavoriteFlagsMatchElectronRegistry() {
        assertThat(ToolRegistry.require(ToolId.MOOTOOL).supportsHistory()).isFalse();
        assertThat(ToolRegistry.require(ToolId.HARDWARE).supportsHistory()).isFalse();
        assertThat(ToolRegistry.require(ToolId.MESSAGE_BOARD).supportsHistory()).isFalse();
        assertThat(ToolRegistry.require(ToolId.JSON).supportsHistory()).isTrue();
        assertThat(ToolRegistry.require(ToolId.REGEX).supportsFavorites()).isTrue();
        assertThat(ToolRegistry.require(ToolId.CRON).supportsFavorites()).isTrue();
        assertThat(ToolRegistry.require(ToolId.COLOR_BOARD).supportsFavorites()).isTrue();
        assertThat(ToolRegistry.require(ToolId.JSON).supportsFavorites()).isFalse();
    }

    @Test
    void homeJsonAndEncodeAreInProgress() {
        assertThat(ToolRegistry.require(ToolId.MOOTOOL).status()).isEqualTo(ToolStatus.IN_PROGRESS);
        assertThat(ToolRegistry.require(ToolId.JSON).status()).isEqualTo(ToolStatus.IN_PROGRESS);
        assertThat(ToolRegistry.require(ToolId.ENCODE).status()).isEqualTo(ToolStatus.IN_PROGRESS);
        assertThat(ToolRegistry.all().stream().filter(item -> item.status() == ToolStatus.PLACEHOLDER)).hasSize(23);
    }

    @Test
    void searchMatchesLocalizedKeywords() {
        assertThat(ToolRegistry.search("json").getFirst().id()).isEqualTo(ToolId.JSON);
        assertThat(ToolRegistry.search("随手记").getFirst().id()).isEqualTo(ToolId.QUICK_NOTE);
        assertThat(ToolRegistry.search("xyz-unknown")).isEmpty();
    }
}
