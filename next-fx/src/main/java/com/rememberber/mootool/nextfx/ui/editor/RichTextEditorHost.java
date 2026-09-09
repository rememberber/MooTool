package com.rememberber.mootool.nextfx.ui.editor;

import com.rememberber.mootool.nextfx.application.DocumentSession;
import com.rememberber.mootool.nextfx.domain.editor.ColumnEdits;
import com.rememberber.mootool.nextfx.domain.editor.FindMatch;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RichTextEditorHost implements EditorHost {

    private final CodeArea area = new CodeArea();
    private final VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(area);
    private final StackPane root = new StackPane(scroll);
    private final List<Consumer<EditorSnapshot>> listeners = new ArrayList<>();
    private DocumentSession document;
    private String origin;
    private boolean applying;
    private Integer columnAnchorLine;
    private Integer columnAnchorColumn;

    public RichTextEditorHost() {
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        area.setWrapText(true);
        area.getStyleClass().add("mt-code-area");
        root.getStyleClass().add("mt-editor-host");
        StackPane.setMargin(scroll, Insets.EMPTY);
        area.plainTextChanges().subscribe(change -> {
            if (applying || document == null) {
                return;
            }
            document.replaceText(area.getText(), area.getCaretPosition(), area.getAnchor());
            publish();
        });
        area.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        area.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isAltDown()) {
                columnAnchorLine = area.getCurrentParagraph();
                columnAnchorColumn = ColumnEdits.visualColumn(
                        area.getParagraph(area.getCurrentParagraph()).getText(),
                        area.getCaretColumn(),
                        4
                );
            } else {
                columnAnchorLine = null;
                columnAnchorColumn = null;
            }
        });
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public EditorSnapshot snapshot() {
        String documentId = document == null ? "" : document.documentId();
        long revision = document == null ? 0 : document.revision();
        return new EditorSnapshot(
                documentId,
                revision,
                area.getText(),
                area.getCaretPosition(),
                area.getAnchor(),
                area.firstVisibleParToAllParIndex(),
                area.getEstimatedScrollX(),
                area.isWrapText()
        );
    }

    @Override
    public void openDocument(DocumentSession document) {
        this.document = document;
        applying = true;
        try {
            area.replaceText(document.text());
            int caret = Math.min(document.caret(), area.getLength());
            int anchor = Math.min(document.anchor(), area.getLength());
            area.selectRange(anchor, caret);
            area.setWrapText(document.wrap());
            area.showParagraphAtTop(document.firstVisibleParagraph());
        } finally {
            applying = false;
        }
        area.requestFollowCaret();
    }

    @Override
    public void setText(String text, String origin) {
        this.origin = origin;
        applying = true;
        try {
            int caret = area.getCaretPosition();
            area.replaceText(text == null ? "" : text);
            int next = Math.min(caret, area.getLength());
            area.moveTo(next);
            if (document != null) {
                document.replaceText(area.getText(), next, next);
            }
        } finally {
            applying = false;
            this.origin = null;
        }
        publish();
    }

    @Override
    public String text() {
        return area.getText();
    }

    @Override
    public void applyColumnInsert(int startLine, int endLine, int column, String insertion) {
        String next = ColumnEdits.insert(area.getText(), startLine, endLine, column, insertion, 4);
        setText(next, "column-insert");
    }

    @Override
    public void applyColumnDelete(int startLine, int endLine, int startColumn, int endColumn) {
        String next = ColumnEdits.delete(area.getText(), startLine, endLine, startColumn, endColumn, 4);
        setText(next, "column-delete");
    }

    @Override
    public void execute(EditorCommand command) {
        switch (command) {
            case UNDO -> area.undo();
            case REDO -> area.redo();
            case SELECT_ALL -> area.selectAll();
            case FOCUS -> focusEditor();
        }
    }

    @Override
    public void setWrap(boolean wrap) {
        area.setWrapText(wrap);
        if (document != null) {
            document.setWrap(wrap);
        }
    }

    @Override
    public void setFont(String family, double size) {
        String safeFamily = family == null || family.isBlank() ? "Menlo" : family;
        area.setStyle("-fx-font-family: '" + safeFamily.replace("'", "") + "'; -fx-font-size: " + size + "px;");
    }

    @Override
    public void setTheme(boolean dark) {
        area.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("dark"), dark);
    }

    @Override
    public void focusEditor() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(area::requestFocus);
            return;
        }
        area.requestFocus();
    }

    @Override
    public void onChange(Consumer<EditorSnapshot> listener) {
        listeners.add(listener);
    }

    @Override
    public void close() {
        listeners.clear();
        document = null;
    }

    public void reveal(FindMatch match) {
        if (match == null) {
            return;
        }
        area.selectRange(match.start(), match.end());
        area.requestFollowCaret();
    }

    public boolean canUndo() {
        return area.isUndoAvailable();
    }

    public boolean canRedo() {
        return area.isRedoAvailable();
    }

    private void onKeyPressed(KeyEvent event) {
        if (columnAnchorLine == null || columnAnchorColumn == null || !event.isAltDown()) {
            return;
        }
        if (event.getCode() == KeyCode.BACK_SPACE) {
            applyColumnDelete(
                    columnAnchorLine,
                    area.getCurrentParagraph(),
                    Math.max(0, columnAnchorColumn - 1),
                    columnAnchorColumn
            );
            columnAnchorColumn = Math.max(0, columnAnchorColumn - 1);
            event.consume();
        } else if (event.getText() != null && !event.getText().isEmpty() && !event.isMetaDown() && !event.isControlDown()) {
            applyColumnInsert(columnAnchorLine, area.getCurrentParagraph(), columnAnchorColumn, event.getText());
            columnAnchorColumn += event.getText().length();
            event.consume();
        }
    }

    private void publish() {
        EditorSnapshot snapshot = snapshot();
        for (Consumer<EditorSnapshot> listener : listeners) {
            listener.accept(snapshot);
        }
    }
}
