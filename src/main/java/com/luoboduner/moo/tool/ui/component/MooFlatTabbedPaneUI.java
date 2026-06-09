package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import static com.formdev.flatlaf.util.UIScale.scale;

/**
 * 左侧功能 Tab 时，将选中指示器绘制在 Tab 左侧（靠窗口边缘），而非默认的右侧。
 */
public class MooFlatTabbedPaneUI extends FlatTabbedPaneUI {

    private boolean selectionOnLeadingEdge;

    public void setSelectionOnLeadingEdge(boolean selectionOnLeadingEdge) {
        this.selectionOnLeadingEdge = selectionOnLeadingEdge;
    }

    @Override
    protected void paintTabSelection(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h) {
        if (!selectionOnLeadingEdge || tabPlacement != JTabbedPane.LEFT) {
            super.paintTabSelection(g, tabPlacement, tabIndex, x, y, w, h);
            return;
        }

        g.setColor(tabPane.isEnabled()
                ? (isTabbedPaneOrChildFocused() ? underlineColor : inactiveUnderlineColor)
                : disabledUnderlineColor);

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
