package com.luoboduner.moo.tool.ui.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.QuickNoteMarkdownUtil;
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

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    private QuickNoteEditorPanel currentEditorPanel;

    /**
     * 按名称获取一个实例，若不存在则新建
     */
    public QuickNoteEditorPanel getEditorPanel(String name) {
        QuickNoteEditorPanel editorPanel = viewMap.get(name);
        if (editorPanel == null) {
            QuickNoteRSyntaxTextViewer plainTextViewer = new QuickNoteRSyntaxTextViewer();
            TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
            plainTextViewer.setText(tQuickNote.getContent());
            if (StringUtils.isNotEmpty(tQuickNote.getSyntax())) {
                plainTextViewer.setSyntaxEditingStyle(tQuickNote.getSyntax());
            }
            if (StringUtils.isNotEmpty(tQuickNote.getFontName()) && StringUtils.isNotEmpty(tQuickNote.getFontSize())) {
                Font font = FontUtils.getCompositeFont(tQuickNote.getFontName(), Font.PLAIN, Integer.parseInt(tQuickNote.getFontSize()));
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
            String noteColor = tQuickNote.getColor();
            if (StringUtils.isEmpty(noteColor)) {
                noteColor = "default";
            }
            editorPanel.applyAccentColor(QuickNoteEditorPanel.resolveAccentColor(noteColor));

            viewMap.put(name, editorPanel);
            currentEditorPanel = editorPanel;
        } else {
            currentEditorPanel = editorPanel;
        }
        return editorPanel;
    }

    public RTextScrollPane getRTextScrollPane(String name) {
        return getEditorPanel(name).getEditorScrollPane();
    }

    public void updateFont(String name) {
        QuickNoteEditorPanel editorPanel = viewMap.get(name);
        if (editorPanel != null) {
            QuickNoteRSyntaxTextViewer plainTextViewer = (QuickNoteRSyntaxTextViewer) editorPanel.getEditorScrollPane().getTextArea();
            TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
            if (StringUtils.isNotEmpty(tQuickNote.getFontName()) && StringUtils.isNotEmpty(tQuickNote.getFontSize())) {
                Font font = FontUtils.getCompositeFont(tQuickNote.getFontName(), Font.PLAIN, Integer.parseInt(tQuickNote.getFontSize()));
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
        return currentEditorPanel.getEditorScrollPane();
    }

    public RSyntaxTextArea getCurrentRSyntaxTextArea() {
        return (RSyntaxTextArea) currentEditorPanel.getEditorScrollPane().getTextArea();
    }

    public String getCurrentText() {
        return currentEditorPanel.getEditorScrollPane().getTextArea().getText();
    }

    public String getTextByName(String name) {
        QuickNoteEditorPanel editorPanel = viewMap.get(name);
        if (editorPanel != null) {
            return editorPanel.getEditorScrollPane().getTextArea().getText();
        }
        return null;
    }

    public void removeRTextScrollPane(String name) {
        viewMap.remove(name);
    }

}
