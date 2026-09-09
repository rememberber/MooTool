package com.rememberber.mootool.nextfx.features.json;

import com.rememberber.mootool.nextfx.infrastructure.JsonVaultStore;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class JsonVaultPanel extends VBox {

    private final Translator translator;
    private final JsonVaultStore vault;
    private final TreeView<JsonVaultStore.Node> tree = new TreeView<>();
    private final Label selected = new Label();
    private final BiConsumer<String, String> onOpen;
    private String selectedPath = "";

    public JsonVaultPanel(Translator translator, JsonVaultStore vault, BiConsumer<String, String> onOpen) {
        this.translator = translator;
        this.vault = vault;
        this.onOpen = onOpen;
        setSpacing(8);
        setPadding(new Insets(12));
        setPrefWidth(240);
        getStyleClass().add("mt-panel");
        Label title = new Label(translator.t("json.vault.title"));
        title.getStyleClass().add("mt-section-title");
        HBox actions = new HBox(6);
        actions.getChildren().addAll(
                small(translator.t("json.vault.newFile"), this::newFile),
                small(translator.t("json.vault.newFolder"), this::newFolder),
                small(translator.t("json.vault.refresh"), this::refresh)
        );
        selected.getStyleClass().add("mt-muted");
        selected.setWrapText(true);
        tree.setShowRoot(false);
        tree.setCellFactory(view -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(JsonVaultStore.Node item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + (item.directory() ? "/" : ""));
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item == null || item.getValue() == null || item.getValue().directory()) {
                return;
            }
            selectedPath = item.getValue().relativePath();
            selected.setText(selectedPath);
            onOpen.accept(selectedPath, vault.read(selectedPath));
        });
        VBox.setVgrow(tree, Priority.ALWAYS);
        Label git = new Label(translator.t("json.vault.gitPending"));
        git.setWrapText(true);
        git.getStyleClass().add("mt-muted");
        getChildren().addAll(title, actions, tree, selected, git);
        refresh();
    }

    public String selectedPath() {
        return selectedPath;
    }

    public void setSelectedPath(String path) {
        this.selectedPath = path == null ? "" : path;
        selected.setText(this.selectedPath.isBlank() ? translator.t("json.vault.unsaved") : this.selectedPath);
    }

    public void markDirty(boolean dirty) {
        if (selectedPath.isBlank()) {
            selected.setText(translator.t("json.vault.unsaved"));
            return;
        }
        selected.setText(dirty ? selectedPath + " *" : selectedPath);
    }

    public void refresh() {
        TreeItem<JsonVaultStore.Node> root = new TreeItem<>(new JsonVaultStore.Node("", "", true, java.time.Instant.EPOCH, List.of()));
        for (JsonVaultStore.Node node : vault.list()) {
            root.getChildren().add(toItem(node));
        }
        tree.setRoot(root);
        expand(root);
        setSelectedPath(selectedPath);
    }

    private void newFile() {
        ask(translator.t("json.vault.newFilePrompt")).ifPresent(name -> {
            String parent = selectedDirectory();
            String created = vault.createFile(parent, name);
            refresh();
            onOpen.accept(created, vault.read(created));
            setSelectedPath(created);
        });
    }

    private void newFolder() {
        ask(translator.t("json.vault.newFolderPrompt")).ifPresent(name -> {
            vault.createFolder(selectedDirectory(), name);
            refresh();
        });
    }

    private String selectedDirectory() {
        TreeItem<JsonVaultStore.Node> item = tree.getSelectionModel().getSelectedItem();
        if (item == null || item.getValue() == null) {
            return "";
        }
        JsonVaultStore.Node node = item.getValue();
        return node.directory() ? node.relativePath() : parentOf(node.relativePath());
    }

    private static String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private Optional<String> ask(String title) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText(title);
        dialog.setTitle(title);
        return dialog.showAndWait().map(String::trim).filter(value -> !value.isEmpty());
    }

    private TreeItem<JsonVaultStore.Node> toItem(JsonVaultStore.Node node) {
        TreeItem<JsonVaultStore.Node> item = new TreeItem<>(node);
        for (JsonVaultStore.Node child : node.children()) {
            item.getChildren().add(toItem(child));
        }
        return item;
    }

    private void expand(TreeItem<JsonVaultStore.Node> item) {
        item.setExpanded(true);
        for (TreeItem<JsonVaultStore.Node> child : item.getChildren()) {
            if (child.getValue() != null && child.getValue().directory()) {
                expand(child);
            }
        }
    }

    private Button small(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }
}
