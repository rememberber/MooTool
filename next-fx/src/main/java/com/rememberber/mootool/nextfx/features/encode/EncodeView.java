package com.rememberber.mootool.nextfx.features.encode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rememberber.mootool.nextfx.application.AppExecutors;
import com.rememberber.mootool.nextfx.domain.encode.EncodeEngine;
import com.rememberber.mootool.nextfx.domain.encode.EncodeException;
import com.rememberber.mootool.nextfx.infrastructure.SqliteDatabase;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import com.rememberber.mootool.nextfx.ui.shell.ToolActionEvent;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class EncodeView extends BorderPane {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<EncodeEngine.Tab, Pair> INITIAL = Map.of(
            EncodeEngine.Tab.UNICODE, new Pair("MooTool 编码转换", ""),
            EncodeEngine.Tab.URL, new Pair("https://mootool.app/search?q=编码", ""),
            EncodeEngine.Tab.HEX, new Pair("MooTool", ""),
            EncodeEngine.Tab.ASCII, new Pair("MooTool", "")
    );

    private final Translator translator;
    private final AppExecutors executors;
    private final SqliteDatabase database;
    private final EnumMap<EncodeEngine.Tab, Pair> pairs = new EnumMap<>(EncodeEngine.Tab.class);
    private final TextArea left = new TextArea();
    private final TextArea right = new TextArea();
    private final Label leftLabel = new Label();
    private final Label rightLabel = new Label();
    private final Button forward = new Button();
    private final Button reverse = new Button();
    private final ComboBox<String> charsetBox = new ComboBox<>();
    private final ComboBox<String> asciiBox = new ComboBox<>();
    private final Label charsetLabel = new Label();
    private final Label asciiLabel = new Label();
    private final Label notice = new Label();
    private final ListView<String> historyList = new ListView<>();
    private final List<SqliteDatabase.HistoryRow> historyRows = new ArrayList<>();
    private final AtomicLong requestId = new AtomicLong();
    private final PauseTransition draftDebounce = new PauseTransition(Duration.millis(600));
    private final ToggleGroup tabs = new ToggleGroup();
    private EncodeEngine.Tab tab = EncodeEngine.Tab.UNICODE;
    private boolean applying;

    public EncodeView(Translator translator, AppExecutors executors, SqliteDatabase database) {
        this.translator = translator;
        this.executors = executors;
        this.database = database;
        getStyleClass().add("mt-encode");
        pairs.putAll(INITIAL);

        left.setWrapText(true);
        right.setWrapText(true);
        left.getStyleClass().add("mt-code-area");
        right.getStyleClass().add("mt-code-area");
        notice.getStyleClass().add("mt-muted");

        charsetBox.getItems().addAll("UTF-8", "GB2312");
        asciiBox.getItems().addAll(translator.t("encode.asciiDecimal"), translator.t("encode.asciiHex"));
        charsetBox.setValue("UTF-8");
        asciiBox.setValue(translator.t("encode.asciiDecimal"));
        charsetLabel.setText(translator.t("encode.charset"));
        asciiLabel.setText("ASCII");
        loadDraft();

        ToolBar toolbar = new ToolBar();
        for (EncodeEngine.Tab item : EncodeEngine.Tab.values()) {
            ToggleButton button = new ToggleButton(translator.t("encode.tab." + item.id()));
            button.setToggleGroup(tabs);
            button.setUserData(item);
            if (item == tab) {
                button.setSelected(true);
            }
            toolbar.getItems().add(button);
        }
        tabs.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            selectTab((EncodeEngine.Tab) newToggle.getUserData());
        });
        toolbar.getItems().addAll(
                button(translator.t("json.action.history"), this::refreshHistory),
                button(translator.t("json.action.clear"), this::clearCurrent),
                button(translator.t("toolWindow.detach"), () -> fireEvent(new ToolActionEvent(ToolActionEvent.DETACH)))
        );

        VBox leftPane = pane(leftLabel, left);
        VBox rightPane = pane(rightLabel, right);
        VBox actions = buildActions();
        SplitPane split = new SplitPane(leftPane, actions, rightPane);
        split.setDividerPositions(0.42, 0.58);
        SplitPane.setResizableWithParent(actions, false);

        VBox header = new VBox(4);
        header.setPadding(new Insets(12, 12, 0, 12));
        Label title = new Label(translator.t("encode.title"));
        title.getStyleClass().add("mt-section-title");
        header.getChildren().add(title);

        VBox center = new VBox(header, toolbar, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        setCenter(center);

        HBox footer = new HBox(notice);
        footer.setPadding(new Insets(6, 12, 6, 12));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("mt-status-bar");
        setBottom(footer);

        left.textProperty().addListener((obs, old, value) -> onTextChanged());
        right.textProperty().addListener((obs, old, value) -> onTextChanged());
        charsetBox.valueProperty().addListener((obs, old, value) -> scheduleDraftSave());
        asciiBox.valueProperty().addListener((obs, old, value) -> scheduleDraftSave());
        draftDebounce.setOnFinished(event -> persistDraft());
        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                restoreSelectedHistory();
            }
        });
        applyPairToEditors();
        refreshLabels();
        refreshOptionVisibility();
        refreshHistory();
    }

    private VBox buildActions() {
        VBox actions = new VBox(10);
        actions.getStyleClass().add("mt-io-actions");
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(16, 12, 16, 12));
        actions.setMinWidth(160);
        actions.setPrefWidth(180);
        forward.getStyleClass().add("primary");
        forward.setMaxWidth(Double.MAX_VALUE);
        reverse.setMaxWidth(Double.MAX_VALUE);
        forward.setOnAction(event -> convert(EncodeEngine.Direction.FORWARD));
        reverse.setOnAction(event -> convert(EncodeEngine.Direction.REVERSE));
        charsetBox.setMaxWidth(Double.MAX_VALUE);
        asciiBox.setMaxWidth(Double.MAX_VALUE);
        Label historyTitle = new Label(translator.t("json.action.history"));
        historyTitle.getStyleClass().add("mt-section-title");
        historyList.setPrefHeight(180);
        VBox.setVgrow(historyList, Priority.ALWAYS);
        actions.getChildren().addAll(
                forward,
                reverse,
                charsetLabel,
                charsetBox,
                asciiLabel,
                asciiBox,
                historyTitle,
                historyList
        );
        return actions;
    }

    private void selectTab(EncodeEngine.Tab next) {
        if (next == tab) {
            return;
        }
        storeEditors();
        tab = next;
        applyPairToEditors();
        refreshLabels();
        refreshOptionVisibility();
        persistDraft();
    }

    private void convert(EncodeEngine.Direction direction) {
        storeEditors();
        Pair pair = currentPair();
        String input = direction == EncodeEngine.Direction.FORWARD ? pair.left : pair.right;
        long id = requestId.incrementAndGet();
        EncodeEngine.Tab currentTab = tab;
        EncodeEngine.UrlCharset charset = currentCharset();
        EncodeEngine.AsciiFormat asciiFormat = currentAsciiFormat();
        notice.setText(translator.t("encode.busy"));
        executors.cpu().execute(() -> {
            try {
                String output = EncodeEngine.convert(currentTab, direction, input, charset, asciiFormat);
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;
                    }
                    if (direction == EncodeEngine.Direction.FORWARD) {
                        pairs.put(currentTab, new Pair(input, output));
                    } else {
                        pairs.put(currentTab, new Pair(output, input));
                    }
                    if (currentTab == tab) {
                        applyPairToEditors();
                    }
                    notice.setText("");
                    database.history().save(
                            "encode",
                            direction == EncodeEngine.Direction.FORWARD ? forward.getText() : reverse.getText(),
                            input,
                            output,
                            extraJson(currentTab, direction, charset, asciiFormat)
                    );
                    refreshHistory();
                    persistDraft();
                });
            } catch (Exception exception) {
                String message = translateEncode(exception);
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;
                    }
                    notice.setText(message);
                });
            }
        });
    }

    private void clearCurrent() {
        for (EncodeEngine.Tab item : EncodeEngine.Tab.values()) {
            pairs.put(item, INITIAL.get(item));
        }
        pairs.put(tab, new Pair("", ""));
        applyPairToEditors();
        notice.setText("");
        persistDraft();
    }

    private void refreshHistory() {
        historyRows.clear();
        historyRows.addAll(database.history().latest("encode", 20));
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
        try {
            JsonNode meta = MAPPER.readTree(row.extra() == null || row.extra().isBlank() ? "{}" : row.extra());
            EncodeEngine.Tab nextTab = EncodeEngine.Tab.fromId(meta.path("tab").asText("unicode"));
            boolean reverseRestore = "reverse".equals(meta.path("direction").asText());
            tab = nextTab;
            selectToolbarTab(nextTab);
            charsetBox.setValue("gb2312".equalsIgnoreCase(meta.path("charset").asText()) ? "GB2312" : "UTF-8");
            asciiBox.setValue(EncodeEngine.AsciiFormat.HEX == EncodeEngine.AsciiFormat.fromId(meta.path("asciiFormat").asText())
                    ? translator.t("encode.asciiHex")
                    : translator.t("encode.asciiDecimal"));
            pairs.put(nextTab, reverseRestore
                    ? new Pair(row.output(), row.input())
                    : new Pair(row.input(), row.output()));
            applyPairToEditors();
            refreshLabels();
            refreshOptionVisibility();
            persistDraft();
        } catch (Exception exception) {
            pairs.put(tab, new Pair(currentPair().left, row.output()));
            applyPairToEditors();
        }
    }

    private void selectToolbarTab(EncodeEngine.Tab next) {
        tabs.getToggles().stream()
                .filter(toggle -> toggle.getUserData() == next)
                .findFirst()
                .ifPresent(toggle -> toggle.setSelected(true));
    }

    private void refreshLabels() {
        leftLabel.setText(translator.t(switch (tab) {
            case UNICODE, HEX, ASCII -> "encode.native";
            case URL -> "encode.url";
        }));
        rightLabel.setText(translator.t(switch (tab) {
            case UNICODE -> "encode.unicode";
            case URL -> "encode.encoded";
            case HEX -> "encode.hex";
            case ASCII -> "encode.ascii";
        }));
        forward.setText(translator.t(switch (tab) {
            case UNICODE -> "encode.toUnicode";
            case URL -> "encode.urlEncode";
            case HEX -> "encode.toHex";
            case ASCII -> "encode.toAscii";
        }));
        reverse.setText(translator.t(switch (tab) {
            case UNICODE -> "encode.fromUnicode";
            case URL -> "encode.urlDecode";
            case HEX -> "encode.fromHex";
            case ASCII -> "encode.fromAscii";
        }));
    }

    private void refreshOptionVisibility() {
        boolean url = tab == EncodeEngine.Tab.URL;
        boolean ascii = tab == EncodeEngine.Tab.ASCII;
        charsetLabel.setVisible(url);
        charsetLabel.setManaged(url);
        charsetBox.setVisible(url);
        charsetBox.setManaged(url);
        asciiLabel.setVisible(ascii);
        asciiLabel.setManaged(ascii);
        asciiBox.setVisible(ascii);
        asciiBox.setManaged(ascii);
    }

    private void onTextChanged() {
        if (applying) {
            return;
        }
        storeEditors();
        scheduleDraftSave();
    }

    private void storeEditors() {
        pairs.put(tab, new Pair(left.getText(), right.getText()));
    }

    private void applyPairToEditors() {
        applying = true;
        Pair pair = currentPair();
        left.setText(pair.left);
        right.setText(pair.right);
        applying = false;
    }

    private Pair currentPair() {
        return pairs.getOrDefault(tab, new Pair("", ""));
    }

    private EncodeEngine.UrlCharset currentCharset() {
        return EncodeEngine.UrlCharset.fromId("GB2312".equals(charsetBox.getValue()) ? "gb2312" : "utf-8");
    }

    private EncodeEngine.AsciiFormat currentAsciiFormat() {
        return translator.t("encode.asciiHex").equals(asciiBox.getValue())
                ? EncodeEngine.AsciiFormat.HEX
                : EncodeEngine.AsciiFormat.DECIMAL;
    }

    private void scheduleDraftSave() {
        draftDebounce.stop();
        draftDebounce.playFromStart();
    }

    private void persistDraft() {
        try {
            storeEditors();
            ObjectNode node = MAPPER.createObjectNode();
            node.put("tab", tab.id());
            node.put("charset", currentCharset().id());
            node.put("asciiFormat", currentAsciiFormat().id());
            ObjectNode pairNode = node.putObject("pairs");
            for (EncodeEngine.Tab item : EncodeEngine.Tab.values()) {
                Pair pair = pairs.getOrDefault(item, new Pair("", ""));
                ObjectNode entry = pairNode.putObject(item.id());
                entry.put("left", pair.left);
                entry.put("right", pair.right);
            }
            database.drafts().save("encode", MAPPER.writeValueAsString(node));
        } catch (Exception ignored) {
            // Draft persistence is best-effort.
        }
    }

    private void loadDraft() {
        try {
            String payload = database.drafts().load("encode");
            if (payload == null || payload.isBlank()) {
                return;
            }
            JsonNode node = MAPPER.readTree(payload);
            tab = EncodeEngine.Tab.fromId(node.path("tab").asText("unicode"));
            charsetBox.setValue("gb2312".equalsIgnoreCase(node.path("charset").asText()) ? "GB2312" : "UTF-8");
            asciiBox.setValue(EncodeEngine.AsciiFormat.HEX == EncodeEngine.AsciiFormat.fromId(node.path("asciiFormat").asText())
                    ? translator.t("encode.asciiHex")
                    : translator.t("encode.asciiDecimal"));
            JsonNode pairNode = node.path("pairs");
            for (EncodeEngine.Tab item : EncodeEngine.Tab.values()) {
                JsonNode entry = pairNode.path(item.id());
                if (entry.isObject()) {
                    pairs.put(item, new Pair(entry.path("left").asText(""), entry.path("right").asText("")));
                }
            }
        } catch (Exception ignored) {
            tab = EncodeEngine.Tab.UNICODE;
        }
    }

    private String extraJson(
            EncodeEngine.Tab currentTab,
            EncodeEngine.Direction direction,
            EncodeEngine.UrlCharset charset,
            EncodeEngine.AsciiFormat asciiFormat
    ) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("tab", currentTab.id());
            node.put("direction", direction == EncodeEngine.Direction.FORWARD ? "forward" : "reverse");
            node.put("charset", charset.id());
            node.put("asciiFormat", asciiFormat.id());
            return MAPPER.writeValueAsString(node);
        } catch (Exception exception) {
            return "";
        }
    }

    private String translateEncode(Exception exception) {
        if (exception instanceof EncodeException encode) {
            if ("encode.error.invalidCodePoint".equals(encode.messageKey())) {
                return translator.t(encode.messageKey(), "part", encode.details());
            }
            String translated = translator.t(encode.messageKey());
            if (encode.details().isBlank() || translated.equals(encode.details())) {
                return translated;
            }
            return translated + " · " + encode.details();
        }
        return exception.getMessage() == null ? translator.t("encode.error.failed") : exception.getMessage();
    }

    private static VBox pane(Label title, TextArea area) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(12));
        title.getStyleClass().add("mt-muted");
        VBox.setVgrow(area, Priority.ALWAYS);
        box.getChildren().addAll(title, area);
        return box;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private record Pair(String left, String right) {
    }
}
