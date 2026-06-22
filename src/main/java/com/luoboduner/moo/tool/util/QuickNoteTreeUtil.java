package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.TQuickNote;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建随手记 Vault 的文件夹树模型。
 */
public final class QuickNoteTreeUtil {

    private QuickNoteTreeUtil() {
    }

    public static DefaultTreeModel buildTreeModel(List<TQuickNote> notes, List<String> folders) {
        return buildTreeModel(notes, folders, QuickNoteListSortMode.MODIFIED_TIME);
    }

    public static DefaultTreeModel buildTreeModel(List<TQuickNote> notes, List<String> folders,
                                                  QuickNoteListSortMode sortMode) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        Map<String, DefaultMutableTreeNode> folderNodes = new LinkedHashMap<>();
        folderNodes.put("", root);

        List<String> sortedFolders = new ArrayList<>(folders);
        sortedFolders.sort(Comparator.comparingInt(QuickNoteTreeUtil::folderDepth)
                .thenComparing(String::compareToIgnoreCase));
        for (String folderPath : sortedFolders) {
            ensureFolderNode(folderNodes, root, folderPath);
        }

        for (TQuickNote note : sortedNotes(notes, sortMode)) {
            String parent = QuickNoteVaultUtil.parentFolder(note.getRelativePath());
            DefaultMutableTreeNode parentNode = ensureFolderNode(folderNodes, root, parent);
            parentNode.add(new DefaultMutableTreeNode(note));
        }
        return new DefaultTreeModel(root);
    }

    public static List<TQuickNote> sortedNotes(List<TQuickNote> notes, QuickNoteListSortMode sortMode) {
        List<TQuickNote> sortedNotes = new ArrayList<>(notes);
        sortedNotes.sort(comparator(sortMode));
        return sortedNotes;
    }

    public static Comparator<TQuickNote> comparator(QuickNoteListSortMode sortMode) {
        QuickNoteListSortMode mode = sortMode == null ? QuickNoteListSortMode.MODIFIED_TIME : sortMode;
        return switch (mode) {
            case NAME -> Comparator.comparing(TQuickNote::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case CREATE_TIME -> Comparator.comparing(TQuickNote::getCreateTime,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case MODIFIED_TIME -> Comparator.comparing(TQuickNote::getModifiedTime,
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
            if (userObject instanceof TQuickNote note && relativePath.equals(note.getRelativePath())) {
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
        if (userObject instanceof TQuickNote note) {
            return QuickNoteVaultUtil.parentFolder(note.getRelativePath());
        }
        if (userObject instanceof String folderPath) {
            return folderPath;
        }
        return "";
    }

    public static TQuickNote selectedNote(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null) {
            return null;
        }
        Object userObject = selectedNode.getUserObject();
        return userObject instanceof TQuickNote note ? note : null;
    }

    private static DefaultMutableTreeNode ensureFolderNode(Map<String, DefaultMutableTreeNode> folderNodes,
                                                           DefaultMutableTreeNode root,
                                                           String folderPath) {
        String normalized = QuickNoteVaultUtil.normalizeFolderPath(folderPath);
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
