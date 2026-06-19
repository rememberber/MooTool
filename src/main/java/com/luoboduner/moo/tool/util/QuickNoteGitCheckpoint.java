package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.QuickNoteGitModifiedFile;
import com.luoboduner.moo.tool.domain.QuickNoteGitStatus;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 统一的 Git 自动检查点提交（对标 Tolaria runAutomaticCheckpoint）。
 */
public final class QuickNoteGitCheckpoint {

    enum CheckpointAction {
        COMMIT_AND_SYNC,
        PUSH_ONLY
    }

    record CheckpointPlan(CheckpointAction action, String message) {
    }

    private QuickNoteGitCheckpoint() {
    }

    public static String buildCommitMessage(File vaultDir) {
        int count = QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir).size();
        if (count <= 1) {
            return I18n.get("quickNote.git.checkpointOne");
        }
        return I18n.format("quickNote.git.checkpointMany", count);
    }

    public static boolean shouldRetryPush(QuickNoteGitStatus status) {
        return status != null && status.isHasRemote() && status.getAhead() > 0;
    }

    public static boolean hasPendingCheckpointWork(File vaultDir) {
        if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
            return false;
        }
        if (isGitCheckpointBlocked(vaultDir)) {
            return false;
        }
        if (!QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir).isEmpty()) {
            return true;
        }
        return shouldRetryPush(QuickNoteGitUtil.getStatus(vaultDir));
    }

    public static boolean isGitCheckpointBlocked(File vaultDir) {
        QuickNoteGitStatus status = QuickNoteGitUtil.getStatus(vaultDir);
        return status.isMerging() || status.getConflictCount() > 0;
    }

    static CheckpointPlan planAutomaticCheckpoint(File vaultDir) {
        return planManualCheckpoint(vaultDir, null);
    }

    static CheckpointPlan planManualCheckpoint(File vaultDir, String customMessage) {
        List<QuickNoteGitModifiedFile> modified = QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir);
        if (!modified.isEmpty()) {
            String message = StringUtils.isNotBlank(customMessage)
                    ? customMessage.trim()
                    : buildCommitMessage(vaultDir);
            return new CheckpointPlan(CheckpointAction.COMMIT_AND_SYNC, message);
        }
        if (shouldRetryPush(QuickNoteGitUtil.getStatus(vaultDir))) {
            return new CheckpointPlan(CheckpointAction.PUSH_ONLY, "");
        }
        return null;
    }

    public static QuickNoteGitUtil.GitCommandResult runManualCheckpoint(String customMessage) {
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        QuickNoteGitUtil.initRepoIfNeeded(vaultDir);
        if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
            return QuickNoteGitUtil.GitCommandResult.failure(I18n.get("quickNote.git.initRequired"));
        }
        if (isGitCheckpointBlocked(vaultDir)) {
            return QuickNoteGitUtil.GitCommandResult.failure(I18n.get("quickNote.git.checkpointBlocked"));
        }
        if (QuickNoteVaultRefreshCoordinator.hasUnsavedChanges()) {
            return QuickNoteGitUtil.GitCommandResult.failure(I18n.get("quickNote.git.unsavedChanges"));
        }

        CheckpointPlan plan = planManualCheckpoint(vaultDir, customMessage);
        if (plan == null) {
            return QuickNoteGitUtil.GitCommandResult.failure(I18n.get("quickNote.git.quickCommit.nothingToDo"));
        }

        QuickNoteGitUtil.GitCommandResult result = executeCheckpointPlan(vaultDir, plan);
        if (result.isSuccess()) {
            javax.swing.SwingUtilities.invokeLater(QuickNoteForm::updateGitButtonStatus);
        }
        return result;
    }

    public static QuickNoteGitUtil.GitCommandResult runAutomaticCheckpoint() {
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        if (!App.config.isQuickNoteAutoGitCommit()) {
            return QuickNoteGitUtil.GitCommandResult.success("");
        }
        if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
            return QuickNoteGitUtil.GitCommandResult.success("");
        }
        if (isGitCheckpointBlocked(vaultDir)) {
            return QuickNoteGitUtil.GitCommandResult.success("");
        }
        if (QuickNoteVaultRefreshCoordinator.hasUnsavedChanges()) {
            return QuickNoteGitUtil.GitCommandResult.success("");
        }

        CheckpointPlan plan = planAutomaticCheckpoint(vaultDir);
        if (plan == null) {
            return QuickNoteGitUtil.GitCommandResult.success("");
        }

        QuickNoteGitUtil.GitCommandResult result = executeCheckpointPlan(vaultDir, plan);
        if (result.isSuccess()) {
            javax.swing.SwingUtilities.invokeLater(QuickNoteForm::updateGitButtonStatus);
        }
        return result;
    }

    private static QuickNoteGitUtil.GitCommandResult executeCheckpointPlan(File vaultDir, CheckpointPlan plan) {
        if (plan.action() == CheckpointAction.PUSH_ONLY) {
            return QuickNoteGitUtil.pushIfNeeded(vaultDir);
        }
        QuickNoteGitUtil.GitCommandResult commitResult = QuickNoteGitUtil.commit(vaultDir, plan.message());
        if (!commitResult.isSuccess()) {
            return commitResult;
        }
        if (!QuickNoteGitUtil.hasRemote(vaultDir)) {
            return commitResult;
        }
        QuickNoteGitUtil.GitCommandResult pushResult = QuickNoteGitUtil.push(vaultDir);
        if (pushResult.isSuccess()) {
            return pushResult;
        }
        if (pushResult.isPushRejected()) {
            return QuickNoteGitUtil.GitCommandResult.pushRejected(
                    StringUtils.defaultIfBlank(pushResult.getMessage(), I18n.get("quickNote.git.pushRejected")));
        }
        return pushResult;
    }
}
