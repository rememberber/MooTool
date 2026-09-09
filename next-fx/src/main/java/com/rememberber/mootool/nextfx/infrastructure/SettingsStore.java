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
import java.util.Set;

public final class SettingsStore {

    private static final Set<String> ACCENTS = Set.of("yellow", "coral", "blue", "green", "red", "purple");
    private static final Set<String> LANGUAGES = Set.of("zh_CN", "en_US", "ja_JP");

    public enum ThemePreference {
        LIGHT,
        DARK,
        SYSTEM
    }

    public record Settings(
            ThemePreference theme,
            String language,
            String accent,
            int fontSize,
            boolean wrapEditors,
            boolean showRecent,
            boolean compactNavigation,
            boolean showSeparators,
            boolean hideNavigationTitles
    ) {
        public static Settings defaults() {
            return new Settings(ThemePreference.SYSTEM, "zh_CN", "blue", 13, true, false, false, true, false);
        }

        public Settings withTheme(ThemePreference theme) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withLanguage(String language) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withAccent(String accent) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withFontSize(int fontSize) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withWrapEditors(boolean wrapEditors) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withShowRecent(boolean showRecent) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withCompactNavigation(boolean compactNavigation) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withShowSeparators(boolean showSeparators) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
        }

        public Settings withHideNavigationTitles(boolean hideNavigationTitles) {
            return new Settings(theme, language, accent, fontSize, wrapEditors, showRecent, compactNavigation, showSeparators, hideNavigationTitles);
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
            settings = new Settings(
                    parseTheme(node.path("theme").asText("system")),
                    parseLanguage(node.path("language").asText("zh_CN")),
                    parseAccent(node.path("accent").asText("blue")),
                    clampFontSize(node.path("fontSize").asInt(13)),
                    node.path("wrapEditors").asBoolean(true),
                    node.path("showRecent").asBoolean(false),
                    node.path("compactNavigation").asBoolean(false),
                    node.path("showSeparators").asBoolean(true),
                    node.path("hideNavigationTitles").asBoolean(false)
            );
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
            node.put("fontSize", next.fontSize());
            node.put("wrapEditors", next.wrapEditors());
            node.put("showRecent", next.showRecent());
            node.put("compactNavigation", next.compactNavigation());
            node.put("showSeparators", next.showSeparators());
            node.put("hideNavigationTitles", next.hideNavigationTitles());
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

    private static String parseLanguage(String value) {
        return LANGUAGES.contains(value) ? value : "zh_CN";
    }

    private static String parseAccent(String value) {
        return ACCENTS.contains(value) ? value : "blue";
    }

    private static int clampFontSize(int size) {
        return Math.max(12, Math.min(18, size));
    }
}
