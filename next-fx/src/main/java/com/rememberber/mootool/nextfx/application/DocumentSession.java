package com.rememberber.mootool.nextfx.application;

import java.util.concurrent.atomic.AtomicLong;

public final class DocumentSession {

    private final String documentId;
    private final AtomicLong revision = new AtomicLong();
    private volatile String text;
    private volatile int caret;
    private volatile int anchor;
    private volatile int firstVisibleParagraph;
    private volatile double horizontalOffset;
    private volatile boolean wrap;
    private volatile boolean dirty;

    public DocumentSession(String documentId, String text, boolean wrap) {
        this.documentId = documentId;
        this.text = text == null ? "" : text;
        this.wrap = wrap;
    }

    public String documentId() {
        return documentId;
    }

    public String text() {
        return text;
    }

    public long revision() {
        return revision.get();
    }

    public boolean wrap() {
        return wrap;
    }

    public boolean dirty() {
        return dirty;
    }

    public int caret() {
        return caret;
    }

    public int anchor() {
        return anchor;
    }

    public int firstVisibleParagraph() {
        return firstVisibleParagraph;
    }

    public double horizontalOffset() {
        return horizontalOffset;
    }

    public long replaceText(String next, int caret, int anchor) {
        this.text = next == null ? "" : next;
        this.caret = caret;
        this.anchor = anchor;
        this.dirty = true;
        return revision.incrementAndGet();
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public void restoreView(int caret, int anchor, int firstVisibleParagraph, double horizontalOffset) {
        this.caret = caret;
        this.anchor = anchor;
        this.firstVisibleParagraph = firstVisibleParagraph;
        this.horizontalOffset = horizontalOffset;
    }

    public void markClean() {
        this.dirty = false;
    }
}
