package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateDownloadManagerTest {

    @Test
    public void pendingDirIsUnderMooToolConfigHome() {
        File dir = UpdateDownloadManager.pendingDir();
        assertEquals(new File(SystemUtil.CONFIG_HOME, "pending-updates"), dir);
        assertTrue(dir.getAbsolutePath().contains(".MooTool"));
    }
}
