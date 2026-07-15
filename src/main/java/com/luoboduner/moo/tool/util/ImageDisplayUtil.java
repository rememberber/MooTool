package com.luoboduner.moo.tool.util;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.ui.component.ImagePreviewComponent;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

    public static BufferedImage readResourceImage(String resourcePath) throws IOException {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream in = ImageDisplayUtil.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Resource not found: " + path);
            }
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Cannot decode image: " + path);
            }
            return image;
        }
    }

    public static BufferedImage readImage(URL url) throws IOException {
        BufferedImage image = ImageIO.read(url);
        if (image == null) {
            throw new IOException("Cannot decode image: " + url);
        }
        return image;
    }

    public static ImagePreviewComponent replaceLabelWithImagePreview(JLabel label) {
        ImagePreviewComponent preview = new ImagePreviewComponent();
        preview.setToolTipText(label.getToolTipText());
        for (MouseListener listener : label.getMouseListeners()) {
            preview.addMouseListener(listener);
        }
        Container parent = label.getParent();
        if (parent == null) {
            return preview;
        }
        if (parent.getLayout() instanceof GridLayoutManager layout) {
            GridConstraints constraints = layout.getConstraintsForComponent(label);
            parent.remove(label);
            parent.add(preview, constraints);
        } else {
            int index = indexOfComponent(parent, label);
            parent.remove(label);
            if (index >= 0) {
                parent.add(preview, index);
            } else {
                parent.add(preview);
            }
        }
        parent.revalidate();
        parent.repaint();
        return preview;
    }

    public static ImagePreviewComponent installResourceImage(JLabel label, String resourcePath) throws IOException {
        ImagePreviewComponent preview = replaceLabelWithImagePreview(label);
        preview.setSourceImage(readResourceImage(resourcePath), 1.0);
        return preview;
    }

    public static ImagePreviewComponent installImage(JLabel label, BufferedImage image) {
        ImagePreviewComponent preview = replaceLabelWithImagePreview(label);
        preview.setSourceImage(image, 1.0);
        return preview;
    }

    public static ImagePreviewComponent installImageInLogicalBounds(JLabel label, BufferedImage image,
                                                                    int logicalWidth, int logicalHeight) {
        ImagePreviewComponent preview = replaceLabelWithImagePreview(label);
        preview.setSourceImageInLogicalBounds(image, logicalWidth, logicalHeight);
        return preview;
    }

    public static ImagePreviewComponent installResourceImageQuietly(JLabel label, String resourcePath) {
        try {
            return installResourceImage(label, resourcePath);
        } catch (IOException ignored) {
            // 静态资源加载失败时保留原 JLabel 展示
            return null;
        }
    }

    private static int indexOfComponent(Container parent, Component component) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == component) {
                return i;
            }
        }
        return -1;
    }
}
