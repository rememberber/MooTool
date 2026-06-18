package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.JsonPathTreeUtil;
import com.luoboduner.moo.tool.util.JsonPathUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 可视化选择 JSON Path 的对话框。
 */
public class JsonPathPickerDialog extends JDialog {

    private final String jsonText;
    private final JTree jsonTree;
    private final JTextField jsonPathTextField;
    private final JTextArea valuePreviewTextArea;
    private String selectedJsonPath;

    public JsonPathPickerDialog(String jsonText) {
        super(App.mainFrame, I18n.get("jsonPathPicker.title"));
        this.jsonText = jsonText;
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel tipsLabel = new JLabel("在左侧树中点击节点，即可获取对应的 JSON Path：");
        jsonTree = new JTree(JsonPathTreeUtil.buildTreeModel(jsonText));
        jsonTree.setRootVisible(true);
        jsonTree.setShowsRootHandles(true);
        expandAllNodes(jsonTree, 0, jsonTree.getRowCount());

        jsonPathTextField = new JTextField("$");
        jsonPathTextField.setEditable(true);

        valuePreviewTextArea = new JTextArea(4, 20);
        valuePreviewTextArea.setEditable(false);
        valuePreviewTextArea.setLineWrap(true);
        valuePreviewTextArea.setWrapStyleWord(true);
        valuePreviewTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, valuePreviewTextArea.getFont().getSize()));

        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");

        JPanel contentPane = new JPanel(new BorderLayout(0, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        contentPane.add(tipsLabel, BorderLayout.NORTH);

        JScrollPane treeScrollPane = new JScrollPane(jsonTree);
        treeScrollPane.setPreferredSize(new Dimension(520, 360));
        contentPane.add(treeScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        JPanel pathPanel = new JPanel(new BorderLayout(8, 0));
        pathPanel.add(new JLabel("JSON Path:"), BorderLayout.WEST);
        pathPanel.add(jsonPathTextField, BorderLayout.CENTER);
        bottomPanel.add(pathPanel, BorderLayout.NORTH);

        JPanel previewPanel = new JPanel(new BorderLayout(8, 0));
        previewPanel.add(new JLabel("取值预览:"), BorderLayout.NORTH);
        previewPanel.add(new JScrollPane(valuePreviewTextArea), BorderLayout.CENTER);
        bottomPanel.add(previewPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.5, 0.7);
        applyMacWindowStyle(contentPane);

        jsonTree.addTreeSelectionListener(this::onTreeSelectionChanged);
        jsonPathTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateValuePreview();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateValuePreview();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateValuePreview();
            }
        });
        jsonTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onOK();
                }
            }
        });

        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        getRootPane().setDefaultButton(okButton);

        TreePath rootPath = new TreePath(jsonTree.getModel().getRoot());
        jsonTree.setSelectionPath(rootPath);
        jsonTree.scrollPathToVisible(rootPath);
        updateValuePreview();
    }

    private void applyMacWindowStyle(JPanel contentPane) {
        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            contentPane.setBorder(BorderFactory.createEmptyBorder(28, 10, 10, 10));
        }
    }

    private void onTreeSelectionChanged(TreeSelectionEvent event) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jsonTree.getLastSelectedPathComponent();
        if (selectedNode == null) {
            return;
        }
        Object userObject = selectedNode.getUserObject();
        if (userObject instanceof JsonPathTreeUtil.JsonPathNode jsonPathNode) {
            jsonPathTextField.setText(jsonPathNode.getJsonPath());
            valuePreviewTextArea.setText(JsonPathUtil.formatValue(jsonPathNode.getValue()));
        }
    }

    private void updateValuePreview() {
        String jsonPath = StringUtils.trimToEmpty(jsonPathTextField.getText());
        if (StringUtils.isBlank(jsonPath)) {
            valuePreviewTextArea.setText("");
            return;
        }
        try {
            valuePreviewTextArea.setText(JsonPathUtil.getFormattedValue(jsonText, jsonPath));
        } catch (Exception ex) {
            valuePreviewTextArea.setText("");
        }
    }

    private void onOK() {
        String jsonPath = StringUtils.trimToEmpty(jsonPathTextField.getText());
        if (StringUtils.isBlank(jsonPath)) {
            MsgUtil.info(this, "msg.jsonPathEmpty");
            jsonPathTextField.grabFocus();
            return;
        }
        try {
            JsonPathUtil.getByPath(jsonText, jsonPath);
            selectedJsonPath = jsonPath;
            dispose();
        } catch (Exception ex) {
            MsgUtil.errorWithDetail(this, "msg.jsonPathInvalid", ex.getMessage());
            jsonPathTextField.grabFocus();
            jsonPathTextField.selectAll();
        }
    }

    private void onCancel() {
        selectedJsonPath = null;
        dispose();
    }

    private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }
        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    public String getSelectedJsonPath() {
        return selectedJsonPath;
    }
}
