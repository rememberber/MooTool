package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitCommit;
import com.luoboduner.moo.tool.domain.QuickNoteGitModifiedFile;
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
import java.util.List;
import java.util.Locale;
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
                String path = extractPorcelainPath(line);
                if (StringUtils.isBlank(path)) {
                    continue;
                }
                modified.add(new QuickNoteGitModifiedFile(path, statusCode, mapStatusLabel(statusCode)));
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
        if (!isGitRepo(vaultDir)) {
            return GitCommandResult.failure("Git repository is not initialized");
        }
        if (!hasRemote(vaultDir)) {
            return GitCommandResult.failure("Remote origin is not configured");
        }
        try {
            GitResult result = runGit(vaultDir, 180, "pull", "--rebase", REMOTE_NAME);
            if (result.success()) {
                return GitCommandResult.success(result.stdout());
            }
            if (isSigningFailure(result.combinedOutput())) {
                return GitCommandResult.failure(result.combinedOutput());
            }
            return GitCommandResult.failure(result.combinedOutput());
        } catch (Exception e) {
            return GitCommandResult.failure(e.getMessage());
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
                GitResult restore = runGit(vaultDir, 30, "restore", "--staged", "--worktree", "--", path);
                if (!restore.success()) {
                    runGit(vaultDir, 30, "reset", "HEAD", "--", path);
                    GitResult checkout = runGit(vaultDir, 30, "checkout", "HEAD", "--", path);
                    if (!checkout.success()) {
                        return GitCommandResult.failure(resolveDiscardFailureMessage(
                                StringUtils.firstNonBlank(restore.stdout(), checkout.stdout()), path));
                    }
                }
            }

            if (isPathStillDirty(vaultDir, path)) {
                return GitCommandResult.failure(I18n.format("quickNote.git.discardStillDirty", path));
            }
            QuickNoteVaultRefreshCoordinator.markInternalWrite();
            return GitCommandResult.success("");
        } catch (Exception e) {
            return GitCommandResult.failure(StringUtils.defaultIfBlank(e.getMessage(), I18n.get("quickNote.git.discardFailed")));
        }
    }

    private record ResolvedDiscardPath(String path, String statusCode) {
    }

    private static ResolvedDiscardPath resolveDiscardPath(File vaultDir, String relativePath, String statusCode) {
        String normalized = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
        List<QuickNoteGitModifiedFile> modified = listModifiedFilesDetailed(vaultDir);
        for (QuickNoteGitModifiedFile file : modified) {
            if (normalized.equals(file.getPath())) {
                return new ResolvedDiscardPath(file.getPath(), StringUtils.defaultIfBlank(statusCode, file.getStatusCode()));
            }
        }
        for (QuickNoteGitModifiedFile file : modified) {
            if (file.getPath().endsWith(normalized)) {
                return new ResolvedDiscardPath(file.getPath(), StringUtils.defaultIfBlank(statusCode, file.getStatusCode()));
            }
        }
        if (StringUtils.isBlank(statusCode)) {
            List<QuickNoteGitModifiedFile> sameName = modified.stream()
                    .filter(file -> file.getPath().endsWith(normalized) || normalized.endsWith(file.getPath()))
                    .toList();
            if (sameName.size() == 1) {
                QuickNoteGitModifiedFile file = sameName.get(0);
                return new ResolvedDiscardPath(file.getPath(), file.getStatusCode());
            }
        }
        return new ResolvedDiscardPath(normalized, StringUtils.defaultString(statusCode));
    }

    private static String extractPorcelainPath(String line) {
        int index = 2;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        if (index >= line.length()) {
            return "";
        }
        String rawPath = line.substring(index).trim();
        if (rawPath.contains(" -> ")) {
            rawPath = rawPath.substring(rawPath.indexOf(" -> ") + 4).trim();
        }
        return QuickNoteVaultUtil.normalizeRelativePath(unquoteGitStatusPath(rawPath));
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
        List<String> conflicts = new ArrayList<>();
        if (!isGitRepo(vaultDir)) {
            return conflicts;
        }
        try {
            GitResult result = runGit(vaultDir, 30, "diff", "--name-only", "--diff-filter=U");
            if (!result.success() || StringUtils.isBlank(result.stdout())) {
                return conflicts;
            }
            for (String line : result.stdout().split("\n")) {
                if (StringUtils.isNotBlank(line)) {
                    conflicts.add(line.trim());
                }
            }
        } catch (Exception e) {
            log.debug("Git conflict scan failed: {}", e.getMessage());
        }
        return conflicts;
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
        return new File(vaultDir, ".git/MERGE_HEAD").isFile()
                || new File(vaultDir, ".git/rebase-merge").isDirectory()
                || new File(vaultDir, ".git/rebase-apply").isDirectory();
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
        String text = output.toString().trim();
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
        private final String message;

        private GitCommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static GitCommandResult success(String message) {
            return new GitCommandResult(true, StringUtils.defaultString(message));
        }

        public static GitCommandResult failure(String message) {
            return new GitCommandResult(false, StringUtils.defaultString(message));
        }
    }
}
