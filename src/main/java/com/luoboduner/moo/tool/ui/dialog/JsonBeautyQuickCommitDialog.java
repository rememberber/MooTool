package com.luoboduner.moo.tool.ui.dialog;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.JsonBeautyCommitEntryAction;
import com.luoboduner.moo.tool.util.JsonBeautyGitCheckpoint;
import com.luoboduner.moo.tool.util.JsonBeautyVaultRefreshCoordinator;
import com.luoboduner.moo.tool.util.JsonBeautyVaultUtil;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * AutoGit 关闭时的 JSON 轻量提交对话框。
 */
public class JsonBeautyQuickCommitDialog extends JDialog {

    private final JTextField commitMessageField = new JTextField();
    private final JLabel hintLabel = new JLabel();
    private final JButton commitButton = new JButton();
    private final JButton openFullPanelButton = new JButton();

    public JsonBeautyQuickCommitDialog(Window owner) {
        super(owner, I18n.get("quickNote.git.quickCommit.title"), ModalityType.APPLICATION_MODAL);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.42, 0.28);
        initUi();
        loadDefaults();
    }

    public static void showDialog(Component parent) {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        QuickNoteGitUtil.initRepoIfNeeded(vaultDir);
        if (JsonBeautyGitCheckpoint.isGitCheckpointBlocked(vaultDir)) {
            if (!QuickNoteGitUtil.listConflictFiles(vaultDir).isEmpty()) {
                JsonBeautyConflictResolverDialog.showDialog();
            } else {
                MsgUtil.info(resolveParent(parent), "quickNote.git.checkpointBlocked");
            }
            return;
        }
        Window owner = parent != null
                ? SwingUtilities.getWindowAncestor(parent)
                : App.mainFrame;
        JsonBeautyQuickCommitDialog dialog = new JsonBeautyQuickCommitDialog(owner);
        dialog.setVisible(true);
    }

    private void initUi() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(content, BorderLayout.CENTER);

        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        content.add(hintLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 4));
        center.add(new JLabel(I18n.get("quickNote.git.commitMessage")), BorderLayout.WEST);
        center.add(commitMessageField, BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        openFullPanelButton.addActionListener(e -> {
            Window owner = getOwner();
            dispose();
            JsonBeautyGitDialog.showDialog(owner);
        });
        commitButton.addActionListener(e -> commit());
        JButton cancelButton = new JButton(I18n.get("common.cancel"));
        cancelButton.addActionListener(e -> dispose());
        actions.add(openFullPanelButton);
        actions.add(commitButton);
        actions.add(cancelButton);
        content.add(actions, BorderLayout.SOUTH);

        applyTexts();
        getRootPane().setDefaultButton(commitButton);
    }

    private void applyTexts() {
        setTitle(I18n.get("quickNote.git.quickCommit.title"));
        commitButton.setText(I18n.get("quickNote.git.quickCommit.commit"));
        openFullPanelButton.setText(I18n.get("quickNote.git.quickCommit.openFullPanel"));
    }

    private void loadDefaults() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        commitMessageField.setText(JsonBeautyGitCheckpoint.buildCommitMessage(vaultDir));
        if (QuickNoteGitUtil.hasRemote(vaultDir)) {
            hintLabel.setText(I18n.get("quickNote.git.quickCommit.hintPush"));
        } else {
            hintLabel.setText(I18n.get("quickNote.git.quickCommit.hintLocal"));
        }
    }

    private void commit() {
        JsonBeautyListener.quickSaveSync(true);
        if (JsonBeautyVaultRefreshCoordinator.hasUnsavedChanges()) {
            MsgUtil.info(this, "quickNote.git.unsavedChanges");
            return;
        }
        String message = commitMessageField.getText().trim();
        if (StringUtils.isBlank(message)) {
            MsgUtil.info(this, "quickNote.git.quickCommit.messageRequired");
            return;
        }

        commitButton.setEnabled(false);
        openFullPanelButton.setEnabled(false);
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return JsonBeautyGitCheckpoint.runManualCheckpoint(message);
            }

            @Override
            protected void done() {
                commitButton.setEnabled(true);
                openFullPanelButton.setEnabled(true);
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        JsonBeautyCommitEntryAction.notifyQuickCommitResult(result);
                        dispose();
                        return;
                    }
                    if (result.isPushRejected()) {
                        MsgUtil.info(JsonBeautyQuickCommitDialog.this, "quickNote.git.pushRejected");
                        return;
                    }
                    MsgUtil.errorWithDetail(JsonBeautyQuickCommitDialog.this,
                            "quickNote.git.quickCommit.failed",
                            QuickNoteGitUtil.formatFailureMessage(result.getMessage()));
                } catch (Exception ex) {
                    MsgUtil.errorWithDetail(JsonBeautyQuickCommitDialog.this,
                            "quickNote.git.quickCommit.failed", ex.getMessage());
                }
            }
        }.execute();
    }

    private static Component resolveParent(Component parent) {
        return parent != null ? parent : App.mainFrame;
    }
}
