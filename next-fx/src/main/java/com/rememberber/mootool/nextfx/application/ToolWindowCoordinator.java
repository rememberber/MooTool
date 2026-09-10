package com.rememberber.mootool.nextfx.application;

import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Moves the same tool Node between the docked workspace and a dedicated Stage.
 */
public final class ToolWindowCoordinator {

    private final Map<ToolId, Stage> stages = new EnumMap<>(ToolId.class);
    private final Translator translator;
    private final Consumer<ToolId> onDocked;
    private final Consumer<Scene> applyTheme;

    public ToolWindowCoordinator(Translator translator, Consumer<ToolId> onDocked, Consumer<Scene> applyTheme) {
        this.translator = translator;
        this.onDocked = onDocked;
        this.applyTheme = applyTheme;
    }

    public boolean detached(ToolId toolId) {
        return stages.containsKey(toolId);
    }

    public void detach(ToolSession session, Stage owner) {
        if (!session.toolId().detachable()) {
            return;
        }
        Stage existing = stages.get(session.toolId());
        if (existing != null) {
            existing.toFront();
            existing.requestFocus();
            return;
        }
        session.setWindowState(ToolSession.WindowState.DETACHING);
        Node view = session.view();
        if (view.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(view);
        } else if (view.getParent() instanceof BorderPane pane) {
            pane.setCenter(null);
        }
        Stage stage = new Stage();
        stage.setTitle("MooTool Next FX — " + translator.t("app.nav." + session.toolId().id()));
        BorderPane root = new BorderPane(view);
        root.getStyleClass().add("mt-root");
        Scene scene = new Scene(root, 1100, 760);
        applyTheme.accept(scene);
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(event -> {
            event.consume();
            dock(session);
        });
        clampToVisible(stage, owner);
        stages.put(session.toolId(), stage);
        session.setWindowState(ToolSession.WindowState.DETACHED);
        stage.show();
        stage.toFront();
    }

    public void dock(ToolSession session) {
        Stage stage = stages.remove(session.toolId());
        session.setWindowState(ToolSession.WindowState.DOCKING);
        Node view = session.view();
        if (view.getParent() instanceof BorderPane pane) {
            pane.setCenter(null);
        }
        if (stage != null) {
            stage.setOnCloseRequest(null);
            stage.close();
        }
        session.setWindowState(ToolSession.WindowState.DOCKED);
        onDocked.accept(session.toolId());
    }

    public void focus(ToolId toolId) {
        Stage stage = stages.get(toolId);
        if (stage != null) {
            stage.toFront();
            stage.requestFocus();
        }
    }

    public void closeAll() {
        for (Stage stage : new ArrayList<>(stages.values())) {
            stage.close();
        }
        stages.clear();
    }

    private static void clampToVisible(Stage stage, Stage owner) {
        Rectangle2D visual = Screen.getPrimary().getVisualBounds();
        if (owner != null) {
            stage.setX(Math.min(owner.getX() + 48, visual.getMaxX() - 760));
            stage.setY(Math.min(owner.getY() + 48, visual.getMaxY() - 560));
        }
        if (stage.getX() < visual.getMinX()) {
            stage.setX(visual.getMinX());
        }
        if (stage.getY() < visual.getMinY()) {
            stage.setY(visual.getMinY());
        }
    }
}
