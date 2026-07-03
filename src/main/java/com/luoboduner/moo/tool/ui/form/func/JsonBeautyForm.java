package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.luoboduner.moo.tool.ui.component.SearchFieldUiUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.QuickNoteGitStatus;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.component.JsonBeautyTreeCellRenderer;
import com.luoboduner.moo.tool.ui.component.JsonBeautyTreeDragDrop;
import com.luoboduner.moo.tool.ui.component.SplitPaneUtil;
import com.luoboduner.moo.tool.ui.component.ToolbarUiUtil;
import com.luoboduner.moo.tool.ui.component.PanelCloseUtil;
import com.luoboduner.moo.tool.ui.component.textviewer.JsonRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.component.textviewer.JsonRTextScrollPane;
import com.luoboduner.moo.tool.ui.dialog.JsonBeautyGitDialog;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.JsonBeautyAutoGitScheduler;
import com.luoboduner.moo.tool.util.JsonBeautyAutoPullScheduler;
import com.luoboduner.moo.tool.util.JsonBeautyListSortMode;
import com.luoboduner.moo.tool.util.JsonBeautyTreeUtil;
import com.luoboduner.moo.tool.util.JsonBeautyVaultRefreshCoordinator;
import com.luoboduner.moo.tool.util.JsonBeautyVaultUtil;
import com.luoboduner.moo.tool.util.JsonBeautyVaultWatcher;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import com.luoboduner.moo.tool.util.VaultTreeExpandMode;
import com.luoboduner.moo.tool.util.VaultTreeUiUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Json格式化
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/6.
 */
@Getter
@Slf4j
public class JsonBeautyForm {
    private JPanel jsonBeautyPanel;
    private JList<TJsonBeauty> noteList;
    private JTree noteTree;
    private JComboBox<JsonBeautyListSortMode> listSortComboBox;
    private JButton deleteButton;
    private JButton saveButton;
    private JButton newFolderButton;
    private JSplitPane splitPane;
    private JButton addButton;
    private JButton findButton;
    private JButton beautifyButton;
    private JButton listItemButton;
    private JPanel rightPanel;
    private JPanel controlPanel;
    private JButton exportButton;
    private JPanel findReplacePanel;
    private JPanel menuPanel;
    private JTextField searchTextField;
    private JButton compressButton;
    private JButton moreButton;
    private JSplitPane contentSplitPane;
    private JCheckBox ignoreCaseCheckBox;
    private JCheckBox keySortCheckBox;
    private JCheckBox checkDuplicateCheckBox;
    private JButton jsonToXmlButton;
    private JButton xmlToJsonButton;
    private JButton customFormatButton;
    private JTextField jsonPathTextField;
    private JButton getByJsonPathButton;
    private JButton moreCloseButton;
    private JButton getJsonPathButton;
    private JScrollPane moreScrollPane;
    private JButton escapeJsonButton;
    private JButton unescapeButton;
    private JButton escapeStringButton;
    private JButton beanToJsonButton;
    private JButton javaBeanToJSONButton;
    private JButton jsonToJavaBeanButton;
    private JButton keyValueSwapButton;
    private JPanel leftMenuPanel;
    private JButton gitButton;
    private JButton vaultSettingsButton;

    private static JsonBeautyForm jsonBeautyForm;
    private static boolean i18nRegistered;
    private JsonRSyntaxTextViewer textArea;
    private JsonRTextScrollPane scrollPane;
    private JToolBar leftMenuToolBar;
    private JToolBar actionToolBar;
    private JPanel actionToolBarPanel;
    private JComboBox fontNameComboBox;
    private JComboBox fontSizeComboBox;
    private JButton wrapButton;

    private JsonBeautyForm() {
        textArea = new JsonRSyntaxTextViewer();
        scrollPane = new JsonRTextScrollPane(textArea);

        leftMenuToolBar = new JToolBar();
        ToolbarUiUtil.configure(leftMenuToolBar);
        fontNameComboBox = new JComboBox();
        fontSizeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("5");
        defaultComboBoxModel1.addElement("6");
        defaultComboBoxModel1.addElement("7");
        defaultComboBoxModel1.addElement("8");
        defaultComboBoxModel1.addElement("9");
        defaultComboBoxModel1.addElement("10");
        defaultComboBoxModel1.addElement("11");
        defaultComboBoxModel1.addElement("12");
        defaultComboBoxModel1.addElement("13");
        defaultComboBoxModel1.addElement("14");
        defaultComboBoxModel1.addElement("15");
        defaultComboBoxModel1.addElement("16");
        defaultComboBoxModel1.addElement("17");
        defaultComboBoxModel1.addElement("18");
        defaultComboBoxModel1.addElement("19");
        defaultComboBoxModel1.addElement("20");
        defaultComboBoxModel1.addElement("21");
        defaultComboBoxModel1.addElement("22");
        defaultComboBoxModel1.addElement("23");
        defaultComboBoxModel1.addElement("24");
        defaultComboBoxModel1.addElement("25");
        defaultComboBoxModel1.addElement("26");
        defaultComboBoxModel1.addElement("27");
        defaultComboBoxModel1.addElement("28");
        defaultComboBoxModel1.addElement("29");
        defaultComboBoxModel1.addElement("30");
        fontSizeComboBox.setModel(defaultComboBoxModel1);
        fontSizeComboBox.setToolTipText("字号");

        ToolbarUiUtil.add(leftMenuToolBar, fontNameComboBox);
        ToolbarUiUtil.add(leftMenuToolBar, fontSizeComboBox);

        wrapButton = new JButton(new FlatSVGIcon("icon/wrap.svg"));
        wrapButton.setToolTipText("自动换行");
        leftMenuToolBar.add(wrapButton);

        leftMenuPanel.add(leftMenuToolBar);

        addButton = new JButton();
        addButton.setText("");
        addButton.setToolTipText("新建(Ctrl+N)");
        findButton = new JButton();
        findButton.setText("");
        findButton.setToolTipText("查找(Ctrl+F)");
        saveButton = new JButton();
        saveButton.setText("");
        saveButton.setToolTipText("保存(Ctrl+S)");
        newFolderButton = new JButton();
        newFolderButton.setText("");
        newFolderButton.setToolTipText("新建文件夹");
        deleteButton = new JButton();
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        exportButton = new JButton();
        exportButton.setText("");
        exportButton.setToolTipText("导出");
        beanToJsonButton = new JButton();
        beanToJsonButton.setText("");
        beanToJsonButton.setToolTipText("JavaBean转JSON");
        compressButton = new JButton();
        compressButton.setText("");
        compressButton.setToolTipText("压缩JSON");
        beautifyButton = new JButton();
        beautifyButton.setText("");
        beautifyButton.setToolTipText("格式化(Ctrl+Shift+F)");
        moreButton = new JButton();
        moreButton.setText("");

        gitButton = new JButton();
        gitButton.setText("");
        gitButton.setToolTipText("Git");
        vaultSettingsButton = new JButton();
        vaultSettingsButton.setText("");
        vaultSettingsButton.setIcon(new FlatSVGIcon("icon/host.svg"));
        vaultSettingsButton.setToolTipText("Vault settings");

        actionToolBar = new JToolBar();
        ToolbarUiUtil.configure(actionToolBar);
        actionToolBar.add(addButton);
        actionToolBar.add(newFolderButton);
        actionToolBar.add(findButton);
        actionToolBar.add(saveButton);
        ToolbarUiUtil.addGroupSeparator(actionToolBar);
        actionToolBar.add(deleteButton);
        actionToolBar.add(exportButton);
        actionToolBar.add(beanToJsonButton);
        actionToolBar.add(compressButton);
        actionToolBar.add(beautifyButton);
        actionToolBar.add(moreButton);
        ToolbarUiUtil.addGroupSeparator(actionToolBar);
        actionToolBar.add(vaultSettingsButton);
        actionToolBar.add(gitButton);
        actionToolBarPanel.add(actionToolBar, BorderLayout.EAST);

        UndoUtil.register(this);
    }

    public static JsonBeautyForm getInstance() {
        if (jsonBeautyForm == null) {
            jsonBeautyForm = new JsonBeautyForm();
        }
        return jsonBeautyForm;
    }

    public static void init() {
        jsonBeautyForm = getInstance();

        initUi();

        initList();

        initTextAreaFont();

        JsonBeautyListener.addListeners();

        JsonBeautyVaultWatcher.start();
        JsonBeautyAutoGitScheduler.start();
        JsonBeautyAutoPullScheduler.start();
        updateGitButtonStatus();

        jsonBeautyForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(JsonBeautyForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setPlaceholder(searchTextField, "common.search");
        I18nUiUtil.setToolTip(fontSizeComboBox, "quickNote.tooltip.fontSize");
        I18nUiUtil.setToolTip(wrapButton, "quickNote.tooltip.wrap");
        I18nUiUtil.setToolTip(addButton, "quickNote.tooltip.new");
        I18nUiUtil.setToolTip(newFolderButton, "quickNote.newFolder");
        I18nUiUtil.setToolTip(findButton, "quickNote.tooltip.find");
        I18nUiUtil.setToolTip(saveButton, "quickNote.tooltip.save");
        I18nUiUtil.setToolTip(deleteButton, "quickNote.tooltip.delete");
        I18nUiUtil.setToolTip(exportButton, "quickNote.tooltip.export");
        I18nUiUtil.setToolTip(beanToJsonButton, "json.tooltip.beanToJson");
        I18nUiUtil.setToolTip(compressButton, "json.tooltip.compress");
        I18nUiUtil.setToolTip(beautifyButton, "quickNote.tooltip.format");
        I18nUiUtil.setToolTip(moreButton, "json.tooltip.more");
        I18nUiUtil.setToolTip(gitButton, "jsonBeauty.tooltip.git");
        I18nUiUtil.setToolTip(vaultSettingsButton, "jsonBeauty.tooltip.vaultSettings");
        if (listSortComboBox != null) {
            I18nUiUtil.setToolTip(listSortComboBox, "quickNote.tooltip.listSort");
        }
        I18nUiUtil.setToolTip(listItemButton, "quickNote.tooltip.toggleList");
        I18nUiUtil.setToolTip(moreCloseButton, "quickNote.tooltip.close");

        I18nUiUtil.setText(ignoreCaseCheckBox, "json.ignoreCase");
        I18nUiUtil.setText(keySortCheckBox, "json.keySort");
        I18nUiUtil.setText(checkDuplicateCheckBox, "json.checkDuplicate");
        I18nUiUtil.setText(jsonToXmlButton, "json.toXml");
        I18nUiUtil.setText(xmlToJsonButton, "json.fromXml");
        I18nUiUtil.setText(javaBeanToJSONButton, "json.javaBeanToJson");
        I18nUiUtil.setText(jsonToJavaBeanButton, "json.jsonToJavaBean");
        I18nUiUtil.setText(getByJsonPathButton, "json.getByPath");
        I18nUiUtil.setText(getJsonPathButton, "json.pickPath");
        I18nUiUtil.setText(customFormatButton, "json.customFormat");
        I18nUiUtil.setText(escapeJsonButton, "json.escapeJson");
        I18nUiUtil.setText(escapeStringButton, "json.escapeString");
        I18nUiUtil.setText(unescapeButton, "json.unescape");
        I18nUiUtil.setText(keyValueSwapButton, "json.keyValueSwap");
    }

    private static void applyI18nStatic() {
        if (jsonBeautyForm != null) {
            jsonBeautyForm.applyI18n();
        }
    }

    private static void initUi() {
        getInstance().getContentSplitPane().setLeftComponent(jsonBeautyForm.getScrollPane());

        SearchFieldUiUtil.configure(jsonBeautyForm.getSearchTextField());
        jsonBeautyForm.getSearchTextField().putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索");

        jsonBeautyForm.getAddButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        jsonBeautyForm.getNewFolderButton().setIcon(new FlatSVGIcon("icon/folder-add.svg"));
        jsonBeautyForm.getFindButton().setIcon(new FlatSVGIcon("icon/find.svg"));
        jsonBeautyForm.getSaveButton().setIcon(new FlatSVGIcon("icon/save.svg"));
        jsonBeautyForm.getBeautifyButton().setIcon(new FlatSVGIcon("icon/json.svg"));
        jsonBeautyForm.getCompressButton().setIcon(new FlatSVGIcon("icon/compress_json.svg"));
        jsonBeautyForm.getBeanToJsonButton().setIcon(new FlatSVGIcon("icon/bean.svg"));
        jsonBeautyForm.getMoreButton().setIcon(new FlatSVGIcon("icon/more.svg"));
        jsonBeautyForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        jsonBeautyForm.getExportButton().setIcon(new FlatSVGIcon("icon/export.svg"));
        jsonBeautyForm.getGitButton().setIcon(new FlatSVGIcon("icon/diff.svg"));
        jsonBeautyForm.getListItemButton().setIcon(new FlatSVGIcon("icon/list.svg"));
        jsonBeautyForm.getWrapButton().setIcon(new FlatSVGIcon("icon/wrap.svg"));
        PanelCloseUtil.installTrailingCloseButton(jsonBeautyForm.getMoreCloseButton(),
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        jsonBeautyForm.getFindReplacePanel().setVisible(false);
        jsonBeautyForm.getMoreScrollPane().setVisible(false);
        configureSplitPanes();
        initListSortComboBox();
        initJsonTree();
        jsonBeautyForm.getJsonBeautyPanel().setMinimumSize(new Dimension(0, 300));

        jsonBeautyForm.getTextArea().grabFocus();

        jsonBeautyForm.getJsonBeautyPanel().updateUI();
    }

    private static void configureSplitPanes() {
        SplitPaneUtil.configureListEditorSplit(jsonBeautyForm.getSplitPane());
        SplitPaneUtil.configureEditorSecondarySplit(jsonBeautyForm.getContentSplitPane());
        SplitPaneUtil.relaxHorizontalMinimum(
                jsonBeautyForm.getJsonBeautyPanel(),
                jsonBeautyForm.getRightPanel(),
                jsonBeautyForm.getControlPanel(),
                jsonBeautyForm.getSplitPane(),
                jsonBeautyForm.getContentSplitPane()
        );
    }

    private static void initJsonTree() {
        jsonBeautyForm.noteTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        jsonBeautyForm.noteTree.setRootVisible(false);
        jsonBeautyForm.noteTree.setShowsRootHandles(true);
        jsonBeautyForm.noteTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        jsonBeautyForm.noteTree.setCellRenderer(new JsonBeautyTreeCellRenderer());
        jsonBeautyForm.noteTree.setVisibleRowCount(20);
        jsonBeautyForm.noteTree.putClientProperty(FlatClientProperties.STYLE,
                "selectionArc: 6; selectionInsets: 0,1,0,1");
        JsonBeautyTreeDragDrop.install(jsonBeautyForm.noteTree);

        Component parent = jsonBeautyForm.getNoteList();
        while (parent != null) {
            if (parent instanceof JScrollPane scrollPane) {
                scrollPane.setViewportView(jsonBeautyForm.noteTree);
                break;
            }
            parent = parent.getParent();
        }
        jsonBeautyForm.getNoteList().setVisible(false);
    }

    private static void initListSortComboBox() {
        JPanel leftPanel = (JPanel) jsonBeautyForm.getSearchTextField().getParent();
        JScrollPane scrollPane = null;
        for (Component child : leftPanel.getComponents()) {
            if (child instanceof JScrollPane pane) {
                scrollPane = pane;
                break;
            }
        }
        if (scrollPane == null) {
            return;
        }

        leftPanel.remove(scrollPane);
        jsonBeautyForm.listSortComboBox = new JComboBox<>();
        for (JsonBeautyListSortMode mode : JsonBeautyListSortMode.values()) {
            jsonBeautyForm.listSortComboBox.addItem(mode);
        }
        jsonBeautyForm.listSortComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String label = value instanceof JsonBeautyListSortMode mode
                        ? I18n.get(mode.i18nKey())
                        : "";
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        jsonBeautyForm.listSortComboBox.setSelectedItem(JsonBeautyListSortMode.fromConfig());

        JPanel listArea = new JPanel(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        listArea.add(jsonBeautyForm.listSortComboBox, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false));
        listArea.add(scrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null, null, null, 0, false));
        leftPanel.add(listArea, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null, null, null, 0, false));
    }

    public static JsonBeautyListSortMode getListSortMode() {
        if (jsonBeautyForm == null || jsonBeautyForm.listSortComboBox == null) {
            return JsonBeautyListSortMode.fromConfig();
        }
        Object selected = jsonBeautyForm.listSortComboBox.getSelectedItem();
        return selected instanceof JsonBeautyListSortMode mode ? mode : JsonBeautyListSortMode.fromConfig();
    }

    public static void refreshJsonTree() {
        JsonBeautyVaultUtil.ensureVaultReady();
        if (jsonBeautyForm.getNoteTree() == null) {
            log.error("JSON tree is not initialized");
            return;
        }

        String titleFilterKeyWord = jsonBeautyForm.getSearchTextField().getText();
        List<TJsonBeauty> jsonList = JsonBeautyVaultUtil.listByFilter(titleFilterKeyWord);
        List<String> folders = StringUtils.isNotBlank(titleFilterKeyWord)
                ? List.of()
                : JsonBeautyVaultUtil.listFolders();
        jsonBeautyForm.getNoteTree().setModel(
                JsonBeautyTreeUtil.buildTreeModel(jsonList, folders, getListSortMode()));
        applyTreeExpandPreference(jsonBeautyForm.getNoteTree());

        String preservePath = JsonBeautyListener.selectedPathJson;
        if (StringUtils.isNotBlank(preservePath)) {
            selectJsonInList(preservePath);
        }
    }

    private static void applyTreeExpandPreference(JTree tree) {
        VaultTreeUiUtil.applyExpandMode(tree,
                VaultTreeExpandMode.fromId(App.config.getJsonBeautyTreeExpandMode()));
    }

    public static void initList() {
        populateJsonTree(false);
    }

    public static void refreshList() {
        populateJsonTree(true);
        updateGitButtonStatus();
    }

    private static void populateJsonTree(boolean preserveOnly) {
        JsonBeautyVaultUtil.ensureVaultReady();
        if (jsonBeautyForm.getNoteTree() == null) {
            log.error("JSON tree is not initialized");
            return;
        }

        String titleFilterKeyWord = jsonBeautyForm.getSearchTextField().getText();
        List<TJsonBeauty> jsonList = JsonBeautyVaultUtil.listByFilter(titleFilterKeyWord);
        List<String> folders = StringUtils.isNotBlank(titleFilterKeyWord)
                ? List.of()
                : JsonBeautyVaultUtil.listFolders();
        jsonBeautyForm.getNoteTree().setModel(
                JsonBeautyTreeUtil.buildTreeModel(jsonList, folders, getListSortMode()));
        applyTreeExpandPreference(jsonBeautyForm.getNoteTree());

        if (jsonList.isEmpty()) {
            JsonBeautyListener.ignoreQuickSave = true;
            try {
                JsonBeautyListener.selectedPathJson = null;
                JsonBeautyListener.selectedNameJson = null;
                jsonBeautyForm.getTextArea().setText("");
            } finally {
                JsonBeautyListener.ignoreQuickSave = false;
            }
            return;
        }

        JsonBeautyListener.ignoreQuickSave = true;
        try {
            String preservePath = JsonBeautyListener.selectedPathJson;
            TJsonBeauty selectedItem = null;
            if (StringUtils.isNotEmpty(preservePath)) {
                selectedItem = jsonList.stream()
                        .filter(item -> preservePath.equals(item.getRelativePath()))
                        .findFirst()
                        .orElse(null);
            }
            if (selectedItem != null) {
                selectJsonInList(selectedItem.getRelativePath());
                if (!preserveOnly) {
                    showJson(selectedItem);
                }
            } else if (StringUtils.isNotEmpty(preservePath)) {
                TJsonBeauty itemOnDisk = JsonBeautyVaultUtil.loadByPath(preservePath);
                if (itemOnDisk == null) {
                    jsonBeautyForm.getTextArea().setText("");
                    JsonBeautyListener.selectedPathJson = null;
                    JsonBeautyListener.selectedNameJson = null;
                } else {
                    selectJsonInList(preservePath);
                    showJson(itemOnDisk);
                }
            } else if (!preserveOnly) {
                selectedItem = JsonBeautyTreeUtil.sortedItems(jsonList, getListSortMode()).get(0);
                selectJsonInList(selectedItem.getRelativePath());
                showJson(selectedItem);
            }
        } catch (Exception e1) {
            log.error(e1.toString());
        } finally {
            JsonBeautyListener.ignoreQuickSave = false;
        }
    }

    public static void selectJsonInList(String relativePath) {
        if (jsonBeautyForm == null || jsonBeautyForm.noteTree == null || StringUtils.isBlank(relativePath)) {
            return;
        }
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) jsonBeautyForm.noteTree.getModel().getRoot();
        DefaultMutableTreeNode node = JsonBeautyTreeUtil.findNodeByPath(root, relativePath);
        if (node != null) {
            TreePath treePath = new TreePath(node.getPath());
            VaultTreeUiUtil.ensurePathVisible(jsonBeautyForm.noteTree, treePath);
            jsonBeautyForm.noteTree.setSelectionPath(treePath);
        }
    }

    public static TJsonBeauty getSelectedTreeJson() {
        if (jsonBeautyForm == null || jsonBeautyForm.getNoteTree() == null) {
            return null;
        }
        TreePath selectionPath = jsonBeautyForm.getNoteTree().getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        return JsonBeautyTreeUtil.selectedItem(node);
    }

    public static String getSelectedFolderPath() {
        if (jsonBeautyForm == null || jsonBeautyForm.getNoteTree() == null) {
            return "";
        }
        TreePath selectionPath = jsonBeautyForm.getNoteTree().getSelectionPath();
        if (selectionPath == null) {
            return "";
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        return JsonBeautyTreeUtil.selectedFolderPath(node);
    }

    public static List<TJsonBeauty> getSelectedJsons() {
        if (jsonBeautyForm == null || jsonBeautyForm.getNoteTree() == null) {
            return List.of();
        }
        TreePath[] selectionPaths = jsonBeautyForm.getNoteTree().getSelectionPaths();
        if (selectionPaths == null) {
            return List.of();
        }
        List<TJsonBeauty> items = new ArrayList<>();
        for (TreePath selectionPath : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            TJsonBeauty item = JsonBeautyTreeUtil.selectedItem(node);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public static void showJson(TJsonBeauty item) {
        if (item == null) {
            return;
        }
        String path = item.getRelativePath();
        TJsonBeauty itemFromDisk = JsonBeautyVaultUtil.loadByPath(path);
        if (itemFromDisk != null) {
            item = itemFromDisk;
        }
        JsonBeautyListener.ignoreQuickSave = true;
        try {
            JsonBeautyListener.selectedPathJson = path;
            JsonBeautyListener.selectedNameJson = item.getName();
        jsonBeautyForm.getTextArea().setText(StringUtils.defaultString(item.getContent()));
        jsonBeautyForm.getTextArea().setCaretPosition(0);
        jsonBeautyForm.getScrollPane().getVerticalScrollBar().setValue(0);
        jsonBeautyForm.getScrollPane().getHorizontalScrollBar().setValue(0);
        } finally {
            JsonBeautyListener.ignoreQuickSave = false;
        }
    }

    public static void reloadCurrentFromDiskIfClean() {
        reloadCurrentFromDisk(false);
    }

    public static void reloadCurrentFromDisk(boolean force) {
        if (!force && JsonBeautyVaultRefreshCoordinator.hasUnsavedChanges()) {
            return;
        }
        String path = JsonBeautyListener.selectedPathJson;
        if (StringUtils.isBlank(path)) {
            return;
        }
        TJsonBeauty item = JsonBeautyVaultUtil.loadByPath(path);
        JsonBeautyListener.ignoreQuickSave = true;
        try {
            if (item == null) {
                jsonBeautyForm.getTextArea().setText("");
                JsonBeautyListener.selectedPathJson = null;
                JsonBeautyListener.selectedNameJson = null;
            } else {
                jsonBeautyForm.getTextArea().setText(item.getContent());
            }
        } finally {
            JsonBeautyListener.ignoreQuickSave = false;
        }
    }

    public static void reloadJsonAfterDiscard(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            return;
        }
        String path = JsonBeautyVaultUtil.normalizeRelativePath(relativePath);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
        if (!path.equals(JsonBeautyListener.selectedPathJson)) {
            return;
        }
        reloadCurrentFromDisk(true);
    }

    public static void updateGitButtonStatus() {
        if (jsonBeautyForm == null || jsonBeautyForm.gitButton == null) {
            return;
        }
        SwingWorker<QuickNoteGitStatus, Void> worker = new SwingWorker<>() {
            @Override
            protected QuickNoteGitStatus doInBackground() {
                return QuickNoteGitUtil.getStatus(JsonBeautyVaultUtil.getVaultDir());
            }

            @Override
            protected void done() {
                try {
                    QuickNoteGitStatus status = get();
                    JButton button = jsonBeautyForm.gitButton;
                    if (!status.isGitRepo()) {
                        button.setText("");
                        I18nUiUtil.setToolTip(button, "jsonBeauty.tooltip.git");
                        return;
                    }
                    int pending = status.getChangedCount() + status.getConflictCount();
                    button.setText(pending > 0 ? String.valueOf(pending) : "");
                    if (status.hasPendingChanges() || status.getAhead() > 0 || status.getBehind() > 0
                            || StringUtils.isNotBlank(status.getBranch())) {
                        button.setToolTipText(I18n.format("jsonBeauty.tooltip.gitStatus",
                                StringUtils.defaultIfBlank(status.getBranch(), "-"),
                                status.getChangedCount(),
                                status.getConflictCount(),
                                status.getAhead(),
                                status.getBehind()));
                    } else {
                        I18nUiUtil.setToolTip(button, "jsonBeauty.tooltip.git");
                    }
                    JsonBeautyGitDialog.refreshIfVisible();
                } catch (Exception ignored) {
                    // ignore background status read failures
                }
            }
        };
        worker.execute();
    }

    public static void initTextAreaFont() {
        String fontName = App.config.getJsonBeautyFontName();
        int fontSize = App.config.getJsonBeautyFontSize();
        if (fontSize == 0) {
            fontSize = jsonBeautyForm.getTextArea().getFont().getSize() + 2;
        }

        getSysFontList();

        jsonBeautyForm.getFontNameComboBox().setSelectedItem(fontName);
        jsonBeautyForm.getFontSizeComboBox().setSelectedItem(String.valueOf(fontSize));

        Font font = new Font(fontName, Font.PLAIN, fontSize);
//        jsonBeautyForm.getTextArea().setFont(font);
    }

    /**
     * 获取系统字体列表
     */
    private static void getSysFontList() {
        jsonBeautyForm.getFontNameComboBox().removeAllItems();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        for (String font : fonts) {
            if (StringUtils.isNotBlank(font)) {
                jsonBeautyForm.getFontNameComboBox().addItem(font);
            }
        }
        jsonBeautyForm.getFontNameComboBox().addItem(FlatJetBrainsMonoFont.FAMILY);
//        jsonBeautyForm.getFontNameComboBox().addItem(FlatInterFont.FAMILY);
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
        jsonBeautyPanel = new JPanel();
        jsonBeautyPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        jsonBeautyPanel.setMinimumSize(new Dimension(400, 300));
        jsonBeautyPanel.setPreferredSize(new Dimension(400, 300));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(172);
        splitPane.setDividerSize(10);
        jsonBeautyPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setMinimumSize(new Dimension(0, 64));
        splitPane.setLeftComponent(panel1);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        noteList = new JList();
        scrollPane1.setViewportView(noteList);
        searchTextField = new JTextField();
        panel1.add(searchTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(rightPanel);
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        rightPanel.add(controlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.add(menuPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        final Spacer spacer1 = new Spacer();
        menuPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        actionToolBarPanel = new JPanel();
        actionToolBarPanel.setLayout(new BorderLayout(0, 0));
        menuPanel.add(actionToolBarPanel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        listItemButton = new JButton();
        listItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/listFiles_dark.png")));
        listItemButton.setText("");
        listItemButton.setToolTipText("显示/隐藏列表");
        menuPanel.add(listItemButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        leftMenuPanel = new JPanel();
        leftMenuPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        menuPanel.add(leftMenuPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, 1, 1, null, null, null, 0, false));
        findReplacePanel = new JPanel();
        findReplacePanel.setLayout(new BorderLayout(0, 0));
        findReplacePanel.setVisible(true);
        controlPanel.add(findReplacePanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        contentSplitPane = new JSplitPane();
        contentSplitPane.setDividerLocation(200);
        controlPanel.add(contentSplitPane, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentSplitPane.setLeftComponent(panel2);
        moreScrollPane = new JScrollPane();
        contentSplitPane.setRightComponent(moreScrollPane);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(21, 1, new Insets(10, 10, 10, 10), -1, -1));
        moreScrollPane.setViewportView(panel3);
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(20, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        ignoreCaseCheckBox = new JCheckBox();
        ignoreCaseCheckBox.setText("忽略key的大小写");
        panel3.add(ignoreCaseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keySortCheckBox = new JCheckBox();
        keySortCheckBox.setText("Key按照字母顺序排序");
        panel3.add(keySortCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkDuplicateCheckBox = new JCheckBox();
        checkDuplicateCheckBox.setText("检查重复key");
        panel3.add(checkDuplicateCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(separator1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        moreCloseButton = new JButton();
        moreCloseButton.setText("");
        moreCloseButton.setToolTipText("关闭");
        panel3.add(moreCloseButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jsonToXmlButton = new JButton();
        jsonToXmlButton.setText("JSON转XML");
        panel3.add(jsonToXmlButton, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        xmlToJsonButton = new JButton();
        xmlToJsonButton.setText("XML转JSON");
        panel3.add(xmlToJsonButton, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        javaBeanToJSONButton = new JButton();
        javaBeanToJSONButton.setText("JavaBean转JSON");
        panel3.add(javaBeanToJSONButton, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jsonToJavaBeanButton = new JButton();
        jsonToJavaBeanButton.setText("JSON转JavaBean");
        panel3.add(jsonToJavaBeanButton, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel3.add(separator2, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel3.add(separator3, new GridConstraints(16, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jsonPathTextField = new JTextField();
        jsonPathTextField.setText("$");
        panel3.add(jsonPathTextField, new GridConstraints(17, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        getByJsonPathButton = new JButton();
        getByJsonPathButton.setText("通过JSON Path取值");
        panel3.add(getByJsonPathButton, new GridConstraints(18, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        getJsonPathButton = new JButton();
        getJsonPathButton.setText("可视化获取JSON Path");
        panel3.add(getJsonPathButton, new GridConstraints(19, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        customFormatButton = new JButton();
        customFormatButton.setText("格式化");
        panel3.add(customFormatButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        escapeJsonButton = new JButton();
        escapeJsonButton.setText("转义(JSON)");
        panel3.add(escapeJsonButton, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        escapeStringButton = new JButton();
        escapeStringButton.setText("转义(字符串)");
        panel3.add(escapeStringButton, new GridConstraints(14, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        unescapeButton = new JButton();
        unescapeButton.setText("反转义");
        panel3.add(unescapeButton, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel3.add(separator4, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        keyValueSwapButton = new JButton();
        keyValueSwapButton.setText("Key-Value互换");
        panel3.add(keyValueSwapButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        controlPanel.add(spacer3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jsonBeautyPanel;
    }

}
