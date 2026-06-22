package com.luoboduner.moo.tool.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

/**
 * 随手记 Git 操作日志（内存环形缓冲）。
 */
public final class QuickNoteGitLog {

    private static final int MAX_LINES = 200;
    private static final Deque<String> LINES = new ArrayDeque<>();

    private QuickNoteGitLog() {
    }

    public static synchronized void append(String message) {
        String line = DateFormatUtils.format(new Date(), "HH:mm:ss") + "  " + message;
        LINES.addLast(line);
        while (LINES.size() > MAX_LINES) {
            LINES.removeFirst();
        }
    }

    public static synchronized String getText() {
        return String.join("\n", LINES);
    }

    public static synchronized List<String> getLines() {
        return new ArrayList<>(LINES);
    }

    public static synchronized void clear() {
        LINES.clear();
    }
}
