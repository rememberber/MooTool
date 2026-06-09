package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片压缩工具
 */
public class ImageCompressUtil {

    public enum OutputMode {
        /** 覆盖原图 */
        OVERWRITE,
        /** 保留原图，另存为新文件 */
        KEEP_ORIGINAL
    }

    public enum OutputFormat {
        /** 保持与原文件相同格式 */
        AUTO,
        PNG,
        JPEG
    }

    @Getter
    @Setter
    public static class CompressOptions {
        /** 压缩质量 0.0 ~ 1.0 */
        private float quality = 0.8f;
        /** 缩放比例 0.1 ~ 1.0 */
        private float scale = 1.0f;
        private OutputFormat outputFormat = OutputFormat.AUTO;
        private OutputMode outputMode = OutputMode.KEEP_ORIGINAL;
    }

    @Getter
    public static class CompressResult {
        private final boolean success;
        private final File outputFile;
        private final long originalSize;
        private final long compressedSize;
        private final String errorMessage;

        private CompressResult(boolean success, File outputFile, long originalSize, long compressedSize, String errorMessage) {
            this.success = success;
            this.outputFile = outputFile;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.errorMessage = errorMessage;
        }

        public static CompressResult ok(File outputFile, long originalSize, long compressedSize) {
            return new CompressResult(true, outputFile, originalSize, compressedSize, null);
        }

        public static CompressResult fail(String errorMessage) {
            return new CompressResult(false, null, 0, 0, errorMessage);
        }

        public long getSavedBytes() {
            return Math.max(0, originalSize - compressedSize);
        }
    }

    public static CompressResult compress(File sourceFile, CompressOptions options) {
        if (sourceFile == null || !sourceFile.isFile()) {
            return CompressResult.fail("源文件不存在");
        }
        long originalSize = sourceFile.length();
        try {
            BufferedImage image = ImageIO.read(sourceFile);
            if (image == null) {
                return CompressResult.fail("无法读取图片");
            }

            BufferedImage processed = scaleImage(image, options.getScale());
            String format = resolveFormat(sourceFile, options.getOutputFormat());
            File outputFile = resolveOutputFile(sourceFile, format, options.getOutputMode());

            if (options.getOutputMode() == OutputMode.OVERWRITE && outputFile.exists()) {
                File tempFile = FileUtil.file(outputFile.getParent(), outputFile.getName() + ".tmp");
                writeImage(processed, tempFile, format, options.getQuality());
                if (!sourceFile.equals(outputFile) && sourceFile.exists()) {
                    FileUtil.del(sourceFile);
                }
                FileUtil.move(tempFile, outputFile, true);
            } else {
                writeImage(processed, outputFile, format, options.getQuality());
            }

            return CompressResult.ok(outputFile, originalSize, outputFile.length());
        } catch (IOException e) {
            return CompressResult.fail(e.getMessage());
        }
    }

    private static String resolveFormat(File sourceFile, OutputFormat outputFormat) {
        if (outputFormat == OutputFormat.PNG) {
            return "png";
        }
        if (outputFormat == OutputFormat.JPEG) {
            return "jpg";
        }
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
        return FileUtil.file(parent, baseName + "_compressed." + format);
    }

    private static BufferedImage scaleImage(BufferedImage image, float scale) {
        if (scale >= 0.999f) {
            return image;
        }
        int newWidth = Math.max(1, Math.round(image.getWidth() * scale));
        int newHeight = Math.max(1, Math.round(image.getHeight() * scale));
        int imageType = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    private static void writeImage(BufferedImage image, File outputFile, String format, float quality) throws IOException {
        BufferedImage toWrite = image;
        if ("jpg".equals(format) && image.getColorModel().hasAlpha()) {
            toWrite = removeAlpha(image);
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            ImageIO.write(toWrite, format, outputFile);
            return;
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.01f, Math.min(1.0f, quality)));
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(toWrite, null, null), param);
        } finally {
            writer.dispose();
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
