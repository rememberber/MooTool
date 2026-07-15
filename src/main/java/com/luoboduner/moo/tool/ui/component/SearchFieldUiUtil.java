package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;

import javax.swing.*;

/**
 * 搜索输入框统一样式：搜索图标 + 圆角边框。
 */
public final class SearchFieldUiUtil {

    private SearchFieldUiUtil() {
    }

    public static void configure(JTextField field) {
        if (field == null) {
            return;
        }
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
//        field.putClientProperty(FlatClientProperties.COMPONENT_ROUND_RECT, true);
        // 在 SearchFieldUiUtil 中追加 FlatClientProperties.STYLE（例如 "arc: 12"）
        field.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
    }
}
