package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.domain.QuickNoteGitModifiedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickNoteGitUtilTest {

    @TempDir
    File tempDir;

    @Test
    void isAuthFailure_detectsCommonHttpsAuthErrors() {
        assertTrue(QuickNoteGitUtil.isAuthFailure(
                "fatal: could not read Username for 'https://gitee.com': Device not configured"));
        assertTrue(QuickNoteGitUtil.isAuthFailure("remote: HTTP Basic: Access denied"));
        assertFalse(QuickNoteGitUtil.isAuthFailure("Everything up-to-date"));
    }

    @Test
    void formatFailureMessage_returnsFriendlyAuthHint() {
        String message = QuickNoteGitUtil.formatFailureMessage(
                "fatal: could not read Username for 'https://gitee.com': Device not configured");
        assertFalse(message.contains("Device not configured"));
    }

    @Test
    void unquoteGitStatusPath_decodesUtf8OctalSequence() {
        String quoted = "\"\\346\\234\\252\\345\\221\\275\\345\\220\\215_2026-06-19_08-09-48.txt\"";
        assertEquals("未命名_2026-06-19_08-09-48.txt", QuickNoteGitUtil.unquoteGitStatusPath(quoted));
    }

    @Test
    void unquoteGitStatusPath_returnsPlainPathAsIs() {
        assertEquals("未命名_2026-06-19_08-09-48.txt",
                QuickNoteGitUtil.unquoteGitStatusPath("未命名_2026-06-19_08-09-48.txt"));
    }

    @Test
    void listModifiedFilesDetailed_preservesRenameOriginalPath() throws Exception {
        initRepoWithFile();
        runGit(tempDir, "mv", "old.txt", "new.txt");

        List<QuickNoteGitModifiedFile> files = QuickNoteGitUtil.listModifiedFilesDetailed(tempDir);

        assertEquals(1, files.size());
        assertEquals("new.txt", files.get(0).getPath());
        assertEquals("old.txt", files.get(0).getOriginalPath());
        assertEquals("R ", files.get(0).getStatusCode());
        assertEquals("[renamed] old.txt -> new.txt", files.get(0).toString());
    }

    @Test
    void discardWorkingChanges_discardsRenamedFileCompletely() throws Exception {
        initRepoWithFile();
        runGit(tempDir, "mv", "old.txt", "new.txt");

        QuickNoteGitUtil.GitCommandResult result = QuickNoteGitUtil.discardWorkingChanges(
                tempDir, "new.txt", "R ");

        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(new File(tempDir, "old.txt").isFile());
        assertFalse(new File(tempDir, "new.txt").exists());
        assertTrue(QuickNoteGitUtil.listModifiedFilesDetailed(tempDir).isEmpty());
    }

    @Test
    void discardWorkingChanges_removesUntrackedNewFile() throws Exception {
        initRepoWithFile();
        File file = new File(tempDir, "new.txt");
        Files.writeString(file.toPath(), "new", StandardCharsets.UTF_8);

        QuickNoteGitUtil.GitCommandResult result = QuickNoteGitUtil.discardWorkingChanges(
                tempDir, "new.txt", "??");

        assertTrue(result.isSuccess(), result.getMessage());
        assertFalse(file.exists());
        assertTrue(QuickNoteGitUtil.listModifiedFilesDetailed(tempDir).isEmpty());
    }

    @Test
    void discardWorkingChanges_removesStagedNewFile() throws Exception {
        initRepoWithFile();
        File file = new File(tempDir, "new.txt");
        Files.writeString(file.toPath(), "new", StandardCharsets.UTF_8);
        runGit(tempDir, "add", "new.txt");

        QuickNoteGitUtil.GitCommandResult result = QuickNoteGitUtil.discardWorkingChanges(
                tempDir, "new.txt", "A ");

        assertTrue(result.isSuccess(), result.getMessage());
        assertFalse(file.exists());
        assertTrue(QuickNoteGitUtil.listModifiedFilesDetailed(tempDir).isEmpty());
    }

    @Test
    void discardWorkingChanges_restoresDeletedFile() throws Exception {
        initRepoWithFile();
        File file = new File(tempDir, "old.txt");
        Files.delete(file.toPath());

        QuickNoteGitUtil.GitCommandResult result = QuickNoteGitUtil.discardWorkingChanges(
                tempDir, "old.txt", " D");

        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(file.isFile());
        assertEquals("old", Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertTrue(QuickNoteGitUtil.listModifiedFilesDetailed(tempDir).isEmpty());
    }

    @Test
    void discardWorkingChanges_restoresStagedDeletedFile() throws Exception {
        initRepoWithFile();
        File file = new File(tempDir, "old.txt");
        Files.delete(file.toPath());
        runGit(tempDir, "add", "-A");

        QuickNoteGitUtil.GitCommandResult result = QuickNoteGitUtil.discardWorkingChanges(
                tempDir, "old.txt", "D ");

        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(file.isFile());
        assertEquals("old", Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertTrue(QuickNoteGitUtil.listModifiedFilesDetailed(tempDir).isEmpty());
    }

    private void initRepoWithFile() throws Exception {
        runGit(tempDir, "init");
        runGit(tempDir, "config", "user.email", "test@example.com");
        runGit(tempDir, "config", "user.name", "Test");
        Files.writeString(new File(tempDir, "old.txt").toPath(), "old", StandardCharsets.UTF_8);
        runGit(tempDir, "add", "old.txt");
        runGit(tempDir, "commit", "-m", "init");
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
