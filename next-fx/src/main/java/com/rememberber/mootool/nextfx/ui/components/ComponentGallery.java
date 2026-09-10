package com.rememberber.mootool.nextfx.ui.components;

import com.rememberber.mootool.nextfx.application.DocumentSession;
import com.rememberber.mootool.nextfx.ui.editor.RichTextEditorHost;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public final class ComponentGallery extends VBox {

    public ComponentGallery(Translator translator) {
        getStyleClass().add("mt-gallery");
        setSpacing(16);
        setPadding(new Insets(24));
        Label title = new Label(translator.t("app.dev.gallery"));
        title.getStyleClass().add("mt-home-title");
        FlowPane buttons = new FlowPane(8, 8);
        Button primary = new Button(translator.t("json.action.format"));
        primary.getStyleClass().add("primary");
        Button danger = new Button(translator.t("json.action.clear"));
        danger.getStyleClass().add("danger");
        buttons.getChildren().addAll(primary, new Button(translator.t("json.action.copy")), danger);

        TextField field = new TextField();
        field.setPromptText(translator.t("app.search.placeholder"));
        CheckBox check = new CheckBox(translator.t("json.option.sortKeys"));
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("modern", "quiet");
        combo.getSelectionModel().selectFirst();
        ProgressBar bar = new ProgressBar(0.42);

        RichTextEditorHost editor = new RichTextEditorHost();
        editor.openDocument(new DocumentSession("gallery", "{\n  \"sample\": true\n}\n", true));
        javafx.scene.layout.Region editorView = (javafx.scene.layout.Region) editor.view();
        editorView.setPrefHeight(220);

        Label error = new Label(translator.t("json.valid.error"));
        error.getStyleClass().add("mt-error");
        getChildren().addAll(title, buttons, field, check, combo, bar, error, editor.view());
    }
}
