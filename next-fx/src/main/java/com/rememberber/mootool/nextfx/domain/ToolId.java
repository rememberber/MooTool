package com.rememberber.mootool.nextfx.domain;

public enum ToolId {
    MOOTOOL("mootool"),
    QUICK_NOTE("quickNote"),
    TEXT_DIFF("textDiff"),
    REFORMAT("reformat"),
    JSON("json"),
    JAVA("java"),
    YML_PROPERTIES("ymlProperties"),
    PROTOBUF("protobuf"),
    VARIABLES("variables"),
    HTTP("http"),
    HOST("host"),
    NET("net"),
    UA_PARSE("uaParse"),
    ENCODE("encode"),
    CRYPTO("crypto"),
    REGEX("regex"),
    CRON("cron"),
    QR_CODE("qrCode"),
    TIME_CONVERT("timeConvert"),
    MESSAGE_BOARD("messageBoard"),
    TRANSLATION("translation"),
    CALCULATOR("calculator"),
    COLOR_BOARD("colorBoard"),
    IMAGE("image"),
    PDF("pdf"),
    HARDWARE("hardware");

    private final String id;

    ToolId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean detachable() {
        return this != MOOTOOL;
    }

    public static ToolId fromId(String value) {
        for (ToolId toolId : values()) {
            if (toolId.id.equals(value)) {
                return toolId;
            }
        }
        throw new IllegalArgumentException("Unknown tool id: " + value);
    }

    public static boolean isToolId(String value) {
        for (ToolId toolId : values()) {
            if (toolId.id.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
