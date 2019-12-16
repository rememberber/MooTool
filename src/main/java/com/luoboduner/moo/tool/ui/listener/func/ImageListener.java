package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
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
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    public static void addListeners() {
        ImageForm imageForm = ImageForm.getInstance();

        // 从剪贴板获取
        imageForm.getSaveFromClipboardButton().addActionListener(e -> imageForm.getImageFromClipboard());
        imageForm.getImagePanel().registerKeyboardAction(e -> imageForm.getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        imageForm.getMenuPanel().registerKeyboardAction(e -> imageForm.getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        imageForm.getShowImagePanel().registerKeyboardAction(e -> imageForm.getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        imageForm.getShowImageLabel().registerKeyboardAction(e -> imageForm.getImageFromClipboard(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        MainWindow.getInstance().getTabbedPane().registerKeyboardAction(e -> {
            int index = MainWindow.getInstance().getTabbedPane().getSelectedIndex();
            if (index == 11) {
                imageForm.getImageFromClipboard();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

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
                File imageFile = FileUtil.touch(new File(IMAGE_PATH_PRE_FIX + name + ".png"));
                try {
                    ImageIO.write(ImageUtil.toBufferedImage(selectedImage), "png", imageFile);
                    ImageForm.initListTable();
                } catch (IOException ex) {
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
                int selectedRow = imageForm.getListTable().getSelectedRow();
                String name = imageForm.getListTable().getValueAt(selectedRow, 1).toString();
                selectedName = name.replace(".png", "");
                imageForm.getShowImageLabel().setIcon(new ImageIcon(ImageListener.IMAGE_PATH_PRE_FIX + name));
                imageForm.getShowImagePanel().updateUI();
                try {
                    ImageListener.selectedImage = ImageIO.read(FileUtil.newFile(ImageListener.IMAGE_PATH_PRE_FIX + name));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
                super.mousePressed(e);
            }
        });
    }
}
