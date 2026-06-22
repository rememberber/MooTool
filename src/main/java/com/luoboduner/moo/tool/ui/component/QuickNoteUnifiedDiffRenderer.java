package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.bean.textdiff.Span;
import com.luoboduner.moo.tool.bean.textdiff.TextDiffSegment;
import com.luoboduner.moo.tool.bean.textdiff.UIDiff;
import com.luoboduner.moo.tool.bean.textdiff.UnifiedView;
import com.luoboduner.moo.tool.enums.DiffTypeEnum;
import com.luoboduner.moo.tool.enums.UnifiedSpanTypeEnum;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;

/**
 * 在只读文本区域渲染带行级/字符级高亮的 unified diff。
 */
public final class QuickNoteUnifiedDiffRenderer {

    private QuickNoteUnifiedDiffRenderer() {
    }

    public static void render(JTextArea area, UnifiedView view) {
        clearHighlights(area);
        if (view == null) {
            area.setText("");
            return;
        }
        area.setText(view.text());
        area.setCaretPosition(0);
        applySpans(area, view);
    }

    public static void renderPlain(JTextArea area, String text) {
        clearHighlights(area);
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
    }

    public static void reapplyHighlights(JTextArea area, UnifiedView view) {
        if (view == null || area.getDocument().getLength() == 0) {
            return;
        }
        clearHighlights(area);
        applySpans(area, view);
    }

    public static void clearHighlights(JTextArea area) {
        area.getHighlighter().removeAllHighlights();
    }

    public static void renderSideBySide(JTextArea left, JTextArea right, String oldText, String newText, UIDiff uiDiff) {
        clearHighlights(left);
        clearHighlights(right);
        left.setText(oldText == null ? "" : oldText);
        right.setText(newText == null ? "" : newText);
        left.setCaretPosition(0);
        right.setCaretPosition(0);
        applySideBySideHighlights(left, right, uiDiff);
    }

    private static void applySideBySideHighlights(JTextArea left, JTextArea right, UIDiff uiDiff) {
        if (uiDiff == null || uiDiff.segments() == null) {
            return;
        }
        DiffPainters painters = DiffPainters.current();
        for (TextDiffSegment segment : uiDiff.segments()) {
            if (segment.type() == DiffTypeEnum.DELETE) {
                if (segment.leftStart() >= 0 && segment.leftEnd() >= 0) {
                    safeAddHighlight(left, segment.leftStart(), segment.leftEnd(), painters.delPainter());
                    safeAddLineHighlights(left, segment.leftStart(), segment.leftEnd(), painters.delLinePainter());
                }
            } else if (segment.type() == DiffTypeEnum.INSERT) {
                if (segment.rightStart() >= 0 && segment.rightEnd() >= 0) {
                    safeAddHighlight(right, segment.rightStart(), segment.rightEnd(), painters.insPainter());
                    safeAddLineHighlights(right, segment.rightStart(), segment.rightEnd(), painters.insLinePainter());
                }
            } else if (segment.type() == DiffTypeEnum.CHANGE) {
                if (segment.leftStart() >= 0 && segment.leftEnd() >= 0) {
                    safeAddHighlight(left, segment.leftStart(), segment.leftEnd(), painters.changePainter());
                    safeAddLineHighlights(left, segment.leftStart(), segment.leftEnd(), painters.changeLinePainter());
                }
                if (segment.rightStart() >= 0 && segment.rightEnd() >= 0) {
                    safeAddHighlight(right, segment.rightStart(), segment.rightEnd(), painters.changePainter());
                    safeAddLineHighlights(right, segment.rightStart(), segment.rightEnd(), painters.changeLinePainter());
                }
            }
        }
    }

    private static void applySpans(JTextArea area, UnifiedView view) {
        DiffPainters painters = DiffPainters.current();
        if (view.lineSpans() != null) {
            for (Span span : view.lineSpans()) {
                DefaultHighlighter.DefaultHighlightPainter painter = switch (span.type()) {
                    case ADD_LINE -> painters.insLinePainter();
                    case DEL_LINE -> painters.delLinePainter();
                    case HUNK_LINE -> painters.changeLinePainter();
                    case HEADER_LINE -> painters.headerLinePainter();
                    default -> null;
                };
                if (painter != null) {
                    safeAddLineHighlights(area, span.start(), span.end(), painter);
                }
            }
        }
        if (view.charSpans() != null) {
            for (Span span : view.charSpans()) {
                DefaultHighlighter.DefaultHighlightPainter painter = switch (span.type()) {
                    case ADD_CHAR -> painters.insPainter();
                    case DEL_CHAR -> painters.delPainter();
                    case CHANGE_CHAR -> painters.changePainter();
                    default -> null;
                };
                if (painter != null) {
                    safeAddHighlight(area, span.start(), span.end(), painter);
                }
            }
        }
    }

    private static void safeAddHighlight(JTextArea area, int start, int end, Highlighter.HighlightPainter painter) {
        if (area == null || painter == null) {
            return;
        }
        try {
            int docLen = area.getDocument().getLength();
            int s = Math.max(0, Math.min(start, docLen));
            int e = Math.max(0, Math.min(end, docLen));
            if (e > s) {
                area.getHighlighter().addHighlight(s, e, painter);
            }
        } catch (BadLocationException ignored) {
        }
    }

    private static void safeAddLineHighlights(JTextArea area, int start, int end,
                                              Highlighter.HighlightPainter painter) {
        if (area == null || painter == null) {
            return;
        }
        try {
            int docLen = area.getDocument().getLength();
            int s = Math.max(0, Math.min(start, docLen));
            int e = Math.max(0, Math.min(end, docLen));
            if (e <= s) {
                return;
            }
            int startLine = area.getLineOfOffset(s);
            int endLine = area.getLineOfOffset(Math.max(s, e - 1));
            for (int line = startLine; line <= endLine; line++) {
                int lineStart = area.getLineStartOffset(line);
                int lineEnd = area.getLineEndOffset(line);
                area.getHighlighter().addHighlight(lineStart, lineEnd, painter);
            }
        } catch (BadLocationException ignored) {
        }
    }

    private record DiffPainters(
            DefaultHighlighter.DefaultHighlightPainter delPainter,
            DefaultHighlighter.DefaultHighlightPainter insPainter,
            DefaultHighlighter.DefaultHighlightPainter changePainter,
            DefaultHighlighter.DefaultHighlightPainter delLinePainter,
            DefaultHighlighter.DefaultHighlightPainter insLinePainter,
            DefaultHighlighter.DefaultHighlightPainter changeLinePainter,
            DefaultHighlighter.DefaultHighlightPainter headerLinePainter) {

        static DiffPainters current() {
            boolean dark = FlatLaf.isLafDark();
            if (dark) {
                return new DiffPainters(
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(180, 70, 70, 120)),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(70, 150, 70, 120)),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(200, 170, 60, 120)),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(200, 80, 80, 50)),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(60, 140, 60, 50)),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(200, 160, 40, 50)),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(70, 70, 70, 80)));
            }
            return new DiffPainters(
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 204, 204)),
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(204, 255, 204)),
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 249, 196)),
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 102, 102, 60)),
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(0, 153, 0, 60)),
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 193, 7, 60)),
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(230, 230, 230)));
        }
    }
}
