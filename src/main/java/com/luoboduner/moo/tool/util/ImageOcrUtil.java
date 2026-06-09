package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图片 OCR 工具（基于 Tesseract）
 */
@Slf4j
public class ImageOcrUtil {

    private static final String LOCAL_TESSDATA_DIR = SystemUtil.CONFIG_HOME + File.separator + "tessdata";
    private static final String TESSDATA_FAST_BASE_URL = "https://github.com/tesseract-ocr/tessdata_fast/raw/main/";

    public enum LanguageMode {
        CHINESE_ENGLISH("chi_sim+eng", "中英文", "chi_sim", "eng"),
        CHINESE("chi_sim", "中文", "chi_sim"),
        ENGLISH("eng", "英文", "eng");

        @Getter
        private final String tesseractLang;

        @Getter
        private final String label;

        private final String[] requiredDataFiles;

        LanguageMode(String tesseractLang, String label, String... requiredDataFiles) {
            this.tesseractLang = tesseractLang;
            this.label = label;
            this.requiredDataFiles = requiredDataFiles;
        }

        public String[] getRequiredDataFiles() {
            return requiredDataFiles;
        }
    }

    @Getter
    @Setter
    public static class OcrOptions {
        private LanguageMode languageMode = LanguageMode.CHINESE_ENGLISH;
        private boolean preprocess = true;
    }

    @Getter
    public static class OcrResult {
        private final boolean success;
        private final String text;
        private final String errorMessage;

        private OcrResult(boolean success, String text, String errorMessage) {
            this.success = success;
            this.text = text;
            this.errorMessage = errorMessage;
        }

        public static OcrResult ok(String text) {
            return new OcrResult(true, StringUtils.defaultString(text).trim(), null);
        }

        public static OcrResult fail(String errorMessage) {
            return new OcrResult(false, null, errorMessage);
        }
    }

    public static String prepareEngine(LanguageMode languageMode) throws IOException {
        TesseractEnvUtil.ensureConfigured();
        if (!TesseractEnvUtil.isAvailable()) {
            throw new IOException(TesseractEnvUtil.getInstallHint());
        }
        String tessdataPath = resolveTessdataPath(languageMode);
        if (tessdataPath == null) {
            throw new IOException("未找到 OCR 语言包，请检查网络后重试，或手动安装 Tesseract 语言包");
        }
        return tessdataPath;
    }

    public static OcrResult recognize(File imageFile, OcrOptions options) {
        if (imageFile == null || !imageFile.isFile()) {
            return OcrResult.fail("图片文件不存在");
        }
        File tempFile = null;
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return OcrResult.fail("无法读取图片");
            }
            String tessdataPath = prepareEngine(options.getLanguageMode());
            BufferedImage input = options.isPreprocess() ? preprocess(image) : image;
            File ocrInputFile = imageFile;
            if (options.isPreprocess()) {
                tempFile = File.createTempFile("mootool_ocr_", ".png");
                ImageIO.write(input, "png", tempFile);
                ocrInputFile = tempFile;
            }
            String text = runOcr(ocrInputFile, input, tessdataPath, options.getLanguageMode().getTesseractLang());
            if (StringUtils.isBlank(text)) {
                return OcrResult.fail("未识别到文字");
            }
            return OcrResult.ok(text);
        } catch (UnsatisfiedLinkError e) {
            log.error("OCR native library missing: {}", imageFile.getAbsolutePath(), e);
            return OcrResult.fail(TesseractEnvUtil.getInstallHint());
        } catch (Exception e) {
            log.error("OCR failed: {}", imageFile.getAbsolutePath(), e);
            String message = e.getMessage();
            if (isNativeLibraryError(e) || StringUtils.containsIgnoreCase(message, "UnsatisfiedLinkError")
                    || StringUtils.containsIgnoreCase(message, "libtesseract")) {
                message = TesseractEnvUtil.getInstallHint();
            }
            return OcrResult.fail(message);
        } finally {
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }

    public static String recognizeFiles(List<File> imageFiles, OcrOptions options) {
        StringBuilder builder = new StringBuilder();
        List<String> errors = new ArrayList<>();
        for (File imageFile : imageFiles) {
            OcrResult result = recognize(imageFile, options);
            if (imageFiles.size() > 1) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append("===== ").append(imageFile.getName()).append(" =====\n");
            }
            if (result.isSuccess()) {
                builder.append(result.getText());
            } else {
                errors.add(imageFile.getName() + "：" + result.getErrorMessage());
                builder.append("[识别失败] ").append(result.getErrorMessage());
            }
        }
        if (!errors.isEmpty() && builder.length() == 0) {
            return null;
        }
        return builder.toString().trim();
    }

    private static boolean isNativeLibraryError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnsatisfiedLinkError) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String runOcr(File imageFile, BufferedImage image, String tessdataPath, String language) throws Exception {
        TesseractEnvUtil.ensureConfigured();
        String cliResult = TesseractEnvUtil.ocrViaCli(imageFile, tessdataPath, language);
        if (StringUtils.isNotBlank(cliResult)) {
            return cliResult;
        }
        return doOcrViaTess4j(image, tessdataPath, language);
    }

    private static String doOcrViaTess4j(BufferedImage image, String tessdataPath, String language) throws TesseractException {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        return tesseract.doOCR(image);
    }

    private static String resolveTessdataPath(LanguageMode languageMode) throws IOException {
        String systemPath = findSystemTessdataPath(languageMode);
        if (systemPath != null) {
            return systemPath;
        }

        FileUtil.mkdir(LOCAL_TESSDATA_DIR);
        ensureLanguageDataFiles(LOCAL_TESSDATA_DIR, languageMode);
        if (hasLanguageDataFiles(LOCAL_TESSDATA_DIR, languageMode)) {
            return LOCAL_TESSDATA_DIR;
        }
        return null;
    }

    private static String findSystemTessdataPath(LanguageMode languageMode) {
        Set<String> candidates = new LinkedHashSet<>();
        String tessPrefix = System.getenv("TESSDATA_PREFIX");
        if (StringUtils.isNotBlank(tessPrefix)) {
            candidates.add(tessPrefix);
            candidates.add(tessPrefix + File.separator + "tessdata");
        }
        if (SystemUtil.isMacOs()) {
            candidates.add("/opt/homebrew/share/tessdata");
            candidates.add("/usr/local/share/tessdata");
            addHomebrewTessdataCandidates(candidates);
        } else if (SystemUtil.isWindowsOs()) {
            candidates.add("C:\\Program Files\\Tesseract-OCR\\tessdata");
            candidates.add("C:\\Program Files (x86)\\Tesseract-OCR\\tessdata");
        } else if (SystemUtil.isLinuxOs()) {
            candidates.add("/usr/share/tesseract-ocr/5/tessdata");
            candidates.add("/usr/share/tesseract-ocr/4.00/tessdata");
            candidates.add("/usr/share/tessdata");
        }

        for (String path : candidates) {
            if (hasLanguageDataFiles(path, languageMode)) {
                return path;
            }
        }
        return null;
    }

    private static void addHomebrewTessdataCandidates(Set<String> candidates) {
        String[] prefixes = {"/opt/homebrew/Cellar/tesseract", "/usr/local/Cellar/tesseract"};
        for (String prefix : prefixes) {
            File cellarDir = FileUtil.file(prefix);
            if (!cellarDir.isDirectory()) {
                continue;
            }
            File[] versions = cellarDir.listFiles(File::isDirectory);
            if (versions == null) {
                continue;
            }
            for (File versionDir : versions) {
                candidates.add(FileUtil.file(versionDir, "share/tessdata").getAbsolutePath());
            }
        }
    }

    private static void ensureLanguageDataFiles(String tessdataDir, LanguageMode languageMode) throws IOException {
        for (String langFile : languageMode.getRequiredDataFiles()) {
            File dataFile = FileUtil.file(tessdataDir, langFile + ".traineddata");
            if (dataFile.isFile()) {
                continue;
            }
            String url = TESSDATA_FAST_BASE_URL + langFile + ".traineddata";
            try {
                HttpUtil.downloadFile(url, dataFile);
            } catch (Exception e) {
                log.warn("Download tessdata failed: {}", url, e);
                throw new IOException("下载语言包失败：" + langFile + "（" + e.getMessage() + "）");
            }
        }
    }

    private static boolean hasLanguageDataFiles(String tessdataDir, LanguageMode languageMode) {
        if (StringUtils.isBlank(tessdataDir) || !FileUtil.isDirectory(tessdataDir)) {
            return false;
        }
        for (String langFile : languageMode.getRequiredDataFiles()) {
            if (!FileUtil.isFile(tessdataDir + File.separator + langFile + ".traineddata")) {
                return false;
            }
        }
        return true;
    }

    static BufferedImage preprocess(BufferedImage source) {
        BufferedImage scaled = source;
        int width = source.getWidth();
        int height = source.getHeight();
        if (width < 800 || height < 800) {
            double scale = Math.min(2.0, 1600.0 / Math.max(width, height));
            int newWidth = Math.max(1, (int) Math.round(width * scale));
            int newHeight = Math.max(1, (int) Math.round(height * scale));
            int imageType = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            scaled = new BufferedImage(newWidth, newHeight, imageType);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(source, 0, 0, newWidth, newHeight, null);
            g.dispose();
        }

        BufferedImage gray = new BufferedImage(scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return gray;
    }
}
