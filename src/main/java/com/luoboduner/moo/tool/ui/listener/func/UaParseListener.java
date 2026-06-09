package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.luoboduner.moo.tool.ui.form.func.UaParseForm;
import com.luoboduner.moo.tool.util.UaParseUtil;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;

/**
 * UA 分析事件监听
 */
public class UaParseListener {

    public static void addListeners() {
        UaParseForm form = UaParseForm.getInstance();

        form.getParseButton().addActionListener(e -> parse(form));
        form.getClearButton().addActionListener(e -> {
            form.getUaInputTextArea().setText("");
            clearResult(form);
        });
        form.getPasteButton().addActionListener(e -> {
            String clipboard = ClipboardUtil.getStr();
            if (clipboard != null) {
                form.getUaInputTextArea().setText(clipboard);
                parse(form);
            }
        });
        form.getPresetComboBox().addActionListener(e -> {
            int index = form.getPresetComboBox().getSelectedIndex();
            if (index <= 0) {
                return;
            }
            List<String[]> presets = UaParseUtil.presetUserAgents();
            if (index - 1 < presets.size()) {
                form.getUaInputTextArea().setText(presets.get(index - 1)[1]);
                parse(form);
            }
        });
    }

    private static void parse(UaParseForm form) {
        Map<String, String> result = UaParseUtil.parse(form.getUaInputTextArea().getText());
        String[] fields = result.keySet().toArray(new String[0]);
        String[] values = result.values().toArray(new String[0]);
        DefaultTableModel model = new DefaultTableModel(new String[]{"字段", "值"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (int i = 0; i < fields.length; i++) {
            model.addRow(new Object[]{fields[i], values[i]});
        }
        form.getResultTable().setModel(model);
        form.getResultTable().getColumnModel().getColumn(0).setPreferredWidth(120);
        form.getResultTable().getColumnModel().getColumn(1).setPreferredWidth(400);
    }

    private static void clearResult(UaParseForm form) {
        form.getResultTable().setModel(new DefaultTableModel(new String[]{"字段", "值"}, 0));
    }
}
