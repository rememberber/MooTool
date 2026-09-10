package com.rememberber.mootool.nextfx.ui.theme;

import javafx.scene.Scene;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ThemeSnapshot {

    public enum Appearance {
        LIGHT,
        DARK
    }

    private final Appearance appearance;
    private final String accent;
    private final int fontSize;

    public ThemeSnapshot(Appearance appearance) {
        this(appearance, "blue", 13);
    }

    public ThemeSnapshot(Appearance appearance, String accent, int fontSize) {
        this.appearance = Objects.requireNonNull(appearance);
        this.accent = accent == null || accent.isBlank() ? "blue" : accent.toLowerCase(Locale.ROOT);
        this.fontSize = Math.max(12, Math.min(18, fontSize));
    }

    public Appearance appearance() {
        return appearance;
    }

    public String accent() {
        return accent;
    }

    public int fontSize() {
        return fontSize;
    }

    public boolean dark() {
        return appearance == Appearance.DARK;
    }

    public void apply(Scene scene) {
        List<String> sheets = List.of(
                resource("/styles/tokens.css"),
                resource("/styles/base.css"),
                resource(dark() ? "/styles/dark.css" : "/styles/light.css")
        );
        scene.getStylesheets().setAll(sheets);
        scene.getRoot().getStyleClass().removeIf(value -> value.startsWith("theme-") || value.startsWith("accent-"));
        scene.getRoot().getStyleClass().add(dark() ? "theme-dark" : "theme-light");
        scene.getRoot().getStyleClass().add("accent-" + accent);
        scene.getRoot().getStyleClass().add("mt-root");
        scene.getRoot().setStyle("-fx-font-size: " + fontSize + "px;");
    }

    private static String resource(String path) {
        return Objects.requireNonNull(ThemeSnapshot.class.getResource(path), path).toExternalForm();
    }
}
