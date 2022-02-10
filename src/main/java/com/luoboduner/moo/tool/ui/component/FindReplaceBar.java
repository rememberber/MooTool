package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.StringUtils;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.RTextAreaHighlighter;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class FindReplaceBar {
    private JPanel findOptionPanel;
    private JTextField findField;
    private JTextField replaceField;
    private JButton doFindButton;
    private JButton replaceButton;
    private JLabel closeButton;
    private JCheckBox matchCaseToggleButton;
    private JCheckBox matchWholeWordToggleButton;
    private JCheckBox regexToggleButton;
    private JButton replaceAllButton;
    private JButton findPreviousButton;
    private JButton findNextButton;
    private JLabel matchesLabel;
    private JLabel replaceMatchesLabel;

    private final RSyntaxTextArea textArea;

    private SearchContext context;

    public FindReplaceBar(RSyntaxTextArea textArea) {
        super();
        this.textArea = textArea;

        $$$setupUI$$$();
        initComponents();

        findField.getDocument().addDocumentListener(new MarkAllUpdater());

        // find previous/next with UP/DOWN keys; focus editor with F12 key
        InputMap inputMap = findOptionPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "findPrevious");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "findNext");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "editorPageUp");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "editorPageDown");
        ActionMap actionMap = findOptionPanel.getActionMap();
        actionMap.put("findPrevious", new ConsumerAction(e -> findPrevious()));
        actionMap.put("findNext", new ConsumerAction(e -> findNext()));
        actionMap.put("editorPageUp", new ConsumerAction(e -> notifyEditorAction("page-up")));
        actionMap.put("editorPageDown", new ConsumerAction(e -> notifyEditorAction("page-down")));

        findPreviousButton.setIcon(new FlatSVGIcon("icon/up.svg"));
        findNextButton.setIcon(new FlatSVGIcon("icon/down.svg"));
        closeButton.setIcon(new FlatSVGIcon("icon/close.svg"));

        SearchContext context = new SearchContext();
        context.setSearchWrap(true);
        setSearchContext(context);
    }

    SearchContext getSearchContext() {
        return context;
    }

    void setSearchContext(SearchContext context) {
        this.context = context;

        findField.setText(context.getSearchFor());
        replaceField.setText(context.getReplaceWith());
        matchCaseToggleButton.setSelected(context.getMatchCase());
        matchWholeWordToggleButton.setSelected(context.getWholeWord());
        regexToggleButton.setSelected(context.isRegularExpression());
    }

    private void findNext() {
        context.setSearchForward(true);
        find();
    }

    private void findPrevious() {
        context.setSearchForward(false);
        find();
    }

    private void find() {
        findOrMarkAll(true);
    }

    void markAll() {
        findOrMarkAll(false);
    }

    private void findOrMarkAll(boolean find) {
        // update search context
        String searchFor = findField.getText();
        context.setSearchFor(searchFor);

        // find
        SearchResult result = find
                ? SearchEngine.find(textArea, context)
                : SearchEngine.markAll(textArea, context);

        // select (and scroll to) match near caret
        if (!find && result.getMarkedCount() > 0)
            selectMatchNearCaret();

        // update matches info label
        updateMatchesLabel(result, false);

        // enabled/disable
        boolean findEnabled = !StringUtils.isEmpty(searchFor);
        findPreviousButton.setEnabled(findEnabled);
        findNextButton.setEnabled(findEnabled);
    }

    private void matchCaseChanged() {
        context.setMatchCase(matchCaseToggleButton.isSelected());
        markAll();
    }

    private void matchWholeWordChanged() {
        context.setWholeWord(matchWholeWordToggleButton.isSelected());
        markAll();
    }

    private void regexChanged() {
        context.setRegularExpression(regexToggleButton.isSelected());
        markAll();
    }

    private void replace() {
        // update search context
        context.setSearchFor(findField.getText());
        context.setReplaceWith(replaceField.getText());

        // replace
        SearchResult result = SearchEngine.replace(textArea, context);

        // update matches info labels
        updateMatchesLabel(result, true);
    }

    private void replaceAll() {
        // update search context
        context.setSearchFor(findField.getText());
        context.setReplaceWith(replaceField.getText());

        // make sure that search wrap is disabled because otherwise it is easy
        // to have endeless loop when replacing e.g. "a" with "aa"
        boolean oldSearchWrap = context.getSearchWrap();
        context.setSearchWrap(false);

        // replace all
        SearchResult result = SearchEngine.replaceAll(textArea, context);

        // restore search wrap
        context.setSearchWrap(oldSearchWrap);

        // update matches info labels
        updateMatchesLabel(result, true);
    }

    private void selectMatchNearCaret() {
        RTextAreaHighlighter highlighter = (RTextAreaHighlighter) textArea.getHighlighter();
        if (highlighter == null)
            return;

        List<DocumentRange> ranges = highlighter.getMarkAllHighlightRanges();
        if (ranges.isEmpty())
            return;

        DocumentRange selectRange = null;
        if (ranges.size() > 1) {
            int selStart = textArea.getSelectionStart();
            for (DocumentRange range : ranges) {
                if (range.getEndOffset() >= selStart) {
                    selectRange = range;
                    break;
                }
            }
        }
        if (selectRange == null)
            selectRange = ranges.get(0);

        RSyntaxUtilities.selectAndPossiblyCenter(textArea, selectRange, true);
    }

    private void updateMatchesLabel(SearchResult result, boolean replace) {
        matchesLabel.setText(String.valueOf(result.getMarkedCount()));
        replaceMatchesLabel.setText(String.valueOf(replace ? result.getCount() : 0));
    }

    private void notifyEditorAction(String actionKey) {
        Action action = textArea.getActionMap().get(actionKey);
        if (action != null)
            action.actionPerformed(new ActionEvent(textArea, ActionEvent.ACTION_PERFORMED, null));
    }

    private void close() {
        QuickNoteForm.getInstance().getFindReplacePanel().setVisible(false);
    }

    private void initComponents() {
        findField.addActionListener(e -> find());


        //---- findPreviousButton ----
        findPreviousButton.setToolTipText("Previous Occurrence");
        findPreviousButton.addActionListener(e -> findPrevious());

        //---- findNextButton ----
        findNextButton.setToolTipText("Next Occurrence");
        findNextButton.addActionListener(e -> findNext());

        //---- matchCaseToggleButton ----
        matchCaseToggleButton.setToolTipText("Match Case");
        matchCaseToggleButton.addActionListener(e -> matchCaseChanged());

        //---- matchWholeWordToggleButton ----
        matchWholeWordToggleButton.setToolTipText("Match Whole Word");
        matchWholeWordToggleButton.addActionListener(e -> matchWholeWordChanged());

        //---- regexToggleButton ----
        regexToggleButton.setToolTipText("Regex");
        regexToggleButton.addActionListener(e -> regexChanged());

        //---- closeButton ----
        closeButton.setToolTipText("Close");
        closeButton.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                close();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });


        //---- replaceButton ----
        replaceButton.setMnemonic('E');
        replaceButton.addActionListener(e -> replace());

        //---- replaceAllButton ----
        replaceAllButton.setMnemonic('A');
        replaceAllButton.addActionListener(e -> replaceAll());

    }

    private class MarkAllUpdater
            implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            markAll();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            markAll();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            markAll();
        }
    }

    //---- class ConsumerAction -----------------------------------------------

    private static class ConsumerAction
            extends AbstractAction {
        private final Consumer<ActionEvent> consumer;

        ConsumerAction(Consumer<ActionEvent> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            consumer.accept(e);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        findOptionPanel.setLayout(new GridLayoutManager(3, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(findOptionPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        findField = new JTextField();
        findOptionPanel.add(findField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        replaceField = new JTextField();
        findOptionPanel.add(replaceField, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        findOptionPanel.add(spacer1, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        findOptionPanel.add(spacer2, new GridConstraints(2, 3, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        doFindButton = new JButton();
        doFindButton.setText("查找");
        findOptionPanel.add(doFindButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        replaceButton = new JButton();
        replaceButton.setText("替换");
        findOptionPanel.add(replaceButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        closeButton = new JLabel();
        closeButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove_dark.png")));
        closeButton.setText("");
        findOptionPanel.add(closeButton, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 8, new Insets(0, 0, 0, 0), -1, -1));
        findOptionPanel.add(panel2, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        regexToggleButton = new JCheckBox();
        regexToggleButton.setText("使用正则");
        panel2.add(regexToggleButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        matchCaseToggleButton = new JCheckBox();
        matchCaseToggleButton.setText("区分大小写");
        panel2.add(matchCaseToggleButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        matchWholeWordToggleButton = new JCheckBox();
        matchWholeWordToggleButton.setText("全词匹配");
        panel2.add(matchWholeWordToggleButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findPreviousButton = new JButton();
        findPreviousButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        findPreviousButton.setText("");
        panel2.add(findPreviousButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findNextButton = new JButton();
        findNextButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        findNextButton.setText("");
        panel2.add(findNextButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("共找到:");
        panel2.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        matchesLabel = new JLabel();
        matchesLabel.setText("0");
        panel2.add(matchesLabel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        findOptionPanel.add(panel3, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        replaceAllButton = new JButton();
        replaceAllButton.setText("替换全部");
        panel3.add(replaceAllButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("已替换:");
        panel3.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        replaceMatchesLabel = new JLabel();
        replaceMatchesLabel.setText("0");
        panel3.add(replaceMatchesLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel3.add(spacer4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        findOptionPanel.add(panel4, new GridConstraints(0, 0, 1, 6, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel4.add(separator1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private void createUIComponents() {
        findOptionPanel = new JPanel();
    }
}
