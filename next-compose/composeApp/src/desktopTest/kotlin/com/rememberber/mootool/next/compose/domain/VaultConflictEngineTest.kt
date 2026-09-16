package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultConflictEngineTest {
    @Test
    fun ignoresTempGitAndAttachmentPaths() {
        assertTrue(VaultConflictEngine.shouldIgnore(".hidden.md", ignoreAttachments = false))
        assertTrue(VaultConflictEngine.shouldIgnore("note.md.tmp", ignoreAttachments = false))
        assertTrue(VaultConflictEngine.shouldIgnore(".git/HEAD", ignoreAttachments = false))
        assertTrue(VaultConflictEngine.shouldIgnore("attachments/a.png", ignoreAttachments = true))
        assertFalse(VaultConflictEngine.shouldIgnore("attachments/a.png", ignoreAttachments = false))
        assertFalse(VaultConflictEngine.shouldIgnore("note.md", ignoreAttachments = true))
    }

    @Test
    fun classifiesReloadConflictAndOwnWrites() {
        val path = "note.md"
        assertEquals(
            VaultChangeKind.Reload,
            VaultConflictEngine.decide(path, path, "saved", "saved", "external")
        )
        assertEquals(
            VaultChangeKind.Conflict,
            VaultConflictEngine.decide(path, path, "local", "saved", "external")
        )
        assertEquals(
            VaultChangeKind.Ignored,
            VaultConflictEngine.decide(path, path, "local", "saved", "saved")
        )
        assertEquals(
            VaultChangeKind.Ignored,
            VaultConflictEngine.decide(path, path, "same", "saved", "same")
        )
        assertEquals(
            VaultChangeKind.Ignored,
            VaultConflictEngine.decide(
                path,
                path,
                "local",
                "saved",
                "written",
                expectedOwnHash = VaultConflictEngine.sha256Text("written")
            )
        )
        assertEquals(
            VaultChangeKind.TreeChanged,
            VaultConflictEngine.decide("other.md", path, "local", "saved", "external")
        )
        assertEquals(
            VaultChangeKind.Deleted,
            VaultConflictEngine.decide(path, path, "saved", "saved", null)
        )
        assertEquals(
            VaultChangeKind.Conflict,
            VaultConflictEngine.decide(path, path, "local", "saved", null)
        )
    }

    @Test
    fun refusesOverwriteWhenDiskDiverged() {
        assertTrue(VaultConflictEngine.canOverwrite("saved", "saved", "local"))
        assertTrue(VaultConflictEngine.canOverwrite("saved", "local", "local"))
        assertFalse(VaultConflictEngine.canOverwrite("saved", "external", "local"))
        assertFalse(VaultConflictEngine.canOverwrite("saved", null, "local"))
        assertEquals("folder/note.local-42.md", VaultConflictEngine.conflictCopyName("folder/note.md", 42))
        assertEquals("draft.local-7.json", VaultConflictEngine.conflictCopyName("draft.json", 7))
    }

    @Test
    fun snapshotDiffAndPollingSeeExternalWrites() {
        val root = Files.createTempDirectory("mootool-compose-vault-watch-")
        try {
            root.resolve("note.md").writeText("old")
            val first = VaultRevisionMonitor(root, ignoreAttachments = true, intervalMs = 50) {}.snapshot()
            assertTrue(first.containsKey("note.md"))
            root.resolve("note.md").writeText("new")
            root.resolve("skip.tmp").writeText("tmp")
            val monitor = VaultRevisionMonitor(root, ignoreAttachments = true, intervalMs = 50) {}
            val next = monitor.snapshot()
            assertEquals(listOf("note.md"), monitor.diff(first, next))
            assertFalse(next.containsKey("skip.tmp"))

            val latch = CountDownLatch(1)
            val seen = mutableListOf<String>()
            val live = VaultRevisionMonitor(root, ignoreAttachments = true, intervalMs = 60) { paths ->
                seen += paths
                latch.countDown()
            }
            live.start()
            try {
                Thread.sleep(90)
                root.resolve("note.md").writeText("watched")
                assertTrue(latch.await(2, TimeUnit.SECONDS), "poller missed external write")
                assertTrue(seen.any { "note.md" in it })
            } finally {
                live.close()
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun rebaselineAfterBulkImportAvoidsReplayDiff() {
        val root = Files.createTempDirectory("mootool-compose-vault-rebase-")
        try {
            root.resolve("seed.md").writeText("v1")
            val monitor = VaultRevisionMonitor(root, ignoreAttachments = true, intervalMs = 50) {}
            monitor.start()
            monitor.rebaseline()
            root.resolve("imported.md").writeText("new")
            val latch = CountDownLatch(1)
            val seen = mutableListOf<List<String>>()
            val live = VaultRevisionMonitor(root, ignoreAttachments = true, intervalMs = 60) { paths ->
                seen += paths
                latch.countDown()
            }
            live.start()
            live.rebaseline()
            try {
                Thread.sleep(90)
                root.resolve("later.md").writeText("x")
                assertTrue(latch.await(2, TimeUnit.SECONDS))
                assertEquals(listOf("later.md"), seen.single())
            } finally {
                live.close()
            }
            monitor.close()
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun rebaselineAfterLocalDeleteAvoidsSpuriousPoll() {
        val root = Files.createTempDirectory("mootool-compose-vault-delete-rebase-")
        try {
            root.resolve("keep.md").writeText("k")
            root.resolve("drop.md").writeText("d")
            val seen = mutableListOf<List<String>>()
            val monitor = VaultRevisionMonitor(root, ignoreAttachments = true, intervalMs = 60) { paths ->
                seen += paths
            }
            monitor.start()
            monitor.rebaseline()
            Thread.sleep(90)
            Files.delete(root.resolve("drop.md"))
            monitor.rebaseline()
            Thread.sleep(150)
            assertTrue(seen.isEmpty(), "local delete after rebaseline should not surface as external change")
            monitor.close()
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
