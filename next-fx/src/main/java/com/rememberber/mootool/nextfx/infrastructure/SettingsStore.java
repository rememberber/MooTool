package com.rememberber.mootool.nextfx.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

public final class SettingsStore {

    public enum ThemePreference {
        LIGHT,
        DARK,
        SYSTEM
    }

    public record Settings(ThemePreference theme, String language, String accent, boolean wrapEditors) {
        public static Settings defaults() {
            return new Settings(ThemePreference.SYSTEM, "zh_CN", "blue", true);
        }
    }

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private Settings settings = Settings.defaults();

    public SettingsStore(AppPaths paths) {
        this.file = Objects.requireNonNull(paths).settingsFile();
    }

    public Settings load() {
        try {
            if (!Files.exists(file)) {
                settings = Settings.defaults();
                save(settings);
                return settings;
            }
            JsonNode node = mapper.readTree(Files.readString(file));
            ThemePreference theme = parseTheme(node.path("theme").asText("system"));
            String language = node.path("language").asText("zh_CN");
            String accent = node.path("accent").asText("blue");
            boolean wrap = node.path("wrapEditors").asBoolean(true);
            settings = new Settings(theme, language, accent, wrap);
            return settings;
        } catch (IOException exception) {
            settings = Settings.defaults();
            return settings;
        }
    }

    public Settings current() {
        return settings;
    }

    public void save(Settings next) {
        this.settings = Objects.requireNonNull(next);
        try {
            Files.createDirectories(file.getParent());
            ObjectNode node = mapper.createObjectNode();
            node.put("productId", "next-fx");
            node.put("theme", next.theme().name().toLowerCase(Locale.ROOT));
            node.put("language", next.language());
            node.put("accent", next.accent());
            node.put("wrapEditors", next.wrapEditors());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save settings", exception);
        }
    }

    private static ThemePreference parseTheme(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "light" -> ThemePreference.LIGHT;
            case "dark" -> ThemePreference.DARK;
            default -> ThemePreference.SYSTEM;
        };
    }
}
