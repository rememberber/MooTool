package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemInfo;
import com.google.common.collect.Lists;
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
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.frame.FavoriteColorFrame;
import com.luoboduner.moo.tool.ui.frame.FindResultFrame;
import com.luoboduner.moo.tool.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
@Slf4j
public class FavoriteColorForm {
    private JPanel favoriteColorPanel;
    private JTable listTable;
    private JButton deleteListButton;
    private JTable itemTable;
    private JButton deleteItemButton;
    private JSplitPane splitPane;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton newListButton;
    private JPanel listControlPanel;
    private JPanel itemControlPanel;
    private JButton listItemButton;

    public static FavoriteColorForm favoriteColorForm;

    private static final Log logger = LogFactory.get();

    private static TFavoriteColorListMapper favoriteColorListMapper = MybatisUtil.getSqlSession().getMapper(TFavoriteColorListMapper.class);
    private static TFavoriteColorItemMapper favoriteColorItemMapper = MybatisUtil.getSqlSession().getMapper(TFavoriteColorItemMapper.class);

    private static Integer lastSelectedListId;

    private FavoriteColorForm() {
        UndoUtil.register(this);
        this.getFavoriteColorPanel().registerKeyboardAction(e -> FindResultFrame.getInstance().dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 点击左侧表格事件
        listTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int focusedRowIndex = listTable.rowAtPoint(e.getPoint());
                if (focusedRowIndex == -1) {
                    return;
                }
                viewListBySelected(focusedRowIndex);
                listControlPanel.setVisible(true);
                itemControlPanel.setVisible(false);
                super.mousePressed(e);
            }
        });

        this.getFavoriteColorPanel().registerKeyboardAction(e -> FavoriteColorFrame.exit(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 右侧列表鼠标点击事件
        itemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                listControlPanel.setVisible(false);
                itemControlPanel.setVisible(true);
            }
        });

        listItemButton.addActionListener(e -> {
            int currentDividerLocation = splitPane.getDividerLocation();
            if (currentDividerLocation < 5) {
                splitPane.setDividerLocation((int) (App.mainFrame.getWidth() / 5));
            } else {
                splitPane.setDividerLocation(0);
            }
        });
        newListButton.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "收藏夹名称", "");
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
                        JOptionPane.showMessageDialog(favoriteColorPanel, "存在相同的名称，请重新命名！", "失败", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(favoriteColorPanel, "异常：" + ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                    }
                    logger.error(ExceptionUtils.getStackTrace(ex));
                }
            }
        });

        // 列表删除按钮事件
        deleteListButton.addActionListener(e -> {
            try {
                deleteList();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(favoriteColorPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        // Item删除按钮事件
        deleteItemButton.addActionListener(e -> {
            deleteItem();
        });
        moveUpButton.addActionListener(e -> {
            try {
                int[] selectedRows = itemTable.getSelectedRows();

                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(favoriteColorPanel, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else if (selectedRows[0] == 0) {
                    JOptionPane.showMessageDialog(favoriteColorPanel, "已到顶部！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    ListSelectionModel listSelectionModel = new DefaultListSelectionModel();
                    DefaultTableModel tableModel = (DefaultTableModel) itemTable.getModel();

                    List<int[]> selectedRowArrays = Lists.newArrayList();
                    int startIndex = 0;
                    for (int i = 0; i < selectedRows.length; i++) {
                        int currentRow = selectedRows[i];
                        if (i + 1 < selectedRows.length) {
                            int nextRow = selectedRows[i + 1];
                            if (nextRow - currentRow > 1) {
                                selectedRowArrays.add(ArrayUtil.sub(selectedRows, startIndex, i + 1));
                                startIndex = i + 1;
                            }
                        } else {
                            selectedRowArrays.add(ArrayUtil.sub(selectedRows, startIndex, selectedRows.length));
                        }
                    }

                    for (int[] selectedRowArray : selectedRowArrays) {
                        int preRow = selectedRowArray[0] - 1;
                        Integer preId = (Integer) tableModel.getValueAt(preRow, 0);
                        Long preSortNum = (Long) tableModel.getValueAt(preRow, 4);

                        TFavoriteColorItem tFavoriteColorItem;
                        for (int selectedRow : selectedRowArray) {
                            listSelectionModel.addSelectionInterval(selectedRow - 1, selectedRow - 1);

                            Long currentSortNum = (Long) tableModel.getValueAt(selectedRow, 4);
                            Integer currentId = (Integer) tableModel.getValueAt(selectedRow, 0);
                            tFavoriteColorItem = new TFavoriteColorItem();
                            tFavoriteColorItem.setId(currentId);
                            tFavoriteColorItem.setSortNum(preSortNum);
                            favoriteColorItemMapper.updateByPrimaryKeySelective(tFavoriteColorItem);
                            preSortNum = currentSortNum;
                        }
                        tFavoriteColorItem = new TFavoriteColorItem();
                        tFavoriteColorItem.setId(preId);
                        tFavoriteColorItem.setSortNum(preSortNum);
                        favoriteColorItemMapper.updateByPrimaryKeySelective(tFavoriteColorItem);
                    }

                    initItemTable(null);
                    itemTable.setSelectionModel(listSelectionModel);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(favoriteColorPanel, "操作失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(e1));
            }
        });
        moveDownButton.addActionListener(e -> {
            try {
                int[] selectedRows = itemTable.getSelectedRows();

                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(favoriteColorPanel, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else if (selectedRows[selectedRows.length - 1] == itemTable.getRowCount() - 1) {
                    JOptionPane.showMessageDialog(favoriteColorPanel, "已到底部！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    ListSelectionModel listSelectionModel = new DefaultListSelectionModel();
                    DefaultTableModel tableModel = (DefaultTableModel) itemTable.getModel();

                    List<int[]> selectedRowArrays = Lists.newArrayList();
                    int startIndex = 0;
                    for (int i = 0; i < selectedRows.length; i++) {
                        int currentRow = selectedRows[i];
                        if (i + 1 < selectedRows.length) {
                            int nextRow = selectedRows[i + 1];
                            if (nextRow - currentRow > 1) {
                                selectedRowArrays.add(ArrayUtil.sub(selectedRows, startIndex, i + 1));
                                startIndex = i + 1;
                            }
                        } else {
                            selectedRowArrays.add(ArrayUtil.sub(selectedRows, startIndex, selectedRows.length));
                        }
                    }

                    for (int[] selectedRowArray : selectedRowArrays) {
                        int firstRow = selectedRowArray[0];
                        Long firstSortNum = (Long) tableModel.getValueAt(firstRow, 4);

                        TFavoriteColorItem tFavoriteColorItem;
                        for (int selectedRow : selectedRowArray) {
                            listSelectionModel.addSelectionInterval(selectedRow + 1, selectedRow + 1);

                            Integer currentId = (Integer) tableModel.getValueAt(selectedRow, 0);
                            int nextRow = selectedRow + 1;
                            Long nextSortNum = (Long) tableModel.getValueAt(nextRow, 4);
                            tFavoriteColorItem = new TFavoriteColorItem();
                            tFavoriteColorItem.setId(currentId);
                            tFavoriteColorItem.setSortNum(nextSortNum);
                            favoriteColorItemMapper.updateByPrimaryKeySelective(tFavoriteColorItem);
                        }
                        tFavoriteColorItem = new TFavoriteColorItem();
                        tFavoriteColorItem.setId((Integer) tableModel.getValueAt(selectedRowArray[selectedRowArray.length - 1] + 1, 0));
                        tFavoriteColorItem.setSortNum(firstSortNum);
                        favoriteColorItemMapper.updateByPrimaryKeySelective(tFavoriteColorItem);
                    }

                    initItemTable(null);
                    itemTable.setSelectionModel(listSelectionModel);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(favoriteColorPanel, "操作失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        // 左侧列表按键事件（重命名）
        listTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = listTable.getSelectedRow();
                    int id = Integer.parseInt(String.valueOf(listTable.getValueAt(selectedRow, 0)));
                    String title = String.valueOf(listTable.getValueAt(selectedRow, 1));
                    if (StringUtils.isNotBlank(title)) {
                        TFavoriteColorList tFavoriteColorList = new TFavoriteColorList();
                        tFavoriteColorList.setId(id);
                        tFavoriteColorList.setTitle(title);
                        try {
                            favoriteColorListMapper.updateByPrimaryKeySelective(tFavoriteColorList);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，和已有文件重名");
                            JsonBeautyForm.initListTable();
                            log.error(e.toString());
                        }
                    }
                    viewListBySelected(selectedRow);
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteList();
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    int selectedRow = listTable.getSelectedRow();
                    viewListBySelected(selectedRow);
                }
            }
        });

        // 右侧项目列表按键事件（重命名）
        itemTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = itemTable.getSelectedRow();
                    int id = Integer.parseInt(String.valueOf(itemTable.getValueAt(selectedRow, 0)));
                    String value = String.valueOf(itemTable.getValueAt(selectedRow, 2));
                    String title = String.valueOf(itemTable.getValueAt(selectedRow, 3));
                    if (StringUtils.isNotBlank(title)) {
                        TFavoriteColorItem tFavoriteColorItem = new TFavoriteColorItem();
                        tFavoriteColorItem.setId(id);
                        tFavoriteColorItem.setName(title);
                        tFavoriteColorItem.setValue(value);
                        try {
                            favoriteColorItemMapper.updateByPrimaryKeySelective(tFavoriteColorItem);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，和已有文件重名");
                            viewListBySelected(selectedRow);
                            log.error(e.toString());
                        }
                    }
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteItem();
                }
            }
        });
    }

    private void deleteItem() {
        try {
            int[] selectedRows = itemTable.getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(favoriteColorPanel, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(favoriteColorPanel, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultTableModel tableModel = (DefaultTableModel) itemTable.getModel();

                    for (int i = selectedRows.length; i > 0; i--) {
                        int selectedRow = itemTable.getSelectedRow();
                        Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                        favoriteColorItemMapper.deleteByPrimaryKey(id);
                        tableModel.removeRow(selectedRow);
                    }
                }
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(favoriteColorPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    private void viewListBySelected(int focusedRowIndex) {
        int listId = Integer.parseInt(listTable.getValueAt(focusedRowIndex, 0).toString());
        initItemTable(listId);
    }

    private void deleteList() {
        int[] selectedRows = listTable.getSelectedRows();

        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(favoriteColorPanel, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            int isDelete = JOptionPane.showConfirmDialog(favoriteColorPanel, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
            if (isDelete == JOptionPane.YES_OPTION) {
                DefaultTableModel tableModel = (DefaultTableModel) listTable.getModel();

                for (int i = 0; i < selectedRows.length; i++) {
                    int selectedRow = selectedRows[i];
                    Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                    favoriteColorListMapper.deleteByPrimaryKey(id);
                }
                initListTable();
            }
        }
    }

    public void init() {
        favoriteColorForm = getInstance();
        favoriteColorForm.getListControlPanel().setVisible(false);
        favoriteColorForm.getItemControlPanel().setVisible(false);
        favoriteColorForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        favoriteColorForm.getListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        favoriteColorForm.getItemTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);

        favoriteColorForm.getListItemButton().setIcon(new FlatSVGIcon("icon/list.svg"));
        favoriteColorForm.getDeleteItemButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        favoriteColorForm.getDeleteListButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        favoriteColorForm.getNewListButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        favoriteColorForm.getMoveUpButton().setIcon(new FlatSVGIcon("icon/up.svg"));
        favoriteColorForm.getMoveDownButton().setIcon(new FlatSVGIcon("icon/down.svg"));

        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            this.getSplitPane().getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            this.getSplitPane().getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            this.getSplitPane().getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            this.getSplitPane().getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            GridLayoutManager gridLayoutManager = (GridLayoutManager) favoriteColorForm.getFavoriteColorPanel().getLayout();
            gridLayoutManager.setMargin(new Insets(28, 0, 0, 0));
        }

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

    public static void initItemTable(Integer listId) {
        if (listId == null) {
            listId = lastSelectedListId;
        } else {
            lastSelectedListId = listId;
        }
        String[] headerNames = {"id", "显示", "色值", "名称", "排序号"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        favoriteColorForm.getItemTable().setModel(model);
        favoriteColorForm.getItemTable().getColumn("显示").setCellRenderer(new TableInCellColorBlockRenderer());
        favoriteColorForm.getItemTable().getColumn("色值").setPreferredWidth(100);
        favoriteColorForm.getItemTable().getColumn("色值").setMaxWidth(100);
        // 隐藏表头
        JTableUtil.hideTableHeader(favoriteColorForm.getItemTable());
        // 隐藏id列和排序列
        JTableUtil.hideColumn(favoriteColorForm.getItemTable(), 0);
        JTableUtil.hideColumn(favoriteColorForm.getItemTable(), 4);

        Object[] data;

        List<TFavoriteColorItem> favoriteColorItems = favoriteColorItemMapper.selectByListId(listId);
        for (TFavoriteColorItem favoriteColorItem : favoriteColorItems) {
            data = new Object[5];
            data[0] = favoriteColorItem.getId();
            data[1] = favoriteColorItem.getValue();
            data[2] = favoriteColorItem.getValue();
            data[3] = favoriteColorItem.getName();
            data[4] = favoriteColorItem.getSortNum();
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
        favoriteColorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(12, 12, 12, 12), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(204);
        splitPane.setDividerSize(10);
        favoriteColorPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        listControlPanel = new JPanel();
        listControlPanel.setLayout(new GridLayoutManager(1, 3, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(listControlPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        listControlPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        deleteListButton = new JButton();
        deleteListButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteListButton.setText("");
        listControlPanel.add(deleteListButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        deleteItemButton = new JButton();
        deleteItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteItemButton.setText("");
        itemControlPanel.add(deleteItemButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        itemControlPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        moveUpButton = new JButton();
        moveUpButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        moveUpButton.setText("");
        itemControlPanel.add(moveUpButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        moveDownButton = new JButton();
        moveDownButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        moveDownButton.setText("");
        itemControlPanel.add(moveDownButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
