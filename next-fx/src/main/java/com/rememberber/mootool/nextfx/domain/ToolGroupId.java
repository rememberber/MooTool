package com.rememberber.mootool.nextfx.domain;

public enum ToolGroupId {
    HOME("home"),
    TEXT("text"),
    DEV("dev"),
    NETWORK("network"),
    ENCODE("encode"),
    DAILY("daily"),
    SYSTEM("system");

    private final String id;

    ToolGroupId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
