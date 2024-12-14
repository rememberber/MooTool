package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.Base64Dialog;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.ImageForm;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.util.Date;

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

    public static final String IMAGE_PATH_PRE_FIX = SystemUtil.CONFIG_HOME + File.separator + "images" + File.separator;

    public static final Image DEFAULT_IMAGE = Toolkit.getDefaultToolkit().getImage(ImageListener.class.getResource("/icon/image_128.png"));

    public static void addListeners() {
        ImageForm imageForm = ImageForm.getInstance();

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

        // 保存按钮事件
        imageForm.getSaveButton().addActionListener(e -> {
            saveImage();
        });
        if (SystemUtil.isMacOs()) {
            imageForm.getImagePanel().registerKeyboardAction(e -> quickSave(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            imageForm.getImagePanel().registerKeyboardAction(e -> quickSave(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        // 点击左侧表格事件
        imageForm.getListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                quickSave();
                try {
                    imageForm.getShowImageLabel().setIcon(new ImageIcon(DEFAULT_IMAGE));
                    imageForm.getShowImagePanel().updateUI();

                    int focusedRowIndex = imageForm.getListTable().rowAtPoint(e.getPoint());
                    if (focusedRowIndex == -1) {
                        return;
                    }
                    String name = imageForm.getListTable().getValueAt(focusedRowIndex, 1).toString();
                    selectedName = name.replace(".png", "");
                    imageForm.getShowImageLabel().setIcon(new ImageIcon(ImageListener.IMAGE_PATH_PRE_FIX + name));
                    imageForm.getShowImagePanel().updateUI();

                    ImageListener.selectedImage = ImageIO.read(FileUtil.newFile(ImageListener.IMAGE_PATH_PRE_FIX + name));

                    String pixel = ImageListener.selectedImage.getWidth(null) + " x " + ImageListener.selectedImage.getHeight(null);
                    String size = FileUtil.readableFileSize(FileUtil.file(IMAGE_PATH_PRE_FIX + name).length());
                    imageForm.getImageInfoLabel().setText("尺寸：" + pixel + "  大小：" + size + " ");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
                super.mousePressed(e);
            }
        });

        // 左侧列表鼠标点击事件（显示下方删除按钮）
        imageForm.getListTable().addMouseListener(new MouseListener() {
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
                    try {
                        Desktop.getDesktop().open(FileUtil.file(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
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
            try {
                Desktop.getDesktop().open(FileUtil.file(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
            } catch (IOException ex) {
                log.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 删除按钮事件
        imageForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(imageForm);
        });

        // 左侧列表按键事件（重命名）
        imageForm.getListTable().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = imageForm.getListTable().getSelectedRow();
                    String originalName = String.valueOf(imageForm.getListTable().getValueAt(selectedRow, 0));
                    String newName = String.valueOf(imageForm.getListTable().getValueAt(selectedRow, 1)).replace(".png", "");

                    if (StringUtils.isNotBlank(newName)) {
                        try {
                            FileUtil.rename(FileUtil.file(IMAGE_PATH_PRE_FIX + originalName), newName, true, true);
                            imageForm.getListTable().setValueAt(newName + ".png", selectedRow, 0);
                            imageForm.getListTable().setValueAt(newName + ".png", selectedRow, 1);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败" + e.getMessage());
                            ImageForm.initListTable();
                            log.error(ExceptionUtils.getStackTrace(e));
                        }
                    }
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(imageForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    quickSave();
                    try {
                        imageForm.getShowImageLabel().setIcon(new ImageIcon(DEFAULT_IMAGE));
                        imageForm.getShowImagePanel().updateUI();

                        int selectedRow = imageForm.getListTable().getSelectedRow();
                        String name = imageForm.getListTable().getValueAt(selectedRow, 1).toString();
                        selectedName = name.replace(".png", "");
                        imageForm.getShowImageLabel().setIcon(new ImageIcon(ImageListener.IMAGE_PATH_PRE_FIX + name));
                        imageForm.getShowImagePanel().updateUI();

                        ImageListener.selectedImage = ImageIO.read(FileUtil.newFile(ImageListener.IMAGE_PATH_PRE_FIX + name));

                        String pixel = ImageListener.selectedImage.getWidth(null) + " x " + ImageListener.selectedImage.getHeight(null);
                        String size = FileUtil.readableFileSize(FileUtil.file(IMAGE_PATH_PRE_FIX + name).length());
                        imageForm.getImageInfoLabel().setText("尺寸：" + pixel + "  大小：" + size + " ");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(App.mainFrame, ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                        log.error(ExceptionUtils.getStackTrace(ex));
                    }
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
            int[] selectedRows = imageForm.getListTable().getSelectedRows();

            try {
                if (selectedRows.length > 0) {
                    JFileChooser fileChooser = new JFileChooser(App.config.getImageExportPath());
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(imageForm.getImagePanel());
                    String exportPath;
                    if (approve == JFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setImageExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    for (int row : selectedRows) {
                        String fileName = (String) imageForm.getListTable().getValueAt(row, 0);
                        File exportFile = FileUtil.touch(exportPath + File.separator + fileName);
                        FileUtil.copy(FileUtil.file(IMAGE_PATH_PRE_FIX + fileName), exportFile, true);
                    }
                    JOptionPane.showMessageDialog(imageForm.getImagePanel(), "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    JOptionPane.showMessageDialog(imageForm.getImagePanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(imageForm.getImagePanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        // 导出为Base64
        imageForm.getToBase64Button().addActionListener(e -> {
            Base64Dialog dialog = new Base64Dialog();

            dialog.setToTextArea(Base64.encode(FileUtil.file(IMAGE_PATH_PRE_FIX + selectedName + ".png")));

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
                        imageForm.getShowImageLabel().setIcon(imageIcon);

                        selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                        File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
                        ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                        ImageForm.initListTable();
                    } else {
                        JOptionPane.showMessageDialog(App.mainFrame, "可能不是正确的图片Base64？\n\n", "失败", JOptionPane.WARNING_MESSAGE);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "从Base64获取异常\n\n" + e1.getMessage(), "失败", JOptionPane.WARNING_MESSAGE);
                log.error("从Base64获取异常,{}", ExceptionUtils.getStackTrace(e1));
            }

        });

        imageForm.getZoomInButton().addActionListener(e -> {
            int width = imageForm.getShowImageLabel().getWidth();
            int height = imageForm.getShowImageLabel().getHeight();
            ImageIcon imageIcon = new ImageIcon(selectedImage.getScaledInstance((int) (width * 1.1), (int) (height * 1.1), Image.SCALE_DEFAULT));
            imageForm.getShowImageLabel().setIcon(imageIcon);
        });

        imageForm.getZoomOutButton().addActionListener(e -> {
            int width = imageForm.getShowImageLabel().getWidth();
            int height = imageForm.getShowImageLabel().getHeight();
            ImageIcon imageIcon = new ImageIcon(selectedImage.getScaledInstance((int) (width * 0.9), (int) (height * 0.9), Image.SCALE_DEFAULT));
            imageForm.getShowImageLabel().setIcon(imageIcon);
        });

        imageForm.getOriginalSizeButton().addActionListener(e -> {
            ImageIcon imageIcon = new ImageIcon(selectedImage);
            imageForm.getShowImageLabel().setIcon(imageIcon);
        });

        imageForm.getFitSizeButton().addActionListener(e -> {
            int width = imageForm.getImageControlPanel().getWidth();
//            int height = imageForm.getShowImagePanel().getHeight();
//          只控制宽度，高度自适应
            ImageIcon imageIcon = new ImageIcon(selectedImage.getScaledInstance(width, -1, Image.SCALE_DEFAULT));
            imageForm.getShowImageLabel().setIcon(imageIcon);
        });

        // 左侧表格增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem("重命名");
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem exportMenuItem = new JMenuItem("导出");
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        noteListPopupMenu.add(exportMenuItem);
        imageForm.getListTable().setComponentPopupMenu(noteListPopupMenu);

        renameMenuItem.addActionListener(e -> {
            int selectedRow = imageForm.getListTable().getSelectedRow();
            String beforeName = String.valueOf(imageForm.getListTable().getValueAt(selectedRow, 0));
            if (StringUtils.isNotBlank(beforeName)) {
                String afterName = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", beforeName);
                if (StringUtils.isNotBlank(afterName)) {
                    try {
                        FileUtil.rename(FileUtil.file(IMAGE_PATH_PRE_FIX + beforeName), afterName.replace(".png", ""), true, true);
                        imageForm.getListTable().setValueAt(afterName, selectedRow, 0);
                        imageForm.getListTable().setValueAt(afterName, selectedRow, 1);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(App.mainFrame, "重命名失败" + e1.getMessage());
                        ImageForm.initListTable();
                        log.error(ExceptionUtils.getStackTrace(e1));
                    }
                }
            }
        });

        deleteMenuItem.addActionListener(e -> {
            deleteFiles(imageForm);
        });

        exportMenuItem.addActionListener(e -> {
            int[] selectedRows = imageForm.getListTable().getSelectedRows();

            try {
                if (selectedRows.length > 0) {
                    JFileChooser fileChooser = new JFileChooser(App.config.getImageExportPath());
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(imageForm.getImagePanel());
                    String exportPath;
                    if (approve == JFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setImageExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    for (int row : selectedRows) {
                        String fileName = (String) imageForm.getListTable().getValueAt(row, 0);
                        File exportFile = FileUtil.touch(exportPath + File.separator + fileName);
                        FileUtil.copy(FileUtil.file(IMAGE_PATH_PRE_FIX + fileName), exportFile, true);
                    }
                    JOptionPane.showMessageDialog(imageForm.getImagePanel(), "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    JOptionPane.showMessageDialog(imageForm.getImagePanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(imageForm.getImagePanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

    }

    private static void deleteFiles(ImageForm imageForm) {
        try {
            int[] selectedRows = imageForm.getListTable().getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultTableModel tableModel = (DefaultTableModel) imageForm.getListTable().getModel();

                    for (int i = 0; i < selectedRows.length; i++) {
                        int selectedRow = selectedRows[i];
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        FileUtil.del(IMAGE_PATH_PRE_FIX + fileName);
                    }
                    selectedName = null;
                    ImageForm.initListTable();
                }
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    private static void newImage() {
        ImageForm imageForm = ImageForm.getInstance();
        imageForm.getShowImageLabel().setIcon(new ImageIcon(DEFAULT_IMAGE));
        selectedName = null;
        selectedImage = null;

        Image image = ClipboardUtil.getImage();
        ImageListener.selectedImage = image;
        if (image != null) {
            imageForm.getShowImageLabel().setIcon(new ImageIcon(image));
        }
    }

    public static void getImageFromClipboard() {
        try {
            ImageForm imageForm = ImageForm.getInstance();
            Image image = ClipboardUtil.getImage();
            ImageListener.selectedImage = image;
            if (image != null) {
                selectedName = null;
                imageForm.getShowImageLabel().setIcon(new ImageIcon(image));

                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + selectedName + ".png"));
                ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                ImageForm.initListTable();
            } else {
                JOptionPane.showMessageDialog(App.mainFrame, "还没有复制图片到剪贴板吧？\n\n", "失败", JOptionPane.WARNING_MESSAGE);
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
        try {
            ClipboardUtil.setImage(selectedImage);
            ImageForm imageForm = ImageForm.getInstance();
            AlertUtil.buttonInfo(imageForm.getCopyToClipboardButton(), "复制到剪贴板", "已复制", 2000);

        } catch (Exception e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "复制失败！\n\n" + e1.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    /**
     * save for manual
     */
    private static void saveImage() {
        if (StringUtils.isEmpty(selectedName)) {
            selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
        }
        String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", selectedName);
        if (StringUtils.isNotBlank(name)) {
            try {
                if (selectedImage != null) {
                    File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + name + ".png"));
                    ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initListTable();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "保存失败！\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
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
                String tempName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", tempName);
                if (StringUtils.isNotBlank(name)) {
                    name = name.replace(".png", "");

                    File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + name + ".png"));
                    ImageIO.write(toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initListTable();
                    selectedName = name;
                }
            }
        } catch (Exception ex) {
            log.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        } else {
            BufferedImage bufferedImage = new BufferedImage(image.getWidth((ImageObserver) null), image.getHeight((ImageObserver) null), 2);
            Graphics2D g = bufferedImage.createGraphics();
            g.drawImage(image, 0, 0, (ImageObserver) null);
            g.dispose();
            return bufferedImage;
        }
    }
}
