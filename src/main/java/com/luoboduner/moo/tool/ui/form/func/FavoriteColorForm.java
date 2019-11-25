package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
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
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
    private JButton newListButton;
    private JPanel listControlPanel;
    private JPanel itemControlPanel;
    private JButton listItemButton;

    public static FavoriteColorForm favoriteColorForm;

    private static final Log logger = LogFactory.get();

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

        // 左侧列表鼠标点击事件
        listTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                favoriteColorForm.getListControlPanel().setVisible(true);
                favoriteColorForm.getItemControlPanel().setVisible(false);
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

        // 右侧列表鼠标点击事件
        itemTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                favoriteColorForm.getListControlPanel().setVisible(false);
                favoriteColorForm.getItemControlPanel().setVisible(true);
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

        listItemButton.addActionListener(e -> {
            int currentDividerLocation = favoriteColorForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                favoriteColorForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
            } else {
                favoriteColorForm.getSplitPane().setDividerLocation(0);
            }
        });
        newListButton.addActionListener(e -> {
            String title = JOptionPane.showInputDialog("收藏夹名称", "");
            if (StringUtils.isNotBlank(title)) {
                try {
                    TFavoriteColorList tFavoriteColorList = new TFavoriteColorList();
                    tFavoriteColorList.setTitle(title);
                    String now = SqliteUtil.nowDateForSqlite();
                    tFavoriteColorList.setCreateTime(now);
                    tFavoriteColorList.setModifiedTime(now);
                    favoriteColorListMapper.insert(tFavoriteColorList);
                    initListTable();
                } catch (Exception ex) {
                    if (ex.getMessage().contains("constraint")) {
                        JOptionPane.showMessageDialog(favoriteColorForm.getFavoriteColorPanel(), "存在相同的名称，请重新命名！", "失败", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(favoriteColorForm.getFavoriteColorPanel(), "异常：" + ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                    }
                    logger.error(ExceptionUtils.getStackTrace(ex));
                }
            }
        });
    }

    public void init() {
        favoriteColorForm = getInstance();
        favoriteColorForm.getListControlPanel().setVisible(false);
        favoriteColorForm.getItemControlPanel().setVisible(false);
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
            favoriteColorForm.getListTable().setRowSelectionInterval(0, 0);
            initItemTable(favoriteColorLists.get(0).getId());
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
        splitPane.setDividerLocation(204);
        splitPane.setDividerSize(2);
        favoriteColorPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        listControlPanel = new JPanel();
        listControlPanel.setLayout(new GridLayoutManager(1, 3, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(listControlPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        listControlPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        button1 = new JButton();
        button1.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        button1.setText("");
        listControlPanel.add(button1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        newListButton = new JButton();
        newListButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        newListButton.setText("");
        listControlPanel.add(newListButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        listTable = new JTable();
        scrollPane1.setViewportView(listTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel2);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel2.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        itemTable = new JTable();
        scrollPane2.setViewportView(itemTable);
        itemControlPanel = new JPanel();
        itemControlPanel.setLayout(new GridLayoutManager(1, 5, new Insets(5, 5, 5, 5), -1, -1));
        panel2.add(itemControlPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button2 = new JButton();
        button2.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        button2.setText("");
        itemControlPanel.add(button2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        itemControlPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        button3 = new JButton();
        button3.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        button3.setText("");
        itemControlPanel.add(button3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button4 = new JButton();
        button4.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        button4.setText("");
        itemControlPanel.add(button4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        listItemButton = new JButton();
        listItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/listFiles_dark.png")));
        listItemButton.setText("");
        itemControlPanel.add(listItemButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return favoriteColorPanel;
    }

}
