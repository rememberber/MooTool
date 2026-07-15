package com.luoboduner.moo.tool.util;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Vault 树列表展开/折叠辅助。
 */
public final class VaultTreeUiUtil {

    private VaultTreeUiUtil() {
    }

    public static void applyExpandMode(JTree tree, VaultTreeExpandMode mode) {
        if (tree == null) {
            return;
        }
        VaultTreeExpandMode resolved = mode == null ? VaultTreeExpandMode.EXPAND_ALL : mode;
        if (resolved == VaultTreeExpandMode.EXPAND_ALL) {
            expandAllRows(tree);
        } else {
            collapseAllRows(tree);
        }
    }

    public static void ensurePathVisible(JTree tree, TreePath path) {
        if (tree == null || path == null) {
            return;
        }
        TreePath parent = path.getParentPath();
        while (parent != null) {
            tree.expandPath(parent);
            parent = parent.getParentPath();
        }
        tree.scrollPathToVisible(path);
    }

    private static void expandAllRows(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private static void collapseAllRows(JTree tree) {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }
}
