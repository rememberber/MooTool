package com.luoboduner.moo.tool.ui.dialog;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TFavoriteRegexItemMapper;
import com.luoboduner.moo.tool.dao.TFavoriteRegexListMapper;
import com.luoboduner.moo.tool.domain.TFavoriteRegexItem;
import com.luoboduner.moo.tool.domain.TFavoriteRegexList;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.FavoriteRegexForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.List;

/**
 * 正则表达式收藏Dialog
 */
@Slf4j
public class FavoriteRegexDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox favoriteBookListComboBox;
    private JButton newFavoriteBookListButton;
    private JTextField nameTextField;
    private JLabel regexValueLabel;

    private static TFavoriteRegexListMapper favoriteRegexListMapper = MybatisUtil.getSqlSession().getMapper(TFavoriteRegexListMapper.class);
    private static TFavoriteRegexItemMapper favoriteRegexItemMapper = MybatisUtil.getSqlSession().getMapper(TFavoriteRegexItemMapper.class);

    public FavoriteRegexDialog() {
        super(App.mainFrame, "正则表达式收藏");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 600, 320);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        newFavoriteBookListButton.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "收藏夹名称", "");
            if (StringUtils.isNotBlank(title)) {
                try {
                    TFavoriteRegexList tFavoriteRegexList = new TFavoriteRegexList();
                    tFavoriteRegexList.setTitle(title);
                    String now = SqliteUtil.nowDateForSqlite();
                    tFavoriteRegexList.setCreateTime(now);
                    tFavoriteRegexList.setModifiedTime(now);
                    favoriteRegexListMapper.insert(tFavoriteRegexList);
                    fillFavoriteListComboBox();
                } catch (Exception ex) {
                    if (ex.getMessage().contains("constraint")) {
                        JOptionPane.showMessageDialog(this, "存在相同的名称，请重新命名！", "失败", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "异常：" + ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                    }
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
            }
        });
    }

    private void onOK() {
        try {
            if (favoriteBookListComboBox.getSelectedItem() == null) {
                favoriteBookListComboBox.grabFocus();
                return;
            }
            TFavoriteRegexList tFavoriteRegexList = favoriteRegexListMapper.selectByTitle((String) favoriteBookListComboBox.getSelectedItem());
            TFavoriteRegexItem tFavoriteRegexItem = new TFavoriteRegexItem();
            tFavoriteRegexItem.setListId(tFavoriteRegexList.getId());
            tFavoriteRegexItem.setName(nameTextField.getText());
            tFavoriteRegexItem.setValue(regexValueLabel.getText());
            tFavoriteRegexItem.setSortNum((int) (System.currentTimeMillis() / 100000));
            String now = SqliteUtil.nowDateForSqlite();
            tFavoriteRegexItem.setCreateTime(now);
            tFavoriteRegexItem.setModifiedTime(now);
            favoriteRegexItemMapper.insert(tFavoriteRegexItem);
            dispose();
            FavoriteRegexForm.getInstance().init();
        } catch (Exception e) {
            if (e.getMessage().contains("constraint")) {
                JOptionPane.showMessageDialog(this, "存在相同的名称，请重新命名！", "失败", JOptionPane.WARNING_MESSAGE);
                nameTextField.grabFocus();
                nameTextField.selectAll();
            } else {
                JOptionPane.showMessageDialog(this, "异常：" + e.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
            }
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private void onCancel() {
        dispose();
    }

    public void init(String regex) {
        regexValueLabel.setText(regex);
        nameTextField.setText("未命名正则表达式-" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss"));
        nameTextField.grabFocus();
        nameTextField.selectAll();
        fillFavoriteListComboBox();
    }

    private void fillFavoriteListComboBox() {
        List<TFavoriteRegexList> favoriteRegexListList = favoriteRegexListMapper.selectAll();
        favoriteBookListComboBox.removeAllItems();
        for (TFavoriteRegexList item : favoriteRegexListList) {
            favoriteBookListComboBox.addItem(item.getTitle());
        }
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("确定");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        nameTextField = new JTextField();
        panel3.add(nameTextField, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 25, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        regexValueLabel = new JLabel();
        regexValueLabel.setText("");
        panel4.add(regexValueLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel4.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(separator1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        favoriteBookListComboBox = new JComboBox();
        panel5.add(favoriteBookListComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        newFavoriteBookListButton = new JButton();
        newFavoriteBookListButton.setText("新建收藏夹");
        panel5.add(newFavoriteBookListButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
