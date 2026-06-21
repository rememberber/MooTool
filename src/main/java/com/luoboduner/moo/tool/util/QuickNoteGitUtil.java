package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitCommit;
import com.luoboduner.moo.tool.domain.QuickNoteGitModifiedFile;
import com.luoboduner.moo.tool.domain.QuickNoteGitPullResult;
import com.luoboduner.moo.tool.domain.QuickNoteGitStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 随手记 Vault 的 Git 管理，参考 Tolaria 的 git 工作流。
 */
@Slf4j
public final class QuickNoteGitUtil {

    private static final String DEFAULT_GITIGNORE = """
            .DS_Store
            .idea/
            .vscode/
            *.tmp
            .migrated-from-db
            """;

    private static final String REMOTE_NAME = "origin";

    private QuickNoteGitUtil() {
    }

    public static boolean isGitRepo(File vaultDir) {
        return new File(vaultDir, ".git").isDirectory();
    }

    public static void initRepoIfNeeded(File vaultDir) {
        if (isGitRepo(vaultDir)) {
            ensureQuotePathDisabled(vaultDir);
            return;
        }
        try {
            runGit(vaultDir, 60, "init");
            ensureQuotePathDisabled(vaultDir);
            File gitignore = new File(vaultDir, ".gitignore");
            if (!gitignore.exists()) {
                Files.writeString(gitignore.toPath(), DEFAULT_GITIGNORE, StandardCharsets.UTF_8);
            }
            runGit(vaultDir, 60, "add", "-A");
            commit(vaultDir, "Initial quick note vault setup");
        } catch (Exception e) {
            log.warn("Init git repo failed: {}", e.getMessage());
        }
    }

    public static GitCommandResult commit(File vaultDir, String message) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(message)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        try {
            runGit(vaultDir, 60, "add", "-A");
            ensureAuthorConfig(vaultDir);
            GitResult result = runGit(vaultDir, 60, "commit", "-m", message);
            if (!result.success() && isSigningFailure(result.combinedOutput())) {
                result = runGit(vaultDir, 60, "-c", "commit.gpgsign=false", "commit", "-m", message);
            }
            if (result.success() || result.combinedOutput().contains("nothing to commit")) {
                return GitCommandResult.success(result.combinedOutput());
            }
            return GitCommandResult.failure(result.combinedOutput());
        } catch (Exception e) {
            log.debug("Git commit skipped: {}", e.getMessage());
            return GitCommandResult.failure(e.getMessage());
        }
    }

    public static List<String> listModifiedFiles(File vaultDir) {
        return listModifiedFilesDetailed(vaultDir).stream()
                .map(QuickNoteGitModifiedFile::getPath)
                .toList();
    }

    public static List<QuickNoteGitModifiedFile> listModifiedFilesDetailed(File vaultDir) {
        List<QuickNoteGitModifiedFile> modified = new ArrayList<>();
        if (!isGitRepo(vaultDir)) {
            return modified;
        }
        try {
            GitResult result = runGit(vaultDir, 30, "status", "--porcelain");
            if (!result.success()) {
                return modified;
            }
            for (String line : result.stdout().split("\n")) {
                if (line.length() < 3) {
                    continue;
                }
                String statusCode = line.substring(0, 2);
                PorcelainPath porcelainPath = extractPorcelainPath(line);
                if (StringUtils.isBlank(porcelainPath.path())) {
                    continue;
                }
                modified.add(new QuickNoteGitModifiedFile(
                        porcelainPath.path(),
                        porcelainPath.originalPath(),
                        statusCode,
                        mapStatusLabel(statusCode)));
            }
        } catch (Exception e) {
            log.debug("Git status failed: {}", e.getMessage());
        }
        return modified;
    }

    public static List<QuickNoteGitCommit> listVaultHistory(File vaultDir, int limit) {
        return listHistory(vaultDir, null, limit);
    }

    public static List<QuickNoteGitCommit> listFileHistory(File vaultDir, String relativePath, int limit) {
        return listHistory(vaultDir, relativePath, limit);
    }

    public static String getWorkingDiff(File vaultDir, String relativePath) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(relativePath)) {
            return "";
        }
        try {
            GitResult result = runGit(vaultDir, 30, "diff", "--", relativePath);
            return result.stdout();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public static String getCommitDiff(File vaultDir, String relativePath, String commitHash) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(commitHash)) {
            return "";
        }
        try {
            if (StringUtils.isBlank(relativePath)) {
                GitResult result = runGit(vaultDir, 30, "show", "--pretty=medium", commitHash);
                return result.stdout();
            }
            String normalizedPath = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
            GitResult result = runGit(vaultDir, 30, "show", "--pretty=format:", commitHash, "--", normalizedPath);
            return result.stdout();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 读取指定 ref 下的文件内容；文件不存在时返回空字符串。
     */
    public static String getFileContentAtRef(File vaultDir, String ref, String relativePath) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(ref) || StringUtils.isBlank(relativePath)) {
            return "";
        }
        try {
            String normalizedPath = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
            String objectSpec = ref + ":" + normalizedPath;
            GitResult result = runGit(vaultDir, 30, false, "show", objectSpec);
            if (!result.success()) {
                return "";
            }
            return result.stdout();
        } catch (Exception e) {
            log.debug("Read git blob failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 列出某次提交涉及的文件及状态（name-status 格式）。
     */
    public static List<String> listCommitNameStatus(File vaultDir, String commitHash) {
        List<String> lines = new ArrayList<>();
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(commitHash)) {
            return lines;
        }
        try {
            GitResult result = runGit(vaultDir, 30, "diff-tree", "--no-commit-id", "--name-status", "-r", commitHash);
            if (!result.success() || StringUtils.isBlank(result.stdout())) {
                return lines;
            }
            for (String line : result.stdout().split("\n")) {
                if (StringUtils.isNotBlank(line)) {
                    lines.add(line.trim());
                }
            }
        } catch (Exception e) {
            log.debug("List commit files failed: {}", e.getMessage());
        }
        return lines;
    }

    public static String getRemoteUrl(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return "";
        }
        try {
            GitResult result = runGit(vaultDir, 10, "remote", "get-url", REMOTE_NAME);
            if (result.success()) {
                return result.stdout().trim();
            }
        } catch (Exception e) {
            log.debug("Read git remote failed: {}", e.getMessage());
        }
        return "";
    }

    public static GitCommandResult configureRemote(File vaultDir, String remoteUrl) {
        if (!isGitRepo(vaultDir)) {
            initRepoIfNeeded(vaultDir);
        }
        if (!isGitRepo(vaultDir)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        try {
            if (StringUtils.isBlank(remoteUrl)) {
                runGit(vaultDir, 10, "remote", "remove", REMOTE_NAME);
                return GitCommandResult.success("Remote removed");
            }
            GitResult existing = runGit(vaultDir, 10, "remote");
            if (existing.success() && existing.stdout().contains(REMOTE_NAME)) {
                GitResult result = runGit(vaultDir, 20, "remote", "set-url", REMOTE_NAME, remoteUrl);
                return result.success()
                        ? GitCommandResult.success(result.stdout())
                        : GitCommandResult.failure(result.stdout());
            }
            GitResult result = runGit(vaultDir, 20, "remote", "add", REMOTE_NAME, remoteUrl);
            return result.success()
                    ? GitCommandResult.success(result.stdout())
                    : GitCommandResult.failure(result.stdout());
        } catch (Exception e) {
            return GitCommandResult.failure(e.getMessage());
        }
    }

    public static boolean hasRemote(File vaultDir) {
        return StringUtils.isNotBlank(getRemoteUrl(vaultDir));
    }

    public static GitCommandResult pull(File vaultDir) {
        QuickNoteGitPullResult result = pullWithResult(vaultDir);
        if (result.isSuccess()) {
            return GitCommandResult.success(result.getMessage());
        }
        return GitCommandResult.failure(result.getMessage());
    }

    public static QuickNoteGitPullResult pullWithResult(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return QuickNoteGitPullResult.error("Git repository is not initialized");
        }
        if (!hasRemote(vaultDir)) {
            return QuickNoteGitPullResult.error("Remote origin is not configured");
        }
        try {
            String oldHead = resolveHead(vaultDir);
            GitResult result = runGit(vaultDir, 180, "pull", "--rebase", REMOTE_NAME);
            if (!result.success()) {
                if (isSigningFailure(result.combinedOutput())) {
                    return QuickNoteGitPullResult.error(result.combinedOutput());
                }
                if (isPullConflict(vaultDir, result.combinedOutput())) {
                    return QuickNoteGitPullResult.conflict(result.combinedOutput());
                }
                return QuickNoteGitPullResult.error(result.combinedOutput());
            }
            if (isPullConflict(vaultDir, result.combinedOutput())) {
                return QuickNoteGitPullResult.conflict(result.combinedOutput());
            }
            String newHead = resolveHead(vaultDir);
            if (isUpToDatePull(result.combinedOutput(), oldHead, newHead)) {
                return QuickNoteGitPullResult.upToDate(result.combinedOutput());
            }
            List<String> updatedFiles = listChangedFilesBetween(vaultDir, oldHead, newHead);
            return QuickNoteGitPullResult.updated(updatedFiles, result.combinedOutput());
        } catch (Exception e) {
            return QuickNoteGitPullResult.error(e.getMessage());
        }
    }

    public static GitCommandResult commitAndPush(File vaultDir, String message) {
        GitCommandResult commitResult = commit(vaultDir, message);
        if (!commitResult.isSuccess()) {
            return commitResult;
        }
        if (!hasRemote(vaultDir)) {
            return commitResult;
        }
        return push(vaultDir);
    }

    public static GitCommandResult pushIfNeeded(File vaultDir) {
        if (!isGitRepo(vaultDir) || !hasRemote(vaultDir)) {
            return GitCommandResult.success("");
        }
        QuickNoteGitStatus status = getStatus(vaultDir);
        if (status.getAhead() <= 0) {
            return GitCommandResult.success("");
        }
        return push(vaultDir);
    }

    public static boolean isPushRejected(String output) {
        if (StringUtils.isBlank(output)) {
            return false;
        }
        String lower = output.toLowerCase(Locale.ROOT);
        return lower.contains("[rejected]")
                || lower.contains("non-fast-forward")
                || lower.contains("failed to push some refs")
                || (lower.contains("rejected") && lower.contains("fetch first"));
    }

    private static boolean isPullConflict(File vaultDir, String output) {
        if (isMerging(vaultDir) || !listConflictFiles(vaultDir).isEmpty()) {
            return true;
        }
        if (StringUtils.isBlank(output)) {
            return false;
        }
        String lower = output.toLowerCase(Locale.ROOT);
        return lower.contains("conflict") || lower.contains("could not apply");
    }

    private static boolean isUpToDatePull(String output, String oldHead, String newHead) {
        if (StringUtils.isNotBlank(output)) {
            String lower = output.toLowerCase(Locale.ROOT);
            if (lower.contains("already up to date") || lower.contains("up to date")) {
                return true;
            }
        }
        return StringUtils.isNotBlank(oldHead) && oldHead.equals(newHead);
    }

    private static String resolveHead(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return "";
        }
        try {
            GitResult result = runGit(vaultDir, 10, "rev-parse", "HEAD");
            return result.success() ? result.stdout().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> listChangedFilesBetween(File vaultDir, String oldHead, String newHead) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(oldHead) || StringUtils.isBlank(newHead)
                || oldHead.equals(newHead)) {
            return List.of();
        }
        try {
            GitResult result = runGit(vaultDir, 30, "diff", "--name-only", oldHead, newHead);
            if (!result.success() || StringUtils.isBlank(result.stdout())) {
                return List.of();
            }
            List<String> files = new ArrayList<>();
            for (String line : result.stdout().split("\n")) {
                if (StringUtils.isNotBlank(line)) {
                    files.add(QuickNoteVaultUtil.normalizeRelativePath(line.trim()));
                }
            }
            return files;
        } catch (Exception e) {
            return List.of();
        }
    }

    public static GitCommandResult push(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        if (!hasRemote(vaultDir)) {
            return GitCommandResult.failure("Remote origin is not configured");
        }
        try {
            GitResult result = runGit(vaultDir, 180, "push", "-u", REMOTE_NAME, "HEAD");
            if (result.success()) {
                return GitCommandResult.success(result.stdout());
            }
            if (isPushRejected(result.combinedOutput())) {
                return GitCommandResult.pushRejected(result.combinedOutput());
            }
            return GitCommandResult.failure(result.combinedOutput());
        } catch (Exception e) {
            return GitCommandResult.failure(e.getMessage());
        }
    }

    public static GitCommandResult discardWorkingChanges(File vaultDir, String relativePath) {
        return discardWorkingChanges(vaultDir, relativePath, "");
    }

    public static GitCommandResult discardWorkingChanges(File vaultDir, String relativePath, String statusCode) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(relativePath)) {
            return GitCommandResult.failure(I18n.get("quickNote.git.discardInvalidPath"));
        }
        ensureQuotePathDisabled(vaultDir);
        ResolvedDiscardPath resolved = resolveDiscardPath(vaultDir, relativePath, statusCode);
        String path = resolved.path();
        String code = resolved.statusCode();
        try {
            if ("??".equals(code)) {
                GitResult clean = runGit(vaultDir, 30, "clean", "-f", "--", path);
                if (!clean.success()) {
                    File file = QuickNoteVaultUtil.toAbsoluteFile(path);
                    if (!file.exists() || !file.delete()) {
                        return GitCommandResult.failure(resolveDiscardFailureMessage(clean.stdout(), path));
                    }
                }
            } else {
                String originalPath = resolved.originalPath();
                GitResult restore;
                if (isRenameStatus(code) && StringUtils.isNotBlank(originalPath)) {
                    restore = runGit(vaultDir, 30, "restore", "--staged", "--worktree", "--", originalPath, path);
                } else {
                    restore = runGit(vaultDir, 30, "restore", "--staged", "--worktree", "--", path);
                }
                if (!restore.success()) {
                    if (isRenameStatus(code) && StringUtils.isNotBlank(originalPath)) {
                        runGit(vaultDir, 30, "reset", "HEAD", "--", originalPath, path);
                    } else {
                        runGit(vaultDir, 30, "reset", "HEAD", "--", path);
                    }
                    GitResult checkout = isRenameStatus(code) && StringUtils.isNotBlank(originalPath)
                            ? runGit(vaultDir, 30, "checkout", "HEAD", "--", originalPath, path)
                            : runGit(vaultDir, 30, "checkout", "HEAD", "--", path);
                    if (!checkout.success()) {
                        return GitCommandResult.failure(resolveDiscardFailureMessage(
                                StringUtils.firstNonBlank(restore.stdout(), checkout.stdout()), path));
                    }
                }
            }

            if (isPathStillDirty(vaultDir, path) || isPathStillDirty(vaultDir, resolved.originalPath())) {
                return GitCommandResult.failure(I18n.format("quickNote.git.discardStillDirty", path));
            }
            QuickNoteVaultRefreshCoordinator.markInternalWrite();
            return GitCommandResult.success("");
        } catch (Exception e) {
            return GitCommandResult.failure(StringUtils.defaultIfBlank(e.getMessage(), I18n.get("quickNote.git.discardFailed")));
        }
    }

    private record ResolvedDiscardPath(String path, String originalPath, String statusCode) {
    }

    private static ResolvedDiscardPath resolveDiscardPath(File vaultDir, String relativePath, String statusCode) {
        String normalized = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
        List<QuickNoteGitModifiedFile> modified = listModifiedFilesDetailed(vaultDir);
        for (QuickNoteGitModifiedFile file : modified) {
            if (normalized.equals(file.getPath())) {
                return new ResolvedDiscardPath(file.getPath(), file.getOriginalPath(),
                        StringUtils.defaultIfBlank(statusCode, file.getStatusCode()));
            }
        }
        for (QuickNoteGitModifiedFile file : modified) {
            if (file.getPath().endsWith(normalized)) {
                return new ResolvedDiscardPath(file.getPath(), file.getOriginalPath(),
                        StringUtils.defaultIfBlank(statusCode, file.getStatusCode()));
            }
        }
        if (StringUtils.isBlank(statusCode)) {
            List<QuickNoteGitModifiedFile> sameName = modified.stream()
                    .filter(file -> file.getPath().endsWith(normalized) || normalized.endsWith(file.getPath()))
                    .toList();
            if (sameName.size() == 1) {
                QuickNoteGitModifiedFile file = sameName.get(0);
                return new ResolvedDiscardPath(file.getPath(), file.getOriginalPath(), file.getStatusCode());
            }
        }
        return new ResolvedDiscardPath(normalized, "", StringUtils.defaultString(statusCode));
    }

    private record PorcelainPath(String path, String originalPath) {
    }

    private static PorcelainPath extractPorcelainPath(String line) {
        int index = 2;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        if (index >= line.length()) {
            return new PorcelainPath("", "");
        }
        String rawPath = line.substring(index).trim();
        String originalPath = "";
        if (rawPath.contains(" -> ")) {
            int renameIndex = rawPath.indexOf(" -> ");
            originalPath = QuickNoteVaultUtil.normalizeRelativePath(
                    unquoteGitStatusPath(rawPath.substring(0, renameIndex).trim()));
            rawPath = rawPath.substring(renameIndex + 4).trim();
        }
        String path = QuickNoteVaultUtil.normalizeRelativePath(unquoteGitStatusPath(rawPath));
        return new PorcelainPath(path, originalPath);
    }

    private static boolean isRenameStatus(String statusCode) {
        return StringUtils.defaultString(statusCode).contains("R");
    }

    private static void ensureQuotePathDisabled(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return;
        }
        try {
            runGit(vaultDir, 10, "config", "core.quotePath", "false");
        } catch (Exception e) {
            log.debug("Unable to disable core.quotePath: {}", e.getMessage());
        }
    }

    private static boolean isPathStillDirty(File vaultDir, String path) {
        return listModifiedFilesDetailed(vaultDir).stream()
                .anyMatch(file -> path.equals(QuickNoteVaultUtil.normalizeRelativePath(file.getPath())));
    }

    private static String resolveDiscardFailureMessage(String gitOutput, String path) {
        if (StringUtils.isNotBlank(gitOutput)) {
            return gitOutput;
        }
        return I18n.format("quickNote.git.discardFailedFor", path);
    }

    static String unquoteGitStatusPath(String raw) {
        if (StringUtils.isBlank(raw)) {
            return raw;
        }
        String trimmed = raw.trim();
        if (!(trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2)) {
            return trimmed;
        }
        String body = trimmed.substring(1, trimmed.length() - 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char next = body.charAt(++i);
                if (next >= '0' && next <= '7') {
                    int value = next - '0';
                    for (int j = 0; j < 2 && i + 1 < body.length(); j++) {
                        char digit = body.charAt(i + 1);
                        if (digit < '0' || digit > '7') {
                            break;
                        }
                        value = value * 8 + (digit - '0');
                        i++;
                    }
                    out.write(value);
                    continue;
                }
                out.write(switch (next) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    default -> next < 128 ? (byte) next : '?';
                });
                continue;
            }
            if (c < 128) {
                out.write((byte) c);
            } else {
                out.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    public static List<String> listConflictFiles(File vaultDir) {
        Set<String> conflicts = new LinkedHashSet<>();
        if (!isGitRepo(vaultDir)) {
            return List.of();
        }
        try {
            GitResult result = runGit(vaultDir, 30, "ls-files", "--unmerged");
            if (StringUtils.isBlank(result.stdout())) {
                return List.of();
            }
            for (String line : result.stdout().split("\n")) {
                int tabIndex = line.indexOf('\t');
                if (tabIndex < 0 || tabIndex >= line.length() - 1) {
                    continue;
                }
                String path = QuickNoteVaultUtil.normalizeRelativePath(line.substring(tabIndex + 1).trim());
                if (StringUtils.isNotBlank(path)) {
                    conflicts.add(path);
                }
            }
        } catch (Exception e) {
            log.debug("Git conflict scan failed: {}", e.getMessage());
        }
        return new ArrayList<>(conflicts);
    }

    public static GitCommandResult resolveConflictFile(File vaultDir, String relativePath, String strategy) {
        if (!isGitRepo(vaultDir) || StringUtils.isBlank(relativePath)) {
            return GitCommandResult.failure(I18n.get("quickNote.git.discardInvalidPath"));
        }
        String checkoutFlag = switch (StringUtils.defaultString(strategy).toLowerCase(Locale.ROOT)) {
            case "ours" -> "--ours";
            case "theirs" -> "--theirs";
            default -> null;
        };
        if (checkoutFlag == null) {
            return GitCommandResult.failure(I18n.get("quickNote.git.invalidConflictStrategy"));
        }
        String path = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
        try {
            ensureQuotePathDisabled(vaultDir);
            GitResult checkout = runGit(vaultDir, 30, "checkout", checkoutFlag, "--", path);
            if (!checkout.success()) {
                return GitCommandResult.failure(checkout.stdout());
            }
            GitResult add = runGit(vaultDir, 30, "add", "--", path);
            if (!add.success()) {
                return GitCommandResult.failure(add.stdout());
            }
            QuickNoteVaultRefreshCoordinator.markInternalWrite();
            return GitCommandResult.success("");
        } catch (Exception e) {
            return GitCommandResult.failure(StringUtils.defaultIfBlank(e.getMessage(),
                    I18n.get("quickNote.git.resolveConflictFailed")));
        }
    }

    public static String getConflictMode(File vaultDir) {
        if (isRebaseInProgress(vaultDir)) {
            return "rebase";
        }
        if (isMergeInProgress(vaultDir)) {
            return "merge";
        }
        return "none";
    }

    public static boolean isRebaseInProgress(File vaultDir) {
        return new File(vaultDir, ".git/rebase-merge").isDirectory()
                || new File(vaultDir, ".git/rebase-apply").isDirectory();
    }

    public static boolean isMergeInProgress(File vaultDir) {
        return new File(vaultDir, ".git/MERGE_HEAD").isFile();
    }

    public static GitCommandResult commitConflictResolution(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        List<String> remaining = listConflictFiles(vaultDir);
        if (!remaining.isEmpty()) {
            return GitCommandResult.failure(I18n.format("quickNote.git.conflictsRemain", remaining.size()));
        }
        try {
            ensureAuthorConfig(vaultDir);
            String mode = getConflictMode(vaultDir);
            GitResult result;
            if ("rebase".equals(mode)) {
                result = runGit(vaultDir, 60, Map.of("GIT_EDITOR", "true"), "rebase", "--continue");
            } else {
                result = runGit(vaultDir, 60, "commit", "-m", I18n.get("quickNote.git.resolveCommitMessage"));
            }
            if (!result.success()) {
                return GitCommandResult.failure(result.stdout());
            }
            QuickNoteVaultRefreshCoordinator.markInternalWrite();
            return GitCommandResult.success(result.stdout());
        } catch (Exception e) {
            return GitCommandResult.failure(StringUtils.defaultIfBlank(e.getMessage(),
                    I18n.get("quickNote.git.resolveCommitFailed")));
        }
    }

    public static QuickNoteGitStatus getStatus(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return new QuickNoteGitStatus(false, false, "", 0, 0, 0, 0, false);
        }
        boolean hasRemote = hasRemote(vaultDir);
        String branch = getCurrentBranch(vaultDir);
        int changedCount = listModifiedFilesDetailed(vaultDir).size();
        int conflictCount = listConflictFiles(vaultDir).size();
        int[] aheadBehind = getAheadBehind(vaultDir);
        boolean merging = isMerging(vaultDir);
        return new QuickNoteGitStatus(true, hasRemote, branch, changedCount, conflictCount,
                aheadBehind[0], aheadBehind[1], merging);
    }

    public static GitCommandResult fetch(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        if (!hasRemote(vaultDir)) {
            return GitCommandResult.failure("Remote origin is not configured");
        }
        try {
            GitResult result = runGit(vaultDir, 180, "fetch", REMOTE_NAME);
            return result.success()
                    ? GitCommandResult.success(result.stdout())
                    : GitCommandResult.failure(result.stdout());
        } catch (Exception e) {
            return GitCommandResult.failure(e.getMessage());
        }
    }

    public static GitCommandResult abortMerge(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        try {
            GitResult result = runGit(vaultDir, 60, "merge", "--abort");
            if (!result.success()) {
                result = runGit(vaultDir, 60, "rebase", "--abort");
            }
            return result.success()
                    ? GitCommandResult.success(result.stdout())
                    : GitCommandResult.failure(result.stdout());
        } catch (Exception e) {
            return GitCommandResult.failure(e.getMessage());
        }
    }

    public static String getCurrentBranch(File vaultDir) {
        if (!isGitRepo(vaultDir)) {
            return "";
        }
        try {
            GitResult result = runGit(vaultDir, 10, "branch", "--show-current");
            return result.success() ? result.stdout().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isMerging(File vaultDir) {
        return isMergeInProgress(vaultDir) || isRebaseInProgress(vaultDir);
    }

    private static int[] getAheadBehind(File vaultDir) {
        int[] counts = new int[]{0, 0};
        if (!isGitRepo(vaultDir) || !hasRemote(vaultDir)) {
            return counts;
        }
        try {
            GitResult upstream = runGit(vaultDir, 10, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}");
            if (!upstream.success() || StringUtils.isBlank(upstream.stdout())) {
                return counts;
            }
            GitResult result = runGit(vaultDir, 10, "rev-list", "--left-right", "--count",
                    upstream.stdout().trim() + "...HEAD");
            if (!result.success()) {
                return counts;
            }
            String[] parts = result.stdout().split("\\s+");
            if (parts.length >= 2) {
                counts[1] = Integer.parseInt(parts[0]);
                counts[0] = Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            log.debug("Read ahead/behind failed: {}", e.getMessage());
        }
        return counts;
    }

    private static List<QuickNoteGitCommit> listHistory(File vaultDir, String relativePath, int limit) {
        List<QuickNoteGitCommit> commits = new ArrayList<>();
        if (!isGitRepo(vaultDir) || limit <= 0) {
            return commits;
        }
        try {
            List<String> args = new ArrayList<>();
            args.add("log");
            // %x1f = unit separator, avoids breaking when author/message contain '|'
            args.add("--pretty=format:%H%x1f%h%x1f%an%x1f%ai%x1f%s");
            args.add("-n");
            args.add(String.valueOf(limit));
            if (StringUtils.isNotBlank(relativePath)) {
                args.add("--");
                args.add(QuickNoteVaultUtil.normalizeRelativePath(relativePath));
            }
            GitResult result = runGit(vaultDir, 30, args.toArray(new String[0]));
            if (!result.success() || StringUtils.isBlank(result.stdout())) {
                return commits;
            }
            for (String line : result.stdout().split("\n")) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String[] parts = line.split("\u001f", 5);
                if (parts.length < 5) {
                    continue;
                }
                String date = formatCommitDate(parts[3]);
                commits.add(new QuickNoteGitCommit(parts[0], parts[1], parts[2], date, parts[4]));
            }
        } catch (Exception e) {
            log.debug("Git log failed: {}", e.getMessage());
        }
        return commits;
    }

    private static String formatCommitDate(String rawDate) {
        if (StringUtils.isBlank(rawDate)) {
            return "";
        }
        String trimmed = rawDate.trim();
        if (trimmed.matches(".+ [+-]\\d{4}$")) {
            return trimmed.replaceAll(" [+-]\\d{4}$", "");
        }
        return trimmed;
    }

    private static String mapStatusLabel(String statusCode) {
        String code = StringUtils.defaultString(statusCode);
        if ("??".equals(code)) {
            return "untracked";
        }
        if (code.contains("U")) {
            return "conflict";
        }
        if (code.contains("D")) {
            return "deleted";
        }
        if (code.contains("A")) {
            return "added";
        }
        if (code.contains("R")) {
            return "renamed";
        }
        if (code.contains("M")) {
            return "modified";
        }
        return code.isBlank() ? "changed" : code.trim();
    }

    private static void ensureAuthorConfig(File vaultDir) throws Exception {
        GitResult name = runGit(vaultDir, 10, "config", "user.name");
        if (!name.success() || StringUtils.isBlank(name.stdout())) {
            runGit(vaultDir, 10, "config", "user.name", "MooTool");
        }
        GitResult email = runGit(vaultDir, 10, "config", "user.email");
        if (!email.success() || StringUtils.isBlank(email.stdout())) {
            runGit(vaultDir, 10, "config", "user.email", "mootool@local");
        }
    }

    private static boolean isSigningFailure(String detail) {
        String lower = detail.toLowerCase(Locale.ROOT);
        return lower.contains("cannot run gpg") || lower.contains("gpg failed") || lower.contains("signing failed");
    }

    private static GitResult runGit(File workDir, long timeoutSeconds, String... args) throws Exception {
        return runGit(workDir, timeoutSeconds, Map.of(), true, args);
    }

    private static GitResult runGit(File workDir, long timeoutSeconds, boolean trimOutput, String... args)
            throws Exception {
        return runGit(workDir, timeoutSeconds, Map.of(), trimOutput, args);
    }

    private static GitResult runGit(File workDir, long timeoutSeconds, Map<String, String> extraEnv, String... args)
            throws Exception {
        return runGit(workDir, timeoutSeconds, extraEnv, true, args);
    }

    private static GitResult runGit(File workDir, long timeoutSeconds, Map<String, String> extraEnv, boolean trimOutput,
                                    String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-c");
        command.add("core.quotePath=false");
        for (String arg : args) {
            command.add(arg);
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir);
        builder.redirectErrorStream(true);
        if (extraEnv != null) {
            for (Map.Entry<String, String> entry : extraEnv.entrySet()) {
                builder.environment().put(entry.getKey(), entry.getValue());
            }
        }
        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!output.isEmpty()) {
                    output.append('\n');
                }
                output.append(line);
            }
        }
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            QuickNoteGitLog.append("git " + String.join(" ", args) + " -> TIMEOUT");
            throw new IllegalStateException("git command timed out");
        }
        int exitCode = process.exitValue();
        String text = trimOutput ? output.toString().trim() : output.toString();
        QuickNoteGitLog.append("git " + String.join(" ", args) + " -> "
                + (exitCode == 0 ? "OK" : "FAIL(" + exitCode + ")")
                + (text.isEmpty() ? "" : ": " + abbreviate(text, 120)));
        return new GitResult(exitCode == 0, text);
    }

    private static String abbreviate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }

    private record GitResult(boolean success, String stdout) {
        String combinedOutput() {
            return stdout;
        }
    }

    @Getter
    public static class GitCommandResult {
        private final boolean success;
        private final boolean pushRejected;
        private final String message;

        private GitCommandResult(boolean success, boolean pushRejected, String message) {
            this.success = success;
            this.pushRejected = pushRejected;
            this.message = message;
        }

        public static GitCommandResult success(String message) {
            return new GitCommandResult(true, false, StringUtils.defaultString(message));
        }

        public static GitCommandResult failure(String message) {
            return new GitCommandResult(false, false, StringUtils.defaultString(message));
        }

        public static GitCommandResult pushRejected(String message) {
            return new GitCommandResult(false, true, StringUtils.defaultString(message));
        }
    }
}
