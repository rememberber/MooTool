package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.util.JsonBeautyTreeUtil;
import com.luoboduner.moo.tool.util.JsonBeautyVaultUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 树：拖拽 JSON 文件或文件夹到目标文件夹以移动。
 */
@Slf4j
public final class JsonBeautyTreeDragDrop {

    private static final DataFlavor JSON_PATHS_FLAVOR = createFlavor("java.util.ArrayList");
    private static final DataFlavor FOLDER_PATH_FLAVOR = createFlavor("java.lang.String");

    private JsonBeautyTreeDragDrop() {
    }

    public static void install(JTree tree) {
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON);
        tree.setTransferHandler(new JsonTreeTransferHandler());
    }

    private static DataFlavor createFlavor(String className) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class JsonTreeTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null || paths.length == 0) {
                return null;
            }
            List<String> jsonPaths = new ArrayList<>();
            String folderPath = null;
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof String pathValue) {
                    if (StringUtils.isNotBlank(pathValue)) {
                        folderPath = pathValue;
                    }
                } else if (userObject instanceof TJsonBeauty item
                        && StringUtils.isNotBlank(item.getRelativePath())) {
                    jsonPaths.add(item.getRelativePath());
                }
            }
            if (!jsonPaths.isEmpty()) {
                return new TreeDragTransferable(jsonPaths, null);
            }
            if (StringUtils.isNotBlank(folderPath) && paths.length == 1) {
                return new TreeDragTransferable(null, folderPath);
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            if (dropLocation.getPath() == null) {
                return false;
            }
            if (support.isDataFlavorSupported(FOLDER_PATH_FLAVOR)) {
                try {
                    String draggedFolder = (String) support.getTransferable().getTransferData(FOLDER_PATH_FLAVOR);
                    DefaultMutableTreeNode dropNode = (DefaultMutableTreeNode) dropLocation.getPath()
                            .getLastPathComponent();
                    String targetFolder = resolveTargetFolder(dropNode);
                    return !isInvalidFolderDrop(draggedFolder, targetFolder);
                } catch (Exception ex) {
                    return false;
                }
            }
            return support.isDataFlavorSupported(JSON_PATHS_FLAVOR);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                JTree tree = (JTree) support.getComponent();
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                DefaultMutableTreeNode dropNode = (DefaultMutableTreeNode) dropLocation.getPath()
                        .getLastPathComponent();
                String targetFolder = resolveTargetFolder(dropNode);
                if (support.isDataFlavorSupported(FOLDER_PATH_FLAVOR)) {
                    String draggedFolder = (String) support.getTransferable().getTransferData(FOLDER_PATH_FLAVOR);
                    moveFolder(draggedFolder, targetFolder, tree);
                    return true;
                }
                List<String> jsonPaths = (List<String>) support.getTransferable().getTransferData(JSON_PATHS_FLAVOR);
                if (jsonPaths == null || jsonPaths.isEmpty()) {
                    return false;
                }
                moveJsonFiles(jsonPaths, targetFolder, tree);
                return true;
            } catch (IOException | UnsupportedFlavorException ex) {
                log.warn("Drop import failed: {}", ex.getMessage());
                return false;
            }
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            // 文件移动在 importData 中完成
        }
    }

    public static void moveJsonFiles(List<String> jsonPaths, String targetFolder, JTree tree) {
        String normalizedTarget = JsonBeautyVaultUtil.normalizeFolderPath(targetFolder);
        List<String> pathsToMove = new ArrayList<>();
        for (String oldPath : jsonPaths) {
            if (StringUtils.isBlank(oldPath)) {
                continue;
            }
            String currentFolder = JsonBeautyVaultUtil.parentFolder(oldPath);
            if (!normalizedTarget.equals(currentFolder)) {
                pathsToMove.add(oldPath);
            }
        }
        if (pathsToMove.isEmpty()) {
            return;
        }
        if (StringUtils.isNotBlank(JsonBeautyListener.selectedPathJson)
                && pathsToMove.contains(JsonBeautyListener.selectedPathJson)) {
            JsonBeautyListener.flushSelectedJsonBeforePathChange();
        }
        JsonBeautyListener.runJsonPathMutation(() -> {
            boolean moved = false;
            for (String oldPath : pathsToMove) {
                try {
                    String newPath = JsonBeautyVaultUtil.moveJsonToFolder(oldPath, normalizedTarget);
                    if (!oldPath.equals(newPath)) {
                        moved = true;
                        if (oldPath.equals(JsonBeautyListener.selectedPathJson)) {
                            JsonBeautyListener.selectedPathJson = newPath;
                            TJsonBeauty movedItem = JsonBeautyVaultUtil.loadByPath(newPath);
                            if (movedItem != null) {
                                JsonBeautyListener.selectedNameJson = movedItem.getName();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Move JSON failed {} -> {}: {}", oldPath, normalizedTarget, e.getMessage());
                }
            }
            if (moved) {
                JsonBeautyForm.initList();
                JsonBeautyForm.updateGitButtonStatus();
                if (tree != null) {
                    tree.updateUI();
                }
            }
        }, pathsToMove.toArray(new String[0]));
    }

    public static void moveFolder(String folderPath, String targetFolder, JTree tree) {
        String normalizedFolder = JsonBeautyVaultUtil.normalizeFolderPath(folderPath);
        String normalizedTarget = JsonBeautyVaultUtil.normalizeFolderPath(targetFolder);
        if (StringUtils.isBlank(normalizedFolder) || isInvalidFolderDrop(normalizedFolder, normalizedTarget)) {
            return;
        }
        String currentParent = JsonBeautyVaultUtil.parentFolder(normalizedFolder + "/.keep");
        if (normalizedTarget.equals(currentParent)) {
            return;
        }
        try {
            List<String> affectedPaths = JsonBeautyVaultUtil.listJsonPathsUnderFolder(normalizedFolder);
            String newFolderPath = JsonBeautyVaultUtil.moveFolderToFolder(normalizedFolder, normalizedTarget);
            if (StringUtils.isNotBlank(JsonBeautyListener.selectedPathJson)) {
                JsonBeautyListener.selectedPathJson = JsonBeautyVaultUtil.remapPathAfterFolderRename(
                        JsonBeautyListener.selectedPathJson, normalizedFolder, newFolderPath);
            }
            JsonBeautyForm.initList();
            JsonBeautyForm.updateGitButtonStatus();
            if (tree != null) {
                tree.updateUI();
            }
        } catch (Exception e) {
            log.warn("Move folder failed {} -> {}: {}", normalizedFolder, normalizedTarget, e.getMessage());
        }
    }

    static String resolveTargetFolder(DefaultMutableTreeNode dropNode) {
        return JsonBeautyTreeUtil.selectedFolderPath(dropNode);
    }

    private static boolean isInvalidFolderDrop(String draggedFolder, String targetFolder) {
        if (StringUtils.isBlank(draggedFolder)) {
            return true;
        }
        String target = JsonBeautyVaultUtil.normalizeFolderPath(targetFolder);
        if (draggedFolder.equals(target)) {
            return true;
        }
        return target.startsWith(draggedFolder + "/");
    }

    private static final class TreeDragTransferable implements Transferable {

        private final List<String> jsonPaths;
        private final String folderPath;

        private TreeDragTransferable(List<String> jsonPaths, String folderPath) {
            this.jsonPaths = jsonPaths == null ? null : new ArrayList<>(jsonPaths);
            this.folderPath = folderPath;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            if (folderPath != null) {
                return new DataFlavor[]{FOLDER_PATH_FLAVOR};
            }
            return new DataFlavor[]{JSON_PATHS_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (folderPath != null) {
                return FOLDER_PATH_FLAVOR.equals(flavor);
            }
            return JSON_PATHS_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (folderPath != null && FOLDER_PATH_FLAVOR.equals(flavor)) {
                return folderPath;
            }
            if (jsonPaths != null && JSON_PATHS_FLAVOR.equals(flavor)) {
                return jsonPaths;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
