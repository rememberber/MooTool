package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.JsonBeautyAutoPullScheduler;
import com.luoboduner.moo.tool.util.JsonBeautyVaultUtil;
import com.luoboduner.moo.tool.util.JsonBeautyVaultWatcher;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * JSON Vault 与 Git 相关配置 UI。
 */
public final class JsonBeautySettingsUi {

    private static final int MIN_IDLE_SECONDS = 5;
    private static final int MAX_IDLE_SECONDS = 3600;
    private static final int MIN_INACTIVE_SECONDS = 10;
    private static final int MAX_INACTIVE_SECONDS = 7200;
    private static final int MAX_AUTO_PULL_MINUTES = 1440;

    private final JTextField vaultPathTextField = new JTextField();
    private final JButton vaultBrowseButton = new JButton("…");
    private final JButton vaultSaveButton = new JButton();
    private final JCheckBox autoGitCommitCheckBox = new JCheckBox();
    private final JCheckBox hideGitignoredCheckBox = new JCheckBox();
    private final JLabel autoGitCommitHintLabel = new JLabel();
    private final JSpinner autoGitIdleSpinner = new JSpinner(
            new SpinnerNumberModel(30, MIN_IDLE_SECONDS, MAX_IDLE_SECONDS, 5));
    private final JSpinner autoGitInactiveSpinner = new JSpinner(
            new SpinnerNumberModel(120, MIN_INACTIVE_SECONDS, MAX_INACTIVE_SECONDS, 10));
    private final JSpinner autoPullIntervalSpinner = new JSpinner(
            new SpinnerNumberModel(5, 0, MAX_AUTO_PULL_MINUTES, 1));
    private final JLabel autoPullHintLabel = new JLabel();
    private final JTextField gitRemoteTextField = new JTextField();
    private final JButton gitRemoteSaveButton = new JButton();
    private final JButton openVaultButton = new JButton();
    private final JButton resetVaultPathButton = new JButton();
    private final JButton openGitPanelButton = new JButton();

    private JsonBeautySettingsUi() {
    }

    public static void showDialog() {
        JsonBeautySettingsUi ui = new JsonBeautySettingsUi();
        JDialog dialog = new JDialog(App.mainFrame, I18n.get("setting.jsonBeauty.vaultSettingsTitle"), true);
        ComponentUtil.setPreferSizeAndLocateToCenter(dialog, 0.58, 0.68);
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(ui.buildExtraPanel(dialog), BorderLayout.CENTER);
        ui.loadValues();
        ui.bindActions(dialog);
        ui.applyTexts();
        ui.updateAutoGitControlsEnabled();
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private JPanel buildExtraPanel(Component hostDialog) {
        JPanel panel = new JPanel(new GridLayoutManager(8, 3, new Insets(0, 0, 0, 0), -1, -1));

        panel.add(label("setting.jsonBeauty.vaultPath"), row(0, 0));
        panel.add(vaultPathTextField, row(0, 1));
        panel.add(vaultBrowseButton, row(0, 2));

        panel.add(label("setting.jsonBeauty.autoGitCommit"), row(1, 0));
        JPanel autoGitPanel = new JPanel(new BorderLayout(0, 4));
        autoGitPanel.add(autoGitCommitCheckBox, BorderLayout.NORTH);
        autoGitCommitHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        autoGitPanel.add(autoGitCommitHintLabel, BorderLayout.CENTER);
        panel.add(autoGitPanel, rowSpan(1, 1, 2));

        panel.add(label("setting.jsonBeauty.autoGitIdleSeconds"), row(2, 0));
        panel.add(autoGitIdleSpinner, row(2, 1));

        panel.add(label("setting.jsonBeauty.autoGitInactiveSeconds"), row(3, 0));
        panel.add(autoGitInactiveSpinner, row(3, 1));

        panel.add(label("setting.jsonBeauty.autoPullIntervalMinutes"), row(4, 0));
        JPanel autoPullPanel = new JPanel(new BorderLayout(0, 4));
        autoPullPanel.add(autoPullIntervalSpinner, BorderLayout.NORTH);
        autoPullHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        autoPullPanel.add(autoPullHintLabel, BorderLayout.CENTER);
        panel.add(autoPullPanel, rowSpan(4, 1, 2));

        panel.add(label("setting.jsonBeauty.hideGitignoredFiles"), row(5, 0));
        JPanel hideGitignoredPanel = new JPanel(new BorderLayout(0, 4));
        hideGitignoredPanel.add(hideGitignoredCheckBox, BorderLayout.NORTH);
        JLabel hideGitignoredHintLabel = new JLabel(I18n.get("setting.jsonBeauty.hideGitignoredFilesHint"));
        hideGitignoredHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hideGitignoredPanel.add(hideGitignoredHintLabel, BorderLayout.CENTER);
        panel.add(hideGitignoredPanel, rowSpan(5, 1, 2));

        panel.add(label("setting.jsonBeauty.gitRemote"), row(6, 0));
        panel.add(gitRemoteTextField, row(6, 1));
        panel.add(gitRemoteSaveButton, row(6, 2));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        vaultSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        gitRemoteSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        openGitPanelButton.setIcon(new FlatSVGIcon("icon/diff.svg"));
        openVaultButton.setIcon(new FlatSVGIcon("icon/list.svg"));
        actionPanel.add(vaultSaveButton);
        actionPanel.add(openVaultButton);
        actionPanel.add(resetVaultPathButton);
        actionPanel.add(openGitPanelButton);
        panel.add(actionPanel, rowSpan(7, 0, 3));
        return panel;
    }

    private void loadValues() {
        String configured = App.config.getJsonBeautyVaultPath();
        if (StringUtils.isBlank(configured)) {
            configured = JsonBeautyVaultUtil.getDefaultVaultPath();
        }
        vaultPathTextField.setText(configured);
        autoGitCommitCheckBox.setSelected(App.config.isJsonBeautyAutoGitCommit());
        hideGitignoredCheckBox.setSelected(App.config.isJsonBeautyHideGitignoredFiles());
        gitRemoteTextField.setText(App.config.getJsonBeautyGitRemoteUrl());
        autoGitIdleSpinner.setValue(clamp(
                App.config.getJsonBeautyAutoGitIdleSeconds(), MIN_IDLE_SECONDS, MAX_IDLE_SECONDS));
        autoGitInactiveSpinner.setValue(clamp(
                App.config.getJsonBeautyAutoGitInactiveSeconds(), MIN_INACTIVE_SECONDS, MAX_INACTIVE_SECONDS));
        autoPullIntervalSpinner.setValue(clamp(
                App.config.getJsonBeautyAutoPullIntervalMinutes(), 0, MAX_AUTO_PULL_MINUTES));
    }

    private void bindActions(Component hostDialog) {
        autoGitCommitCheckBox.addActionListener(e -> updateAutoGitControlsEnabled());

        vaultBrowseButton.addActionListener(e -> {
            SystemFileChooser fileChooser = new SystemFileChooser(vaultPathTextField.getText());
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(hostDialog) == SystemFileChooser.APPROVE_OPTION) {
                vaultPathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        vaultSaveButton.addActionListener(e -> saveVaultSettings());

        gitRemoteSaveButton.addActionListener(e -> {
            String remoteUrl = gitRemoteTextField.getText().trim();
            App.config.setJsonBeautyGitRemoteUrl(remoteUrl);
            App.config.save();
            QuickNoteGitUtil.configureRemote(JsonBeautyVaultUtil.getVaultDir(), remoteUrl);
            AlertUtil.buttonInfo(gitRemoteSaveButton, I18n.get("common.save"), I18n.get("common.saveSuccess"), 2000);
        });

        openGitPanelButton.addActionListener(e -> JsonBeautyGitDialog.showDialog(openGitPanelButton));
        openVaultButton.addActionListener(e -> JsonBeautyVaultUtil.openVaultDir());
        resetVaultPathButton.addActionListener(e -> vaultPathTextField.setText(JsonBeautyVaultUtil.getDefaultVaultPath()));
    }

    private void saveVaultSettings() {
        String vaultPath = vaultPathTextField.getText().trim();
        App.config.setJsonBeautyVaultPath(vaultPath);
        App.config.setJsonBeautyAutoGitCommit(autoGitCommitCheckBox.isSelected());
        App.config.setJsonBeautyHideGitignoredFiles(hideGitignoredCheckBox.isSelected());
        App.config.setJsonBeautyAutoGitIdleSeconds(spinnerIntValue(autoGitIdleSpinner));
        App.config.setJsonBeautyAutoGitInactiveSeconds(spinnerIntValue(autoGitInactiveSpinner));
        App.config.setJsonBeautyAutoPullIntervalMinutes(spinnerIntValue(autoPullIntervalSpinner));
        App.config.save();

        JsonBeautyVaultUtil.resetVaultCache();
        JsonBeautyVaultUtil.ensureVaultReady();
        JsonBeautyVaultWatcher.restart();
        JsonBeautyAutoPullScheduler.onSettingsChanged();
        JsonBeautyForm.refreshList();
        JsonBeautyForm.updateGitButtonStatus();
        AlertUtil.buttonInfo(vaultSaveButton, I18n.get("common.save"), I18n.get("common.saveSuccess"), 2000);
    }

    private void updateAutoGitControlsEnabled() {
        boolean enabled = autoGitCommitCheckBox.isSelected();
        autoGitIdleSpinner.setEnabled(enabled);
        autoGitInactiveSpinner.setEnabled(enabled);
    }

    private void applyTexts() {
        autoGitCommitCheckBox.setText(I18n.get("setting.jsonBeauty.autoGitCommit"));
        hideGitignoredCheckBox.setText(I18n.get("setting.jsonBeauty.hideGitignoredFiles"));
        autoGitCommitHintLabel.setText(I18n.get("setting.jsonBeauty.autoGitCommitHint"));
        autoPullHintLabel.setText(I18n.get("setting.jsonBeauty.autoPullIntervalHint"));
        vaultSaveButton.setText(I18n.get("setting.jsonBeauty.saveVault"));
        gitRemoteSaveButton.setText(I18n.get("common.save"));
        openGitPanelButton.setText(I18n.get("setting.jsonBeauty.openGitPanel"));
        openVaultButton.setText(I18n.get("setting.jsonBeauty.openVault"));
        resetVaultPathButton.setText(I18n.get("setting.jsonBeauty.resetVaultPath"));
    }

    private static int spinnerIntValue(JSpinner spinner) {
        Object value = spinner.getValue();
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
