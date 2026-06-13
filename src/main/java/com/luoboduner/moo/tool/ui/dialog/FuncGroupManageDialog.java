package com.luoboduner.moo.tool.ui.dialog;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.bean.FuncGroup;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.FuncTabCatalog.FuncTab;
import com.luoboduner.moo.tool.util.FuncGroupUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义功能分组管理。
 */
public class FuncGroupManageDialog extends JDialog {

    private final DefaultListModel<String> groupListModel = new DefaultListModel<>();
    private final JList<String> groupList = new JList<>(groupListModel);
    private final JTextField nameField = new JTextField();
    private final JPanel funcCheckPanel = new JPanel();
    private final JButton newButton = new JButton();
    private final JButton deleteButton = new JButton();
    private final JButton saveButton = new JButton();
    private final JButton closeButton = new JButton();

    private final Map<String, JCheckBox> funcCheckBoxes = new LinkedHashMap<>();
    private List<FuncGroup> groups = new ArrayList<>();
    private int selectedIndex = -1;

    public FuncGroupManageDialog(Window owner) {
        super(owner, ModalityType.APPLICATION_MODAL);
        initUi();
        addListeners();
        applyI18n();
        reloadGroups();
    }

    public static void showDialog(Window owner) {
        FuncGroupManageDialog dialog = new FuncGroupManageDialog(owner);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void initUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupScroll = new JScrollPane(groupList);
        groupScroll.setPreferredSize(new Dimension(160, 360));

        funcCheckPanel.setLayout(new BoxLayout(funcCheckPanel, BoxLayout.Y_AXIS));
        for (FuncTab tab : FuncTabCatalog.toolTabs()) {
            JCheckBox checkBox = new JCheckBox(I18n.get(tab.titleKey()), false);
            funcCheckBoxes.put(tab.id(), checkBox);
            funcCheckPanel.add(checkBox);
        }
        JScrollPane funcScroll = new JScrollPane(funcCheckPanel);
        funcScroll.setPreferredSize(new Dimension(300, 360));

        JPanel editorPanel = new JPanel(new GridLayoutManager(2, 1, new Insets(0, 8, 0, 0), -1, 8));
        editorPanel.add(nameField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editorPanel.add(funcScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

        JPanel centerPanel = new JPanel(new BorderLayout(8, 0));
        centerPanel.add(groupScroll, BorderLayout.WEST);
        centerPanel.add(editorPanel, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.add(newButton);
        buttonBar.add(deleteButton);
        buttonBar.add(saveButton);
        buttonBar.add(closeButton);

        root.add(centerPanel, BorderLayout.CENTER);
        root.add(buttonBar, BorderLayout.SOUTH);
        setContentPane(root);
        setPreferredSize(new Dimension(560, 460));
        pack();
    }

    private void addListeners() {
        groupList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            selectedIndex = groupList.getSelectedIndex();
            loadSelectedGroup();
        });
        newButton.addActionListener(e -> createGroup());
        deleteButton.addActionListener(e -> deleteGroup());
        saveButton.addActionListener(e -> saveCurrentGroup());
        closeButton.addActionListener(e -> dispose());
    }

    private void applyI18n() {
        setTitle(I18n.get("funcGroup.manage.title"));
        nameField.putClientProperty("JTextField.placeholderText", I18n.get("funcGroup.groupName"));
        newButton.setText(I18n.get("funcGroup.newGroup"));
        deleteButton.setText(I18n.get("funcGroup.deleteGroup"));
        saveButton.setText(I18n.get("common.save"));
        closeButton.setText(I18n.get("common.close"));
    }

    private void reloadGroups() {
        groups = new ArrayList<>(FuncGroupUtil.getCustomGroups());
        groupListModel.clear();
        for (FuncGroup group : groups) {
            groupListModel.addElement(StringUtils.defaultIfBlank(group.getName(), I18n.get("funcGroup.unnamed")));
        }
        if (!groups.isEmpty()) {
            groupList.setSelectedIndex(0);
        } else {
            clearEditor();
        }
    }

    private void loadSelectedGroup() {
        if (selectedIndex < 0 || selectedIndex >= groups.size()) {
            clearEditor();
            return;
        }
        FuncGroup group = groups.get(selectedIndex);
        nameField.setText(group.getName());
        for (Map.Entry<String, JCheckBox> entry : funcCheckBoxes.entrySet()) {
            entry.getValue().setSelected(group.getFuncIds().contains(entry.getKey()));
        }
    }

    private void clearEditor() {
        nameField.setText("");
        funcCheckBoxes.values().forEach(box -> box.setSelected(false));
    }

    private void createGroup() {
        String name = MsgUtil.input(this, "funcGroup.groupNamePrompt", I18n.get("funcGroup.newGroupDefault"));
        if (StringUtils.isBlank(name)) {
            return;
        }
        FuncGroup group = new FuncGroup(name.trim(), new ArrayList<>());
        groups.add(group);
        FuncGroupUtil.saveCustomGroups(groups);
        reloadGroups();
        groupList.setSelectedIndex(groups.size() - 1);
    }

    private void deleteGroup() {
        if (selectedIndex < 0 || selectedIndex >= groups.size()) {
            return;
        }
        if (MsgUtil.confirm(this, "funcGroup.deleteConfirm") != JOptionPane.YES_OPTION) {
            return;
        }
        groups.remove(selectedIndex);
        FuncGroupUtil.saveCustomGroups(groups);
        reloadGroups();
    }

    private void saveCurrentGroup() {
        if (selectedIndex < 0 || selectedIndex >= groups.size()) {
            MsgUtil.warn(this, "funcGroup.selectGroupFirst");
            return;
        }
        String name = nameField.getText();
        if (StringUtils.isBlank(name)) {
            MsgUtil.warn(this, "funcGroup.nameRequired");
            return;
        }
        List<String> selectedIds = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> entry : funcCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedIds.add(entry.getKey());
            }
        }
        if (selectedIds.isEmpty()) {
            MsgUtil.warn(this, "funcGroup.funcRequired");
            return;
        }
        FuncGroup group = groups.get(selectedIndex);
        group.setName(name.trim());
        group.setFuncIds(selectedIds);
        FuncGroupUtil.saveCustomGroups(groups);
        reloadGroups();
        groupList.setSelectedIndex(selectedIndex);
        MsgUtil.success(this, "common.saveSuccess");
    }
}
