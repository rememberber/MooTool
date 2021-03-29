package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.bulenkov.iconloader.util.ImageUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.ImageForm;
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
        imageForm.getImagePanel().registerKeyboardAction(e -> getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
//        MainWindow.getInstance().getTabbedPane().registerKeyboardAction(e -> {
//            int index = MainWindow.getInstance().getTabbedPane().getSelectedIndex();
//            if (index == 11) {
//                imageForm.getImageFromClipboard();
//            }
//        }, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

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
        imageForm.getImagePanel().registerKeyboardAction(e -> quickSave(), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 点击左侧表格事件
        imageForm.getListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
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
                }
            }
        });

        // 复制到剪贴板
        imageForm.getCopyToClipboardButton().addActionListener(e -> copyToClipboard());
        imageForm.getImagePanel().registerKeyboardAction(e -> copyToClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 新建
        imageForm.getNewButton().addActionListener(e -> newImage());
        imageForm.getImagePanel().registerKeyboardAction(e -> newImage(), KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

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

                    for (int i = selectedRows.length; i > 0; i--) {
                        int selectedRow = imageForm.getListTable().getSelectedRow();
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        FileUtil.del(IMAGE_PATH_PRE_FIX + fileName);
                        tableModel.removeRow(selectedRow);
                        imageForm.getListTable().updateUI();
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
            } else {
                JOptionPane.showMessageDialog(App.mainFrame, "还没有复制图片到剪贴板吧？\n\n", "失败", JOptionPane.WARNING_MESSAGE);
            }
        } catch (HeadlessException ex) {
            ex.printStackTrace();
            log.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    private static void copyToClipboard() {
        try {
            ClipboardUtil.setImage(selectedImage);
            JOptionPane.showMessageDialog(App.mainFrame, "已复制图片到剪贴板！");
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
                    ImageIO.write(ImageUtil.toBufferedImage(selectedImage), "png", imageFile);
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
                    ImageIO.write(ImageUtil.toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initListTable();
                    selectedName = name;
                }
            }
        } catch (Exception ex) {
            log.error(ExceptionUtils.getStackTrace(ex));
        }
    }
}
