package com.rememberber.mootool.nextfx.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsCategoryTest {

    @Test
    void keepsElevenCategoriesInElectronOrder() {
        assertThat(SettingsCategory.values()).containsExactly(
                SettingsCategory.GENERAL,
                SettingsCategory.APPEARANCE,
                SettingsCategory.LAYOUT,
                SettingsCategory.EDITOR,
                SettingsCategory.NETWORK,
                SettingsCategory.DATA,
                SettingsCategory.VAULT,
                SettingsCategory.RUNTIME,
                SettingsCategory.TOOLS,
                SettingsCategory.SHORTCUTS,
                SettingsCategory.ABOUT
        );
        assertThat(Arrays.stream(SettingsCategory.values()).filter(SettingsCategory::hasWorkingControls).count()).isEqualTo(6);
    }
}
