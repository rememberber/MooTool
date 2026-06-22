package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Vault 列表的 .gitignore 可见性过滤（对标 Tolaria vault/ignored.rs）。
 */
@Slf4j
public final class QuickNoteGitIgnoreUtil {

    private QuickNoteGitIgnoreUtil() {
    }

    public static boolean shouldHideGitignoredFiles() {
        return App.config.isQuickNoteHideGitignoredFiles();
    }

    public static boolean hasGitignoreFile(File vaultDir) {
        if (vaultDir == null) {
            return false;
        }
        File rootIgnore = new File(vaultDir, ".gitignore");
        if (rootIgnore.isFile()) {
            return true;
        }
        File[] children = vaultDir.listFiles();
        if (children == null) {
            return false;
        }
        for (File child : children) {
            if (child.isFile() && ".gitignore".equals(child.getName())) {
                return true;
            }
            if (child.isDirectory() && !child.getName().startsWith(".") && hasGitignoreInTree(child)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> filterVisiblePaths(File vaultDir, List<String> relativePaths) {
        return filterVisiblePaths(vaultDir, relativePaths, shouldHideGitignoredFiles());
    }

    public static List<String> filterVisiblePaths(File vaultDir, List<String> relativePaths,
                                                    boolean hideGitignoredFiles) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return List.of();
        }
        if (!hideGitignoredFiles || !hasGitignoreFile(vaultDir)) {
            return relativePaths;
        }
        Set<String> ignored = findIgnoredPaths(vaultDir, relativePaths);
        return relativePaths.stream()
                .filter(path -> !ignored.contains(normalizeRelativePath(path)))
                .collect(Collectors.toList());
    }

    public static List<String> filterVisibleFolders(File vaultDir, List<String> folders) {
        if (folders == null || folders.isEmpty()) {
            return List.of();
        }
        if (!shouldHideGitignoredFiles() || !hasGitignoreFile(vaultDir)) {
            return folders;
        }
        List<String> candidates = new ArrayList<>();
        for (String folder : folders) {
            String normalized = normalizeRelativePath(folder);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            candidates.add(normalized);
            candidates.add(normalized + "/");
        }
        Set<String> ignored = findIgnoredPaths(vaultDir, candidates);
        return folders.stream()
                .filter(folder -> !isFolderHidden(folder, ignored))
                .collect(Collectors.toList());
    }

    static Set<String> findIgnoredPaths(File vaultDir, List<String> relativePaths) {
        List<String> candidates = relativePaths.stream()
                .map(QuickNoteGitIgnoreUtil::normalizeRelativePath)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return Set.of();
        }
        try {
            ProcessBuilder builder = new ProcessBuilder("git", "check-ignore", "--no-index", "--stdin");
            builder.directory(vaultDir);
            builder.redirectErrorStream(false);
            Process process = builder.start();
            try (OutputStream stdin = process.getOutputStream()) {
                for (String path : candidates) {
                    stdin.write((path + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!stdout.isEmpty()) {
                        stdout.append('\n');
                    }
                    stdout.append(line);
                }
            }
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Set.of();
            }
            int exitCode = process.exitValue();
            if (exitCode != 0 && exitCode != 1) {
                return Set.of();
            }
            Set<String> ignored = new LinkedHashSet<>();
            for (String line : stdout.toString().split("\n")) {
                String normalized = normalizeRelativePath(line);
                if (StringUtils.isNotBlank(normalized)) {
                    ignored.add(normalized);
                    if (normalized.endsWith("/")) {
                        ignored.add(normalized.substring(0, normalized.length() - 1));
                    }
                }
            }
            return ignored;
        } catch (Exception e) {
            log.debug("git check-ignore failed: {}", e.getMessage());
            return Set.of();
        }
    }

    private static boolean hasGitignoreInTree(File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File child : children) {
            if (child.isFile() && ".gitignore".equals(child.getName())) {
                return true;
            }
            if (child.isDirectory() && !".git".equals(child.getName()) && hasGitignoreInTree(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFolderHidden(String folder, Set<String> ignored) {
        String normalized = normalizeRelativePath(folder);
        if (ignored.contains(normalized) || ignored.contains(normalized + "/")) {
            return true;
        }
        int index = normalized.indexOf('/');
        while (index > 0) {
            String prefix = normalized.substring(0, index);
            if (ignored.contains(prefix) || ignored.contains(prefix + "/")) {
                return true;
            }
            index = normalized.indexOf('/', index + 1);
        }
        return false;
    }

    static String normalizeRelativePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/').trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
