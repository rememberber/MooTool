package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.bean.FuncGroup;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.FuncTabCatalog.BuiltinGroup;
import com.luoboduner.moo.tool.ui.FuncTabCatalog.FuncTab;
import com.luoboduner.moo.tool.util.FuncGroupUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能分组导航：最近使用、自定义分组、内置分组与搜索。
 */
public class FuncNavigatorDialog extends JDialog {

    private static final int GROUP_GRID_COLUMNS = 3;

    private final JTextField searchField = new JTextField();
    private final JPanel contentPanel = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane(contentPanel);
    private final JButton manageButton = new JButton();
    private final JButton closeButton = new JButton();

    private static FuncNavigatorDialog instance;

    public FuncNavigatorDialog() {
        super(App.mainFrame, true);
        initUi();
        addListeners();
        applyI18n();
        refreshContent();
    }

    public static void showDialog() {
        if (instance == null) {
            instance = new FuncNavigatorDialog();
        } else {
            instance.applyI18n();
            instance.refreshContent();
        }
        instance.setLocationRelativeTo(App.mainFrame);
        instance.setVisible(true);
    }

    private void initUi() {
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.add(manageButton);
        buttonBar.add(closeButton);

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        root.add(searchField, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(buttonBar, BorderLayout.SOUTH);
        setContentPane(root);
        setPreferredSize(new Dimension(560, 620));
        pack();
    }

    private void addListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshContent();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshContent();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshContent();
            }
        });
        manageButton.addActionListener(e -> {
            FuncGroupManageDialog.showDialog(this);
            refreshContent();
        });
        closeButton.addActionListener(e -> dispose());
    }

    private void applyI18n() {
        setTitle(I18n.get("funcGroup.navigator.title"));
        I18nUiUtil.setPlaceholder(searchField, "funcGroup.searchPlaceholder");
        manageButton.setText(I18n.get("funcGroup.manage"));
        closeButton.setText(I18n.get("common.close"));
    }

    private void refreshContent() {
        contentPanel.removeAll();
        String query = searchField.getText();
        if (StringUtils.isNotBlank(query)) {
            appendSearchResults(query.trim());
        } else {
            appendRecentGroup();
            appendCustomGroups();
            appendBuiltinGroups();
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void appendSearchResults(String query) {
        boolean matched = appendGroupedTabs(FuncTabCatalog.builtinGroups(), query, true);
        matched |= appendCustomGroupSearch(query);
        if (!matched) {
            contentPanel.add(createEmptyLabel("funcGroup.searchEmpty"));
        }
        contentPanel.add(Box.createVerticalGlue());
    }

    private boolean appendCustomGroupSearch(String query) {
        boolean matched = false;
        for (FuncGroup group : FuncGroupUtil.getCustomGroups()) {
            String title = StringUtils.defaultIfBlank(group.getName(), I18n.get("funcGroup.unnamed"));
            boolean groupMatched = title.toLowerCase().contains(query.toLowerCase());
            List<FuncTab> tabs = new ArrayList<>();
            for (String funcId : group.getFuncIds()) {
                FuncTabCatalog.byId(funcId).ifPresent(tab -> {
                    if (groupMatched || FuncTabCatalog.matchesSearch(tab, query)) {
                        tabs.add(tab);
                    }
                });
            }
            if (!tabs.isEmpty()) {
                contentPanel.add(createGroupPanel(title, tabs));
                matched = true;
            }
        }
        return matched;
    }

    private boolean appendGroupedTabs(List<BuiltinGroup> groups, String query, boolean filter) {
        boolean matched = false;
        for (BuiltinGroup group : groups) {
            List<FuncTab> tabs = new ArrayList<>();
            for (FuncTab tab : FuncTabCatalog.tabsInGroup(group.funcIds())) {
                if (!filter || FuncTabCatalog.matchesSearch(tab, query)
                        || FuncTabCatalog.matchesGroupSearch(group, query)) {
                    tabs.add(tab);
                }
            }
            if (!tabs.isEmpty()) {
                contentPanel.add(createGroupPanel(I18n.get(group.titleKey()), tabs));
                matched = true;
            }
        }
        return matched;
    }

    private void appendRecentGroup() {
        List<FuncTab> recentTabs = FuncGroupUtil.getRecentTabs();
        if (!recentTabs.isEmpty()) {
            contentPanel.add(createGroupPanel(I18n.get("funcGroup.recent"), recentTabs));
        }
    }

    private void appendCustomGroups() {
        List<FuncGroup> groups = FuncGroupUtil.getCustomGroups();
        for (FuncGroup group : groups) {
            List<FuncTab> tabs = FuncTabCatalog.tabsInGroup(group.getFuncIds());
            if (!tabs.isEmpty()) {
                String title = StringUtils.defaultIfBlank(group.getName(), I18n.get("funcGroup.unnamed"));
                contentPanel.add(createGroupPanel(title, tabs));
            }
        }
    }

    private void appendBuiltinGroups() {
        contentPanel.add(createSectionLabel("funcGroup.allGroups"));
        appendGroupedTabs(FuncTabCatalog.builtinGroups(), null, false);
        contentPanel.add(Box.createVerticalGlue());
    }

    private JLabel createSectionLabel(String key) {
        JLabel label = new JLabel(I18n.get(key));
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 6, 2));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private JLabel createEmptyLabel(String key) {
        JLabel label = new JLabel(I18n.get(key));
        label.setBorder(BorderFactory.createEmptyBorder(6, 4, 10, 4));
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        return label;
    }

    private JPanel createGroupPanel(String title, List<FuncTab> tabs) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0),
                BorderFactory.createTitledBorder(title)));
        panel.add(createFuncGrid(tabs), BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel createFuncGrid(List<FuncTab> tabs) {
        int rows = (tabs.size() + GROUP_GRID_COLUMNS - 1) / GROUP_GRID_COLUMNS;
        JPanel grid = new JPanel(new GridLayout(rows, GROUP_GRID_COLUMNS, 8, 8));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        for (FuncTab tab : tabs) {
            grid.add(createFuncButton(tab));
        }
        int placeholders = rows * GROUP_GRID_COLUMNS - tabs.size();
        for (int i = 0; i < placeholders; i++) {
            grid.add(new JPanel());
        }
        return grid;
    }

    private JButton createFuncButton(FuncTab tab) {
        JButton button = new JButton(I18n.get(tab.titleKey()), FuncTabCatalog.smallIcon(tab));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusable(false);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addActionListener(e -> openTab(tab));
        return button;
    }

    private void openTab(FuncTab tab) {
        FuncTabCatalog.switchTo(tab);
        dispose();
    }
}
