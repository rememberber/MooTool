package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.FavoriteColorDialog;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.frame.FavoriteColorFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

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
    }
}
