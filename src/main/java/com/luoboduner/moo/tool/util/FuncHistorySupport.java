package com.luoboduner.moo.tool.util;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.domain.TFuncHistory;
import com.luoboduner.moo.tool.ui.component.FuncHistoryPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 历史记录面板挂载工具
 */
public final class FuncHistorySupport {

    private FuncHistorySupport() {
    }

    public static FuncHistoryPanel attachTab(JTabbedPane tabbedPane,
                                             String funcType,
                                             Consumer<TFuncHistory> applyHandler) {
        FuncHistoryPanel historyPanel = new FuncHistoryPanel(funcType, applyHandler);
        tabbedPane.addTab("历史记录", historyPanel);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == historyPanel) {
                historyPanel.refreshList();
            }
        });
        return historyPanel;
    }

    public static FuncHistoryPanel wrapWithTabs(JPanel panel,
                                                String mainTabTitle,
                                                String funcType,
                                                Consumer<TFuncHistory> applyHandler) {
        LayoutManager layout = panel.getLayout();
        List<ComponentEntry> entries = new ArrayList<>();
        if (layout instanceof GridLayoutManager gridLayout) {
            for (Component component : panel.getComponents()) {
                entries.add(new ComponentEntry(component, gridLayout.getConstraintsForComponent(component)));
            }
        } else {
            for (Component component : panel.getComponents()) {
                entries.add(new ComponentEntry(component, null));
            }
        }
        panel.removeAll();

        JPanel mainContent = new JPanel(layout);
        for (ComponentEntry entry : entries) {
            if (entry.constraints instanceof GridConstraints constraints) {
                mainContent.add(entry.component, constraints);
            } else {
                mainContent.add(entry.component);
            }
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(mainTabTitle, mainContent);
        FuncHistoryPanel historyPanel = new FuncHistoryPanel(funcType, applyHandler);
        tabbedPane.addTab("历史记录", historyPanel);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == historyPanel) {
                historyPanel.refreshList();
            }
        });

        panel.setLayout(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        return historyPanel;
    }

    private record ComponentEntry(Component component, Object constraints) {
    }
}
