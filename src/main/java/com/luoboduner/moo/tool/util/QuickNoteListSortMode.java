package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import org.apache.commons.lang3.StringUtils;

/**
 * 随手记列表排序方式。
 */
public enum QuickNoteListSortMode {
    MODIFIED_TIME,
    CREATE_TIME,
    NAME;

    public String i18nKey() {
        return switch (this) {
            case NAME -> "quickNote.sort.name";
            case MODIFIED_TIME -> "quickNote.sort.modifiedTime";
            case CREATE_TIME -> "quickNote.sort.createTime";
        };
    }

    public static QuickNoteListSortMode fromConfig() {
        return fromId(App.config.getQuickNoteListSortMode());
    }

    public static QuickNoteListSortMode fromId(String id) {
        if (StringUtils.isBlank(id)) {
            return MODIFIED_TIME;
        }
        try {
            return valueOf(id);
        } catch (IllegalArgumentException ignored) {
            return MODIFIED_TIME;
        }
    }
}
