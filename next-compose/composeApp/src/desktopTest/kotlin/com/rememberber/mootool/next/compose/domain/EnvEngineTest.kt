package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EnvEngineTest {
    @Test
    fun parsesAndUpdatesWhileKeepingUnknownLines() {
        val original = """
            # keep me
            export FOO='bar'
            PATH=/usr/bin
            not an assignment
            export FOO='old'
            
        """.trimIndent() + "\n"
        val parsed = EnvEngine.parseContent(original)
        assertEquals("old", parsed.first { it.key == "FOO" }.value)
        assertEquals("/usr/bin", parsed.first { it.key == "PATH" }.value)
        val updated = EnvEngine.updateContent(original, "FOO", "baz", shellExport = true)
        assertTrue(updated.contains("# keep me"))
        assertTrue(updated.contains("not an assignment"))
        assertTrue(updated.contains("export FOO='baz'"))
        assertEquals(1, updated.lineSequence().count { it.contains("FOO=") })
        val deleted = EnvEngine.updateContent(updated, "FOO", null, shellExport = true)
        assertFalse(deleted.contains("FOO="))
        assertTrue(deleted.contains("# keep me"))
        val quoted = EnvEngine.updateContent("", "MSG", "it's fine", shellExport = true)
        assertEquals("it's fine", EnvEngine.parseContent(quoted).first().value)
        assertFailsWith<EnvException> { EnvEngine.updateContent("", "1ABC", "x", true) }
        assertFailsWith<EnvException> { EnvEngine.updateContent("", "FOO", "a\nb", true) }
    }

    @Test
    fun userFileRoundTripCreatesBackupAndDoesNotMutateProcessMap() {
        val root = Files.createTempDirectory("compose-env-")
        val user = root.resolve("environment")
        val system = root.resolve("system-env")
        val backups = root.resolve("backups")
        val profile = root.resolve(".zshenv")
        user.parent?.let { }
        user.writeText("export EXISTING='keep'\n# comment\n")
        val config = EnvStoreConfig(
            userFile = user,
            systemFile = system,
            backupDir = backups,
            shellProfile = profile,
            installShellHook = true,
            allowElevation = false,
            applyLaunchctl = false,
            osFamily = OsFamily.Mac
        )
        val beforeProcess = System.getenv().toMap()
        val written = EnvEngine.set(config, EnvPersistScope.User, "COMPOSE_ENV_TEST", "hello")
        assertEquals("hello", written.snapshot.user.first { it.key == "COMPOSE_ENV_TEST" }.value)
        assertEquals("keep", written.snapshot.user.first { it.key == "EXISTING" }.value)
        assertTrue(user.readText().contains("# comment"))
        assertNotNull(written.backupPath)
        assertTrue(Files.exists(java.nio.file.Path.of(written.backupPath!!)))
        assertTrue(profile.readText().contains(EnvEngine.SHELL_HOOK_BEGIN))
        assertTrue(profile.readText().contains(EnvEngine.shellQuote(user.toString())))
        assertFalse(profile.readText().contains("# >>> MooTool environment >>>"))
        assertEquals(beforeProcess, System.getenv().toMap())
        assertTrue(written.snapshot.process.none { it.key == "COMPOSE_ENV_TEST" && it.value == "hello" } || beforeProcess.containsKey("COMPOSE_ENV_TEST"))
        val runtime = written.snapshot.runtime
        assertTrue(runtime.any { it.key == "java.version" && it.value.isNotBlank() })
        assertTrue(runtime.any { it.key == "mootool.product" && it.value.contains("Compose") })
        val deleted = EnvEngine.delete(config, EnvPersistScope.User, "COMPOSE_ENV_TEST")
        assertTrue(deleted.snapshot.user.none { it.key == "COMPOSE_ENV_TEST" })
        assertEquals(beforeProcess, System.getenv().toMap())
        val export = EnvEngine.formatExport(deleted.snapshot)
        assertTrue(export.contains("Compose runtime"))
        assertFalse(export.contains("Electron runtime"))
    }

    @Test
    fun systemWriteWithoutElevationKeepsOriginalFile() {
        val root = Files.createTempDirectory("compose-env-sys-")
        val locked = root.resolve("locked")
        Files.createDirectories(locked)
        val system = locked.resolve("environment")
        val config = EnvStoreConfig(
            userFile = root.resolve("user-env"),
            systemFile = system,
            backupDir = root.resolve("backups"),
            shellProfile = root.resolve(".profile"),
            installShellHook = false,
            allowElevation = false,
            applyLaunchctl = false,
            osFamily = OsFamily.Mac
        )
        check(locked.toFile().setWritable(false))
        val error = assertFailsWith<EnvException> {
            EnvEngine.set(config, EnvPersistScope.System, "SYS_KEY", "nope")
        }
        assertEquals(EnvErrorCode.PERMISSION, error.code)
        assertFalse(Files.exists(system))
        val snapshot = EnvEngine.snapshot(config)
        assertTrue(snapshot.system.isEmpty())
    }
}
