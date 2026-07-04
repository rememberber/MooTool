package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.I18n;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Vault Git HTTPS 凭据配置（随手记与 JSON Vault 共用）。
 */
public final class VaultGitCredentialUi {

    private final JTextField usernameField = new JTextField();
    private final JPasswordField tokenField = new JPasswordField();
    private final JLabel tokenHintLabel = new JLabel();
    private final JButton saveButton = new JButton();

    public void addRows(JPanel panel, int usernameRow, int tokenRow) {
        panel.add(new JLabel(I18n.get("setting.vaultGit.username")), row(usernameRow, 0));
        panel.add(usernameField, row(usernameRow, 1));

        panel.add(new JLabel(I18n.get("setting.vaultGit.token")), row(tokenRow, 0));
        JPanel tokenPanel = new JPanel(new BorderLayout(0, 4));
        tokenPanel.add(tokenField, BorderLayout.NORTH);
        tokenHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        tokenPanel.add(tokenHintLabel, BorderLayout.CENTER);
        panel.add(tokenPanel, rowSpan(tokenRow, 1, 2));

        saveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        panel.add(saveButton, row(tokenRow, 2));
    }

    public void loadValues() {
        usernameField.setText(App.config.getVaultGitUsername());
        tokenField.setText("");
    }

    public void bindActions() {
        saveButton.addActionListener(e -> saveCredentials());
    }

    public void applyTexts() {
        tokenHintLabel.setText(I18n.get("setting.vaultGit.tokenHint"));
        saveButton.setText(I18n.get("common.save"));
    }

    public void saveCredentials() {
        App.config.setVaultGitUsername(usernameField.getText().trim());
        char[] tokenChars = tokenField.getPassword();
        if (tokenChars != null && tokenChars.length > 0) {
            App.config.setVaultGitToken(new String(tokenChars));
        }
        App.config.save();
        tokenField.setText("");
        AlertUtil.buttonInfo(saveButton, I18n.get("common.save"), I18n.get("common.saveSuccess"), 2000);
    }

    public void saveCredentialsIfTokenProvided() {
        String username = usernameField.getText().trim();
        if (StringUtils.isNotBlank(username)) {
            App.config.setVaultGitUsername(username);
        }
        char[] tokenChars = tokenField.getPassword();
        if (tokenChars != null && tokenChars.length > 0) {
            App.config.setVaultGitToken(new String(tokenChars));
            tokenField.setText("");
        }
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
