package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.util.ImageDisplayUtil;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

/**
 * 图片预览组件：通过 paintComponent 直接绘制，在 HiDPI 屏幕上保持清晰
 */
public class ImagePreviewComponent extends JComponent {

    private BufferedImage sourceImage;
    private Image placeholderImage;
    private double zoomFactor = 1.0;
    /** true 表示图片像素与逻辑坐标 1:1（如二维码）；false 表示按物理像素映射（照片、截图） */
    private boolean pixelEqualsLogicalPoint;
    private boolean crispPixels;

    public ImagePreviewComponent() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                updatePreferredSize();
            }
        });
    }

    public void setSourceImage(BufferedImage image, double zoomFactor) {
        setSourceImage(image, zoomFactor, false);
    }

    public void setSourceImage(BufferedImage image, double zoomFactor, boolean pixelEqualsLogicalPoint) {
        this.sourceImage = image;
        this.placeholderImage = null;
        this.pixelEqualsLogicalPoint = pixelEqualsLogicalPoint;
        this.zoomFactor = Math.max(0.01, zoomFactor);
        updatePreferredSize();
        repaint();
    }

    public void setPlaceholderImage(Image image) {
        this.sourceImage = null;
        this.placeholderImage = image;
        this.zoomFactor = 1.0;
        setPreferredSize(new Dimension(128, 128));
        revalidate();
        repaint();
    }

    public void clear() {
        sourceImage = null;
        placeholderImage = null;
        zoomFactor = 1.0;
        setPreferredSize(new Dimension(0, 0));
        revalidate();
        repaint();
    }

    public void setCrispPixels(boolean crispPixels) {
        this.crispPixels = crispPixels;
        repaint();
    }

    /**
     * 在固定逻辑尺寸区域内展示图片（如头像、应用图标）
     */
    public void setSourceImageInLogicalBounds(BufferedImage image, int logicalWidth, int logicalHeight) {
        this.sourceImage = image;
        this.placeholderImage = null;
        this.pixelEqualsLogicalPoint = false;
        this.zoomFactor = 1.0;
        Dimension size = new Dimension(Math.max(1, logicalWidth), Math.max(1, logicalHeight));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        revalidate();
        repaint();
    }

    public int getBaseLogicalWidth() {
        if (sourceImage == null) {
            return 0;
        }
        if (pixelEqualsLogicalPoint) {
            return sourceImage.getWidth();
        }
        double scale = ImageDisplayUtil.getScaleFactor(this);
        return Math.max(1, (int) Math.round(sourceImage.getWidth() / scale));
    }

    private void updatePreferredSize() {
        if (sourceImage == null) {
            setPreferredSize(new Dimension(0, 0));
            revalidate();
            return;
        }
        int baseLogicalWidth;
        int baseLogicalHeight;
        if (pixelEqualsLogicalPoint) {
            baseLogicalWidth = sourceImage.getWidth();
            baseLogicalHeight = sourceImage.getHeight();
        } else {
            double scale = ImageDisplayUtil.getScaleFactor(this);
            baseLogicalWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() / scale));
            baseLogicalHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() / scale));
        }
        int width = Math.max(1, (int) Math.round(baseLogicalWidth * zoomFactor));
        int height = Math.max(1, (int) Math.round(baseLogicalHeight * zoomFactor));
        setPreferredSize(new Dimension(width, height));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (sourceImage != null) {
                Object interpolation = crispPixels
                        ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                        : RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                if (!crispPixels) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                }
                g2.drawImage(sourceImage, 0, 0, getWidth(), getHeight(), null);
            } else if (placeholderImage != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(placeholderImage, 0, 0, getWidth(), getHeight(), this);
            }
        } finally {
            g2.dispose();
        }
    }
}
