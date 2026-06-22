package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TJsonBeautyMapper;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON Vault：以 json 文件管理 JSON 片段，并在目录内启用 Git。
 */
@Slf4j
public final class JsonBeautyVaultUtil {

    public static final String VAULT_DIR_NAME = "json-beauty";
    public static final String MIGRATION_MARKER = ".migrated-from-db";
    public static final String JSON_EXTENSION = ".json";

    private static volatile boolean vaultInitialized;
    private static final ThreadLocal<Boolean> vaultInitializing = ThreadLocal.withInitial(() -> false);

    private JsonBeautyVaultUtil() {
    }

    public static File getVaultDir() {
        String configured = App.config.getJsonBeautyVaultPath();
        if (StringUtils.isNotBlank(configured)) {
            return FileUtil.mkdir(configured);
        }
        return FileUtil.mkdir(SystemUtil.CONFIG_HOME + File.separator + VAULT_DIR_NAME);
    }

    public static String getDefaultVaultPath() {
        return SystemUtil.CONFIG_HOME + File.separator + VAULT_DIR_NAME;
    }

    public static void ensureVaultReady() {
        if (vaultInitialized) {
            return;
        }
        if (Boolean.TRUE.equals(vaultInitializing.get())) {
            return;
        }
        synchronized (JsonBeautyVaultUtil.class) {
            if (vaultInitialized) {
                return;
            }
            vaultInitializing.set(true);
            try {
                File vaultDir = getVaultDir();
                FileUtil.mkdir(vaultDir);
                migrateFromDatabaseIfNeeded(vaultDir);
                seedWelcomeJsonIfEmpty(vaultDir);
                QuickNoteGitUtil.initRepoIfNeeded(vaultDir);
                syncConfiguredRemote(vaultDir);
                vaultInitialized = true;
            } finally {
                vaultInitializing.set(false);
            }
        }
    }

    public static void resetVaultCache() {
        synchronized (JsonBeautyVaultUtil.class) {
            vaultInitialized = false;
        }
    }

    public static List<TJsonBeauty> listAll() {
        ensureVaultReady();
        List<TJsonBeauty> items = new ArrayList<>();
        collectJsonFiles(getVaultDir(), "", items);
        items.sort(Comparator.comparing(TJsonBeauty::getModifiedTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return filterVisibleItems(items);
    }

    public static List<TJsonBeauty> listByFilter(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(normalized)) {
            return listAll();
        }
        return listAll().stream()
                .filter(item -> StringUtils.defaultString(item.getName()).toLowerCase(Locale.ROOT).contains(normalized)
                        || StringUtils.defaultString(item.getRelativePath()).toLowerCase(Locale.ROOT).contains(normalized))
                .collect(Collectors.toList());
    }

    public static TJsonBeauty loadByPath(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            return null;
        }
        File file = toAbsoluteFile(relativePath);
        if (!file.isFile()) {
            return null;
        }
        return readJsonFile(file, normalizeRelativePath(relativePath));
    }

    public static TJsonBeauty loadByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return listAll().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElse(null);
    }

    public static TJsonBeauty createJson(String title) {
        ensureVaultReady();
        String name = StringUtils.defaultIfBlank(title, NamingUtil.defaultUntitledName());
        String relativePath = buildUniqueRelativePath(sanitizeFileName(name));
        String now = SqliteUtil.nowDateForSqlite();

        TJsonBeauty item = new TJsonBeauty();
        item.setRelativePath(relativePath);
        item.setName(name);
        item.setContent("");
        item.setCreateTime(now);
        item.setModifiedTime(now);
        saveJson(item, "");
        return item;
    }

    public static void saveJson(TJsonBeauty item, String content) {
        if (item == null || StringUtils.isBlank(item.getRelativePath())) {
            throw new IllegalArgumentException("json path is required");
        }
        ensureVaultReady();
        File file = toAbsoluteFile(item.getRelativePath());
        FileUtil.mkParentDirs(file);
        FileUtil.writeString(content == null ? "" : content, file, StandardCharsets.UTF_8);
    }

    public static String renameJson(String oldRelativePath, String newTitle) {
        if (StringUtils.isBlank(oldRelativePath) || StringUtils.isBlank(newTitle)) {
            return oldRelativePath;
        }
        TJsonBeauty existing = loadByPath(oldRelativePath);
        if (existing == null) {
            return oldRelativePath;
        }
        String newRelativePath = buildUniqueRelativePath(sanitizeFileName(newTitle), oldRelativePath);
        File oldFile = toAbsoluteFile(oldRelativePath);
        File newFile = toAbsoluteFile(newRelativePath);
        FileUtil.mkParentDirs(newFile);
        FileUtil.move(oldFile, newFile, true);
        return newRelativePath;
    }

    public static void deleteByPath(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            return;
        }
        File file = toAbsoluteFile(relativePath);
        if (file.isFile()) {
            FileUtil.del(file);
        }
    }

    public static void openVaultDir() {
        ensureVaultReady();
        openDirectory(getVaultDir());
    }

    public static void revealInFileManager(String relativePath) {
        ensureVaultReady();
        if (StringUtils.isBlank(relativePath)) {
            openVaultDir();
            return;
        }
        File file = toAbsoluteFile(relativePath);
        if (file.isFile()) {
            revealFile(file);
        } else if (file.getParentFile() != null) {
            openDirectory(file.getParentFile());
        }
    }

    public static String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        String normalized = relativePath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public static String sanitizeFileName(String name) {
        String sanitized = StringUtils.defaultIfBlank(name, NamingUtil.defaultUntitledName())
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();
        return StringUtils.defaultIfBlank(sanitized, NamingUtil.defaultUntitledName());
    }

    public static File toAbsoluteFile(String relativePath) {
        return new File(getVaultDir(), normalizeRelativePath(relativePath));
    }

    private static void collectJsonFiles(File dir, String relativePrefix, List<TJsonBeauty> items) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String name = child.getName();
            if (shouldSkipEntry(name)) {
                continue;
            }
            String childRelative = joinRelativePath(relativePrefix, name);
            if (child.isDirectory()) {
                collectJsonFiles(child, childRelative, items);
            } else if (child.isFile() && name.endsWith(JSON_EXTENSION)) {
                items.add(readJsonFile(child, childRelative));
            }
        }
    }

    private static TJsonBeauty readJsonFile(File file, String relativePath) {
        TJsonBeauty item = new TJsonBeauty();
        item.setRelativePath(normalizeRelativePath(relativePath));
        item.setName(fileNameWithoutExtension(relativePath));
        item.setContent(FileUtil.readString(file, StandardCharsets.UTF_8));
        String modifiedTime = formatFileTime(file.lastModified());
        item.setCreateTime(modifiedTime);
        item.setModifiedTime(modifiedTime);
        return item;
    }

    private static void migrateFromDatabaseIfNeeded(File vaultDir) {
        File marker = new File(vaultDir, MIGRATION_MARKER);
        if (marker.exists()) {
            return;
        }
        try {
            TJsonBeautyMapper mapper = MybatisUtil.getSqlSession().getMapper(TJsonBeautyMapper.class);
            List<TJsonBeauty> dbItems = mapper.selectAll();
            for (TJsonBeauty dbItem : dbItems) {
                String title = StringUtils.defaultIfBlank(dbItem.getName(), NamingUtil.defaultUntitledName());
                String relativePath = buildUniqueRelativePath(sanitizeFileName(title));
                File file = new File(vaultDir, relativePath);
                FileUtil.writeString(StringUtils.defaultString(dbItem.getContent()), file, StandardCharsets.UTF_8);
            }
            Files.writeString(marker.toPath(), SqliteUtil.nowDateForSqlite(), StandardCharsets.UTF_8);
            if (!dbItems.isEmpty()) {
                QuickNoteGitUtil.commit(vaultDir, "Migrated " + dbItems.size() + " JSON item(s) from database");
            }
            log.info("Migrated {} JSON items from database to vault", dbItems.size());
        } catch (Exception e) {
            log.warn("JSON DB migration failed: {}", e.getMessage());
        }
    }

    private static void seedWelcomeJsonIfEmpty(File vaultDir) {
        File[] children = vaultDir.listFiles(pathname ->
                pathname.isFile() && pathname.getName().endsWith(JSON_EXTENSION));
        if (children != null && children.length > 0) {
            return;
        }
        File welcome = new File(vaultDir, "关于JSON.json");
        FileUtil.writeString("""
                {
                  "name": "MooTool JSON",
                  "tip": "点击加号按钮，开始保存常用 JSON"
                }
                """, welcome, StandardCharsets.UTF_8);
    }

    private static void syncConfiguredRemote(File vaultDir) {
        String remoteUrl = App.config.getJsonBeautyGitRemoteUrl();
        if (StringUtils.isNotBlank(remoteUrl)) {
            QuickNoteGitUtil.configureRemote(vaultDir, remoteUrl);
        }
    }

    private static String buildUniqueRelativePath(String baseName) {
        return buildUniqueRelativePath(baseName, null);
    }

    private static String buildUniqueRelativePath(String baseName, String excludePath) {
        String candidate = baseName + JSON_EXTENSION;
        if (excludePath != null && candidate.equals(excludePath)) {
            return candidate;
        }
        if (loadByPath(candidate) == null && !toAbsoluteFile(candidate).exists()) {
            return candidate;
        }
        int index = 1;
        while (true) {
            String numbered = baseName + "-" + index + JSON_EXTENSION;
            if (excludePath != null && numbered.equals(excludePath)) {
                return numbered;
            }
            if (loadByPath(numbered) == null && !toAbsoluteFile(numbered).exists()) {
                return numbered;
            }
            index++;
        }
    }

    private static String fileNameWithoutExtension(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        int slash = normalized.lastIndexOf('/');
        String fileName = slash < 0 ? normalized : normalized.substring(slash + 1);
        if (fileName.endsWith(JSON_EXTENSION)) {
            return fileName.substring(0, fileName.length() - JSON_EXTENSION.length());
        }
        return fileName;
    }

    private static String joinRelativePath(String prefix, String name) {
        if (StringUtils.isBlank(prefix)) {
            return name;
        }
        return normalizeRelativePath(prefix) + "/" + name;
    }

    private static boolean shouldSkipEntry(String name) {
        return ".git".equals(name) || ".DS_Store".equals(name) || MIGRATION_MARKER.equals(name);
    }

    private static String formatFileTime(long lastModified) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastModified));
    }

    private static List<TJsonBeauty> filterVisibleItems(List<TJsonBeauty> items) {
        if (items.isEmpty()) {
            return items;
        }
        List<String> paths = items.stream()
                .map(TJsonBeauty::getRelativePath)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Set<String> visible = new java.util.LinkedHashSet<>(
                QuickNoteGitIgnoreUtil.filterVisiblePaths(
                        getVaultDir(), paths, App.config.isJsonBeautyHideGitignoredFiles()));
        return items.stream()
                .filter(item -> visible.contains(item.getRelativePath()))
                .collect(Collectors.toList());
    }

    private static void openDirectory(File dir) {
        try {
            Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            log.warn("Open directory failed: {}", e.getMessage());
        }
    }

    private static void revealFile(File file) {
        try {
            if (SystemUtil.isMacOs()) {
                new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
            } else if (SystemUtil.isWindowsOs()) {
                new ProcessBuilder("explorer", "/select,", file.getAbsolutePath()).start();
            } else if (file.getParentFile() != null) {
                openDirectory(file.getParentFile());
            }
        } catch (Exception e) {
            log.warn("Reveal file failed: {}", e.getMessage());
        }
    }
}
