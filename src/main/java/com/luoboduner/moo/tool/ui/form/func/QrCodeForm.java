package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TFuncContentMapper;
import com.luoboduner.moo.tool.dao.TQrCodeMapper;
import com.luoboduner.moo.tool.domain.TFuncContent;
import com.luoboduner.moo.tool.domain.TQrCode;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.component.ImagePreviewComponent;
import com.luoboduner.moo.tool.ui.listener.func.QrCodeListener;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

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
    private ImagePreviewComponent qrCodeImagePreview;
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
    private JSplitPane splitPane;
    private JButton fromClipBoardButton;
    private JTextArea historyTextArea;
    private JScrollPane historyScrollPane;

    private static final Log logger = LogFactory.get();

    private static QrCodeForm qrCodeForm;

    private static boolean i18nRegistered;

    private static final String[] ERROR_CORRECTION_KEYS = {
            "qrcode.error.low", "qrcode.error.mediumLow", "qrcode.error.mediumHigh", "qrcode.error.high"
    };

    private static TQrCodeMapper qrCodeMapper = MybatisUtil.getSqlSession().getMapper(TQrCodeMapper.class);

    private static TFuncContentMapper funcContentMapper = MybatisUtil.getSqlSession().getMapper(TFuncContentMapper.class);

    private static final int DEFAULT_PRIMARY_KEY = 1;

    public static File qrCodeImageTempFile = null;

    private QrCodeForm() {
        UndoUtil.register(this);
    }

    public static String getRecognitionImagePath() {
        return getInstance().getRecognitionImagePathTextField().getText().trim();
    }

    public static String decodeImageFile(String recognitionImagePath) throws Exception {
        return QrCodeUtil.decode(FileUtil.file(recognitionImagePath));
    }

    public static void applyRecognitionResult(String recognitionImagePath, String decode) {
        qrCodeForm = getInstance();
        qrCodeForm.getRecognitionContentTextArea().setText(decode);
        App.config.setQrCodeRecognitionImagePath(recognitionImagePath);
        App.config.save();
        qrCodeForm.getQrCodePanel().updateUI();
        QrCodeListener.output("从文件识别:\n" + decode);
    }

    public static void recognition() {
        String recognitionImagePath = getRecognitionImagePath();
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return decodeImageFile(recognitionImagePath);
            }

            @Override
            protected void done() {
                try {
                    applyRecognitionResult(recognitionImagePath, get());
                } catch (Exception ex) {
                    MsgUtil.errorWithDetail(App.mainFrame, "msg.qrRecognizeFailed", ex.getMessage());
                    logger.error(ExceptionUtils.getStackTrace(ex));
                }
            }
        };
        worker.execute();
    }

    public static void recognitionFromClipBoard() {
        qrCodeForm = getInstance();
        Image image = ClipboardUtil.getImage();
        if (image != null) {
            String recognitionContent = QrCodeUtil.decode(image);
            qrCodeForm.getRecognitionContentTextArea().setText(recognitionContent);
            qrCodeForm.getQrCodePanel().updateUI();

            QrCodeListener.output("从剪贴板识别:\n" + recognitionContent);
        } else {
            MsgUtil.info(App.mainFrame, "msg.clipboardNoImage");
        }
    }

    @Getter
    public static class GenerateRequest {
        private final String content;
        private final int size;
        private final String logoPath;
        private final String errorCorrectionLevel;
        private final boolean save;

        private GenerateRequest(String content, int size, String logoPath, String errorCorrectionLevel, boolean save) {
            this.content = content;
            this.size = size;
            this.logoPath = logoPath;
            this.errorCorrectionLevel = errorCorrectionLevel;
            this.save = save;
        }
    }

    public static GenerateRequest collectGenerateRequest(boolean save) {
        qrCodeForm = getInstance();
        int size = Integer.parseInt(qrCodeForm.getSizeTextField().getText().trim());
        return new GenerateRequest(
                qrCodeForm.getToGenerateContentTextArea().getText(),
                size,
                qrCodeForm.getLogoPathTextField().getText(),
                (String) qrCodeForm.getErrorCorrectionLevelComboBox().getSelectedItem(),
                save
        );
    }

    public static BufferedImage generateImage(GenerateRequest request) throws Exception {
        String nowTime = DateUtil.now().replace(":", "-").replace(" ", "-");
        qrCodeImageTempFile = FileUtil.file(App.tempDir + File.separator + "qrCode-" + nowTime + ".jpg");

        QrConfig config = new QrConfig(request.getSize(), request.getSize());
        if (StringUtils.isNotBlank(request.getLogoPath())) {
            try {
                config.setImg(request.getLogoPath());
            } catch (Exception e) {
                logger.error("生成二维码设置log异常{}", ExceptionUtils.getStackTrace(e));
            }
        }
        applyErrorCorrection(config, request.getErrorCorrectionLevel());
        QrCodeUtil.generate(request.getContent(), config, qrCodeImageTempFile);
        BufferedImage image = ImageIO.read(qrCodeImageTempFile);
        if (image == null) {
            throw new IOException("无法读取生成的二维码图片");
        }
        return image;
    }

    public static void showGeneratedImage(BufferedImage image, GenerateRequest request) {
        qrCodeForm = getInstance();
        ImagePreviewComponent preview = qrCodeForm.getQrCodeImagePreview();
        preview.setCrispPixels(true);
        preview.setSourceImage(image, 1.0, true);
        qrCodeForm.getQrCodePanel().revalidate();
        qrCodeForm.getQrCodePanel().repaint();
        if (request.isSave()) {
            saveConfig();
            QrCodeListener.output("生成:\n" + request.getContent());
        }
    }

    public static void generate(Boolean save) {
        try {
            GenerateRequest request = collectGenerateRequest(save);
            showGeneratedImage(generateImage(request), request);
        } catch (Exception ex) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.generateFailed", ex.getMessage());
            logger.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    private static void applyErrorCorrection(QrConfig config, String errorCorrectionLevel) {
        int index = qrCodeForm.getErrorCorrectionLevelComboBox().getSelectedIndex();
        if (index == 0) {
            config.setErrorCorrection(ErrorCorrectionLevel.L);
        } else if (index == 1) {
            config.setErrorCorrection(ErrorCorrectionLevel.M);
        } else if (index == 2) {
            config.setErrorCorrection(ErrorCorrectionLevel.Q);
        } else if (index == 3) {
            config.setErrorCorrection(ErrorCorrectionLevel.H);
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

    public static int saveContent() {
        qrCodeForm = getInstance();
        String text = qrCodeForm.getHistoryTextArea().getText();
        String now = SqliteUtil.nowDateForSqlite();

        TFuncContent tFuncContent = funcContentMapper.selectByFunc(FuncConsts.QR_CODE);
        if (tFuncContent == null) {
            tFuncContent = new TFuncContent();
            tFuncContent.setFunc(FuncConsts.QR_CODE);
            tFuncContent.setContent(text);
            tFuncContent.setCreateTime(now);
            tFuncContent.setModifiedTime(now);

            return funcContentMapper.insert(tFuncContent);
        } else {
            tFuncContent.setContent(text);
            tFuncContent.setModifiedTime(now);
            return funcContentMapper.updateByPrimaryKeySelective(tFuncContent);
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

        TFuncContent tFuncContent = funcContentMapper.selectByFunc(FuncConsts.QR_CODE);
        if (tFuncContent != null) {
            qrCodeForm.getHistoryTextArea().setText(tFuncContent.getContent());
            // 滚动到最后一行
            qrCodeForm.getHistoryTextArea().setCaretPosition(qrCodeForm.getHistoryTextArea().getText().length());
        }

        generate(false);

        QrCodeListener.addListeners();

        ScrollUtil.smoothPane(qrCodeForm.getHistoryScrollPane());

        qrCodeForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(QrCodeForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setTabTitle(tabbedPane1, 0, "qrcode.tab.generate");
        I18nUiUtil.setTabTitle(tabbedPane1, 1, "qrcode.tab.recognize");
        I18nUiUtil.setTabTitle(tabbedPane1, 2, "qrcode.tab.history");
        I18nUiUtil.setText(generateButton, "qrcode.generate");
        I18nUiUtil.setText(saveAsButton, "common.save");
        I18nUiUtil.setText(recognitionButton, "qrcode.recognize");
        I18nUiUtil.setText(fromClipBoardButton, "qrcode.fromClipboard");
        I18nUiUtil.localizeTree(controlPanel, Map.of(
                "大小", "qrcode.size",
                "纠错级别", "qrcode.errorLevel",
                "Logo图片", "qrcode.logoImage"
        ));
        if (tabbedPane1.getTabCount() > 1 && tabbedPane1.getComponentAt(1) instanceof Container recognizeTab) {
            I18nUiUtil.localizeTree(recognizeTab, Map.of(
                    "二维码图片路径", "qrcode.imagePath"
            ));
        }
        errorCorrectionLevelComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index >= 0 && index < ERROR_CORRECTION_KEYS.length) {
                    setText(I18n.get(ERROR_CORRECTION_KEYS[index]));
                }
                return this;
            }
        });
    }

    private static void applyI18nStatic() {
        if (qrCodeForm != null) {
            qrCodeForm.applyI18n();
        }
    }

    private static void initUi() {
        qrCodeForm.getGeneratePanel().removeAll();
        if (App.config.isMenuBarOnTop()) {
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getControlPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getSplitPane(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        } else if (App.config.isMenuBarOnBottom()) {
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getControlPanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            qrCodeForm.getGeneratePanel().add(qrCodeForm.getSplitPane(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        }

        qrCodeForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 2));

        if (qrCodeForm.getQrCodeImagePreview() == null) {
            Container parent = qrCodeForm.getQrCodeImageLabel().getParent();
            parent.remove(qrCodeForm.getQrCodeImageLabel());
            qrCodeForm.qrCodeImagePreview = new ImagePreviewComponent();
            qrCodeForm.qrCodeImagePreview.setCrispPixels(true);
            parent.add(qrCodeForm.qrCodeImagePreview, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        }

        Style.blackTextArea(qrCodeForm.getRecognitionContentTextArea());
        Style.blackTextArea(qrCodeForm.getToGenerateContentTextArea());

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
        ScrollUtil.smoothPane(qrCodeForm.getGenerateScrollPane());

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
        qrCodePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        qrCodePanel.setMinimumSize(new Dimension(400, 300));
        qrCodePanel.setPreferredSize(new Dimension(400, 300));
        tabbedPane1 = new JTabbedPane();
        qrCodePanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        generatePanel = new JPanel();
        generatePanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("生成", generatePanel);
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(1, 10, new Insets(0, 0, 0, 0), -1, -1));
        generatePanel.add(controlPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        saveAsButton.setText("保存");
        controlPanel.add(saveAsButton, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(500);
        splitPane.setLastDividerLocation(200);
        generatePanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        splitPane.setLeftComponent(scrollPane1);
        toGenerateContentTextArea = new JTextArea();
        toGenerateContentTextArea.setMargin(new Insets(8, 8, 8, 8));
        scrollPane1.setViewportView(toGenerateContentTextArea);
        generateMainPanel = new JPanel();
        generateMainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(generateMainPanel);
        generateScrollPane = new JScrollPane();
        generateMainPanel.add(generateScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        generateScrollPane.setViewportView(panel1);
        qrCodeImageLabel = new JLabel();
        qrCodeImageLabel.setHorizontalAlignment(0);
        qrCodeImageLabel.setHorizontalTextPosition(0);
        qrCodeImageLabel.setText("");
        panel1.add(qrCodeImageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("识别", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
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
        final JSeparator separator1 = new JSeparator();
        separator1.setOrientation(1);
        panel3.add(separator1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fromClipBoardButton = new JButton();
        fromClipBoardButton.setText("从剪贴板");
        panel3.add(fromClipBoardButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel2.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        recognitionContentTextArea = new JTextArea();
        recognitionContentTextArea.setMargin(new Insets(8, 8, 8, 8));
        scrollPane2.setViewportView(recognitionContentTextArea);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("历史记录", panel4);
        historyScrollPane = new JScrollPane();
        panel4.add(historyScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        historyTextArea = new JTextArea();
        historyScrollPane.setViewportView(historyTextArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return qrCodePanel;
    }

}
