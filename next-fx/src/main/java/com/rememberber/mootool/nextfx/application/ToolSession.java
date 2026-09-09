package com.rememberber.mootool.nextfx.application;

import com.rememberber.mootool.nextfx.domain.ToolId;
import javafx.scene.Node;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ToolSession {

    public enum WindowState {
        DOCKED,
        DETACHING,
        DETACHED,
        DOCKING,
        DISPOSED
    }

    private final ToolId toolId;
    private final Supplier<Node> factory;
    private final AtomicLong requestId = new AtomicLong();
    private Node view;
    private WindowState windowState = WindowState.DOCKED;
    private DocumentSession document;

    public ToolSession(ToolId toolId, Supplier<Node> factory) {
        this.toolId = toolId;
        this.factory = factory;
    }

    public ToolId toolId() {
        return toolId;
    }

    public synchronized Node view() {
        if (view == null) {
            view = factory.get();
        }
        return view;
    }

    public boolean loaded() {
        return view != null;
    }

    public WindowState windowState() {
        return windowState;
    }

    public void setWindowState(WindowState windowState) {
        this.windowState = windowState;
    }

    public DocumentSession document() {
        return document;
    }

    public void setDocument(DocumentSession document) {
        this.document = document;
    }

    public long nextRequestId() {
        return requestId.incrementAndGet();
    }

    public long currentRequestId() {
        return requestId.get();
    }
}
