package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class QuickNoteVaultUtilTest {

    @org.junit.Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousVaultPath;

    @Before
    public void setUp() throws Exception {
        previousVaultPath = App.config.getQuickNoteVaultPath();
        App.config.setQuickNoteVaultPath(temporaryFolder.newFolder("vault").getAbsolutePath());
        QuickNoteVaultUtil.resetVaultCache();
    }

    @After
    public void tearDown() {
        App.config.setQuickNoteVaultPath(previousVaultPath);
        QuickNoteVaultUtil.resetVaultCache();
    }

    @Test
    public void normalizeRelativePath_collapsesPathInsideVault() {
        Assert.assertEquals("b.txt", QuickNoteVaultUtil.normalizeRelativePath("a/../b.txt"));
        Assert.assertEquals("notes/a.txt", QuickNoteVaultUtil.normalizeRelativePath("/notes//a.txt"));
    }

    @Test
    public void normalizeRelativePath_rejectsEscapingVault() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> QuickNoteVaultUtil.normalizeRelativePath("../outside.txt"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> QuickNoteVaultUtil.normalizeRelativePath("a/../../outside.txt"));
    }

    @Test
    public void toAbsoluteFile_keepsResolvedPathUnderVault() {
        File resolved = QuickNoteVaultUtil.toAbsoluteFile("folder/../note.txt");

        Assert.assertTrue(resolved.toPath().toAbsolutePath().normalize()
                .startsWith(QuickNoteVaultUtil.getVaultDir().toPath().toAbsolutePath().normalize()));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> QuickNoteVaultUtil.toAbsoluteFile("../outside.txt"));
    }

    @Test
    public void sanitizeFileName_replacesPathSeparators() {
        Assert.assertEquals("a_b_c", QuickNoteVaultUtil.sanitizeFileName("a/b\\c"));
        Assert.assertEquals("_hidden", QuickNoteVaultUtil.sanitizeFileName(".hidden"));
        Assert.assertEquals("__", QuickNoteVaultUtil.sanitizeFileName(".."));
    }
}
