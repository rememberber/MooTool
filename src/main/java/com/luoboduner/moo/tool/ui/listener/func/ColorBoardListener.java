package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.dialog.CommonTipsDialog;
import com.luoboduner.moo.tool.ui.dialog.FavoriteColorDialog;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.frame.FavoriteColorFrame;
import com.luoboduner.moo.tool.util.ColorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

/**
 * <pre>
 * ColorBoardListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/25.
 */
public class ColorBoardListener {

    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        ColorBoardForm colorBoardForm = ColorBoardForm.getInstance();
        colorBoardForm.getPickerButton().addActionListener(e -> {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            if (!gd.isWindowTranslucencySupported(TRANSLUCENT)) {
                JOptionPane.showMessageDialog(colorBoardForm.getColorBoardPanel(), "当前系统环境不支持！", "系统环境", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            App.mainFrame.setVisible(false);
            ColorPickerFrame.showPicker();
        });
        colorBoardForm.getCopyButton().addActionListener(e -> {
            try {
                colorBoardForm.getCopyButton().setEnabled(false);
                ClipboardUtil.setStr(colorBoardForm.getColorCodeTextField().getText());
                JOptionPane.showMessageDialog(colorBoardForm.getColorBoardPanel(), "已复制！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                colorBoardForm.getCopyButton().setEnabled(true);
            }
        });
        colorBoardForm.getCodeTypeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String code = colorBoardForm.getColorCodeTextField().getText();
                ColorBoardForm.setColorCode(code);
                App.config.setColorCodeType(e.getItem().toString());
                App.config.save();
            }
        });
        colorBoardForm.getThemeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ColorBoardForm.fillColorBlocks();
                App.config.setColorTheme(e.getItem().toString());
                App.config.save();
            }
        });
        colorBoardForm.getFavoriteBookButton().addActionListener(e -> FavoriteColorFrame.showWindow());
        colorBoardForm.getFavoriteButton().addActionListener(e -> {
            FavoriteColorDialog favoriteColorDialog = new FavoriteColorDialog();
            favoriteColorDialog.pack();
            favoriteColorDialog.init(colorBoardForm.getShowColorPanel().getBackground());
            favoriteColorDialog.setVisible(true);
        });
        colorBoardForm.getColorCodeTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ColorBoardForm.setSelectedColor(ColorUtil.fromHex(colorBoardForm.getColorCodeTextField().getText()));
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        colorBoardForm.getAboutLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>关于调色板</h1>");
                tipsBuilder.append("<p>调色板和取色器的设计借鉴了PicPick，其中的颜色主题更是完全照搬了过来。</p>");
                tipsBuilder.append("<p>PicPick是一款非常优秀的集取色、截图、标尺、放大镜、图片编辑等于一身的桌面应用，我非常喜欢它，感谢作者的付出！</p>");
                tipsBuilder.append("<p><b>注：MooTool的取色器实时预览的色值存在微小误差，请以鼠标点击取色后的色值为准</b></p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }
}
