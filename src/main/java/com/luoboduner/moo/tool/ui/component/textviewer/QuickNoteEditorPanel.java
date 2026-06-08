package com.luoboduner.moo.tool.ui.component.textviewer;

import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * 随手记编辑器容器，支持 Markdown 预览分栏
 */
public class QuickNoteEditorPanel extends JPanel {

    private final JSplitPane splitPane;
    private final RTextScrollPane editorScrollPane;
    private final QuickNoteMarkdownPreviewPane previewPane;
    private boolean markdownPreviewEnabled;

    public QuickNoteEditorPanel(RTextScrollPane editorScrollPane) {
        this.editorScrollPane = editorScrollPane;
        this.previewPane = new QuickNoteMarkdownPreviewPane();
        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScrollPane, previewPane);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(0.5);
        setLayout(new BorderLayout());
        add(editorScrollPane, BorderLayout.CENTER);
    }

    public void setMarkdownPreviewEnabled(boolean enabled) {
        if (this.markdownPreviewEnabled == enabled) {
            if (enabled) {
                previewPane.updateContent(editorScrollPane.getTextArea().getText());
            }
            return;
        }
        this.markdownPreviewEnabled = enabled;
        removeAll();
        if (enabled) {
            splitPane.setLeftComponent(editorScrollPane);
            splitPane.setRightComponent(previewPane);
            add(splitPane, BorderLayout.CENTER);
            previewPane.updateContent(editorScrollPane.getTextArea().getText());
            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
        } else {
            add(editorScrollPane, BorderLayout.CENTER);
            previewPane.reset();
        }
        revalidate();
        repaint();
    }

    public void updatePreview(String markdown) {
        if (markdownPreviewEnabled) {
            previewPane.updateContent(markdown);
        }
    }

    public void updatePreviewTheme() {
        if (markdownPreviewEnabled) {
            previewPane.refreshTheme(editorScrollPane.getTextArea().getText());
        }
    }

    public boolean isMarkdownPreviewEnabled() {
        return markdownPreviewEnabled;
    }

    public RTextScrollPane getEditorScrollPane() {
        return editorScrollPane;
    }
}
