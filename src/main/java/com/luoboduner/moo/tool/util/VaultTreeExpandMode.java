package com.luoboduner.moo.tool.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Vault 树列表默认展开方式。
 */
public enum VaultTreeExpandMode {
    EXPAND_ALL,
    COLLAPSE_ALL;

    public String i18nKey() {
        return switch (this) {
            case EXPAND_ALL -> "setting.vaultTree.expandAll";
            case COLLAPSE_ALL -> "setting.vaultTree.collapseAll";
        };
    }

    public static VaultTreeExpandMode fromId(String id) {
        if (StringUtils.isBlank(id)) {
            return EXPAND_ALL;
        }
        try {
            return valueOf(id);
        } catch (IllegalArgumentException ignored) {
            return EXPAND_ALL;
        }
    }
}
