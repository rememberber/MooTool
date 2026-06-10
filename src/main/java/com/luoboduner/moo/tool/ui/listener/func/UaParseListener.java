package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.form.func.UaParseForm;
import com.luoboduner.moo.tool.util.FuncHistoryUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.UaParseUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;

/**
 * UA 分析事件监听
 */
public class UaParseListener {

    public static void addListeners() {
        UaParseForm form = UaParseForm.getInstance();

        form.getParseButton().addActionListener(e -> parse(form, true));
        form.getClearButton().addActionListener(e -> {
            form.getUaInputTextArea().setText("");
            clearResult(form);
        });
        form.getPasteButton().addActionListener(e -> {
            String clipboard = ClipboardUtil.getStr();
            if (clipboard != null) {
                form.getUaInputTextArea().setText(clipboard);
                parse(form, true);
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
                parse(form, true);
            }
        });
    }

    public static void parse(UaParseForm form, boolean saveHistory) {
        String ua = form.getUaInputTextArea().getText();
        Map<String, String> result = UaParseUtil.parse(ua);
        String[] fields = result.keySet().toArray(new String[0]);
        String[] values = result.values().toArray(new String[0]);
        DefaultTableModel model = new DefaultTableModel(resultColumns(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        StringBuilder outputBuilder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            model.addRow(new Object[]{fields[i], values[i]});
            outputBuilder.append(fields[i]).append(": ").append(values[i]).append('\n');
        }
        form.getResultTable().setModel(model);
        form.getResultTable().getColumnModel().getColumn(0).setPreferredWidth(120);
        form.getResultTable().getColumnModel().getColumn(1).setPreferredWidth(400);

        if (saveHistory && StringUtils.isNotBlank(ua)) {
            FuncHistoryUtil.save(FuncConsts.UA_PARSE, I18n.get("history.summary.uaParse"), ua, outputBuilder.toString().trim(), I18n.get("history.summary.uaParse"));
            if (UaParseForm.getHistoryPanel() != null) {
                UaParseForm.getHistoryPanel().refreshListIfVisible();
            }
        }
    }

    private static void clearResult(UaParseForm form) {
        form.getResultTable().setModel(new DefaultTableModel(resultColumns(), 0));
    }

    private static String[] resultColumns() {
        return new String[]{I18n.get("table.col.field"), I18n.get("table.col.value")};
    }
}
