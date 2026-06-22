package com.luoboduner.moo.tool.ui.dialog;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultRefreshCoordinator;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用内冲突解决向导：逐文件选择 ours/theirs，完成后提交合并。
 */
public class QuickNoteConflictResolverDialog extends JDialog {

    private final JPanel filesPanel = new JPanel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton commitButton = new JButton();
    private final Map<String, String> chosenStrategies = new HashMap<>();

    public QuickNoteConflictResolverDialog(Window owner) {
        super(owner, I18n.get("quickNote.git.resolveConflicts"), ModalityType.APPLICATION_MODAL);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.55, 0.5);
        initUi();
        refreshConflictFiles();
    }

    public static void showDialog() {
        Window owner = App.mainFrame != null ? App.mainFrame : null;
        QuickNoteConflictResolverDialog dialog = new QuickNoteConflictResolverDialog(owner);
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
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
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
        JButton openNoteButton = new JButton(I18n.get("quickNote.git.openNote"));

        keepMineButton.addActionListener(e -> resolveFile(path, "ours", keepMineButton, keepTheirsButton));
        keepTheirsButton.addActionListener(e -> resolveFile(path, "theirs", keepMineButton, keepTheirsButton));
        openNoteButton.addActionListener(e -> openNote(path));

        buttons.add(keepMineButton);
        buttons.add(keepTheirsButton);
        buttons.add(openNoteButton);
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
                return QuickNoteGitUtil.resolveConflictFile(QuickNoteVaultUtil.getVaultDir(), path, strategy);
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
                        QuickNoteForm.updateGitButtonStatus();
                    } else {
                        MsgUtil.errorWithDetail(QuickNoteConflictResolverDialog.this,
                                "quickNote.git.resolveConflictFailed", result.getMessage());
                        keepMineButton.setEnabled(true);
                        keepTheirsButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    MsgUtil.errorWithDetail(QuickNoteConflictResolverDialog.this,
                            "quickNote.git.resolveConflictFailed", ex.getMessage());
                    keepMineButton.setEnabled(true);
                    keepTheirsButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void updateCommitButtonState() {
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        List<String> conflicts = QuickNoteGitUtil.listConflictFiles(vaultDir);
        boolean ready = conflicts.isEmpty()
                && (QuickNoteGitUtil.isMerging(vaultDir) || !chosenStrategies.isEmpty());
        commitButton.setEnabled(ready);
        if (ready) {
            statusLabel.setText(I18n.get("quickNote.git.readyToCommitResolution"));
        }
    }

    private void openNote(String path) {
        TQuickNote note = QuickNoteVaultUtil.loadByPath(path);
        if (note == null) {
            MsgUtil.info(this, "quickNote.git.noteNotFound");
            return;
        }
        QuickNoteForm.selectNoteInTree(note.getRelativePath());
        QuickNoteForm.showNote(note);
    }

    private void commitResolution() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        commitButton.setEnabled(false);
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return QuickNoteGitUtil.commitConflictResolution(QuickNoteVaultUtil.getVaultDir());
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        QuickNoteVaultRefreshCoordinator.refreshAfterExternalChange(true);
                        QuickNoteForm.updateGitButtonStatus();
                        MsgUtil.info(QuickNoteConflictResolverDialog.this, "quickNote.git.conflictsResolved");
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
