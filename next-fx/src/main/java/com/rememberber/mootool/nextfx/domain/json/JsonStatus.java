package com.rememberber.mootool.nextfx.domain.json;

public record JsonStatus(Kind kind, String type, String details) {

    public enum Kind {
        IDLE,
        VALID,
        ERROR
    }

    public static JsonStatus idle() {
        return new JsonStatus(Kind.IDLE, "", "");
    }

    public static JsonStatus valid(String type) {
        return new JsonStatus(Kind.VALID, type, "");
    }

    public static JsonStatus error(String details) {
        return new JsonStatus(Kind.ERROR, "", details);
    }
}
