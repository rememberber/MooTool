package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片水印工具
 */
public class ImageWatermarkUtil {

    public enum OutputMode {
        OVERWRITE,
        KEEP_ORIGINAL
    }

    public enum Position {
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        TOP_RIGHT,
        TOP_LEFT,
        CENTER,
        TILE
    }

    public enum FontSizeMode {
        AUTO,
        SMALL,
        MEDIUM,
        LARGE
    }

    @Getter
    @Setter
    public static class WatermarkOptions {
        private String text;
        /** 不透明度 0.0 ~ 1.0 */
        private float opacity = 0.5f;
        private Color color = Color.WHITE;
        private Position position = Position.BOTTOM_RIGHT;
        private FontSizeMode fontSizeMode = FontSizeMode.AUTO;
        /** 是否倾斜 -45° */
        private boolean diagonal;
        private OutputMode outputMode = OutputMode.KEEP_ORIGINAL;
    }

    @Getter
    public static class WatermarkResult {
        private final boolean success;
        private final File outputFile;
        private final String errorMessage;

        private WatermarkResult(boolean success, File outputFile, String errorMessage) {
            this.success = success;
            this.outputFile = outputFile;
            this.errorMessage = errorMessage;
        }

        public static WatermarkResult ok(File outputFile) {
            return new WatermarkResult(true, outputFile, null);
        }

        public static WatermarkResult fail(String errorMessage) {
            return new WatermarkResult(false, null, errorMessage);
        }
    }

    public static WatermarkResult watermark(File sourceFile, WatermarkOptions options) {
        if (sourceFile == null || !sourceFile.isFile()) {
            return WatermarkResult.fail("源文件不存在");
        }
        if (StrUtil.isBlank(options.getText())) {
            return WatermarkResult.fail("水印文字不能为空");
        }
        try {
            BufferedImage image = ImageIO.read(sourceFile);
            if (image == null) {
                return WatermarkResult.fail("无法读取图片");
            }

            BufferedImage watermarked = applyWatermark(image, options);
            String format = resolveFormat(sourceFile);
            File outputFile = resolveOutputFile(sourceFile, format, options.getOutputMode());

            if (options.getOutputMode() == OutputMode.OVERWRITE && outputFile.exists()) {
                File tempFile = FileUtil.file(outputFile.getParent(), outputFile.getName() + ".tmp");
                writeImage(watermarked, tempFile, format);
                FileUtil.move(tempFile, outputFile, true);
            } else {
                writeImage(watermarked, outputFile, format);
            }

            return WatermarkResult.ok(outputFile);
        } catch (IOException e) {
            return WatermarkResult.fail(e.getMessage());
        }
    }

    public static BufferedImage applyWatermark(BufferedImage image, WatermarkOptions options) {
        int imageType = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), imageType);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, null);

        int fontSize = resolveFontSize(image, options.getFontSizeMode());
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
        g.setFont(font);

        int alpha = Math.max(1, Math.min(255, Math.round(options.getOpacity() * 255)));
        Color baseColor = options.getColor() == null ? Color.WHITE : options.getColor();
        g.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));

        FontMetrics metrics = g.getFontMetrics();
        String text = options.getText().trim();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        int margin = Math.max(8, Math.round(Math.min(image.getWidth(), image.getHeight()) * 0.02f));

        if (options.getPosition() == Position.TILE) {
            drawTiledWatermark(g, text, image.getWidth(), image.getHeight(), textWidth, textHeight,
                    options.isDiagonal(), margin);
        } else {
            Point anchor = resolveAnchor(image.getWidth(), image.getHeight(), textWidth, textHeight,
                    options.getPosition(), margin);
            drawRotatedText(g, text, anchor.x, anchor.y, options.isDiagonal());
        }

        g.dispose();
        return result;
    }

    private static void drawTiledWatermark(Graphics2D g, String text, int imageWidth, int imageHeight,
                                           int textWidth, int textHeight, boolean diagonal, int margin) {
        int stepX = textWidth + margin * 3;
        int stepY = textHeight + margin * 3;
        if (diagonal) {
            AffineTransform original = g.getTransform();
            for (int y = -imageHeight; y < imageHeight * 2; y += stepY) {
                for (int x = -imageWidth; x < imageWidth * 2; x += stepX) {
                    g.setTransform(original);
                    drawRotatedText(g, text, x, y, true);
                }
            }
            g.setTransform(original);
        } else {
            for (int y = margin + textHeight; y < imageHeight; y += stepY) {
                for (int x = margin; x < imageWidth; x += stepX) {
                    g.drawString(text, x, y);
                }
            }
        }
    }

    private static void drawRotatedText(Graphics2D g, String text, int x, int y, boolean diagonal) {
        if (!diagonal) {
            g.drawString(text, x, y);
            return;
        }
        AffineTransform original = g.getTransform();
        g.rotate(Math.toRadians(-45), x, y);
        g.drawString(text, x, y);
        g.setTransform(original);
    }

    private static Point resolveAnchor(int imageWidth, int imageHeight, int textWidth, int textHeight,
                                       Position position, int margin) {
        return switch (position) {
            case TOP_LEFT -> new Point(margin, margin + textHeight - metricsDescent(textHeight));
            case TOP_RIGHT -> new Point(imageWidth - textWidth - margin, margin + textHeight - metricsDescent(textHeight));
            case BOTTOM_LEFT -> new Point(margin, imageHeight - margin);
            case CENTER -> new Point((imageWidth - textWidth) / 2, (imageHeight + textHeight) / 2 - metricsDescent(textHeight));
            case BOTTOM_RIGHT -> new Point(imageWidth - textWidth - margin, imageHeight - margin);
            default -> new Point(imageWidth - textWidth - margin, imageHeight - margin);
        };
    }

    private static int metricsDescent(int textHeight) {
        return Math.round(textHeight * 0.25f);
    }

    private static int resolveFontSize(BufferedImage image, FontSizeMode mode) {
        int base = Math.round(Math.min(image.getWidth(), image.getHeight()) * 0.05f);
        return switch (mode) {
            case SMALL -> Math.max(16, Math.min(base, 28));
            case MEDIUM -> Math.max(24, Math.min(base + 8, 42));
            case LARGE -> Math.max(32, Math.min(base + 16, 64));
            case AUTO -> Math.max(18, Math.min(base, 48));
        };
    }

    private static String resolveFormat(File sourceFile) {
        String ext = FileUtil.extName(sourceFile).toLowerCase();
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "jpg";
        }
        return "png";
    }

    private static File resolveOutputFile(File sourceFile, String format, OutputMode outputMode) {
        String baseName = FileUtil.mainName(sourceFile);
        String parent = sourceFile.getParent();
        if (outputMode == OutputMode.OVERWRITE) {
            return FileUtil.file(parent, baseName + "." + format);
        }
        return FileUtil.file(parent, baseName + "_watermarked." + format);
    }

    private static void writeImage(BufferedImage image, File outputFile, String format) throws IOException {
        BufferedImage toWrite = image;
        if ("jpg".equals(format) && image.getColorModel().hasAlpha()) {
            toWrite = removeAlpha(image);
        }
        if (!ImageIO.write(toWrite, format, outputFile)) {
            throw new IOException("不支持的图片格式：" + format);
        }
    }

    private static BufferedImage removeAlpha(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }
}
