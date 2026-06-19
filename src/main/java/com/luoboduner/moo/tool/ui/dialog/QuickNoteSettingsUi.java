package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultWatcher;
import com.luoboduner.moo.tool.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 设置页「随手记」Vault 与 Git 相关配置 UI。
 */
public final class QuickNoteSettingsUi {

    private final JTextField vaultPathTextField = new JTextField();
    private final JButton vaultBrowseButton = new JButton("…");
    private final JButton vaultSaveButton = new JButton();
    private final JCheckBox autoGitCommitCheckBox = new JCheckBox();
    private final JTextField gitRemoteTextField = new JTextField();
    private final JButton gitRemoteSaveButton = new JButton();
    private final JButton openVaultButton = new JButton();
    private final JButton resetVaultPathButton = new JButton();
    private final JButton openGitPanelButton = new JButton();

    private QuickNoteSettingsUi() {
    }

    public static void install(JPanel quickNotePanel, JComponent hostDialog) {
        new QuickNoteSettingsUi().attach(quickNotePanel, hostDialog);
    }

    private void attach(JPanel quickNotePanel, JComponent hostDialog) {
        rebuildQuickNotePanel(quickNotePanel, hostDialog);
        loadValues();
        bindActions(hostDialog);
        applyTexts();
    }

    private void rebuildQuickNotePanel(JPanel quickNotePanel, JComponent hostDialog) {
        Component[] children = quickNotePanel.getComponents();
        quickNotePanel.removeAll();
        quickNotePanel.setLayout(new BorderLayout(0, 10));

        JPanel topPanel = new JPanel(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        for (Component child : children) {
            topPanel.add(child, new GridConstraints(
                    0, topPanel.getComponentCount(), 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        }
        quickNotePanel.add(topPanel, BorderLayout.NORTH);
        quickNotePanel.add(buildExtraPanel(hostDialog), BorderLayout.CENTER);
    }

    private JPanel buildExtraPanel(JComponent hostDialog) {
        JPanel panel = new JPanel(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));

        panel.add(label("setting.quickNote.vaultPath"), row(0, 0));
        panel.add(vaultPathTextField, row(0, 1));
        panel.add(vaultBrowseButton, row(0, 2));

        panel.add(label("setting.quickNote.autoGitCommit"), row(1, 0));
        panel.add(autoGitCommitCheckBox, rowSpan(1, 1, 2));

        panel.add(label("setting.quickNote.gitRemote"), row(2, 0));
        panel.add(gitRemoteTextField, row(2, 1));
        panel.add(gitRemoteSaveButton, row(2, 2));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        vaultSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        gitRemoteSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        openGitPanelButton.setIcon(new FlatSVGIcon("icon/diff.svg"));
        openVaultButton.setIcon(new FlatSVGIcon("icon/list.svg"));
        actionPanel.add(vaultSaveButton);
        actionPanel.add(openVaultButton);
        actionPanel.add(resetVaultPathButton);
        actionPanel.add(openGitPanelButton);
        panel.add(actionPanel, rowSpan(3, 0, 3));
        return panel;
    }

    private void loadValues() {
        String configured = App.config.getQuickNoteVaultPath();
        if (StringUtils.isBlank(configured)) {
            configured = SystemUtil.CONFIG_HOME + File.separator + QuickNoteVaultUtil.VAULT_DIR_NAME;
        }
        vaultPathTextField.setText(configured);
        autoGitCommitCheckBox.setSelected(App.config.isQuickNoteAutoGitCommit());
        gitRemoteTextField.setText(App.config.getQuickNoteGitRemoteUrl());
    }

    private void bindActions(JComponent hostDialog) {
        vaultBrowseButton.addActionListener(e -> {
            SystemFileChooser fileChooser = new SystemFileChooser(vaultPathTextField.getText());
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(hostDialog) == SystemFileChooser.APPROVE_OPTION) {
                vaultPathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        vaultSaveButton.addActionListener(e -> {
            String vaultPath = vaultPathTextField.getText().trim();
            App.config.setQuickNoteVaultPath(vaultPath);
            App.config.setQuickNoteAutoGitCommit(autoGitCommitCheckBox.isSelected());
            App.config.save();
            QuickNoteVaultUtil.resetVaultCache();
            QuickNoteVaultUtil.ensureVaultReady();
            QuickNoteVaultWatcher.restart();
            QuickNoteForm.initNoteList();
            QuickNoteForm.updateGitButtonStatus();
            AlertUtil.buttonInfo(vaultSaveButton, I18n.get("common.save"), I18n.get("common.saveSuccess"), 2000);
        });

        gitRemoteSaveButton.addActionListener(e -> {
            String remoteUrl = gitRemoteTextField.getText().trim();
            App.config.setQuickNoteGitRemoteUrl(remoteUrl);
            App.config.save();
            QuickNoteGitUtil.configureRemote(QuickNoteVaultUtil.getVaultDir(), remoteUrl);
            AlertUtil.buttonInfo(gitRemoteSaveButton, I18n.get("common.save"), I18n.get("common.saveSuccess"), 2000);
        });

        openGitPanelButton.addActionListener(e -> QuickNoteGitDialog.showDialog());
        openVaultButton.addActionListener(e -> QuickNoteVaultUtil.openVaultDir());
        resetVaultPathButton.addActionListener(e -> vaultPathTextField.setText(QuickNoteVaultUtil.getDefaultVaultPath()));
    }

    private void applyTexts() {
        autoGitCommitCheckBox.setText(I18n.get("setting.quickNote.autoGitCommit"));
        vaultSaveButton.setText(I18n.get("setting.quickNote.saveVault"));
        gitRemoteSaveButton.setText(I18n.get("common.save"));
        openGitPanelButton.setText(I18n.get("setting.quickNote.openGitPanel"));
        openVaultButton.setText(I18n.get("setting.quickNote.openVault"));
        resetVaultPathButton.setText(I18n.get("setting.quickNote.resetVaultPath"));
    }

    private static JLabel label(String key) {
        return new JLabel(I18n.get(key));
    }

    private static GridConstraints row(int row, int col) {
        return new GridConstraints(row, col, 1, 1, GridConstraints.ANCHOR_WEST,
                col == 1 ? GridConstraints.FILL_HORIZONTAL : GridConstraints.FILL_NONE,
                col == 1 ? GridConstraints.SIZEPOLICY_WANT_GROW : GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false);
    }

    private static GridConstraints rowSpan(int row, int col, int width) {
        return new GridConstraints(row, col, 1, width, GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false);
    }
}
