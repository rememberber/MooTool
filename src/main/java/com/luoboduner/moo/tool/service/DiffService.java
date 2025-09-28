package com.luoboduner.moo.tool.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.luoboduner.moo.tool.bean.textdiff.Span;
import com.luoboduner.moo.tool.bean.textdiff.TextDiffSegment;
import com.luoboduner.moo.tool.bean.textdiff.UIDiff;
import com.luoboduner.moo.tool.bean.textdiff.UnifiedView;
import com.luoboduner.moo.tool.enums.DiffTypeEnum;
import com.luoboduner.moo.tool.enums.UnifiedSpanTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *     差异服务
 *     计算 diff 并生成 unified diff（行级）
 * </pre>
 *
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 10:51
 */
@Slf4j
public class DiffService {

    /**
     * 简单行切分器
     *
     * @param text 文本
     * @return 行列表
     */
    private static List<String> toLines(String text) {
        // -1 保留末尾空格
        return List.of(text.split("\\R", -1));
    }

    public static UIDiff getSegmentsForUI(String oldText, String newText, boolean ignoreWhitespace) {
        return getSegmentsForUI(oldText, newText, ignoreWhitespace, false);
    }

    public static UIDiff getSegmentsForUI(String oldText, String newText, boolean ignoreWhitespace, boolean charOnly) {
        List<String> oldLines = toLines(oldText);
        List<String> newLines = toLines(newText);
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        // 计算每一行的起始字符偏移（含换行）
        int[] leftLineStarts = computeLineStartOffsets(oldText);
        int[] rightLineStarts = computeLineStartOffsets(newText);

        List<TextDiffSegment> textDiffSegments = new ArrayList<>();

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int leftPos = delta.getSource().getPosition();
            int rightPos = delta.getTarget().getPosition();
            List<String> leftLines = delta.getSource().getLines();
            List<String> rightLines = delta.getTarget().getLines();
            DeltaType type = delta.getType();

            switch (type) {
                case DELETE: {
                    if (charOnly) {
                        // 仅字符模式：整行删除不计入字符级差异
                        break;
                    }
                    for (int i = 0; i < leftLines.size(); i++) {
                        int lineIdx = leftPos + i;
                        int start = safeLineStart(leftLineStarts, lineIdx);
                        int end = start + leftLines.get(i).length();
                        if (!(ignoreWhitespace && isAllWhitespace(leftLines.get(i))))
                            textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.DELETE, start, end, -1, -1));
                    }
                    break;
                }
                case INSERT: {
                    if (charOnly) {
                        // 仅字符模式：整行插入不计入字符级差异
                        break;
                    }
                    for (int i = 0; i < rightLines.size(); i++) {
                        int lineIdx = rightPos + i;
                        int start = safeLineStart(rightLineStarts, lineIdx);
                        int end = start + rightLines.get(i).length();
                        if (!(ignoreWhitespace && isAllWhitespace(rightLines.get(i))))
                            textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.INSERT, -1, -1, start, end));
                    }
                    break;
                }
                case CHANGE: {
                    int pairCount = Math.min(leftLines.size(), rightLines.size());
                    // 对应行做字符级 diff
                    for (int i = 0; i < pairCount; i++) {
                        String a = leftLines.get(i);
                        String b = rightLines.get(i);
                        int aLineStart = safeLineStart(leftLineStarts, leftPos + i);
                        int bLineStart = safeLineStart(rightLineStarts, rightPos + i);
                        // 使用 DiffUtils 在字符层面做 diff
                        List<String> ac = toChars(a);
                        List<String> bc = toChars(b);
                        Patch<String> charPatch = DiffUtils.diff(ac, bc);
                        for (AbstractDelta<String> cDelta : charPatch.getDeltas()) {
                            int aStart = aLineStart + cDelta.getSource().getPosition();
                            int aEnd = aStart + cDelta.getSource().getLines().size();
                            int bStart = bLineStart + cDelta.getTarget().getPosition();
                            int bEnd = bStart + cDelta.getTarget().getLines().size();
                            if (cDelta.getType() == DeltaType.DELETE) {
                                if (!(ignoreWhitespace && isAllWhitespace(a.substring(
                                        Math.min(a.length(), Math.max(0, cDelta.getSource().getPosition())),
                                        Math.min(a.length(), Math.max(0, cDelta.getSource().getPosition() + cDelta.getSource().getLines().size()))
                                )))) {
                                    textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.DELETE, aStart, aEnd, -1, -1));
                                }
                            } else if (cDelta.getType() == DeltaType.INSERT) {
                                if (!(ignoreWhitespace && isAllWhitespace(b.substring(
                                        Math.min(b.length(), Math.max(0, cDelta.getTarget().getPosition())),
                                        Math.min(b.length(), Math.max(0, cDelta.getTarget().getPosition() + cDelta.getTarget().getLines().size()))
                                )))) {
                                    textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.INSERT, -1, -1, bStart, bEnd));
                                }
                            } else { // CHANGE
                                String aSub = a.substring(
                                        Math.min(a.length(), Math.max(0, cDelta.getSource().getPosition())),
                                        Math.min(a.length(), Math.max(0, cDelta.getSource().getPosition() + cDelta.getSource().getLines().size())));
                                String bSub = b.substring(
                                        Math.min(b.length(), Math.max(0, cDelta.getTarget().getPosition())),
                                        Math.min(b.length(), Math.max(0, cDelta.getTarget().getPosition() + cDelta.getTarget().getLines().size())));
                                if (!(ignoreWhitespace && equalsIgnoreWhitespace(aSub, bSub))) {
                                    textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.CHANGE, aStart, aEnd, bStart, bEnd));
                                }
                            }
                        }
                    }
                    if (!charOnly) {
                        // 左侧多余：视为整行删除
                        for (int i = pairCount; i < leftLines.size(); i++) {
                            int aLineStart = safeLineStart(leftLineStarts, leftPos + i);
                            if (!(ignoreWhitespace && isAllWhitespace(leftLines.get(i))))
                                textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.DELETE, aLineStart, aLineStart + leftLines.get(i).length(), -1, -1));
                        }
                        // 右侧多余：视为整行插入
                        for (int i = pairCount; i < rightLines.size(); i++) {
                            int bLineStart = safeLineStart(rightLineStarts, rightPos + i);
                            if (!(ignoreWhitespace && isAllWhitespace(rightLines.get(i))))
                                textDiffSegments.add(new TextDiffSegment(DiffTypeEnum.INSERT, -1, -1, bLineStart, bLineStart + rightLines.get(i).length()));
                        }
                    }
                    break;
                }
                case EQUAL:
                    // 不产生片段
                    break;
            }
        }

        return new UIDiff(textDiffSegments);
    }

    private static int[] computeLineStartOffsets(String text) {
        int n = 1; // 至少一行
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') n++;
        }
        int[] starts = new int[n];
        int idx = 0;
        starts[idx++] = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                if (idx < n) starts[idx++] = i + 1;
            }
        }
        return starts;
    }

    private static int safeLineStart(int[] starts, int lineIndex) {
        if (lineIndex < 0) {
            return 0;
        }
        if (lineIndex >= starts.length) {
            return (starts.length == 0 ? 0 : starts[starts.length - 1]);
        }
        return starts[lineIndex];
    }

    public static List<String> toChars(String s) {
        List<String> list = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            list.add(String.valueOf(s.charAt(i)));
        }
        return list;
    }

    public static boolean isAllWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean equalsIgnoreWhitespace(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isWhitespace(ca)) {
                i++;
                continue;
            }
            if (Character.isWhitespace(cb)) {
                j++;
                continue;
            }
            if (ca != cb) return false;
            i++;
            j++;
        }
        while (i < a.length()) {
            if (!Character.isWhitespace(a.charAt(i++))) return false;
        }
        while (j < b.length()) {
            if (!Character.isWhitespace(b.charAt(j++))) return false;
        }
        return true;
    }

    /**
     * 构建统一差异视图（包含文本与高亮区间）
     */
    public static UnifiedView buildUnifiedView(String oldText, String newText, int context, boolean ignoreWhitespace) {
        List<String> oldLines = toLines(oldText);
        List<String> newLines = toLines(newText);
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        List<String> unifiedLines = UnifiedDiffUtils.generateUnifiedDiff("old", "new", oldLines, patch, context);

        String unifiedText = String.join("\n", unifiedLines);
        List<Span> lineSpans = new ArrayList<>();
        List<Span> charSpans = new ArrayList<>();
        int charEvents = 0;

        int offset = 0;
        List<Integer> minusStarts = new ArrayList<>();
        List<String> minusTexts = new ArrayList<>();
        List<Integer> plusStarts = new ArrayList<>();
        List<String> plusTexts = new ArrayList<>();

        for (int i = 0; i < unifiedLines.size(); i++) {
            String line = unifiedLines.get(i);
            int lineStart = offset;
            int lineLen = line.length();
            int lineEnd = lineStart + lineLen;

            if (line.startsWith("@@")) {
                lineSpans.add(new Span(lineStart, lineEnd, UnifiedSpanTypeEnum.HUNK_LINE));
                charEvents += addIntralineSpans(minusStarts, minusTexts, plusStarts, plusTexts, charSpans, ignoreWhitespace);
                minusStarts.clear();
                minusTexts.clear();
                plusStarts.clear();
                plusTexts.clear();
            } else if (line.startsWith("+")) {
                if (!(ignoreWhitespace && isAllWhitespace(line.substring(1)))) {
                    lineSpans.add(new Span(lineStart, lineEnd, UnifiedSpanTypeEnum.ADD_LINE));
                }
                plusStarts.add(lineStart);
                plusTexts.add(line.substring(1));
            } else if (line.startsWith("-")) {
                if (!(ignoreWhitespace && isAllWhitespace(line.substring(1)))) {
                    lineSpans.add(new Span(lineStart, lineEnd, UnifiedSpanTypeEnum.DEL_LINE));
                }
                minusStarts.add(lineStart);
                minusTexts.add(line.substring(1));
            } else if (line.startsWith("---") || line.startsWith("+++")) {
                lineSpans.add(new Span(lineStart, lineEnd, UnifiedSpanTypeEnum.HEADER_LINE));
            }

            offset = (i < unifiedLines.size() - 1) ? (lineEnd + 1) : lineEnd;
        }

        charEvents += addIntralineSpans(minusStarts, minusTexts, plusStarts, plusTexts, charSpans, ignoreWhitespace);

        return new UnifiedView(unifiedText, lineSpans, charSpans, charEvents);
    }

    private static int addIntralineSpans(List<Integer> minusStarts, List<String> minusTexts,
                                         List<Integer> plusStarts, List<String> plusTexts,
                                         List<Span> out, boolean ignoreWhitespace) {
        int events = 0;
        int pairs = Math.min(minusTexts.size(), plusTexts.size());
        for (int i = 0; i < pairs; i++) {
            String a = minusTexts.get(i);
            String b = plusTexts.get(i);
            int aBase = minusStarts.get(i) + 1; // 跳过前缀符号
            int bBase = plusStarts.get(i) + 1;
            Patch<String> charPatch = DiffUtils.diff(toChars(a), toChars(b));
            for (AbstractDelta<String> d : charPatch.getDeltas()) {
                int aStart = aBase + d.getSource().getPosition();
                int aEnd = aStart + d.getSource().getLines().size();
                int bStart = bBase + d.getTarget().getPosition();
                int bEnd = bStart + d.getTarget().getLines().size();
                switch (d.getType()) {
                    case DELETE:
                        if (ignoreWhitespace && isAllWhitespace(safeSub(a, d.getSource().getPosition(), d.getSource().getLines().size())))
                            continue;
                        out.add(new Span(aStart, aEnd, UnifiedSpanTypeEnum.DEL_CHAR));
                        events++;
                        break;
                    case INSERT:
                        if (ignoreWhitespace && isAllWhitespace(safeSub(b, d.getTarget().getPosition(), d.getTarget().getLines().size())))
                            continue;
                        out.add(new Span(bStart, bEnd, UnifiedSpanTypeEnum.ADD_CHAR));
                        events++;
                        break;
                    case CHANGE:
                        String aSub = safeSub(a, d.getSource().getPosition(), d.getSource().getLines().size());
                        String bSub = safeSub(b, d.getTarget().getPosition(), d.getTarget().getLines().size());
                        if (ignoreWhitespace && equalsIgnoreWhitespace(aSub, bSub)) continue;
                        if (!aSub.isEmpty()) out.add(new Span(aStart, aEnd, UnifiedSpanTypeEnum.CHANGE_CHAR));
                        if (!bSub.isEmpty()) out.add(new Span(bStart, bEnd, UnifiedSpanTypeEnum.CHANGE_CHAR));
                        events++;
                        break;
                    case EQUAL:
                        break;
                }
            }
        }
        return events;
    }

    private static String safeSub(String s, int pos, int len) {
        int start = Math.max(0, Math.min(pos, s.length()));
        int end = Math.max(0, Math.min(pos + len, s.length()));
        if (end < start) {
            end = start;
        }
        return s.substring(start, end);
    }

}
