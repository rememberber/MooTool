package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.domain.ToolDefinition;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class PlaceholderView extends VBox {

    public PlaceholderView(ToolDefinition definition, Translator translator) {
        getStyleClass().add("mt-placeholder");
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(32));
        Label title = new Label(translator.t(definition.titleKey()));
        title.getStyleClass().add("mt-placeholder-title");
        Label status = new Label(translator.t("app.placeholder.status"));
        status.getStyleClass().add("mt-muted");
        Label detail = new Label(translator.t("app.placeholder.detail"));
        detail.setWrapText(true);
        detail.getStyleClass().add("mt-muted");
        getChildren().addAll(title, status, detail);
    }
}
