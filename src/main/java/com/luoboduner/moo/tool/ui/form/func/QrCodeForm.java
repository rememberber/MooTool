package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQrCodeMapper;
import com.luoboduner.moo.tool.domain.TQrCode;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * <pre>
 * 二维码
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/9.
 */
@Getter
public class QrCodeForm {
    private JTabbedPane tabbedPane1;
    private JPanel qrCodePanel;
    private JTextField recognitionImagePathTextField;
    private JButton recognitionExploreButton;
    private JTextArea recognitionContentTextArea;
    private JTextArea toGenerateContentTextArea;
    private JButton generateButton;
    private JLabel qrCodeImageLabel;
    private JComboBox errorCorrectionLevelComboBox;
    private JTextField sizeTextField;
    private JTextField logoPathTextField;
    private JButton exploreButton;
    private JButton saveAsButton;
    private JButton recognitionButton;
    private JPanel generatePanel;
    private JPanel controlPanel;
    private JScrollPane generateScrollPane;
    private JPanel generateMainPanel;

    private static final Log logger = LogFactory.get();

    private static QrCodeForm qrCodeForm;

    private static TQrCodeMapper qrCodeMapper = MybatisUtil.getSqlSession().getMapper(TQrCodeMapper.class);

    private static final int DEFAULT_PRIMARY_KEY = 1;

    private static File tempDir = null;

    public static File qrCodeImageTempFile = null;

    private QrCodeForm() {
        UndoUtil.register(this);
    }

    public static void recognition() {
        ThreadUtil.execute(() -> {
            try {
                qrCodeForm = getInstance();
                String recognitionImagePath = qrCodeForm.getRecognitionImagePathTextField().getText().trim();
                String decode = QrCodeUtil.decode(FileUtil.file(recognitionImagePath));
                qrCodeForm.getRecognitionContentTextArea().setText(decode);
                App.config.setQrCodeRecognitionImagePath(recognitionImagePath);
                App.config.save();
                qrCodeForm.getQrCodePanel().updateUI();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(App.mainFrame, "识别失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
    }

    public static void generate() {
        try {
            qrCodeForm = getInstance();
            String nowTime = DateUtil.now().replace(":", "-").replace(" ", "-");
            qrCodeImageTempFile = FileUtil.file(tempDir + File.separator + "qrCode-" + nowTime + ".jpg");

            int size = Integer.parseInt(qrCodeForm.getSizeTextField().getText());
            QrConfig config = new QrConfig(size, size);
            String logoPath = qrCodeForm.getLogoPathTextField().getText();
            if (StringUtils.isNotBlank(logoPath)) {
                config.setImg(logoPath);
            }
            String errorCorrectionLevel = (String) qrCodeForm.getErrorCorrectionLevelComboBox().getSelectedItem();
            if ("低".equals(errorCorrectionLevel)) {
                config.setErrorCorrection(ErrorCorrectionLevel.L);
            } else if ("中低".equals(errorCorrectionLevel)) {
                config.setErrorCorrection(ErrorCorrectionLevel.M);
            } else if ("中高".equals(errorCorrectionLevel)) {
                config.setErrorCorrection(ErrorCorrectionLevel.Q);
            } else if ("高".equals(errorCorrectionLevel)) {
                config.setErrorCorrection(ErrorCorrectionLevel.H);
            }
            QrCodeUtil.generate(qrCodeForm.getToGenerateContentTextArea().getText(), config, qrCodeImageTempFile);
            BufferedImage image = ImageIO.read(qrCodeImageTempFile);
            ImageIcon imageIcon = new ImageIcon(image);
            qrCodeForm.getQrCodeImageLabel().setIcon(imageIcon);
            qrCodeForm.getQrCodePanel().updateUI();

            saveConfig();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(App.mainFrame, "生成失败！\n\n" + ex.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * 保存界面的配置信息
     */
    private static void saveConfig() {
        App.config.setQrCodeSize(Integer.parseInt(qrCodeForm.getSizeTextField().getText()));
        App.config.setQrCodeErrorCorrectionLevel(String.valueOf(qrCodeForm.getErrorCorrectionLevelComboBox().getSelectedItem()));
        App.config.setQrCodeLogoPath(qrCodeForm.getLogoPathTextField().getText());
        App.config.save();
        TQrCode tQrCode = new TQrCode();
        tQrCode.setId(DEFAULT_PRIMARY_KEY);
        tQrCode.setContent(qrCodeForm.getToGenerateContentTextArea().getText());
        String now = SqliteUtil.nowDateForSqlite();
        tQrCode.setModifiedTime(now);

        if (qrCodeMapper.selectByPrimaryKey(DEFAULT_PRIMARY_KEY) == null) {
            tQrCode.setCreateTime(now);
            qrCodeMapper.insert(tQrCode);
        } else {
            qrCodeMapper.updateByPrimaryKeySelective(tQrCode);
        }
    }

    public static QrCodeForm getInstance() {
        if (qrCodeForm == null) {
            qrCodeForm = new QrCodeForm();
        }
        return qrCodeForm;
    }

    public static void init() {
        qrCodeForm = getInstance();

        initUi();

        if (SystemUtil.isLinuxOs()) {
            tempDir = new File(SystemUtil.configHome + File.separator + "temp");
        } else {
            tempDir = new File(FileUtil.getTmpDirPath() + "MooTool");
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        FileUtil.clean(tempDir);

        generate();
    }

    private static void initUi() {
        qrCodeForm.getGeneratePanel().removeAll();
        if ("上方".equals(App.config.getMenuBarPosition())) {
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getControlPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getGenerateMainPanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        } else if ("下方".equals(App.config.getMenuBarPosition())) {
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getControlPanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getGenerateMainPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        }

        if ("Darcula(推荐)".equals(App.config.getTheme())) {
            Color bgColor = new Color(30, 30, 30);
            Color foreColor = new Color(187, 187, 187);
            qrCodeForm.getRecognitionContentTextArea().setBackground(bgColor);
            qrCodeForm.getRecognitionContentTextArea().setForeground(foreColor);

            qrCodeForm.getToGenerateContentTextArea().setBackground(bgColor);
            qrCodeForm.getToGenerateContentTextArea().setForeground(foreColor);
        }

        qrCodeForm.getSizeTextField().setText(String.valueOf(App.config.getQrCodeSize()));
        qrCodeForm.getErrorCorrectionLevelComboBox().setSelectedItem(App.config.getQrCodeErrorCorrectionLevel());
        qrCodeForm.getLogoPathTextField().setText(App.config.getQrCodeLogoPath());
        qrCodeForm.getRecognitionImagePathTextField().setText(App.config.getQrCodeRecognitionImagePath());
        TQrCode tQrCode = qrCodeMapper.selectByPrimaryKey(DEFAULT_PRIMARY_KEY);
        if (tQrCode != null && StringUtils.isNotEmpty(tQrCode.getContent())) {
            qrCodeForm.getToGenerateContentTextArea().setText(tQrCode.getContent());
        } else {
            qrCodeForm.getToGenerateContentTextArea().setText("https://github.com/rememberber/MooTool");
        }

        // 设置滚动条速度
        qrCodeForm.getGenerateScrollPane().getVerticalScrollBar().setUnitIncrement(16);
        qrCodeForm.getGenerateScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        qrCodeForm.getQrCodePanel().updateUI();
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
        qrCodePanel = new JPanel();
        qrCodePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        qrCodePanel.setMinimumSize(new Dimension(400, 300));
        qrCodePanel.setPreferredSize(new Dimension(400, 300));
        tabbedPane1 = new JTabbedPane();
        qrCodePanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        generatePanel = new JPanel();
        generatePanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("生成", generatePanel);
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(1, 10, new Insets(5, 5, 5, 5), -1, -1));
        generatePanel.add(controlPanel, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        generateButton = new JButton();
        generateButton.setText("生成");
        controlPanel.add(generateButton, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("px");
        controlPanel.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        errorCorrectionLevelComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("低");
        defaultComboBoxModel1.addElement("中低");
        defaultComboBoxModel1.addElement("中高");
        defaultComboBoxModel1.addElement("高");
        errorCorrectionLevelComboBox.setModel(defaultComboBoxModel1);
        controlPanel.add(errorCorrectionLevelComboBox, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("大小");
        controlPanel.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sizeTextField = new JTextField();
        controlPanel.add(sizeTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("纠错级别");
        controlPanel.add(label3, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Logo图片");
        controlPanel.add(label4, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logoPathTextField = new JTextField();
        controlPanel.add(logoPathTextField, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        exploreButton = new JButton();
        exploreButton.setText("…");
        controlPanel.add(exploreButton, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveAsButton = new JButton();
        saveAsButton.setText("保存为");
        controlPanel.add(saveAsButton, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateMainPanel = new JPanel();
        generateMainPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        generatePanel.add(generateMainPanel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        generateScrollPane = new JScrollPane();
        generateMainPanel.add(generateScrollPane, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        generateScrollPane.setViewportView(panel1);
        qrCodeImageLabel = new JLabel();
        qrCodeImageLabel.setHorizontalAlignment(0);
        qrCodeImageLabel.setHorizontalTextPosition(0);
        qrCodeImageLabel.setText("");
        panel1.add(qrCodeImageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        generateMainPanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        toGenerateContentTextArea = new JTextArea();
        toGenerateContentTextArea.setMargin(new Insets(8, 8, 8, 8));
        scrollPane1.setViewportView(toGenerateContentTextArea);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("识别", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(5, 5, 5, 5), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("二维码图片路径");
        panel3.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        recognitionImagePathTextField = new JTextField();
        panel3.add(recognitionImagePathTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        recognitionExploreButton = new JButton();
        recognitionExploreButton.setText("…");
        panel3.add(recognitionExploreButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        recognitionButton = new JButton();
        recognitionButton.setText("识别");
        panel3.add(recognitionButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel2.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        recognitionContentTextArea = new JTextArea();
        recognitionContentTextArea.setMargin(new Insets(8, 8, 8, 8));
        scrollPane2.setViewportView(recognitionContentTextArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return qrCodePanel;
    }

}
