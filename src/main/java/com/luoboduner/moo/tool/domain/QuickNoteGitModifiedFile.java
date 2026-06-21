package com.luoboduner.moo.tool.domain;

import lombok.Getter;

@Getter
public class QuickNoteGitModifiedFile {

    private final String path;
    private final String originalPath;
    private final String statusCode;
    private final String statusLabel;

    public QuickNoteGitModifiedFile(String path, String statusCode, String statusLabel) {
        this(path, "", statusCode, statusLabel);
    }

    public QuickNoteGitModifiedFile(String path, String originalPath, String statusCode, String statusLabel) {
        this.path = path;
        this.originalPath = originalPath;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
    }

    @Override
    public String toString() {
        if (originalPath != null && !originalPath.isBlank()) {
            return "[" + statusLabel + "] " + originalPath + " -> " + path;
        }
        return "[" + statusLabel + "] " + path;
    }
}
