package com.rememberber.mootool.nextfx.features.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rememberber.mootool.nextfx.application.AppExecutors;
import com.rememberber.mootool.nextfx.application.DocumentSession;
import com.rememberber.mootool.nextfx.application.ToolSession;
import com.rememberber.mootool.nextfx.domain.editor.FindMatch;
import com.rememberber.mootool.nextfx.domain.editor.FindReplace;
import com.rememberber.mootool.nextfx.domain.editor.FindReplaceOptions;
import com.rememberber.mootool.nextfx.domain.json.JsonEngine;
import com.rememberber.mootool.nextfx.domain.json.JsonException;
import com.rememberber.mootool.nextfx.domain.json.JsonFormatOptions;
import com.rememberber.mootool.nextfx.domain.json.JsonStatus;
import com.rememberber.mootool.nextfx.infrastructure.AppPaths;
import com.rememberber.mootool.nextfx.infrastructure.JsonVaultStore;
import com.rememberber.mootool.nextfx.infrastructure.SqliteDatabase;
import com.rememberber.mootool.nextfx.ui.editor.RichTextEditorHost;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
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
import javafx.scene.control.ToolBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class JsonView extends BorderPane {

    static final String SAMPLE = """
            {
              "name": "MooTool Next FX",
              "stack": ["OpenJDK", "JavaFX", "RichTextFX"],
              "desktop": {
                "style": "modern workspace",
                "theme": "light"
              }
            }
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final KeyCodeCombination SAVE_KEYS =
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);

    private final Translator translator;
    private final AppExecutors executors;
    private final SqliteDatabase database;
    private final JsonVaultStore vault;
    private final RichTextEditorHost editor = new RichTextEditorHost();
    private final Label status = new Label();
    private final Label notice = new Label();
    private final TextField jsonPathField = new TextField("$");
    private final TextArea pathResult = new TextArea();
    private final TextField findField = new TextField();
    private final CheckBox sortKeys = new CheckBox();
    private final CheckBox ignoreCase = new CheckBox();
    private final CheckBox checkDuplicates = new CheckBox();
    private final ToggleButton wrapToggle = new ToggleButton();
    private final HBox findBar = new HBox(8);
    private final ListView<String> historyList = new ListView<>();
    private final AtomicLong requestId = new AtomicLong();
    private final PauseTransition draftDebounce = new PauseTransition(Duration.millis(600));
    private final List<SqliteDatabase.HistoryRow> historyRows = new ArrayList<>();
    private JsonVaultPanel vaultPanel;
    private String savedContent = "";
    private JsonFormatOptions formatOptions = JsonFormatOptions.defaults();

    public JsonView(
            Translator translator,
            AppExecutors executors,
            SqliteDatabase database,
            AppPaths paths,
            ToolSession session
    ) {
        this.translator = translator;
        this.executors = executors;
        this.database = database;
        this.vault = new JsonVaultStore(paths.jsonVaultRoot());
        getStyleClass().add("mt-json");
        JsonDraft draft = loadDraft();
        DocumentSession document = session.document() == null
                ? new DocumentSession(draft.documentId(), draft.text(), draft.wrap())
                : session.document();
        session.setDocument(document);
        editor.openDocument(document);
        editor.setFont("Menlo", 14);
        wrapToggle.setSelected(document.wrap());
        jsonPathField.setText(draft.jsonPath());
        sortKeys.setSelected(draft.sortKeys());
        ignoreCase.setSelected(draft.ignoreCase());
        checkDuplicates.setSelected(draft.checkDuplicates());
        savedContent = draft.savedContent();

        vaultPanel = new JsonVaultPanel(translator, vault, this::openVaultFile);
        if (!draft.vaultPath().isBlank()) {
            vaultPanel.setSelectedPath(draft.vaultPath());
        }

        ToolBar toolbar = buildToolbar();
        findBar.setPadding(new Insets(6, 12, 6, 12));
        findBar.setAlignment(Pos.CENTER_LEFT);
        findBar.getStyleClass().add("mt-find-bar");
        findField.setPromptText(translator.t("json.action.find"));
        Button findNext = button(translator.t("findReplace.next"), this::findNext);
        Button replace = button(translator.t("findReplace.replace"), this::replaceCurrent);
        findBar.getChildren().addAll(new Label(translator.t("json.action.find")), findField, findNext, replace);
        findBar.setVisible(false);
        findBar.setManaged(false);

        VBox inspector = buildInspector();
        inspector.setPrefWidth(300);

        SplitPane split = new SplitPane(vaultPanel, editor.view(), inspector);
        split.setDividerPositions(0.18, 0.72);
        SplitPane.setResizableWithParent(vaultPanel, false);
        SplitPane.setResizableWithParent(inspector, false);

        VBox center = new VBox(toolbar, findBar, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        setCenter(center);

        HBox footer = new HBox(16);
        footer.setPadding(new Insets(6, 12, 6, 12));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("mt-status-bar");
        status.getStyleClass().add("mt-status");
        notice.getStyleClass().add("mt-muted");
        footer.getChildren().addAll(status, notice);
        setBottom(footer);

        wrapToggle.setText(translator.t(document.wrap() ? "json.action.wrap" : "json.action.nowrap"));
        sortKeys.setText(translator.t("json.option.sortKeys"));
        ignoreCase.setText(translator.t("json.option.ignoreCase"));
        checkDuplicates.setText(translator.t("json.option.duplicates"));
        editor.onChange(snapshot -> {
            refreshStatus();
            vaultPanel.markDirty(!editor.text().equals(savedContent));
            scheduleDraftSave();
        });
        draftDebounce.setOnFinished(event -> persistDraft());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.getAccelerators().remove(SAVE_KEYS);
            }
            if (newScene != null) {
                newScene.getAccelerators().put(SAVE_KEYS, this::saveCurrent);
            }
        });
        if (getScene() != null) {
            getScene().getAccelerators().put(SAVE_KEYS, this::saveCurrent);
        }
        refreshHistory();
        refreshStatus();
        vaultPanel.markDirty(!editor.text().equals(savedContent));
    }

    public RichTextEditorHost editor() {
        return editor;
    }

    public void format() {
        runJson("format", () -> JsonEngine.formatAdvanced(editor.text(), currentOptions()));
    }

    public void compress() {
        runJson("compress", () -> JsonEngine.compress(editor.text()));
    }

    public void saveCurrent() {
        String path = vaultPanel.selectedPath();
        if (path == null || path.isBlank()) {
            notice.setText(translator.t("json.vault.needFile"));
            return;
        }
        try {
            vault.save(path, editor.text());
            savedContent = editor.text();
            vaultPanel.markDirty(false);
            notice.setText(translator.t("json.vault.saved"));
            persistDraft();
        } catch (RuntimeException exception) {
            notice.setText(exception.getMessage());
        }
    }

    private ToolBar buildToolbar() {
        Button format = primary(translator.t("json.action.format"), this::format);
        Button compress = button(translator.t("json.action.compress"), this::compress);
        wrapToggle.setOnAction(event -> {
            editor.setWrap(wrapToggle.isSelected());
            wrapToggle.setText(translator.t(wrapToggle.isSelected() ? "json.action.wrap" : "json.action.nowrap"));
            persistDraft();
        });
        Button copy = button(translator.t("json.action.copy"), this::copy);
        Button find = button(translator.t("json.action.find"), this::toggleFind);
        Button save = button(translator.t("json.action.save"), this::saveCurrent);
        Button more = button(translator.t("json.action.more"), this::showMore);
        Button history = button(translator.t("json.action.history"), this::refreshHistory);
        Button clear = button(translator.t("json.action.clear"), this::clear);
        Button detach = button(translator.t("toolWindow.detach"), () -> fireEvent(new ToolActionEvent(ToolActionEvent.DETACH)));
        return new ToolBar(format, compress, wrapToggle, copy, find, save, history, more, clear, detach);
    }

    private VBox buildInspector() {
        VBox inspector = new VBox(10);
        inspector.setPadding(new Insets(12));
        inspector.getStyleClass().add("mt-panel");
        Label title = new Label(translator.t("json.inspector.title"));
        title.getStyleClass().add("mt-section-title");
        jsonPathField.setPromptText("$.store.books[0].title");
        Button query = primary(translator.t("json.action.jsonPath"), this::queryPath);
        pathResult.setPrefRowCount(8);
        pathResult.setWrapText(true);
        pathResult.getStyleClass().add("mt-result");
        Label historyTitle = new Label(translator.t("json.action.history"));
        historyTitle.getStyleClass().add("mt-section-title");
        historyList.setPrefHeight(160);
        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                restoreSelectedHistory();
            }
        });
        inspector.getChildren().addAll(
                title,
                sortKeys,
                ignoreCase,
                checkDuplicates,
                new Label(translator.t("json.action.jsonPath")),
                jsonPathField,
                query,
                pathResult,
                historyTitle,
                historyList
        );
        return inspector;
    }

    private JsonFormatOptions currentOptions() {
        formatOptions = new JsonFormatOptions(2, sortKeys.isSelected(), ignoreCase.isSelected(), checkDuplicates.isSelected());
        return formatOptions;
    }

    private void runJson(String summary, java.util.concurrent.Callable<String> work) {
        long id = requestId.incrementAndGet();
        String input = editor.text();
        status.setText(translator.t("json.busy"));
        executors.cpu().execute(() -> {
            try {
                String output = work.call();
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;
                    }
                    editor.setText(output, summary);
                    notice.setText("");
                    refreshStatus();
                    database.history().save("json", summary, input, output);
                    refreshHistory();
                    persistDraft();
                });
            } catch (Exception exception) {
                String message = exception instanceof JsonException json
                        ? translateJson(json)
                        : exception.getMessage();
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;
                    }
                    notice.setText(message);
                    refreshStatus();
                });
            }
        });
    }

    private void queryPath() {
        try {
            pathResult.setText(JsonEngine.queryJsonPath(editor.text(), jsonPathField.getText()));
            notice.setText("");
            persistDraft();
        } catch (JsonException exception) {
            pathResult.setText("");
            notice.setText(translateJson(exception));
        }
    }

    private void copy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(editor.text());
        Clipboard.getSystemClipboard().setContent(content);
        notice.setText(translator.t("json.action.copied"));
    }

    private void toggleFind() {
        boolean next = !findBar.isVisible();
        findBar.setVisible(next);
        findBar.setManaged(next);
        if (next) {
            findField.requestFocus();
        }
    }

    private void findNext() {
        FindMatch match = FindReplace.findNext(
                editor.text(),
                findField.getText(),
                FindReplaceOptions.defaults(),
                editor.snapshot().caret(),
                true
        );
        editor.reveal(match);
    }

    private void replaceCurrent() {
        FindMatch match = FindReplace.findNext(
                editor.text(),
                findField.getText(),
                FindReplaceOptions.defaults(),
                editor.snapshot().caret(),
                true
        );
        if (match != null) {
            editor.setText(FindReplace.replaceCurrent(editor.text(), match, ""), "replace");
        }
    }

    private void showMore() {
        try {
            pathResult.setText(JsonEngine.jsonToXml(editor.text()));
        } catch (JsonException exception) {
            notice.setText(translateJson(exception));
        }
    }

    private void clear() {
        editor.setText("", "clear");
        persistDraft();
    }

    private void openVaultFile(String path, String content) {
        editor.setText(content, "vault-open");
        savedContent = content;
        vaultPanel.setSelectedPath(path);
        vaultPanel.markDirty(false);
        persistDraft();
    }

    private void refreshHistory() {
        historyRows.clear();
        historyRows.addAll(database.history().latest("json", 20));
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
        editor.setText(historyRows.get(index).output(), "history");
        persistDraft();
    }

    private void scheduleDraftSave() {
        draftDebounce.stop();
        draftDebounce.playFromStart();
    }

    private void persistDraft() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("text", editor.text());
            node.put("caret", editor.snapshot().caret());
            node.put("anchor", editor.snapshot().anchor());
            node.put("wrap", wrapToggle.isSelected());
            node.put("vaultPath", vaultPanel.selectedPath() == null ? "" : vaultPanel.selectedPath());
            node.put("savedContent", savedContent);
            node.put("jsonPath", jsonPathField.getText());
            node.put("sortKeys", sortKeys.isSelected());
            node.put("ignoreCase", ignoreCase.isSelected());
            node.put("checkDuplicates", checkDuplicates.isSelected());
            database.drafts().save("json", MAPPER.writeValueAsString(node));
        } catch (Exception ignored) {
            // Draft persistence is best-effort.
        }
    }

    private JsonDraft loadDraft() {
        try {
            String payload = database.drafts().load("json");
            if (payload == null || payload.isBlank()) {
                return JsonDraft.sample();
            }
            JsonNode node = MAPPER.readTree(payload);
            return new JsonDraft(
                    node.path("vaultPath").asText("json-scratch"),
                    node.path("text").asText(SAMPLE),
                    node.path("wrap").asBoolean(true),
                    node.path("vaultPath").asText(""),
                    node.path("savedContent").asText(""),
                    node.path("jsonPath").asText("$"),
                    node.path("sortKeys").asBoolean(false),
                    node.path("ignoreCase").asBoolean(false),
                    node.path("checkDuplicates").asBoolean(true)
            );
        } catch (Exception exception) {
            return JsonDraft.sample();
        }
    }

    private void refreshStatus() {
        JsonStatus current = JsonEngine.validate(editor.text());
        status.setText(switch (current.kind()) {
            case IDLE -> translator.t("json.valid.idle");
            case VALID -> translator.t("json.valid.ok", "type", current.type());
            case ERROR -> current.details().isBlank() ? translator.t("json.valid.error") : current.details();
        });
        status.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("error"), current.kind() == JsonStatus.Kind.ERROR);
        status.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("valid"), current.kind() == JsonStatus.Kind.VALID);
    }

    private String translateJson(JsonException exception) {
        if ("json.error.duplicateKeys".equals(exception.messageKey())) {
            return translator.t(exception.messageKey(), "paths", exception.details());
        }
        String translated = translator.t(exception.messageKey());
        if (exception.details() == null || exception.details().isBlank() || translated.equals(exception.details())) {
            return translated;
        }
        return translated + " · " + exception.details();
    }

    private Button primary(String text, Runnable action) {
        Button button = button(text, action);
        button.getStyleClass().add("primary");
        return button;
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private record JsonDraft(
            String documentId,
            String text,
            boolean wrap,
            String vaultPath,
            String savedContent,
            String jsonPath,
            boolean sortKeys,
            boolean ignoreCase,
            boolean checkDuplicates
    ) {
        static JsonDraft sample() {
            return new JsonDraft("json-scratch", SAMPLE, true, "", SAMPLE, "$", false, false, true);
        }
    }

    public static final class ToolActionEvent extends javafx.event.Event {
        public static final javafx.event.EventType<ToolActionEvent> DETACH =
                new javafx.event.EventType<>(javafx.event.Event.ANY, "TOOL_DETACH");

        public ToolActionEvent(javafx.event.EventType<ToolActionEvent> type) {
            super(type);
        }
    }
}
