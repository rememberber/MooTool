package com.luoboduner.moo.tool.util;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

/**
 * 图片展示工具
 */
public final class ImageDisplayUtil {

    private ImageDisplayUtil() {
    }

    public static double getScaleFactor(Component component) {
        GraphicsConfiguration gc = resolveGraphicsConfiguration(component);
        if (gc == null) {
            return 1.0;
        }
        AffineTransform transform = gc.getDefaultTransform();
        double scale = transform.getScaleX();
        return scale > 0 ? scale : 1.0;
    }

    private static GraphicsConfiguration resolveGraphicsConfiguration(Component component) {
        if (component != null) {
            if (component.isDisplayable()) {
                GraphicsConfiguration gc = component.getGraphicsConfiguration();
                if (gc != null) {
                    return gc;
                }
            }
            Window window = SwingUtilities.getWindowAncestor(component);
            if (window != null && window.isDisplayable()) {
                GraphicsConfiguration gc = window.getGraphicsConfiguration();
                if (gc != null) {
                    return gc;
                }
            }
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
    }

    public static BufferedImage scaleImage(BufferedImage source, int targetWidth, int targetHeight) {
        if (source == null) {
            return null;
        }
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        if (targetWidth <= 0 && targetHeight <= 0) {
            return source;
        }
        if (targetWidth <= 0) {
            targetWidth = Math.max(1, (int) Math.round(srcWidth * ((double) targetHeight / srcHeight)));
        } else if (targetHeight <= 0) {
            targetHeight = Math.max(1, (int) Math.round(srcHeight * ((double) targetWidth / srcWidth)));
        }
        if (targetWidth == srcWidth && targetHeight == srcHeight) {
            return source;
        }

        int imageType = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return scaled;
    }
}
