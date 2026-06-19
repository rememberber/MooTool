package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickNoteGitUtilTest {

    @Test
    void unquoteGitStatusPath_decodesUtf8OctalSequence() {
        String quoted = "\"\\346\\234\\252\\345\\221\\275\\345\\220\\215_2026-06-19_08-09-48.txt\"";
        assertEquals("未命名_2026-06-19_08-09-48.txt", QuickNoteGitUtil.unquoteGitStatusPath(quoted));
    }

    @Test
    void unquoteGitStatusPath_returnsPlainPathAsIs() {
        assertEquals("未命名_2026-06-19_08-09-48.txt",
                QuickNoteGitUtil.unquoteGitStatusPath("未命名_2026-06-19_08-09-48.txt"));
    }
}
