package com.luoboduner.moo.tool.ui.listener.func;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.ImageForm;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * <pre>
 * ImageListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/12/16.
 */
public class ImageListener {
    public static String selectedName;

    public static void addListeners() {
        ImageForm imageForm = ImageForm.getInstance();

        imageForm.getSaveFromClipboardButton().addActionListener(e -> {
            imageForm.getImageFromClipboard();
        });

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
        });
    }
}
