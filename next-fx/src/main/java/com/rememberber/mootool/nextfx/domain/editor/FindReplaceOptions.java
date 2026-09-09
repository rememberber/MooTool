package com.rememberber.mootool.nextfx.domain.editor;

public record FindReplaceOptions(boolean matchCase, boolean wholeWord, boolean regex) {

    public static FindReplaceOptions defaults() {
        return new FindReplaceOptions(false, false, false);
    }
}
