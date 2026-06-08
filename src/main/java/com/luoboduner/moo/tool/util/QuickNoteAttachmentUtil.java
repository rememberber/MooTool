package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 随手记附件（图片）工具
 */
@Slf4j
public class QuickNoteAttachmentUtil {

    public static final String ATTACHMENTS_DIR_NAME = "attachments";
    public static final String ATTACHMENTS_RELATIVE_PREFIX = ATTACHMENTS_DIR_NAME + "/";

    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile(
            "!\\[[^\\]]*]\\(\\s*([^)\\s]+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile(
            "<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

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

    public static Set<String> extractAttachmentPaths(String content) {
        Set<String> paths = new LinkedHashSet<>();
        if (StringUtils.isBlank(content)) {
            return paths;
        }
        addAttachmentPathsFromMatcher(paths, MARKDOWN_IMAGE_PATTERN.matcher(content));
        addAttachmentPathsFromMatcher(paths, HTML_IMAGE_PATTERN.matcher(content));
        return paths;
    }

    public static Set<String> collectAttachmentPaths(Collection<String> contents) {
        Set<String> paths = new HashSet<>();
        if (contents == null) {
            return paths;
        }
        for (String content : contents) {
            paths.addAll(extractAttachmentPaths(content));
        }
        return paths;
    }

    /**
     * 文档内容变更后，删除已从文中移除且未被其他笔记引用的附件
     */
    public static void cleanupRemovedAttachments(String oldContent, String newContent,
                                                 Collection<String> otherNotesContents) {
        Set<String> removedPaths = new HashSet<>(extractAttachmentPaths(oldContent));
        removedPaths.removeAll(extractAttachmentPaths(newContent));
        if (removedPaths.isEmpty()) {
            return;
        }
        Set<String> otherReferences = collectAttachmentPaths(otherNotesContents);
        for (String path : removedPaths) {
            if (!otherReferences.contains(path)) {
                deleteAttachmentIfExists(path);
            }
        }
    }

    /**
     * 删除笔记后，清理仅被这些笔记引用的附件
     */
    public static void cleanupAttachmentsForDeletedNotes(Collection<String> deletedNotesContents,
                                                         Collection<String> remainingNotesContents) {
        Set<String> deletedPaths = collectAttachmentPaths(deletedNotesContents);
        if (deletedPaths.isEmpty()) {
            return;
        }
        Set<String> remainingReferences = collectAttachmentPaths(remainingNotesContents);
        for (String path : deletedPaths) {
            if (!remainingReferences.contains(path)) {
                deleteAttachmentIfExists(path);
            }
        }
    }

    public static boolean deleteAttachmentIfExists(String relativePath) {
        String normalized = normalizeAttachmentPath(relativePath);
        if (normalized == null) {
            return false;
        }
        try {
            File file = resolveImageFile(normalized);
            if (file == null || !file.exists()) {
                return false;
            }
            File attachmentsDir = getAttachmentsDir().getCanonicalFile();
            if (!file.getCanonicalFile().toPath().startsWith(attachmentsDir.toPath())) {
                log.warn("拒绝删除 attachments 目录外的文件: {}", file);
                return false;
            }
            boolean deleted = FileUtil.del(file);
            if (deleted) {
                log.info("已删除未引用附件: {}", normalized);
            }
            return deleted;
        } catch (IOException e) {
            log.error("删除附件失败: {}, {}", normalized, ExceptionUtils.getStackTrace(e));
            return false;
        }
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

    private static void addAttachmentPathsFromMatcher(Set<String> paths, Matcher matcher) {
        while (matcher.find()) {
            String normalized = normalizeAttachmentPath(matcher.group(1));
            if (normalized != null) {
                paths.add(normalized);
            }
        }
    }

    private static String normalizeAttachmentPath(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String normalized = path.trim().replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith(ATTACHMENTS_RELATIVE_PREFIX)) {
            return null;
        }
        String fileName = normalized.substring(ATTACHMENTS_RELATIVE_PREFIX.length());
        if (StringUtils.isBlank(fileName) || fileName.contains("..") || fileName.contains("/")) {
            return null;
        }
        if (!isAttachmentImageFileName(fileName)) {
            return null;
        }
        return ATTACHMENTS_RELATIVE_PREFIX + fileName;
    }

    private static boolean isAttachmentImageFileName(String fileName) {
        return IMAGE_EXTENSIONS.contains(normalizeImageExtension(FileUtil.extName(fileName)));
    }

    private static File resolveImageFile(String src) {
        String normalized = normalizeAttachmentPath(src);
        if (normalized == null && StringUtils.isNotBlank(src)) {
            String trimmed = src.trim().replace('\\', '/');
            if (trimmed.startsWith(ATTACHMENTS_RELATIVE_PREFIX)) {
                return new File(SystemUtil.CONFIG_HOME, trimmed);
            }
            if (trimmed.startsWith("/" + ATTACHMENTS_DIR_NAME + "/")) {
                return new File(SystemUtil.CONFIG_HOME, trimmed.substring(1));
            }
            File file = new File(trimmed);
            if (file.isAbsolute()) {
                return file;
            }
            return new File(getAttachmentsDir(), trimmed);
        }
        if (normalized == null) {
            return null;
        }
        return new File(getAttachmentsDir(), normalized.substring(ATTACHMENTS_RELATIVE_PREFIX.length()));
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
