package com.rememberber.mootool.nextfx.infrastructure;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppPathsTest {

    @TempDir
    Path temp;

    @Test
    void developmentProfileUsesDistinctBundleSuffixOnMacLayout() {
        ProductIdentity identity = new ProductIdentity(ProductIdentity.Profile.DEV, "0.1.0-SNAPSHOT");
        AppPaths paths = AppPaths.standard(identity, temp);
        assertThat(paths.configRoot().toString()).contains("com.rememberber.mootool.next.fx.dev");
        assertThat(paths.databaseFile().getFileName().toString()).isEqualTo("MooToolNextFX.db");
        assertThat(paths.lockFile().toString()).contains("/state/");
    }

    @Test
    void releaseProfileDoesNotUseDevSuffix() {
        ProductIdentity identity = new ProductIdentity(ProductIdentity.Profile.RELEASE, "0.1.0");
        AppPaths paths = AppPaths.standard(identity, temp);
        assertThat(paths.configRoot().toString()).contains("com.rememberber.mootool.next.fx");
        assertThat(paths.configRoot().toString()).doesNotContain(".dev");
    }

    @Test
    void isolatedRootsDoNotReadUserHome() {
        ProductIdentity identity = ProductIdentity.load(ProductIdentity.Profile.DEV);
        AppPaths paths = AppPaths.isolated(identity, temp.resolve("fx-test"));
        assertThat(paths.dataRoot().toString()).startsWith(temp.toAbsolutePath().toString());
        assertThat(paths.dataRoot().toString()).doesNotContain(".MooTool");
        assertThat(paths.jsonVaultRoot()).isEqualTo(paths.dataRoot().resolve("vaults").resolve("json"));
    }
}
