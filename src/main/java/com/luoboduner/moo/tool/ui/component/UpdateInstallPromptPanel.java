package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.UpdateDownloadManager;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 左侧导航底部的「新版本已就绪」安装提示（对齐 Next 版 UpdateReadyAction）。
 */
@Slf4j
public class UpdateInstallPromptPanel extends JPanel {

    private static boolean i18nRegistered;

    private static final CopyOnWriteArrayList<UpdateInstallPromptPanel> INSTANCES = new CopyOnWriteArrayList<>();

    private final JButton actionButton = new JButton();
    private final JLabel titleLabel = new JLabel();
    private final JLabel versionLabel = new JLabel();
    private boolean installing;

    public UpdateInstallPromptPanel() {
        INSTANCES.add(this);
        setLayout(new BorderLayout());
        setOpaque(false);
        setVisible(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        actionButton.setLayout(new BorderLayout(8, 0));
        actionButton.setFocusable(false);
        actionButton.setMargin(new Insets(8, 10, 8, 10));
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D()));
        versionLabel.setFont(versionLabel.getFont().deriveFont(Math.max(10f, versionLabel.getFont().getSize2D() - 1f)));
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(versionLabel);

        actionButton.add(textPanel, BorderLayout.CENTER);
        add(actionButton, BorderLayout.CENTER);

        actionButton.addActionListener(e -> install());
        applyAccentStyle();
        applyI18n();

        UpdateDownloadManager.getInstance().addListener(this::refresh);
        if (!i18nRegistered) {
            I18nUiUtil.register(UpdateInstallPromptPanel::refreshAllI18n);
            i18nRegistered = true;
        }
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName()) || String.valueOf(e.getPropertyName()).contains("accent")) {
                SwingUtilities.invokeLater(this::applyAccentStyle);
            }
        });
    }

    private static void refreshAllI18n() {
        for (UpdateInstallPromptPanel panel : INSTANCES) {
            panel.applyI18n();
            panel.refresh(UpdateDownloadManager.getInstance());
        }
    }

    private void refresh(UpdateDownloadManager manager) {
        boolean ready = manager.getStatus() == UpdateDownloadManager.Status.READY
                && manager.getVersion() != null
                && manager.getDownloadedFile() != null
                && manager.getDownloadedFile().exists();
        setVisible(ready);
        if (ready) {
            applyI18n();
            actionButton.setEnabled(!installing);
        }
        revalidate();
        repaint();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void applyI18n() {
        titleLabel.setText(I18n.get("update.sidebar.install"));
        String version = UpdateDownloadManager.getInstance().getVersion();
        if (version != null) {
            versionLabel.setText(I18n.format("update.sidebar.ready", version));
        }
        actionButton.setToolTipText(titleLabel.getText());
    }

    private void applyAccentStyle() {
        Color accent = UIManager.getColor("Component.accentColor");
        if (accent == null) {
            accent = UIManager.getColor("Button.default.background");
        }
        if (accent == null) {
            accent = new Color(0x3F6FAE);
        }
        Color contrastColor = UIManager.getColor("Component.accentContrastColor");
        if (contrastColor == null) {
            contrastColor = Color.WHITE;
        }
        final Color contrast = contrastColor;
        actionButton.setBackground(accent);
        actionButton.setForeground(contrast);
        titleLabel.setForeground(contrast);
        versionLabel.setForeground(new Color(contrast.getRed(), contrast.getGreen(), contrast.getBlue(), 200));
        FlatSVGIcon icon = new FlatSVGIcon("icon/down.svg");
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> contrast));
        actionButton.setIcon(icon);
        actionButton.setOpaque(true);
        actionButton.setBorderPainted(false);
        actionButton.setContentAreaFilled(true);
    }

    private void install() {
        if (installing) {
            return;
        }
        installing = true;
        actionButton.setEnabled(false);
        try {
            UpdateDownloadManager.getInstance().installAndExit();
        } catch (Exception e) {
            log.error("打开更新安装包失败", e);
            installing = false;
            actionButton.setEnabled(true);
            MsgUtil.show(SwingUtilities.getWindowAncestor(this),
                    I18n.get("update.sidebar.installFailed"),
                    "common.failure",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
