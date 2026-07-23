package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.collect.Lists;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.ImagePreviewComponent;
import com.luoboduner.moo.tool.ui.component.ToolbarUiUtil;
import com.luoboduner.moo.tool.ui.listener.func.ImageListener;
import com.luoboduner.moo.tool.ui.startup.EdtGuard;
import com.luoboduner.moo.tool.ui.startup.ImageLoadData;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * ImageForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/12/13.
 */
@Getter
public class ImageForm {
    private JPanel imagePanel;
    private JList<String> imageList;
    private JButton deleteButton;
    private JButton 截图Button;
    private JButton saveFromClipboardButton;
    private JButton openButton;
    private JSplitPane splitPane;
    private JPanel showImagePanel;
    private JLabel showImageLabel;
    private ImagePreviewComponent imagePreview;
    private JScrollPane scrollPane;
    private JPanel menuPanel;
    private JButton copyToClipboardButton;
    private JButton saveButton;
    private JButton listItemButton;
    private JButton exportButton;
    private JPanel deletePanel;
    private JButton newButton;
    private JButton ocrButton;
    private JButton scaleImageButton;
    private JButton pressImageButton;
    private JButton fromBase64Button;
    private JButton toBase64Button;
    private JPanel imageControlPanel;

    private JToolBar imageToolBar;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton originalSizeButton;
    private JButton fitSizeButton;
    private JLabel imageInfoLabel;

    private static ImageForm imageForm;

    private static boolean i18nRegistered;

    private static boolean viewShellInitialized;

    private static final Log logger = LogFactory.get();

    private ImageForm() {
        UndoUtil.register(this);

        imageToolBar = new JToolBar();
        ToolbarUiUtil.configure(imageToolBar);
        zoomInButton = new JButton(new FlatSVGIcon("icon/zoom_in.svg"));
        zoomInButton.setToolTipText("放大");
        zoomOutButton = new JButton(new FlatSVGIcon("icon/zoom_out.svg"));
        zoomOutButton.setToolTipText("缩小");
        originalSizeButton = new JButton(new FlatSVGIcon("icon/actual_size.svg"));
        originalSizeButton.setToolTipText("原始尺寸");
        fitSizeButton = new JButton(new FlatSVGIcon("icon/fit_size.svg"));
        fitSizeButton.setToolTipText("适应窗口");
        imageToolBar.add(zoomInButton);
        imageToolBar.add(zoomOutButton);
        imageToolBar.add(originalSizeButton);
        imageToolBar.add(fitSizeButton);
        imageControlPanel.add(imageToolBar, BorderLayout.WEST);

        imageInfoLabel = new JLabel();
        imageInfoLabel.setToolTipText("图片信息");
        imageControlPanel.add(imageInfoLabel, BorderLayout.EAST);

        showImagePanel.remove(showImageLabel);
        imagePreview = new ImagePreviewComponent();
        showImagePanel.add(imagePreview, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    public static ImageForm getInstance() {
        if (imageForm == null) {
            imageForm = new ImageForm();
        }
        return imageForm;
    }

    public static void init() {
        createViewShell();
        initList();
    }

    public static JPanel createViewShell() {
        EdtGuard.assertEdt();
        imageForm = getInstance();
        if (viewShellInitialized) {
            return imageForm.getImagePanel();
        }
        initUi();
        ImageListener.addListeners();
        imageForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(ImageForm::applyI18nStatic);
            i18nRegistered = true;
        }
        viewShellInitialized = true;
        return imageForm.getImagePanel();
    }

    public static void bindLoadedData(ImageLoadData data) {
        EdtGuard.assertEdt();
        if (data == null) {
            initList();
            return;
        }
        applyImageListData(data);
    }

    public static void applyImageListData(ImageLoadData data) {
        EdtGuard.assertEdt();
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> imageList = imageForm.getImageList();
        imageList.setModel(model);
        List<String> fileNames = data.getFileNames();
        for (String fileName : fileNames) {
            model.addElement(fileName);
        }
        if (fileNames.isEmpty()) {
            return;
        }
        String selectedFileName = data.getSelectedFileName();
        int selectedIndex = 0;
        if (selectedFileName != null) {
            for (int i = 0; i < fileNames.size(); i++) {
                if (selectedFileName.equals(fileNames.get(i))) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        imageList.setSelectedIndex(selectedIndex);
        if (data.getPreviewImage() != null) {
            ImageListener.showDecodedImage(imageForm, selectedFileName, data.getPreviewImage(), data.getPreviewInfoText());
        } else {
            ImageListener.showImageByFileName(imageForm, selectedFileName != null ? selectedFileName : fileNames.get(0));
        }
    }

    private void applyI18n() {
        I18nUiUtil.setToolTip(zoomInButton, "image.tooltip.zoomIn");
        I18nUiUtil.setToolTip(zoomOutButton, "image.tooltip.zoomOut");
        I18nUiUtil.setToolTip(originalSizeButton, "image.tooltip.originalSize");
        I18nUiUtil.setToolTip(fitSizeButton, "image.tooltip.fitSize");
        I18nUiUtil.setToolTip(imageInfoLabel, "image.tooltip.imageInfo");
        I18nUiUtil.setToolTip(deleteButton, "common.delete");
        I18nUiUtil.setToolTip(exportButton, "common.export");
        I18nUiUtil.setToolTip(saveFromClipboardButton, "image.fromClipboard");
        I18nUiUtil.setToolTip(newButton, "common.new");
        I18nUiUtil.setToolTip(saveButton, "common.save");
        I18nUiUtil.setToolTip(copyToClipboardButton, "common.copy");

        I18nUiUtil.setText(newButton, "common.new");
        I18nUiUtil.setText(saveButton, "common.save");
        I18nUiUtil.setText(copyToClipboardButton, "common.copy");

        I18nUiUtil.localizeTree(imagePanel, Map.of(
                "截图", "image.screenshot",
                "从剪贴板获取", "image.fromClipboard",
                "从系统打开", "image.openFromSystem",
                "从Base64获取", "image.fromBase64",
                "压缩", "image.compress",
                "加水印", "image.watermark",
                "OCR识别", "image.ocr",
                "导出为Base64", "image.toBase64",
                "复制到剪贴板", "image.copyToClipboard"
        ));
    }

    private static void applyI18nStatic() {
        if (imageForm != null) {
            imageForm.applyI18n();
        }
    }

    private static void initUi() {
        imageForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        imageForm.getImageList().setFixedCellHeight(UiConsts.TABLE_ROW_HEIGHT);
        imageForm.getImageList().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        imageForm.getImageList().putClientProperty(FlatClientProperties.STYLE,
                "selectionArc: 6; selectionInsets: 0,1,0,1");

        imageForm.getDeletePanel().setVisible(false);

        imageForm.getListItemButton().setIcon(new FlatSVGIcon("icon/list.svg"));
        imageForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        imageForm.getExportButton().setIcon(new FlatSVGIcon("icon/export.svg"));

        // 设置滚动条速度
        ScrollUtil.smoothPane(imageForm.getScrollPane());

        imageForm.getSaveFromClipboardButton().grabFocus();

        imageForm.getImagePanel().updateUI();
    }

    public static void initList() {
        String previousSelectedName = ImageListener.selectedName;
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> imageList = imageForm.getImageList();
        imageList.setModel(model);

        if (!FileUtil.exist(ImageListener.IMAGE_PATH_PRE_FIX)) {
            FileUtil.mkdir(ImageListener.IMAGE_PATH_PRE_FIX);
        }
        List<String> fileNames = Lists.newArrayList();
        List<File> files = FileUtil.loopFiles(ImageListener.IMAGE_PATH_PRE_FIX);
        files.stream().sorted((f1, f2) -> {
            long diff = f2.lastModified() - f1.lastModified();
            if (diff > 0) {
                return 1;
            } else if (diff == 0) {
                return 0;
            } else {
                return -1;
            }
        }).forEach(file -> fileNames.add(file.getName()));

        for (String fileName : fileNames) {
            model.addElement(fileName);
        }
        if (fileNames.isEmpty()) {
            return;
        }
        if (StringUtils.isNotBlank(previousSelectedName)) {
            for (int i = 0; i < fileNames.size(); i++) {
                if (previousSelectedName.equals(FileUtil.mainName(fileNames.get(i)))) {
                    imageList.setSelectedIndex(i);
                    ImageListener.showImageByFileName(imageForm, fileNames.get(i));
                    return;
                }
            }
        }
        imageList.setSelectedIndex(0);
        ImageListener.showImageByFileName(imageForm, fileNames.get(0));
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
        imagePanel = new JPanel();
        imagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(204);
        splitPane.setDividerSize(10);
        imagePanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        deletePanel = new JPanel();
        deletePanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(deletePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        deletePanel.add(deleteButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        deletePanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        exportButton = new JButton();
        exportButton.setIcon(new ImageIcon(getClass().getResource("/icon/export_dark.png")));
        exportButton.setText("");
        exportButton.setToolTipText("导出");
        deletePanel.add(exportButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageList = new JList();
        scrollPane1.setViewportView(imageList);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel3);
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(menuPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        menuPanel.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        截图Button = new JButton();
        截图Button.setText("截图");
        panel5.add(截图Button, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        saveFromClipboardButton = new JButton();
        saveFromClipboardButton.setText("从剪贴板获取");
        saveFromClipboardButton.setToolTipText("Ctrl+V");
        panel5.add(saveFromClipboardButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openButton = new JButton();
        openButton.setText("从系统打开");
        panel5.add(openButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        newButton = new JButton();
        newButton.setText("新建");
        newButton.setToolTipText("Ctrl+N");
        panel5.add(newButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fromBase64Button = new JButton();
        fromBase64Button.setText("从Base64获取");
        panel5.add(fromBase64Button, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        scaleImageButton = new JButton();
        scaleImageButton.setText("压缩");
        panel6.add(scaleImageButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pressImageButton = new JButton();
        pressImageButton.setText("加水印");
        panel6.add(pressImageButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ocrButton = new JButton();
        ocrButton.setText("OCR识别");
        panel6.add(ocrButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setText("保存");
        saveButton.setToolTipText("Ctrl+S");
        panel6.add(saveButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyToClipboardButton = new JButton();
        copyToClipboardButton.setText("复制到剪贴板");
        copyToClipboardButton.setToolTipText("Ctrl+C");
        panel6.add(copyToClipboardButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        toBase64Button = new JButton();
        toBase64Button.setText("导出为Base64");
        panel6.add(toBase64Button, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        menuPanel.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        menuPanel.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        listItemButton = new JButton();
        listItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/listFiles_dark.png")));
        listItemButton.setText("");
        panel7.add(listItemButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel7.add(spacer5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        panel3.add(scrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        showImagePanel = new JPanel();
        showImagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane.setViewportView(showImagePanel);
        showImageLabel = new JLabel();
        showImageLabel.setText("");
        showImagePanel.add(showImageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageControlPanel = new JPanel();
        imageControlPanel.setLayout(new BorderLayout(0, 0));
        panel3.add(imageControlPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageControlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return imagePanel;
    }

}
