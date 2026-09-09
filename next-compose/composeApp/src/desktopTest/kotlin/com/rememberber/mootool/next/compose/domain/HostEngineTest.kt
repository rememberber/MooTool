package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.storage.HostProfileStore
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HostEngineTest {
    @Test
    fun normalizesCrlfAndRejectsNul() {
        assertEquals("127.0.0.1 localhost\n", HostEngine.normalize("127.0.0.1 localhost\r\n\r\n"))
        assertEquals("127.0.0.1 localhost\n", HostEngine.normalize("127.0.0.1 localhost"))
        assertFailsWith<HostException> { HostEngine.normalize("bad\u0000value") }
        val oversized = "a".repeat(HostEngine.MAX_BYTES + 1)
        val invalid = assertFailsWith<HostException> { HostEngine.normalize(oversized) }
        assertEquals(HostErrorCode.INVALID, invalid.code)
    }

    @Test
    fun rejectsInvalidEntriesAndAcceptsIpv4Ipv6() {
        assertEquals(HostErrorCode.INVALID, assertFailsWith<HostException> { HostEngine.validate("not a host") }.code)
        assertEquals(HostErrorCode.INVALID, assertFailsWith<HostException> { HostEngine.validate("127.0.0.1") }.code)
        assertEquals(HostErrorCode.INVALID, assertFailsWith<HostException> { HostEngine.validate("999.0.0.1 localhost") }.code)
        assertEquals(HostErrorCode.EMPTY, assertFailsWith<HostException> { HostEngine.validate("  \n# only comment\n") }.code)
        val ok = HostEngine.validate("127.0.0.1 localhost\r\n::1 localhost # loopback\n# comment\n")
        assertEquals("127.0.0.1 localhost\n::1 localhost # loopback\n# comment\n", ok)
        assertTrue(HostEngine.DEFAULT_TEMPLATE.contains("MooTool Next Compose"))
        assertFalse(HostEngine.DEFAULT_TEMPLATE.contains("# MooTool hosts profile\n"))
    }

    @Test
    fun profileStoreCrudAndSearchStayInComposeDataDir() {
        val root = Files.createTempDirectory("compose-host-store-")
        val directories = AppDirectories(root, root.resolve("data"), root.resolve("cache"), root.resolve("logs"))
        directories.ensureCreated()
        val store = HostProfileStore(directories)
        val saved = store.save(null, "dev", "127.0.0.1 localhost\n")
        val updated = store.save(saved.id, "dev-local", "127.0.0.1 app.local\n")
        assertEquals(saved.id, updated.id)
        assertEquals("dev-local", store.get(saved.id)?.name)
        val copy = store.duplicate(saved.id)
        assertNotEquals(saved.id, copy.id)
        assertEquals("dev-local copy", copy.name)
        assertEquals(2, store.list("app.local", includeContent = true).size)
        assertEquals(0, store.list("app.local", includeContent = false).size)
        assertEquals(1, store.list("dev-local copy", includeContent = false).size)
        store.delete(saved.id)
        assertTrue(store.list().none { it.id == saved.id })
        assertTrue(store.get(copy.id) != null)
        val stored = directories.dataRoot.resolve("hosts").resolve("profiles.json").readText()
        assertTrue(stored.contains(copy.id))
        assertFalse(stored.contains("/etc/hosts"))
    }

    @Test
    fun applyWritesTempSystemFileWithBackupAndConflictCheck() {
        val root = Files.createTempDirectory("compose-host-apply-")
        val system = root.resolve("hosts")
        system.writeText("127.0.0.1 old.local\n")
        val config = HostApplyConfig(
            systemPath = system,
            backupDir = root.resolve("backups"),
            allowElevation = false,
            flushDns = false,
            osFamily = OsFamily.Mac
        )
        val current = HostEngine.readSystem(config)
        val next = "127.0.0.1 new.local\n::1 localhost\n"
        val applied = HostEngine.apply(config, next, current.fingerprint)
        assertEquals(HostEngine.normalize(next), system.readText())
        assertNotNull(applied.backupPath)
        assertEquals("127.0.0.1 old.local\n", java.nio.file.Path.of(applied.backupPath!!).readText())
        assertTrue(applied.diff.contains("-") || applied.diff.contains("old.local"))
        system.writeText("127.0.0.1 other.local\n")
        val conflict = assertFailsWith<HostException> {
            HostEngine.apply(config, "127.0.0.1 third.local\n", applied.system.fingerprint)
        }
        assertEquals(HostErrorCode.CONFLICT, conflict.code)
        assertEquals("127.0.0.1 other.local\n", system.readText())
        val restored = HostEngine.restore(config, applied.backupPath!!, HostEngine.fingerprint(system.readText()))
        assertEquals("127.0.0.1 old.local\n", system.readText())
        assertEquals("127.0.0.1 old.local\n", restored.system.content)
    }

    @Test
    fun applyWithoutElevationKeepsLockedSystemFile() {
        val root = Files.createTempDirectory("compose-host-locked-")
        val locked = root.resolve("locked")
        Files.createDirectories(locked)
        val system = locked.resolve("hosts")
        system.writeText("127.0.0.1 keep.local\n")
        check(system.toFile().setWritable(false))
        check(locked.toFile().setWritable(false))
        val config = HostApplyConfig(
            systemPath = system,
            backupDir = root.resolve("backups"),
            allowElevation = false,
            flushDns = false,
            osFamily = OsFamily.Mac
        )
        val before = system.readText()
        val error = assertFailsWith<HostException> {
            HostEngine.apply(config, "127.0.0.1 change.local\n", HostEngine.fingerprint(before))
        }
        assertEquals(HostErrorCode.PERMISSION, error.code)
        assertEquals(before, system.readText())
    }
}
