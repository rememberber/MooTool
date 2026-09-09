package com.rememberber.mootool.nextfx.infrastructure;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsStoreTest {

    @TempDir
    Path temp;

    @Test
    void roundTripsLayoutAndAppearanceWithoutTouchingUserHome() {
        ProductIdentity identity = ProductIdentity.load(ProductIdentity.Profile.DEV);
        AppPaths paths = AppPaths.isolated(identity, temp);
        SettingsStore store = new SettingsStore(paths);
        store.save(SettingsStore.Settings.defaults()
                .withTheme(SettingsStore.ThemePreference.DARK)
                .withLanguage("en_US")
                .withAccent("coral")
                .withFontSize(15)
                .withShowRecent(true)
                .withHideNavigationTitles(true));
        SettingsStore reloaded = new SettingsStore(paths);
        SettingsStore.Settings settings = reloaded.load();
        assertThat(settings.theme()).isEqualTo(SettingsStore.ThemePreference.DARK);
        assertThat(settings.language()).isEqualTo("en_US");
        assertThat(settings.accent()).isEqualTo("coral");
        assertThat(settings.fontSize()).isEqualTo(15);
        assertThat(settings.showRecent()).isTrue();
        assertThat(settings.hideNavigationTitles()).isTrue();
        assertThat(Files.exists(paths.settingsFile())).isTrue();
        assertThat(paths.settingsFile().toString()).startsWith(temp.toAbsolutePath().toString());
    }
}
