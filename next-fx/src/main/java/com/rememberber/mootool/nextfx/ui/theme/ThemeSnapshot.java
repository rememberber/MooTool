package com.rememberber.mootool.nextfx.ui.theme;

import javafx.scene.Scene;

import java.util.List;
import java.util.Objects;

public final class ThemeSnapshot {

    public enum Appearance {
        LIGHT,
        DARK
    }

    private final Appearance appearance;

    public ThemeSnapshot(Appearance appearance) {
        this.appearance = Objects.requireNonNull(appearance);
    }

    public Appearance appearance() {
        return appearance;
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
        scene.getRoot().getStyleClass().removeAll("theme-light", "theme-dark");
        scene.getRoot().getStyleClass().add(dark() ? "theme-dark" : "theme-light");
        scene.getRoot().getStyleClass().add("mt-root");
    }

    private static String resource(String path) {
        return Objects.requireNonNull(ThemeSnapshot.class.getResource(path), path).toExternalForm();
    }
}
