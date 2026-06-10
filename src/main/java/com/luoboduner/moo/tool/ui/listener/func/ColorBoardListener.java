package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.FlatColorPipette;
import com.luoboduner.moo.tool.ui.dialog.CommonTipsDialog;
import com.luoboduner.moo.tool.ui.dialog.FavoriteColorDialog;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.frame.FavoriteColorFrame;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.ColorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            GraphicsDevice gd = ge.getDefaultScreenDevice();
//            if (!gd.isWindowTranslucencySupported(TRANSLUCENT)) {
//                JOptionPane.showMessageDialog(colorBoardForm.getColorBoardPanel(), "当前系统环境不支持！", "系统环境", JOptionPane.INFORMATION_MESSAGE);
//                return;
//            }
//            App.mainFrame.setVisible(false);
//            ColorPickerFrame.showPicker();

            // show pipette color picker
            Window window = SwingUtilities.windowForComponent((JComponent) e.getSource());
            try {
                App.mainFrame.setExtendedState(Frame.ICONIFIED);
                FlatColorPipette.pick(window, true,
                        color -> {
//                            if (color != null) {
//                                ColorBoardForm.setSelectedColor(color);
//                            }
                        },
                        color -> {
                            if (color != null) {
                                String before = ColorUtil.toHex(ColorBoardForm.getSelectedColor());
                                ColorBoardForm.setSelectedColor(color);
                                ColorBoardForm.saveColorHistory("取色器", before, ColorUtil.toHex(color));
                            }
                            App.mainFrame.setExtendedState(Frame.NORMAL);
                        });
            } catch (AWTException | UnsupportedOperationException ex) {
                logger.error(ex);
                JOptionPane.showMessageDialog(colorBoardForm.getColorBoardPanel(), "当前系统环境不支持！", "系统环境", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        colorBoardForm.getCopyButton().addActionListener(e -> {
            try {
                ClipboardUtil.setStr(colorBoardForm.getColorCodeTextField().getText());
                AlertUtil.buttonInfo(colorBoardForm.getCopyButton(), "复制", "已复制", 2000);
            } catch (Exception e1) {
                logger.error(e1);
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
            favoriteColorDialog.init(ColorBoardForm.getSelectedColor());
            favoriteColorDialog.setVisible(true);
        });
        colorBoardForm.getColorCodeTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String before = ColorUtil.toHex(ColorBoardForm.getSelectedColor());
                    Color color = ColorUtil.fromHex(colorBoardForm.getColorCodeTextField().getText());
                    ColorBoardForm.setSelectedColor(color);
                    ColorBoardForm.saveColorHistory("输入色值", before, ColorUtil.toHex(color));
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
                tipsBuilder.append("<p>颜色运算：取反对当前颜色取反色；相交/相加/差值/平均对当前颜色与对比色进行运算，结果更新为当前颜色。</p>");
                tipsBuilder.append("<p>按住 Shift 点击色块可将其设为对比色；点击对比色色块可自由选色。</p>");
                tipsBuilder.append("<p>PicPick是一款非常优秀的集取色、截图、标尺、放大镜、图片编辑等于一身的桌面应用，我非常喜欢它，感谢作者的付出！</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(UiConsts.HELP_FOCUSED_ICON);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(UiConsts.HELP_ICON);
                super.mouseExited(e);
            }
        });

        colorBoardForm.getChooseColorButton().addActionListener(e -> {
            Color before = ColorBoardForm.getSelectedColor();
            Color color = JColorChooser.showDialog(colorBoardForm.getColorBoardPanel(), "选择颜色", before);
            if (color != null) {
                ColorBoardForm.setSelectedColor(color);
                ColorBoardForm.saveColorHistory("选择颜色", ColorUtil.toHex(before), ColorUtil.toHex(color));
            }
        });

        colorBoardForm.getSecondaryColorPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color color = JColorChooser.showDialog(colorBoardForm.getColorBoardPanel(), "选择对比色", ColorBoardForm.getSecondaryColor());
                if (color != null) {
                    ColorBoardForm.setSecondaryColor(color);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                colorBoardForm.getSecondaryColorPanel().setBorder(BorderFactory.createEtchedBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                colorBoardForm.getSecondaryColorPanel().setBorder(BorderFactory.createEmptyBorder());
            }
        });

        colorBoardForm.getSwapColorButton().addActionListener(e -> {
            String primary = ColorUtil.toHex(ColorBoardForm.getSelectedColor());
            String secondary = ColorUtil.toHex(ColorBoardForm.getSecondaryColor());
            ColorBoardForm.swapColors();
            ColorBoardForm.saveColorHistory("交换颜色", primary + " / " + secondary,
                    ColorUtil.toHex(ColorBoardForm.getSelectedColor()) + " / " + ColorUtil.toHex(ColorBoardForm.getSecondaryColor()));
        });

        colorBoardForm.getInvertColorButton().addActionListener(e ->
                applyColorOperation("取反", ColorUtil.invert(ColorBoardForm.getSelectedColor())));

        colorBoardForm.getIntersectColorButton().addActionListener(e ->
                applyColorOperation("相交", ColorUtil.intersect(ColorBoardForm.getSelectedColor(), ColorBoardForm.getSecondaryColor())));

        colorBoardForm.getAddColorButton().addActionListener(e ->
                applyColorOperation("相加", ColorUtil.add(ColorBoardForm.getSelectedColor(), ColorBoardForm.getSecondaryColor())));

        colorBoardForm.getDiffColorButton().addActionListener(e ->
                applyColorOperation("差值", ColorUtil.difference(ColorBoardForm.getSelectedColor(), ColorBoardForm.getSecondaryColor())));

        colorBoardForm.getAverageColorButton().addActionListener(e ->
                applyColorOperation("平均", ColorUtil.average(ColorBoardForm.getSelectedColor(), ColorBoardForm.getSecondaryColor())));
    }

    private static void applyColorOperation(String operation, Color resultColor) {
        String before = ColorUtil.toHex(ColorBoardForm.getSelectedColor());
        String secondary = ColorUtil.toHex(ColorBoardForm.getSecondaryColor());
        ColorBoardForm.setSelectedColor(resultColor);
        ColorBoardForm.saveColorHistory(operation, before, ColorUtil.toHex(resultColor) + " (对比色:" + secondary + ")");
    }
}
