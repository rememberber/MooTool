package com.rememberber.mootool.nextfx.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/**
 * Compile-time product identity. Profile only changes directory suffixes, never the product ID.
 */
public final class ProductIdentity {

    public static final String PRODUCT_ID = "next-fx";
    public static final String DISPLAY_NAME = "MooTool Next FX";
    public static final String BUNDLE_ID = "com.rememberber.mootool.next.fx";
    public static final String LINUX_PACKAGE = "mootool-next-fx";
    public static final String WINDOWS_PRODUCT_DIR = "MooToolNextFX";
    public static final UUID WINDOWS_UPGRADE_CODE = UUID.fromString("500dc26c-8050-4b1e-b7c6-691e1229bc00");

    public enum Profile {
        DEV,
        RELEASE
    }

    private final Profile profile;
    private final String version;

    public ProductIdentity(Profile profile, String version) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.version = Objects.requireNonNull(version, "version");
    }

    public static ProductIdentity load(Profile profile) {
        return new ProductIdentity(profile, readVersion());
    }

    public static ProductIdentity fromArgs(String[] args) {
        Profile profile = Profile.DEV;
        for (String arg : args) {
            if ("--profile=release".equals(arg) || "--release".equals(arg)) {
                profile = Profile.RELEASE;
            } else if ("--profile=dev".equals(arg)) {
                profile = Profile.DEV;
            }
        }
        String property = System.getProperty("mootool.profile", "").trim();
        if ("release".equalsIgnoreCase(property)) {
            profile = Profile.RELEASE;
        } else if ("dev".equalsIgnoreCase(property)) {
            profile = Profile.DEV;
        }
        return load(profile);
    }

    public Profile profile() {
        return profile;
    }

    public boolean development() {
        return profile == Profile.DEV;
    }

    public String version() {
        return version;
    }

    public String displayName() {
        return development() ? DISPLAY_NAME + " (Development)" : DISPLAY_NAME;
    }

    public String qualifiedBundleId() {
        return development() ? BUNDLE_ID + ".dev" : BUNDLE_ID;
    }

    public String linuxDirectoryName() {
        return development() ? LINUX_PACKAGE + "-dev" : LINUX_PACKAGE;
    }

    public String windowsDirectoryName() {
        return development() ? WINDOWS_PRODUCT_DIR + "Dev" : WINDOWS_PRODUCT_DIR;
    }

    public String credentialService() {
        return qualifiedBundleId();
    }

    private static String readVersion() {
        Properties properties = new Properties();
        try (InputStream stream = ProductIdentity.class.getResourceAsStream("/version.properties")) {
            if (stream != null) {
                properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // Fall back to the development version below.
        }
        String version = properties.getProperty("version", "0.1.0-SNAPSHOT").trim();
        return version.isEmpty() ? "0.1.0-SNAPSHOT" : version;
    }
}
