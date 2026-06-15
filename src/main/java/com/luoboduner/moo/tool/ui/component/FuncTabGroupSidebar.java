package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.luoboduner.moo.tool.bean.FuncGroup;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.FuncTabCatalog.BuiltinGroup;
import com.luoboduner.moo.tool.ui.FuncTabCatalog.FuncTab;
import com.luoboduner.moo.tool.ui.dialog.FuncGroupManageDialog;
import com.luoboduner.moo.tool.util.FuncGroupUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 主界面左侧分组功能导航。
 */
public class FuncTabGroupSidebar extends JPanel {

    private static final int SIDEBAR_WIDTH = 210;

    private static boolean i18nRegistered;

    private static final CopyOnWriteArrayList<FuncTabGroupSidebar> INSTANCES = new CopyOnWriteArrayList<>();

    private final JTabbedPane tabbedPane;
    private final JTextField searchField = new JTextField();
    private final JButton manageButton = new JButton();
    private final JPanel groupsPanel = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane(groupsPanel);
    private final Map<Integer, JButton> tabButtons = new HashMap<>();

    public FuncTabGroupSidebar(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
        INSTANCES.add(this);
        initUi();
        addListeners();
        applyI18n();
        refresh();
        if (!i18nRegistered) {
            I18nUiUtil.register(FuncTabGroupSidebar::refreshAllI18n);
            i18nRegistered = true;
        }
    }

    public static void refreshAllI18n() {
        for (FuncTabGroupSidebar sidebar : INSTANCES) {
            sidebar.applyI18n();
            sidebar.refresh();
        }
    }

    private void initUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));

        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        manageButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        JPanel topPanel = new JPanel(new BorderLayout(4, 0));
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(manageButton, BorderLayout.EAST);

        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        manageButton.addActionListener(e -> {
            FuncGroupManageDialog.showDialog(SwingUtilities.getWindowAncestor(this));
            refresh();
        });
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = tabbedPane.getSelectedIndex();
                updateSelection(index);
                if (index > 0) {
                    // 等 TabListener 完成 recordRecent 后再刷新最近使用列表
                    SwingUtilities.invokeLater(FuncTabGroupSidebar.this::refreshPreservingScroll);
                }
            }
        });
    }

    private void refreshPreservingScroll() {
        int scroll = scrollPane.getVerticalScrollBar().getValue();
        refresh();
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setValue(Math.min(scroll, verticalBar.getMaximum()));
    }

    private void applyI18n() {
        I18nUiUtil.setPlaceholder(searchField, "funcGroup.searchPlaceholder");
        manageButton.setText(I18n.get("funcGroup.manage"));
        manageButton.setToolTipText(I18n.get("funcGroup.manage"));
    }

    public void refresh() {
        tabButtons.clear();
        groupsPanel.removeAll();
        String query = StringUtils.trimToEmpty(searchField.getText());

        appendTabButton(groupsPanel, FuncTabCatalog.byIndex(0).orElse(null), query, "funcGroup.home");
        appendRecentGroup(query);
        appendCustomGroups(query);
        appendBuiltinGroups(query);

        groupsPanel.add(Box.createVerticalGlue());
        groupsPanel.revalidate();
        groupsPanel.repaint();
        updateSelection(tabbedPane.getSelectedIndex());
    }

    private void appendRecentGroup(String query) {
        List<FuncTab> tabs = new ArrayList<>();
        for (FuncTab tab : FuncGroupUtil.getRecentTabs()) {
            if (matchesQuery(tab, query)) {
                tabs.add(tab);
            }
        }
        appendGroupSection(I18n.get("funcGroup.recent"), tabs);
    }

    private void appendCustomGroups(String query) {
        for (FuncGroup group : FuncGroupUtil.getCustomGroups()) {
            List<FuncTab> tabs = new ArrayList<>();
            String title = StringUtils.defaultIfBlank(group.getName(), I18n.get("funcGroup.unnamed"));
            boolean groupMatched = StringUtils.isNotBlank(query)
                    && title.toLowerCase().contains(query.toLowerCase());
            for (FuncTab tab : FuncTabCatalog.tabsInGroup(group.getFuncIds())) {
                if (groupMatched || matchesQuery(tab, query)) {
                    tabs.add(tab);
                }
            }
            appendGroupSection(title, tabs);
        }
    }

    private void appendBuiltinGroups(String query) {
        groupsPanel.add(createSectionLabel("funcGroup.allGroups"));
        for (BuiltinGroup group : FuncTabCatalog.builtinGroups()) {
            List<FuncTab> tabs = new ArrayList<>();
            boolean groupMatched = StringUtils.isNotBlank(query)
                    && FuncTabCatalog.matchesGroupSearch(group, query);
            for (FuncTab tab : FuncTabCatalog.tabsInGroup(group.funcIds())) {
                if (groupMatched || matchesQuery(tab, query)) {
                    tabs.add(tab);
                }
            }
            appendGroupSection(I18n.get(group.titleKey()), tabs);
        }
    }

    private void appendGroupSection(String title, List<FuncTab> tabs) {
        if (tabs.isEmpty()) {
            return;
        }
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 0),
                BorderFactory.createTitledBorder(title)));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        for (FuncTab tab : tabs) {
            appendTabButton(section, tab, null, null);
        }
        groupsPanel.add(section);
    }

    private void appendTabButton(JPanel parent, FuncTab tab, String query, String titleOverrideKey) {
        if (tab == null) {
            return;
        }
        if (query != null && !matchesQuery(tab, query)
                && (titleOverrideKey == null || !I18n.get(titleOverrideKey).toLowerCase().contains(query.toLowerCase()))) {
            return;
        }
        String title = titleOverrideKey != null ? I18n.get(titleOverrideKey) : I18n.get(tab.titleKey());
        JButton button = new JButton(title, FuncTabCatalog.smallIcon(tab));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusable(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addActionListener(e -> FuncTabCatalog.switchTo(tab));
        parent.add(button);
        tabButtons.put(tab.index(), button);
    }

    private JLabel createSectionLabel(String key) {
        JLabel label = new JLabel(I18n.get(key));
        label.setBorder(BorderFactory.createEmptyBorder(6, 2, 4, 2));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private boolean matchesQuery(FuncTab tab, String query) {
        return StringUtils.isBlank(query) || FuncTabCatalog.matchesSearch(tab, query);
    }

    private void updateSelection(int selectedIndex) {
        Color selectedBg = UIManager.getColor("List.selectionBackground");
        Color selectedFg = UIManager.getColor("List.selectionForeground");
        for (Map.Entry<Integer, JButton> entry : tabButtons.entrySet()) {
            JButton button = entry.getValue();
            boolean selected = entry.getKey() == selectedIndex;
            if (selected) {
                button.setBackground(selectedBg);
                button.setForeground(selectedFg);
                button.setOpaque(true);
            } else {
                button.setBackground(null);
                button.setForeground(UIManager.getColor("Button.foreground"));
                button.setOpaque(false);
            }
        }
    }
}
