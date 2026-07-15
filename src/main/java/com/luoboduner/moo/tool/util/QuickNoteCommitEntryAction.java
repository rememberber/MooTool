package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.QuickNoteConflictResolverDialog;
import com.luoboduner.moo.tool.ui.dialog.QuickNoteQuickCommitDialog;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 工具栏 Git 按钮入口：AutoGit 开则立即 checkpoint，关则打开轻量提交框。
 */
public final class QuickNoteCommitEntryAction {

    private QuickNoteCommitEntryAction() {
    }

    public static void trigger(Component parent) {
        if (App.config.isQuickNoteAutoGitCommit()) {
            runQuickCheckpoint(parent);
            return;
        }
        QuickNoteQuickCommitDialog.showDialog(parent);
    }

    private static void runQuickCheckpoint(Component parent) {
        QuickNoteListener.quickSaveSync(true, false, false);
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        if (QuickNoteGitCheckpoint.isGitCheckpointBlocked(vaultDir)) {
            handleBlocked(parent);
            return;
        }
        if (!QuickNoteGitCheckpoint.hasPendingCheckpointWork(vaultDir)) {
            QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.quickCommit.nothingToDo"),
                    QuickNoteIndicatorTools.TipsLevel.INFO);
            return;
        }
        if (QuickNoteVaultRefreshCoordinator.hasUnsavedChanges()) {
            QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.unsavedChanges"),
                    QuickNoteIndicatorTools.TipsLevel.WARN);
            return;
        }

        JButton gitButton = QuickNoteForm.getInstance() != null ? QuickNoteForm.getInstance().getGitButton() : null;
        if (gitButton != null) {
            gitButton.setEnabled(false);
        }
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return QuickNoteGitCheckpoint.runManualCheckpoint(null);
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
        String detail = QuickNoteGitUtil.formatFailureMessage(result.getMessage());
        QuickNoteIndicatorTools.showTips(detail, QuickNoteIndicatorTools.TipsLevel.ERROR);
    }

    private static String resolveSuccessMessage() {
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        if (QuickNoteGitUtil.hasRemote(vaultDir)) {
            return I18n.get("quickNote.git.quickCommit.successPushed");
        }
        return I18n.get("quickNote.git.quickCommit.successLocal");
    }

    private static void handleBlocked(Component parent) {
        if (!QuickNoteGitUtil.listConflictFiles(QuickNoteVaultUtil.getVaultDir()).isEmpty()) {
            QuickNoteConflictResolverDialog.showDialog();
            return;
        }
        QuickNoteIndicatorTools.showTips(I18n.get("quickNote.git.checkpointBlocked"),
                QuickNoteIndicatorTools.TipsLevel.WARN);
    }
}
