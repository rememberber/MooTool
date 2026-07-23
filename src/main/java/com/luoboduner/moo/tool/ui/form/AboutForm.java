package com.luoboduner.moo.tool.ui.form;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.base.Strings;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.bean.ContributorInfo;
import com.luoboduner.moo.tool.bean.Dau;
import com.luoboduner.moo.tool.bean.Grace;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.ImagePreviewComponent;
import com.luoboduner.moo.tool.ui.component.NextEditionRecommendationPanel;
import com.luoboduner.moo.tool.ui.listener.AboutListener;
import com.luoboduner.moo.tool.ui.startup.AboutLoadData;
import com.luoboduner.moo.tool.ui.startup.EdtGuard;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.ImageDisplayUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * About
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/8/13.
 */
@Getter
@Slf4j
public class AboutForm {

    private JPanel aboutPanel;
    private JScrollPane scrollPane;
    private JLabel codeGitHubLabel;
    private JLabel versionLabel;
    private JLabel codeGiteeLabel;
    private JLabel issueLabel;
    private JLabel daculaLabel;
    private JLabel hutoolLabel;
    private JLabel wePushLinkLabel;
    private JPanel wePushPanel;
    private JLabel vsCodeIconsLabel;
    private JPanel mooInfoPanel;
    private JLabel mooInfoLinkLabel;
    private JLabel mooInfoIconLabel;
    private JPanel gracePanel;
    private JLabel graceLabel;
    private JLabel graceTitleLabel;
    private JPanel contributorPanel;
    private JLabel logoLabel;
    private JLabel homePageLabel;
    private JLabel taglineLabel;
    private JLabel authorLabel;
    private JLabel aboutLine1Label;
    private JLabel aboutLine2Label;
    private JLabel aboutLine3Label;
    private JLabel aboutLine4Label;
    private JLabel aboutLine5Label;
    private JLabel wePushDescLabel;
    private JLabel mooInfoDescLabel;
    private JLabel sponsorPromptLabel;
    private JLabel sponsorQrLabel;
    private JPanel aboutSectionPanel;
    private JPanel codeSectionPanel;
    private JPanel helpSectionPanel;
    private JPanel thanksSectionPanel;
    private JPanel otherWorksSectionPanel;
    private JPanel sponsorSectionPanel;
    private JPanel mooToolHeaderPanel;
    private NextEditionRecommendationPanel nextEditionPanel;

    private ImagePreviewComponent logoPreview;
    private ImagePreviewComponent gracePreview;
    private ImagePreviewComponent sponsorQrPreview;

    private static AboutForm aboutForm;
    private static boolean i18nRegistered;
    private static boolean viewShellInitialized;
    private static boolean pageServicesStarted;
    private static ScheduledThreadPoolExecutor graceScheduler;
    private static ScheduledThreadPoolExecutor dauScheduler;

    private AboutForm() {
    }

    public static AboutForm getInstance() {
        if (aboutForm == null) {
            aboutForm = new AboutForm();
        }
        return aboutForm;
    }

    public static void init() {
        createViewShell();
        ThreadUtil.execute(() -> {
            AboutLoadData data = AboutLoadData.loadInitial();
            SwingUtilities.invokeLater(() -> bindLoadedData(data));
        });
        startPageServices();
    }

    public static JPanel createViewShell() {
        EdtGuard.assertEdt();
        aboutForm = getInstance();
        if (viewShellInitialized) {
            return aboutForm.getAboutPanel();
        }
        aboutForm.versionLabel.setText(UiConsts.APP_VERSION);
        ScrollUtil.smoothPane(aboutForm.getScrollPane());
        aboutForm.installStaticImages();
        aboutForm.installNextEditionRecommendation();
        aboutForm.getAboutPanel().updateUI();

        aboutForm.getMooInfoPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/rememberber/MooInfo"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });

        aboutForm.getMooInfoIconLabel().setIcon(new FlatSVGIcon("icon/MooInfo.svg"));
        AboutListener.addListeners();
        aboutForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(AboutForm::applyI18nStatic);
            i18nRegistered = true;
        }
        viewShellInitialized = true;
        return aboutForm.getAboutPanel();
    }

    public static void bindLoadedData(AboutLoadData data) {
        EdtGuard.assertEdt();
        if (data == null) {
            return;
        }
        JPanel panel = aboutForm.getContributorPanel();
        for (ContributorInfo.Contributor contributor : data.contributors()) {
            try {
                addContributorItem(panel, contributor, data.getAvatarsByName().get(contributor.getName()));
            } catch (Exception e) {
                log.warn("添加贡献者 {} 失败", contributor.getName(), e);
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    public static void startPageServices() {
        if (pageServicesStarted) {
            return;
        }
        pageServicesStarted = true;
        startGraceScheduler();
        startDauScheduler();
    }

    public static void stopPageServices() {
        pageServicesStarted = false;
        if (graceScheduler != null) {
            graceScheduler.shutdownNow();
            graceScheduler = null;
        }
        if (dauScheduler != null) {
            dauScheduler.shutdownNow();
            dauScheduler = null;
        }
    }

    private static void startGraceScheduler() {
        try {
            graceScheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "mootool-about-grace");
                t.setDaemon(true);
                return t;
            });
            graceScheduler.scheduleAtFixedRate(() -> {
                try {
                    String graceInfoContent = HttpUtil.get(UiConsts.GRACE_INFO_URL, 10000);
                    if (graceInfoContent == null) {
                        return;
                    }
                    Grace grace = JSON.parseObject(graceInfoContent, Grace.class);
                    if (grace == null) {
                        return;
                    }
                    BufferedImage graceImage = null;
                    if (!Strings.isNullOrEmpty(grace.getImage())) {
                        try {
                            graceImage = ImageDisplayUtil.readImage(new URL(grace.getImage()));
                        } catch (IOException e) {
                            log.error("加载 Grace 图片失败", e);
                        }
                    }
                    final BufferedImage imageToShow = graceImage;
                    SwingUtilities.invokeLater(() -> applyGrace(grace, imageToShow));
                } catch (Exception e) {
                    log.error("获取Grace信息失败", e);
                }
            }, 0, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("获取Grace信息失败", e);
        }
    }

    private static void applyGrace(Grace grace, BufferedImage graceImage) {
        EdtGuard.assertEdt();
        if (graceImage != null && aboutForm.gracePreview != null) {
            aboutForm.gracePreview.setSourceImage(graceImage, 1.0);
        }
        if (!Strings.isNullOrEmpty(grace.getTips()) && aboutForm.gracePreview != null) {
            aboutForm.gracePreview.setToolTipText(grace.getTips());
        }
        if (!Strings.isNullOrEmpty(grace.getTitle())) {
            aboutForm.getGraceTitleLabel().setText(grace.getTitle());
        }
        if (!Strings.isNullOrEmpty(grace.getUrl())) {
            MouseAdapter browseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(grace.getUrl()));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            };
            aboutForm.getGracePanel().addMouseListener(browseListener);
            if (aboutForm.gracePreview != null) {
                aboutForm.gracePreview.addMouseListener(browseListener);
            }
        }
    }

    private static void startDauScheduler() {
        try {
            dauScheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "mootool-about-dau");
                t.setDaemon(true);
                return t;
            });
            dauScheduler.scheduleAtFixedRate(() -> {
                try {
                    String dauContent = HttpUtil.get(UiConsts.DAU_URL, 10000);
                    if (dauContent == null) {
                        return;
                    }
                    Dau dau = JSON.parseObject(dauContent, Dau.class);
                    if (dau == null) {
                        return;
                    }
                    String dauUrl = dau.getUrl();
                    log.info("dau:" + HttpUtil.get(dauUrl, 10000));
                    SwingUtilities.invokeLater(() -> attachDauLogoListener(dauUrl));
                } catch (Exception e) {
                    log.error("获取DAU信息失败", e);
                }
            }, 0, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("获取DAU信息失败", e);
        }
    }

    private static void attachDauLogoListener(String dauUrl) {
        EdtGuard.assertEdt();
        Component logoComponent = aboutForm.logoPreview != null
                ? aboutForm.logoPreview : aboutForm.getLogoLabel();
        logoComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(dauUrl));
                } catch (Exception ex) {
                    log.error("打开 DAU 链接失败", ex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
    }

    private static void loadContributors() {
        try {
            AboutLoadData data = AboutLoadData.loadInitial();
            SwingUtilities.invokeLater(() -> bindLoadedData(data));
        } catch (Exception e) {
            log.error("加载贡献者列表失败", e);
        }
    }

    private static void addContributorItem(JPanel panel, ContributorInfo.Contributor contributor) {
        addContributorItem(panel, contributor, null);
    }

    private static void addContributorItem(JPanel panel, ContributorInfo.Contributor contributor, BufferedImage preloadedAvatar) {
        if (contributor == null || Strings.isNullOrEmpty(contributor.getName())) {
            return;
        }
        JPanel contributorItem = new JPanel(new BorderLayout(0, 4));
        contributorItem.setOpaque(false);
        contributorItem.setToolTipText(contributor.getName());

        BufferedImage avatar = preloadedAvatar;
        if (avatar == null && !Strings.isNullOrEmpty(contributor.getAvatarUrl())) {
            try {
                avatar = ImageDisplayUtil.readImage(new URL(contributor.getAvatarUrl()));
            } catch (Exception e) {
                log.warn("加载贡献者 {} 头像失败", contributor.getName(), e);
            }
        }
        if (avatar != null) {
            ImagePreviewComponent avatarPreview = new ImagePreviewComponent();
            avatarPreview.setSourceImageInLogicalBounds(avatar, 50, 50);
            contributorItem.add(avatarPreview, BorderLayout.NORTH);
        }

        JLabel nameLabel = new JLabel(contributor.getName(), SwingConstants.CENTER);
        contributorItem.add(nameLabel, BorderLayout.SOUTH);

        if (!Strings.isNullOrEmpty(contributor.getLink())) {
            contributorItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(new URI(contributor.getLink()));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            });
        }
        panel.add(contributorItem);
    }

    private void applyI18n() {
        I18nUiUtil.setTitledBorder(mooToolHeaderPanel, "tab.mootool");
        I18nUiUtil.setTitledBorder(aboutSectionPanel, "about.section.about");
        I18nUiUtil.setTitledBorder(codeSectionPanel, "about.section.code");
        I18nUiUtil.setTitledBorder(helpSectionPanel, "about.section.help");
        I18nUiUtil.setTitledBorder(thanksSectionPanel, "about.section.thanks");
        I18nUiUtil.setTitledBorder(otherWorksSectionPanel, "about.section.otherWorks");
        I18nUiUtil.setTitledBorder(sponsorSectionPanel, "about.section.sponsor");
        I18nUiUtil.setTitledBorder(contributorPanel, "about.section.contributor");

        I18nUiUtil.setText(taglineLabel, "about.tagline");
        I18nUiUtil.setText(authorLabel, "about.author");
        versionLabel.setToolTipText(I18n.get("about.checkUpdates"));
        I18nUiUtil.setText(aboutLine1Label, "about.line1");
        I18nUiUtil.setText(aboutLine2Label, "about.line2");
        I18nUiUtil.setText(aboutLine3Label, "about.line3");
        I18nUiUtil.setText(aboutLine4Label, "about.line4");
        I18nUiUtil.setText(aboutLine5Label, "about.line5");
        codeGitHubLabel.setText(I18n.get("about.githubLink"));
        codeGiteeLabel.setText(I18n.get("about.giteeLink"));
        issueLabel.setText(I18n.get("about.issueLink"));
        I18nUiUtil.setText(wePushDescLabel, "about.wepush.desc");
        I18nUiUtil.setText(mooInfoDescLabel, "about.mooinfo.desc");
        I18nUiUtil.setText(sponsorPromptLabel, "about.sponsor.prompt");
        if (sponsorQrPreview != null) {
            I18nUiUtil.setToolTip(sponsorQrPreview, "about.sponsor.tip");
        }
        if (nextEditionPanel != null) {
            nextEditionPanel.applyI18n();
        }
    }

    private void installNextEditionRecommendation() {
        if (nextEditionPanel != null) {
            return;
        }
        nextEditionPanel = new NextEditionRecommendationPanel();
        aboutPanel.add(nextEditionPanel, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private void installStaticImages() {
        logoPreview = ImageDisplayUtil.installResourceImageQuietly(logoLabel, "/icon/logo-256.png");
        gracePreview = ImageDisplayUtil.replaceLabelWithImagePreview(graceLabel);
        sponsorQrPreview = installResourceImageForIconLabel(aboutPanel, "/icon/wx-zanshang.jpg");
        installResourceImageForIconLabel(wePushPanel, "/icon/WePush-logo-128.png");
    }

    private static ImagePreviewComponent installResourceImageForIconLabel(Container root, String resourcePath) {
        JLabel target = findIconOnlyLabel(root, resourcePath);
        if (target == null) {
            return null;
        }
        return ImageDisplayUtil.installResourceImageQuietly(target, resourcePath);
    }

    private static JLabel findIconOnlyLabel(Container root, String resourcePath) {
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label && label.getIcon() != null
                    && (label.getText() == null || label.getText().isEmpty())) {
                if (label.getIcon() instanceof ImageIcon imageIcon) {
                    Object description = imageIcon.getDescription();
                    if (description != null && description.toString().contains(fileName)) {
                        return label;
                    }
                }
            }
            if (component instanceof Container container) {
                JLabel found = findIconOnlyLabel(container, resourcePath);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void applyI18nStatic() {
        if (aboutForm != null) {
            aboutForm.applyI18n();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        aboutPanel = new JPanel();
        aboutPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        aboutPanel.setPreferredSize(new Dimension(400, 300));
        scrollPane = new JScrollPane();
        aboutPanel.add(scrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(11, 2, new Insets(40, 80, 0, 0), -1, -1));
        scrollPane.setViewportView(panel1);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        logoLabel = new JLabel();
        logoLabel.setIcon(new ImageIcon(getClass().getResource("/icon/logo-128.png")));
        logoLabel.setText("");
        panel1.add(logoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "MooTool", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, 20, panel2.getFont()), new Color(-7883)));
        final JLabel label1 = new JLabel();
        label1.setText("Handy desktop toolset for developers");
        panel2.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Proudly by RememBerBer 周波");
        panel2.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        versionLabel = new JLabel();
        Font versionLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, versionLabel.getFont());
        if (versionLabelFont != null) versionLabel.setFont(versionLabelFont);
        versionLabel.setText("v0.0.0");
        versionLabel.setToolTipText("Check for updates");
        panel2.add(versionLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        homePageLabel = new JLabel();
        homePageLabel.setText("");
        panel2.add(homePageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "ABOUT", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel3.getFont()), null));
        final JLabel label3 = new JLabel();
        label3.setText("Hi! Thanks to use MooTool. \"Moo\" named from my daughter.");
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Less Java developer use Swing building projects, but I still love to develop by it.");
        panel3.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("So every little free time, I had went towards the development.");
        panel3.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("I did some works finaly, although there are so many same on web page.");
        panel3.add(label6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Hope you enjoy using it as much as I did building it.");
        panel3.add(label7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "CODE", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel4.getFont()), null));
        codeGitHubLabel = new JLabel();
        codeGitHubLabel.setText("<html>GitHub：<a href=\"https://github.com/rememberber/MooTool\">https://github.com/rememberber/MooTool</a></html>");
        panel4.add(codeGitHubLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codeGiteeLabel = new JLabel();
        codeGiteeLabel.setText("<html>Gitee：<a href=\"https://gitee.com/zhoubochina/MooTool\">https://gitee.com/zhoubochina/MooTool</a></html>");
        panel4.add(codeGiteeLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "HELP TO DO BETTER", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel5.getFont()), null));
        issueLabel = new JLabel();
        issueLabel.setText("<html><a href=\"https://github.com/rememberber/MooTool/issues\">https://github.com/rememberber/MooTool/issues</a></html>");
        panel5.add(issueLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel6, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "THANKS TO", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel6.getFont()), null));
        daculaLabel = new JLabel();
        daculaLabel.setText("<html><a href=\"https://github.com/bulenkov/Darcula\">Darcula</a></html>");
        panel6.add(daculaLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hutoolLabel = new JLabel();
        hutoolLabel.setText("<html><a href=\"https://hutool.cn/\">Hutool</a></html>");
        panel6.add(hutoolLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        vsCodeIconsLabel = new JLabel();
        vsCodeIconsLabel.setText("<html><a href=\"https://github.com/microsoft/vscode-icons\">vscode-icons</a></html>");
        panel6.add(vsCodeIconsLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel7, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "OTHER WORKS", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel7.getFont()), null));
        wePushPanel = new JPanel();
        wePushPanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(wePushPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        wePushLinkLabel = new JLabel();
        wePushLinkLabel.setText("<html><a href=\"https://github.com/rememberber/WePush\">https://github.com/rememberber/WePush</a></html>");
        wePushPanel.add(wePushLinkLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setIcon(new ImageIcon(getClass().getResource("/icon/WePush-logo-64.png")));
        label8.setText("");
        wePushPanel.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("WePush");
        wePushPanel.add(label9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("专注批量推送的小而美的工具");
        wePushPanel.add(label10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mooInfoPanel = new JPanel();
        mooInfoPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(mooInfoPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        mooInfoLinkLabel = new JLabel();
        mooInfoLinkLabel.setText("<html><a href=\"https://github.com/rememberber/MooInfo\">https://github.com/rememberber/MooInfo</a></html>");
        mooInfoPanel.add(mooInfoLinkLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mooInfoIconLabel = new JLabel();
        mooInfoIconLabel.setIcon(new ImageIcon(getClass().getResource("/icon/WePush-logo-64.png")));
        mooInfoIconLabel.setText("");
        mooInfoPanel.add(mooInfoIconLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("MooInfo");
        mooInfoPanel.add(label11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Visual implementation of OSHI, to view information about the system and hardware");
        mooInfoPanel.add(label12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(2, 1, new Insets(5, 3, 20, 0), -1, -1));
        panel1.add(panel8, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "SPONSOR", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel8.getFont()), null));
        final JLabel label13 = new JLabel();
        label13.setIcon(new ImageIcon(getClass().getResource("/icon/wx-zanshang.jpg")));
        label13.setText("");
        label13.setToolTipText("感谢您的鼓励和支持");
        panel8.add(label13, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Give me a grace");
        panel8.add(label14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gracePanel = new JPanel();
        gracePanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 5, 0, 0), -1, -1));
        panel1.add(gracePanel, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        graceLabel = new JLabel();
        graceLabel.setText("");
        gracePanel.add(graceLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        gracePanel.add(spacer3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        graceTitleLabel = new JLabel();
        Font graceTitleLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, graceTitleLabel.getFont());
        if (graceTitleLabelFont != null) graceTitleLabel.setFont(graceTitleLabelFont);
        graceTitleLabel.setText("");
        gracePanel.add(graceTitleLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contributorPanel = new JPanel();
        contributorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(contributorPanel, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        contributorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "CONTRIBUTOR", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, contributorPanel.getFont()), null));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return aboutPanel;
    }

}
