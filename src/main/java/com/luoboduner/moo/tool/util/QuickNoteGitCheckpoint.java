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

    private QuickNoteGitCheckpoint() {
    }

    public static String buildCommitMessage(File vaultDir) {
        int count = QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir).size();
        if (count <= 1) {
            return I18n.get("quickNote.git.checkpointOne");
        }
        return I18n.format("quickNote.git.checkpointMany", count);
    }

    public static boolean hasPendingCheckpointWork(File vaultDir) {
        if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
            return false;
        }
        if (isGitCheckpointBlocked(vaultDir)) {
            return false;
        }
        return !QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir).isEmpty();
    }

    public static boolean isGitCheckpointBlocked(File vaultDir) {
        QuickNoteGitStatus status = QuickNoteGitUtil.getStatus(vaultDir);
        return status.isMerging() || status.getConflictCount() > 0;
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
        List<QuickNoteGitModifiedFile> modified = QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir);
        if (modified.isEmpty()) {
            return QuickNoteGitUtil.GitCommandResult.success("");
        }
        QuickNoteGitUtil.GitCommandResult result = QuickNoteGitUtil.commit(vaultDir, buildCommitMessage(vaultDir));
        if (result.isSuccess()) {
            javax.swing.SwingUtilities.invokeLater(QuickNoteForm::updateGitButtonStatus);
        }
        return result;
    }
}
