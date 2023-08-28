package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.FontUtils;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.util.MybatisUtil;
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
public class QuickNoteSyntaxTextViewerManager {

    public static Map<String, RTextScrollPane> viewMap = new HashMap<>();

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    private RTextScrollPane currentRTextScrollPane;

    /**
     * 按名称获取一个实例，若不存在则新建
     *
     * @return
     */
    public RTextScrollPane getRTextScrollPane(String name) {
        RTextScrollPane rTextScrollPane = viewMap.get(name);
        if (rTextScrollPane == null) {
            QuickNoteSyntaxTextViewer plainTextViewer = new QuickNoteSyntaxTextViewer();
            TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
            plainTextViewer.setText(tQuickNote.getContent());
            if (StringUtils.isNotEmpty(tQuickNote.getSyntax())) {
                plainTextViewer.setSyntaxEditingStyle(tQuickNote.getSyntax());
            }
            if (StringUtils.isNotEmpty(tQuickNote.getFontName()) && StringUtils.isNotEmpty(tQuickNote.getFontSize())) {
                Font font;
                if (FlatJetBrainsMonoFont.FAMILY.equals(tQuickNote.getFontName())) {
                    font = FontUtils.getCompositeFont(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, Integer.parseInt(tQuickNote.getFontSize()));
                } else {
                    font = new Font(tQuickNote.getFontName(), Font.PLAIN, Integer.parseInt(tQuickNote.getFontSize()));
                }
                plainTextViewer.setFont(font);
            }

            plainTextViewer.setCaretPosition(0);

            rTextScrollPane = new RTextScrollPane(plainTextViewer);
            rTextScrollPane.setMaximumSize(new Dimension(-1, -1));
            rTextScrollPane.setMinimumSize(new Dimension(-1, -1));

            updateGutter(rTextScrollPane);

            viewMap.put(name, rTextScrollPane);
            currentRTextScrollPane = rTextScrollPane;
        }
        return rTextScrollPane;
    }

    public void updateFont(String name){
        RTextScrollPane rTextScrollPane = viewMap.get(name);
        if (rTextScrollPane != null) {
            QuickNoteSyntaxTextViewer plainTextViewer = (QuickNoteSyntaxTextViewer) rTextScrollPane.getTextArea();
            TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
            if (StringUtils.isNotEmpty(tQuickNote.getFontName()) && StringUtils.isNotEmpty(tQuickNote.getFontSize())) {
                Font font;
                if (FlatJetBrainsMonoFont.FAMILY.equals(tQuickNote.getFontName())) {
                    font = FontUtils.getCompositeFont(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, Integer.parseInt(tQuickNote.getFontSize()));
                } else {
                    font = new Font(tQuickNote.getFontName(), Font.PLAIN, Integer.parseInt(tQuickNote.getFontSize()));
                }
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

    public RTextScrollPane getCurrentRTextScrollPane() {
        return currentRTextScrollPane;
    }

    public RSyntaxTextArea getCurrentRSyntaxTextArea() {
        return (RSyntaxTextArea) currentRTextScrollPane.getTextArea();
    }

    public String getCurrentText() {
        return currentRTextScrollPane.getTextArea().getText();
    }

    public String getTextByName(String name) {
        if (viewMap.get(name) != null) {
            return viewMap.get(name).getTextArea().getText();
        }
        return null;
    }

    public void removeRTextScrollPane(String name) {
        viewMap.remove(name);
    }

}
