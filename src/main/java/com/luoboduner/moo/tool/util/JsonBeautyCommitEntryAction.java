package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.JsonBeautyConflictResolverDialog;
import com.luoboduner.moo.tool.ui.dialog.JsonBeautyQuickCommitDialog;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * JSON 工具栏 Git 按钮入口。
 */
public final class JsonBeautyCommitEntryAction {

    private JsonBeautyCommitEntryAction() {
    }

    public static void trigger(Component parent) {
        if (App.config.isJsonBeautyAutoGitCommit()) {
            runQuickCheckpoint(parent);
            return;
        }
        JsonBeautyQuickCommitDialog.showDialog(parent);
    }

    private static void runQuickCheckpoint(Component parent) {
        JsonBeautyListener.quickSaveSync(true);
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        if (JsonBeautyGitCheckpoint.isGitCheckpointBlocked(vaultDir)) {
            handleBlocked(parent);
            return;
        }
        if (!JsonBeautyGitCheckpoint.hasPendingCheckpointWork(vaultDir)) {
            QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.quickCommit.nothingToDo"),
                    QuickNoteIndicatorTools.TipsLevel.INFO);
            return;
        }
        if (JsonBeautyVaultRefreshCoordinator.hasUnsavedChanges()) {
            QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.unsavedChanges"),
                    QuickNoteIndicatorTools.TipsLevel.WARN);
            return;
        }

        JButton gitButton = JsonBeautyForm.getInstance() != null ? JsonBeautyForm.getInstance().getGitButton() : null;
        if (gitButton != null) {
            gitButton.setEnabled(false);
        }
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return JsonBeautyGitCheckpoint.runManualCheckpoint(null);
            }

            @Override
            protected void done() {
                if (gitButton != null) {
                    gitButton.setEnabled(true);
                }
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    notifyQuickCommitResult(result);
                } catch (Exception ex) {
                    QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.quickCommit.failed"),
                            QuickNoteIndicatorTools.TipsLevel.ERROR);
                }
            }
        }.execute();
    }

    public static void notifyQuickCommitResult(QuickNoteGitUtil.GitCommandResult result) {
        if (result == null) {
            return;
        }
        if (result.isSuccess()) {
            String message = resolveSuccessMessage();
            QuickNoteIndicatorTools.showTips(message, QuickNoteIndicatorTools.TipsLevel.SUCCESS);
            return;
        }
        if (result.isPushRejected()) {
            QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.pushRejected"),
                    QuickNoteIndicatorTools.TipsLevel.WARN);
            return;
        }
        String detail = StringUtils.defaultIfBlank(result.getMessage(), I18n.get("quickNote.git.quickCommit.failed"));
        QuickNoteIndicatorTools.showTips(detail, QuickNoteIndicatorTools.TipsLevel.ERROR);
    }

    private static String resolveSuccessMessage() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        if (QuickNoteGitUtil.hasRemote(vaultDir)) {
            return I18n.get("quickNote.git.quickCommit.successPushed");
        }
        return I18n.get("quickNote.git.quickCommit.successLocal");
    }

    private static void handleBlocked(Component parent) {
        if (!QuickNoteGitUtil.listConflictFiles(JsonBeautyVaultUtil.getVaultDir()).isEmpty()) {
            JsonBeautyConflictResolverDialog.showDialog();
            return;
        }
        QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.checkpointBlocked"),
                QuickNoteIndicatorTools.TipsLevel.WARN);
    }
}
