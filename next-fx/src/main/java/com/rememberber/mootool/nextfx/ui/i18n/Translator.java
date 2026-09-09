package com.rememberber.mootool.nextfx.ui.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class Translator {

    private static final Map<String, Properties> BUNDLES = new ConcurrentHashMap<>();

    private volatile String language;

    public Translator(String language) {
        this.language = normalize(language);
        load(this.language);
    }

    public void setLanguage(String language) {
        this.language = normalize(language);
        load(this.language);
    }

    public String language() {
        return language;
    }

    public Locale locale() {
        String[] parts = language.split("_", 2);
        return parts.length == 2 ? Locale.of(parts[0], parts[1]) : Locale.of(language);
    }

    public String t(String key, Map<String, String> params) {
        Properties bundle = BUNDLES.getOrDefault(language, BUNDLES.get("zh_CN"));
        String message = bundle.getProperty(key, key);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public String t(String key) {
        return t(key, Map.of());
    }

    public String t(String key, String name, String value) {
        return t(key, Map.of(name, value));
    }

    private static String normalize(String language) {
        if (language == null) {
            return "zh_CN";
        }
        return switch (language) {
            case "en", "en_US" -> "en_US";
            case "ja", "ja_JP" -> "ja_JP";
            default -> "zh_CN";
        };
    }

    private static void load(String language) {
        BUNDLES.computeIfAbsent(language, key -> {
            Properties properties = new Properties();
            String resource = "/i18n/messages_" + key + ".properties";
            try (InputStream stream = Translator.class.getResourceAsStream(resource)) {
                if (stream != null) {
                    properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {
                // Missing bundle falls back to keys.
            }
            return properties;
        });
    }
}
