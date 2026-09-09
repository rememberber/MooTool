package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import com.rememberber.mootool.nextfx.app.ToolRegistry;
import com.rememberber.mootool.nextfx.domain.NavigationLayout;
import com.rememberber.mootool.nextfx.domain.ToolDefinition;
import com.rememberber.mootool.nextfx.domain.ToolGroupId;
import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.domain.ToolStatus;
import com.rememberber.mootool.nextfx.infrastructure.SettingsStore;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public final class Sidebar extends BorderPane {

    private final ProductIdentity identity;
    private final Translator translator;
    private final Consumer<ToolId> onSelect;
    private final Consumer<ToolId> onDetach;
    private final Runnable onSettings;
    private final Runnable onGallery;
    private final TextField search = new TextField();
    private final VBox items = new VBox(4);
    private ToolId active = ToolId.MOOTOOL;
    private SettingsStore.Settings layout = SettingsStore.Settings.defaults();
    private List<ToolId> recent = List.of();

    public Sidebar(
            ProductIdentity identity,
            Translator translator,
            Consumer<ToolId> onSelect,
            Consumer<ToolId> onDetach,
            Runnable onSettings,
            Runnable onGallery
    ) {
        this.identity = identity;
        this.translator = translator;
        this.onSelect = onSelect;
        this.onDetach = onDetach;
        this.onSettings = onSettings;
        this.onGallery = onGallery;
        getStyleClass().add("mt-sidebar");
        search.setPromptText(translator.t("app.search.placeholder"));
        search.getStyleClass().add("mt-search");
        search.textProperty().addListener((obs, old, value) -> rebuild());
        VBox header = new VBox(8, search);
        header.setPadding(new Insets(12, 12, 8, 12));
        setTop(header);

        items.setPadding(new Insets(4, 8, 16, 8));
        ScrollPane scroll = new ScrollPane(items);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("mt-sidebar-scroll");
        setCenter(scroll);

        applyLayout(layout, recent);
    }

    public void setActive(ToolId toolId) {
        this.active = toolId;
        rebuild();
    }

    public void applyLayout(SettingsStore.Settings settings, List<ToolId> recentTools) {
        this.layout = settings;
        this.recent = recentTools == null ? List.of() : List.copyOf(recentTools);
        boolean hideTitles = settings.hideNavigationTitles();
        double width = NavigationLayout.width(hideTitles);
        setPrefWidth(width);
        setMinWidth(width);
        setMaxWidth(width);
        getStyleClass().remove("compact");
        if (hideTitles || settings.compactNavigation()) {
            getStyleClass().add("compact");
        }
        rebuildFooter();
        rebuild();
    }

    public void focusSearch() {
        search.requestFocus();
    }

    private void rebuildFooter() {
        VBox footer = new VBox(6);
        footer.setPadding(new Insets(8, 12, 16, 12));
        Button settings = new Button(hideTitles() ? "⚙" : translator.t("app.nav.settings"));
        settings.setMaxWidth(Double.MAX_VALUE);
        settings.setTooltip(new Tooltip(translator.t("app.nav.settings")));
        settings.setOnAction(event -> onSettings.run());
        footer.getChildren().add(settings);
        if (identity.development()) {
            Button gallery = new Button(hideTitles() ? "▦" : translator.t("app.dev.gallery"));
            gallery.setMaxWidth(Double.MAX_VALUE);
            gallery.setTooltip(new Tooltip(translator.t("app.dev.gallery")));
            gallery.setOnAction(event -> onGallery.run());
            footer.getChildren().add(gallery);
            if (!hideTitles()) {
                Label version = new Label(identity.displayName() + " v" + identity.version());
                version.getStyleClass().add("mt-muted");
                version.setWrapText(true);
                footer.getChildren().add(version);
            }
        }
        setBottom(footer);
    }

    private void rebuild() {
        items.getChildren().clear();
        items.setSpacing(layout.compactNavigation() ? 2 : 4);
        String query = search.getText() == null ? "" : search.getText().trim();
        if (!query.isEmpty()) {
            if (!hideTitles()) {
                items.getChildren().add(heading(translator.t("app.search.title")));
            }
            List<ToolDefinition> matches = ToolRegistry.search(query);
            for (ToolDefinition definition : matches) {
                items.getChildren().add(item(definition));
            }
            if (matches.isEmpty()) {
                Label empty = new Label(translator.t("app.search.empty"));
                empty.getStyleClass().add("mt-muted");
                empty.setWrapText(true);
                items.getChildren().add(empty);
            }
            return;
        }
        items.getChildren().add(item(ToolRegistry.require(ToolId.MOOTOOL)));
        if (layout.showRecent() && !recent.isEmpty()) {
            addSeparator();
            if (!hideTitles()) {
                items.getChildren().add(heading(translator.t("app.nav.recent")));
            }
            for (ToolId id : recent) {
                items.getChildren().add(item(ToolRegistry.require(id)));
            }
        }
        for (ToolGroupId groupId : ToolRegistry.GROUP_ORDER) {
            addSeparator();
            if (!hideTitles()) {
                items.getChildren().add(heading(translator.t("app.group." + groupId.id())));
            }
            for (ToolDefinition definition : ToolRegistry.inGroup(groupId)) {
                items.getChildren().add(item(definition));
            }
        }
    }

    private void addSeparator() {
        if (!layout.showSeparators()) {
            return;
        }
        Separator separator = new Separator();
        separator.getStyleClass().add("mt-nav-separator");
        items.getChildren().add(separator);
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("mt-nav-group");
        label.setPadding(new Insets(10, 8, 2, 8));
        return label;
    }

    private Button item(ToolDefinition definition) {
        Button button = new Button();
        button.getStyleClass().add("mt-nav-item");
        if (layout.compactNavigation()) {
            button.getStyleClass().add("compact");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(NavigationLayout.rowHeight(layout.compactNavigation()));
        String title = translator.t(definition.titleKey());
        button.setTooltip(new Tooltip(title));
        button.setAccessibleText(title);
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label glyph = new Label(definition.glyph());
        glyph.getStyleClass().add("mt-nav-glyph");
        row.getChildren().add(glyph);
        if (!hideTitles()) {
            Label label = new Label(title);
            HBox.setHgrow(label, Priority.ALWAYS);
            row.getChildren().add(label);
            if (definition.status() == ToolStatus.PLACEHOLDER) {
                Label badge = new Label(translator.t("app.status.wip"));
                badge.getStyleClass().add("mt-badge");
                row.getChildren().add(badge);
            }
        }
        button.setGraphic(row);
        if (definition.id() == active) {
            button.getStyleClass().add("selected");
        }
        button.setOnAction(event -> onSelect.accept(definition.id()));
        if (definition.id().detachable()) {
            MenuItem detach = new MenuItem(translator.t("toolWindow.detach"));
            detach.setOnAction(event -> onDetach.accept(definition.id()));
            button.setContextMenu(new ContextMenu(detach));
        }
        return button;
    }

    private boolean hideTitles() {
        return layout.hideNavigationTitles();
    }
}
