package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tesseract 运行环境检测与配置
 */
@Slf4j
public class TesseractEnvUtil {

    private static volatile boolean configured;
    private static volatile String tesseractCommand;
    private static volatile String nativeLibPath;

    private TesseractEnvUtil() {
    }

    public static void ensureConfigured() {
        if (configured) {
            return;
        }
        synchronized (TesseractEnvUtil.class) {
            if (configured) {
                return;
            }
            nativeLibPath = findNativeLibPath();
            if (StringUtils.isNotBlank(nativeLibPath)) {
                prependJnaLibraryPath(nativeLibPath);
                log.info("Tesseract native lib path: {}", nativeLibPath);
            }
            tesseractCommand = findTesseractCommand();
            if (StringUtils.isNotBlank(tesseractCommand)) {
                log.info("Tesseract command: {}", tesseractCommand);
            }
            configured = true;
        }
    }

    public static boolean isAvailable() {
        ensureConfigured();
        return StringUtils.isNotBlank(tesseractCommand) || StringUtils.isNotBlank(nativeLibPath);
    }

    public static String getTesseractCommand() {
        ensureConfigured();
        return tesseractCommand;
    }

    public static String getInstallHint() {
        if (SystemUtil.isMacOs()) {
            return "macOS 请先安装 Tesseract：\n\n"
                    + "brew install tesseract tesseract-lang\n\n"
                    + "安装完成后重启 MooTool 再试。";
        }
        if (SystemUtil.isWindowsOs()) {
            return "Windows 请先安装 Tesseract：\n\n"
                    + "https://github.com/UB-Mannheim/tesseract/wiki\n\n"
                    + "安装时勾选中文语言包，安装完成后重启 MooTool。";
        }
        return "Linux 请先安装 Tesseract，例如：\n\n"
                + "sudo apt install tesseract-ocr tesseract-ocr-chi-sim\n\n"
                + "安装完成后重启 MooTool 再试。";
    }

    public static String ocrViaCli(File imageFile, String tessdataPath, String language) throws Exception {
        ensureConfigured();
        if (StringUtils.isBlank(tesseractCommand)) {
            return null;
        }
        List<String> command = new ArrayList<>();
        command.add(tesseractCommand);
        command.add(imageFile.getAbsolutePath());
        command.add("stdout");
        command.add("-l");
        command.add(language);
        if (StringUtils.isNotBlank(tessdataPath)) {
            command.add("--tessdata-dir");
            command.add(tessdataPath);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        }
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Tesseract 识别超时");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(output, "Tesseract 执行失败"));
        }
        return output;
    }

    private static String findTesseractCommand() {
        Set<String> candidates = new LinkedHashSet<>();
        String pathEnv = System.getenv("PATH");
        if (StringUtils.isNotBlank(pathEnv)) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                if (StringUtils.isBlank(dir)) {
                    continue;
                }
                candidates.add(FileUtil.file(dir, SystemUtil.isWindowsOs() ? "tesseract.exe" : "tesseract").getAbsolutePath());
            }
        }
        if (SystemUtil.isMacOs()) {
            candidates.add("/opt/homebrew/bin/tesseract");
            candidates.add("/usr/local/bin/tesseract");
        } else if (SystemUtil.isWindowsOs()) {
            candidates.add("C:\\Program Files\\Tesseract-OCR\\tesseract.exe");
            candidates.add("C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe");
        } else if (SystemUtil.isLinuxOs()) {
            candidates.add("/usr/bin/tesseract");
            candidates.add("/usr/local/bin/tesseract");
        }

        for (String candidate : candidates) {
            File file = FileUtil.file(candidate);
            if (file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private static String findNativeLibPath() {
        Set<String> candidates = new LinkedHashSet<>();
        if (SystemUtil.isMacOs()) {
            candidates.add("/opt/homebrew/lib");
            candidates.add("/usr/local/lib");
            candidates.add(findHomebrewCellarLib("tesseract"));
        } else if (SystemUtil.isWindowsOs()) {
            candidates.add("C:\\Program Files\\Tesseract-OCR");
            candidates.add("C:\\Program Files (x86)\\Tesseract-OCR");
        } else if (SystemUtil.isLinuxOs()) {
            candidates.add("/usr/lib/x86_64-linux-gnu");
            candidates.add("/usr/lib64");
            candidates.add("/usr/lib");
            candidates.add("/usr/local/lib");
        }

        String tessPrefix = System.getenv("TESSDATA_PREFIX");
        if (StringUtils.isNotBlank(tessPrefix)) {
            File prefixDir = FileUtil.file(tessPrefix);
            if (prefixDir.getParentFile() != null) {
                candidates.add(FileUtil.file(prefixDir.getParentFile(), "lib").getAbsolutePath());
            }
        }

        for (String candidate : candidates) {
            if (hasNativeLibs(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String findHomebrewCellarLib(String formula) {
        String[] prefixes = {"/opt/homebrew/Cellar/" + formula, "/usr/local/Cellar/" + formula};
        for (String prefix : prefixes) {
            File cellarDir = FileUtil.file(prefix);
            if (!cellarDir.isDirectory()) {
                continue;
            }
            File[] versions = cellarDir.listFiles(File::isDirectory);
            if (versions == null || versions.length == 0) {
                continue;
            }
            Arrays.sort(versions, Comparator.comparing(File::getName).reversed());
            for (File versionDir : versions) {
                File libDir = FileUtil.file(versionDir, "lib");
                if (hasNativeLibs(libDir.getAbsolutePath())) {
                    return libDir.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static boolean hasNativeLibs(String libDirPath) {
        if (StringUtils.isBlank(libDirPath) || !FileUtil.isDirectory(libDirPath)) {
            return false;
        }
        File libDir = FileUtil.file(libDirPath);
        File[] files = libDir.listFiles();
        if (files == null) {
            return false;
        }
        boolean hasTesseract = false;
        boolean hasLeptonica = false;
        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (name.contains("tesseract") && (name.endsWith(".dylib") || name.endsWith(".so") || name.endsWith(".dll"))) {
                hasTesseract = true;
            }
            if (name.contains("leptonica") && (name.endsWith(".dylib") || name.endsWith(".so") || name.endsWith(".dll"))) {
                hasLeptonica = true;
            }
        }
        return hasTesseract && hasLeptonica;
    }

    private static void prependJnaLibraryPath(String path) {
        String existing = System.getProperty("jna.library.path");
        if (StringUtils.isBlank(existing)) {
            System.setProperty("jna.library.path", path);
            return;
        }
        if (existing.contains(path)) {
            return;
        }
        System.setProperty("jna.library.path", path + File.pathSeparator + existing);
    }
}
