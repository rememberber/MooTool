package com.luoboduner.moo.tool.domain;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class QuickNoteGitPullResult {

    public enum Status {
        UPDATED,
        UP_TO_DATE,
        CONFLICT,
        ERROR
    }

    private final Status status;
    private final List<String> updatedFiles;
    private final String message;

    public QuickNoteGitPullResult(Status status, List<String> updatedFiles, String message) {
        this.status = status;
        this.updatedFiles = updatedFiles == null ? List.of() : List.copyOf(updatedFiles);
        this.message = message == null ? "" : message;
    }

    public static QuickNoteGitPullResult updated(List<String> updatedFiles, String message) {
        return new QuickNoteGitPullResult(Status.UPDATED, updatedFiles, message);
    }

    public static QuickNoteGitPullResult upToDate(String message) {
        return new QuickNoteGitPullResult(Status.UP_TO_DATE, Collections.emptyList(), message);
    }

    public static QuickNoteGitPullResult conflict(String message) {
        return new QuickNoteGitPullResult(Status.CONFLICT, Collections.emptyList(), message);
    }

    public static QuickNoteGitPullResult error(String message) {
        return new QuickNoteGitPullResult(Status.ERROR, Collections.emptyList(), message);
    }

    public boolean isSuccess() {
        return status == Status.UPDATED || status == Status.UP_TO_DATE;
    }
}
