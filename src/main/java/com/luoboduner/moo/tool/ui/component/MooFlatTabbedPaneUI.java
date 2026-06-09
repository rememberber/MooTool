package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import static com.formdev.flatlaf.util.UIScale.scale;

/**
 * 左侧功能 Tab 定制 UI：
 * <ul>
 *   <li>选中指示器绘制在 Tab 左侧（靠窗口边缘）</li>
 *   <li>失焦时保持与点击选中时一致的样式</li>
 * </ul>
 */
public class MooFlatTabbedPaneUI extends FlatTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new MooFlatTabbedPaneUI();
    }

    private boolean selectionOnLeadingEdge;

    public void setSelectionOnLeadingEdge(boolean selectionOnLeadingEdge) {
        this.selectionOnLeadingEdge = selectionOnLeadingEdge;
    }

    /**
     * FlatLaf 在 UI 卸载/重装间隙绘制 Tab 时 tabInsets 可能为 null，需防御性初始化。
     */
    private boolean ensureUiDefaults() {
        if (tabPane == null) {
            return false;
        }
        if (tabInsets == null) {
            installDefaults();
        }
        return tabInsets != null;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if (!ensureUiDefaults()) {
            return;
        }
        super.update(g, c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!ensureUiDefaults()) {
            return;
        }
        super.paint(g, c);
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        if (!ensureUiDefaults()) {
            return 0;
        }
        return super.calculateTabWidth(tabPlacement, tabIndex, metrics);
    }

    @Override
    protected int calculateMaxTabWidth(int tabPlacement) {
        if (!ensureUiDefaults()) {
            return 0;
        }
        return super.calculateMaxTabWidth(tabPlacement);
    }

    /**
     * 失焦时仍按「有焦点」绘制选中指示器，避免 inactiveUnderlineColor 导致强调线消失。
     */
    @Override
    protected boolean isTabbedPaneOrChildFocused() {
        return true;
    }

    /**
     * 选中 Tab 始终使用 focus/selected 前景色，不随 TabbedPane 失焦而回退为普通前景色。
     */
    @Override
    protected Color getTabForeground(int tabPlacement, int tabIndex, boolean isSelected) {
        if (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex)) {
            return disabledForeground;
        }
        if (hoverForeground != null && getRolloverTab() == tabIndex) {
            return hoverForeground;
        }
        Color foreground = tabPane.getForegroundAt(tabIndex);
        if (foreground != tabPane.getForeground()) {
            return foreground;
        }
        if (focusForeground != null && isSelected) {
            return focusForeground;
        }
        if (selectedForeground != null && isSelected) {
            return selectedForeground;
        }
        return foreground;
    }

    /**
     * 选中 Tab 始终使用 focus/selected 背景色，不随 TabbedPane 失焦而回退为普通背景色。
     */
    @Override
    protected Color getTabBackground(int tabPlacement, int tabIndex, boolean isSelected) {
        Color background = tabPane.getBackgroundAt(tabIndex);
        if (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex)) {
            return background;
        }
        if (hoverColor != null && getRolloverTab() == tabIndex) {
            return hoverColor;
        }
        if (background != tabPane.getBackground()) {
            return background;
        }
        if (focusColor != null && isSelected) {
            return focusColor;
        }
        if (selectedBackground != null && isSelected) {
            return selectedBackground;
        }
        return background;
    }

    @Override
    protected void paintTabSelection(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h) {
        if (!selectionOnLeadingEdge || tabPlacement != JTabbedPane.LEFT) {
            super.paintTabSelection(g, tabPlacement, tabIndex, x, y, w, h);
            return;
        }

        g.setColor(tabPane.isEnabled() ? underlineColor : disabledUnderlineColor);

        boolean isCard = (getTabType() == TAB_TYPE_CARD);

        int tabSelectionHeight = scale(isCard ? cardTabSelectionHeight : this.tabSelectionHeight);
        float arc = scale((float) (isCard ? cardTabArc : tabSelectionArc)) / 2f;
        int sx = x;
        int sy = y;
        int sw = tabSelectionHeight;
        int sh = h;

        if (!isCard && tabSelectionInsets != null) {
            Insets insets = new Insets(0, 0, 0, 0);
            rotateInsets(tabSelectionInsets, insets, tabPane.getTabPlacement());

            sx += scale(insets.left);
            sy += scale(insets.top);
            sw -= scale(insets.left + insets.right);
            sh -= scale(insets.top + insets.bottom);
        }

        if (arc <= 0) {
            g.fillRect(sx, sy, sw, sh);
        } else if (isCard) {
            Area area = new Area(createCardTabOuterPath(tabPlacement, x, y, w, h));
            area.intersect(new Area(new Rectangle2D.Float(sx, sy, sw, sh)));
            ((Graphics2D) g).fill(area);
        } else {
            FlatUIUtils.paintSelection((Graphics2D) g, sx, sy, sw, sh, null, arc, arc, arc, arc, 0);
        }
    }
}
