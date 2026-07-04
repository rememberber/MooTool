package com.luoboduner.moo.tool.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffServiceTest {

    @Test
    void charOnlyModeSkipsWholeLineInsertDelete() {
        String left = "line1\nline2\nline3";
        String right = "lineA\nlineB\nlineC";
        var charOnly = DiffService.getSegmentsForUI(left, right, false, true);
        assertTrue(charOnly.segments().isEmpty(), "整行替换在仅字符模式下不应产生片段");
    }

    @Test
    void bothModeIncludesWholeLineInsertDelete() {
        String left = "line1\nline2\nline3";
        String right = "lineA\nlineB\nlineC";
        var both = DiffService.getSegmentsForUI(left, right, false, false);
        assertFalse(both.segments().isEmpty(), "整行替换在双层高亮模式下应产生片段");
    }

    @Test
    void charOnlyModeIncludesIntraLineChanges() {
        String left = "hello world";
        String right = "hello there";
        var charOnly = DiffService.getSegmentsForUI(left, right, false, true);
        assertFalse(charOnly.segments().isEmpty(), "同行字符变更在仅字符模式下应产生片段");
    }
}
