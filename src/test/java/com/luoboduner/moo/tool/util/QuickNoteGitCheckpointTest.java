package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickNoteGitCheckpointTest {

    @Test
    void shouldRetryPush_whenAheadWithRemote() {
        QuickNoteGitStatus status = new QuickNoteGitStatus(true, true, "main", 0, 0, 2, 0, false);
        assertTrue(QuickNoteGitCheckpoint.shouldRetryPush(status));
    }

    @Test
    void shouldRetryPush_whenNoRemoteOrNotAhead() {
        assertFalse(QuickNoteGitCheckpoint.shouldRetryPush(
                new QuickNoteGitStatus(true, true, "main", 0, 0, 0, 0, false)));
        assertFalse(QuickNoteGitCheckpoint.shouldRetryPush(
                new QuickNoteGitStatus(true, false, "main", 0, 0, 3, 0, false)));
        assertFalse(QuickNoteGitCheckpoint.shouldRetryPush(null));
    }

    @Test
    void isPushRejected_detectsCommonGitMessages() {
        assertTrue(QuickNoteGitUtil.isPushRejected(
                "error: failed to push some refs to 'origin'\n ! [rejected] main -> main (non-fast-forward)"));
        assertFalse(QuickNoteGitUtil.isPushRejected("Everything up-to-date"));
    }
}
