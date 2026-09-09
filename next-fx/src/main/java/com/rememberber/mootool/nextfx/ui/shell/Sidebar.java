package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import com.rememberber.mootool.nextfx.app.ToolRegistry;
import com.rememberber.mootool.nextfx.domain.ToolDefinition;
import com.rememberber.mootool.nextfx.domain.ToolGroupId;
import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.domain.ToolStatus;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class Sidebar extends BorderPane {

    private final Translator translator;
    private final Consumer<ToolId> onSelect;
    private final Runnable onSettings;
    private final Runnable onGallery;
    private final TextField search = new TextField();
    private final VBox items = new VBox(4);
    private ToolId active = ToolId.MOOTOOL;

    public Sidebar(ProductIdentity identity, Translator translator, Consumer<ToolId> onSelect, Runnable onSettings, Runnable onGallery) {
        this.translator = translator;
        this.onSelect = onSelect;
        this.onSettings = onSettings;
        this.onGallery = onGallery;
        getStyleClass().add("mt-sidebar");
        setPrefWidth(248);
        setMinWidth(248);
        setMaxWidth(248);

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

        VBox footer = new VBox(6);
        footer.setPadding(new Insets(8, 12, 16, 12));
        Button settings = new Button(translator.t("app.nav.settings"));
        settings.setMaxWidth(Double.MAX_VALUE);
        settings.setOnAction(event -> onSettings.run());
        footer.getChildren().add(settings);
        if (identity.development()) {
            Button gallery = new Button(translator.t("app.dev.gallery"));
            gallery.setMaxWidth(Double.MAX_VALUE);
            gallery.setOnAction(event -> onGallery.run());
            Label version = new Label(identity.displayName() + " v" + identity.version());
            version.getStyleClass().add("mt-muted");
            footer.getChildren().addAll(gallery, version);
        }
        setBottom(footer);
        rebuild();
    }

    public void setActive(ToolId toolId) {
        this.active = toolId;
        rebuild();
    }

    public void focusSearch() {
        search.requestFocus();
    }

    private void rebuild() {
        items.getChildren().clear();
        String query = search.getText() == null ? "" : search.getText().trim();
        if (!query.isEmpty()) {
            Label heading = heading(translator.t("app.search.title"));
            items.getChildren().add(heading);
            for (ToolDefinition definition : ToolRegistry.search(query)) {
                items.getChildren().add(item(definition));
            }
            if (ToolRegistry.search(query).isEmpty()) {
                Label empty = new Label(translator.t("app.search.empty"));
                empty.getStyleClass().add("mt-muted");
                items.getChildren().add(empty);
            }
            return;
        }
        items.getChildren().add(item(ToolRegistry.require(ToolId.MOOTOOL)));
        for (ToolGroupId groupId : ToolRegistry.GROUP_ORDER) {
            items.getChildren().add(heading(translator.t("app.group." + groupId.id())));
            for (ToolDefinition definition : ToolRegistry.inGroup(groupId)) {
                items.getChildren().add(item(definition));
            }
        }
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
        button.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(10);
        Label glyph = new Label(definition.glyph());
        glyph.getStyleClass().add("mt-nav-glyph");
        Label title = new Label(translator.t(definition.titleKey()));
        HBox.setHgrow(title, Priority.ALWAYS);
        row.getChildren().addAll(glyph, title);
        if (definition.status() == ToolStatus.PLACEHOLDER) {
            Label badge = new Label(translator.t("app.status.wip"));
            badge.getStyleClass().add("mt-badge");
            row.getChildren().add(badge);
        }
        button.setGraphic(row);
        button.setTooltip(new Tooltip(translator.t(definition.titleKey())));
        if (definition.id() == active) {
            button.getStyleClass().add("selected");
        }
        button.setOnAction(event -> onSelect.accept(definition.id()));
        return button;
    }
}
