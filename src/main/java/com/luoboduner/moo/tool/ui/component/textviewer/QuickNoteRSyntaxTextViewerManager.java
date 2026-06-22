package com.luoboduner.moo.tool.ui.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.util.QuickNoteMarkdownUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义普通纯文本视图管理器
 */
public class QuickNoteRSyntaxTextViewerManager {

    public static Map<String, QuickNoteEditorPanel> viewMap = new HashMap<>();

    private QuickNoteEditorPanel currentEditorPanel;

    /**
     * 按 Vault 相对路径获取一个实例，若不存在则新建
     */
    public QuickNoteEditorPanel getEditorPanel(String relativePath) {
        QuickNoteEditorPanel editorPanel = viewMap.get(relativePath);
        if (editorPanel == null) {
            QuickNoteRSyntaxTextViewer plainTextViewer = new QuickNoteRSyntaxTextViewer();
            TQuickNote tQuickNote = QuickNoteVaultUtil.loadByPath(relativePath);
            if (tQuickNote == null) {
                tQuickNote = new TQuickNote();
                tQuickNote.setRelativePath(relativePath);
                tQuickNote.setContent("");
            }
            plainTextViewer.setText(StringUtils.defaultString(tQuickNote.getContent()));
            if (StringUtils.isNotEmpty(tQuickNote.getSyntax())) {
                plainTextViewer.setSyntaxEditingStyle(tQuickNote.getSyntax());
            }
            if (StringUtils.isNotEmpty(tQuickNote.getFontName()) && StringUtils.isNotEmpty(tQuickNote.getFontSize())) {
                Font font = FontUtils.getCompositeFont(tQuickNote.getFontName(), Font.PLAIN,
                        Integer.parseInt(tQuickNote.getFontSize()));
                plainTextViewer.setFont(font);
            }

            plainTextViewer.setCaretPosition(0);

            RTextScrollPane rTextScrollPane = new RTextScrollPane(plainTextViewer);
            rTextScrollPane.setMaximumSize(new Dimension(-1, -1));
            rTextScrollPane.setMinimumSize(new Dimension(-1, -1));

            updateGutter(rTextScrollPane);

            QuickNoteEditorPanel newEditorPanel = new QuickNoteEditorPanel(rTextScrollPane);
            newEditorPanel.setMarkdownPreviewEnabled(QuickNoteMarkdownUtil.isMarkdownSyntax(plainTextViewer.getSyntaxEditingStyle()));

            plainTextViewer.setOnContentChanged(() -> newEditorPanel.updatePreview(plainTextViewer.getText()));

            editorPanel = newEditorPanel;
            plainTextViewer.refreshConflictHighlights();
            String noteColor = tQuickNote.getColor();
            if (StringUtils.isEmpty(noteColor)) {
                noteColor = "default";
            }
            editorPanel.applyAccentColor(QuickNoteEditorPanel.resolveAccentColor(noteColor));

            viewMap.put(relativePath, editorPanel);
            currentEditorPanel = editorPanel;
        } else {
            currentEditorPanel = editorPanel;
        }
        return editorPanel;
    }

    public RTextScrollPane getRTextScrollPane(String relativePath) {
        return getEditorPanel(relativePath).getEditorScrollPane();
    }

    public void updateFont(String relativePath) {
        QuickNoteEditorPanel editorPanel = viewMap.get(relativePath);
        if (editorPanel != null) {
            QuickNoteRSyntaxTextViewer plainTextViewer =
                    (QuickNoteRSyntaxTextViewer) editorPanel.getEditorScrollPane().getTextArea();
            TQuickNote tQuickNote = QuickNoteVaultUtil.loadByPath(relativePath);
            if (tQuickNote != null
                    && StringUtils.isNotEmpty(tQuickNote.getFontName())
                    && StringUtils.isNotEmpty(tQuickNote.getFontSize())) {
                Font font = FontUtils.getCompositeFont(tQuickNote.getFontName(), Font.PLAIN,
                        Integer.parseInt(tQuickNote.getFontSize()));
                plainTextViewer.setFont(font);
            }
        }
    }

    public static void updateGutter(RTextScrollPane rTextScrollPane) {
        Color defaultBackground = App.mainFrame.getBackground();

        Gutter gutter = rTextScrollPane.getGutter();
        if (FlatLaf.isLafDark()) {
            gutter.setBorderColor(gutter.getLineNumberColor().darker());
        } else {
            gutter.setBorderColor(gutter.getLineNumberColor().brighter());
        }
        gutter.setBackground(defaultBackground);
        Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        gutter.setLineNumberFont(font);
        gutter.setBackground(UIManager.getColor("Editor.gutter.background"));
        gutter.setBorderColor(UIManager.getColor("Editor.gutter.borderColor"));
        gutter.setLineNumberColor(UIManager.getColor("Editor.gutter.lineNumberColor"));
    }

    public QuickNoteEditorPanel getCurrentEditorPanel() {
        return currentEditorPanel;
    }

    public RTextScrollPane getCurrentRTextScrollPane() {
        if (currentEditorPanel == null) {
            return null;
        }
        return currentEditorPanel.getEditorScrollPane();
    }

    public RSyntaxTextArea getCurrentRSyntaxTextArea() {
        if (currentEditorPanel == null) {
            return null;
        }
        return (RSyntaxTextArea) currentEditorPanel.getEditorScrollPane().getTextArea();
    }

    public String getCurrentText() {
        if (currentEditorPanel == null) {
            return null;
        }
        return currentEditorPanel.getEditorScrollPane().getTextArea().getText();
    }

    public boolean hasCurrentEditor() {
        return currentEditorPanel != null;
    }

    public String getTextByPath(String relativePath) {
        QuickNoteEditorPanel editorPanel = viewMap.get(relativePath);
        if (editorPanel != null) {
            return editorPanel.getEditorScrollPane().getTextArea().getText();
        }
        return null;
    }

    public void removeRTextScrollPane(String relativePath) {
        viewMap.remove(relativePath);
    }

}
