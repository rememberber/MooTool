package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickNoteGitIgnoreUtilTest {

    @TempDir
    File tempDir;

    @Test
    void findIgnoredPaths_respectsGitignore() throws Exception {
        runGit(tempDir, "init");
        Files.writeString(new File(tempDir, ".gitignore").toPath(), """
                ignored/*
                !ignored/keep.txt
                """, StandardCharsets.UTF_8);
        Files.createDirectories(new File(tempDir, "ignored").toPath());
        Files.writeString(new File(tempDir, "visible.txt").toPath(), "ok", StandardCharsets.UTF_8);
        Files.writeString(new File(tempDir, "ignored/hidden.txt").toPath(), "hidden", StandardCharsets.UTF_8);
        Files.writeString(new File(tempDir, "ignored/keep.txt").toPath(), "keep", StandardCharsets.UTF_8);

        Set<String> ignored = QuickNoteGitIgnoreUtil.findIgnoredPaths(tempDir,
                List.of("visible.txt", "ignored/hidden.txt", "ignored/keep.txt"));

        assertTrue(ignored.contains("ignored/hidden.txt"));
        assertFalse(ignored.contains("visible.txt"));
        assertFalse(ignored.contains("ignored/keep.txt"));
    }

    @Test
    void findIgnoredPaths_marksIgnoredFolder() throws Exception {
        runGit(tempDir, "init");
        Files.writeString(new File(tempDir, ".gitignore").toPath(), "private/\n", StandardCharsets.UTF_8);

        Set<String> ignored = QuickNoteGitIgnoreUtil.findIgnoredPaths(tempDir,
                List.of("private", "private/", "public", "public/"));

        assertTrue(ignored.contains("private") || ignored.contains("private/"));
        assertFalse(ignored.contains("public"));
    }

    @Test
    void normalizeRelativePath_stripsLeadingNoise() {
        assertEquals("notes/a.txt", QuickNoteGitIgnoreUtil.normalizeRelativePath("./notes/a.txt"));
        assertEquals("notes", QuickNoteGitIgnoreUtil.normalizeRelativePath("notes/"));
    }

    private static void runGit(File dir, String... args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        builder.command(command);
        builder.directory(dir);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("git command failed: " + String.join(" ", args));
        }
    }
}
