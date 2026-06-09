package com.luoboduner.moo.tool.ui.component;

import javax.swing.*;
import java.awt.*;

public final class TabUiUtil {

    private static final ThreadLocal<Boolean> INSTALLING = ThreadLocal.withInitial(() -> false);

    private TabUiUtil() {
    }

    public static void applySafeTabbedPaneUi(Component root) {
        if (root == null) {
            return;
        }
        if (root instanceof JTabbedPane) {
            installSafeUi((JTabbedPane) root, false);
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                applySafeTabbedPaneUi(child);
            }
        }
    }

    public static void installSafeUi(JTabbedPane tabbedPane, boolean leadingEdgeSelection) {
        if (tabbedPane == null || INSTALLING.get()) {
            return;
        }
        if (tabbedPane.getUI() instanceof MooFlatTabbedPaneUI) {
            ((MooFlatTabbedPaneUI) tabbedPane.getUI()).setSelectionOnLeadingEdge(leadingEdgeSelection);
            return;
        }
        INSTALLING.set(true);
        try {
            MooFlatTabbedPaneUI ui = new MooFlatTabbedPaneUI();
            ui.setSelectionOnLeadingEdge(leadingEdgeSelection);
            tabbedPane.setUI(ui);
        } finally {
            INSTALLING.set(false);
        }
    }
}
