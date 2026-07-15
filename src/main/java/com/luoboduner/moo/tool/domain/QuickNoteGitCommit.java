package com.luoboduner.moo.tool.domain;

import lombok.Getter;

@Getter
public class QuickNoteGitCommit {

    private final String hash;
    private final String shortHash;
    private final String author;
    private final String date;
    private final String message;

    public QuickNoteGitCommit(String hash, String shortHash, String author, String date, String message) {
        this.hash = hash;
        this.shortHash = shortHash;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    @Override
    public String toString() {
        return shortHash + " " + date + " " + message;
    }
}
