package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import com.luoboduner.moo.tool.bean.textdiff.UIDiff;
import com.luoboduner.moo.tool.domain.QuickNoteGitCommit;
import com.luoboduner.moo.tool.service.DiffService;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 将 Git 变更/历史内容转换为可高亮展示的 unified diff。
 */
public final class QuickNoteGitDiffHelper {

    private static final int MAX_DIFF_CHARS = 512 * 1024;

    private QuickNoteGitDiffHelper() {
    }

    public record Result(String text, String oldText, String newText, UIDiff uiDiff, String conflictProbe,
                         boolean sideBySide) {
        public static Result plain(String text) {
            return new Result(StringUtils.defaultString(text), null, null, null, text, false);
        }

        public static Result sideBySide(String oldText, String newText, UIDiff uiDiff, String conflictProbe) {
            return new Result(null,
                    StringUtils.defaultString(oldText),
                    StringUtils.defaultString(newText),
                    uiDiff,
                    conflictProbe,
                    true);
        }
    }

    public static Result buildWorkingDiff(File vaultDir, String relativePath) {
        if (!QuickNoteGitUtil.isGitRepo(vaultDir) || StringUtils.isBlank(relativePath)) {
            return Result.plain("");
        }
        try {
            String normalizedPath = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
            String oldContent = QuickNoteGitUtil.getFileContentAtRef(vaultDir, "HEAD", normalizedPath);
            String newContent = readWorkingContent(vaultDir, normalizedPath);
            String conflictProbe = StringUtils.defaultString(newContent);
            if (isTooLarge(oldContent, newContent)) {
                return Result.plain(QuickNoteGitUtil.getWorkingDiff(vaultDir, normalizedPath));
            }
            String oldText = StringUtils.defaultString(oldContent);
            String newText = StringUtils.defaultString(newContent);
            UIDiff uiDiff = DiffService.getSegmentsForUI(oldText, newText, false);
            return Result.sideBySide(oldText, newText, uiDiff, conflictProbe);
        } catch (Exception e) {
            return Result.plain(e.getMessage());
        }
    }

    public static Result buildCommitDiff(File vaultDir, String relativePath, String commitHash,
                                         QuickNoteGitCommit commitMeta) {
        if (!QuickNoteGitUtil.isGitRepo(vaultDir) || StringUtils.isBlank(commitHash)) {
            return Result.plain("");
        }
        if (StringUtils.isBlank(relativePath)) {
            return Result.plain(buildCommitSummary(vaultDir, commitHash, commitMeta));
        }
        try {
            String normalizedPath = QuickNoteVaultUtil.normalizeRelativePath(relativePath);
            String parentRef = commitHash + "^";
            String oldContent = QuickNoteGitUtil.getFileContentAtRef(vaultDir, parentRef, normalizedPath);
            String newContent = QuickNoteGitUtil.getFileContentAtRef(vaultDir, commitHash, normalizedPath);
            if (isTooLarge(oldContent, newContent)) {
                return Result.plain(QuickNoteGitUtil.getCommitDiff(vaultDir, normalizedPath, commitHash));
            }
            String oldText = StringUtils.defaultString(oldContent);
            String newText = StringUtils.defaultString(newContent);
            UIDiff uiDiff = DiffService.getSegmentsForUI(oldText, newText, false);
            return Result.sideBySide(oldText, newText, uiDiff, newText);
        } catch (Exception e) {
            return Result.plain(e.getMessage());
        }
    }

    private static String buildCommitSummary(File vaultDir, String commitHash, QuickNoteGitCommit commitMeta) {
        StringBuilder builder = new StringBuilder();
        if (commitMeta != null) {
            builder.append("commit ").append(commitMeta.getHash()).append('\n');
            if (StringUtils.isNotBlank(commitMeta.getAuthor())) {
                builder.append("Author: ").append(commitMeta.getAuthor()).append('\n');
            }
            if (StringUtils.isNotBlank(commitMeta.getDate())) {
                builder.append("Date: ").append(commitMeta.getDate()).append('\n');
            }
            builder.append('\n');
            if (StringUtils.isNotBlank(commitMeta.getMessage())) {
                builder.append(commitMeta.getMessage().trim()).append('\n');
            }
            builder.append('\n');
        } else {
            builder.append(QuickNoteGitUtil.getCommitDiff(vaultDir, "", commitHash)).append('\n');
        }
        List<String> changedFiles = QuickNoteGitUtil.listCommitNameStatus(vaultDir, commitHash);
        if (!changedFiles.isEmpty()) {
            builder.append("--- ").append(changedFiles.size()).append(" ---\n");
            for (String line : changedFiles) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private static String readWorkingContent(File vaultDir, String relativePath) {
        File file = new File(vaultDir, relativePath);
        if (!file.exists() || !file.isFile()) {
            return "";
        }
        return FileUtil.readString(file, StandardCharsets.UTF_8);
    }

    private static boolean isTooLarge(String... parts) {
        int total = 0;
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            total += part.length();
            if (total > MAX_DIFF_CHARS) {
                return true;
            }
        }
        return false;
    }
}
