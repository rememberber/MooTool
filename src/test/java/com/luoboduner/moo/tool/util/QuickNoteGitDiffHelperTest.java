package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.enums.DiffTypeEnum;
import com.luoboduner.moo.tool.service.DiffService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickNoteGitDiffHelperTest {

    @Test
    void getSegmentsForUI_marksChangedLines() {
        var uiDiff = DiffService.getSegmentsForUI("line one\nline two\n", "line one\nline THREE\n", false);
        assertNotNull(uiDiff);
        assertNotNull(uiDiff.segments());
        assertFalse(uiDiff.segments().isEmpty());
        assertTrue(uiDiff.segments().stream().anyMatch(seg -> seg.type() == DiffTypeEnum.CHANGE
                || seg.type() == DiffTypeEnum.DELETE
                || seg.type() == DiffTypeEnum.INSERT));
    }
}
