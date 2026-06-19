package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.util.ArrayUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.util.ColorFunctions;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.QuickNoteGitStatus;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.component.SplitPaneUtil;
import com.luoboduner.moo.tool.ui.component.ToolbarUiUtil;
import com.luoboduner.moo.tool.ui.component.PanelCloseUtil;
import com.luoboduner.moo.tool.ui.component.QuickNoteTreeCellRenderer;
import com.luoboduner.moo.tool.ui.component.QuickNoteTreeDragDrop;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteEditorPanel;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewerManager;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.QuickNoteConflictHighlightUtil;
import com.luoboduner.moo.tool.util.QuickNoteGitUtil;
import com.luoboduner.moo.tool.util.QuickNoteTreeUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import com.luoboduner.moo.tool.util.QuickNoteVaultWatcher;
import com.luoboduner.moo.tool.util.ScrollUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 随手记
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/15.
 */
@Getter
@Slf4j
public class QuickNoteForm {
    private JPanel quickNotePanel;
    private JList<TQuickNote> noteList;
    private JTree noteTree;
    private JButton deleteButton;
    private JButton saveButton;
    private JSplitPane splitPane;
    private JButton addButton;
    private JComboBox fontNameComboBox;
    private JComboBox fontSizeComboBox;
    private JButton findButton;
    private JButton listItemButton;
    private JPanel rightPanel;
    private JPanel controlPanel;
    private JButton exportButton;
    private JPanel findReplacePanel;
    private JPanel menuPanel;
    private JCheckBox trimBlankCheckBox;
    private JCheckBox clearTabTCheckBox;
    private JCheckBox clearEnterCheckBox;
    private JCheckBox scientificToNormalCheckBox;
    private JCheckBox toThousandthCheckBox;
    private JCheckBox enterToCommaCheckBox;
    private JCheckBox enterToCommaDoubleQuotesCheckBox;
    private JCheckBox commaToEnterCheckBox;
    private JButton startQuickReplaceButton;
    private JCheckBox tabToEnterCheckBox;
    private JCheckBox enterToCommaSingleQuotesCheckBox;
    private JScrollPane quickReplaceScrollPane;
    private JPanel quickReplacePanel;
    private JButton quickReplaceButton;
    private JButton quickReplaceCloseButton;
    private JSplitPane contentSplitPane;
    private JComboBox syntaxComboBox;
    private JButton formatButton;
    private JLabel tipsLabel;
    private JPanel indicatorPanel;
    private JPanel leftMenuPanel;
    private JPanel colorSettingPanel;
    private JCheckBox underlineToHumpCheckBox;
    private JCheckBox humpToUnderlineCheckBox;
    private JCheckBox trimBlankRowCheckBox;
    private JCheckBox uperToLowerCheckBox;
    private JCheckBox lowerToUperCheckBox;
    private JCheckBox toNormalNumCheckBox;
    private JCheckBox normalToScientificCheckBox;
    private JTextField searchTextField;
    private JCheckBox deduplicationByLineCheckBox;
    private JCheckBox deduplicationByLineCntCheckBox;
    private JCheckBox escapeCheckBox;
    private JCheckBox unescapeCheckBox;
    private JCheckBox commaSingleQuotesToEnterCheckBox;
    private JCheckBox commaDoubleQuotesToEnterCheckBox;
    private JCheckBox reverseByRowCheckBox;
    private JCheckBox sortFromAToZByRowCheckBox;
    private JCheckBox sortFromZToAByRowCheckBox;
    private JCheckBox sortByPinyinCheckBox;
    private JButton infoButton;
    private JButton gitButton;

    private JButton colorButton;
    private JCheckBox searchContentCheckBox;

    private JToggleButton wrapButton;
    private JButton unOrderListButton;
    private JButton orderListButton;
    private JButton insertImageButton;
    private JToolBar leftMenuToolBar;

    private JToolBar toolBar;

    private JToolBar actionToolBar;

    private JPanel actionToolBarPanel;
    public final static String[] COLOR_KEYS = {
            "default", "Moo.note.color.color1", "Moo.note.color.color2", "Moo.note.color.color3",
            "Moo.note.color.color4", "Moo.note.color.color5", "Moo.note.color.color6",
            "Moo.note.color.color7", "Moo.note.color.color8", "Moo.note.color.color9",
            "Moo.note.color.color10", "Moo.note.color.color11", "Moo.note.color.color12",
            "Moo.note.color.color13", "Moo.note.color.color14", "Moo.note.color.color15",
            "Moo.note.color.color16", "Moo.note.color.color17"
    };
    public final static JToggleButton[] COLOR_BUTTONS = new JToggleButton[COLOR_KEYS.length];

    private static QuickNoteForm quickNoteForm;
    private static boolean i18nRegistered;

    public static QuickNoteRSyntaxTextViewerManager quickNoteRSyntaxTextViewerManager;

    private QuickNoteForm() {
        colorButton = new JButton(new ListColorIcon("Moo.accent.blue", 18, 18));
        colorButton.setSelected(true);

        toolBar = new JToolBar();
        ToolbarUiUtil.configure(toolBar);
        searchContentCheckBox = new JCheckBox();

        leftMenuToolBar = new JToolBar();
        ToolbarUiUtil.configure(leftMenuToolBar);
        leftMenuToolBar.add(colorButton);
        ToolbarUiUtil.add(leftMenuToolBar, syntaxComboBox);
        ToolbarUiUtil.add(leftMenuToolBar, fontNameComboBox);
        ToolbarUiUtil.add(leftMenuToolBar, fontSizeComboBox);
        wrapButton = new JToggleButton(new FlatSVGIcon("icon/wrap.svg"));
        wrapButton.setSelected(false);
        wrapButton.setToolTipText("自动换行");
        leftMenuToolBar.add(wrapButton);
        // separator
        ToolbarUiUtil.addGroupSeparator(leftMenuToolBar);
        unOrderListButton = new JButton(new FlatSVGIcon("icon/list_unordered.svg"));
        unOrderListButton.setToolTipText("无序列表");
        orderListButton = new JButton(new FlatSVGIcon("icon/list_ordered.svg"));
        orderListButton.setToolTipText("有序列表");
        leftMenuToolBar.add(unOrderListButton);
        leftMenuToolBar.add(orderListButton);
        insertImageButton = new JButton(new FlatSVGIcon("icon/image.svg"));
        insertImageButton.setToolTipText("插入图片");
        insertImageButton.setVisible(false);
        leftMenuToolBar.add(insertImageButton);

        addButton = new JButton();
        addButton.setText("");
        addButton.setToolTipText("新建(Ctrl+N)");
        findButton = new JButton();
        findButton.setText("");
        findButton.setToolTipText("查找(Ctrl+F)");
        saveButton = new JButton();
        saveButton.setText("");
        saveButton.setToolTipText("保存(Ctrl+S)");
        deleteButton = new JButton();
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        exportButton = new JButton();
        exportButton.setText("");
        exportButton.setToolTipText("导出");
        formatButton = new JButton();
        formatButton.setText("");
        formatButton.setToolTipText("格式化(Ctrl+Shift+F)");
        infoButton = new JButton();
        infoButton.setText("");
        infoButton.setToolTipText("文档信息");
        gitButton = new JButton();
        gitButton.setText("");
        gitButton.setToolTipText("Git");
        quickReplaceButton = new JButton();
        quickReplaceButton.setText("");
        quickReplaceButton.setToolTipText("快捷替换");

        actionToolBar = new JToolBar();
        ToolbarUiUtil.configure(actionToolBar);
        actionToolBar.add(addButton);
        actionToolBar.add(findButton);
        actionToolBar.add(saveButton);
        ToolbarUiUtil.addGroupSeparator(actionToolBar);
        actionToolBar.add(deleteButton);
        actionToolBar.add(exportButton);
        actionToolBar.add(formatButton);
        actionToolBar.add(infoButton);
        actionToolBar.add(gitButton);
        actionToolBar.add(quickReplaceButton);
        actionToolBarPanel.add(actionToolBar, BorderLayout.EAST);

        UndoUtil.register(this);
    }

    public static QuickNoteForm getInstance() {
        if (quickNoteForm == null) {
            quickNoteForm = new QuickNoteForm();
        }
        return quickNoteForm;
    }

    public static void resetInstance() {
        quickNoteForm = null;
    }

    public static void init() {
        quickNoteForm = getInstance();

        quickNoteRSyntaxTextViewerManager = new QuickNoteRSyntaxTextViewerManager();

        initUi();

        initTextAreaFont();

        initNoteList();

        QuickNoteVaultWatcher.start();
        updateGitButtonStatus();

        QuickNoteListener.addListeners();

        quickNoteForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(QuickNoteForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setPlaceholder(searchTextField, "common.search");
        I18nUiUtil.setToolTip(addButton, "quickNote.tooltip.new");
        I18nUiUtil.setToolTip(findButton, "quickNote.tooltip.find");
        I18nUiUtil.setToolTip(saveButton, "quickNote.tooltip.save");
        I18nUiUtil.setToolTip(deleteButton, "quickNote.tooltip.delete");
        I18nUiUtil.setToolTip(exportButton, "quickNote.tooltip.export");
        I18nUiUtil.setToolTip(formatButton, "quickNote.tooltip.format");
        I18nUiUtil.setToolTip(infoButton, "quickNote.tooltip.docInfo");
        I18nUiUtil.setToolTip(gitButton, "quickNote.tooltip.git");
        I18nUiUtil.setToolTip(quickReplaceButton, "quickNote.tooltip.quickReplace");
        I18nUiUtil.setToolTip(searchContentCheckBox, "quickNote.tooltip.includeContent");
        I18nUiUtil.setToolTip(fontNameComboBox, "quickNote.tooltip.font");
        I18nUiUtil.setToolTip(fontSizeComboBox, "quickNote.tooltip.fontSize");
        I18nUiUtil.setToolTip(listItemButton, "quickNote.tooltip.toggleList");
        I18nUiUtil.setToolTip(wrapButton, "quickNote.tooltip.wrap");
        I18nUiUtil.setToolTip(unOrderListButton, "quickNote.tooltip.unorderedList");
        I18nUiUtil.setToolTip(orderListButton, "quickNote.tooltip.orderedList");
        I18nUiUtil.setToolTip(insertImageButton, "quickNote.tooltip.insertImage");
        I18nUiUtil.setToolTip(quickReplaceCloseButton, "quickNote.tooltip.close");

        I18nUiUtil.setText(trimBlankCheckBox, "quickNote.replace.trimBlank");
        I18nUiUtil.setText(trimBlankRowCheckBox, "quickNote.replace.trimBlankRow");
        I18nUiUtil.setText(clearTabTCheckBox, "quickNote.replace.clearTab");
        I18nUiUtil.setText(scientificToNormalCheckBox, "quickNote.replace.scientificToNormal");
        I18nUiUtil.setText(normalToScientificCheckBox, "quickNote.replace.normalToScientific");
        I18nUiUtil.setText(toThousandthCheckBox, "quickNote.replace.toThousandth");
        I18nUiUtil.setText(toNormalNumCheckBox, "quickNote.replace.toNormalNum");
        I18nUiUtil.setText(underlineToHumpCheckBox, "quickNote.replace.underlineToHump");
        I18nUiUtil.setText(humpToUnderlineCheckBox, "quickNote.replace.humpToUnderline");
        I18nUiUtil.setText(enterToCommaCheckBox, "quickNote.replace.enterToComma");
        I18nUiUtil.setText(enterToCommaSingleQuotesCheckBox, "quickNote.replace.enterToCommaSingle");
        I18nUiUtil.setText(enterToCommaDoubleQuotesCheckBox, "quickNote.replace.enterToCommaDouble");
        I18nUiUtil.setText(commaToEnterCheckBox, "quickNote.replace.commaToEnter");
        I18nUiUtil.setText(tabToEnterCheckBox, "quickNote.replace.tabToEnter");
        I18nUiUtil.setText(clearEnterCheckBox, "quickNote.replace.clearEnter");
        I18nUiUtil.setText(uperToLowerCheckBox, "quickNote.replace.upperToLower");
        I18nUiUtil.setText(lowerToUperCheckBox, "quickNote.replace.lowerToUpper");
        I18nUiUtil.setText(deduplicationByLineCheckBox, "quickNote.replace.dedupByLine");
        I18nUiUtil.setText(deduplicationByLineCntCheckBox, "quickNote.replace.dedupByLineCount");
        I18nUiUtil.setText(escapeCheckBox, "quickNote.replace.escape");
        I18nUiUtil.setText(unescapeCheckBox, "quickNote.replace.unescape");
        I18nUiUtil.setText(commaSingleQuotesToEnterCheckBox, "quickNote.replace.commaSingleToEnter");
        I18nUiUtil.setText(commaDoubleQuotesToEnterCheckBox, "quickNote.replace.commaDoubleToEnter");
        I18nUiUtil.setText(reverseByRowCheckBox, "quickNote.replace.reverseByRow");
        I18nUiUtil.setText(sortFromAToZByRowCheckBox, "quickNote.replace.sortAToZ");
        I18nUiUtil.setText(sortFromZToAByRowCheckBox, "quickNote.replace.sortZToA");
        I18nUiUtil.setText(sortByPinyinCheckBox, "quickNote.replace.sortByPinyin");
        I18nUiUtil.setText(startQuickReplaceButton, "common.start");
    }

    private static void applyI18nStatic() {
        if (quickNoteForm != null) {
            quickNoteForm.applyI18n();
        }
    }

    public static void updateInsertImageButtonVisibility() {
        JButton insertImageButton = getInstance().getInsertImageButton();
        if (insertImageButton == null) {
            return;
        }
        Object selectedSyntax = getInstance().getSyntaxComboBox().getSelectedItem();
        boolean markdown = selectedSyntax != null
                && SyntaxConstants.SYNTAX_STYLE_MARKDOWN.substring(5).equals(selectedSyntax.toString());
        insertImageButton.setVisible(markdown);
        getInstance().getLeftMenuToolBar().revalidate();
        getInstance().getLeftMenuToolBar().repaint();
    }

    private static void initUi() {
        quickNoteForm.getSearchTextField().putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索"); // updated in applyI18n
        quickNoteForm.getSearchTextField().putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSearchIcon());
        quickNoteForm.getSearchContentCheckBox().setToolTipText("包含内容");
        quickNoteForm.getSearchTextField().putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, quickNoteForm.getSearchContentCheckBox());

        quickNoteForm.getAddButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        quickNoteForm.getSaveButton().setIcon(new FlatSVGIcon("icon/save.svg"));
        quickNoteForm.getFindButton().setIcon(new FlatSVGIcon("icon/find.svg"));
        quickNoteForm.getQuickReplaceButton().setIcon(new FlatSVGIcon("icon/replace.svg"));
        quickNoteForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        quickNoteForm.getExportButton().setIcon(new FlatSVGIcon("icon/export.svg"));
        quickNoteForm.getListItemButton().setIcon(new FlatSVGIcon("icon/list.svg"));
        quickNoteForm.getFormatButton().setIcon(new FlatSVGIcon("icon/json.svg"));
        quickNoteForm.getInfoButton().setIcon(new FlatSVGIcon("icon/info.svg"));
        quickNoteForm.getGitButton().setIcon(new FlatSVGIcon("icon/diff.svg"));
        PanelCloseUtil.installTrailingCloseButton(quickNoteForm.getQuickReplaceCloseButton(),
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        getInstance().getContentSplitPane().setLeftComponent(new JPanel());

        quickNoteForm.getFindReplacePanel().setVisible(false);

        configureQuickReplacePanel();
        quickNoteForm.getQuickReplacePanel().setVisible(false);
        configureSplitPanes();
        quickNoteForm.getQuickNotePanel().setMinimumSize(new Dimension(0, 300));
        quickNoteForm.getNoteList().setFixedCellHeight(20);
        quickNoteForm.getNoteList().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        quickNoteForm.getNoteList().putClientProperty(FlatClientProperties.STYLE,
                "selectionArc: 6; selectionInsets: 0,1,0,1");
        initNoteTree();

        initSyntaxComboBox();

        quickNoteForm.getLeftMenuPanel().removeAll();
        quickNoteForm.getLeftMenuPanel().add(quickNoteForm.getLeftMenuToolBar());

        quickNoteForm.getColorSettingPanel().setVisible(false);

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < COLOR_BUTTONS.length; i++) {
            COLOR_BUTTONS[i] = new JToggleButton(new ListColorIcon(COLOR_KEYS[i]));
            COLOR_BUTTONS[i].setEnabled(true);
            quickNoteForm.getToolBar().add(COLOR_BUTTONS[i]);
            group.add(COLOR_BUTTONS[i]);
        }

        quickNoteForm.getColorSettingPanel().add(quickNoteForm.getToolBar());

        ScrollUtil.smoothPane(quickNoteForm.getQuickReplaceScrollPane());

        quickNoteForm.getQuickNotePanel().updateUI();

    }

    private static void configureQuickReplacePanel() {
        JScrollPane scrollPane = quickNoteForm.getQuickReplaceScrollPane();
        JPanel contentPanel = (JPanel) scrollPane.getViewport().getView();
        contentPanel.remove(quickNoteForm.getStartQuickReplaceButton());
        for (Component component : contentPanel.getComponents()) {
            if (component instanceof Spacer) {
                contentPanel.remove(component);
                break;
            }
        }

        quickNoteForm.quickReplacePanel = new JPanel(new BorderLayout(0, 0));
        quickNoteForm.quickReplacePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel startButtonPanel = new JPanel(new BorderLayout());
        startButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        startButtonPanel.add(quickNoteForm.getStartQuickReplaceButton(), BorderLayout.CENTER);
        quickNoteForm.quickReplacePanel.add(startButtonPanel, BorderLayout.SOUTH);

        quickNoteForm.getContentSplitPane().setRightComponent(quickNoteForm.quickReplacePanel);
    }

    private static void configureSplitPanes() {
        SplitPaneUtil.configureListEditorSplit(quickNoteForm.getSplitPane());
        SplitPaneUtil.configureEditorSecondarySplit(quickNoteForm.getContentSplitPane());
        SplitPaneUtil.relaxHorizontalMinimum(
                quickNoteForm.getQuickNotePanel(),
                quickNoteForm.getRightPanel(),
                quickNoteForm.getControlPanel(),
                quickNoteForm.getSplitPane(),
                quickNoteForm.getContentSplitPane()
        );
    }

    /**
     * codes are copied from FlatLaf/flatlaf-demo/ (https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-demo)
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     * <p>
     * https://www.apache.org/licenses/LICENSE-2.0
     * <p>
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    public static class AccentColorIcon
            extends FlatAbstractIcon {
        private final String colorKey;

        public AccentColorIcon(String colorKey) {
            super(16, 16, null);
            this.colorKey = colorKey;
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g) {
            Color color;
            if (colorKey == null) {
                color = MainWindow.getInstance().getMainPanel().getForeground();
            } else {
                color = UIManager.getColor(colorKey);
            }
            if (color == null)
                color = MainWindow.getInstance().getMainPanel().getForeground();
            else if (!c.isEnabled()) {
                color = FlatLaf.isLafDark()
                        ? ColorFunctions.shade(color, 0.5f)
                        : ColorFunctions.tint(color, 0.6f);
            }

            g.setColor(color);
            g.fillRoundRect(0, 0, width - 0, height - 0, 0, 0);
        }
    }

    /**
     * codes are copied from FlatLaf/flatlaf-demo/ with some change (https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-demo)
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     * <p>
     * https://www.apache.org/licenses/LICENSE-2.0
     * <p>
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    public static class ListColorIcon
            extends FlatAbstractIcon {
        private final String colorKey;

        public ListColorIcon(String colorKey) {
            super(32, 32, null);
            this.colorKey = colorKey;
        }

        public ListColorIcon(String colorKey, int width, int height) {
            super(width, height, null);
            this.colorKey = colorKey;
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g) {
            Color color;
            if (colorKey == null) {
                color = MainWindow.getInstance().getMainPanel().getForeground();
            } else {
                color = UIManager.getColor(colorKey);
            }
            if (color == null)
                color = MainWindow.getInstance().getMainPanel().getForeground();
            else if (!c.isEnabled()) {
                color = FlatLaf.isLafDark()
                        ? ColorFunctions.shade(color, 0.5f)
                        : ColorFunctions.tint(color, 0.6f);
            }

            g.setColor(color);
            g.fillRoundRect(1, 1, width - 2, height - 2, 5, 5);
        }
    }

    private static void initSyntaxComboBox() {
        quickNoteForm.getSyntaxComboBox().removeAllItems();
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_NONE.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_MARKDOWN.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_JAVA.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_C.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_CSHARP.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_PYTHON.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_GO.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_KOTLIN.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_SCALA.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_GROOVY.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_RUBY.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_HTML.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_SQL.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_JSON.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_XML.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_YAML.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_JSP.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_CSS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_LESS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_PHP.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_6502.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_BBCODE.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_CLOJURE.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_CSV.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_D.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_DART.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_DELPHI.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_DTD.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_FORTRAN.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_HOSTS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_HTACCESS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_INI.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_LATEX.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_LISP.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_LUA.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_MAKEFILE.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_MXML.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_NSIS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_PERL.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_SAS.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_TCL.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC.substring(5));
        quickNoteForm.getSyntaxComboBox().addItem(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH.substring(5));
    }

    public static Color resolveNoteColor(String colorKey) {
        return QuickNoteEditorPanel.resolveAccentColor(colorKey);
    }

    public static void applyEditorOutline(QuickNoteEditorPanel editorPanel, String colorKey) {
        if (editorPanel == null) {
            return;
        }
        editorPanel.applyAccentColor(QuickNoteEditorPanel.resolveAccentColor(colorKey));
    }

    public static void applyCurrentEditorOutline(String colorKey) {
        if (quickNoteRSyntaxTextViewerManager == null || QuickNoteListener.selectedPath == null) {
            return;
        }
        applyEditorOutline(quickNoteRSyntaxTextViewerManager.getEditorPanel(QuickNoteListener.selectedPath), colorKey);
    }

    private static void initNoteTree() {
        quickNoteForm.noteTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        quickNoteForm.noteTree.setRootVisible(false);
        quickNoteForm.noteTree.setShowsRootHandles(true);
        quickNoteForm.noteTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        quickNoteForm.noteTree.setCellRenderer(new QuickNoteTreeCellRenderer());
        quickNoteForm.noteTree.setVisibleRowCount(20);
        quickNoteForm.noteTree.putClientProperty(FlatClientProperties.STYLE, "selectionArc: 6; selectionInsets: 0,1,0,1");
        QuickNoteTreeDragDrop.install(quickNoteForm.noteTree);

        Component parent = quickNoteForm.getNoteList();
        while (parent != null) {
            if (parent instanceof JScrollPane scrollPane) {
                scrollPane.setViewportView(quickNoteForm.noteTree);
                break;
            }
            parent = parent.getParent();
        }
        quickNoteForm.getNoteList().setVisible(false);
    }

    public static void initNoteList() {
        QuickNoteVaultUtil.ensureVaultReady();
        if (quickNoteForm.getNoteTree() == null) {
            log.error("Quick note tree is not initialized");
            return;
        }

        String titleFilterKeyWord = quickNoteForm.getSearchTextField().getText();
        boolean searchContent = quickNoteForm.getSearchContentCheckBox().isSelected();

        List<TQuickNote> quickNoteList;
        if (searchContent && StringUtils.isNotBlank(titleFilterKeyWord)) {
            quickNoteList = QuickNoteVaultUtil.listByFilter(titleFilterKeyWord, true);
        } else {
            quickNoteList = QuickNoteVaultUtil.listByFilter(titleFilterKeyWord, false);
        }

        List<String> folders = searchContent && StringUtils.isNotBlank(titleFilterKeyWord)
                ? List.of()
                : QuickNoteVaultUtil.listFolders();
        quickNoteForm.getNoteTree().setModel(QuickNoteTreeUtil.buildTreeModel(quickNoteList, folders));
        expandAllTreeRows(quickNoteForm.getNoteTree());

        if (!quickNoteList.isEmpty()) {
            QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
            try {
                String preservePath = QuickNoteListener.selectedPath;
                TQuickNote selectedNote = null;
                if (StringUtils.isNotEmpty(preservePath)) {
                    selectedNote = quickNoteList.stream()
                            .filter(note -> preservePath.equals(note.getRelativePath()))
                            .findFirst()
                            .orElse(null);
                }
                if (selectedNote == null) {
                    selectedNote = quickNoteList.get(0);
                }

                selectNoteInTree(selectedNote.getRelativePath());
                showNote(selectedNote);
            } catch (Exception e1) {
                log.error(e1.toString());
            } finally {
                QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
            }
        } else {
            getInstance().getContentSplitPane().setLeftComponent(new JPanel());
            QuickNoteListener.selectedPath = null;
            QuickNoteListener.selectedName = null;
            updateInsertImageButtonVisibility();
        }
        updateGitButtonStatus();
    }

    public static void updateGitButtonStatus() {
        if (quickNoteForm == null || quickNoteForm.gitButton == null) {
            return;
        }
        SwingWorker<QuickNoteGitStatus, Void> worker = new SwingWorker<>() {
            @Override
            protected QuickNoteGitStatus doInBackground() {
                return QuickNoteGitUtil.getStatus(QuickNoteVaultUtil.getVaultDir());
            }

            @Override
            protected void done() {
                try {
                    QuickNoteGitStatus status = get();
                    JButton button = quickNoteForm.gitButton;
                    if (!status.isGitRepo()) {
                        button.setText("");
                        I18nUiUtil.setToolTip(button, "quickNote.tooltip.git");
                        return;
                    }
                    int pending = status.getChangedCount() + status.getConflictCount();
                    button.setText(pending > 0 ? String.valueOf(pending) : "");
                    if (status.hasPendingChanges() || status.getAhead() > 0 || status.getBehind() > 0
                            || StringUtils.isNotBlank(status.getBranch())) {
                        button.setToolTipText(I18n.format("quickNote.tooltip.gitStatus",
                                StringUtils.defaultIfBlank(status.getBranch(), "-"),
                                status.getChangedCount(),
                                status.getConflictCount(),
                                status.getAhead(),
                                status.getBehind()));
                    } else {
                        I18nUiUtil.setToolTip(button, "quickNote.tooltip.git");
                    }
                } catch (Exception ignored) {
                    // ignore background status read failures
                }
            }
        };
        worker.execute();
    }

    private static void expandAllTreeRows(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    public static void selectNoteInTree(String relativePath) {
        if (quickNoteForm.getNoteTree() == null || StringUtils.isBlank(relativePath)) {
            return;
        }
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) quickNoteForm.getNoteTree().getModel().getRoot();
        DefaultMutableTreeNode node = QuickNoteTreeUtil.findNodeByPath(root, relativePath);
        if (node != null) {
            TreePath treePath = new TreePath(node.getPath());
            quickNoteForm.getNoteTree().setSelectionPath(treePath);
            quickNoteForm.getNoteTree().scrollPathToVisible(treePath);
        }
    }

    public static void showNote(TQuickNote tQuickNote) {
        if (tQuickNote == null || StringUtils.isBlank(tQuickNote.getRelativePath())) {
            return;
        }
        String path = tQuickNote.getRelativePath();
        QuickNoteListener.selectedPath = path;
        QuickNoteListener.selectedName = tQuickNote.getName();

        quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(path);
        QuickNoteEditorPanel editorPanel = quickNoteRSyntaxTextViewerManager.getEditorPanel(path);
        getInstance().getContentSplitPane().setLeftComponent(editorPanel);

        String color = tQuickNote.getColor();
        if (StringUtils.isEmpty(color)) {
            color = COLOR_KEYS[0];
        }
        quickNoteForm.getColorButton().setIcon(new QuickNoteForm.ListColorIcon(color, 18, 18));
        quickNoteForm.getColorButton().setSelected(true);
        if (StringUtils.isNotEmpty(tQuickNote.getSyntax()) && tQuickNote.getSyntax().length() > 5) {
            quickNoteForm.getSyntaxComboBox().setSelectedItem(tQuickNote.getSyntax().substring(5));
        }
        updateInsertImageButtonVisibility();
        quickNoteForm.getFontNameComboBox().setSelectedItem(tQuickNote.getFontName());
        quickNoteForm.getFontSizeComboBox().setSelectedItem(String.valueOf(tQuickNote.getFontSize()));

        quickNoteForm.getFindReplacePanel().removeAll();
        quickNoteForm.getFindReplacePanel().setVisible(false);

        int colorIndex = ArrayUtil.indexOf(QuickNoteForm.COLOR_KEYS, color);
        if (colorIndex >= 0 && colorIndex < QuickNoteForm.COLOR_BUTTONS.length) {
            QuickNoteForm.COLOR_BUTTONS[colorIndex].setSelected(true);
        }
        quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setLineWrap("1".equals(tQuickNote.getLineWrap()));
        quickNoteForm.getWrapButton().setSelected("1".equals(tQuickNote.getLineWrap()));
        applyEditorOutline(editorPanel, color);
        QuickNoteConflictHighlightUtil.refresh(quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea());
    }

    public static TQuickNote getSelectedTreeNote() {
        if (quickNoteForm == null || quickNoteForm.getNoteTree() == null) {
            return null;
        }
        TreePath selectionPath = quickNoteForm.getNoteTree().getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        return QuickNoteTreeUtil.selectedNote(node);
    }

    public static String getSelectedFolderPath() {
        if (quickNoteForm == null || quickNoteForm.getNoteTree() == null) {
            return "";
        }
        TreePath selectionPath = quickNoteForm.getNoteTree().getSelectionPath();
        if (selectionPath == null) {
            return "";
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        return QuickNoteTreeUtil.selectedFolderPath(node);
    }

    public static List<TQuickNote> getSelectedNotes() {
        if (quickNoteForm == null || quickNoteForm.getNoteTree() == null) {
            return List.of();
        }
        TreePath[] selectionPaths = quickNoteForm.getNoteTree().getSelectionPaths();
        if (selectionPaths == null) {
            return List.of();
        }
        List<TQuickNote> notes = new ArrayList<>();
        for (TreePath selectionPath : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            TQuickNote note = QuickNoteTreeUtil.selectedNote(node);
            if (note != null) {
                notes.add(note);
            }
        }
        return notes;
    }

    private static void initTextAreaFont() {
        String fontName = App.config.getQuickNoteFontName();
        int fontSize = App.config.getQuickNoteFontSize();
        if (fontSize == 0) {
            fontSize = quickNoteForm.getNoteList().getFont().getSize() + 2;
        }

        getSysFontList();

        quickNoteForm.getFontNameComboBox().setSelectedItem(fontName);
        quickNoteForm.getFontSizeComboBox().setSelectedItem(String.valueOf(fontSize));
    }

    /**
     * 获取系统字体列表
     */
    private static void getSysFontList() {
        quickNoteForm.getFontNameComboBox().removeAllItems();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        for (String font : fonts) {
            if (StringUtils.isNotBlank(font)) {
                quickNoteForm.getFontNameComboBox().addItem(font);
            }
        }
        quickNoteForm.getFontNameComboBox().addItem(FlatJetBrainsMonoFont.FAMILY);
//        quickNoteForm.getFontNameComboBox().addItem(FlatInterFont.FAMILY);
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
        quickNotePanel = new JPanel();
        quickNotePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        quickNotePanel.setMinimumSize(new Dimension(400, 300));
        quickNotePanel.setPreferredSize(new Dimension(400, 300));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(94);
        splitPane.setDividerSize(10);
        quickNotePanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
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
        controlPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        rightPanel.add(controlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.add(menuPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        menuPanel.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        actionToolBarPanel = new JPanel();
        actionToolBarPanel.setLayout(new BorderLayout(0, 0));
        menuPanel.add(actionToolBarPanel, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        leftMenuPanel = new JPanel();
        leftMenuPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftMenuPanel.setAlignmentX(0.0f);
        menuPanel.add(leftMenuPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        syntaxComboBox = new JComboBox();
        leftMenuPanel.add(syntaxComboBox);
        fontNameComboBox = new JComboBox();
        fontNameComboBox.setToolTipText("设置字体");
        leftMenuPanel.add(fontNameComboBox);
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
        leftMenuPanel.add(fontSizeComboBox);
        listItemButton = new JButton();
        listItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/listFiles_dark.png")));
        listItemButton.setText("");
        listItemButton.setToolTipText("显示/隐藏列表");
        menuPanel.add(listItemButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        colorSettingPanel = new JPanel();
        colorSettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        colorSettingPanel.setAlignmentX(0.0f);
        menuPanel.add(colorSettingPanel, new GridConstraints(1, 1, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        indicatorPanel = new JPanel();
        indicatorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        indicatorPanel.setAlignmentX(0.0f);
        indicatorPanel.setAlignmentY(0.0f);
        indicatorPanel.setFocusable(false);
        indicatorPanel.setRequestFocusEnabled(false);
        indicatorPanel.setVisible(true);
        menuPanel.add(indicatorPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        indicatorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        tipsLabel = new JLabel();
        tipsLabel.setFocusable(false);
        tipsLabel.setRequestFocusEnabled(false);
        tipsLabel.setText(" ");
        indicatorPanel.add(tipsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findReplacePanel = new JPanel();
        findReplacePanel.setLayout(new BorderLayout(0, 0));
        findReplacePanel.setVisible(true);
        controlPanel.add(findReplacePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, true));
        contentSplitPane = new JSplitPane();
        contentSplitPane.setContinuousLayout(true);
        contentSplitPane.setDividerLocation(200);
        contentSplitPane.setDoubleBuffered(true);
        panel2.add(contentSplitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        quickReplaceScrollPane = new JScrollPane();
        quickReplaceScrollPane.setMaximumSize(new Dimension(-1, -1));
        quickReplaceScrollPane.setMinimumSize(new Dimension(-1, -1));
        contentSplitPane.setRightComponent(quickReplaceScrollPane);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(37, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel3.setMaximumSize(new Dimension(-1, -1));
        panel3.setMinimumSize(new Dimension(-1, -1));
        quickReplaceScrollPane.setViewportView(panel3);
        trimBlankCheckBox = new JCheckBox();
        trimBlankCheckBox.setText("去掉空格");
        panel3.add(trimBlankCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        trimBlankRowCheckBox = new JCheckBox();
        trimBlankRowCheckBox.setText("去掉空行");
        panel3.add(trimBlankRowCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(36, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        clearTabTCheckBox = new JCheckBox();
        clearTabTCheckBox.setText("去掉Tab(\\t)");
        panel3.add(clearTabTCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scientificToNormalCheckBox = new JCheckBox();
        scientificToNormalCheckBox.setText("科学计数法 -> 普通数字");
        panel3.add(scientificToNormalCheckBox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        normalToScientificCheckBox = new JCheckBox();
        normalToScientificCheckBox.setText("普通数字 -> 科学计数法");
        panel3.add(normalToScientificCheckBox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        toThousandthCheckBox = new JCheckBox();
        toThousandthCheckBox.setText("普通数字 -> 千分位");
        panel3.add(toThousandthCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        toNormalNumCheckBox = new JCheckBox();
        toNormalNumCheckBox.setText("千分位 -> 普通数字");
        panel3.add(toNormalNumCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        underlineToHumpCheckBox = new JCheckBox();
        underlineToHumpCheckBox.setText("下划线规则 -> 驼峰规则");
        panel3.add(underlineToHumpCheckBox, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        humpToUnderlineCheckBox = new JCheckBox();
        humpToUnderlineCheckBox.setText("驼峰规则 -> 下划线规则");
        panel3.add(humpToUnderlineCheckBox, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enterToCommaCheckBox = new JCheckBox();
        enterToCommaCheckBox.setText("换行 -> ,");
        panel3.add(enterToCommaCheckBox, new GridConstraints(16, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enterToCommaSingleQuotesCheckBox = new JCheckBox();
        enterToCommaSingleQuotesCheckBox.setText("换行 -> ','");
        panel3.add(enterToCommaSingleQuotesCheckBox, new GridConstraints(17, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enterToCommaDoubleQuotesCheckBox = new JCheckBox();
        enterToCommaDoubleQuotesCheckBox.setText("换行 -> \",\"");
        panel3.add(enterToCommaDoubleQuotesCheckBox, new GridConstraints(18, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        commaToEnterCheckBox = new JCheckBox();
        commaToEnterCheckBox.setText(", -> 换行");
        panel3.add(commaToEnterCheckBox, new GridConstraints(20, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabToEnterCheckBox = new JCheckBox();
        tabToEnterCheckBox.setText("Tab(\\t) -> 换行");
        panel3.add(tabToEnterCheckBox, new GridConstraints(23, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startQuickReplaceButton = new JButton();
        startQuickReplaceButton.setText("开始");
        panel3.add(startQuickReplaceButton, new GridConstraints(35, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(separator1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel3.add(separator2, new GridConstraints(14, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel3.add(separator3, new GridConstraints(19, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        clearEnterCheckBox = new JCheckBox();
        clearEnterCheckBox.setText("去掉换行");
        panel3.add(clearEnterCheckBox, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        quickReplaceCloseButton = new JButton();
        quickReplaceCloseButton.setText("");
        quickReplaceCloseButton.setToolTipText("关闭");
        panel3.add(quickReplaceCloseButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel3.add(separator4, new GridConstraints(24, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        panel3.add(separator5, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        uperToLowerCheckBox = new JCheckBox();
        uperToLowerCheckBox.setText("大写 -> 小写");
        panel3.add(uperToLowerCheckBox, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lowerToUperCheckBox = new JCheckBox();
        lowerToUperCheckBox.setText("小写 -> 大写");
        panel3.add(lowerToUperCheckBox, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deduplicationByLineCheckBox = new JCheckBox();
        deduplicationByLineCheckBox.setText("按行去重");
        panel3.add(deduplicationByLineCheckBox, new GridConstraints(25, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deduplicationByLineCntCheckBox = new JCheckBox();
        deduplicationByLineCntCheckBox.setText("按行去重并统计出现次数");
        panel3.add(deduplicationByLineCntCheckBox, new GridConstraints(26, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator6 = new JSeparator();
        panel3.add(separator6, new GridConstraints(27, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        escapeCheckBox = new JCheckBox();
        escapeCheckBox.setText("转义");
        panel3.add(escapeCheckBox, new GridConstraints(28, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        unescapeCheckBox = new JCheckBox();
        unescapeCheckBox.setText("反转义");
        panel3.add(unescapeCheckBox, new GridConstraints(29, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        commaSingleQuotesToEnterCheckBox = new JCheckBox();
        commaSingleQuotesToEnterCheckBox.setText("',' -> 换行");
        panel3.add(commaSingleQuotesToEnterCheckBox, new GridConstraints(21, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        commaDoubleQuotesToEnterCheckBox = new JCheckBox();
        commaDoubleQuotesToEnterCheckBox.setText("\",\" -> 换行");
        panel3.add(commaDoubleQuotesToEnterCheckBox, new GridConstraints(22, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator7 = new JSeparator();
        panel3.add(separator7, new GridConstraints(30, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        reverseByRowCheckBox = new JCheckBox();
        reverseByRowCheckBox.setText("按行倒序");
        panel3.add(reverseByRowCheckBox, new GridConstraints(31, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sortFromAToZByRowCheckBox = new JCheckBox();
        sortFromAToZByRowCheckBox.setText("按行A->Z排序");
        panel3.add(sortFromAToZByRowCheckBox, new GridConstraints(32, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sortFromZToAByRowCheckBox = new JCheckBox();
        sortFromZToAByRowCheckBox.setText("按行Z->A排序");
        panel3.add(sortFromZToAByRowCheckBox, new GridConstraints(33, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sortByPinyinCheckBox = new JCheckBox();
        sortByPinyinCheckBox.setText("按拼音排序");
        panel3.add(sortByPinyinCheckBox, new GridConstraints(34, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return quickNotePanel;
    }

}
