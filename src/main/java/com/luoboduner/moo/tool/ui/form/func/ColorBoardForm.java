package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.util.ColorUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

/**
 * <pre>
 * ColorBoardForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/13.
 */
@Getter
public class ColorBoardForm {
    private JPanel colorBoardPanel;
    private JButton pickerButton;
    private JComboBox codeTypeComboBox;
    private JTextField colorCodeTextField;
    private JButton copyButton;
    private JPanel showColorPanel;
    private JButton 收藏Button;
    private JButton 收藏夹Button;
    private JPanel standardColorPanel;
    private JPanel themeColorPanel;
    private JComboBox comboBox2;
    private JPanel themeColorMainPanel;
    private JPanel themeColorSubPanel;

    private static ColorBoardForm colorBoardForm;

    private static final Log logger = LogFactory.get();

    private ColorBoardForm() {
        UndoUtil.register(this);

        pickerButton.addActionListener(e -> {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            if (!gd.isWindowTranslucencySupported(TRANSLUCENT)) {
                JOptionPane.showMessageDialog(colorBoardForm.getColorBoardPanel(), "当前系统环境不支持！", "系统环境", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            App.mainFrame.setVisible(false);
            ColorPickerFrame.showPicker();
        });
        copyButton.addActionListener(e -> {
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
        codeTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String codeType = e.getItem().toString();
                String code = colorBoardForm.getColorCodeTextField().getText();
                if (code.contains(",")) {
                    code = ColorUtil.rgbStrToHex(code);
                }
                if ("HTML".equals(codeType)) {
                    code = code.toUpperCase();
                } else if ("html".equals(codeType)) {
                    code = code.toLowerCase();
                } else if ("RGB".equals(codeType)) {
                    code = ColorUtil.toRgbStr(ColorUtil.fromHex(code));
                }
                colorBoardForm.getColorCodeTextField().setText(code);
            }
        });
    }

    public static ColorBoardForm getInstance() {
        if (colorBoardForm == null) {
            colorBoardForm = new ColorBoardForm();
        }
        return colorBoardForm;
    }

    public static void init() {
        colorBoardForm = getInstance();
        colorBoardForm.getShowColorPanel().setBackground(new Color(166, 166, 166));

        Dimension dimensionColorBlock = new Dimension(50, 50);
        Dimension dimensionSubPanel = new Dimension(50, 250);

        for (String colorHex : ColorConsts.STANDARD) {
            JPanel panel = new JPanel();
            panel.setBackground(ColorUtil.fromHex(colorHex));
            panel.setSize(dimensionColorBlock);
            panel.setPreferredSize(dimensionColorBlock);
            addColorSelectionListener(panel);
            colorBoardForm.getStandardColorPanel().add(panel);
        }

        for (String colorHex : ColorConsts.DEFAULT_MAIN) {
            JPanel panel = new JPanel();
            panel.setBackground(ColorUtil.fromHex(colorHex));
            panel.setSize(dimensionColorBlock);
            panel.setPreferredSize(dimensionColorBlock);
            addColorSelectionListener(panel);
            colorBoardForm.getThemeColorMainPanel().add(panel);
        }

        for (String[] colors : ColorConsts.DEFAULT_SUB) {
            JPanel subPanel = new JPanel();
            subPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            subPanel.setSize(dimensionSubPanel);
            subPanel.setPreferredSize(dimensionSubPanel);
            for (String colorHex : colors) {
                JPanel panel = new JPanel();
                panel.setBackground(ColorUtil.fromHex(colorHex));
                panel.setSize(dimensionColorBlock);
                panel.setPreferredSize(dimensionColorBlock);
                addColorSelectionListener(panel);
                subPanel.add(panel);
            }
            colorBoardForm.getThemeColorSubPanel().add(subPanel);
        }
    }

    private static void addColorSelectionListener(JPanel panel) {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPanel clickedPanel = (JPanel) e.getComponent();
                Color background = clickedPanel.getBackground();
                setSelectedColor(background);
                super.mouseClicked(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JPanel enteredPanel = (JPanel) e.getComponent();
                enteredPanel.setBorder(BorderFactory.createEtchedBorder());
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JPanel enteredPanel = (JPanel) e.getComponent();
                enteredPanel.setBorder(BorderFactory.createEmptyBorder());
                super.mouseExited(e);
            }
        });
    }

    public static void setSelectedColor(Color color) {
        colorBoardForm.getShowColorPanel().setBackground(color);
        colorBoardForm.getColorCodeTextField().setText(ColorUtil.toHex(color));
        colorBoardForm.getColorCodeTextField().grabFocus();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        colorBoardPanel = new JPanel();
        colorBoardPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 10, 10, 10), -1, -1));
        colorBoardPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        themeColorPanel = new JPanel();
        themeColorPanel.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        panel2.add(themeColorPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        themeColorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "主题颜色", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, themeColorPanel.getFont())));
        comboBox2 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("默认");
        defaultComboBoxModel1.addElement("主题1");
        defaultComboBoxModel1.addElement("主题2");
        comboBox2.setModel(defaultComboBoxModel1);
        themeColorPanel.add(comboBox2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        themeColorMainPanel = new JPanel();
        themeColorMainPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        themeColorPanel.add(themeColorMainPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        themeColorSubPanel = new JPanel();
        themeColorSubPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        themeColorPanel.add(themeColorSubPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        standardColorPanel = new JPanel();
        standardColorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(standardColorPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        standardColorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "标准颜色", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, standardColorPanel.getFont())));
        showColorPanel = new JPanel();
        showColorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 10, 0, 0), -1, -1));
        panel1.add(showColorPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        showColorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 7, new Insets(10, 10, 10, 10), -1, -1));
        colorBoardPanel.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pickerButton = new JButton();
        pickerButton.setIcon(new ImageIcon(getClass().getResource("/icon/color_picker.png")));
        pickerButton.setText("取色器");
        panel3.add(pickerButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        codeTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("HTML");
        defaultComboBoxModel2.addElement("html");
        defaultComboBoxModel2.addElement("RGB");
        codeTypeComboBox.setModel(defaultComboBoxModel2);
        panel3.add(codeTypeComboBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        colorCodeTextField = new JTextField();
        panel3.add(colorCodeTextField, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(180, -1), null, 0, false));
        copyButton = new JButton();
        copyButton.setIcon(new ImageIcon(getClass().getResource("/icon/copy.png")));
        copyButton.setText("复制");
        panel3.add(copyButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        收藏Button = new JButton();
        收藏Button.setIcon(new ImageIcon(getClass().getResource("/icon/star-empty.png")));
        收藏Button.setText("收藏");
        panel3.add(收藏Button, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        收藏夹Button = new JButton();
        收藏夹Button.setIcon(new ImageIcon(getClass().getResource("/icon/favorite.png")));
        收藏夹Button.setText("收藏夹");
        panel3.add(收藏夹Button, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        colorBoardPanel.add(separator1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return colorBoardPanel;
    }

}
