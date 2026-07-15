package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import org.apache.commons.lang3.StringUtils;

/**
 * JSON 列表排序方式。
 */
public enum JsonBeautyListSortMode {
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

    public static JsonBeautyListSortMode fromConfig() {
        return fromId(App.config.getJsonBeautyListSortMode());
    }

    public static JsonBeautyListSortMode fromId(String id) {
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
