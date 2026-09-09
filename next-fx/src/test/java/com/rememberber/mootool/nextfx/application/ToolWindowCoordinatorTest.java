package com.rememberber.mootool.nextfx.application;

import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.support.FxSupport;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ui")
class ToolWindowCoordinatorTest {

    @Test
    void transfersTheSameNodeFiftyTimesAndDoesNotDetachHome() {
        FxSupport.run(() -> {
            Translator translator = new Translator("zh_CN");
            Label view = new Label("json-session");
            ToolSession json = new ToolSession(ToolId.JSON, () -> view);
            StackPane dock = new StackPane(json.view());
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane(), 400, 300));
            owner.show();
            ToolWindowCoordinator coordinator = new ToolWindowCoordinator(translator, id -> dock.getChildren().setAll(json.view()), scene -> {});
            for (int i = 0; i < 50; i++) {
                coordinator.detach(json, owner);
                assertThat(json.view()).isSameAs(view);
                assertThat(json.windowState()).isEqualTo(ToolSession.WindowState.DETACHED);
                coordinator.dock(json);
                assertThat(json.windowState()).isEqualTo(ToolSession.WindowState.DOCKED);
                assertThat(dock.getChildren().getFirst()).isSameAs(view);
            }
            ToolSession home = new ToolSession(ToolId.MOOTOOL, Label::new);
            coordinator.detach(home, owner);
            assertThat(coordinator.detached(ToolId.MOOTOOL)).isFalse();
            owner.close();
        });
    }
}
