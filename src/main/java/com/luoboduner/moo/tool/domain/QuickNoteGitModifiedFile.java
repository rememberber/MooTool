package com.luoboduner.moo.tool.domain;

import lombok.Getter;

@Getter
public class QuickNoteGitModifiedFile {

    private final String path;
    private final String statusCode;
    private final String statusLabel;

    public QuickNoteGitModifiedFile(String path, String statusCode, String statusLabel) {
        this.path = path;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
    }

    @Override
    public String toString() {
        return "[" + statusLabel + "] " + path;
    }
}
