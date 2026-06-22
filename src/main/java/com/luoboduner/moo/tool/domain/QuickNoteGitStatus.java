package com.luoboduner.moo.tool.domain;

import lombok.Getter;

@Getter
public class QuickNoteGitStatus {

    private final boolean gitRepo;
    private final boolean hasRemote;
    private final String branch;
    private final int changedCount;
    private final int conflictCount;
    private final int ahead;
    private final int behind;
    private final boolean merging;

    public QuickNoteGitStatus(boolean gitRepo, boolean hasRemote, String branch,
                              int changedCount, int conflictCount, int ahead, int behind, boolean merging) {
        this.gitRepo = gitRepo;
        this.hasRemote = hasRemote;
        this.branch = branch;
        this.changedCount = changedCount;
        this.conflictCount = conflictCount;
        this.ahead = ahead;
        this.behind = behind;
        this.merging = merging;
    }

    public boolean hasPendingChanges() {
        return changedCount > 0 || conflictCount > 0;
    }
}
