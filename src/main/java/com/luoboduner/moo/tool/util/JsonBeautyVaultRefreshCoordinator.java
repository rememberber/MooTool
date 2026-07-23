package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitPullResult;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.ui.startup.AppExecutors;
import com.luoboduner.moo.tool.ui.startup.JsonBeautyLoadData;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协调 JSON Vault 外部变更刷新：抑制应用自身写入触发的 watcher，并保护未保存的编辑器内容。
 */
public final class JsonBeautyVaultRefreshCoordinator {

    public static final long INTERNAL_WRITE_SUPPRESSION_MS = 4000;

    private static volatile long lastInternalWriteAt;
    private static volatile String discardInProgressPath;
    private static final Set<String> skipSavePaths = ConcurrentHashMap.newKeySet();

    private static final VaultExternalRefreshSupport EXTERNAL_REFRESH =
            new VaultExternalRefreshSupport(
                    JsonBeautyVaultRefreshCoordinator::shouldSuppressWatcherRefresh,
                    JsonBeautyVaultRefreshCoordinator::runAsyncExternalRefresh);

    private JsonBeautyVaultRefreshCoordinator() {
    }

    public static void addSkipSavePath(String relativePath) {
        if (StringUtils.isNotBlank(relativePath)) {
            skipSavePaths.add(JsonBeautyVaultUtil.normalizeRelativePath(relativePath));
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

    public static void clearSkipSavePaths() {
        skipSavePaths.clear();
    }

    public static void beginDiscard(String relativePath) {
        if (StringUtils.isNotBlank(relativePath)) {
            discardInProgressPath = JsonBeautyVaultUtil.normalizeRelativePath(relativePath);
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
        String normalized = JsonBeautyVaultUtil.normalizeRelativePath(relativePath);
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

    public static boolean hasUnsavedChanges() {
        String path = JsonBeautyListener.selectedPathJson;
        if (StringUtils.isBlank(path)) {
            return false;
        }
        JsonBeautyForm form = JsonBeautyForm.getInstance();
        if (form == null || form.getTextArea() == null) {
            return false;
        }
        String editorText = form.getTextArea().getText();
        TJsonBeauty onDisk = JsonBeautyVaultUtil.loadByPath(path);
        String diskText = onDisk != null ? StringUtils.defaultString(onDisk.getContent()) : "";
        return !editorText.equals(diskText);
    }

    public static void refreshAfterExternalChange() {
        refreshAfterExternalChange(false);
    }

    public static void refreshAfterExternalChange(boolean forceReloadCurrent) {
        EXTERNAL_REFRESH.requestImmediate(forceReloadCurrent);
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
                case UP_TO_DATE, CONFLICT, ERROR -> JsonBeautyForm.updateGitButtonStatus();
            }
        };
        VaultExternalRefreshSupport.runOnEdt(task);
    }

    private static void refreshAfterPullUpdates(java.util.List<String> updatedFiles) {
        JsonBeautyForm.refreshList();
        String currentPath = JsonBeautyListener.selectedPathJson;
        boolean shouldReloadCurrent = StringUtils.isNotBlank(currentPath)
                && (updatedFiles.isEmpty()
                || updatedFiles.contains(JsonBeautyVaultUtil.normalizeRelativePath(currentPath)));
        if (shouldReloadCurrent) {
            JsonBeautyForm.reloadCurrentFromDiskIfClean();
        }
        JsonBeautyForm.updateGitButtonStatus();
    }

    private static void runAsyncExternalRefresh(boolean forceReloadCurrent) {
        VaultExternalRefreshSupport.runOnEdt(() -> {
            String filter = JsonBeautyForm.getInstance().getSearchTextField().getText();
            AppExecutors.io().execute(() -> {
                try {
                    JsonBeautyVaultUtil.ensureVaultReady();
                    List<TJsonBeauty> items = JsonBeautyVaultUtil.listByFilter(filter);
                    List<String> folders = StringUtils.isNotBlank(filter) ? List.of() : JsonBeautyVaultUtil.listFolders();
                    JsonBeautyLoadData data = new JsonBeautyLoadData(items, folders);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            JsonBeautyForm.applyJsonListData(data.getItems(), data.getFolders(), true);
                            if (forceReloadCurrent) {
                                JsonBeautyForm.reloadCurrentFromDisk(true);
                            } else {
                                JsonBeautyForm.reloadCurrentFromDiskIfClean();
                            }
                            JsonBeautyForm.updateGitButtonStatus();
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
