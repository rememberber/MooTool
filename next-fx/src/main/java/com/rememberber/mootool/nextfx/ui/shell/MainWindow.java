package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.app.AppServices;
import com.rememberber.mootool.nextfx.app.ToolRegistry;
import com.rememberber.mootool.nextfx.application.ToolSession;
import com.rememberber.mootool.nextfx.application.ToolWindowCoordinator;
import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.features.json.JsonView;
import com.rememberber.mootool.nextfx.infrastructure.SettingsStore;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class MainWindow {

    public static final double DEFAULT_WIDTH = 1440;
    public static final double DEFAULT_HEIGHT = 920;
    public static final double MIN_WIDTH = 1080;
    public static final double MIN_HEIGHT = 720;

    private final AppServices services;
    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final StackPane workspace = new StackPane();
    private final Sidebar sidebar;
    private ToolId active = ToolId.MOOTOOL;
    private ToolId previousTool = ToolId.MOOTOOL;
    private boolean settingsOpen;
    private boolean galleryOpen;

    public MainWindow(AppServices services, Stage stage) {
        this.services = services;
        this.stage = stage;
        this.sidebar = new Sidebar(
                services.identity(),
                services.translator(),
                this::openTool,
                this::openSettings,
                this::openGallery
        );
        root.getStyleClass().add("mt-root");
        workspace.getStyleClass().add("mt-workspace");
        root.setLeft(sidebar);
        root.setCenter(workspace);
        services.setWindows(new ToolWindowCoordinator(services.translator(), this::redock, this::applyTheme));
        openTool(ToolId.MOOTOOL);
        restoreWindow();
    }

    public Scene attach() {
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        applyTheme(scene);
        stage.setTitle(services.identity().displayName());
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), sidebar::focusSearch);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), this::openSettings);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), this::focusFind);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> {});
        scene.addEventFilter(JsonView.ToolActionEvent.DETACH, event -> {
            detachActive();
            event.consume();
        });
        stage.setOnCloseRequest(event -> persistWindow());
        return scene;
    }

    public void openTool(ToolId toolId) {
        settingsOpen = false;
        galleryOpen = false;
        if (toolId.detachable() && services.windows().detached(toolId)) {
            services.windows().focus(toolId);
            workspace.getChildren().setAll(detachedPlaceholder(toolId));
            active = toolId;
            sidebar.setActive(toolId);
            return;
        }
        previousTool = active;
        active = toolId;
        sidebar.setActive(toolId);
        ToolSession session = services.session(toolId);
        workspace.getChildren().setAll(session.view());
        persistActiveTool();
    }

    public void detachActive() {
        if (!active.detachable()) {
            return;
        }
        ToolSession session = services.session(active);
        workspace.getChildren().clear();
        services.windows().detach(session, stage);
        workspace.getChildren().setAll(detachedPlaceholder(active));
    }

    private void redock(ToolId toolId) {
        Platform.runLater(() -> openTool(toolId));
    }

    private void openSettings() {
        settingsOpen = true;
        galleryOpen = false;
        workspace.getChildren().setAll(new SettingsView(services.settings(), services.translator(), this::changeSettings));
    }

    private void openGallery() {
        galleryOpen = true;
        settingsOpen = false;
        workspace.getChildren().setAll(services.gallery());
    }

    private void changeSettings(SettingsStore.Settings next) {
        services.applySettings(next);
        applyTheme(stage.getScene());
        if (settingsOpen) {
            openSettings();
        }
    }

    private void applyTheme(Scene scene) {
        if (scene != null) {
            services.theme().apply(scene);
        }
    }

    private Node detachedPlaceholder(ToolId toolId) {
        Label label = new Label(services.translator().t("toolWindow.detachedTitle", "tool", services.translator().t(ToolRegistry.require(toolId).titleKey())));
        label.getStyleClass().add("mt-placeholder-title");
        StackPane pane = new StackPane(label);
        pane.getStyleClass().add("mt-placeholder");
        return pane;
    }

    private void focusFind() {
        Node current = workspace.getChildren().isEmpty() ? null : workspace.getChildren().getFirst();
        if (current != null) {
            current.requestFocus();
        }
    }

    private void restoreWindow() {
        String payload = services.database().windowState().load("main");
        if (payload == null || payload.isBlank()) {
            stage.setWidth(DEFAULT_WIDTH);
            stage.setHeight(DEFAULT_HEIGHT);
            return;
        }
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            stage.setWidth(node.path("width").asDouble(DEFAULT_WIDTH));
            stage.setHeight(node.path("height").asDouble(DEFAULT_HEIGHT));
            if (node.hasNonNull("x")) {
                stage.setX(node.get("x").asDouble());
            }
            if (node.hasNonNull("y")) {
                stage.setY(node.get("y").asDouble());
            }
            String tool = node.path("activeToolId").asText(ToolId.MOOTOOL.id());
            if (ToolId.isToolId(tool)) {
                openTool(ToolId.fromId(tool));
            }
        } catch (Exception ignored) {
            stage.setWidth(DEFAULT_WIDTH);
            stage.setHeight(DEFAULT_HEIGHT);
        }
    }

    private void persistWindow() {
        persistActiveTool();
    }

    private void persistActiveTool() {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.createObjectNode();
            node.put("x", stage.getX());
            node.put("y", stage.getY());
            node.put("width", stage.getWidth() <= 0 ? DEFAULT_WIDTH : stage.getWidth());
            node.put("height", stage.getHeight() <= 0 ? DEFAULT_HEIGHT : stage.getHeight());
            node.put("activeToolId", active.id());
            services.database().windowState().save("main", mapper.writeValueAsString(node));
        } catch (Exception ignored) {
            // Persistence is best-effort on close.
        }
    }
}
