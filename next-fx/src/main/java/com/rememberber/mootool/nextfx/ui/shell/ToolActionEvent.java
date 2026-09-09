package com.rememberber.mootool.nextfx.ui.shell;

import javafx.event.Event;
import javafx.event.EventType;

public final class ToolActionEvent extends Event {

    public static final EventType<ToolActionEvent> DETACH =
            new EventType<>(Event.ANY, "TOOL_DETACH");

    public ToolActionEvent(EventType<ToolActionEvent> type) {
        super(type);
    }
}
