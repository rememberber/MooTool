package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 随手记 Vault：以 txt 文件 + YAML frontmatter 管理笔记，支持文件夹嵌套。
 */
@Slf4j
public final class QuickNoteVaultUtil {

    public static final String VAULT_DIR_NAME = "quick-notes";
    public static final String MIGRATION_MARKER = ".migrated-from-db";
    public static final String TXT_EXTENSION = ".txt";

    private static volatile boolean vaultInitialized;
    private static final ThreadLocal<Boolean> vaultInitializing = ThreadLocal.withInitial(() -> false);

    private QuickNoteVaultUtil() {
    }

    public static File getVaultDir() {
        String configured = App.config.getQuickNoteVaultPath();
        if (StringUtils.isNotBlank(configured)) {
            return FileUtil.mkdir(configured);
        }
        return FileUtil.mkdir(SystemUtil.CONFIG_HOME + File.separator + VAULT_DIR_NAME);
    }

    public static void ensureVaultReady() {
        if (vaultInitialized) {
            return;
        }
        if (Boolean.TRUE.equals(vaultInitializing.get())) {
            return;
        }
        synchronized (QuickNoteVaultUtil.class) {
            if (vaultInitialized) {
                return;
            }
            vaultInitializing.set(true);
            try {
                File vaultDir = getVaultDir();
                FileUtil.mkdir(vaultDir);
                migrateFromDatabaseIfNeeded(vaultDir);
                seedWelcomeNoteIfEmpty(vaultDir);
                QuickNoteGitUtil.initRepoIfNeeded(vaultDir);
                syncConfiguredRemote(vaultDir);
                vaultInitialized = true;
            } finally {
                vaultInitializing.set(false);
            }
        }
    }

    public static void resetVaultCache() {
        synchronized (QuickNoteVaultUtil.class) {
            vaultInitialized = false;
        }
    }

    public static List<TQuickNote> listAll() {
        ensureVaultReady();
        List<TQuickNote> notes = new ArrayList<>();
        collectNotes(getVaultDir(), "", notes);
        notes.sort(Comparator.comparing(TQuickNote::getModifiedTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return filterVisibleNotes(notes);
    }

    public static List<TQuickNote> listByFilter(String keyword, boolean includeContent) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(normalized)) {
            return listAll();
        }
        return listAll().stream()
                .filter(note -> matchesFilter(note, normalized, includeContent))
                .collect(Collectors.toList());
    }

    private static boolean matchesFilter(TQuickNote note, String keyword, boolean includeContent) {
        if (note.getName() != null && note.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        if (note.getRelativePath() != null && note.getRelativePath().toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        return includeContent && note.getContent() != null
                && note.getContent().toLowerCase(Locale.ROOT).contains(keyword);
    }

    public static TQuickNote loadByPath(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            return null;
        }
        File file = toAbsoluteFile(relativePath);
        if (!file.isFile()) {
            return null;
        }
        return readNoteFile(file, normalizeRelativePath(relativePath));
    }

    public static TQuickNote loadByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return listAll().stream()
                .filter(note -> name.equals(note.getName()))
                .findFirst()
                .orElse(null);
    }

    public static void saveNote(TQuickNote note, String body) {
        if (note == null || StringUtils.isBlank(note.getRelativePath())) {
            throw new IllegalArgumentException("note path is required");
        }
        ensureVaultReady();
        File file = toAbsoluteFile(note.getRelativePath());
        FileUtil.mkParentDirs(file);

        Map<String, Object> metadata = buildMetadata(note);
        String raw = QuickNoteFrontmatter.serialize(metadata, body == null ? "" : body);
        FileUtil.writeString(raw, file, StandardCharsets.UTF_8);
        QuickNoteVaultRefreshCoordinator.markInternalWrite();
    }

    public static void saveMetadata(TQuickNote note) {
        TQuickNote existing = loadByPath(note.getRelativePath());
        String body = existing != null ? existing.getContent() : "";
        saveNote(note, body);
    }

    public static TQuickNote createNote(String title, String folderRelativePath) {
        ensureVaultReady();
        String sanitizedTitle = sanitizeFileName(title);
        String folder = normalizeFolderPath(folderRelativePath);
        String relativePath = buildUniqueRelativePath(folder, sanitizedTitle);
        String now = SqliteUtil.nowDateForSqlite();

        TQuickNote note = new TQuickNote();
        note.setRelativePath(relativePath);
        note.setName(title);
        note.setContent("");
        note.setCreateTime(now);
        note.setModifiedTime(now);
        note.setSyntax(SyntaxConstants.SYNTAX_STYLE_NONE);
        note.setFontName(App.config.getQuickNoteFontName());
        int fontSize = App.config.getQuickNoteFontSize();
        note.setFontSize(fontSize > 0 ? String.valueOf(fontSize) : "14");
        note.setColor("default");
        note.setLineWrap("0");

        saveNote(note, "");
        return note;
    }

    public static TQuickNote duplicateNote(String sourceRelativePath) {
        TQuickNote source = loadByPath(sourceRelativePath);
        if (source == null) {
            return null;
        }
        String folder = parentFolder(sourceRelativePath);
        String copyName = source.getName() + I18n.get("quickNote.copySuffix");
        String copyBase = sanitizeFileName(copyName);
        String newRelativePath = buildUniqueRelativePath(folder, copyBase);
        String now = SqliteUtil.nowDateForSqlite();

        TQuickNote copy = new TQuickNote();
        copy.setRelativePath(newRelativePath);
        copy.setName(copyName);
        copy.setContent(source.getContent());
        copy.setCreateTime(now);
        copy.setModifiedTime(now);
        copy.setSyntax(defaultString(source.getSyntax(), SyntaxConstants.SYNTAX_STYLE_NONE));
        copy.setFontName(defaultString(source.getFontName(), App.config.getQuickNoteFontName()));
        copy.setFontSize(defaultString(source.getFontSize(),
                String.valueOf(Math.max(App.config.getQuickNoteFontSize(), 14))));
        copy.setColor(defaultString(source.getColor(), "default"));
        copy.setLineWrap(defaultString(source.getLineWrap(), "0"));

        saveNote(copy, source.getContent());
        return copy;
    }

    public static void deleteByPath(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            return;
        }
        File file = toAbsoluteFile(relativePath);
        if (file.isFile()) {
            FileUtil.del(file);
            QuickNoteVaultRefreshCoordinator.markInternalWrite();
        }
    }

    public static String renameNote(String oldRelativePath, String newTitle) {
        if (StringUtils.isBlank(oldRelativePath) || StringUtils.isBlank(newTitle)) {
            return oldRelativePath;
        }
        TQuickNote existing = loadByPath(oldRelativePath);
        if (existing == null) {
            return oldRelativePath;
        }
        String folder = parentFolder(oldRelativePath);
        String newRelativePath = buildUniqueRelativePath(folder, sanitizeFileName(newTitle), oldRelativePath);

        File oldFile = toAbsoluteFile(oldRelativePath);
        File newFile = toAbsoluteFile(newRelativePath);
        FileUtil.mkParentDirs(newFile);
        FileUtil.move(oldFile, newFile, true);
        QuickNoteVaultRefreshCoordinator.markInternalWrite();

        existing.setRelativePath(newRelativePath);
        existing.setName(newTitle);
        existing.setModifiedTime(SqliteUtil.nowDateForSqlite());
        saveMetadata(existing);
        return newRelativePath;
    }

    public static void createFolder(String folderRelativePath) {
        ensureVaultReady();
        File folder = toAbsoluteFolder(folderRelativePath);
        FileUtil.mkdir(folder);
    }

    public static String moveNoteToFolder(String oldRelativePath, String targetFolderPath) {
        if (StringUtils.isBlank(oldRelativePath)) {
            return oldRelativePath;
        }
        TQuickNote existing = loadByPath(oldRelativePath);
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
        QuickNoteVaultRefreshCoordinator.markInternalWrite();

        existing.setRelativePath(newRelativePath);
        existing.setModifiedTime(SqliteUtil.nowDateForSqlite());
        saveMetadata(existing);
        return newRelativePath;
    }

    public static boolean isFolderEmpty(String folderRelativePath) {
        String folder = normalizeFolderPath(folderRelativePath);
        if (StringUtils.isBlank(folder)) {
            return false;
        }
        for (TQuickNote note : listAll()) {
            String parent = parentFolder(note.getRelativePath());
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
        QuickNoteVaultRefreshCoordinator.markInternalWrite();
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
        QuickNoteVaultRefreshCoordinator.markInternalWrite();
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
        QuickNoteVaultRefreshCoordinator.markInternalWrite();
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

    public static List<String> listNotePathsUnderFolder(String folderRelativePath) {
        String folder = normalizeFolderPath(folderRelativePath);
        if (StringUtils.isBlank(folder)) {
            return List.of();
        }
        String prefix = folder + "/";
        return listAll().stream()
                .map(TQuickNote::getRelativePath)
                .filter(path -> path.startsWith(prefix))
                .collect(Collectors.toList());
    }

    public static String fileNameWithoutExtension(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        int slash = normalized.lastIndexOf('/');
        String fileName = slash < 0 ? normalized : normalized.substring(slash + 1);
        if (fileName.endsWith(TXT_EXTENSION)) {
            fileName = fileName.substring(0, fileName.length() - TXT_EXTENSION.length());
        }
        return fileName;
    }

    public static List<String> listFolders() {
        ensureVaultReady();
        List<String> folders = new ArrayList<>();
        collectFolders(getVaultDir(), "", folders);
        folders.sort(String::compareToIgnoreCase);
        return QuickNoteGitIgnoreUtil.filterVisibleFolders(getVaultDir(), folders);
    }

    private static List<TQuickNote> filterVisibleNotes(List<TQuickNote> notes) {
        if (notes.isEmpty()) {
            return notes;
        }
        List<String> paths = notes.stream()
                .map(TQuickNote::getRelativePath)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Set<String> visible = new java.util.LinkedHashSet<>(
                QuickNoteGitIgnoreUtil.filterVisiblePaths(getVaultDir(), paths));
        return notes.stream()
                .filter(note -> visible.contains(note.getRelativePath()))
                .collect(Collectors.toList());
    }

    public static List<String> listAllBodies() {
        return listAll().stream().map(TQuickNote::getContent).collect(Collectors.toList());
    }

    public static List<String> listOtherBodies(String excludePath) {
        return listAll().stream()
                .filter(note -> !excludePath.equals(note.getRelativePath()))
                .map(TQuickNote::getContent)
                .collect(Collectors.toList());
    }

    private static void collectNotes(File dir, String relativePrefix, List<TQuickNote> notes) {
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
                collectNotes(child, childRelative, notes);
            } else if (child.isFile() && name.endsWith(TXT_EXTENSION)) {
                notes.add(readNoteFile(child, childRelative));
            }
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

    private static TQuickNote readNoteFile(File file, String relativePath) {
        String raw = FileUtil.readString(file, StandardCharsets.UTF_8);
        QuickNoteFrontmatter.ParsedNote parsed = QuickNoteFrontmatter.parse(raw);
        TQuickNote note = new TQuickNote();
        note.setRelativePath(normalizeRelativePath(relativePath));
        note.setName(resolveTitle(parsed, file));
        note.setContent(parsed.getBody());
        note.setCreateTime(parsed.getString("created_at"));
        note.setModifiedTime(firstNonBlank(
                parsed.getString("modified_at"),
                formatFileTime(file.lastModified()),
                note.getCreateTime()));
        note.setColor(defaultString(parsed.getString("color"), "default"));
        note.setStyle(parsed.getString("style"));
        note.setFontName(parsed.getString("font_name"));
        note.setFontSize(parsed.getString("font_size"));
        note.setSyntax(defaultString(parsed.getString("syntax"), SyntaxConstants.SYNTAX_STYLE_NONE));
        note.setLineWrap(defaultString(parsed.getString("line_wrap"), "0"));
        return note;
    }

    private static Map<String, Object> buildMetadata(TQuickNote note) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", note.getName());
        metadata.put("syntax", defaultString(note.getSyntax(), SyntaxConstants.SYNTAX_STYLE_NONE));
        metadata.put("font_name", defaultString(note.getFontName(), ""));
        metadata.put("font_size", defaultString(note.getFontSize(), ""));
        metadata.put("color", defaultString(note.getColor(), "default"));
        metadata.put("line_wrap", defaultString(note.getLineWrap(), "0"));
        metadata.put("created_at", defaultString(note.getCreateTime(), SqliteUtil.nowDateForSqlite()));
        metadata.put("modified_at", defaultString(note.getModifiedTime(), SqliteUtil.nowDateForSqlite()));
        return metadata;
    }

    private static String resolveTitle(QuickNoteFrontmatter.ParsedNote parsed, File file) {
        String title = parsed.getString("title");
        if (StringUtils.isNotBlank(title)) {
            return title.trim();
        }
        String fileName = file.getName();
        if (fileName.endsWith(TXT_EXTENSION)) {
            return fileName.substring(0, fileName.length() - TXT_EXTENSION.length());
        }
        return fileName;
    }

    private static void migrateFromDatabaseIfNeeded(File vaultDir) {
        File marker = new File(vaultDir, MIGRATION_MARKER);
        if (marker.exists()) {
            return;
        }
        try {
            TQuickNoteMapper mapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);
            List<TQuickNote> dbNotes = mapper.selectAll();
            for (TQuickNote dbNote : dbNotes) {
                String title = StringUtils.defaultIfBlank(dbNote.getName(), NamingUtil.defaultUntitledName());
                String relativePath = buildUniqueRelativePath("", sanitizeFileName(title));
                dbNote.setRelativePath(relativePath);
                dbNote.setName(title);
                writeNoteFile(dbNote, StringUtils.defaultString(dbNote.getContent()));
            }
            Files.writeString(marker.toPath(), SqliteUtil.nowDateForSqlite(), StandardCharsets.UTF_8);
            if (!dbNotes.isEmpty()) {
                QuickNoteGitUtil.commit(vaultDir, "Migrated " + dbNotes.size() + " quick note(s) from database");
            }
            log.info("Migrated {} quick notes from database to vault", dbNotes.size());
        } catch (Exception e) {
            log.warn("Quick note DB migration failed: {}", e.getMessage());
        }
    }

    private static void seedWelcomeNoteIfEmpty(File vaultDir) {
        File[] children = vaultDir.listFiles(pathname ->
                pathname.isFile() && pathname.getName().endsWith(TXT_EXTENSION));
        if (children != null && children.length > 0) {
            return;
        }
        List<String> folders = new ArrayList<>();
        collectFolders(vaultDir, "", folders);
        if (!folders.isEmpty()) {
            return;
        }
        String now = SqliteUtil.nowDateForSqlite();
        TQuickNote welcome = new TQuickNote();
        welcome.setRelativePath("关于随手记.txt");
        welcome.setName("关于随手记");
        welcome.setContent("""
                随手记可以用来快速记录一些：
                代码片段、常用的SQL、常用的接口、常用的数据、暂存一些临时log等

                点击加号按钮，开始创建一条新的笔记
                """);
        welcome.setCreateTime(now);
        welcome.setModifiedTime(now);
        welcome.setSyntax(SyntaxConstants.SYNTAX_STYLE_NONE);
        welcome.setFontName(App.config.getQuickNoteFontName());
        int fontSize = App.config.getQuickNoteFontSize();
        welcome.setFontSize(fontSize > 0 ? String.valueOf(fontSize) : "14");
        welcome.setColor("default");
        welcome.setLineWrap("0");
        writeNoteFile(welcome, welcome.getContent());
    }

    private static void writeNoteFile(TQuickNote note, String body) {
        File file = new File(getVaultDir(), normalizeRelativePath(note.getRelativePath()));
        FileUtil.mkParentDirs(file);
        Map<String, Object> metadata = buildMetadata(note);
        String raw = QuickNoteFrontmatter.serialize(metadata, body == null ? "" : body);
        FileUtil.writeString(raw, file, StandardCharsets.UTF_8);
        QuickNoteVaultRefreshCoordinator.markInternalWrite();
    }

    private static String buildUniqueRelativePath(String folder, String baseName) {
        return buildUniqueRelativePath(folder, baseName, null);
    }

    private static String buildUniqueRelativePath(String folder, String baseName, String excludePath) {
        String candidate = joinRelativePath(folder, baseName + TXT_EXTENSION);
        if (excludePath != null && candidate.equals(excludePath)) {
            return candidate;
        }
        if (loadByPath(candidate) == null && !toAbsoluteFile(candidate).exists()) {
            return candidate;
        }
        int index = 1;
        while (true) {
            String numbered = joinRelativePath(folder, baseName + "-" + index + TXT_EXTENSION);
            if (excludePath != null && numbered.equals(excludePath)) {
                return numbered;
            }
            if (loadByPath(numbered) == null && !toAbsoluteFile(numbered).exists()) {
                return numbered;
            }
            index++;
        }
    }

    public static File toAbsoluteFile(String relativePath) {
        return new File(getVaultDir(), normalizeRelativePath(relativePath));
    }

    private static File toAbsoluteFolder(String folderRelativePath) {
        return new File(getVaultDir(), normalizeFolderPath(folderRelativePath));
    }

    public static String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        return relativePath.replace('\\', '/').replaceAll("^/+", "");
    }

    public static String normalizeFolderPath(String folderRelativePath) {
        String normalized = normalizeRelativePath(folderRelativePath);
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String parentFolder(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        int index = normalized.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        return normalized.substring(0, index);
    }

    public static String joinRelativePath(String folder, String name) {
        String normalizedFolder = normalizeFolderPath(folder);
        if (StringUtils.isBlank(normalizedFolder)) {
            return normalizeRelativePath(name);
        }
        return normalizeRelativePath(normalizedFolder + "/" + name);
    }

    public static String sanitizeFileName(String name) {
        String trimmed = StringUtils.trimToEmpty(name);
        if (trimmed.isEmpty()) {
            return NamingUtil.defaultUntitledName();
        }
        return trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static boolean shouldSkipEntry(String name) {
        return name.startsWith(".") || "attachments".equals(name);
    }

    public static String getDefaultVaultPath() {
        return SystemUtil.CONFIG_HOME + File.separator + VAULT_DIR_NAME;
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
            return;
        }
        File folder = toAbsoluteFolder(relativePath);
        if (folder.isDirectory()) {
            openDirectory(folder);
        }
    }

    private static void revealFile(File file) {
        try {
            if (SystemUtil.isMacOs()) {
                new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
            } else if (SystemUtil.isWindowsOs()) {
                new ProcessBuilder("explorer", "/select,", file.getAbsolutePath()).start();
            } else if (file.getParentFile() != null) {
                Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception e) {
            log.warn("Reveal file failed: {}", e.getMessage());
        }
    }

    private static void openDirectory(File dir) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (Exception e) {
            log.warn("Open directory failed: {}", e.getMessage());
        }
    }

    private static void syncConfiguredRemote(File vaultDir) {
        String remoteUrl = App.config.getQuickNoteGitRemoteUrl();
        if (StringUtils.isNotBlank(remoteUrl) && QuickNoteGitUtil.isGitRepo(vaultDir)) {
            QuickNoteGitUtil.configureRemote(vaultDir, remoteUrl);
        }
    }

    private static String defaultString(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return SqliteUtil.nowDateForSqlite();
    }

    private static String formatFileTime(long epochMillis) {
        return cn.hutool.core.date.DateUtil.format(new java.util.Date(epochMillis), "yyyy-MM-dd HH:mm:ss");
    }
}
