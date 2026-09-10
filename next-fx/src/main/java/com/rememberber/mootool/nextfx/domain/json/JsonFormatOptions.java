package com.rememberber.mootool.nextfx.domain.json;

public record JsonFormatOptions(int spaces, boolean sortKeys, boolean ignoreCase, boolean checkDuplicateKeys) {

    public static JsonFormatOptions defaults() {
        return new JsonFormatOptions(2, false, false, true);
    }
}
