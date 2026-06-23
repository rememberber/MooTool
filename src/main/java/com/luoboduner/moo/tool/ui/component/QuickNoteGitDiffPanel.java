package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.bean.textdiff.UIDiff;
import com.luoboduner.moo.tool.util.I18n;

import javax.swing.*;
import java.awt.*;

/**
 * Git 面板底部 diff 区域：左右全文对比，或单栏摘要。
 */
public class QuickNoteGitDiffPanel extends JPanel {

    private static final String CARD_SIDE_BY_SIDE = "sideBySide";
    private static final String CARD_SUMMARY = "summary";

    private final JTextArea beforeArea = createTextArea();
    private final JTextArea afterArea = createTextArea();
    private final JTextArea summaryArea = createTextArea();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JSplitPane splitPane;
    private final JPanel leftWrap = new JPanel(new BorderLayout());
    private final JPanel rightWrap = new JPanel(new BorderLayout());

    private boolean syncingScroll;
    private String lastOldText;
    private String lastNewText;
    private UIDiff lastUiDiff;

    public QuickNoteGitDiffPanel() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(100, 200));

        JScrollPane leftScroll = new JScrollPane(beforeArea);
        JScrollPane rightScroll = new JScrollPane(afterArea);
        setupScrollSync(leftScroll, rightScroll);

        leftWrap.add(leftScroll, BorderLayout.CENTER);
        rightWrap.add(rightScroll, BorderLayout.CENTER);
        updatePanelTitles();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftWrap, rightWrap);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(splitPane, BorderLayout.CENTER);

        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        cardPanel.add(sidePanel, CARD_SIDE_BY_SIDE);
        cardPanel.add(summaryScroll, CARD_SUMMARY);
        add(cardPanel, BorderLayout.CENTER);
        cards.show(cardPanel, CARD_SUMMARY);
    }

    public void renderSideBySide(String oldText, String newText, UIDiff uiDiff) {
        lastOldText = oldText == null ? "" : oldText;
        lastNewText = newText == null ? "" : newText;
        lastUiDiff = uiDiff;
        cards.show(cardPanel, CARD_SIDE_BY_SIDE);
        QuickNoteUnifiedDiffRenderer.renderSideBySide(beforeArea, afterArea, lastOldText, lastNewText, uiDiff);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
    }

    public void renderPlain(String text) {
        lastOldText = null;
        lastNewText = null;
        lastUiDiff = null;
        cards.show(cardPanel, CARD_SUMMARY);
        QuickNoteUnifiedDiffRenderer.renderPlain(summaryArea, text);
    }

    public void clear() {
        renderPlain("");
    }

    public void applyTheme() {
        styleTextArea(beforeArea);
        styleTextArea(afterArea);
        styleTextArea(summaryArea);
        if (lastUiDiff != null && lastOldText != null && lastNewText != null) {
            QuickNoteUnifiedDiffRenderer.renderSideBySide(beforeArea, afterArea, lastOldText, lastNewText, lastUiDiff);
        }
    }

    public void updatePanelTitles() {
        leftWrap.setBorder(BorderFactory.createTitledBorder(I18n.get("quickNote.git.diff.before")));
        rightWrap.setBorder(BorderFactory.createTitledBorder(I18n.get("quickNote.git.diff.after")));
    }

    private static JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        return area;
    }

    private void setupScrollSync(JScrollPane leftScroll, JScrollPane rightScroll) {
        JScrollBar leftBar = leftScroll.getVerticalScrollBar();
        JScrollBar rightBar = rightScroll.getVerticalScrollBar();
        if (leftBar == null || rightBar == null) {
            return;
        }
        leftBar.addAdjustmentListener(e -> {
            if (syncingScroll) {
                return;
            }
            syncingScroll = true;
            rightBar.setValue(e.getValue());
            syncingScroll = false;
        });
        rightBar.addAdjustmentListener(e -> {
            if (syncingScroll) {
                return;
            }
            syncingScroll = true;
            leftBar.setValue(e.getValue());
            syncingScroll = false;
        });
    }

    private static void styleTextArea(JTextArea area) {
        Color background = firstUiColor("TextArea.background", "Editor.background");
        Color foreground = firstUiColor("TextArea.foreground", "Editor.foreground");
        if (background != null) {
            area.setBackground(background);
            area.setOpaque(true);
            Container parent = area.getParent();
            if (parent instanceof JViewport viewport) {
                viewport.setBackground(background);
            }
        }
        if (foreground != null) {
            area.setForeground(foreground);
        }
        Color caret = UIManager.getColor("TextArea.caretForeground");
        if (caret == null) {
            caret = UIManager.getColor("Editor.caretColor");
        }
        if (caret != null) {
            area.setCaretColor(caret);
        }
    }

    private static Color firstUiColor(String primaryKey, String fallbackKey) {
        Color color = UIManager.getColor(primaryKey);
        if (color == null) {
            color = UIManager.getColor(fallbackKey);
        }
        return color;
    }
}
