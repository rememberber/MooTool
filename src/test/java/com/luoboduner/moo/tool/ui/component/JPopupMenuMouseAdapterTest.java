package com.luoboduner.moo.tool.ui.component;

import org.junit.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;

public class JPopupMenuMouseAdapterTest {

    @Test
    public void calculatePopupLocationUsesAbsoluteCoordinatesOnSecondaryScreen() {
        Point location = JPopupMenuMouseAdapter.calculatePopupLocation(
                new Point(2500, 1040),
                new Dimension(180, 120),
                new Rectangle(1920, 0, 1920, 1080));

        assertEquals(new Point(2320, 920), location);
    }

    @Test
    public void calculatePopupLocationKeepsPopupInScreenWithNegativeOrigin() {
        Point location = JPopupMenuMouseAdapter.calculatePopupLocation(
                new Point(-10, 1040),
                new Dimension(180, 120),
                new Rectangle(-1920, 0, 1920, 1080));

        assertEquals(new Point(-190, 920), location);
    }

    @Test
    public void calculatePopupLocationClampsToCurrentScreenBounds() {
        Point location = JPopupMenuMouseAdapter.calculatePopupLocation(
                new Point(1930, 10),
                new Dimension(180, 120),
                new Rectangle(1920, 0, 1920, 1080));

        assertEquals(new Point(1920, 0), location);
    }
}
