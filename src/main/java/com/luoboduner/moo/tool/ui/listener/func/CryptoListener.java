package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.codec.Base32;
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
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.CryptoForm;
import com.luoboduner.moo.tool.util.AlertUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.io.File;
import java.security.KeyPair;

/**
 * <pre>
 * CryptoListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/24.
 */
public class CryptoListener {
    private static final Log logger = LogFactory.get();

    private final static String RANDOM_BASE_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()_+-=[]{};':,./<>?";

    public static void addListeners() {
        CryptoForm cryptoForm = CryptoForm.getInstance();
        // 对称-加密按钮
        cryptoForm.getSymEncryptButton().addActionListener(e -> {
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
        cryptoForm.getSymDecryptButton().addActionListener(e -> {
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
        cryptoForm.getDigestTextButton().addActionListener(e -> {
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
        cryptoForm.getDigestFileButton().addActionListener(e -> {
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
                App.config.setDigestFilePath(filePath);
                App.config.save();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 文件摘要加密-文件浏览按钮
        cryptoForm.getExploreButton().addActionListener(e -> {
            File beforeFile = new File(App.config.getDigestFilePath());
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

        cryptoForm.getGenerateUuidButton().addActionListener(e -> {
            String randomUUID = IdUtil.randomUUID();
            cryptoForm.getUuidTextField().setText(randomUUID);
        });

        cryptoForm.getGenerateRandomNumButton().addActionListener(e -> {
            try {
                int digit = Integer.parseInt(cryptoForm.getRandomNumDigitTextField().getText().trim());
                String randomNumbers = RandomUtil.randomNumbers(digit);
                cryptoForm.getRandomNumTextField().setText(randomNumbers);
                App.config.setRandomNumDigit(digit);
                App.config.save();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        cryptoForm.getGenerateRadomStringButton().addActionListener(e -> {
            try {
                int digit = Integer.parseInt(cryptoForm.getRandomStringDigitTextField().getText().trim());
                String randomString = RandomUtil.randomString(RandomUtil.BASE_NUMBER + RandomUtil.BASE_CHAR + RandomUtil.BASE_CHAR.toUpperCase(), digit);
                cryptoForm.getRandomStringTextField().setText(randomString);
                App.config.setRandomStringDigit(digit);
                App.config.save();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        cryptoForm.getToUpperCaseButton().addActionListener(e -> {
            cryptoForm.getRandomStringTextField().setText(cryptoForm.getRandomStringTextField().getText().toUpperCase());
        });

        cryptoForm.getToLowerCaseButton().addActionListener(e -> {
            cryptoForm.getRandomStringTextField().setText(cryptoForm.getRandomStringTextField().getText().toLowerCase());
        });

        cryptoForm.getGenerateRandomPasswordButton().addActionListener(e -> {
            try {
                int digit = Integer.parseInt(cryptoForm.getRandomPasswordDigitTextField().getText().trim());
                String randomPassword = RandomUtil.randomString(RANDOM_BASE_STRING, digit);
                cryptoForm.getRandomPasswordTextField().setText(randomPassword);
                App.config.setRandomPasswordDigit(digit);
                App.config.save();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        cryptoForm.getCopyUuidButton().addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getUuidTextField().getText());
                AlertUtil.buttonInfo(cryptoForm.getCopyUuidButton(), "复制", "已复制", 2000);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });

        cryptoForm.getCopyRandomNumButton().addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getRandomNumTextField().getText());
                AlertUtil.buttonInfo(cryptoForm.getCopyRandomNumButton(), "复制", "已复制", 2000);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });

        cryptoForm.getCopyRadomStringButton().addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getRandomStringTextField().getText());
                AlertUtil.buttonInfo(cryptoForm.getCopyRadomStringButton(), "复制", "已复制", 2000);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });

        cryptoForm.getCopyRandomPasswordButton().addActionListener(e -> {
            try {
                ClipboardUtil.setStr(cryptoForm.getRandomPasswordTextField().getText());
                AlertUtil.buttonInfo(cryptoForm.getCopyRandomPasswordButton(), "复制", "已复制", 2000);
            } catch (Exception e1) {
                logger.error(e1);
            }
        });

        // 生成一对密钥
        cryptoForm.getAsymKeyGenerateButton().addActionListener(e -> {
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
        cryptoForm.getAsymEncryptWithPubKeyButton().addActionListener(e -> {
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
        cryptoForm.getAsymDecryptWithPrivateKeyButton().addActionListener(e -> {
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
        cryptoForm.getAsymEncryptWithPrivateKeyButton().addActionListener(e -> {
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

        // 使用公钥解密
        cryptoForm.getAsymDecryptWithPubKeyButton().addActionListener(e -> {
            try {
                String asymType = (String) cryptoForm.getAsymComboBox().getSelectedItem();
                String publicKeyStr = cryptoForm.getAsymPubKeyTextArea().getText();
                String toDecryptStr = cryptoForm.getAsymRightTextArea().getText();
                String decryptStr = "";
                if ("RSA".equals(asymType)) {
                    RSA rsa = new RSA(null, publicKeyStr);
                    decryptStr = StrUtil.str(rsa.decrypt(Base64.decode(toDecryptStr), KeyType.PublicKey), CharsetUtil.CHARSET_UTF_8);
                }

                cryptoForm.getAsymLeftTextArea().setText(decryptStr);
                cryptoForm.getAsymLeftTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "解密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // Base64加密
        cryptoForm.getBase64EncodeButton().addActionListener(e -> {
            try {
                String type = (String) cryptoForm.getBase64ComboBox().getSelectedItem();
                String text = cryptoForm.getBase64LeftTextArea().getText();
                String encode = "";
                if ("Base64".equals(type)) {
                    encode = Base64.encode(text);
                } else if ("Base32".equals(type)) {
                    encode = Base32.encode(text);
                }

                cryptoForm.getBase64RightTextArea().setText(encode);
                cryptoForm.getBase64RightTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "加密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // Base64解密
        cryptoForm.getBase64DecodeButton().addActionListener(e -> {
            try {
                String type = (String) cryptoForm.getBase64ComboBox().getSelectedItem();
                String text = cryptoForm.getBase64RightTextArea().getText();
                String decode = "";
                if ("Base64".equals(type)) {
                    decode = Base64.decodeStr(text);
                } else if ("Base32".equals(type)) {
                    decode = Base32.decodeStr(text);
                }

                cryptoForm.getBase64LeftTextArea().setText(decode);
                cryptoForm.getBase64LeftTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "解密失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
    }
}
