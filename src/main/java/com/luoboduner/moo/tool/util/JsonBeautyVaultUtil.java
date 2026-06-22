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
import java.nio.file.Path;
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
        return createJson(title, "");
    }

    public static boolean jsonTitleExistsInFolder(String title, String folderRelativePath) {
        if (StringUtils.isBlank(title)) {
            return false;
        }
        String path = joinRelativePath(normalizeFolderPath(folderRelativePath),
                sanitizeFileName(title) + JSON_EXTENSION);
        return toAbsoluteFile(path).exists();
    }

    public static TJsonBeauty createJson(String title, String folderRelativePath) {
        ensureVaultReady();
        String name = StringUtils.defaultIfBlank(title, NamingUtil.defaultUntitledName());
        String folder = normalizeFolderPath(folderRelativePath);
        String relativePath = buildUniqueRelativePath(folder, sanitizeFileName(name));
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
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
    }

    public static TJsonBeauty duplicateJson(String sourceRelativePath) {
        TJsonBeauty source = loadByPath(sourceRelativePath);
        if (source == null) {
            return null;
        }
        String folder = parentFolder(sourceRelativePath);
        String copyName = source.getName() + I18n.get("quickNote.copySuffix");
        String copyBase = sanitizeFileName(copyName);
        String newRelativePath = buildUniqueRelativePath(folder, copyBase);
        String now = SqliteUtil.nowDateForSqlite();

        TJsonBeauty copy = new TJsonBeauty();
        copy.setRelativePath(newRelativePath);
        copy.setName(copyName);
        copy.setContent(source.getContent());
        copy.setCreateTime(now);
        copy.setModifiedTime(now);
        saveJson(copy, source.getContent());
        return copy;
    }

    public static String renameJson(String oldRelativePath, String newTitle) {
        if (StringUtils.isBlank(oldRelativePath) || StringUtils.isBlank(newTitle)) {
            return oldRelativePath;
        }
        TJsonBeauty existing = loadByPath(oldRelativePath);
        if (existing == null) {
            return oldRelativePath;
        }
        String folder = parentFolder(oldRelativePath);
        String newRelativePath = buildUniqueRelativePath(folder, sanitizeFileName(newTitle), oldRelativePath);
        File oldFile = toAbsoluteFile(oldRelativePath);
        File newFile = toAbsoluteFile(newRelativePath);
        FileUtil.mkParentDirs(newFile);
        FileUtil.move(oldFile, newFile, true);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
        return newRelativePath;
    }

    public static void createFolder(String folderRelativePath) {
        ensureVaultReady();
        File folder = toAbsoluteFolder(folderRelativePath);
        FileUtil.mkdir(folder);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
    }

    public static String moveJsonToFolder(String oldRelativePath, String targetFolderPath) {
        if (StringUtils.isBlank(oldRelativePath)) {
            return oldRelativePath;
        }
        TJsonBeauty existing = loadByPath(oldRelativePath);
        if (existing == null) {
            return oldRelativePath;
        }
        String targetFolder = normalizeFolderPath(targetFolderPath);
        String currentFolder = parentFolder(oldRelativePath);
        if (targetFolder.equals(currentFolder)) {
            return oldRelativePath;
        }
        String baseName = fileNameWithoutExtension(oldRelativePath);
        String newRelativePath = buildUniqueRelativePath(targetFolder, baseName, oldRelativePath);

        File oldFile = toAbsoluteFile(oldRelativePath);
        File newFile = toAbsoluteFile(newRelativePath);
        FileUtil.mkParentDirs(newFile);
        FileUtil.move(oldFile, newFile, true);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
        return newRelativePath;
    }

    public static boolean isFolderEmpty(String folderRelativePath) {
        String folder = normalizeFolderPath(folderRelativePath);
        if (StringUtils.isBlank(folder)) {
            return false;
        }
        for (TJsonBeauty item : listAll()) {
            String parent = parentFolder(item.getRelativePath());
            if (parent.equals(folder) || parent.startsWith(folder + "/")) {
                return false;
            }
        }
        for (String childFolder : listFolders()) {
            if (childFolder.startsWith(folder + "/")) {
                return false;
            }
        }
        return toAbsoluteFolder(folder).isDirectory();
    }

    public static boolean deleteFolderIfEmpty(String folderRelativePath) {
        String folder = normalizeFolderPath(folderRelativePath);
        if (StringUtils.isBlank(folder) || !isFolderEmpty(folder)) {
            return false;
        }
        File dir = toAbsoluteFolder(folder);
        if (!dir.isDirectory()) {
            return false;
        }
        FileUtil.del(dir);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
        return true;
    }

    public static String folderLeafName(String folderRelativePath) {
        String normalized = normalizeFolderPath(folderRelativePath);
        if (StringUtils.isBlank(normalized)) {
            return "";
        }
        int index = normalized.lastIndexOf('/');
        return index < 0 ? normalized : normalized.substring(index + 1);
    }

    public static String renameFolder(String oldFolderPath, String newFolderName) {
        String oldFolder = normalizeFolderPath(oldFolderPath);
        if (StringUtils.isBlank(oldFolder)) {
            throw new IllegalArgumentException("Cannot rename root folder");
        }
        String parent = parentFolder(oldFolder + "/.keep");
        String newLeaf = sanitizeFileName(newFolderName);
        if (StringUtils.isBlank(newLeaf)) {
            throw new IllegalArgumentException("Invalid folder name");
        }
        String newFolder = StringUtils.isBlank(parent) ? newLeaf : parent + "/" + newLeaf;
        if (oldFolder.equals(newFolder)) {
            return oldFolder;
        }
        File oldDir = toAbsoluteFolder(oldFolder);
        File newDir = toAbsoluteFolder(newFolder);
        if (!oldDir.isDirectory()) {
            throw new IllegalStateException("Folder not found: " + oldFolder);
        }
        if (newDir.exists()) {
            throw new IllegalStateException("Folder already exists: " + newFolder);
        }
        FileUtil.move(oldDir, newDir, true);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
        return newFolder;
    }

    public static String moveFolderToFolder(String folderPath, String targetFolderPath) {
        String folder = normalizeFolderPath(folderPath);
        String target = normalizeFolderPath(targetFolderPath);
        if (StringUtils.isBlank(folder)) {
            throw new IllegalArgumentException("Cannot move root folder");
        }
        if (folder.equals(target) || target.startsWith(folder + "/")) {
            throw new IllegalArgumentException("Invalid target folder");
        }
        String leaf = folderLeafName(folder);
        String newFolder = StringUtils.isBlank(target) ? leaf : joinRelativePath(target, leaf);
        newFolder = normalizeFolderPath(newFolder);
        if (folder.equals(newFolder)) {
            return folder;
        }
        File oldDir = toAbsoluteFolder(folder);
        File newDir = toAbsoluteFolder(newFolder);
        if (!oldDir.isDirectory()) {
            throw new IllegalStateException("Folder not found: " + folder);
        }
        if (newDir.exists()) {
            throw new IllegalStateException("Folder already exists: " + newFolder);
        }
        FileUtil.move(oldDir, newDir, true);
        JsonBeautyVaultRefreshCoordinator.markInternalWrite();
        return newFolder;
    }

    public static String remapPathAfterFolderRename(String relativePath, String oldFolder, String newFolder) {
        if (StringUtils.isBlank(relativePath) || StringUtils.isBlank(oldFolder)) {
            return relativePath;
        }
        if (relativePath.equals(oldFolder) || relativePath.startsWith(oldFolder + "/")) {
            return newFolder + relativePath.substring(oldFolder.length());
        }
        return relativePath;
    }

    public static List<String> listJsonPathsUnderFolder(String folderRelativePath) {
        String folder = normalizeFolderPath(folderRelativePath);
        if (StringUtils.isBlank(folder)) {
            return List.of();
        }
        String prefix = folder + "/";
        return listAll().stream()
                .map(TJsonBeauty::getRelativePath)
                .filter(path -> path.startsWith(prefix))
                .collect(Collectors.toList());
    }

    public static List<String> listFolders() {
        ensureVaultReady();
        List<String> folders = new ArrayList<>();
        collectFolders(getVaultDir(), "", folders);
        folders.sort(String::compareToIgnoreCase);
        return QuickNoteGitIgnoreUtil.filterVisibleFolders(
                getVaultDir(), folders, App.config.isJsonBeautyHideGitignoredFiles());
    }

    public static String parentFolder(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        int index = normalized.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        return normalized.substring(0, index);
    }

    public static String normalizeFolderPath(String folderRelativePath) {
        String normalized = normalizeRelativePath(folderRelativePath);
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static void deleteByPath(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            return;
        }
        File file = toAbsoluteFile(relativePath);
        if (file.isFile()) {
            FileUtil.del(file);
            JsonBeautyVaultRefreshCoordinator.markInternalWrite();
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
        String cleaned = relativePath.replace('\\', '/').replaceAll("^/+", "");
        if (cleaned.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid JSON path");
        }
        if (StringUtils.isBlank(cleaned)) {
            return "";
        }
        String normalized = Path.of(cleaned).normalize().toString().replace('\\', '/');
        if (".".equals(normalized)) {
            return "";
        }
        if (normalized.equals("..") || normalized.startsWith("../")) {
            throw new IllegalArgumentException("JSON path escapes vault: " + relativePath);
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
        return resolveVaultChild(normalizeRelativePath(relativePath));
    }

    private static File toAbsoluteFolder(String folderRelativePath) {
        return resolveVaultChild(normalizeFolderPath(folderRelativePath));
    }

    private static File resolveVaultChild(String relativePath) {
        File vaultDir = getVaultDir();
        Path vaultPath = vaultDir.toPath().toAbsolutePath().normalize();
        Path childPath = vaultPath.resolve(StringUtils.defaultString(relativePath)).normalize();
        if (!childPath.startsWith(vaultPath)) {
            throw new IllegalArgumentException("JSON path escapes vault: " + relativePath);
        }
        return childPath.toFile();
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
                String relativePath = buildUniqueRelativePath("", sanitizeFileName(title));
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

    private static void collectFolders(File dir, String relativePrefix, List<String> folders) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String name = child.getName();
            if (shouldSkipEntry(name) || !child.isDirectory()) {
                continue;
            }
            String childRelative = joinRelativePath(relativePrefix, name);
            folders.add(childRelative);
            collectFolders(child, childRelative, folders);
        }
    }

    private static String buildUniqueRelativePath(String folder, String baseName) {
        return buildUniqueRelativePath(folder, baseName, null);
    }

    private static String buildUniqueRelativePath(String folder, String baseName, String excludePath) {
        String candidate = joinRelativePath(folder, baseName + JSON_EXTENSION);
        if (excludePath != null && candidate.equals(excludePath)) {
            return candidate;
        }
        if (!toAbsoluteFile(candidate).exists()) {
            return candidate;
        }
        int index = 1;
        while (true) {
            String numbered = joinRelativePath(folder, baseName + "-" + index + JSON_EXTENSION);
            if (excludePath != null && numbered.equals(excludePath)) {
                return numbered;
            }
            if (!toAbsoluteFile(numbered).exists()) {
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
