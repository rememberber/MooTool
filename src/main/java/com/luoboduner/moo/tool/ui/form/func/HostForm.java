package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.io.FileUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.THostMapper;
import com.luoboduner.moo.tool.domain.THost;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.HostSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.listener.func.HostListener;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

/**
 * <pre>
 * Host
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/7.
 */
@Getter
@Slf4j
public class HostForm {
    private JPanel hostPanel;
    private JTable noteListTable;
    private JButton deleteButton;
    private JButton saveButton;
    private JSplitPane splitPane;
    private JButton addButton;
    private JPanel deletePanel;
    private JButton switchButton;
    private JPanel rightPanel;
    private JPanel controlPanel;
    private JButton exportButton;
    private JButton currentHostButton;
    private JButton findButton;
    private JPanel findReplacePanel;
    private JPanel findOptionPanel;
    private JTextField findTextField;
    private JTextField replaceTextField;
    private JButton doFindButton;
    private JButton doReplaceButton;
    private JLabel findReplaceCloseLabel;
    private JCheckBox findMatchCaseCheckBox;
    private JCheckBox findWordsCheckBox;
    private JCheckBox findUseRegexCheckBox;
    private JPanel findMenuSeparatorPanel;

    private static HostForm hostForm;
    private static THostMapper hostMapper = MybatisUtil.getSqlSession().getMapper(THostMapper.class);

    public static final String WIN_HOST_FILE_PATH = "C:\\Windows\\System32\\drivers\\etc\\hosts";

    public static final String MAC_HOST_FILE_PATH = "/etc/hosts";

    public static final String LINUX_HOST_FILE_PATH = "/etc/hosts";

    public static final String MAC_HOST_DIR_PATH = "/etc/";

    public static final String LINUX_HOST_DIR_PATH = "/etc/";

    public static final String NOT_SUPPORTED_TIPS = "暂不支持该操作系统！";

    private HostSyntaxTextViewer textArea;

    private RTextScrollPane scrollPane;

    private HostForm() {
        textArea = new HostSyntaxTextViewer();
        scrollPane = new RTextScrollPane(textArea);

        scrollPane.setMaximumSize(new Dimension(-1, -1));
        scrollPane.setMinimumSize(new Dimension(-1, -1));

        Color defaultBackground = App.mainFrame.getBackground();
        Color defaultForeground = findTextField.getForeground();

        Gutter gutter = scrollPane.getGutter();
        gutter.setBorderColor(defaultBackground);
        Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        gutter.setLineNumberFont(font);
//            gutter.setLineNumberColor(defaultBackground);
        gutter.setFoldBackground(defaultBackground);
        gutter.setArmedFoldBackground(defaultBackground);

        UndoUtil.register(this);
    }

    public static void setHost(String hostName, String hostText) {
        try {
            HostForm hostForm = HostForm.getInstance();
            hostForm.getSwitchButton().setEnabled(false);
            if (SystemUtil.isWindowsOs()) {
                File hostFile = FileUtil.file(WIN_HOST_FILE_PATH);
                FileUtil.writeUtf8String(hostText, hostFile);
                if (App.trayIcon != null) {
                    App.trayIcon.displayMessage("MooTool", "Host已切换！\n" + hostName, TrayIcon.MessageType.INFO);
                    highlightHostMenu(hostName);
                }
            } else if (SystemUtil.isMacOs()) {
                try {
                    File hostFile = FileUtil.file(MAC_HOST_FILE_PATH);
                    FileUtil.writeUtf8String(hostText, hostFile);
                    if (App.trayIcon != null) {
                        App.trayIcon.displayMessage("MooTool", "Host已切换！\n" + hostName, TrayIcon.MessageType.INFO);
                        highlightHostMenu(hostName);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(hostForm.getHostPanel(), "需要管理员或root权限运行！\n现在帮你打开hosts文件所在目录，请自行修改替换" + e.getMessage(), "切换失败！", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(MAC_HOST_DIR_PATH));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                }
            } else if (SystemUtil.isLinuxOs()) {
                try {
                    File hostFile = FileUtil.file(LINUX_HOST_FILE_PATH);
                    FileUtil.writeUtf8String(hostText, hostFile);
                    if (App.trayIcon != null) {
                        App.trayIcon.displayMessage("MooTool", "Host已切换！\n" + hostName, TrayIcon.MessageType.INFO);
                        highlightHostMenu(hostName);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(hostForm.getHostPanel(), "需要管理员或root权限运行！\n现在帮你打开hosts文件所在目录，请自行修改替换" + e.getMessage(), "切换失败！", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(LINUX_HOST_DIR_PATH));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                }
            } else {
                JOptionPane.showMessageDialog(hostForm.getHostPanel(), NOT_SUPPORTED_TIPS, "抱歉！", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(hostForm.getHostPanel(), ex.getMessage(), "切换失败！", JOptionPane.ERROR_MESSAGE);
        } finally {
            hostForm.getSwitchButton().setEnabled(true);
        }
    }

    public static HostForm getInstance() {
        if (hostForm == null) {
            hostForm = new HostForm();
        }
        return hostForm;
    }

    public static void init() {
        hostForm = getInstance();
        Init.initTray();

        initUi();
        initListTable();
        highlightHostMenu(App.config.getCurrentHostName());

        HostListener.addListeners();
    }

    private static void initUi() {

        hostForm.getRightPanel().removeAll();
        hostForm.getRightPanel().setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        if ("上方".equals(App.config.getMenuBarPosition())) {
            hostForm.getRightPanel().add(hostForm.getControlPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            hostForm.getRightPanel().add(hostForm.getFindReplacePanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            hostForm.getRightPanel().add(hostForm.getScrollPane(), new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        } else if ("下方".equals(App.config.getMenuBarPosition())) {
            hostForm.getRightPanel().add(hostForm.getScrollPane(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
            hostForm.getRightPanel().add(hostForm.getFindReplacePanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            hostForm.getRightPanel().add(hostForm.getControlPanel(), new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        }

        hostForm.getFindReplacePanel().removeAll();
        if ("上方".equals(App.config.getMenuBarPosition())) {
            hostForm.getFindReplacePanel().add(hostForm.getFindMenuSeparatorPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            hostForm.getFindReplacePanel().add(hostForm.getFindOptionPanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        } else if ("下方".equals(App.config.getMenuBarPosition())) {
            hostForm.getFindReplacePanel().add(hostForm.getFindOptionPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            hostForm.getFindReplacePanel().add(hostForm.getFindMenuSeparatorPanel(), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        }

        hostForm.getAddButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        hostForm.getFindButton().setIcon(new FlatSVGIcon("icon/find.svg"));
        hostForm.getSaveButton().setIcon(new FlatSVGIcon("icon/save.svg"));
        hostForm.getCurrentHostButton().setIcon(new FlatSVGIcon("icon/host.svg"));
        hostForm.getSwitchButton().setIcon(new FlatSVGIcon("icon/check.svg"));
        hostForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        hostForm.getExportButton().setIcon(new FlatSVGIcon("icon/export.svg"));

        hostForm.getFindReplacePanel().setVisible(false);

        hostForm.getSplitPane().setDividerLocation(App.mainFrame.getWidth() / 5);
        hostForm.getNoteListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);

        hostForm.getDeletePanel().setVisible(false);
        hostForm.getTextArea().grabFocus();

        hostForm.getHostPanel().updateUI();
    }

    public static void initListTable() {
        String[] headerNames = {"id", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        hostForm.getNoteListTable().setModel(model);
        // 隐藏表头
        JTableUtil.hideTableHeader(hostForm.getNoteListTable());
        // 隐藏id列
        JTableUtil.hideColumn(hostForm.getNoteListTable(), 0);

        List<THost> hostList = hostMapper.selectAll();

        if (!SystemUtil.isLinuxOs()) {
            App.popupMenu.removeAll();
            MenuItem openItem = new MenuItem("MooTool");
            MenuItem colorPickerItem = new MenuItem("取色器");
            MenuItem exitItem = new MenuItem("Quit");

            openItem.addActionListener(e -> {
                Init.showMainFrame();
            });
            colorPickerItem.addActionListener(e -> {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                if (gd.isWindowTranslucencySupported(TRANSLUCENT)) {
                    App.mainFrame.setVisible(false);
                    ColorPickerFrame.showPicker();
                }
            });
            exitItem.addActionListener(e -> {
                Init.shutdown();
            });

            App.popupMenu.add(openItem);
            App.popupMenu.addSeparator();
            App.popupMenu.add(colorPickerItem);
            App.popupMenu.addSeparator();

            MenuItem menuItem;
            Object[] data;
            for (THost tHost : hostList) {
                data = new Object[2];
                data[0] = tHost.getId();
                data[1] = tHost.getName();
                model.addRow(data);
                menuItem = new MenuItem(tHost.getName());
                menuItem.addActionListener(e -> {
                    THost tHost1 = hostMapper.selectByName(tHost.getName());
                    String hostName = tHost1.getName();
                    setHost(hostName, tHost1.getContent());
                });
                App.popupMenu.add(menuItem);
            }
            App.popupMenu.addSeparator();
            App.popupMenu.add(exitItem);
        }

        if (hostList.size() > 0) {
            hostForm.getTextArea().setText(hostList.get(0).getContent());
            hostForm.getNoteListTable().setRowSelectionInterval(0, 0);
            HostListener.selectedNameHost = hostList.get(0).getName();
        }

        if (hostForm.getNoteListTable().getRowCount() > 0) {
            hostForm.getNoteListTable().setRowSelectionInterval(0, 0);
        }
    }

    private static void highlightHostMenu(String hostName) {
        if (!SystemUtil.isLinuxOs()) {
            Font fontBold = new Font(App.config.getFont(), Font.BOLD, App.config.getFontSize());
            Font fontPlain = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
            for (int i = 2; i < App.popupMenu.getItemCount(); i++) {
                MenuItem item = App.popupMenu.getItem(i);
                if (hostName.equals(item.getLabel())) {
                    item.setFont(fontBold);
                } else {
                    item.setFont(fontPlain);
                }
            }
            App.config.setCurrentHostName(hostName);
            App.config.save();
            HostListener.refreshHostContentInTextArea();
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
        hostPanel = new JPanel();
        hostPanel.setLayout(new GridLayoutManager(1, 1, new Insets(12, 12, 12, 12), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(10);
        hostPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setMinimumSize(new Dimension(0, 64));
        splitPane.setLeftComponent(panel1);
        deletePanel = new JPanel();
        deletePanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(deletePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        deletePanel.add(deleteButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        deletePanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        exportButton = new JButton();
        exportButton.setIcon(new ImageIcon(getClass().getResource("/icon/export_dark.png")));
        exportButton.setText("");
        exportButton.setToolTipText("导出");
        deletePanel.add(exportButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        noteListTable = new JTable();
        scrollPane1.setViewportView(noteListTable);
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(rightPanel);
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        rightPanel.add(controlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setIcon(new ImageIcon(getClass().getResource("/icon/menu-saveall_dark.png")));
        saveButton.setText("");
        saveButton.setToolTipText("保存(Ctrl+S)");
        controlPanel.add(saveButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        controlPanel.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        addButton.setText("");
        addButton.setToolTipText("新建(Ctrl+N)");
        controlPanel.add(addButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        switchButton = new JButton();
        switchButton.setIcon(new ImageIcon(getClass().getResource("/icon/check.png")));
        switchButton.setText("");
        switchButton.setToolTipText("切换host");
        controlPanel.add(switchButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentHostButton = new JButton();
        currentHostButton.setIcon(new ImageIcon(getClass().getResource("/icon/host.png")));
        currentHostButton.setText("");
        currentHostButton.setToolTipText("查看系统当前host");
        controlPanel.add(currentHostButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findButton = new JButton();
        findButton.setIcon(new ImageIcon(getClass().getResource("/icon/find_dark.png")));
        findButton.setText("");
        findButton.setToolTipText("新建(Ctrl+N)");
        controlPanel.add(findButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findReplacePanel = new JPanel();
        findReplacePanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        findReplacePanel.setVisible(true);
        rightPanel.add(findReplacePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        findOptionPanel = new JPanel();
        findOptionPanel.setLayout(new GridLayoutManager(2, 6, new Insets(0, 0, 0, 0), -1, -1));
        findReplacePanel.add(findOptionPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        findTextField = new JTextField();
        findOptionPanel.add(findTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        replaceTextField = new JTextField();
        findOptionPanel.add(replaceTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer3 = new Spacer();
        findOptionPanel.add(spacer3, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        findOptionPanel.add(spacer4, new GridConstraints(1, 3, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        doFindButton = new JButton();
        doFindButton.setText("查找");
        findOptionPanel.add(doFindButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        doReplaceButton = new JButton();
        doReplaceButton.setText("替换");
        findOptionPanel.add(doReplaceButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findReplaceCloseLabel = new JLabel();
        findReplaceCloseLabel.setIcon(new ImageIcon(getClass().getResource("/icon/remove_dark.png")));
        findReplaceCloseLabel.setText("");
        findOptionPanel.add(findReplaceCloseLabel, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findMatchCaseCheckBox = new JCheckBox();
        findMatchCaseCheckBox.setText("区分大小写");
        findOptionPanel.add(findMatchCaseCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findWordsCheckBox = new JCheckBox();
        findWordsCheckBox.setText("全词匹配");
        findOptionPanel.add(findWordsCheckBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findUseRegexCheckBox = new JCheckBox();
        findUseRegexCheckBox.setText("使用正则");
        findOptionPanel.add(findUseRegexCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findMenuSeparatorPanel = new JPanel();
        findMenuSeparatorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        findReplacePanel.add(findMenuSeparatorPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        findMenuSeparatorPanel.add(separator1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return hostPanel;
    }

}
