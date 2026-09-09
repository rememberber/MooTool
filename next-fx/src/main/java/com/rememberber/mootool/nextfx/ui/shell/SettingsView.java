package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.infrastructure.SettingsStore;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class SettingsView extends VBox {

    public SettingsView(SettingsStore store, Translator translator, Consumer<SettingsStore.Settings> onChange) {
        getStyleClass().add("mt-settings");
        setSpacing(16);
        setPadding(new Insets(24, 32, 32, 32));
        Label title = new Label(translator.t("app.nav.settings"));
        title.getStyleClass().add("mt-home-title");
        Label note = new Label(translator.t("settings.p0.note"));
        note.setWrapText(true);
        note.getStyleClass().add("mt-muted");

        SettingsStore.Settings current = store.current();
        ComboBox<String> theme = new ComboBox<>();
        theme.getItems().addAll("light", "dark", "system");
        theme.getSelectionModel().select(current.theme().name().toLowerCase());
        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll("zh_CN", "en_US", "ja_JP");
        language.getSelectionModel().select(current.language());

        theme.setOnAction(event -> onChange.accept(new SettingsStore.Settings(
                parseTheme(theme.getValue()), language.getValue(), current.accent(), current.wrapEditors())));
        language.setOnAction(event -> onChange.accept(new SettingsStore.Settings(
                parseTheme(theme.getValue()), language.getValue(), current.accent(), current.wrapEditors())));

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label(translator.t("settings.theme")), 0, 0);
        form.add(theme, 1, 0);
        form.add(new Label(translator.t("settings.language")), 0, 1);
        form.add(language, 1, 1);
        getChildren().addAll(title, note, form);
    }

    private static SettingsStore.ThemePreference parseTheme(String value) {
        return switch (value) {
            case "light" -> SettingsStore.ThemePreference.LIGHT;
            case "dark" -> SettingsStore.ThemePreference.DARK;
            default -> SettingsStore.ThemePreference.SYSTEM;
        };
    }
}
