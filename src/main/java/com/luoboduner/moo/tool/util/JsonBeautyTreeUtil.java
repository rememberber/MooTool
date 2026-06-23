package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.TJsonBeauty;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建 JSON Vault 的文件夹树模型。
 */
public final class JsonBeautyTreeUtil {

    private JsonBeautyTreeUtil() {
    }

    public static DefaultTreeModel buildTreeModel(List<TJsonBeauty> items, List<String> folders) {
        return buildTreeModel(items, folders, JsonBeautyListSortMode.MODIFIED_TIME);
    }

    public static DefaultTreeModel buildTreeModel(List<TJsonBeauty> items, List<String> folders,
                                                  JsonBeautyListSortMode sortMode) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        Map<String, DefaultMutableTreeNode> folderNodes = new LinkedHashMap<>();
        folderNodes.put("", root);

        List<String> sortedFolders = new ArrayList<>(folders);
        sortedFolders.sort(Comparator.comparingInt(JsonBeautyTreeUtil::folderDepth)
                .thenComparing(String::compareToIgnoreCase));
        for (String folderPath : sortedFolders) {
            ensureFolderNode(folderNodes, root, folderPath);
        }

        for (TJsonBeauty item : sortedItems(items, sortMode)) {
            String parent = JsonBeautyVaultUtil.parentFolder(item.getRelativePath());
            DefaultMutableTreeNode parentNode = ensureFolderNode(folderNodes, root, parent);
            parentNode.add(new DefaultMutableTreeNode(item));
        }
        return new DefaultTreeModel(root);
    }

    public static List<TJsonBeauty> sortedItems(List<TJsonBeauty> items, JsonBeautyListSortMode sortMode) {
        List<TJsonBeauty> sorted = new ArrayList<>(items);
        sorted.sort(comparator(sortMode));
        return sorted;
    }

    public static Comparator<TJsonBeauty> comparator(JsonBeautyListSortMode sortMode) {
        JsonBeautyListSortMode mode = sortMode == null ? JsonBeautyListSortMode.MODIFIED_TIME : sortMode;
        return switch (mode) {
            case NAME -> Comparator.comparing(TJsonBeauty::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case CREATE_TIME -> Comparator.comparing(TJsonBeauty::getCreateTime,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case MODIFIED_TIME -> Comparator.comparing(TJsonBeauty::getModifiedTime,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    public static DefaultMutableTreeNode findNodeByPath(DefaultMutableTreeNode root, String relativePath) {
        if (root == null || StringUtils.isBlank(relativePath)) {
            return null;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof TJsonBeauty item && relativePath.equals(item.getRelativePath())) {
                return child;
            }
            DefaultMutableTreeNode found = findNodeByPath(child, relativePath);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static String selectedFolderPath(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null) {
            return "";
        }
        Object userObject = selectedNode.getUserObject();
        if (userObject instanceof TJsonBeauty item) {
            return JsonBeautyVaultUtil.parentFolder(item.getRelativePath());
        }
        if (userObject instanceof String folderPath) {
            return folderPath;
        }
        return "";
    }

    public static TJsonBeauty selectedItem(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null) {
            return null;
        }
        Object userObject = selectedNode.getUserObject();
        return userObject instanceof TJsonBeauty item ? item : null;
    }

    private static DefaultMutableTreeNode ensureFolderNode(Map<String, DefaultMutableTreeNode> folderNodes,
                                                           DefaultMutableTreeNode root,
                                                           String folderPath) {
        String normalized = JsonBeautyVaultUtil.normalizeFolderPath(folderPath);
        if (folderNodes.containsKey(normalized)) {
            return folderNodes.get(normalized);
        }
        int separatorIndex = normalized.lastIndexOf('/');
        String parentPath = separatorIndex < 0 ? "" : normalized.substring(0, separatorIndex);
        DefaultMutableTreeNode parent = ensureFolderNode(folderNodes, root, parentPath);
        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(normalized);
        parent.add(folderNode);
        folderNodes.put(normalized, folderNode);
        return folderNode;
    }

    private static int folderDepth(String folderPath) {
        if (StringUtils.isBlank(folderPath)) {
            return 0;
        }
        return folderPath.split("/").length;
    }
}
