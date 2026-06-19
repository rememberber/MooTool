package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitPullResult;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewerManager;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.domain.TQuickNote;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 协调 Vault 外部变更刷新：抑制应用自身写入触发的 watcher，并保护未保存的编辑器内容。
 */
public final class QuickNoteVaultRefreshCoordinator {

    public static final long INTERNAL_WRITE_SUPPRESSION_MS = 4000;

    private static volatile long lastInternalWriteAt;
    private static final AtomicInteger pendingSaveTasks = new AtomicInteger();
    private static volatile String discardInProgressPath;

    private QuickNoteVaultRefreshCoordinator() {
    }

    public static void beginDiscard(String relativePath) {
        if (StringUtils.isNotBlank(relativePath)) {
            discardInProgressPath = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
            markInternalWrite();
        }
    }

    public static void endDiscard() {
        discardInProgressPath = null;
    }

    public static boolean shouldSkipSaveForPath(String relativePath) {
        if (StringUtils.isBlank(discardInProgressPath) || StringUtils.isBlank(relativePath)) {
            return false;
        }
        return discardInProgressPath.equals(QuickNoteVaultUtil.normalizeRelativePath(relativePath));
    }

    public static void markInternalWrite() {
        lastInternalWriteAt = System.currentTimeMillis();
    }

    public static boolean shouldSuppressWatcherRefresh() {
        return System.currentTimeMillis() - lastInternalWriteAt < INTERNAL_WRITE_SUPPRESSION_MS;
    }

    public static void onSaveTaskStarted() {
        pendingSaveTasks.incrementAndGet();
    }

    public static void onSaveTaskFinished() {
        pendingSaveTasks.decrementAndGet();
        markInternalWrite();
    }

    public static boolean hasUnsavedChanges() {
        if (pendingSaveTasks.get() > 0) {
            return true;
        }
        String path = QuickNoteListener.selectedPath;
        if (StringUtils.isBlank(path)) {
            return false;
        }
        QuickNoteRSyntaxTextViewerManager manager = QuickNoteForm.quickNoteRSyntaxTextViewerManager;
        if (manager == null) {
            return false;
        }
        String editorText = manager.getTextByPath(path);
        if (editorText == null) {
            return false;
        }
        TQuickNote onDisk = QuickNoteVaultUtil.loadByPath(path);
        String diskText = onDisk != null ? StringUtils.defaultString(onDisk.getContent()) : "";
        return !editorText.equals(diskText);
    }

    public static void refreshAfterExternalChange() {
        refreshAfterExternalChange(false);
    }

    public static void refreshAfterExternalChange(boolean forceReloadCurrentNote) {
        if (SwingUtilities.isEventDispatchThread()) {
            doRefreshAfterExternalChange(forceReloadCurrentNote);
        } else {
            SwingUtilities.invokeLater(() -> doRefreshAfterExternalChange(forceReloadCurrentNote));
        }
    }

    public static void refreshAfterPull(QuickNoteGitPullResult result) {
        if (result == null) {
            return;
        }
        Runnable task = () -> {
            switch (result.getStatus()) {
                case UPDATED -> refreshAfterPullUpdates(result.getUpdatedFiles());
                case UP_TO_DATE -> QuickNoteForm.updateGitButtonStatus();
                case CONFLICT, ERROR -> QuickNoteForm.updateGitButtonStatus();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static void refreshAfterPullUpdates(java.util.List<String> updatedFiles) {
        QuickNoteForm.refreshNoteTree();
        String currentPath = QuickNoteListener.selectedPath;
        boolean shouldReloadCurrent = StringUtils.isNotBlank(currentPath)
                && (updatedFiles.isEmpty() || updatedFiles.contains(QuickNoteVaultUtil.normalizeRelativePath(currentPath)));
        if (shouldReloadCurrent) {
            QuickNoteForm.reloadCurrentNoteFromDiskIfClean();
        }
        QuickNoteForm.updateGitButtonStatus();
    }

    private static void doRefreshAfterExternalChange(boolean forceReloadCurrentNote) {
        QuickNoteForm.refreshNoteTree();
        if (forceReloadCurrentNote) {
            QuickNoteForm.reloadCurrentNoteFromDisk(true);
        } else {
            QuickNoteForm.reloadCurrentNoteFromDiskIfClean();
        }
        QuickNoteForm.updateGitButtonStatus();
    }
}
