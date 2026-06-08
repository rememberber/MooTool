package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 随手记附件（图片）工具
 */
public class QuickNoteAttachmentUtil {

    public static final String ATTACHMENTS_DIR_NAME = "attachments";
    public static final String ATTACHMENTS_RELATIVE_PREFIX = ATTACHMENTS_DIR_NAME + "/";

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "webp"
    );

    private QuickNoteAttachmentUtil() {
    }

    public static File getAttachmentsDir() {
        File dir = new File(SystemUtil.CONFIG_HOME, ATTACHMENTS_DIR_NAME);
        FileUtil.mkdir(dir);
        return dir;
    }

    public static String saveImage(BufferedImage image) throws IOException {
        String fileName = generateFileName("png");
        File dest = new File(getAttachmentsDir(), fileName);
        ImageIO.write(image, "png", dest);
        return ATTACHMENTS_RELATIVE_PREFIX + fileName;
    }

    public static String saveImage(File sourceFile) throws IOException {
        String ext = normalizeImageExtension(FileUtil.extName(sourceFile));
        String fileName = generateFileName(ext);
        File dest = new File(getAttachmentsDir(), fileName);
        FileUtil.copy(sourceFile, dest, true);
        return ATTACHMENTS_RELATIVE_PREFIX + fileName;
    }

    public static boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(normalizeImageExtension(FileUtil.extName(file)));
    }

    public static String toMarkdownImage(String relativePath, String alt) {
        if (StringUtils.isBlank(alt)) {
            alt = "image";
        }
        return "![" + alt + "](" + relativePath + ")";
    }

    public static String resolveImageSrc(String src) {
        if (StringUtils.isBlank(src)) {
            return src;
        }
        String trimmed = src.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:")) {
            return trimmed;
        }

        File file = resolveImageFile(trimmed);
        if (file != null && file.exists()) {
            return file.toURI().toString();
        }
        return trimmed;
    }

    private static File resolveImageFile(String src) {
        if (src.startsWith(ATTACHMENTS_RELATIVE_PREFIX)) {
            return new File(SystemUtil.CONFIG_HOME, src);
        }
        if (src.startsWith("/" + ATTACHMENTS_DIR_NAME + "/")) {
            return new File(SystemUtil.CONFIG_HOME, src.substring(1));
        }
        File file = new File(src);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(getAttachmentsDir(), src);
    }

    private static String generateFileName(String ext) {
        return DateFormatUtils.format(new Date(), "yyyyMMdd_HHmmss")
                + "_" + UUID.randomUUID().toString().substring(0, 8)
                + "." + ext;
    }

    private static String normalizeImageExtension(String ext) {
        if (StringUtils.isBlank(ext)) {
            return "png";
        }
        String normalized = ext.toLowerCase(Locale.ROOT);
        if ("jpeg".equals(normalized)) {
            return "jpg";
        }
        return IMAGE_EXTENSIONS.contains(normalized) ? normalized : "png";
    }
}
