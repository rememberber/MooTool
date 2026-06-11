package com.luoboduner.moo.tool.util;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.*;

/**
 * some functions about scroll
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/11/23.
 */
public class ScrollUtil {

    private static final int GENTLE_WHEEL_PIXELS_PER_NOTCH = 16;

    public static void smoothPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(14);
        scrollPane.getVerticalScrollBar().setDoubleBuffered(true);
        scrollPane.getHorizontalScrollBar().setDoubleBuffered(true);
    }

    /**
     * 降低滚轮滚动速度，避免 JEditorPane 等组件默认滚动过快。
     */
    public static void gentleWheelScroll(JScrollPane scrollPane, Component wheelTarget) {
        scrollPane.setWheelScrollingEnabled(false);
        MouseWheelListener listener = e -> applyGentleWheelScroll(scrollPane, e);
        scrollPane.addMouseWheelListener(listener);
        if (wheelTarget != null) {
            wheelTarget.addMouseWheelListener(listener);
        }
    }

    private static void applyGentleWheelScroll(JScrollPane scrollPane, MouseWheelEvent e) {
        if (e.isConsumed()) {
            return;
        }
        JScrollBar bar = e.isShiftDown()
                ? scrollPane.getHorizontalScrollBar()
                : scrollPane.getVerticalScrollBar();
        if (bar == null || !bar.isVisible()) {
            return;
        }
        bar.setValue(bar.getValue() + wheelDelta(e));
        e.consume();
    }

    private static int wheelDelta(MouseWheelEvent e) {
        double precise = e.getPreciseWheelRotation();
        if (precise != 0.0d) {
            return (int) Math.round(precise * GENTLE_WHEEL_PIXELS_PER_NOTCH);
        }
        return e.getWheelRotation() * GENTLE_WHEEL_PIXELS_PER_NOTCH;
    }
}
