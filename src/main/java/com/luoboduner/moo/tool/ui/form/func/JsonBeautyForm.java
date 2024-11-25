package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TJsonBeautyMapper;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.textviewer.JsonRTextScrollPane;
import com.luoboduner.moo.tool.ui.component.textviewer.JsonRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
    private JTable noteListTable;
    private JButton deleteButton;
    private JButton saveButton;
    private JSplitPane splitPane;
    private JButton addButton;
    private JComboBox fontNameComboBox;
    private JComboBox fontSizeComboBox;
    private JButton findButton;
    private JButton wrapButton;
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
    private JLabel moreCloseLabel;
    private JButton getJsonPathButton;
    private JScrollPane moreScrollPane;
    private JButton escapeJsonButton;
    private JButton unescapeButton;
    private JButton escapeStringButton;
    private JButton beanToJsonButton;
    private JButton javaBeanToJSONButton;

    private static JsonBeautyForm jsonBeautyForm;
    private static TJsonBeautyMapper jsonBeautyMapper = MybatisUtil.getSqlSession().getMapper(TJsonBeautyMapper.class);

    private JsonRSyntaxTextViewer textArea;
    private JsonRTextScrollPane scrollPane;

    private JsonBeautyForm() {
        textArea = new JsonRSyntaxTextViewer();
        scrollPane = new JsonRTextScrollPane(textArea);

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

        initListTable();

        initTextAreaFont();

        JsonBeautyListener.addListeners();
    }

    private static void initUi() {
        getInstance().getContentSplitPane().setLeftComponent(jsonBeautyForm.getScrollPane());

        jsonBeautyForm.getSearchTextField().putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索");
        jsonBeautyForm.getSearchTextField().putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSearchIcon());

        jsonBeautyForm.getAddButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        jsonBeautyForm.getFindButton().setIcon(new FlatSVGIcon("icon/find.svg"));
        jsonBeautyForm.getSaveButton().setIcon(new FlatSVGIcon("icon/save.svg"));
        jsonBeautyForm.getBeautifyButton().setIcon(new FlatSVGIcon("icon/json.svg"));
        jsonBeautyForm.getCompressButton().setIcon(new FlatSVGIcon("icon/compress_json.svg"));
        jsonBeautyForm.getBeanToJsonButton().setIcon(new FlatSVGIcon("icon/bean.svg"));
        jsonBeautyForm.getMoreButton().setIcon(new FlatSVGIcon("icon/more.svg"));
        jsonBeautyForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        jsonBeautyForm.getExportButton().setIcon(new FlatSVGIcon("icon/export.svg"));
        jsonBeautyForm.getListItemButton().setIcon(new FlatSVGIcon("icon/list.svg"));
        jsonBeautyForm.getWrapButton().setIcon(new FlatSVGIcon("icon/wrap.svg"));
        jsonBeautyForm.getMoreCloseLabel().setIcon(new FlatSVGIcon("icon/remove2.svg"));

        jsonBeautyForm.getFindReplacePanel().setVisible(false);
        int totalWidth = jsonBeautyForm.getContentSplitPane().getWidth();
        jsonBeautyForm.getContentSplitPane().setDividerLocation(totalWidth);
        jsonBeautyForm.getMoreScrollPane().setVisible(false);

        jsonBeautyForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        jsonBeautyForm.getNoteListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);

        jsonBeautyForm.getTextArea().grabFocus();

        jsonBeautyForm.getJsonBeautyPanel().updateUI();
    }

    public static void initListTable() {
        String[] headerNames = {"id", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        jsonBeautyForm.getNoteListTable().setModel(model);
        // 隐藏表头
        JTableUtil.hideTableHeader(jsonBeautyForm.getNoteListTable());
        // 隐藏id列
        JTableUtil.hideColumn(jsonBeautyForm.getNoteListTable(), 0);

        Object[] data;

        String titleFilterKeyWord = jsonBeautyForm.getSearchTextField().getText();
        titleFilterKeyWord = "%" + titleFilterKeyWord + "%";

        List<TJsonBeauty> jsonBeautyList = jsonBeautyMapper.selectByFilter(titleFilterKeyWord);
        for (TJsonBeauty tJsonBeauty : jsonBeautyList) {
            data = new Object[2];
            data[0] = tJsonBeauty.getId();
            data[1] = tJsonBeauty.getName();
            model.addRow(data);
        }
        if (jsonBeautyList.size() > 0) {
            JsonBeautyListener.ignoreQuickSave = true;
            try {
                JsonBeautyListener.selectedNameJson = jsonBeautyList.get(0).getName();
                jsonBeautyForm.getTextArea().setText(jsonBeautyList.get(0).getContent());
                jsonBeautyForm.getNoteListTable().setRowSelectionInterval(0, 0);
            } catch (Exception e2) {
                log.error(e2.getMessage());
            } finally {
                JsonBeautyListener.ignoreQuickSave = false;
            }

        }
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
        jsonBeautyForm.getFontNameComboBox().addItem(FlatJetBrainsMonoFont.FAMILY);
        for (String font : fonts) {
            if (StringUtils.isNotBlank(font)) {
                jsonBeautyForm.getFontNameComboBox().addItem(font);
            }
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
        noteListTable = new JTable();
        scrollPane1.setViewportView(noteListTable);
        searchTextField = new JTextField();
        panel1.add(searchTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(rightPanel);
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        rightPanel.add(controlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayoutManager(1, 14, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.add(menuPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        saveButton = new JButton();
        saveButton.setIcon(new ImageIcon(getClass().getResource("/icon/menu-saveall_dark.png")));
        saveButton.setText("");
        saveButton.setToolTipText("保存(Ctrl+S)");
        menuPanel.add(saveButton, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        menuPanel.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        fontNameComboBox = new JComboBox();
        fontNameComboBox.setToolTipText("设置字体");
        menuPanel.add(fontNameComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        menuPanel.add(fontSizeComboBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findButton = new JButton();
        findButton.setIcon(new ImageIcon(getClass().getResource("/icon/find_dark.png")));
        findButton.setText("");
        findButton.setToolTipText("查找(Ctrl+F)");
        menuPanel.add(findButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wrapButton = new JButton();
        wrapButton.setIcon(new ImageIcon(getClass().getResource("/icon/toggleSoftWrap_dark.png")));
        wrapButton.setText("");
        wrapButton.setToolTipText("自动换行");
        menuPanel.add(wrapButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        beautifyButton = new JButton();
        beautifyButton.setIcon(new ImageIcon(getClass().getResource("/icon/object_dark.png")));
        beautifyButton.setText("");
        beautifyButton.setToolTipText("格式化(Ctrl+Shift+F)");
        menuPanel.add(beautifyButton, new GridConstraints(0, 12, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        listItemButton = new JButton();
        listItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/listFiles_dark.png")));
        listItemButton.setText("");
        listItemButton.setToolTipText("显示/隐藏列表");
        menuPanel.add(listItemButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        addButton.setText("");
        addButton.setToolTipText("新建(Ctrl+N)");
        menuPanel.add(addButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exportButton = new JButton();
        exportButton.setIcon(new ImageIcon(getClass().getResource("/icon/export_dark.png")));
        exportButton.setText("");
        exportButton.setToolTipText("导出");
        menuPanel.add(exportButton, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        menuPanel.add(deleteButton, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        compressButton = new JButton();
        compressButton.setText("");
        compressButton.setToolTipText("压缩JSON");
        menuPanel.add(compressButton, new GridConstraints(0, 11, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        moreButton = new JButton();
        moreButton.setText("");
        menuPanel.add(moreButton, new GridConstraints(0, 13, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        beanToJsonButton = new JButton();
        beanToJsonButton.setText("");
        beanToJsonButton.setToolTipText("JavaBean转JSON");
        menuPanel.add(beanToJsonButton, new GridConstraints(0, 10, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findReplacePanel = new JPanel();
        findReplacePanel.setLayout(new BorderLayout(0, 0));
        findReplacePanel.setVisible(true);
        controlPanel.add(findReplacePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        contentSplitPane = new JSplitPane();
        contentSplitPane.setDividerLocation(200);
        controlPanel.add(contentSplitPane, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentSplitPane.setLeftComponent(panel2);
        moreScrollPane = new JScrollPane();
        contentSplitPane.setRightComponent(moreScrollPane);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(18, 1, new Insets(10, 10, 10, 10), -1, -1));
        moreScrollPane.setViewportView(panel3);
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(17, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        panel3.add(separator1, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel4.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        moreCloseLabel = new JLabel();
        moreCloseLabel.setText("");
        panel4.add(moreCloseLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jsonToXmlButton = new JButton();
        jsonToXmlButton.setText("JSON转XML");
        panel3.add(jsonToXmlButton, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        xmlToJsonButton = new JButton();
        xmlToJsonButton.setText("XML转JSON");
        panel3.add(xmlToJsonButton, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        javaBeanToJSONButton = new JButton();
        javaBeanToJSONButton.setText("JavaBean转JSON");
        panel3.add(javaBeanToJSONButton, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel3.add(separator2, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel3.add(separator3, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jsonPathTextField = new JTextField();
        jsonPathTextField.setText("$.");
        panel3.add(jsonPathTextField, new GridConstraints(14, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        getByJsonPathButton = new JButton();
        getByJsonPathButton.setText("通过JSON Path取值");
        panel3.add(getByJsonPathButton, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        getJsonPathButton = new JButton();
        getJsonPathButton.setText("可视化获取JSON Path");
        panel3.add(getJsonPathButton, new GridConstraints(16, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        customFormatButton = new JButton();
        customFormatButton.setText("格式化");
        panel3.add(customFormatButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        escapeJsonButton = new JButton();
        escapeJsonButton.setText("转义(JSON)");
        panel3.add(escapeJsonButton, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        escapeStringButton = new JButton();
        escapeStringButton.setText("转义(字符串)");
        panel3.add(escapeStringButton, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        unescapeButton = new JButton();
        unescapeButton.setText("反转义");
        panel3.add(unescapeButton, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jsonBeautyPanel;
    }

}
