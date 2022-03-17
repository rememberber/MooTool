package com.luoboduner.moo.tool.ui.listener.func;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.RegexForm;
import com.luoboduner.moo.tool.ui.frame.FavoriteRegexFrame;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.RTextAreaHighlighter;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;

public class RegexListener {

    private static SearchContext context;

    public static void addListeners() {
        RegexForm regexForm = RegexForm.getInstance();
        context = new SearchContext();
        context.setSearchWrap(true);
        context.setRegularExpression(true);

        regexForm.getRegexTextField().getDocument().addDocumentListener(new MarkAllUpdater());
        regexForm.getMatchTestButton().addActionListener(e -> markAll());

        regexForm.getFavoriteBookButton().addActionListener(e -> FavoriteRegexFrame.showWindow());
    }


    private static class MarkAllUpdater
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

    private static void markAll() {
        RegexForm regexForm = RegexForm.getInstance();
        findOrMarkAll(false);

        App.config.setRegexText(regexForm.getRegexTextField().getText());
        App.config.save();

        RegexForm.saveContent();
    }

    private static void findOrMarkAll(boolean find) {
        RegexForm regexForm = RegexForm.getInstance();
        String searchFor = regexForm.getRegexTextField().getText();
        context.setSearchFor(searchFor);
        context.setMarkAll(true);
        context.setMatchCase(true);

        // find
        SearchResult result = find
                ? SearchEngine.find(regexForm.getTextArea(), context)
                : SearchEngine.markAll(regexForm.getTextArea(), context);

        // select (and scroll to) match near caret
        if (!find && result.getMarkedCount() > 0)
            selectMatchNearCaret();

        // update matches info label
        updateMatchesLabel(result, false);
    }

    private static void selectMatchNearCaret() {
        RegexForm regexForm = RegexForm.getInstance();
        RTextAreaHighlighter highlighter = (RTextAreaHighlighter) regexForm.getTextArea().getHighlighter();
        if (highlighter == null)
            return;

        List<DocumentRange> ranges = highlighter.getMarkAllHighlightRanges();
        if (ranges.isEmpty())
            return;

        DocumentRange selectRange = null;
        if (ranges.size() > 1) {
            int selStart = regexForm.getTextArea().getSelectionStart();
            for (DocumentRange range : ranges) {
                if (range.getEndOffset() >= selStart) {
                    selectRange = range;
                    break;
                }
            }
        }
        if (selectRange == null)
            selectRange = ranges.get(0);

        RSyntaxUtilities.selectAndPossiblyCenter(regexForm.getTextArea(), selectRange, true);
    }

    private static void updateMatchesLabel(SearchResult result, boolean replace) {
        RegexForm regexForm = RegexForm.getInstance();

        regexForm.getMatchesLabel().setText(String.valueOf(result.getMarkedCount()));
    }

}
