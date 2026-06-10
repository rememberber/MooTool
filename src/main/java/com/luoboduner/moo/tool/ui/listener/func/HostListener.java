package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.THostMapper;
import com.luoboduner.moo.tool.domain.THost;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.dialog.CurrentHostDialog;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.HostForm;
import com.luoboduner.moo.tool.util.HostFileUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Date;

/**
 * <pre>
 * Host事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/7.
 */
@Slf4j
public class HostListener {

    private static THostMapper hostMapper = MybatisUtil.getSqlSession().getMapper(THostMapper.class);

    public static String selectedNameHost;

    public static boolean ignoreQuickSave;

    /** 忽略 JOptionPane 关闭后回传到列表的 Enter 键，避免重命名弹框重复弹出 */
    private static boolean suppressListEnterRename;

    public static void addListeners() {
        HostForm hostForm = HostForm.getInstance();

        hostForm.getSaveButton().addActionListener(e -> save(true));

        hostForm.getSwitchButton().addActionListener(e -> {
            String hostText = hostForm.getTextArea().getText();
            String hostName = selectedNameHost;
            ThreadUtil.execute(() -> {
                HostForm.setHost(hostName, hostText);
                persistHostContent(hostName, hostText);
            });
        });

        // 点击左侧列表事件
        hostForm.getNoteList().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ignoreQuickSave = true;
                try {
                    int index = hostForm.getNoteList().locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }
                    refreshHostContentInTextArea(index);
                } catch (Exception e1) {
                    log.error(e1.getMessage());
                } finally {
                    ignoreQuickSave = false;
                }

                super.mousePressed(e);
            }
        });

        // 文本域按键事件
        hostForm.getTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_S) {
                    quickSave(true);
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_N) {
                    newHost();
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_F) {
                    showFindPanel();
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_R) {
                    showFindPanel();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        hostForm.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                quickSave(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                quickSave(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                quickSave(true);
            }
        });

        // 删除按钮事件
        hostForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(hostForm);
        });

        // 添加按钮事件
        hostForm.getAddButton().addActionListener(e -> {
            newHost();
        });

        // 查看系统当前host按钮事件
        hostForm.getCurrentHostButton().addActionListener(e -> {

            String content = HostFileUtil.readSystemHosts();
            CurrentHostDialog currentHostDialog = new CurrentHostDialog();
            currentHostDialog.setPlaneText(content);
            currentHostDialog.setVisible(true);

        });

        // 左侧列表按键事件（重命名）
        hostForm.getNoteList().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (suppressListEnterRename) {
                        suppressListEnterRename = false;
                        return;
                    }
                    renameSelectedHost(hostForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(hostForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    ignoreQuickSave = true;
                    try {
                        int selectedIndex = hostForm.getNoteList().getSelectedIndex();
                        if (selectedIndex < 0) {
                            return;
                        }
                        refreshHostContentInTextArea(selectedIndex);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    } finally {
                        ignoreQuickSave = false;
                    }
                }
            }
        });

        hostForm.getFindButton().addActionListener(e -> {
            showFindPanel();
        });

        hostForm.getExportButton().addActionListener(e -> {
            int[] selectedIndices = hostForm.getNoteList().getSelectedIndices();

            try {
                if (selectedIndices.length > 0) {
                    SystemFileChooser fileChooser = new SystemFileChooser(App.config.getHostExportPath());
                    fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(hostForm.getHostPanel());
                    String exportPath;
                    if (approve == SystemFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setHostExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    DefaultListModel<THost> listModel = (DefaultListModel<THost>) hostForm.getNoteList().getModel();
                    for (int index : selectedIndices) {
                        THost tHost = hostMapper.selectByPrimaryKey(listModel.getElementAt(index).getId());
                        File exportFile = FileUtil.touch(exportPath + File.separator + tHost.getName() + ".txt");
                        FileUtil.writeUtf8String(tHost.getContent(), exportFile);
                    }
                    JOptionPane.showMessageDialog(hostForm.getHostPanel(), "导出成功！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    JOptionPane.showMessageDialog(hostForm.getHostPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(hostForm.getHostPanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        // 搜索框变更事件
        hostForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                HostForm.initList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                HostForm.initList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                HostForm.initList();
            }
        });

        // 左侧列表增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem("重命名");
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem exportMenuItem = new JMenuItem("导出");
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        noteListPopupMenu.add(exportMenuItem);
        hostForm.getNoteList().setComponentPopupMenu(noteListPopupMenu);

        renameMenuItem.addActionListener(e -> renameSelectedHost(hostForm));

        deleteMenuItem.addActionListener(e -> {
            deleteFiles(hostForm);
        });

        exportMenuItem.addActionListener(e -> {
            int[] selectedIndices = hostForm.getNoteList().getSelectedIndices();

            try {
                if (selectedIndices.length > 0) {
                    SystemFileChooser fileChooser = new SystemFileChooser(App.config.getHostExportPath());
                    fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(hostForm.getHostPanel());
                    String exportPath;
                    if (approve == SystemFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setHostExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    DefaultListModel<THost> listModel = (DefaultListModel<THost>) hostForm.getNoteList().getModel();
                    for (int index : selectedIndices) {
                        THost tHost = hostMapper.selectByPrimaryKey(listModel.getElementAt(index).getId());
                        File exportFile = FileUtil.touch(exportPath + File.separator + tHost.getName() + ".txt");
                        FileUtil.writeUtf8String(tHost.getContent(), exportFile);
                    }
                    JOptionPane.showMessageDialog(hostForm.getHostPanel(), "导出成功！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    JOptionPane.showMessageDialog(hostForm.getHostPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(hostForm.getHostPanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

    }

    private static void renameSelectedHost(HostForm hostForm) {
        int selectedIndex = hostForm.getNoteList().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        DefaultListModel<THost> model = (DefaultListModel<THost>) hostForm.getNoteList().getModel();
        THost item = model.getElementAt(selectedIndex);
        String beforeName = item.getName();
        if (StringUtils.isBlank(beforeName)) {
            return;
        }
        suppressListEnterRename = true;
        String afterName = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            THost tHost = new THost();
            tHost.setId(item.getId());
            tHost.setName(afterName);
            tHost.setModifiedTime(SqliteUtil.nowDateForSqlite());
            hostMapper.updateByPrimaryKeySelective(tHost);
            selectedNameHost = afterName;
            HostForm.initList();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，和已有文件重名");
            HostForm.initList();
            log.error(e.toString());
        }
    }

    private static void deleteFiles(HostForm hostForm) {
        try {
            int[] selectedIndices = hostForm.getNoteList().getSelectedIndices();

            if (selectedIndices.length == 0) {
                JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultListModel<THost> listModel = (DefaultListModel<THost>) hostForm.getNoteList().getModel();

                    for (int selectedIndex : selectedIndices) {
                        Integer id = listModel.getElementAt(selectedIndex).getId();
                        hostMapper.deleteByPrimaryKey(id);
                    }
                    selectedNameHost = null;
                    HostForm.initList();
                }
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(e1.toString());
        }
    }

    /**
     * save for quick key and item change
     *
     * @param refreshModifiedTime
     */
    private static void quickSave(boolean refreshModifiedTime) {
        HostForm hostForm = HostForm.getInstance();
        String now = SqliteUtil.nowDateForSqlite();
        if (selectedNameHost != null) {
            THost tHost = new THost();
            tHost.setName(selectedNameHost);
            tHost.setContent(hostForm.getTextArea().getText());
            if (refreshModifiedTime) {
                tHost.setModifiedTime(now);
            }

            hostMapper.updateByName(tHost);
        } else {
            if (refreshModifiedTime) {
                // 通过refreshModifiedTime可以判断是否主动按快捷键保存，只有主动触发时才保存，避免初次点击列表误提示问题
                String tempName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", tempName);
                if (StringUtils.isNotBlank(name)) {
                    THost tHost = new THost();
                    tHost.setName(name);
                    tHost.setContent(hostForm.getTextArea().getText());
                    tHost.setCreateTime(now);
                    tHost.setModifiedTime(now);

                    hostMapper.insert(tHost);
                    HostForm.initList();
                    selectedNameHost = name;
                }
            }
        }
    }

    private static void newHost() {
        String name = getDefaultFileName();
        name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", name);
        if (StringUtils.isNotBlank(name)) {
            THost tHost = hostMapper.selectByName(name);
            if (tHost == null) {
                tHost = new THost();
            } else {
                JOptionPane.showMessageDialog(App.mainFrame, "存在同名文件，请重新命名！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String now = SqliteUtil.nowDateForSqlite();
            tHost.setName(name);
            tHost.setCreateTime(now);
            tHost.setModifiedTime(now);
            hostMapper.insert(tHost);
            HostForm.initList();
        }
    }

    public static void refreshHostContentInTextArea(int index) {
        HostForm hostForm = HostForm.getInstance();

        hostForm.getTextArea().setEditable(true);
        hostForm.getSwitchButton().setVisible(true);
        if (index >= 0) {
            DefaultListModel<THost> listModel = (DefaultListModel<THost>) hostForm.getNoteList().getModel();
            String name = listModel.getElementAt(index).getName();
            selectedNameHost = name;
            THost tHost = hostMapper.selectByName(name);

            hostForm.getTextArea().setText(tHost.getContent());
        }
        hostForm.getTextArea().setCaretPosition(0);
        hostForm.getScrollPane().getVerticalScrollBar().setValue(0);
        hostForm.getScrollPane().getHorizontalScrollBar().setValue(0);
    }

    private static void save(boolean needRename) {
        if (StringUtils.isEmpty(selectedNameHost)) {
            selectedNameHost = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
        }
        String name = selectedNameHost;
        if (needRename) {
            name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", selectedNameHost);
        }
        if (StringUtils.isNotBlank(name)) {
            String content = HostForm.getInstance().getTextArea().getText();
            persistHostContent(name, content);
            selectedNameHost = name;
        }
    }

    private static void persistHostContent(String name, String content) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        THost tHost = hostMapper.selectByName(name);
        if (tHost == null) {
            tHost = new THost();
        }
        String now = SqliteUtil.nowDateForSqlite();
        tHost.setName(name);
        tHost.setContent(content);
        tHost.setCreateTime(now);
        tHost.setModifiedTime(now);
        if (tHost.getId() == null) {
            hostMapper.insert(tHost);
            SwingUtilities.invokeLater(HostForm::initList);
        } else {
            hostMapper.updateByPrimaryKey(tHost);
        }
    }

    public static void showFindPanel() {
        HostForm hostForm = HostForm.getInstance();
        hostForm.getFindReplacePanel().removeAll();
        hostForm.getFindReplacePanel().setDoubleBuffered(true);
        FindReplaceBar findReplaceBar = new FindReplaceBar(hostForm.getTextArea());
        hostForm.getFindReplacePanel().add(findReplaceBar.getFindOptionPanel());
        hostForm.getFindReplacePanel().setVisible(true);
        hostForm.getFindReplacePanel().updateUI();
        findReplaceBar.getFindField().setText(hostForm.getTextArea().getSelectedText());
        findReplaceBar.getFindField().grabFocus();
        findReplaceBar.getFindField().selectAll();
    }

    /**
     * Default File Name
     *
     * @return
     */
    private static String getDefaultFileName() {
        return "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
    }
}
