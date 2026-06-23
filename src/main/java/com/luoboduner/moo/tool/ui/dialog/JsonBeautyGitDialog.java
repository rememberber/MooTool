package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.*;
import com.luoboduner.moo.tool.ui.component.QuickNoteGitDiffPanel;
import com.luoboduner.moo.tool.ui.component.SplitPaneUtil;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.util.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * JSON Vault Git 变更、历史与远程同步面板。
 */
public class JsonBeautyGitDialog extends JDialog {

    private static JsonBeautyGitDialog instance;

    private static final int TAB_CHANGES = 0;
    private static final int TAB_HISTORY = 1;
    private static final double MAIN_SPLIT_TOP_RATIO = 0.62;

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
    private final QuickNoteGitDiffPanel diffPanel = new QuickNoteGitDiffPanel();
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
    private JTabbedPane tabbedPane;

    private int diffRequestSeq;
    private boolean suppressListDiffEvents;

    public JsonBeautyGitDialog() {
        this(App.mainFrame);
    }

    public JsonBeautyGitDialog(Window owner) {
        super(owner, I18n.get("quickNote.git.title"), ModalityType.MODELESS);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.72, 0.72);
        setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(content, BorderLayout.CENTER);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(I18n.get("quickNote.git.tab.changes"), buildChangesPanel());
        tabbedPane.addTab(I18n.get("quickNote.git.tab.history"), buildHistoryPanel());
        tabbedPane.addTab(I18n.get("quickNote.git.tab.sync"), buildSyncPanel());
        tabbedPane.addTab(I18n.get("quickNote.git.tab.log"), buildLogPanel());
        tabbedPane.addChangeListener(this::onTabChanged);

        JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        conflictDiffLabel.setVisible(false);
        southPanel.add(conflictDiffLabel, BorderLayout.NORTH);
        southPanel.add(diffPanel, BorderLayout.CENTER);
        southPanel.setMinimumSize(new Dimension(0, 80));
        tabbedPane.setMinimumSize(new Dimension(0, 120));

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, southPanel);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setDividerSize(6);
        mainSplitPane.setResizeWeight(MAIN_SPLIT_TOP_RATIO);
        SplitPaneUtil.setDividerProportion(mainSplitPane, MAIN_SPLIT_TOP_RATIO);
        content.add(mainSplitPane, BorderLayout.CENTER);

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

        diffPanel.applyTheme();
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
        commitMessageField.setText(JsonBeautyGitCheckpoint.buildCommitMessage(JsonBeautyVaultUtil.getVaultDir()));
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
        historyScopeCurrentCheckBox.setSelected(StringUtils.isNotBlank(JsonBeautyListener.selectedPathJson));
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
        changesList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || suppressListDiffEvents) {
                return;
            }
            if (tabbedPane.getSelectedIndex() == TAB_CHANGES) {
                refreshDiffForSelectedChange();
            }
        });
        historyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || suppressListDiffEvents) {
                return;
            }
            if (tabbedPane.getSelectedIndex() == TAB_HISTORY) {
                refreshDiffForSelectedHistory();
            }
        });
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
                TJsonBeauty note = JsonBeautyVaultUtil.loadByPath(selected.getPath());
                if (note != null) {
                    JsonBeautyForm.selectJsonInList(note.getRelativePath());
                    JsonBeautyForm.showJson(note);
                }
            }
        });

        openVaultButton.addActionListener(e -> JsonBeautyVaultUtil.openVaultDir());
        refreshButton.addActionListener(e -> refreshAll());
        clearLogButton.addActionListener(e -> {
            QuickNoteGitLog.clear();
            refreshGitLog();
        });

        commitButton.addActionListener(e -> {
            if (!flushCurrentNoteBeforeGitAction()) {
                return;
            }
            runGitTask(I18n.get("quickNote.git.committing"), () -> {
                File vaultDir = JsonBeautyVaultUtil.getVaultDir();
                String message = StringUtils.defaultIfBlank(commitMessageField.getText(),
                        JsonBeautyGitCheckpoint.buildCommitMessage(vaultDir));
                return QuickNoteGitUtil.commitAndPush(vaultDir, message);
            }, false);
        });

        discardButton.addActionListener(e -> discardSelectedChange());

        abortMergeButton.addActionListener(e -> {
            int confirm = MsgUtil.confirm(this, "quickNote.git.confirmAbortMerge");
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            runGitTask(I18n.get("quickNote.git.abortingMerge"), () ->
                    QuickNoteGitUtil.abortMerge(JsonBeautyVaultUtil.getVaultDir()), true);
        });

        openChangeFileButton.addActionListener(e -> openSelectedChangeInFileManager());
        openConflictsButton.addActionListener(e -> JsonBeautyConflictResolverDialog.showDialog());

        saveRemoteButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.savingRemote"), () -> {
            String remoteUrl = remoteUrlField.getText().trim();
            App.config.setJsonBeautyGitRemoteUrl(remoteUrl);
            App.config.save();
            return QuickNoteGitUtil.configureRemote(JsonBeautyVaultUtil.getVaultDir(), remoteUrl);
        }, false));

        fetchButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.fetching"), () ->
                QuickNoteGitUtil.fetch(JsonBeautyVaultUtil.getVaultDir()), false));

        pullButton.addActionListener(e -> runPullTask());

        pushButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.pushing"), () ->
                QuickNoteGitUtil.push(JsonBeautyVaultUtil.getVaultDir()), false));

        initGitButton.addActionListener(e -> runGitTask(I18n.get("quickNote.git.initializing"), () -> {
            QuickNoteGitUtil.initRepoIfNeeded(JsonBeautyVaultUtil.getVaultDir());
            String remoteUrl = remoteUrlField.getText().trim();
            if (StringUtils.isNotBlank(remoteUrl)) {
                return QuickNoteGitUtil.configureRemote(JsonBeautyVaultUtil.getVaultDir(), remoteUrl);
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
        JsonBeautyVaultUtil.revealInFileManager(selected.getPath());
    }

    private void openConflictFilesInFileManager() {
        List<String> conflicts = QuickNoteGitUtil.listConflictFiles(JsonBeautyVaultUtil.getVaultDir());
        if (conflicts.isEmpty()) {
            MsgUtil.info(this, "quickNote.git.noConflicts");
            return;
        }
        if (conflicts.size() == 1) {
            JsonBeautyVaultUtil.revealInFileManager(conflicts.get(0));
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
                JsonBeautyVaultUtil.revealInFileManager(selected);
            }
        }
    }

    private void onTabChanged(ChangeEvent event) {
        int tab = tabbedPane.getSelectedIndex();
        if (tab == TAB_CHANGES) {
            refreshDiffForSelectedChange();
        } else if (tab == TAB_HISTORY) {
            refreshDiffForSelectedHistory();
        } else {
            clearDiffDisplay();
        }
    }

    private void refreshDiffForSelectedChange() {
        QuickNoteGitModifiedFile selected = changesList.getSelectedValue();
        if (selected == null) {
            clearDiffDisplay();
            return;
        }
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        String path = selected.getPath();
        String statusLabel = selected.getStatusLabel();
        loadDiffAsync(() -> QuickNoteGitDiffHelper.buildWorkingDiff(vaultDir, path), statusLabel);
    }

    private void refreshDiffForSelectedHistory() {
        QuickNoteGitCommit selected = historyList.getSelectedValue();
        if (selected == null) {
            clearDiffDisplay();
            return;
        }
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        String path = historyScopeCurrentCheckBox.isSelected() ? currentNotePath() : "";
        loadDiffAsync(() -> QuickNoteGitDiffHelper.buildCommitDiff(vaultDir, path, selected.getHash(), selected), "");
    }

    private void loadDiffAsync(java.util.function.Supplier<QuickNoteGitDiffHelper.Result> supplier,
                               String conflictStatus) {
        int requestId = ++diffRequestSeq;
        new SwingWorker<QuickNoteGitDiffHelper.Result, Void>() {
            @Override
            protected QuickNoteGitDiffHelper.Result doInBackground() {
                return supplier.get();
            }

            @Override
            protected void done() {
                if (requestId != diffRequestSeq) {
                    return;
                }
                try {
                    QuickNoteGitDiffHelper.Result result = get();
                    displayDiff(result);
                    updateConflictDiffHint(result.conflictProbe(), conflictStatus);
                } catch (Exception e) {
                    clearDiffDisplay();
                    diffPanel.renderPlain(e.getMessage());
                }
            }
        }.execute();
    }

    private void displayDiff(QuickNoteGitDiffHelper.Result result) {
        if (result == null) {
            clearDiffDisplay();
            return;
        }
        if (result.sideBySide()) {
            diffPanel.renderSideBySide(result.oldText(), result.newText(), result.uiDiff());
        } else {
            diffPanel.renderPlain(result.text());
        }
    }

    private void clearDiffDisplay() {
        diffRequestSeq++;
        diffPanel.clear();
        updateConflictDiffHint("", "");
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
        if (StringUtils.isNotBlank(JsonBeautyListener.selectedPathJson)) {
            return JsonBeautyListener.selectedPathJson;
        }
        QuickNoteGitModifiedFile selectedChange = changesList.getSelectedValue();
        return selectedChange != null ? selectedChange.getPath() : "";
    }

    private void refreshHistory() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        QuickNoteGitCommit previousSelection = historyList.getSelectedValue();
        String previousHash = previousSelection != null ? previousSelection.getHash() : null;

        suppressListDiffEvents = true;
        try {
            historyModel.clear();
            if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
                if (previousHash == null) {
                    clearDiffDisplay();
                }
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
            int restoredIndex = -1;
            for (int i = 0; i < history.size(); i++) {
                QuickNoteGitCommit commit = history.get(i);
                historyModel.addElement(commit);
                if (previousHash != null && previousHash.equals(commit.getHash())) {
                    restoredIndex = i;
                }
            }
            if (restoredIndex >= 0) {
                historyList.setSelectedIndex(restoredIndex);
            } else if (previousHash != null) {
                clearDiffDisplay();
            }
        } finally {
            suppressListDiffEvents = false;
        }
    }

    private void refreshChangesList() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        suppressListDiffEvents = true;
        try {
            changesModel.clear();
            for (QuickNoteGitModifiedFile file : QuickNoteGitUtil.listModifiedFilesDetailed(vaultDir)) {
                changesModel.addElement(file);
            }
        } finally {
            suppressListDiffEvents = false;
        }
    }

    private void refreshChangesAndStatus() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        QuickNoteGitStatus status = QuickNoteGitUtil.getStatus(vaultDir);
        applyStatusPresentation(status, vaultDir);
        refreshChangesList();
        applyGitActionStates(status);
    }

    private void applyStatusPresentation(QuickNoteGitStatus status, File vaultDir) {
        if (!status.isGitRepo()) {
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
    }

    private void applyGitActionStates(QuickNoteGitStatus status) {
        boolean gitRepo = status.isGitRepo();
        boolean hasRemote = status.isHasRemote();
        commitButton.setEnabled(gitRepo);
        discardButton.setEnabled(gitRepo);
        abortMergeButton.setEnabled(gitRepo && (status.isMerging() || status.getConflictCount() > 0));
        openChangeFileButton.setEnabled(gitRepo);
        openConflictsButton.setEnabled(gitRepo && status.getConflictCount() > 0);
        fetchButton.setEnabled(gitRepo && hasRemote);
        pullButton.setEnabled(gitRepo && hasRemote);
        pushButton.setEnabled(gitRepo && hasRemote);
        initGitButton.setEnabled(!gitRepo);
    }

    private void refreshAll() {
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        QuickNoteGitStatus status = QuickNoteGitUtil.getStatus(vaultDir);
        applyStatusPresentation(status, vaultDir);

        refreshChangesList();
        refreshHistory();

        String remote = StringUtils.defaultIfBlank(QuickNoteGitUtil.getRemoteUrl(vaultDir),
                App.config.getJsonBeautyGitRemoteUrl());
        remoteUrlField.setText(remote);

        if (status.isGitRepo()) {
            commitMessageField.setText(JsonBeautyGitCheckpoint.buildCommitMessage(vaultDir));
        }

        applyGitActionStates(status);
        refreshGitLog();
        JsonBeautyForm.updateGitButtonStatus();
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
        JsonBeautyVaultRefreshCoordinator.beginDiscard(path);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText(I18n.get("quickNote.git.discarding"));
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return QuickNoteGitUtil.discardWorkingChanges(
                        JsonBeautyVaultUtil.getVaultDir(), path, statusCode);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        statusLabel.setText(I18n.get("common.success"));
                        JsonBeautyForm.reloadJsonAfterDiscard(path);
                        refreshAll();
                        JsonBeautyVaultRefreshCoordinator.refreshAfterExternalChange(true);
                    } else {
                        showGitFailure(result.getMessage());
                        refreshAll();
                    }
                } catch (Exception ex) {
                    showGitFailure(ex.getMessage());
                } finally {
                    JsonBeautyVaultRefreshCoordinator.endDiscard();
                }
            }
        }.execute();
    }

    private void showGitFailure(String detail) {
        String message = StringUtils.defaultIfBlank(detail, I18n.get("quickNote.git.discardFailed"));
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, I18n.get("common.failure"), JOptionPane.ERROR_MESSAGE);
    }

    private void runPullTask() {
        if (!flushCurrentNoteBeforeGitAction()) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText(I18n.get("quickNote.git.pulling"));
        SwingWorker<QuickNoteGitPullResult, Void> worker = new SwingWorker<>() {
            @Override
            protected QuickNoteGitPullResult doInBackground() {
                return QuickNoteGitUtil.pullWithResult(JsonBeautyVaultUtil.getVaultDir());
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    QuickNoteGitPullResult result = get();
                    if (result.isSuccess()) {
                        statusLabel.setText(I18n.get("common.success"));
                        refreshAll();
                        JsonBeautyVaultRefreshCoordinator.refreshAfterPull(result);
                    } else if (result.getStatus() == QuickNoteGitPullResult.Status.CONFLICT) {
                        showGitFailure(I18n.get("quickNote.git.pullConflict"));
                        refreshAll();
                        JsonBeautyVaultRefreshCoordinator.refreshAfterPull(result);
                        JsonBeautyConflictResolverDialog.showDialog();
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

    private boolean flushCurrentNoteBeforeGitAction() {
        JsonBeautyListener.quickSaveSync(true);
        if (JsonBeautyVaultRefreshCoordinator.hasUnsavedChanges()) {
            MsgUtil.info(this, "quickNote.git.unsavedChanges");
            return false;
        }
        return true;
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
                        JsonBeautyVaultRefreshCoordinator.refreshAfterExternalChange(forceReloadEditor);
                    } else if (result.isPushRejected()) {
                        showGitFailure(I18n.get("quickNote.git.pushRejected"));
                        refreshAll();
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
        openConflictsButton.setText(I18n.get("quickNote.git.resolveConflicts"));
        fetchButton.setText(I18n.get("quickNote.git.fetch"));
        pullButton.setText(I18n.get("quickNote.git.pull"));
        pushButton.setText(I18n.get("quickNote.git.push"));
        initGitButton.setText(I18n.get("quickNote.git.initRepo"));
        saveRemoteButton.setText(I18n.get("common.save"));
        openVaultButton.setText(I18n.get("quickNote.git.openVault"));
        clearLogButton.setText(I18n.get("quickNote.git.clearLog"));
        historyScopeCurrentCheckBox.setText(I18n.get("quickNote.git.historyScopeCurrent"));
        diffPanel.updatePanelTitles();
    }

    public static void showDialog() {
        showDialog(App.mainFrame);
    }

    public static void showDialog(Component parent) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : App.mainFrame;
        if (owner == null) {
            owner = App.mainFrame;
        }
        if (instance == null || !instance.isDisplayable() || instance.getOwner() != owner) {
            if (instance != null && instance.isDisplayable()) {
                instance.dispose();
            }
            instance = new JsonBeautyGitDialog(owner);
        } else {
            instance.applyTheme();
        }
        instance.refreshAll();
        instance.setVisible(true);
        instance.toFront();
        instance.requestFocusInWindow();
        instance.requestFocus();
    }

    public static void refreshIfVisible() {
        if (instance != null && instance.isDisplayable() && instance.isVisible()) {
            instance.refreshChangesAndStatus();
        }
    }

    @FunctionalInterface
    private interface GitTask {
        QuickNoteGitUtil.GitCommandResult run();
    }
}
