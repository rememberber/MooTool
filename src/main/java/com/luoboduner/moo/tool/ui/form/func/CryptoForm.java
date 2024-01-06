package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.listener.func.CryptoListener;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * 加解密/随机
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/10.
 */
@Getter
public class CryptoForm {
    private JTabbedPane tabbedPane1;
    private JPanel cryptoPanel;
    private JTextArea symTextAreaLeft;
    private JTextArea symTextAreaRight;
    private JButton symEncryptButton;
    private JButton symDecryptButton;
    private JTextField symKeyTextField;
    private JComboBox symTypeComboBox;
    private JPanel asyPanelLeft;
    private JPanel asyPanelRight;
    private JPanel asyPanelCenter;
    private JTextArea asymPubKeyTextArea;
    private JTextArea asymLeftTextArea;
    private JTextArea asymPrivateKeyTextArea;
    private JTextArea asymRightTextArea;
    private JButton asymKeyGenerateButton;
    private JButton asymEncryptWithPubKeyButton;
    private JButton asymDecryptWithPrivateKeyButton;
    private JComboBox asymComboBox;
    private JButton asymEncryptWithPrivateKeyButton;
    private JButton asymDecryptWithPubKeyButton;
    private JTextArea digestContentTextArea;
    private JTextArea digestResultTextArea;
    private JTextField digestFilePathTextField;
    private JButton exploreButton;
    private JButton digestTextButton;
    private JComboBox digestTypeComboBox;
    private JTextField uuidTextField;
    private JTextField randomNumTextField;
    private JTextField randomPasswordTextField;
    private JTextField randomStringTextField;
    private JButton copyUuidButton;
    private JButton copyRandomNumButton;
    private JButton copyRadomStringButton;
    private JButton copyRandomPasswordButton;
    private JButton digestFileButton;
    private JButton generateUuidButton;
    private JButton generateRandomNumButton;
    private JButton generateRadomStringButton;
    private JButton generateRandomPasswordButton;
    private JTextField randomNumDigitTextField;
    private JTextField randomStringDigitTextField;
    private JTextField randomPasswordDigitTextField;
    private JPanel digestLeftPanel;
    private JPanel digestControlPanel;
    private JScrollPane digestLeftScrollPane;
    private JButton toUpperCaseButton;
    private JButton toLowerCaseButton;
    private JTextArea base64LeftTextArea;
    private JTextArea base64RightTextArea;
    private JButton base64EncodeButton;
    private JButton base64DecodeButton;
    private JComboBox base64ComboBox;

    private static CryptoForm cryptoForm;

    private static final Log logger = LogFactory.get();

    private CryptoForm() {
        UndoUtil.register(this);
    }

    public static CryptoForm getInstance() {
        if (cryptoForm == null) {
            cryptoForm = new CryptoForm();
        }
        return cryptoForm;
    }

    public static void init() {
        cryptoForm = getInstance();

        initUi();

        CryptoListener.addListeners();
    }

    private static void initUi() {
        cryptoForm.getDigestLeftPanel().removeAll();
        if ("上方".equals(App.config.getMenuBarPosition())) {
            cryptoForm.getDigestLeftPanel().add(cryptoForm.getDigestControlPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            cryptoForm.getDigestLeftPanel().add(cryptoForm.getDigestLeftScrollPane(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        } else if ("下方".equals(App.config.getMenuBarPosition())) {
            cryptoForm.getDigestLeftPanel().add(cryptoForm.getDigestControlPanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            cryptoForm.getDigestLeftPanel().add(cryptoForm.getDigestLeftScrollPane(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        }

        Style.blackTextArea(cryptoForm.getSymTextAreaLeft());
        Style.blackTextArea(cryptoForm.getSymTextAreaRight());
        Style.blackTextArea(cryptoForm.getAsymPubKeyTextArea());
        Style.blackTextArea(cryptoForm.getAsymPrivateKeyTextArea());
        Style.blackTextArea(cryptoForm.getAsymLeftTextArea());
        Style.blackTextArea(cryptoForm.getAsymRightTextArea());
        Style.blackTextArea(cryptoForm.getDigestContentTextArea());
        Style.blackTextArea(cryptoForm.getDigestResultTextArea());
        Style.blackTextArea(cryptoForm.getBase64LeftTextArea());
        Style.blackTextArea(cryptoForm.getBase64RightTextArea());

        cryptoForm.getSymEncryptButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        cryptoForm.getSymDecryptButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));
        cryptoForm.getAsymEncryptWithPubKeyButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        cryptoForm.getAsymDecryptWithPubKeyButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));
        cryptoForm.getAsymEncryptWithPrivateKeyButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        cryptoForm.getAsymDecryptWithPrivateKeyButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));
        cryptoForm.getDigestFileButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        cryptoForm.getDigestTextButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        cryptoForm.getBase64EncodeButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        cryptoForm.getBase64DecodeButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));

        cryptoForm.getCryptoPanel().updateUI();
        cryptoForm.getDigestFilePathTextField().setText(App.config.getDigestFilePath());
        cryptoForm.getRandomNumDigitTextField().setText(String.valueOf(App.config.getRandomNumDigit()));
        cryptoForm.getRandomStringDigitTextField().setText(String.valueOf(App.config.getRandomStringDigit()));
        cryptoForm.getRandomPasswordDigitTextField().setText(String.valueOf(App.config.getRandomPasswordDigit()));
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
        cryptoPanel = new JPanel();
        cryptoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        cryptoPanel.setMinimumSize(new Dimension(400, 300));
        cryptoPanel.setPreferredSize(new Dimension(400, 300));
        tabbedPane1 = new JTabbedPane();
        cryptoPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("对称加密（symmetric）", panel1);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        symTextAreaLeft = new JTextArea();
        scrollPane1.setViewportView(symTextAreaLeft);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        symTextAreaRight = new JTextArea();
        scrollPane2.setViewportView(symTextAreaRight);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        symEncryptButton = new JButton();
        symEncryptButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        symEncryptButton.setText("加密");
        panel2.add(symEncryptButton, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        symDecryptButton = new JButton();
        symDecryptButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-left.png")));
        symDecryptButton.setText("解密");
        panel2.add(symDecryptButton, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        symKeyTextField = new JTextField();
        symKeyTextField.setToolTipText("输入16位字符");
        panel2.add(symKeyTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        symTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("AES");
        defaultComboBoxModel1.addElement("DES");
        symTypeComboBox.setModel(defaultComboBoxModel1);
        panel2.add(symTypeComboBox, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("密钥");
        panel2.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("非对称加密（asymmetric）", panel3);
        asyPanelLeft = new JPanel();
        asyPanelLeft.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(asyPanelLeft, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("公钥");
        asyPanelLeft.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("待加密/解密结果：");
        asyPanelLeft.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        asyPanelLeft.add(scrollPane3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        asymPubKeyTextArea = new JTextArea();
        scrollPane3.setViewportView(asymPubKeyTextArea);
        final JScrollPane scrollPane4 = new JScrollPane();
        asyPanelLeft.add(scrollPane4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        asymLeftTextArea = new JTextArea();
        scrollPane4.setViewportView(asymLeftTextArea);
        asyPanelRight = new JPanel();
        asyPanelRight.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(asyPanelRight, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("私钥");
        asyPanelRight.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("待解密/加密结果：");
        asyPanelRight.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        asyPanelRight.add(scrollPane5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        asymPrivateKeyTextArea = new JTextArea();
        scrollPane5.setViewportView(asymPrivateKeyTextArea);
        final JScrollPane scrollPane6 = new JScrollPane();
        asyPanelRight.add(scrollPane6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        asymRightTextArea = new JTextArea();
        scrollPane6.setViewportView(asymRightTextArea);
        asyPanelCenter = new JPanel();
        asyPanelCenter.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(asyPanelCenter, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        asymKeyGenerateButton = new JButton();
        asymKeyGenerateButton.setText("生成一对密钥");
        asyPanelCenter.add(asymKeyGenerateButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        asyPanelCenter.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        asyPanelCenter.add(spacer4, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        asymComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("RSA");
        asymComboBox.setModel(defaultComboBoxModel2);
        asyPanelCenter.add(asymComboBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(20, 0, 0, 0), -1, -1));
        asyPanelCenter.add(panel4, new GridConstraints(3, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        asymEncryptWithPubKeyButton = new JButton();
        asymEncryptWithPubKeyButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        asymEncryptWithPubKeyButton.setText("使用公钥加密");
        panel4.add(asymEncryptWithPubKeyButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        asymDecryptWithPrivateKeyButton = new JButton();
        asymDecryptWithPrivateKeyButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-left.png")));
        asymDecryptWithPrivateKeyButton.setText("使用私钥解密");
        panel4.add(asymDecryptWithPrivateKeyButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(20, 0, 0, 0), -1, -1));
        asyPanelCenter.add(panel5, new GridConstraints(5, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        asymEncryptWithPrivateKeyButton = new JButton();
        asymEncryptWithPrivateKeyButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        asymEncryptWithPrivateKeyButton.setText("使用私钥加密");
        panel5.add(asymEncryptWithPrivateKeyButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        asymDecryptWithPubKeyButton = new JButton();
        asymDecryptWithPubKeyButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-left.png")));
        asymDecryptWithPubKeyButton.setText("使用公钥解密");
        panel5.add(asymDecryptWithPubKeyButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("摘要加密（digest）", panel6);
        digestLeftPanel = new JPanel();
        digestLeftPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(digestLeftPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        digestControlPanel = new JPanel();
        digestControlPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        digestLeftPanel.add(digestControlPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("文件");
        digestControlPanel.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        digestFilePathTextField = new JTextField();
        digestControlPanel.add(digestFilePathTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exploreButton = new JButton();
        exploreButton.setText("…");
        digestControlPanel.add(exploreButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        digestLeftScrollPane = new JScrollPane();
        digestLeftPanel.add(digestLeftScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        digestContentTextArea = new JTextArea();
        digestLeftScrollPane.setViewportView(digestContentTextArea);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane7 = new JScrollPane();
        panel7.add(scrollPane7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        digestResultTextArea = new JTextArea();
        scrollPane7.setViewportView(digestResultTextArea);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        digestTextButton = new JButton();
        digestTextButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        digestTextButton.setText("文本摘要加密/哈希");
        panel8.add(digestTextButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        digestFileButton = new JButton();
        digestFileButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        digestFileButton.setText("文件摘要加密/哈希");
        panel8.add(digestFileButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel8.add(spacer5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel8.add(spacer6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        digestTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("MD5");
        defaultComboBoxModel3.addElement("SHA-1");
        defaultComboBoxModel3.addElement("SHA-256");
        defaultComboBoxModel3.addElement("SHA-384");
        defaultComboBoxModel3.addElement("SHA-512");
        digestTypeComboBox.setModel(defaultComboBoxModel3);
        panel8.add(digestTypeComboBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Base64", panel9);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane8 = new JScrollPane();
        panel10.add(scrollPane8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        base64LeftTextArea = new JTextArea();
        scrollPane8.setViewportView(base64LeftTextArea);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel11, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane9 = new JScrollPane();
        panel11.add(scrollPane9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        base64RightTextArea = new JTextArea();
        scrollPane9.setViewportView(base64RightTextArea);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel12, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        base64EncodeButton = new JButton();
        base64EncodeButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        base64EncodeButton.setText("加密");
        panel12.add(base64EncodeButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        base64DecodeButton = new JButton();
        base64DecodeButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-left.png")));
        base64DecodeButton.setText("解密");
        panel12.add(base64DecodeButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel12.add(spacer7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel12.add(spacer8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        base64ComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("Base64");
        defaultComboBoxModel4.addElement("Base32");
        base64ComboBox.setModel(defaultComboBoxModel4);
        panel12.add(base64ComboBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("随机", panel13);
        final Spacer spacer9 = new Spacer();
        panel13.add(spacer9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 0, 10), -1, -1));
        panel13.add(panel14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(2, 3, new Insets(10, 0, 0, 0), -1, -1));
        panel14.add(panel15, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("随机UUID");
        panel15.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uuidTextField = new JTextField();
        panel15.add(uuidTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyUuidButton = new JButton();
        copyUuidButton.setText("复制");
        panel15.add(copyUuidButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateUuidButton = new JButton();
        generateUuidButton.setText("生成");
        panel15.add(generateUuidButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(2, 5, new Insets(20, 0, 0, 0), -1, -1));
        panel14.add(panel16, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("获得一个只包含数字的字符串");
        panel16.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomNumTextField = new JTextField();
        panel16.add(randomNumTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyRandomNumButton = new JButton();
        copyRandomNumButton.setText("复制");
        panel16.add(copyRandomNumButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateRandomNumButton = new JButton();
        generateRandomNumButton.setText("生成");
        panel16.add(generateRandomNumButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomNumDigitTextField = new JTextField();
        panel16.add(randomNumDigitTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("位数");
        panel16.add(label9, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(2, 7, new Insets(20, 0, 0, 0), -1, -1));
        panel14.add(panel17, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("获得一个随机的字符串（只包含数字和字符）");
        panel17.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomStringTextField = new JTextField();
        panel17.add(randomStringTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyRadomStringButton = new JButton();
        copyRadomStringButton.setText("复制");
        panel17.add(copyRadomStringButton, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateRadomStringButton = new JButton();
        generateRadomStringButton.setText("生成");
        panel17.add(generateRadomStringButton, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("位数");
        panel17.add(label11, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomStringDigitTextField = new JTextField();
        panel17.add(randomStringDigitTextField, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        toUpperCaseButton = new JButton();
        toUpperCaseButton.setText("转大写");
        panel17.add(toUpperCaseButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        toLowerCaseButton = new JButton();
        toLowerCaseButton.setText("转小写");
        panel17.add(toLowerCaseButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(2, 5, new Insets(20, 0, 0, 0), -1, -1));
        panel14.add(panel18, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("随机生成一个复杂的密码（带特殊符号）");
        panel18.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomPasswordTextField = new JTextField();
        panel18.add(randomPasswordTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyRandomPasswordButton = new JButton();
        copyRandomPasswordButton.setText("复制");
        panel18.add(copyRandomPasswordButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateRandomPasswordButton = new JButton();
        generateRandomPasswordButton.setText("生成");
        panel18.add(generateRandomPasswordButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("位数");
        panel18.add(label13, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomPasswordDigitTextField = new JTextField();
        panel18.add(randomPasswordDigitTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return cryptoPanel;
    }

}
