package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.thread.ThreadUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TFavoriteColorItemMapper;
import com.luoboduner.moo.tool.dao.TFavoriteColorListMapper;
import com.luoboduner.moo.tool.domain.TFavoriteColorItem;
import com.luoboduner.moo.tool.domain.TFavoriteColorList;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.TableInCellColorBlockRenderer;
import com.luoboduner.moo.tool.ui.frame.FavoriteColorFrame;
import com.luoboduner.moo.tool.ui.frame.FindResultFrame;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * <pre>
 * 调色板-收藏夹
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/21.
 */
@Getter
public class FavoriteColorForm {
    private JPanel favoriteColorPanel;
    private JTable listTable;
    private JButton button1;
    private JTable itemTable;
    private JButton button2;
    private JSplitPane splitPane;
    private JButton button3;
    private JButton button4;

    public static FavoriteColorForm favoriteColorForm;

    private static TFavoriteColorListMapper favoriteColorListMapper = MybatisUtil.getSqlSession().getMapper(TFavoriteColorListMapper.class);
    private static TFavoriteColorItemMapper favoriteColorItemMapper = MybatisUtil.getSqlSession().getMapper(TFavoriteColorItemMapper.class);

    private FavoriteColorForm() {
        UndoUtil.register(this);
        this.getFavoriteColorPanel().registerKeyboardAction(e -> FindResultFrame.getInstance().dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 点击左侧表格事件
        listTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    int selectedRow = favoriteColorForm.getListTable().getSelectedRow();
                    int listId = Integer.parseInt(favoriteColorForm.getListTable().getValueAt(selectedRow, 0).toString());
                    initItemTable(listId);
                });
                super.mousePressed(e);
            }
        });

        this.getFavoriteColorPanel().registerKeyboardAction(e -> FavoriteColorFrame.exit(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void init() {
        favoriteColorForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        favoriteColorForm.getListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        favoriteColorForm.getItemTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        initListTable();
        favoriteColorForm.getFavoriteColorPanel().updateUI();
    }

    public static FavoriteColorForm getInstance() {
        if (favoriteColorForm == null) {
            favoriteColorForm = new FavoriteColorForm();
            UndoUtil.register(favoriteColorForm);
        }
        return favoriteColorForm;
    }

    public static void initListTable() {
        String[] headerNames = {"id", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        favoriteColorForm.getListTable().setModel(model);
        // 隐藏表头
        JTableUtil.hideTableHeader(favoriteColorForm.getListTable());
        // 隐藏id列
        JTableUtil.hideColumn(favoriteColorForm.getListTable(), 0);

        Object[] data;

        List<TFavoriteColorList> favoriteColorLists = favoriteColorListMapper.selectAll();
        for (TFavoriteColorList tFavoriteColorList : favoriteColorLists) {
            data = new Object[2];
            data[0] = tFavoriteColorList.getId();
            data[1] = tFavoriteColorList.getTitle();
            model.addRow(data);
        }
        if (favoriteColorLists.size() > 0) {
//            quickNoteForm.getTextArea().setText(favoriteColorLists.get(0).getContent());
//            quickNoteForm.getNoteListTable().setRowSelectionInterval(0, 0);
//            QuickNoteListener.selectedName = favoriteColorLists.get(0).getName();
        }
    }

    public static void initItemTable(int listId) {
        String[] headerNames = {"id", "显示", "色值", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        favoriteColorForm.getItemTable().setModel(model);
        favoriteColorForm.getItemTable().getColumn("显示").setCellRenderer(new TableInCellColorBlockRenderer());
        favoriteColorForm.getItemTable().getColumn("色值").setPreferredWidth(100);
        favoriteColorForm.getItemTable().getColumn("色值").setMaxWidth(100);
        // 隐藏表头
        JTableUtil.hideTableHeader(favoriteColorForm.getItemTable());
        // 隐藏id列
        JTableUtil.hideColumn(favoriteColorForm.getItemTable(), 0);

        Object[] data;

        List<TFavoriteColorItem> favoriteColorItems = favoriteColorItemMapper.selectByListId(listId);
        for (TFavoriteColorItem favoriteColorItem : favoriteColorItems) {
            data = new Object[4];
            data[0] = favoriteColorItem.getId();
            data[1] = favoriteColorItem.getValue();
            data[2] = favoriteColorItem.getValue();
            data[3] = favoriteColorItem.getName();
            model.addRow(data);
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
        favoriteColorPanel = new JPanel();
        favoriteColorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(240);
        splitPane.setDividerSize(2);
        favoriteColorPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button1 = new JButton();
        button1.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        button1.setText("");
        panel2.add(button1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        listTable = new JTable();
        scrollPane1.setViewportView(listTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel3);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        itemTable = new JTable();
        scrollPane2.setViewportView(itemTable);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button2 = new JButton();
        button2.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        button2.setText("");
        panel4.add(button2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        button3 = new JButton();
        button3.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        button3.setText("");
        panel4.add(button3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button4 = new JButton();
        button4.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        button4.setText("");
        panel4.add(button4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return favoriteColorPanel;
    }

}
