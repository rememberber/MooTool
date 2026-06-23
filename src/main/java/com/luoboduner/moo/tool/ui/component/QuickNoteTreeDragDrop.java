package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.util.QuickNoteTreeUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
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
 * 随手记树：拖拽笔记或文件夹到目标文件夹以移动。
 */
@Slf4j
public final class QuickNoteTreeDragDrop {

    private static final DataFlavor NOTE_PATHS_FLAVOR = createFlavor("java.util.ArrayList");
    private static final DataFlavor FOLDER_PATH_FLAVOR = createFlavor("java.lang.String");

    private QuickNoteTreeDragDrop() {
    }

    public static void install(JTree tree) {
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON);
        tree.setTransferHandler(new NoteTreeTransferHandler());
    }

    private static DataFlavor createFlavor(String className) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class NoteTreeTransferHandler extends TransferHandler {

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
            List<String> notePaths = new ArrayList<>();
            String folderPath = null;
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof String pathValue) {
                    if (StringUtils.isNotBlank(pathValue)) {
                        folderPath = pathValue;
                    }
                } else if (userObject instanceof TQuickNote note
                        && StringUtils.isNotBlank(note.getRelativePath())) {
                    notePaths.add(note.getRelativePath());
                }
            }
            if (!notePaths.isEmpty()) {
                return new TreeDragTransferable(notePaths, null);
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
            return support.isDataFlavorSupported(NOTE_PATHS_FLAVOR);
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
                List<String> notePaths = (List<String>) support.getTransferable().getTransferData(NOTE_PATHS_FLAVOR);
                if (notePaths == null || notePaths.isEmpty()) {
                    return false;
                }
                moveNotes(notePaths, targetFolder, tree);
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

    public static void moveNotes(List<String> notePaths, String targetFolder, JTree tree) {
        String normalizedTarget = QuickNoteVaultUtil.normalizeFolderPath(targetFolder);
        List<String> pathsToMove = new ArrayList<>();
        for (String oldPath : notePaths) {
            if (StringUtils.isBlank(oldPath)) {
                continue;
            }
            String currentFolder = QuickNoteVaultUtil.parentFolder(oldPath);
            if (!normalizedTarget.equals(currentFolder)) {
                pathsToMove.add(oldPath);
            }
        }
        if (pathsToMove.isEmpty()) {
            return;
        }
        if (StringUtils.isNotBlank(QuickNoteListener.selectedPath)
                && pathsToMove.contains(QuickNoteListener.selectedPath)) {
            QuickNoteListener.flushSelectedNoteBeforePathChange();
        }
        QuickNoteListener.runNotePathMutation(() -> {
            boolean moved = false;
            for (String oldPath : pathsToMove) {
                try {
                    String newPath = QuickNoteVaultUtil.moveNoteToFolder(oldPath, normalizedTarget);
                    if (!oldPath.equals(newPath)) {
                        moved = true;
                        if (oldPath.equals(QuickNoteListener.selectedPath)) {
                            QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(oldPath);
                            QuickNoteListener.selectedPath = newPath;
                            TQuickNote movedNote = QuickNoteVaultUtil.loadByPath(newPath);
                            if (movedNote != null) {
                                QuickNoteListener.selectedName = movedNote.getName();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Move note failed {} -> {}: {}", oldPath, normalizedTarget, e.getMessage());
                }
            }
            if (moved) {
                QuickNoteForm.initNoteList();
                QuickNoteForm.updateGitButtonStatus();
                if (tree != null) {
                    tree.updateUI();
                }
            }
        }, pathsToMove.toArray(new String[0]));
    }

    public static void moveFolder(String folderPath, String targetFolder, JTree tree) {
        String normalizedFolder = QuickNoteVaultUtil.normalizeFolderPath(folderPath);
        String normalizedTarget = QuickNoteVaultUtil.normalizeFolderPath(targetFolder);
        if (StringUtils.isBlank(normalizedFolder) || isInvalidFolderDrop(normalizedFolder, normalizedTarget)) {
            return;
        }
        String currentParent = QuickNoteVaultUtil.parentFolder(normalizedFolder + "/.keep");
        if (normalizedTarget.equals(currentParent)) {
            return;
        }
        try {
            List<String> affectedPaths = QuickNoteVaultUtil.listNotePathsUnderFolder(normalizedFolder);
            String newFolderPath = QuickNoteVaultUtil.moveFolderToFolder(normalizedFolder, normalizedTarget);
            for (String oldPath : affectedPaths) {
                QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(oldPath);
            }
            if (StringUtils.isNotBlank(QuickNoteListener.selectedPath)) {
                QuickNoteListener.selectedPath = QuickNoteVaultUtil.remapPathAfterFolderRename(
                        QuickNoteListener.selectedPath, normalizedFolder, newFolderPath);
            }
            QuickNoteForm.initNoteList();
            QuickNoteForm.updateGitButtonStatus();
            if (tree != null) {
                tree.updateUI();
            }
        } catch (Exception e) {
            log.warn("Move folder failed {} -> {}: {}", normalizedFolder, normalizedTarget, e.getMessage());
        }
    }

    static String resolveTargetFolder(DefaultMutableTreeNode dropNode) {
        Object userObject = dropNode.getUserObject();
        if (userObject instanceof String folderPath) {
            return folderPath;
        }
        if (userObject instanceof TQuickNote note) {
            return QuickNoteVaultUtil.parentFolder(note.getRelativePath());
        }
        return "";
    }

    private static boolean isInvalidFolderDrop(String draggedFolder, String targetFolder) {
        if (StringUtils.isBlank(draggedFolder)) {
            return true;
        }
        String target = QuickNoteVaultUtil.normalizeFolderPath(targetFolder);
        if (draggedFolder.equals(target)) {
            return true;
        }
        return target.startsWith(draggedFolder + "/");
    }

    private static final class TreeDragTransferable implements Transferable {

        private final List<String> notePaths;
        private final String folderPath;

        private TreeDragTransferable(List<String> notePaths, String folderPath) {
            this.notePaths = notePaths == null ? null : new ArrayList<>(notePaths);
            this.folderPath = folderPath;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            if (folderPath != null) {
                return new DataFlavor[]{FOLDER_PATH_FLAVOR};
            }
            return new DataFlavor[]{NOTE_PATHS_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (folderPath != null) {
                return FOLDER_PATH_FLAVOR.equals(flavor);
            }
            return NOTE_PATHS_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (folderPath != null && FOLDER_PATH_FLAVOR.equals(flavor)) {
                return folderPath;
            }
            if (notePaths != null && NOTE_PATHS_FLAVOR.equals(flavor)) {
                return notePaths;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
