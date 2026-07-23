package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitPullResult;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewerManager;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.ui.startup.AppExecutors;
import com.luoboduner.moo.tool.ui.startup.QuickNoteLoadData;
import com.luoboduner.moo.tool.domain.TQuickNote;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 协调 Vault 外部变更刷新：抑制应用自身写入触发的 watcher，并保护未保存的编辑器内容。
 */
public final class QuickNoteVaultRefreshCoordinator {

    public static final long INTERNAL_WRITE_SUPPRESSION_MS = 4000;

    private static volatile long lastInternalWriteAt;
    private static final AtomicInteger pendingSaveTasks = new AtomicInteger();
    private static volatile String discardInProgressPath;
    private static final Set<String> skipSavePaths = ConcurrentHashMap.newKeySet();

    private static final VaultExternalRefreshSupport EXTERNAL_REFRESH =
            new VaultExternalRefreshSupport(
                    QuickNoteVaultRefreshCoordinator::shouldSuppressWatcherRefresh,
                    QuickNoteVaultRefreshCoordinator::runAsyncExternalRefresh);

    private QuickNoteVaultRefreshCoordinator() {
    }

    public static void addSkipSavePath(String relativePath) {
        if (StringUtils.isNotBlank(relativePath)) {
            skipSavePaths.add(QuickNoteVaultUtil.normalizeRelativePath(relativePath));
        }
    }

    public static void addSkipSavePaths(Collection<String> relativePaths) {
        if (relativePaths == null) {
            return;
        }
        for (String relativePath : relativePaths) {
            addSkipSavePath(relativePath);
        }
    }

    public static void removeSkipSavePath(String relativePath) {
        if (StringUtils.isNotBlank(relativePath)) {
            skipSavePaths.remove(QuickNoteVaultUtil.normalizeRelativePath(relativePath));
        }
    }

    public static void clearSkipSavePaths() {
        skipSavePaths.clear();
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
        if (StringUtils.isBlank(relativePath)) {
            return false;
        }
        String normalized = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
        if (skipSavePaths.contains(normalized)) {
            return true;
        }
        return StringUtils.isNotBlank(discardInProgressPath)
                && discardInProgressPath.equals(normalized);
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

    public static boolean hasPendingSaveTasks() {
        return pendingSaveTasks.get() > 0;
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
        EXTERNAL_REFRESH.requestImmediate(forceReloadCurrentNote);
    }

    public static void requestDebouncedExternalRefresh() {
        EXTERNAL_REFRESH.requestDebounced();
    }

    public static void cancelPendingExternalRefresh() {
        EXTERNAL_REFRESH.cancelPending();
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
        VaultExternalRefreshSupport.runOnEdt(task);
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

    private static void runAsyncExternalRefresh(boolean forceReloadCurrentNote) {
        VaultExternalRefreshSupport.runOnEdt(() -> {
            String filter = QuickNoteForm.getInstance().getSearchTextField().getText();
            boolean searchContent = QuickNoteForm.getInstance().getSearchContentCheckBox().isSelected();
            AppExecutors.io().execute(() -> {
                try {
                    QuickNoteVaultUtil.ensureVaultReady();
                    List<TQuickNote> notes;
                    if (searchContent && StringUtils.isNotBlank(filter)) {
                        notes = QuickNoteVaultUtil.listByFilter(filter, true);
                    } else {
                        notes = QuickNoteVaultUtil.listByFilter(filter, false);
                    }
                    List<String> folders = searchContent && StringUtils.isNotBlank(filter)
                            ? List.of()
                            : QuickNoteVaultUtil.listFolders();
                    QuickNoteLoadData data = new QuickNoteLoadData(notes, folders);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            QuickNoteForm.applyNoteListData(data.getNotes(), data.getFolders(), true);
                            if (forceReloadCurrentNote) {
                                QuickNoteForm.reloadCurrentNoteFromDisk(true);
                            } else {
                                QuickNoteForm.reloadCurrentNoteFromDiskIfClean();
                            }
                            QuickNoteForm.updateGitButtonStatus();
                        } finally {
                            EXTERNAL_REFRESH.markFinished();
                        }
                    });
                } catch (Exception e) {
                    EXTERNAL_REFRESH.markFinished();
                }
            });
        });
    }
}
