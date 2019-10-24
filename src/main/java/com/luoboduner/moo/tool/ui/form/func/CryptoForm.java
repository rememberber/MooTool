package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.security.KeyPair;

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

    private static CryptoForm cryptoForm;

    private static final Log logger = LogFactory.get();

    public final static String RANDOM_BASE_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()_+-=[]{};':,./<>?";

    private CryptoForm() {
        UndoUtil.register(this);

        // 对称-加密按钮
        symEncryptButton.addActionListener(e -> {
            try {
                String symType = (String) cryptoForm.getSymTypeComboBox().getSelectedItem();
                String content = cryptoForm.getSymTextAreaLeft().getText();
                String key = cryptoForm.getSymKeyTextField().getText();

                if ("AES".equals(symType)) {
                    byte[] generatedKey = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), key.getBytes()).getEncoded();

                    AES aes = SecureUtil.aes(generatedKey);
                    // 加密为16进制表示
                    String encryptHex = aes.encryptHex(content);
                    cryptoForm.getSymTextAreaRight().setText(encryptHex);
                } else if ("DES".equals(symType)) {
                    byte[] generatedKey = SecureUtil.generateKey(SymmetricAlgorithm.DES.getValue(), key.getBytes()).getEncoded();

                    DES des = SecureUtil.des(generatedKey);
                    // 加密为16进制表示
                    String encryptHex = des.encryptHex(content);
                    cryptoForm.getSymTextAreaRight().setText(encryptHex);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        // 对称-解密按钮
        symDecryptButton.addActionListener(e -> {
            try {
                String symType = (String) cryptoForm.getSymTypeComboBox().getSelectedItem();
                String content = cryptoForm.getSymTextAreaRight().getText();
                String key = cryptoForm.getSymKeyTextField().getText();
                if ("AES".equals(symType)) {
                    byte[] generatedKey = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), key.getBytes()).getEncoded();

                    AES aes = SecureUtil.aes(generatedKey);
                    String decryptStr = aes.decryptStr(content);
                    cryptoForm.getSymTextAreaLeft().setText(decryptStr);
                } else if ("DES".equals(symType)) {
                    byte[] generatedKey = SecureUtil.generateKey(SymmetricAlgorithm.DES.getValue(), key.getBytes()).getEncoded();

                    DES des = SecureUtil.des(generatedKey);
                    String decryptStr = des.decryptStr(content);
                    cryptoForm.getSymTextAreaLeft().setText(decryptStr);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "解密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 文本摘要加密
        digestTextButton.addActionListener(e -> {
            try {
                String digestContent = cryptoForm.getDigestContentTextArea().getText();
                String digestType = (String) cryptoForm.getDigestTypeComboBox().getSelectedItem();
                String digestResult = "";
                assert digestType != null;
                switch (digestType) {
                    case "MD5":
                        digestResult = DigestUtil.md5Hex(digestContent);
                        break;
                    case "SHA-1":
                        digestResult = DigestUtil.sha1Hex(digestContent);
                        break;
                    case "SHA-256":
                        digestResult = DigestUtil.sha256Hex(digestContent);
                        break;
                    case "SHA-384":
                        digestResult = new Digester(DigestAlgorithm.SHA384).digestHex(digestContent, CharsetUtil.UTF_8);
                        break;
                    case "SHA-512":
                        digestResult = new Digester(DigestAlgorithm.SHA512).digestHex(digestContent, CharsetUtil.UTF_8);
                        break;
                    default:
                        digestResult = "";
                }
                cryptoForm.getDigestResultTextArea().setText(digestResult);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 文件摘要加密
        digestFileButton.addActionListener(e -> {
            try {
                String filePath = cryptoForm.getDigestFilePathTextField().getText();
                String digestType = (String) cryptoForm.getDigestTypeComboBox().getSelectedItem();
                String digestResult = "";
                assert digestType != null;
                switch (digestType) {
                    case "MD5":
                        digestResult = DigestUtil.md5Hex(FileUtil.file(filePath));
                        break;
                    case "SHA-1":
                        digestResult = DigestUtil.sha1Hex(FileUtil.file(filePath));
                        break;
                    case "SHA-256":
                        digestResult = DigestUtil.sha256Hex(FileUtil.file(filePath));
                        break;
                    case "SHA-384":
                        digestResult = new Digester(DigestAlgorithm.SHA384).digestHex(FileUtil.file(filePath));
                        break;
                    case "SHA-512":
                        digestResult = new Digester(DigestAlgorithm.SHA512).digestHex(FileUtil.file(filePath));
                        break;
                    default:
                        digestResult = "";
                }
                cryptoForm.getDigestResultTextArea().setText(digestResult);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 文件摘要加密-文件浏览按钮
        exploreButton.addActionListener(e -> {
            File beforeFile = new File(cryptoForm.getDigestFilePathTextField().getText());
            JFileChooser fileChooser;

            if (beforeFile.exists()) {
                fileChooser = new JFileChooser(beforeFile);
            } else {
                fileChooser = new JFileChooser();
            }

            int approve = fileChooser.showOpenDialog(cryptoForm.getCryptoPanel());
            if (approve == JFileChooser.APPROVE_OPTION) {
                cryptoForm.getDigestFilePathTextField().setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        generateUuidButton.addActionListener(e -> {
            String randomUUID = IdUtil.randomUUID();
            cryptoForm.getUuidTextField().setText(randomUUID);
        });
        generateRandomNumButton.addActionListener(e -> {
            try {
                int digit = Integer.parseInt(cryptoForm.getRandomNumDigitTextField().getText().trim());
                String randomNumbers = RandomUtil.randomNumbers(digit);
                cryptoForm.getRandomNumTextField().setText(randomNumbers);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        generateRadomStringButton.addActionListener(e -> {
            try {
                int digit = Integer.parseInt(cryptoForm.getRandomStringDigitTextField().getText().trim());
                String randomString = RandomUtil.randomString(digit);
                cryptoForm.getRandomStringTextField().setText(randomString);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        generateRandomPasswordButton.addActionListener(e -> {
            try {
                int digit = Integer.parseInt(cryptoForm.getRandomPasswordDigitTextField().getText().trim());
                String randomPassword = RandomUtil.randomString(RANDOM_BASE_STRING, digit);
                cryptoForm.getRandomPasswordTextField().setText(randomPassword);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        copyUuidButton.addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getUuidTextField().getText());
                JOptionPane.showMessageDialog(cryptoForm.getCryptoPanel(), "内容已经复制到剪贴板！", "复制成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });
        copyRandomNumButton.addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getRandomNumTextField().getText());
                JOptionPane.showMessageDialog(cryptoForm.getCryptoPanel(), "内容已经复制到剪贴板！", "复制成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });
        copyRadomStringButton.addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getRandomStringTextField().getText());
                JOptionPane.showMessageDialog(cryptoForm.getCryptoPanel(), "内容已经复制到剪贴板！", "复制成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });
        copyRandomPasswordButton.addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getRandomPasswordTextField().getText());
                JOptionPane.showMessageDialog(cryptoForm.getCryptoPanel(), "内容已经复制到剪贴板！", "复制成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });
        // 生成一对密钥
        asymKeyGenerateButton.addActionListener(e -> {
            try {
                String asymType = (String) cryptoForm.getAsymComboBox().getSelectedItem();
                String privateKeyStr = "";
                String publicKeyStr = "";
                if ("RSA".equals(asymType)) {
                    KeyPair pair = SecureUtil.generateKeyPair("RSA");
                    privateKeyStr = Base64.encode(pair.getPrivate().getEncoded());
                    publicKeyStr = Base64.encode(pair.getPublic().getEncoded());
                } else if ("DSA".equals(asymType)) {
                    KeyPair pair = SecureUtil.generateKeyPair("DSA");
                    privateKeyStr = Base64.encode(pair.getPrivate().getEncoded());
                    publicKeyStr = Base64.encode(pair.getPublic().getEncoded());
                }
                cryptoForm.getAsymPubKeyTextArea().setText(publicKeyStr);
                cryptoForm.getAsymPubKeyTextArea().setCaretPosition(0);
                cryptoForm.getAsymPrivateKeyTextArea().setText(privateKeyStr);
                cryptoForm.getAsymPrivateKeyTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 使用公钥加密
        asymEncryptWithPubKeyButton.addActionListener(e -> {
            try {
                String asymType = (String) cryptoForm.getAsymComboBox().getSelectedItem();
                String publicKeyStr = cryptoForm.getAsymPubKeyTextArea().getText();
                String toEncryptStr = cryptoForm.getAsymLeftTextArea().getText();
                String encryptStr = "";
                if ("RSA".equals(asymType)) {
                    RSA rsa = new RSA(null, publicKeyStr);
                    encryptStr = Base64.encode(rsa.encrypt(StrUtil.bytes(toEncryptStr, CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey));
                }

                cryptoForm.getAsymRightTextArea().setText(encryptStr);
                cryptoForm.getAsymRightTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        // 使用私钥解密
        asymDecryptWithPrivateKeyButton.addActionListener(e -> {
            try {
                String asymType = (String) cryptoForm.getAsymComboBox().getSelectedItem();
                String privateKeyStr = cryptoForm.getAsymPrivateKeyTextArea().getText();
                String toDecryptStr = cryptoForm.getAsymRightTextArea().getText();
                String decryptStr = "";
                if ("RSA".equals(asymType)) {
                    RSA rsa = new RSA(privateKeyStr, null);
                    decryptStr = StrUtil.str(rsa.decrypt(Base64.decode(toDecryptStr), KeyType.PrivateKey), CharsetUtil.CHARSET_UTF_8);
                }

                cryptoForm.getAsymLeftTextArea().setText(decryptStr);
                cryptoForm.getAsymLeftTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "解密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 使用私钥加密
        asymEncryptWithPrivateKeyButton.addActionListener(e -> {
            try {
                String asymType = (String) cryptoForm.getAsymComboBox().getSelectedItem();
                String privateKeyStr = cryptoForm.getAsymPrivateKeyTextArea().getText();
                String toEncryptStr = cryptoForm.getAsymLeftTextArea().getText();
                String encryptStr = "";
                if ("RSA".equals(asymType)) {
                    RSA rsa = new RSA(privateKeyStr, null);
                    encryptStr = Base64.encode(rsa.encrypt(StrUtil.bytes(toEncryptStr, CharsetUtil.CHARSET_UTF_8), KeyType.PrivateKey));
                }

                cryptoForm.getAsymRightTextArea().setText(encryptStr);
                cryptoForm.getAsymRightTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
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
    }

    private static void initUi() {
        if ("Darcula(推荐)".equals(App.config.getTheme())) {
            Color bgColor = new Color(30, 30, 30);
            Color foreColor = new Color(187, 187, 187);
            cryptoForm.getSymTextAreaLeft().setBackground(bgColor);
            cryptoForm.getSymTextAreaLeft().setForeground(foreColor);

            cryptoForm.getSymTextAreaRight().setBackground(bgColor);
            cryptoForm.getSymTextAreaRight().setForeground(foreColor);

            cryptoForm.getAsymPubKeyTextArea().setBackground(bgColor);
            cryptoForm.getAsymPubKeyTextArea().setForeground(foreColor);

            cryptoForm.getAsymPrivateKeyTextArea().setBackground(bgColor);
            cryptoForm.getAsymPrivateKeyTextArea().setForeground(foreColor);

            cryptoForm.getAsymLeftTextArea().setBackground(bgColor);
            cryptoForm.getAsymLeftTextArea().setForeground(foreColor);

            cryptoForm.getAsymRightTextArea().setBackground(bgColor);
            cryptoForm.getAsymRightTextArea().setForeground(foreColor);

            cryptoForm.getDigestContentTextArea().setBackground(bgColor);
            cryptoForm.getDigestContentTextArea().setForeground(foreColor);

            cryptoForm.getDigestResultTextArea().setBackground(bgColor);
            cryptoForm.getDigestResultTextArea().setForeground(foreColor);
        }
        cryptoForm.getCryptoPanel().updateUI();
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
        cryptoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        cryptoPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
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
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
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
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("摘要加密（digest）", panel6);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(5, 5, 5, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("文件");
        panel8.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        digestFilePathTextField = new JTextField();
        panel8.add(digestFilePathTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exploreButton = new JButton();
        exploreButton.setText("…");
        panel8.add(exploreButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane7 = new JScrollPane();
        panel7.add(scrollPane7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        digestContentTextArea = new JTextArea();
        scrollPane7.setViewportView(digestContentTextArea);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane8 = new JScrollPane();
        panel9.add(scrollPane8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        digestResultTextArea = new JTextArea();
        scrollPane8.setViewportView(digestResultTextArea);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        digestTextButton = new JButton();
        digestTextButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        digestTextButton.setText("文本摘要加密/哈希");
        panel10.add(digestTextButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        digestFileButton = new JButton();
        digestFileButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        digestFileButton.setText("文件摘要加密/哈希");
        panel10.add(digestFileButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel10.add(spacer5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel10.add(spacer6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        digestTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("MD5");
        defaultComboBoxModel3.addElement("SHA-1");
        defaultComboBoxModel3.addElement("SHA-256");
        defaultComboBoxModel3.addElement("SHA-384");
        defaultComboBoxModel3.addElement("SHA-512");
        digestTypeComboBox.setModel(defaultComboBoxModel3);
        panel10.add(digestTypeComboBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(9, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("随机", panel11);
        final Spacer spacer7 = new Spacer();
        panel11.add(spacer7, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(8, 1, new Insets(10, 10, 0, 10), -1, -1));
        panel11.add(panel12, new GridConstraints(0, 0, 8, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(2, 3, new Insets(10, 0, 0, 0), -1, -1));
        panel12.add(panel13, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("随机UUID");
        panel13.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uuidTextField = new JTextField();
        panel13.add(uuidTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyUuidButton = new JButton();
        copyUuidButton.setText("复制");
        panel13.add(copyUuidButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateUuidButton = new JButton();
        generateUuidButton.setText("生成");
        panel13.add(generateUuidButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(2, 5, new Insets(20, 0, 0, 0), -1, -1));
        panel12.add(panel14, new GridConstraints(2, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("获得一个只包含数字的字符串");
        panel14.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomNumTextField = new JTextField();
        panel14.add(randomNumTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyRandomNumButton = new JButton();
        copyRandomNumButton.setText("复制");
        panel14.add(copyRandomNumButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateRandomNumButton = new JButton();
        generateRandomNumButton.setText("生成");
        panel14.add(generateRandomNumButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomNumDigitTextField = new JTextField();
        panel14.add(randomNumDigitTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("位数");
        panel14.add(label9, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(2, 5, new Insets(20, 0, 0, 0), -1, -1));
        panel12.add(panel15, new GridConstraints(4, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("获得一个随机的字符串（只包含数字和字符）");
        panel15.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomStringTextField = new JTextField();
        panel15.add(randomStringTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyRadomStringButton = new JButton();
        copyRadomStringButton.setText("复制");
        panel15.add(copyRadomStringButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateRadomStringButton = new JButton();
        generateRadomStringButton.setText("生成");
        panel15.add(generateRadomStringButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("位数");
        panel15.add(label11, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomStringDigitTextField = new JTextField();
        panel15.add(randomStringDigitTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(2, 5, new Insets(20, 0, 0, 0), -1, -1));
        panel12.add(panel16, new GridConstraints(6, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("随机生成一个复杂的密码（带特殊符号）");
        panel16.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomPasswordTextField = new JTextField();
        panel16.add(randomPasswordTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyRandomPasswordButton = new JButton();
        copyRandomPasswordButton.setText("复制");
        panel16.add(copyRandomPasswordButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateRandomPasswordButton = new JButton();
        generateRandomPasswordButton.setText("生成");
        panel16.add(generateRandomPasswordButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("位数");
        panel16.add(label13, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        randomPasswordDigitTextField = new JTextField();
        panel16.add(randomPasswordDigitTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return cryptoPanel;
    }

}
