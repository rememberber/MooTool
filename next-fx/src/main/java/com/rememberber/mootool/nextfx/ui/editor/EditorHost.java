package com.rememberber.mootool.nextfx.ui.editor;

import com.rememberber.mootool.nextfx.application.DocumentSession;
import javafx.scene.Node;

import java.util.function.Consumer;

public interface EditorHost extends AutoCloseable {

    Node view();

    EditorSnapshot snapshot();

    void openDocument(DocumentSession document);

    void setText(String text, String origin);

    String text();

    void applyColumnInsert(int startLine, int endLine, int column, String insertion);

    void applyColumnDelete(int startLine, int endLine, int startColumn, int endColumn);

    void execute(EditorCommand command);

    void setWrap(boolean wrap);

    void setFont(String family, double size);

    void setTheme(boolean dark);

    void focusEditor();

    void onChange(Consumer<EditorSnapshot> listener);

    @Override
    void close();
}
