package com.rememberber.mootool.nextfx.infrastructure;

import com.rememberber.mootool.nextfx.app.ProductIdentity;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Resolves isolated config/data/cache/state directories for this product only.
 * Tests inject roots; production never rewrites {@code user.home}.
 */
public final class AppPaths {

    private final ProductIdentity identity;
    private final Path configRoot;
    private final Path dataRoot;
    private final Path cacheRoot;
    private final Path stateRoot;
    private final boolean portable;

    private AppPaths(
            ProductIdentity identity,
            Path configRoot,
            Path dataRoot,
            Path cacheRoot,
            Path stateRoot,
            boolean portable
    ) {
        this.identity = Objects.requireNonNull(identity);
        this.configRoot = configRoot.toAbsolutePath().normalize();
        this.dataRoot = dataRoot.toAbsolutePath().normalize();
        this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
        this.stateRoot = stateRoot.toAbsolutePath().normalize();
        this.portable = portable;
    }

    public static AppPaths standard(ProductIdentity identity) {
        return standard(identity, Path.of(System.getProperty("user.home")));
    }

    public static AppPaths standard(ProductIdentity identity, Path home) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            Path support = home.resolve("Library/Application Support").resolve(identity.qualifiedBundleId());
            return new AppPaths(
                    identity,
                    support.resolve("config"),
                    support.resolve("data"),
                    home.resolve("Library/Caches").resolve(identity.qualifiedBundleId()),
                    support.resolve("state"),
                    false
            );
        }
        if (os.contains("win")) {
            Path roaming = envPath("APPDATA", home.resolve("AppData/Roaming")).resolve(identity.windowsDirectoryName());
            Path local = envPath("LOCALAPPDATA", home.resolve("AppData/Local")).resolve(identity.windowsDirectoryName());
            return new AppPaths(
                    identity,
                    roaming.resolve("config"),
                    local.resolve("data"),
                    local.resolve("cache"),
                    local.resolve("state"),
                    false
            );
        }
        Path configHome = envPath("XDG_CONFIG_HOME", home.resolve(".config")).resolve(identity.linuxDirectoryName());
        Path dataHome = envPath("XDG_DATA_HOME", home.resolve(".local/share")).resolve(identity.linuxDirectoryName());
        Path cacheHome = envPath("XDG_CACHE_HOME", home.resolve(".cache")).resolve(identity.linuxDirectoryName());
        Path stateHome = envPath("XDG_STATE_HOME", home.resolve(".local/state")).resolve(identity.linuxDirectoryName());
        return new AppPaths(identity, configHome, dataHome, cacheHome, stateHome, false);
    }

    public static AppPaths portable(ProductIdentity identity, Path portableRoot) {
        Path root = portableRoot.toAbsolutePath().normalize().resolve(identity.development() ? "data-dev" : "data");
        return new AppPaths(
                identity,
                root.resolve("config"),
                root.resolve("data"),
                root.resolve("cache"),
                root.resolve("state"),
                true
        );
    }

    public static AppPaths isolated(ProductIdentity identity, Path root) {
        Path base = root.toAbsolutePath().normalize();
        return new AppPaths(
                identity,
                base.resolve("config"),
                base.resolve("data"),
                base.resolve("cache"),
                base.resolve("state"),
                false
        );
    }

    public static AppPaths fromArgs(ProductIdentity identity, String[] args) {
        Path override = null;
        boolean portable = false;
        Path portableRoot = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--portable".equals(arg)) {
                portable = true;
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    portableRoot = Path.of(args[++i]);
                }
            } else if (arg.startsWith("--data-root=")) {
                override = Path.of(arg.substring("--data-root=".length()));
            } else if ("--data-root".equals(arg) && i + 1 < args.length) {
                override = Path.of(args[++i]);
            }
        }
        if (portable) {
            return portable(identity, portableRoot == null ? Path.of(".").toAbsolutePath() : portableRoot);
        }
        if (override != null) {
            return isolated(identity, override);
        }
        return standard(identity);
    }

    public ProductIdentity identity() {
        return identity;
    }

    public Path configRoot() {
        return configRoot;
    }

    public Path dataRoot() {
        return dataRoot;
    }

    public Path cacheRoot() {
        return cacheRoot;
    }

    public Path stateRoot() {
        return stateRoot;
    }

    public Path databaseFile() {
        return dataRoot.resolve("MooToolNextFX.db");
    }

    public Path productMarker() {
        return dataRoot.resolve("product.json");
    }

    public Path settingsFile() {
        return configRoot.resolve("settings.json");
    }

    public Path bootstrapFile() {
        return configRoot.resolve("bootstrap.json");
    }

    public Path lockFile() {
        return stateRoot.resolve("instance.lock");
    }

    public boolean portable() {
        return portable;
    }

    private static Path envPath(String name, Path fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Path.of(value);
    }
}
