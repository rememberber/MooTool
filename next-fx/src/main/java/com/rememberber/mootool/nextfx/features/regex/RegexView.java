package com.rememberber.mootool.nextfx.features.regex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rememberber.mootool.nextfx.application.AppExecutors;
import com.rememberber.mootool.nextfx.domain.regex.RegexEngine;
import com.rememberber.mootool.nextfx.domain.regex.RegexException;
import com.rememberber.mootool.nextfx.infrastructure.SqliteDatabase;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import com.rememberber.mootool.nextfx.ui.shell.ToolActionEvent;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class RegexView extends BorderPane {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Translator translator;
    private final AppExecutors executors;
    private final SqliteDatabase database;
    private final TextField patternField = new TextField("(moo)(\\d+)");
    private final TextArea source = new TextArea("moo1\nMOO22\nmoo333");
    private final CheckBox global = new CheckBox();
    private final CheckBox ignoreCase = new CheckBox();
    private final CheckBox multiline = new CheckBox();
    private final CheckBox dotAll = new CheckBox();
    private final Label status = new Label();
    private final Label engine = new Label();
    private final ListView<String> matchList = new ListView<>();
    private final ListView<String> historyList = new ListView<>();
    private final ListView<String> favoriteList = new ListView<>();
    private final ListView<String> commonList = new ListView<>();
    private final TextField favoriteName = new TextField();
    private final List<RegexEngine.Match> matches = new ArrayList<>();
    private final List<SqliteDatabase.HistoryRow> historyRows = new ArrayList<>();
    private final List<SqliteDatabase.FavoriteRow> favoriteRows = new ArrayList<>();
    private final AtomicLong requestId = new AtomicLong();
    private final PauseTransition draftDebounce = new PauseTransition(Duration.millis(600));
    private final ToggleGroup tabs = new ToggleGroup();
    private final StackPane workspace = new StackPane();
    private boolean testTab = true;

    public RegexView(Translator translator, AppExecutors executors, SqliteDatabase database) {
        this.translator = translator;
        this.executors = executors;
        this.database = database;
        getStyleClass().add("mt-regex");
        source.setWrapText(true);
        source.getStyleClass().add("mt-code-area");
        global.setSelected(true);
        global.setText(translator.t("regex.flag.global"));
        ignoreCase.setText(translator.t("regex.flag.ignoreCase"));
        multiline.setText(translator.t("regex.flag.multiline"));
        dotAll.setText(translator.t("regex.flag.dotAll"));
        engine.setText(translator.t("regex.engine", "engine", RegexEngine.ENGINE_NAME));
        engine.getStyleClass().add("mt-muted");
        favoriteName.setPromptText(translator.t("favorite.namePlaceholder"));
        loadDraft();

        ToggleButton test = tabButton("regex.tab.test", true);
        ToggleButton common = tabButton("regex.tab.common", false);
        tabs.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            showTest(newToggle == test);
        });
        (testTab ? test : common).setSelected(true);
        ToolBar toolbar = new ToolBar(
                test,
                common,
                button(translator.t("json.action.history"), this::refreshHistory),
                button(translator.t("favorite.title"), this::refreshFavorites),
                button(translator.t("toolWindow.detach"), () -> fireEvent(new ToolActionEvent(ToolActionEvent.DETACH)))
        );

        VBox header = new VBox(4);
        header.setPadding(new Insets(12, 12, 0, 12));
        Label title = new Label(translator.t("regex.title"));
        title.getStyleClass().add("mt-section-title");
        header.getChildren().add(title);

        workspace.getChildren().addAll(buildTestPane(), buildCommonPane());
        showTest(testTab);
        fillCommonList();

        VBox center = new VBox(header, toolbar, workspace);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        setCenter(center);

        HBox footer = new HBox(16, status, engine);
        footer.setPadding(new Insets(6, 12, 6, 12));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("mt-status-bar");
        setBottom(footer);

        patternField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                run();
            }
        });
        source.textProperty().addListener((obs, old, value) -> scheduleDraftSave());
        patternField.textProperty().addListener((obs, old, value) -> scheduleDraftSave());
        global.selectedProperty().addListener((obs, old, value) -> scheduleDraftSave());
        ignoreCase.selectedProperty().addListener((obs, old, value) -> scheduleDraftSave());
        multiline.selectedProperty().addListener((obs, old, value) -> scheduleDraftSave());
        dotAll.selectedProperty().addListener((obs, old, value) -> scheduleDraftSave());
        draftDebounce.setOnFinished(event -> persistDraft());
        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                restoreSelectedHistory();
            }
        });
        favoriteList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                applySelectedFavorite();
            }
        });
        commonList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 || event.getClickCount() == 2) {
                applySelectedCommon();
            }
        });
        refreshHistory();
        refreshFavorites();
        status.setText(translator.t("regex.matches", "count", "0"));
    }

    private VBox buildTestPane() {
        Button run = primary(translator.t("regex.tab.test"), this::run);
        HBox expression = new HBox(8, patternField, run);
        HBox.setHgrow(patternField, Priority.ALWAYS);
        expression.setAlignment(Pos.CENTER_LEFT);
        HBox flags = new HBox(12, global, ignoreCase, multiline, dotAll);
        flags.setAlignment(Pos.CENTER_LEFT);
        VBox controls = new VBox(8, new Label(translator.t("regex.expression")), expression, flags);
        controls.setPadding(new Insets(12));

        VBox sourcePane = new VBox(6, new Label(translator.t("regex.source")), source);
        sourcePane.setPadding(new Insets(12));
        VBox.setVgrow(source, Priority.ALWAYS);

        VBox results = new VBox(8);
        results.setPadding(new Insets(12));
        results.getStyleClass().add("mt-panel");
        Label matchTitle = new Label(translator.t("regex.tab.test"));
        matchTitle.getStyleClass().add("mt-section-title");
        VBox.setVgrow(matchList, Priority.ALWAYS);
        Label historyTitle = new Label(translator.t("json.action.history"));
        historyTitle.getStyleClass().add("mt-section-title");
        historyList.setPrefHeight(120);
        Label favoriteTitle = new Label(translator.t("favorite.title"));
        favoriteTitle.getStyleClass().add("mt-section-title");
        Button addFavorite = button(translator.t("favorite.add"), this::addFavorite);
        Button deleteFavorite = button(translator.t("common.action.delete"), this::deleteSelectedFavorite);
        HBox favoriteBar = new HBox(8, favoriteName, addFavorite, deleteFavorite);
        HBox.setHgrow(favoriteName, Priority.ALWAYS);
        favoriteList.setPrefHeight(120);
        results.getChildren().addAll(
                matchTitle, matchList, historyTitle, historyList, favoriteTitle, favoriteBar, favoriteList
        );

        SplitPane split = new SplitPane(sourcePane, results);
        split.setDividerPositions(0.58);
        VBox pane = new VBox(controls, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        return pane;
    }

    private VBox buildCommonPane() {
        VBox pane = new VBox(8);
        pane.setPadding(new Insets(12));
        Label title = new Label(translator.t("regex.tab.common"));
        title.getStyleClass().add("mt-section-title");
        VBox.setVgrow(commonList, Priority.ALWAYS);
        pane.getChildren().addAll(title, commonList);
        return pane;
    }

    private void showTest(boolean test) {
        testTab = test;
        workspace.getChildren().getFirst().setVisible(test);
        workspace.getChildren().getFirst().setManaged(test);
        workspace.getChildren().get(1).setVisible(!test);
        workspace.getChildren().get(1).setManaged(!test);
        persistDraft();
    }

    private void run() {
        String pattern = patternField.getText();
        String text = source.getText();
        RegexEngine.Options options = currentOptions();
        long id = requestId.incrementAndGet();
        status.setText(translator.t("regex.busy"));
        executors.cpu().execute(() -> {
            try {
                List<RegexEngine.Match> next = RegexEngine.match(pattern, text, options);
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;
                    }
                    matches.clear();
                    matches.addAll(next);
                    renderMatches("");
                    database.history().save(
                            "regex",
                            translator.t("regex.matches", "count", String.valueOf(next.size())),
                            pattern,
                            text,
                            extraJson(options, next.size())
                    );
                    refreshHistory();
                    persistDraft();
                });
            } catch (Exception exception) {
                String message = translateRegex(exception);
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;
                    }
                    matches.clear();
                    renderMatches(message);
                });
            }
        });
    }

    private void renderMatches(String error) {
        matchList.getItems().clear();
        if (!error.isBlank()) {
            status.setText(error);
            status.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("error"), true);
            return;
        }
        status.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("error"), false);
        status.setText(translator.t("regex.matches", "count", String.valueOf(matches.size())));
        if (matches.isEmpty()) {
            matchList.getItems().add(translator.t("regex.noMatches"));
            return;
        }
        int index = 1;
        for (RegexEngine.Match match : matches) {
            String groups = match.groups().isEmpty() ? "" : " · " + String.join(" · ", match.groups());
            String value = match.value().isEmpty() ? "∅" : match.value();
            matchList.getItems().add("#" + index + " · " + match.index() + " · " + value + groups);
            index++;
        }
    }

    private void fillCommonList() {
        commonList.getItems().clear();
        for (RegexEngine.CommonPattern item : RegexEngine.COMMON) {
            commonList.getItems().add(translator.t(item.labelKey()) + "  " + item.pattern());
        }
    }

    private void applySelectedCommon() {
        int index = commonList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= RegexEngine.COMMON.size()) {
            return;
        }
        patternField.setText(RegexEngine.COMMON.get(index).pattern());
        tabs.getToggles().getFirst().setSelected(true);
        persistDraft();
    }

    private void refreshHistory() {
        historyRows.clear();
        historyRows.addAll(database.history().latest("regex", 20));
        historyList.getItems().clear();
        for (SqliteDatabase.HistoryRow row : historyRows) {
            historyList.getItems().add(row.createdAt() + " · " + row.summary());
        }
    }

    private void restoreSelectedHistory() {
        int index = historyList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= historyRows.size()) {
            return;
        }
        SqliteDatabase.HistoryRow row = historyRows.get(index);
        patternField.setText(row.input());
        source.setText(row.output());
        try {
            JsonNode meta = MAPPER.readTree(row.extra() == null || row.extra().isBlank() ? "{}" : row.extra());
            JsonNode options = meta.path("options");
            global.setSelected(options.path("global").asBoolean(true));
            ignoreCase.setSelected(options.path("ignoreCase").asBoolean(false));
            multiline.setSelected(options.path("multiline").asBoolean(false));
            dotAll.setSelected(options.path("dotAll").asBoolean(false));
        } catch (Exception ignored) {
            // Keep current flags if extra is not JSON.
        }
        persistDraft();
        run();
    }

    private void refreshFavorites() {
        favoriteRows.clear();
        favoriteRows.addAll(database.favorites().list("regex"));
        favoriteList.getItems().clear();
        if (favoriteRows.isEmpty()) {
            favoriteList.getItems().add(translator.t("favorite.empty"));
            return;
        }
        for (SqliteDatabase.FavoriteRow row : favoriteRows) {
            favoriteList.getItems().add(row.name() + "  " + row.value());
        }
    }

    private void addFavorite() {
        String name = favoriteName.getText() == null ? "" : favoriteName.getText().trim();
        String value = patternField.getText() == null ? "" : patternField.getText().trim();
        if (name.isEmpty() || value.isEmpty()) {
            return;
        }
        database.favorites().save("regex", name, value);
        favoriteName.clear();
        status.setText(translator.t("favorite.saved"));
        refreshFavorites();
    }

    private void applySelectedFavorite() {
        int index = favoriteList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= favoriteRows.size()) {
            return;
        }
        patternField.setText(favoriteRows.get(index).value());
        tabs.getToggles().getFirst().setSelected(true);
        persistDraft();
    }

    private void deleteSelectedFavorite() {
        int index = favoriteList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= favoriteRows.size()) {
            return;
        }
        database.favorites().delete(favoriteRows.get(index).id());
        status.setText(translator.t("favorite.deleted"));
        refreshFavorites();
    }

    private RegexEngine.Options currentOptions() {
        return new RegexEngine.Options(
                global.isSelected(),
                ignoreCase.isSelected(),
                multiline.isSelected(),
                dotAll.isSelected()
        );
    }

    private String extraJson(RegexEngine.Options options, int matchCount) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            ObjectNode optionNode = node.putObject("options");
            optionNode.put("global", options.global());
            optionNode.put("ignoreCase", options.ignoreCase());
            optionNode.put("multiline", options.multiline());
            optionNode.put("dotAll", options.dotAll());
            node.put("matchCount", matchCount);
            return MAPPER.writeValueAsString(node);
        } catch (Exception exception) {
            return "";
        }
    }

    private void scheduleDraftSave() {
        draftDebounce.stop();
        draftDebounce.playFromStart();
    }

    private void persistDraft() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("tab", testTab ? "test" : "common");
            node.put("pattern", patternField.getText());
            node.put("source", source.getText());
            node.put("global", global.isSelected());
            node.put("ignoreCase", ignoreCase.isSelected());
            node.put("multiline", multiline.isSelected());
            node.put("dotAll", dotAll.isSelected());
            database.drafts().save("regex", MAPPER.writeValueAsString(node));
        } catch (Exception ignored) {
            // Draft persistence is best-effort.
        }
    }

    private void loadDraft() {
        try {
            String payload = database.drafts().load("regex");
            if (payload == null || payload.isBlank()) {
                return;
            }
            JsonNode node = MAPPER.readTree(payload);
            patternField.setText(node.path("pattern").asText(patternField.getText()));
            source.setText(node.path("source").asText(source.getText()));
            global.setSelected(node.path("global").asBoolean(true));
            ignoreCase.setSelected(node.path("ignoreCase").asBoolean(false));
            multiline.setSelected(node.path("multiline").asBoolean(false));
            dotAll.setSelected(node.path("dotAll").asBoolean(false));
            testTab = !"common".equals(node.path("tab").asText("test"));
        } catch (Exception ignored) {
            // Keep defaults.
        }
    }

    private String translateRegex(Exception exception) {
        if (exception instanceof RegexException regex) {
            if ("regex.invalid".equals(regex.messageKey())) {
                return translator.t(regex.messageKey(), "message", regex.details());
            }
            return translator.t(regex.messageKey());
        }
        return exception.getMessage() == null ? translator.t("regex.invalid", "message", "") : exception.getMessage();
    }

    private ToggleButton tabButton(String key, boolean test) {
        ToggleButton button = new ToggleButton(translator.t(key));
        button.setToggleGroup(tabs);
        button.setUserData(test);
        return button;
    }

    private Button primary(String text, Runnable action) {
        Button button = button(text, action);
        button.getStyleClass().add("primary");
        return button;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }
}
