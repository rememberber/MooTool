package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.dialog.Base64Dialog;
import com.luoboduner.moo.tool.ui.dialog.ImageCompressDialog;
import com.luoboduner.moo.tool.ui.dialog.ImageOcrDialog;
import com.luoboduner.moo.tool.ui.dialog.ImageOcrResultDialog;
import com.luoboduner.moo.tool.ui.dialog.ImageWatermarkDialog;
import com.luoboduner.moo.tool.util.ImageCompressUtil;
import com.luoboduner.moo.tool.util.ImageDisplayUtil;
import com.luoboduner.moo.tool.util.ImageOcrUtil;
import com.luoboduner.moo.tool.util.ImageWatermarkUtil;
import com.luoboduner.moo.tool.util.TesseractEnvUtil;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.ImageForm;
import com.luoboduner.moo.tool.ui.frame.ScreenCaptureFrame;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.NamingUtil;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <pre>
 * ImageListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/12/16.
 */
@Slf4j
public class ImageListener {
    public static String selectedName;

    public static Image selectedImage;

    /** 相对原图的展示缩放比例，始终从原图重采样以避免多次缩放导致模糊 */
    private static double displayZoomFactor = 1.0;

    public static final String IMAGE_PATH_PRE_FIX = SystemUtil.CONFIG_HOME + File.separator + "images" + File.separator;

    public static final Image DEFAULT_IMAGE = Toolkit.getDefaultToolkit().getImage(ImageListener.class.getResource("/icon/image_128.png"));

    /** 忽略 JOptionPane 关闭后回传到列表的 Enter 键，避免重命名弹框重复弹出 */
    private static boolean suppressListEnterRename;

    public static void addListeners() {
        ImageForm imageForm = ImageForm.getInstance();

        // 截图
        imageForm.get截图Button().addActionListener(e -> captureScreen());

        // 从剪贴板获取
        imageForm.getSaveFromClipboardButton().addActionListener(e -> getImageFromClipboard());
        if (SystemUtil.isMacOs()) {
            imageForm.getImagePanel().registerKeyboardAction(e -> getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            imageForm.getImagePanel().registerKeyboardAction(e -> getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }//        MainWindow.getInstance().getTabbedPane().registerKeyboardAction(e -> {
//            int index = MainWindow.getInstance().getTabbedPane().getSelectedIndex();
//            if (index == 11) {
//                imageForm.getImageFromClipboard();
//            }
//        }, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 列表显示切换按钮事件
        imageForm.getListItemButton().addActionListener(e -> {
            int currentDividerLocation = imageForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                imageForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
            } else {
                imageForm.getSplitPane().setDividerLocation(0);
            }
        });

        // 压缩按钮事件
        imageForm.getScaleImageButton().addActionListener(e -> compressImages(imageForm));

        // 加水印按钮事件
        imageForm.getPressImageButton().addActionListener(e -> watermarkImages(imageForm));

        // OCR 识别按钮事件
        imageForm.getOcrButton().addActionListener(e -> ocrImages(imageForm));

        // 保存按钮事件
        imageForm.getSaveButton().addActionListener(e -> {
            saveImage();
        });
        if (SystemUtil.isMacOs()) {
            imageForm.getImagePanel().registerKeyboardAction(e -> quickSave(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            imageForm.getImagePanel().registerKeyboardAction(e -> quickSave(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        // 点击左侧列表事件
        imageForm.getImageList().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                quickSave();
                int index = imageForm.getImageList().locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }
                DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
                showImageByFileName(imageForm, listModel.getElementAt(index));
                super.mousePressed(e);
            }
        });

        // 左侧列表鼠标点击事件（显示下方删除按钮）
        imageForm.getImageList().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                imageForm.getDeletePanel().setVisible(true);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        // 浏览区点击事件，隐藏删除按钮
        imageForm.getShowImagePanel().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                imageForm.getDeletePanel().setVisible(false);
                // 双击打开图片
                int clickTimes = e.getClickCount();
                if (clickTimes == 2) {
                    File imageFile = resolveCurrentImageFile(imageForm);
                    if (imageFile == null) {
                        return;
                    }
                    try {
                        Desktop.getDesktop().open(imageFile);
                    } catch (IOException ex) {
                        log.error(ExceptionUtils.getStackTrace(ex));
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        // 从系统打开按钮事件
        imageForm.getOpenButton().addActionListener(e -> {
            File imageFile = resolveCurrentImageFile(imageForm);
            if (imageFile == null) {
                MsgUtil.info(imageForm.getImagePanel(), "msg.selectImageFirst");
                return;
            }
            try {
                Desktop.getDesktop().open(imageFile);
            } catch (IOException ex) {
                log.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 删除按钮事件
        imageForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(imageForm);
        });

        // 左侧列表按键事件（重命名）
        imageForm.getImageList().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (suppressListEnterRename) {
                        suppressListEnterRename = false;
                        return;
                    }
                    renameSelectedImage(imageForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(imageForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    quickSave();
                    int selectedIndex = imageForm.getImageList().getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
                    showImageByFileName(imageForm, listModel.getElementAt(selectedIndex));
                }
            }
        });

        // 复制到剪贴板
        imageForm.getCopyToClipboardButton().addActionListener(e -> copyToClipboard());
        if (SystemUtil.isMacOs()) {
            imageForm.getImagePanel().registerKeyboardAction(e -> copyToClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            imageForm.getImagePanel().registerKeyboardAction(e -> copyToClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        // 新建
        imageForm.getNewButton().addActionListener(e -> newImage());
        if (SystemUtil.isMacOs()) {
            imageForm.getImagePanel().registerKeyboardAction(e -> newImage(), KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            imageForm.getImagePanel().registerKeyboardAction(e -> newImage(), KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        // 导出
        imageForm.getExportButton().addActionListener(e -> {
            int[] selectedIndices = imageForm.getImageList().getSelectedIndices();

            try {
                if (selectedIndices.length > 0) {
                    SystemFileChooser fileChooser = new SystemFileChooser(App.config.getImageExportPath());
                    fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(imageForm.getImagePanel());
                    String exportPath;
                    if (approve == SystemFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setImageExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
                    for (int index : selectedIndices) {
                        String fileName = listModel.getElementAt(index);
                        File exportFile = FileUtil.touch(exportPath + File.separator + fileName);
                        FileUtil.copy(FileUtil.file(IMAGE_PATH_PRE_FIX + fileName), exportFile, true);
                    }
                    MsgUtil.success(imageForm.getImagePanel(), "msg.exportSuccess");
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    MsgUtil.info(imageForm.getImagePanel(), "msg.selectAtLeastOne");
                }

            } catch (Exception e1) {
                MsgUtil.errorWithDetail(imageForm.getImagePanel(), "msg.exportFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        // 导出为Base64
        imageForm.getToBase64Button().addActionListener(e -> {
            File imageFile = resolveCurrentImageFile(imageForm);
            if (imageFile == null || !imageFile.isFile()) {
                MsgUtil.info(imageForm.getImagePanel(), "msg.selectOrSaveImageFirst");
                return;
            }
            Base64Dialog dialog = new Base64Dialog();

            dialog.setToTextArea(Base64.encode(imageFile));

            dialog.pack();
            dialog.setVisible(true);
        });

        // 从Base64获取
        imageForm.getFromBase64Button().addActionListener(e -> {
            try {
                Base64Dialog.textInputValue = null;
                Base64Dialog.ok = false;
                Base64Dialog dialog = new Base64Dialog();
                dialog.pack();
                dialog.setVisible(true);

                if (Base64Dialog.ok && StringUtils.isNotBlank(Base64Dialog.textInputValue)) {
                    ImageIcon imageIcon = new ImageIcon(Base64.decode(Base64Dialog.textInputValue.getBytes()));
                    Image image = imageIcon.getImage();
                    ImageListener.selectedImage = image;
                    if (image != null) {
                        selectedName = null;
                        displayZoomFactor = 1.0;
                        updateImageDisplay(imageForm);

                        selectedName = NamingUtil.defaultUntitledName();
                        File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
                        ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                        ImageForm.initList();
                    } else {
                        MsgUtil.info(App.mainFrame, "msg.invalidImageBase64");
                    }
                }
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.base64FetchFailed", e1.getMessage());
                log.error("从Base64获取异常,{}", ExceptionUtils.getStackTrace(e1));
            }

        });

        imageForm.getZoomInButton().addActionListener(e -> {
            if (selectedImage == null) {
                return;
            }
            displayZoomFactor *= 1.1;
            updateImageDisplay(imageForm);
        });

        imageForm.getZoomOutButton().addActionListener(e -> {
            if (selectedImage == null) {
                return;
            }
            displayZoomFactor *= 0.9;
            updateImageDisplay(imageForm);
        });

        imageForm.getOriginalSizeButton().addActionListener(e -> {
            if (selectedImage == null) {
                return;
            }
            displayZoomFactor = 1.0;
            updateImageDisplay(imageForm);
        });

        imageForm.getFitSizeButton().addActionListener(e -> {
            if (selectedImage == null) {
                return;
            }
            double scale = ImageDisplayUtil.getScaleFactor(imageForm.getImagePreview());
            BufferedImage source = toBufferedImage(selectedImage);
            int baseLogicalWidth = Math.max(1, (int) Math.round(source.getWidth() / scale));
            int availableWidth = imageForm.getScrollPane().getViewport().getWidth();
            if (availableWidth <= 0) {
                availableWidth = imageForm.getImageControlPanel().getWidth();
            }
            if (availableWidth > 0) {
                displayZoomFactor = (double) availableWidth / baseLogicalWidth;
            }
            updateImageDisplay(imageForm);
        });

        // 左侧列表增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem(I18n.get("common.rename"));
        JMenuItem deleteMenuItem = new JMenuItem(I18n.get("common.delete"));
        JMenuItem exportMenuItem = new JMenuItem(I18n.get("common.export"));
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        noteListPopupMenu.add(exportMenuItem);
        imageForm.getImageList().setComponentPopupMenu(noteListPopupMenu);

        renameMenuItem.addActionListener(e -> renameSelectedImage(imageForm));

        deleteMenuItem.addActionListener(e -> {
            deleteFiles(imageForm);
        });

        exportMenuItem.addActionListener(e -> {
            int[] selectedIndices = imageForm.getImageList().getSelectedIndices();

            try {
                if (selectedIndices.length > 0) {
                    SystemFileChooser fileChooser = new SystemFileChooser(App.config.getImageExportPath());
                    fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(imageForm.getImagePanel());
                    String exportPath;
                    if (approve == SystemFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setImageExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
                    for (int index : selectedIndices) {
                        String fileName = listModel.getElementAt(index);
                        File exportFile = FileUtil.touch(exportPath + File.separator + fileName);
                        FileUtil.copy(FileUtil.file(IMAGE_PATH_PRE_FIX + fileName), exportFile, true);
                    }
                    MsgUtil.success(imageForm.getImagePanel(), "msg.exportSuccess");
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    MsgUtil.info(imageForm.getImagePanel(), "msg.selectAtLeastOne");
                }

            } catch (Exception e1) {
                MsgUtil.errorWithDetail(imageForm.getImagePanel(), "msg.exportFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

    }

    private static void compressImages(ImageForm imageForm) {
        int[] selectedIndices = imageForm.getImageList().getSelectedIndices();
        if (selectedIndices.length == 0) {
            MsgUtil.info(imageForm.getImagePanel(), "msg.selectAtLeastOneImage");
            return;
        }

        ImageCompressDialog dialog = new ImageCompressDialog(selectedIndices.length);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        ImageCompressUtil.CompressOptions options = dialog.getOptions();
        if (options.getOutputMode() == ImageCompressUtil.OutputMode.OVERWRITE) {
            int confirm = JOptionPane.showConfirmDialog(imageForm.getImagePanel(),
                    I18n.format("msg.confirmOverwrite", selectedIndices.length),
                    I18n.get("msg.confirmOverwriteTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
        int successCount = 0;
        long savedBytes = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (int index : selectedIndices) {
            String fileName = listModel.getElementAt(index);
            File sourceFile = FileUtil.file(IMAGE_PATH_PRE_FIX + fileName);
            ImageCompressUtil.CompressResult result = ImageCompressUtil.compress(sourceFile, options);
            if (result.isSuccess()) {
                successCount++;
                savedBytes += result.getSavedBytes();
                if (options.getOutputMode() == ImageCompressUtil.OutputMode.OVERWRITE
                        && !fileName.equals(result.getOutputFile().getName())) {
                    FileUtil.del(sourceFile);
                }
            } else {
                errorMessages.append(fileName).append("：").append(result.getErrorMessage()).append("\n");
            }
        }

        String refreshBaseName = selectedName;
        ImageForm.initList();

        if (refreshBaseName != null) {
            DefaultListModel<String> refreshedModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
            for (int i = 0; i < refreshedModel.size(); i++) {
                String name = refreshedModel.getElementAt(i);
                String baseName = FileUtil.mainName(name);
                if (refreshBaseName.equals(baseName) || baseName.equals(refreshBaseName + "_compressed")) {
                    imageForm.getImageList().setSelectedIndex(i);
                    showImageByFileName(imageForm, name);
                    break;
                }
            }
        }

        String suffix = "";
        if (savedBytes > 0) {
            suffix = I18n.format("msg.compressSavedSuffix", FileUtil.readableFileSize(savedBytes));
        } else if (successCount > 0) {
            suffix = I18n.get("msg.compressNoReduction");
        }
        String message = I18n.format("msg.compressResult", successCount, selectedIndices.length, suffix);
        if (errorMessages.length() > 0) {
            message += I18n.format("msg.compressFailedList", errorMessages);
        }
        MsgUtil.show(imageForm.getImagePanel(), message, "msg.compressCompleteTitle",
                errorMessages.length() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    private static void ocrImages(ImageForm imageForm) {
        List<File> imageFiles = resolveOcrImageFiles(imageForm);
        if (imageFiles.isEmpty()) {
            MsgUtil.info(imageForm.getImagePanel(), "msg.selectAtLeastOneImage");
            return;
        }

        ImageOcrDialog dialog = new ImageOcrDialog(imageFiles.size());
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        ImageOcrUtil.OcrOptions options = dialog.getOptions();
        JDialog progressDialog = new JDialog(App.mainFrame, "正在识别", true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel progressPanel = new JPanel(new BorderLayout(12, 12));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        progressPanel.add(new JLabel("正在识别文字，请稍候…"), BorderLayout.NORTH);
        progressPanel.add(new JProgressBar(), BorderLayout.CENTER);
        JPanel progressButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton cancelButton = new JButton("取消");
        progressButtonPanel.add(cancelButton);
        progressPanel.add(progressButtonPanel, BorderLayout.SOUTH);
        progressDialog.add(progressPanel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(imageForm.getImagePanel());

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return ImageOcrUtil.recognizeFiles(imageFiles, options);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                if (isCancelled()) {
                    return;
                }
                try {
                    String result = get();
                    if (StringUtils.isBlank(result)) {
                        MsgUtil.show(imageForm.getImagePanel(), I18n.get("msg.ocrNoText"), "msg.ocrTitle",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    ImageOcrResultDialog resultDialog = new ImageOcrResultDialog(result);
                    resultDialog.setVisible(true);
                } catch (Exception ex) {
                    String message = ex.getMessage();
                    Throwable cause = ex.getCause();
                    if (cause instanceof UnsatisfiedLinkError
                            || StringUtils.containsIgnoreCase(message, "UnsatisfiedLinkError")
                            || StringUtils.containsIgnoreCase(message, "libtesseract")) {
                        message = TesseractEnvUtil.getInstallHint();
                    }
                    MsgUtil.errorWithDetail(imageForm.getImagePanel(), "msg.ocrFailed", message);
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
            }
        };
        cancelButton.addActionListener(ev -> {
            worker.cancel(true);
            progressDialog.dispose();
        });
        worker.execute();
        progressDialog.setVisible(true);
    }

    private static File resolveCurrentImageFile(ImageForm imageForm) {
        int selectedIndex = imageForm.getImageList().getSelectedIndex();
        DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
        if (selectedIndex >= 0) {
            return FileUtil.file(IMAGE_PATH_PRE_FIX + listModel.getElementAt(selectedIndex));
        }
        if (StringUtils.isNotBlank(selectedName)) {
            for (int i = 0; i < listModel.size(); i++) {
                String fileName = listModel.getElementAt(i);
                if (selectedName.equals(FileUtil.mainName(fileName))) {
                    return FileUtil.file(IMAGE_PATH_PRE_FIX + fileName);
                }
            }
            File pngFile = FileUtil.file(IMAGE_PATH_PRE_FIX + selectedName + ".png");
            if (pngFile.isFile()) {
                return pngFile;
            }
        }
        return null;
    }

    private static List<File> resolveOcrImageFiles(ImageForm imageForm) {
        List<File> files = new ArrayList<>();
        int[] selectedIndices = imageForm.getImageList().getSelectedIndices();
        DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
        if (selectedIndices.length > 0) {
            for (int index : selectedIndices) {
                files.add(FileUtil.file(IMAGE_PATH_PRE_FIX + listModel.getElementAt(index)));
            }
            return files;
        }
        if (StringUtils.isNotBlank(selectedName)) {
            for (int i = 0; i < listModel.size(); i++) {
                String fileName = listModel.getElementAt(i);
                if (selectedName.equals(FileUtil.mainName(fileName))) {
                    files.add(FileUtil.file(IMAGE_PATH_PRE_FIX + fileName));
                    break;
                }
            }
        }
        return files;
    }

    private static void watermarkImages(ImageForm imageForm) {
        int[] selectedIndices = imageForm.getImageList().getSelectedIndices();
        if (selectedIndices.length == 0) {
            MsgUtil.info(imageForm.getImagePanel(), "msg.selectAtLeastOneImage");
            return;
        }

        ImageWatermarkDialog dialog = new ImageWatermarkDialog(selectedIndices.length);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        ImageWatermarkUtil.WatermarkOptions options = dialog.getOptions();
        if (options.getOutputMode() == ImageWatermarkUtil.OutputMode.OVERWRITE) {
            int confirm = JOptionPane.showConfirmDialog(imageForm.getImagePanel(),
                    I18n.format("msg.confirmOverwrite", selectedIndices.length),
                    I18n.get("msg.confirmOverwriteTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
        int successCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (int index : selectedIndices) {
            String fileName = listModel.getElementAt(index);
            File sourceFile = FileUtil.file(IMAGE_PATH_PRE_FIX + fileName);
            ImageWatermarkUtil.WatermarkResult result = ImageWatermarkUtil.watermark(sourceFile, options);
            if (result.isSuccess()) {
                successCount++;
            } else {
                errorMessages.append(fileName).append("：").append(result.getErrorMessage()).append("\n");
            }
        }

        String refreshBaseName = selectedName;
        ImageForm.initList();

        if (refreshBaseName != null) {
            DefaultListModel<String> refreshedModel = (DefaultListModel<String>) imageForm.getImageList().getModel();
            for (int i = 0; i < refreshedModel.size(); i++) {
                String name = refreshedModel.getElementAt(i);
                String baseName = FileUtil.mainName(name);
                if (refreshBaseName.equals(baseName) || baseName.equals(refreshBaseName + "_watermarked")) {
                    imageForm.getImageList().setSelectedIndex(i);
                    showImageByFileName(imageForm, name);
                    break;
                }
            }
        }

        String message = I18n.format("msg.watermarkResult", successCount, selectedIndices.length, "");
        if (errorMessages.length() > 0) {
            message += I18n.format("msg.watermarkFailedList", errorMessages);
        }
        MsgUtil.show(imageForm.getImagePanel(), message, "msg.watermarkCompleteTitle",
                errorMessages.length() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showImageByFileName(ImageForm imageForm, String fileName) {
        try {
            imageForm.getImagePreview().setPlaceholderImage(DEFAULT_IMAGE);

            selectedName = FileUtil.mainName(fileName);
            selectedImage = ImageIO.read(FileUtil.newFile(IMAGE_PATH_PRE_FIX + fileName));
            if (selectedImage == null) {
                MsgUtil.error(App.mainFrame, "msg.cannotReadImage", fileName);
                return;
            }

            displayZoomFactor = 1.0;
            updateImageDisplay(imageForm);
            imageForm.getShowImagePanel().updateUI();

            String pixel = selectedImage.getWidth(null) + " x " + selectedImage.getHeight(null);
            String size = FileUtil.readableFileSize(FileUtil.file(IMAGE_PATH_PRE_FIX + fileName).length());
            imageForm.getImageInfoLabel().setText(I18n.format("image.info.size", pixel, size));
        } catch (IOException ex) {
            MsgUtil.errorDetail(App.mainFrame, "common.exception", ex.getMessage());
            log.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    private static void renameSelectedImage(ImageForm imageForm) {
        int selectedIndex = imageForm.getImageList().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        DefaultListModel<String> model = (DefaultListModel<String>) imageForm.getImageList().getModel();
        String beforeName = model.getElementAt(selectedIndex);
        if (StringUtils.isBlank(beforeName)) {
            return;
        }
        suppressListEnterRename = true;
        String afterName = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            FileUtil.rename(FileUtil.file(IMAGE_PATH_PRE_FIX + beforeName), afterName.replace(".png", ""), true, true);
            model.set(selectedIndex, afterName);
            selectedName = afterName.replace(".png", "");
        } catch (Exception e) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.renameFailedDetail", e.getMessage());
            ImageForm.initList();
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void deleteFiles(ImageForm imageForm) {
        try {
            int[] selectedIndices = imageForm.getImageList().getSelectedIndices();

            if (selectedIndices.length == 0) {
                MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            } else {
                int isDelete = MsgUtil.confirm(App.mainFrame, "msg.confirmDelete");
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultListModel<String> listModel = (DefaultListModel<String>) imageForm.getImageList().getModel();

                    for (int selectedIndex : selectedIndices) {
                        String fileName = listModel.getElementAt(selectedIndex);
                        FileUtil.del(IMAGE_PATH_PRE_FIX + fileName);
                    }
                    selectedName = null;
                    ImageForm.initList();
                }
            }
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.deleteFailed", e1.getMessage());
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    private static void newImage() {
        ImageForm imageForm = ImageForm.getInstance();
        imageForm.getImagePreview().setPlaceholderImage(DEFAULT_IMAGE);
        selectedName = null;
        selectedImage = null;
        displayZoomFactor = 1.0;

        Image image = ClipboardUtil.getImage();
        ImageListener.selectedImage = image;
        if (image != null) {
            updateImageDisplay(imageForm);
        }
    }

    public static void captureScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        if (!gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            MsgUtil.info(App.mainFrame, "msg.screenshotUnsupported");
            return;
        }

        App.mainFrame.setVisible(false);
        Timer timer = new Timer(150, evt -> {
            ((Timer) evt.getSource()).stop();
            ScreenCaptureFrame.start(
                    ImageListener::loadCapturedImage,
                    Init::showMainFrame
            );
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void loadCapturedImage(BufferedImage image) {
        try {
            Init.showMainFrame();
            ImageForm imageForm = ImageForm.getInstance();
            selectedImage = image;
            selectedName = "截图_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            displayZoomFactor = 1.0;
            updateImageDisplay(imageForm);

            File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
            ImageIO.write(image, "png", imageFile);
            ImageForm.initList();

            String pixel = image.getWidth() + " x " + image.getHeight();
            String size = FileUtil.readableFileSize(imageFile.length());
            imageForm.getImageInfoLabel().setText(I18n.format("image.info.size", pixel, size));
        } catch (Exception ex) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.screenshotSaveFailed", ex.getMessage());
            log.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    public static void getImageFromClipboard() {
        try {
            ImageForm imageForm = ImageForm.getInstance();
            Image image = ClipboardUtil.getImage();
            ImageListener.selectedImage = image;
            if (image != null) {
                selectedName = null;
                displayZoomFactor = 1.0;
                updateImageDisplay(imageForm);

                selectedName = NamingUtil.defaultUntitledName();
                File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
                ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                ImageForm.initList();
            } else {
                MsgUtil.info(App.mainFrame, "msg.clipboardNoImage");
            }
        } catch (HeadlessException ex) {
            ex.printStackTrace();
            log.error(ExceptionUtils.getStackTrace(ex));
        } catch (IOException e) {
            e.printStackTrace();
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void copyToClipboard() {
        if (selectedImage == null) {
            MsgUtil.info(App.mainFrame, "msg.selectImageFirst");
            return;
        }
        try {
            ClipboardUtil.setImage(selectedImage);
            ImageForm imageForm = ImageForm.getInstance();
            AlertUtil.buttonInfo(imageForm.getCopyToClipboardButton(), "复制到剪贴板", "已复制", 2000);

        } catch (Exception e1) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.copyFailed", e1.getMessage());
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    /**
     * save for manual
     */
    private static void saveImage() {
        if (StringUtils.isEmpty(selectedName)) {
            selectedName = NamingUtil.defaultUntitledName();
        }
        String name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), selectedName);
        if (StringUtils.isNotBlank(name)) {
            try {
                if (selectedImage != null) {
                    File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + name + ".png"));
                    ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initList();
                }
            } catch (Exception ex) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.saveFailed", ex.getMessage());
                log.error(ExceptionUtils.getStackTrace(ex));
            }
        }
    }

    /**
     * save for quick key and item change
     */
    private static void quickSave() {
        try {
            if (selectedImage != null && selectedName == null) {
                String tempName = NamingUtil.defaultUntitledName();
                String name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), tempName);
                if (StringUtils.isNotBlank(name)) {
                    name = name.replace(".png", "");

                    File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + name + ".png"));
                    ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initList();
                    selectedName = name;
                }
            }
        } catch (Exception ex) {
            log.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    private static void updateImageDisplay(ImageForm imageForm) {
        if (selectedImage == null) {
            return;
        }
        imageForm.getImagePreview().setSourceImage(toBufferedImage(selectedImage), displayZoomFactor);
        imageForm.getShowImagePanel().revalidate();
        imageForm.getShowImagePanel().repaint();
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage bufferedImage) {
            return bufferedImage;
        }
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width < 1 || height < 1) {
            MediaTracker tracker = new MediaTracker(new JLabel());
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0, 5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("图片加载被中断", e);
            }
            width = image.getWidth(null);
            height = image.getHeight(null);
        }
        if (width < 1 || height < 1) {
            throw new IllegalStateException("无法获取图片尺寸");
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bufferedImage;
    }
}
