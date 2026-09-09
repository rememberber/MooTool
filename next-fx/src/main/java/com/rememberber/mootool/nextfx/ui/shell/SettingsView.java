package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import com.rememberber.mootool.nextfx.domain.SettingsCategory;
import com.rememberber.mootool.nextfx.infrastructure.AppPaths;
import com.rememberber.mootool.nextfx.infrastructure.SettingsStore;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class SettingsView extends BorderPane {

    private final SettingsStore store;
    private final Translator translator;
    private final ProductIdentity identity;
    private final AppPaths paths;
    private final Consumer<SettingsStore.Settings> onChange;
    private final ListView<SettingsCategory> categories = new ListView<>();
    private final VBox content = new VBox(16);

    public SettingsView(
            SettingsStore store,
            Translator translator,
            ProductIdentity identity,
            AppPaths paths,
            Consumer<SettingsStore.Settings> onChange,
            Runnable onBack
    ) {
        this.store = store;
        this.translator = translator;
        this.identity = identity;
        this.paths = paths;
        this.onChange = onChange;
        getStyleClass().add("mt-settings");

        Button back = new Button(translator.t("settings.back"));
        back.setOnAction(event -> onBack.run());
        Label title = new Label(translator.t("app.nav.settings"));
        title.getStyleClass().add("mt-home-title");
        HBox header = new HBox(12, back, title);
        header.setPadding(new Insets(16, 24, 8, 24));
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setTop(header);

        categories.getItems().addAll(SettingsCategory.values());
        categories.setPrefWidth(220);
        categories.setMinWidth(180);
        categories.setMaxWidth(240);
        categories.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(SettingsCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : translator.t(item.titleKey()));
            }
        });
        categories.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) {
                show(value);
            }
        });
        categories.getStyleClass().add("mt-settings-nav");
        setLeft(categories);

        content.setPadding(new Insets(8, 32, 32, 24));
        content.setMaxWidth(760);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("mt-settings-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        setCenter(scroll);
        categories.getSelectionModel().select(SettingsCategory.GENERAL);
    }

    private void show(SettingsCategory category) {
        content.getChildren().clear();
        Label heading = new Label(translator.t(category.titleKey()));
        heading.getStyleClass().add("mt-section-title");
        content.getChildren().add(heading);
        content.getChildren().add(body(category));
    }

    private Node body(SettingsCategory category) {
        SettingsStore.Settings current = store.current();
        return switch (category) {
            case GENERAL -> general(current);
            case APPEARANCE -> appearance(current);
            case LAYOUT -> layout(current);
            case EDITOR -> editor(current);
            case DATA -> data();
            case ABOUT -> about();
            default -> pending(category);
        };
    }

    private Node general(SettingsStore.Settings current) {
        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll("zh_CN", "en_US", "ja_JP");
        language.getSelectionModel().select(current.language());
        language.setOnAction(event -> onChange.accept(current.withLanguage(language.getValue())));
        GridPane form = form();
        form.add(new Label(translator.t("settings.language")), 0, 0);
        form.add(language, 1, 0);
        Label note = muted(translator.t("settings.pending.general"));
        return new VBox(12, form, note);
    }

    private Node appearance(SettingsStore.Settings current) {
        ComboBox<String> theme = new ComboBox<>();
        theme.getItems().addAll("light", "dark", "system");
        theme.getSelectionModel().select(current.theme().name().toLowerCase());
        theme.setOnAction(event -> onChange.accept(current.withTheme(parseTheme(theme.getValue()))));
        ComboBox<String> accent = new ComboBox<>();
        accent.getItems().addAll("yellow", "coral", "blue", "green", "red", "purple");
        accent.getSelectionModel().select(current.accent());
        accent.setOnAction(event -> onChange.accept(current.withAccent(accent.getValue())));
        Spinner<Integer> font = new Spinner<>(12, 18, current.fontSize());
        font.setEditable(true);
        font.valueProperty().addListener((obs, old, value) -> {
            if (value != null && value != store.current().fontSize()) {
                onChange.accept(store.current().withFontSize(value));
            }
        });
        GridPane form = form();
        form.add(new Label(translator.t("settings.theme")), 0, 0);
        form.add(theme, 1, 0);
        form.add(new Label(translator.t("settings.accent")), 0, 1);
        form.add(accent, 1, 1);
        form.add(new Label(translator.t("settings.fontSize")), 0, 2);
        form.add(font, 1, 2);
        Label style = muted(translator.t("settings.pending.style"));
        return new VBox(12, form, style);
    }

    private Node layout(SettingsStore.Settings current) {
        CheckBox recent = toggle(translator.t("settings.layout.showRecent"), current.showRecent(),
                value -> onChange.accept(store.current().withShowRecent(value)));
        CheckBox compact = toggle(translator.t("settings.layout.compact"), current.compactNavigation(),
                value -> onChange.accept(store.current().withCompactNavigation(value)));
        CheckBox separators = toggle(translator.t("settings.layout.separators"), current.showSeparators(),
                value -> onChange.accept(store.current().withShowSeparators(value)));
        CheckBox hideTitles = toggle(translator.t("settings.layout.hideTitles"), current.hideNavigationTitles(),
                value -> onChange.accept(store.current().withHideNavigationTitles(value)));
        Label note = muted(translator.t("settings.layout.note"));
        return new VBox(12, recent, compact, separators, hideTitles, note);
    }

    private Node editor(SettingsStore.Settings current) {
        CheckBox wrap = toggle(translator.t("settings.editor.wrap"), current.wrapEditors(),
                value -> onChange.accept(store.current().withWrapEditors(value)));
        Label note = muted(translator.t("settings.pending.editor"));
        return new VBox(12, wrap, note);
    }

    private Node data() {
        Label path = new Label(paths.dataRoot().toString());
        path.setWrapText(true);
        return new VBox(12, muted(translator.t("settings.data.directory")), path, muted(translator.t("settings.pending.data")));
    }

    private Node about() {
        Label name = new Label(identity.displayName());
        name.getStyleClass().add("mt-section-title");
        Label version = new Label("v" + identity.version());
        Label product = muted("productId=" + ProductIdentity.PRODUCT_ID);
        Label bundle = muted(identity.qualifiedBundleId());
        return new VBox(8, name, version, product, bundle);
    }

    private Node pending(SettingsCategory category) {
        Label badge = new Label(translator.t("app.status.wip"));
        badge.getStyleClass().add("mt-badge");
        Label reason = muted(translator.t("settings.pending." + category.id()));
        reason.setWrapText(true);
        return new VBox(12, badge, reason);
    }

    private CheckBox toggle(String label, boolean selected, Consumer<Boolean> onToggle) {
        CheckBox box = new CheckBox(label);
        box.setSelected(selected);
        box.setOnAction(event -> onToggle.accept(box.isSelected()));
        return box;
    }

    private static GridPane form() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        return form;
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("mt-muted");
        label.setWrapText(true);
        return label;
    }

    private static SettingsStore.ThemePreference parseTheme(String value) {
        return switch (value) {
            case "light" -> SettingsStore.ThemePreference.LIGHT;
            case "dark" -> SettingsStore.ThemePreference.DARK;
            default -> SettingsStore.ThemePreference.SYSTEM;
        };
    }
}
