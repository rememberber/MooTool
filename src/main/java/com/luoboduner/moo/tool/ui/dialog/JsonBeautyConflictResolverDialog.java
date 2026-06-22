package com.luoboduner.moo.tool.ui.dialog;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.JsonBeautyVaultRefreshCoordinator;
import com.luoboduner.moo.tool.util.JsonBeautyVaultUtil;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Vault 应用内冲突解决向导。
 */
public class JsonBeautyConflictResolverDialog extends JDialog {

    private final JPanel filesPanel = new JPanel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton commitButton = new JButton();
    private final Map<String, String> chosenStrategies = new HashMap<>();

    public JsonBeautyConflictResolverDialog(Window owner) {
        super(owner, I18n.get("quickNote.git.resolveConflicts"), ModalityType.APPLICATION_MODAL);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.55, 0.5);
        initUi();
        refreshConflictFiles();
    }

    public static void showDialog() {
        Window owner = App.mainFrame != null ? App.mainFrame : null;
        JsonBeautyConflictResolverDialog dialog = new JsonBeautyConflictResolverDialog(owner);
        dialog.setVisible(true);
    }

    private void initUi() {
        setLayout(new BorderLayout(8, 8));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(content, BorderLayout.CENTER);

        JLabel hintLabel = new JLabel(I18n.get("quickNote.git.resolveConflictsHint"));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        content.add(hintLabel, BorderLayout.NORTH);

        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        content.add(new JScrollPane(filesPanel), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        bottom.add(statusLabel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton closeButton = new JButton(I18n.get("common.close"));
        closeButton.addActionListener(e -> dispose());
        commitButton.addActionListener(e -> commitResolution());
        actions.add(commitButton);
        actions.add(closeButton);
        bottom.add(actions, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);

        applyTexts();
    }

    private void applyTexts() {
        setTitle(I18n.get("quickNote.git.resolveConflicts"));
        commitButton.setText(I18n.get("quickNote.git.commitAndContinue"));
    }

    private void refreshConflictFiles() {
        filesPanel.removeAll();
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        List<String> conflicts = QuickNoteGitUtil.listConflictFiles(vaultDir);
        if (conflicts.isEmpty()) {
            if (QuickNoteGitUtil.isMerging(vaultDir)) {
                statusLabel.setText(I18n.get("quickNote.git.readyToCommitResolution"));
                commitButton.setEnabled(true);
            } else {
                statusLabel.setText(I18n.get("quickNote.git.noConflicts"));
                commitButton.setEnabled(false);
            }
            filesPanel.revalidate();
            filesPanel.repaint();
            return;
        }

        String mode = QuickNoteGitUtil.getConflictMode(vaultDir);
        statusLabel.setText(I18n.format("quickNote.git.resolveStatus", conflicts.size(), mode));
        for (String path : conflicts) {
            filesPanel.add(buildConflictRow(path));
        }
        commitButton.setEnabled(false);
        filesPanel.revalidate();
        filesPanel.repaint();
    }

    private JPanel buildConflictRow(String path) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel pathLabel = new JLabel(path);
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.BOLD));
        row.add(pathLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton keepMineButton = new JButton(I18n.get("quickNote.git.keepMine"));
        JButton keepTheirsButton = new JButton(I18n.get("quickNote.git.keepTheirs"));
        JButton openFileButton = new JButton(I18n.get("quickNote.git.openFile"));

        keepMineButton.addActionListener(e -> resolveFile(path, "ours", keepMineButton, keepTheirsButton));
        keepTheirsButton.addActionListener(e -> resolveFile(path, "theirs", keepMineButton, keepTheirsButton));
        openFileButton.addActionListener(e -> openJson(path));

        buttons.add(keepMineButton);
        buttons.add(keepTheirsButton);
        buttons.add(openFileButton);
        row.add(buttons, BorderLayout.CENTER);
        return row;
    }

    private void resolveFile(String path, String strategy, JButton keepMineButton, JButton keepTheirsButton) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        keepMineButton.setEnabled(false);
        keepTheirsButton.setEnabled(false);
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return QuickNoteGitUtil.resolveConflictFile(JsonBeautyVaultUtil.getVaultDir(), path, strategy);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        chosenStrategies.put(path, strategy);
                        refreshConflictFiles();
                        updateCommitButtonState();
                        JsonBeautyForm.updateGitButtonStatus();
                    } else {
                        MsgUtil.errorWithDetail(JsonBeautyConflictResolverDialog.this,
                                "quickNote.git.resolveConflictFailed", result.getMessage());
                        keepMineButton.setEnabled(true);
                        keepTheirsButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    MsgUtil.errorWithDetail(JsonBeautyConflictResolverDialog.this,
                            "quickNote.git.resolveConflictFailed", ex.getMessage());
                    keepMineButton.setEnabled(true);
                    keepTheirsButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void updateCommitButtonState() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        List<String> conflicts = QuickNoteGitUtil.listConflictFiles(vaultDir);
        boolean ready = conflicts.isEmpty()
                && (QuickNoteGitUtil.isMerging(vaultDir) || !chosenStrategies.isEmpty());
        commitButton.setEnabled(ready);
        if (ready) {
            statusLabel.setText(I18n.get("quickNote.git.readyToCommitResolution"));
        }
    }

    private void openJson(String path) {
        TJsonBeauty item = JsonBeautyVaultUtil.loadByPath(path);
        if (item == null) {
            MsgUtil.info(this, "quickNote.git.noteNotFound");
            return;
        }
        JsonBeautyForm.selectJsonInList(item.getRelativePath());
        JsonBeautyForm.showJson(item);
    }

    private void commitResolution() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        commitButton.setEnabled(false);
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return QuickNoteGitUtil.commitConflictResolution(JsonBeautyVaultUtil.getVaultDir());
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        JsonBeautyVaultRefreshCoordinator.refreshAfterExternalChange(true);
                        JsonBeautyForm.updateGitButtonStatus();
                        MsgUtil.info(JsonBeautyConflictResolverDialog.this, "quickNote.git.conflictsResolved");
                        dispose();
                    } else {
                        statusLabel.setText(StringUtils.defaultIfBlank(result.getMessage(),
                                I18n.get("quickNote.git.resolveCommitFailed")));
                        commitButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    statusLabel.setText(StringUtils.defaultIfBlank(ex.getMessage(),
                            I18n.get("quickNote.git.resolveCommitFailed")));
                    commitButton.setEnabled(true);
                }
            }
        }.execute();
    }
}
