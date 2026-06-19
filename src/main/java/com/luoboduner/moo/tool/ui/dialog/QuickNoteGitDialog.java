package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.QuickNoteGitCommit;
import com.luoboduner.moo.tool.domain.QuickNoteGitModifiedFile;
import com.luoboduner.moo.tool.domain.QuickNoteGitStatus;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.QuickNoteGitCheckpoint;
import com.luoboduner.moo.tool.util.QuickNoteGitLog;
import com.luoboduner.moo.tool.util.QuickNoteVaultRefreshCoordinator;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * 随手记 Git 变更、历史与远程同步面板。
 */
public class QuickNoteGitDialog extends JDialog {

    private static QuickNoteGitDialog instance;

    static {
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                onThemeChanged();
            }
        });
    }

    private final DefaultListModel<QuickNoteGitModifiedFile> changesModel = new DefaultListModel<>();
    private final DefaultListModel<QuickNoteGitCommit> historyModel = new DefaultListModel<>();
    private final JList<QuickNoteGitModifiedFile> changesList = new JList<>(changesModel);
    private final JList<QuickNoteGitCommit> historyList = new JList<>(historyModel);
    private final JTextArea diffTextArea = new JTextArea();
    private final JTextField commitMessageField = new JTextField();
    private final JTextField remoteUrlField = new JTextField();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel conflictLabel = new JLabel(" ");
    private final JCheckBox historyScopeCurrentCheckBox = new JCheckBox();
    private final JButton refreshButton = new JButton();
    private final JButton openVaultButton = new JButton();
    private final JButton commitButton = new JButton();
    private final JButton discardButton = new JButton();
    private final JButton abortMergeButton = new JButton();
    private final JButton openChangeFileButton = new JButton();
    private final JButton openConflictsButton = new JButton();
    private final JButton pullButton = new JButton();
    private final JButton pushButton = new JButton();
    private final JButton fetchButton = new JButton();
    private final JButton saveRemoteButton = new JButton();
    private final JButton initGitButton = new JButton();
    private final JTextArea gitLogTextArea = new JTextArea();
    private final JButton clearLogButton = new JButton();
    private final JLabel conflictDiffLabel = new JLabel(" ");

    public QuickNoteGitDialog() {
        super(App.mainFrame, I18n.get("quickNote.git.title"), false);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.72, 0.72);
        setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(content, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(I18n.get("quickNote.git.tab.changes"), buildChangesPanel());
        tabbedPane.addTab(I18n.get("quickNote.git.tab.history"), buildHistoryPanel());
        tabbedPane.addTab(I18n.get("quickNote.git.tab.sync"), buildSyncPanel());
        tabbedPane.addTab(I18n.get("quickNote.git.tab.log"), buildLogPanel());
        content.add(tabbedPane, BorderLayout.CENTER);

        diffTextArea.setEditable(false);
        diffTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane diffScrollPane = new JScrollPane(diffTextArea);
        diffScrollPane.setPreferredSize(new Dimension(100, 180));

        JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        conflictDiffLabel.setVisible(false);
        southPanel.add(conflictDiffLabel, BorderLayout.NORTH);
        southPanel.add(diffScrollPane, BorderLayout.CENTER);
        content.add(southPanel, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.add(statusLabel, BorderLayout.CENTER);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        refreshButton.setToolTipText(I18n.get("common.refresh"));
        topActions.add(openVaultButton);
        topActions.add(refreshButton);
        bottom.add(topActions, BorderLayout.EAST);
        content.add(bottom, BorderLayout.NORTH);

        bindEvents();
        applyTexts();
        applyTheme();
        refreshAll();
    }

    private void applyTheme() {
        Color warning = warningForeground();
        conflictLabel.setForeground(warning);
        conflictDiffLabel.setForeground(warning);

        Color disabled = UIManager.getColor("Label.disabledForeground");
        if (disabled != null) {
            statusLabel.setForeground(disabled);
        }

        styleReaderTextArea(diffTextArea);
        styleReaderTextArea(gitLogTextArea);
        refreshButtonIcons();
    }

    private static Color warningForeground() {
        Color color = UIManager.getColor("Objects.Red");
        if (color != null) {
            return color;
        }
        return FlatLaf.isLafDark() ? new Color(255, 120, 120) : new Color(200, 60, 60);
    }

    private static Color firstUiColor(String primaryKey, String fallbackKey) {
        Color color = UIManager.getColor(primaryKey);
        if (color == null) {
            color = UIManager.getColor(fallbackKey);
        }
        return color;
    }

    private static void styleReaderTextArea(JTextArea area) {
        Color background = firstUiColor("TextArea.background", "Editor.background");
        Color foreground = firstUiColor("TextArea.foreground", "Editor.foreground");
        if (background != null) {
            area.setBackground(background);
            area.setOpaque(true);
            Container parent = area.getParent();
            if (parent instanceof JViewport viewport) {
                viewport.setBackground(background);
            }
        }
        if (foreground != null) {
            area.setForeground(foreground);
        }
        Color caret = UIManager.getColor("TextArea.caretForeground");
        if (caret == null) {
            caret = UIManager.getColor("Editor.caretColor");
        }
        if (caret != null) {
            area.setCaretColor(caret);
        }
    }

    private void refreshButtonIcons() {
        openVaultButton.setIcon(new FlatSVGIcon("icon/list.svg"));
        refreshButton.setIcon(new FlatSVGIcon("icon/refresh.svg"));
        commitButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        discardButton.setIcon(new FlatSVGIcon("icon/remove.svg"));
        abortMergeButton.setIcon(new FlatSVGIcon("icon/info.svg"));
        openChangeFileButton.setIcon(new FlatSVGIcon("icon/list.svg"));
        openConflictsButton.setIcon(new FlatSVGIcon("icon/find.svg"));
        fetchButton.setIcon(new FlatSVGIcon("icon/refresh.svg"));
        pullButton.setIcon(new FlatSVGIcon("icon/down.svg"));
        pushButton.setIcon(new FlatSVGIcon("icon/up.svg"));
        initGitButton.setIcon(new FlatSVGIcon("icon/add.svg"));
        saveRemoteButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        clearLogButton.setIcon(new FlatSVGIcon("icon/remove.svg"));
    }

    public static void onThemeChanged() {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                return;
            }
            if (!instance.isVisible()) {
                instance.dispose();
                instance = null;
                return;
            }
            SwingUtilities.updateComponentTreeUI(instance);
            instance.applyTheme();
            instance.repaint();
        });
    }

    private JPanel buildChangesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel north = new JPanel(new BorderLayout(8, 4));
        north.add(conflictLabel, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        commitMessageField.setText(QuickNoteGitCheckpoint.buildCommitMessage(QuickNoteVaultUtil.getVaultDir()));
        top.add(new JLabel(I18n.get("quickNote.git.commitMessage")), BorderLayout.WEST);
        top.add(commitMessageField, BorderLayout.CENTER);
        north.add(top, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);

        changesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(changesList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(commitButton);
        actions.add(discardButton);
        actions.add(abortMergeButton);
        actions.add(openChangeFileButton);
        actions.add(openConflictsButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        historyScopeCurrentCheckBox.setSelected(StringUtils.isNotBlank(QuickNoteListener.selectedPath));
        top.add(historyScopeCurrentCheckBox);
        panel.add(top, BorderLayout.NORTH);

        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        gitLogTextArea.setEditable(false);
        gitLogTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(gitLogTextArea), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(clearLogButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSyncPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 8);
        panel.add(new JLabel(I18n.get("quickNote.git.remoteUrl")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        remoteUrlField.setColumns(40);
        panel.add(remoteUrlField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(saveRemoteButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(fetchButton);
        actions.add(pullButton);
        actions.add(pushButton);
        actions.add(initGitButton);
        panel.add(actions, gbc);
        return panel;
    }

    private void bindEvents() {
        changesList.addListSelectionListener(e -> showSelectedChangeDiff(e));
        historyList.addListSelectionListener(e -> showSelectedHistoryDiff(e));
        historyScopeCurrentCheckBox.addActionListener(e -> refreshHistory());

        changesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                QuickNoteGitModifiedFile selected = changesList.getSelectedValue();
                if (selected == null) {
                    return;
                }
                TQuickNote note = QuickNoteVaultUtil.loadByPath(selected.getPath());
                if (note != null) {
                    QuickNoteForm.selectNoteInTree(note.getRelativePath());
                    QuickNoteForm.showNote(note);
                }
            }
        });

        openVaultButton.addActionListener(e -> QuickNoteVaultUtil.openVaultDir());
        refreshButton.addActionListener(e -> refreshAll());
        clearLogButton.addActionListener(e -> {
            QuickNoteGitLog.clear();
            refreshGitLog();
        });

        commitButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.committing"), () -> {
            File vaultDir = QuickNoteVaultUtil.getVaultDir();
            String message = StringUtils.defaultIfBlank(commitMessageField.getText(),
                    QuickNoteGitCheckpoint.buildCommitMessage(vaultDir));
            return QuickNoteGitUtil.commit(vaultDir, message);
        }, false));

        discardButton.addActionListener(e -> discardSelectedChange());

        abortMergeButton.addActionListener(e -> {
            int confirm = MsgUtil.confirm(this, "quickNote.git.confirmAbortMerge");
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            runGitTask(I18n.get("quickNote.git.abortingMerge"), () ->
                    QuickNoteGitUtil.abortMerge(QuickNoteVaultUtil.getVaultDir()), true);
        });

        openChangeFileButton.addActionListener(e -> openSelectedChangeInFileManager());
        openConflictsButton.addActionListener(e -> openConflictFilesInFileManager());

        saveRemoteButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.savingRemote"), () -> {
            String remoteUrl = remoteUrlField.getText().trim();
            App.config.setQuickNoteGitRemoteUrl(remoteUrl);
            App.config.save();
            return QuickNoteGitUtil.configureRemote(QuickNoteVaultUtil.getVaultDir(), remoteUrl);
        }, false));

        fetchButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.fetching"), () ->
                QuickNoteGitUtil.fetch(QuickNoteVaultUtil.getVaultDir()), false));

        pullButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.pulling"), () ->
                QuickNoteGitUtil.pull(QuickNoteVaultUtil.getVaultDir()), true));

        pushButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.pushing"), () ->
                QuickNoteGitUtil.push(QuickNoteVaultUtil.getVaultDir()), false));

        initGitButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.initializing"), () -> {
            QuickNoteGitUtil.initRepoIfNeeded(QuickNoteVaultUtil.getVaultDir());
            String remoteUrl = remoteUrlField.getText().trim();
            if (StringUtils.isNotBlank(remoteUrl)) {
                return QuickNoteGitUtil.configureRemote(QuickNoteVaultUtil.getVaultDir(), remoteUrl);
            }
            return QuickNoteGitUtil.GitCommandResult.success(I18n.get("quickNote.git.initSuccess"));
        }, false));
    }

    private void openSelectedChangeInFileManager() {
        QuickNoteGitModifiedFile selected = changesList.getSelectedValue();
        if (selected == null) {
            MsgUtil.info(this, "quickNote.git.selectChangeFirst");
            return;
        }
        QuickNoteVaultUtil.revealInFileManager(selected.getPath());
    }

    private void openConflictFilesInFileManager() {
        List<String> conflicts = QuickNoteGitUtil.listConflictFiles(QuickNoteVaultUtil.getVaultDir());
        if (conflicts.isEmpty()) {
            MsgUtil.info(this, "quickNote.git.noConflicts");
            return;
        }
        if (conflicts.size() == 1) {
            QuickNoteVaultUtil.revealInFileManager(conflicts.get(0));
            return;
        }
        JList<String> conflictList = new JList<>(conflicts.toArray(new String[0]));
        conflictList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conflictList.setSelectedIndex(0);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                new JScrollPane(conflictList),
                I18n.get("quickNote.git.openConflicts"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (confirm == JOptionPane.OK_OPTION) {
            String selected = conflictList.getSelectedValue();
            if (StringUtils.isNotBlank(selected)) {
                QuickNoteVaultUtil.revealInFileManager(selected);
            }
        }
    }

    private void showSelectedChangeDiff(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        QuickNoteGitModifiedFile selected = changesList.getSelectedValue();
        if (selected == null) {
            diffTextArea.setText("");
            return;
        }
        diffTextArea.setText(QuickNoteGitUtil.getWorkingDiff(QuickNoteVaultUtil.getVaultDir(), selected.getPath()));
        updateConflictDiffHint(diffTextArea.getText(), selected.getStatusLabel());
    }

    private void showSelectedHistoryDiff(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        QuickNoteGitCommit selected = historyList.getSelectedValue();
        if (selected == null) {
            diffTextArea.setText("");
            return;
        }
        String path = historyScopeCurrentCheckBox.isSelected() ? currentNotePath() : "";
        if (StringUtils.isBlank(path)) {
            diffTextArea.setText(QuickNoteGitUtil.getCommitDiff(
                    QuickNoteVaultUtil.getVaultDir(), "", selected.getHash()));
            return;
        }
        diffTextArea.setText(QuickNoteGitUtil.getCommitDiff(
                QuickNoteVaultUtil.getVaultDir(), path, selected.getHash()));
        updateConflictDiffHint(diffTextArea.getText(), "");
    }

    private void updateConflictDiffHint(String diff, String status) {
        boolean conflict = "conflict".equalsIgnoreCase(status)
                || (diff != null && diff.contains("<<<<<<<"));
        if (conflict) {
            conflictDiffLabel.setText(I18n.get("quickNote.git.conflictMarkersHint"));
            conflictDiffLabel.setVisible(true);
        } else {
            conflictDiffLabel.setText(" ");
            conflictDiffLabel.setVisible(false);
        }
    }

    private void refreshGitLog() {
        gitLogTextArea.setText(QuickNoteGitLog.getText());
        gitLogTextArea.setCaretPosition(gitLogTextArea.getDocument().getLength());
    }

    private String currentNotePath() {
        if (StringUtils.isNotBlank(QuickNoteListener.selectedPath)) {
            return QuickNoteListener.selectedPath;
        }
        QuickNoteGitModifiedFile selectedChange = changesList.getSelectedValue();
        return selectedChange != null ? selectedChange.getPath() : "";
    }

    private void refreshHistory() {
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        historyModel.clear();
        if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
            return;
        }
        boolean scopeCurrent = historyScopeCurrentCheckBox.isSelected();
        String path = scopeCurrent ? currentNotePath() : "";
        if (scopeCurrent && StringUtils.isBlank(path)) {
            historyScopeCurrentCheckBox.setSelected(false);
            path = "";
        }
        List<QuickNoteGitCommit> history = StringUtils.isNotBlank(path)
                ? QuickNoteGitUtil.listFileHistory(vaultDir, path, 50)
                : QuickNoteGitUtil.listVaultHistory(vaultDir, 50);
        for (QuickNoteGitCommit commit : history) {
            historyModel.addElement(commit);
        }
        diffTextArea.setText("");
    }

    private void refreshAll() {
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        QuickNoteGitStatus status = QuickNoteGitUtil.getStatus(vaultDir);
        boolean gitRepo = status.isGitRepo();
        boolean hasRemote = status.isHasRemote();
        if (!gitRepo) {
            statusLabel.setText(I18n.get("quickNote.git.statusNoRepo"));
        } else {
            statusLabel.setText(I18n.format("quickNote.git.statusDetail",
                    vaultDir.getAbsolutePath(),
                    StringUtils.defaultIfBlank(status.getBranch(), "-"),
                    status.getChangedCount(),
                    status.getConflictCount(),
                    status.getAhead(),
                    status.getBehind()));
        }

        if (status.getConflictCount() > 0) {
            conflictLabel.setText(I18n.format("quickNote.git.conflicts", status.getConflictCount()));
            conflictLabel.setVisible(true);
        } else {
            conflictLabel.setText(" ");
            conflictLabel.setVisible(false);
        }

        changesModel.clear();
        for (QuickNoteGitModifiedFile file : QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir)) {
            changesModel.addElement(file);
        }

        refreshHistory();

        String remote = StringUtils.defaultIfBlank(QuickNoteGitUtil.getRemoteUrl(vaultDir),
                App.config.getQuickNoteGitRemoteUrl());
        remoteUrlField.setText(remote);

        if (gitRepo) {
            commitMessageField.setText(QuickNoteGitCheckpoint.buildCommitMessage(vaultDir));
        }

        diffTextArea.setText("");
        updateConflictDiffHint("", "");
        commitButton.setEnabled(gitRepo);
        discardButton.setEnabled(gitRepo);
        abortMergeButton.setEnabled(gitRepo && (status.isMerging() || status.getConflictCount() > 0));
        openChangeFileButton.setEnabled(gitRepo);
        openConflictsButton.setEnabled(gitRepo && status.getConflictCount() > 0);
        fetchButton.setEnabled(gitRepo && hasRemote);
        pullButton.setEnabled(gitRepo && hasRemote);
        pushButton.setEnabled(gitRepo && hasRemote);
        initGitButton.setEnabled(!gitRepo);
        refreshGitLog();
        QuickNoteForm.updateGitButtonStatus();
    }

    private void discardSelectedChange() {
        QuickNoteGitModifiedFile selected = changesList.getSelectedValue();
        if (selected == null) {
            MsgUtil.info(this, "quickNote.git.selectChangeFirst");
            return;
        }
        int confirm = MsgUtil.confirm(this, "quickNote.git.confirmDiscard");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        final String path = selected.getPath();
        final String statusCode = selected.getStatusCode();
        QuickNoteVaultRefreshCoordinator.beginDiscard(path);
        if (QuickNoteForm.quickNoteRSyntaxTextViewerManager != null) {
            QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(path);
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText(I18n.get("quickNote.git.discarding"));
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return QuickNoteGitUtil.discardWorkingChanges(
                        QuickNoteVaultUtil.getVaultDir(), path, statusCode);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        statusLabel.setText(I18n.get("common.success"));
                        QuickNoteForm.reloadNoteAfterDiscard(path);
                        refreshAll();
                        QuickNoteVaultRefreshCoordinator.refreshAfterExternalChange(true);
                    } else {
                        showGitFailure(result.getMessage());
                        refreshAll();
                    }
                } catch (Exception ex) {
                    showGitFailure(ex.getMessage());
                } finally {
                    QuickNoteVaultRefreshCoordinator.endDiscard();
                }
            }
        }.execute();
    }

    private void showGitFailure(String detail) {
        String message = StringUtils.defaultIfBlank(detail, I18n.get("quickNote.git.discardFailed"));
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, I18n.get("common.failure"), JOptionPane.ERROR_MESSAGE);
    }

    private void runGitTask(String pendingMessage, GitTask task) {
        runGitTask(pendingMessage, task, false);
    }

    private void runGitTask(String pendingMessage, GitTask task, boolean forceReloadEditor) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText(pendingMessage);
        SwingWorker<QuickNoteGitUtil.GitCommandResult, Void> worker = new SwingWorker<>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return task.run();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        statusLabel.setText(I18n.get("common.success"));
                        refreshAll();
                        QuickNoteVaultRefreshCoordinator.refreshAfterExternalChange(forceReloadEditor);
                    } else {
                        showGitFailure(result.getMessage());
                        refreshAll();
                    }
                } catch (Exception ex) {
                    showGitFailure(ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyTexts() {
        commitButton.setText(I18n.get("quickNote.git.commit"));
        discardButton.setText(I18n.get("quickNote.git.discard"));
        abortMergeButton.setText(I18n.get("quickNote.git.abortMerge"));
        openChangeFileButton.setText(I18n.get("quickNote.git.openFile"));
        openConflictsButton.setText(I18n.get("quickNote.git.openConflicts"));
        fetchButton.setText(I18n.get("quickNote.git.fetch"));
        pullButton.setText(I18n.get("quickNote.git.pull"));
        pushButton.setText(I18n.get("quickNote.git.push"));
        initGitButton.setText(I18n.get("quickNote.git.initRepo"));
        saveRemoteButton.setText(I18n.get("common.save"));
        openVaultButton.setText(I18n.get("quickNote.git.openVault"));
        clearLogButton.setText(I18n.get("quickNote.git.clearLog"));
        historyScopeCurrentCheckBox.setText(I18n.get("quickNote.git.historyScopeCurrent"));
    }

    public static void showDialog() {
        if (instance == null || !instance.isDisplayable()) {
            instance = new QuickNoteGitDialog();
        } else {
            instance.applyTheme();
        }
        instance.refreshAll();
        instance.setVisible(true);
        instance.toFront();
        instance.requestFocus();
    }

    @FunctionalInterface
    private interface GitTask {
        QuickNoteGitUtil.GitCommandResult run();
    }
}
