package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.service.DiffService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickNoteGitDiffHelperTest {

    @Test
    void buildUnifiedView_producesLineSpansForTextChange() {
        var view = DiffService.buildUnifiedView("line one\nline two\n", "line one\nline THREE\n", 3, false);
        assertNotNull(view);
        assertTrue(view.text().contains("-line two"));
        assertTrue(view.text().contains("+line THREE"));
        assertNotNull(view.lineSpans());
        assertTrue(!view.lineSpans().isEmpty());
    }
}
