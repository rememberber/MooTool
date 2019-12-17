package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.bulenkov.iconloader.util.ImageUtil;
import com.luoboduner.moo.tool.App;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

    public static final String IMAGE_PATH_PRE_FIX = SystemUtil.configHome + File.separator + "images" + File.separator;

    public static final Image DEFAULT_IMAGE = Toolkit.getDefaultToolkit().getImage(ImageListener.class.getResource("/icon/image_128.png"));

    public static void addListeners() {
        ImageForm imageForm = ImageForm.getInstance();

        // 从剪贴板获取
        imageForm.getSaveFromClipboardButton().addActionListener(e -> imageForm.getImageFromClipboard());
        imageForm.getImagePanel().registerKeyboardAction(e -> imageForm.getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
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
            if (StringUtils.isEmpty(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog("名称", selectedName);
            if (StringUtils.isNotBlank(name)) {
                try {
                    File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + name + ".png"));
                    ImageIO.write(ImageUtil.toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initListTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, "保存失败！\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
            }
        });

        // 点击左侧表格事件
        imageForm.getListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
//                quickSave(false);
                try {
                    imageForm.getShowImageLabel().setIcon(new ImageIcon(DEFAULT_IMAGE));

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
                }
            }
        });

        // 复制到剪贴板
        imageForm.getCopyToClipboardButton().addActionListener(e -> {
            copyToClipboard();
        });
        imageForm.getImagePanel().registerKeyboardAction(e -> copyToClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 新建
        imageForm.getNewButton().addActionListener(e -> {
            imageForm.getShowImageLabel().setIcon(new ImageIcon(DEFAULT_IMAGE));
            selectedName = null;
            selectedImage = null;
        });

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
}
