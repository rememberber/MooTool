package com.luoboduner.moo.tool.ui.listener;


import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * <pre>
 * 窗体事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/21.
 */
public class FrameListener {

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    public static void addListeners() {
        App.mainFrame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveBeforeExit();
                if (SystemUtil.isWindowsOs()) {
                    App.mainFrame.setVisible(false);
                } else if (SystemUtil.isMacOs()) {
                    // 最小化窗口
                    App.mainFrame.setExtendedState(Frame.ICONIFIED);
                } else {
                    App.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {
                if (App.config.isDefaultMaxWindow()) {
                    // 低分辨率下自动最大化窗口
                    App.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        // 鼠标双击最大化/还原
        App.mainFrame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    if (App.mainFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                        App.mainFrame.setExtendedState(JFrame.NORMAL);
                    } else {
                        App.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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

        if (SystemUtil.isMacOs()) {
            MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> App.mainFrame.setExtendedState(Frame.NORMAL), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> App.mainFrame.setExtendedState(Frame.ICONIFIED), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        // Command + W 最小化窗口
        MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> App.mainFrame.setExtendedState(Frame.ICONIFIED), KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

    }

    public static void saveBeforeExit() {
        App.config.setRecentTabIndex(MainWindow.getInstance().getTabbedPane().getSelectedIndex());
        App.config.save();
    }
}
