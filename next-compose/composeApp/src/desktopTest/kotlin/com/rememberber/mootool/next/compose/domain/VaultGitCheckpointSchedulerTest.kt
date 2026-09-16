package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.ColorFavoriteStore
import com.rememberber.mootool.next.compose.storage.CronFavoriteStore
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.HostProfileStore
import com.rememberber.mootool.next.compose.storage.HttpCollectionStore
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.TranslationStore
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.RegexFavoriteStore
import java.sql.DriverManager
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VaultGitCheckpointSchedulerTest {
    @Test
    fun waitsForIdleThresholdAndCheckpointsOncePerActivity() {
        var now = 1_000L
        var calls = 0
        val scheduler = VaultGitCheckpointScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { false },
            idleMilliseconds = { 30_000 },
            inactiveMilliseconds = { 120_000 },
            checkpoint = { GitActionResult(true, "Done").also { calls += 1 } },
            now = { now }
        )
        scheduler.recordActivity("Update Quick Note")
        now += 29_999
        assertEquals(false, scheduler.evaluate())
        now += 1
        assertEquals(true, scheduler.evaluate())
        assertEquals(1, calls)
        assertEquals(false, scheduler.evaluate())
        now += 1
        scheduler.recordActivity("Update JSON snippet")
        now += 30_000
        assertEquals(true, scheduler.evaluate())
        assertEquals(2, calls)
    }

    @Test
    fun waitsForEditorAutosaveAndSupportsInactiveThreshold() {
        var now = 10_000L
        var dirty = true
        var calls = 0
        val scheduler = VaultGitCheckpointScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { dirty },
            idleMilliseconds = { 30_000 },
            inactiveMilliseconds = { 120_000 },
            checkpoint = { GitActionResult(true, "Done").also { calls += 1 } },
            now = { now }
        )
        scheduler.recordActivity("Update Vault")
        scheduler.setWindowActive(false)
        now += 120_000
        assertEquals(false, scheduler.evaluate())
        dirty = false
        assertEquals(true, scheduler.evaluate())
        assertEquals(1, calls)
    }

    @Test
    fun retriesFailedCheckpointInsteadOfConsumingActivity() {
        var now = 1_000L
        var calls = 0
        val scheduler = VaultGitCheckpointScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { false },
            idleMilliseconds = { 30_000 },
            inactiveMilliseconds = { 120_000 },
            checkpoint = {
                calls += 1
                GitActionResult(calls > 1, if (calls > 1) "Done" else "failed")
            },
            now = { now }
        )
        scheduler.recordActivity("Update Vault")
        now += 30_000
        assertEquals(false, scheduler.evaluate())
        assertEquals(true, scheduler.evaluate())
        assertEquals(2, calls)
    }
}

class CrossProductImporterTest {
    @Test
    fun importsElectronNotesAndJavaSqliteWithoutTouchingSource() {
        val source = createTempDirectory("mootool-import-source-")
        source.resolve("config").createDirectories()
        source.resolve("config/config.setting").writeText(
            """
            [setting.common]
            locale=ja_JP
            [setting.custom]
            tabCompact=true
            """.trimIndent()
        )
        source.resolve("quick-notes").createDirectories()
        source.resolve("quick-notes").resolve("Work").createDirectories()
        source.resolve("quick-notes").resolve("Work").resolve("API ideas.txt").writeText(
            NoteFrontmatter.serialize(
                NoteMetadata.defaults("API ideas").copy(fontName = "PingFang SC", fontSize = 15),
                "# Hello\nneedle"
            )
        )
        source.resolve("mootool-next.json").writeText(
            """
            {
              "settings": {
                "general": { "language": "en-US" },
                "layout": {
                  "customGroups": [{ "id": "g1", "name": "开发常用", "toolIds": ["json", "http"] }]
                }
              }
            }
            """.trimIndent()
        )
        val db = source.resolve("MooTool.db")
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${db.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE t_quick_note (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT, color TEXT, style TEXT, font_name TEXT, font_size TEXT, syntax TEXT, line_wrap TEXT)")
                statement.execute("INSERT INTO t_quick_note VALUES (1, 'Database Note', 'Database note body', '2026-01-01', '2026-01-02', 'blue', '', 'Monaco', '15', 'text/plain', '1')")
                statement.execute("CREATE TABLE t_json_beauty (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT)")
                statement.execute("""INSERT INTO t_json_beauty VALUES (1, 'Database JSON', '{"database":true}', '2026-01-01', '2026-01-02')""")
                statement.execute("CREATE TABLE t_favorite_regex_list (id INTEGER PRIMARY KEY, title TEXT, remark TEXT)")
                statement.execute("CREATE TABLE t_favorite_regex_item (id INTEGER PRIMARY KEY, list_id INTEGER, name TEXT, value TEXT, remark TEXT, create_time TEXT)")
                statement.execute("INSERT INTO t_favorite_regex_list VALUES (1, 'Work', '')")
                statement.execute("INSERT INTO t_favorite_regex_item VALUES (1, 1, 'Moo', '^moo$', '', '2026-01-01')")
                statement.execute("CREATE TABLE t_favorite_cron_list (id INTEGER PRIMARY KEY, title TEXT, remark TEXT)")
                statement.execute("CREATE TABLE t_favorite_cron_item (id INTEGER PRIMARY KEY, list_id INTEGER, name TEXT, value TEXT, remark TEXT, create_time TEXT)")
                statement.execute("INSERT INTO t_favorite_cron_list VALUES (1, 'Ops', '')")
                statement.execute("INSERT INTO t_favorite_cron_item VALUES (1, 1, 'Daily', '0 0 * * *', '', '2026-01-01')")
                statement.execute("CREATE TABLE t_favorite_color_list (id INTEGER PRIMARY KEY, title TEXT, remark TEXT)")
                statement.execute("CREATE TABLE t_favorite_color_item (id INTEGER PRIMARY KEY, list_id INTEGER, name TEXT, value TEXT, remark TEXT, create_time TEXT)")
                statement.execute("INSERT INTO t_favorite_color_list VALUES (1, 'Palette', '')")
                statement.execute("INSERT INTO t_favorite_color_item VALUES (1, 1, 'Coral', '#de8f7d', '', '2026-01-01')")
                statement.execute(
                    "CREATE TABLE t_func_history (id INTEGER PRIMARY KEY, func_type TEXT, summary TEXT, input_text TEXT, output_text TEXT, extra_data TEXT, create_time TEXT)"
                )
                statement.execute(
                    """INSERT INTO t_func_history VALUES (1, 'json', 'Legacy history', '{"a":1}', '{"a": 1}', NULL, '2026-01-01')"""
                )
                statement.execute(
                    "CREATE TABLE t_func_content (id INTEGER PRIMARY KEY, func TEXT, content TEXT, remark TEXT, create_time TEXT, modified_time TEXT)"
                )
                statement.execute("INSERT INTO t_func_content VALUES (1, 'regex', '^draft$', 'Legacy draft', '2026-01-01', '2026-01-02')")
                statement.execute("CREATE TABLE t_qr_code (id INTEGER PRIMARY KEY, content TEXT, create_time TEXT, modified_time TEXT)")
                statement.execute("INSERT INTO t_qr_code VALUES (1, 'https://mootool.test', '2026-01-01', '2026-01-01')")
                statement.execute(
                    """
                    CREATE TABLE t_msg_http (
                      id INTEGER PRIMARY KEY, msg_name TEXT, method TEXT, url TEXT, params TEXT, headers TEXT,
                      cookies TEXT, body TEXT, body_type TEXT, create_time TEXT, modified_time TEXT,
                      response_body TEXT, response_headers TEXT, response_cookies TEXT
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """INSERT INTO t_msg_http VALUES (
                      1, 'API', 'GET', 'https://example.com', '[]', '[]', '[]', '', 'application/json',
                      '2026-01-01', '2026-01-02', 'ok', '', ''
                    )"""
                )
                statement.execute(
                    """
                    CREATE TABLE t_http_request_history (
                      id INTEGER PRIMARY KEY, request_id INTEGER, title TEXT, method TEXT, url TEXT,
                      params TEXT, headers TEXT, cookies TEXT, body TEXT, body_type TEXT,
                      response_body TEXT, response_headers TEXT, response_cookies TEXT,
                      status TEXT, cost_time INTEGER, create_time TEXT, modified_time TEXT
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """INSERT INTO t_http_request_history VALUES (
                      1, 1, 'Example', 'GET', 'https://example.com/history', '[]', '[]', '[]', '', 'application/json',
                      'body', '', '', '200', 12, '2026-01-03', '2026-01-03'
                    )"""
                )
                statement.execute("CREATE TABLE t_host (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT)")
                statement.execute("INSERT INTO t_host VALUES (1, 'Dev', '127.0.0.1 localhost', '2026-01-01', '2026-01-01')")
                statement.execute(
                    "CREATE TABLE t_translation_word (id INTEGER PRIMARY KEY, source_text TEXT, target_text TEXT, source_lang TEXT, target_lang TEXT, remark TEXT, create_time TEXT, modified_time TEXT)"
                )
                statement.execute("INSERT INTO t_translation_word VALUES (1, 'hello', '你好', 'en', 'zh-CN', '', '2026-01-01', '2026-01-01')")
                statement.execute(
                    "CREATE TABLE t_translation_history (id INTEGER PRIMARY KEY, source_text TEXT, target_text TEXT, source_lang TEXT, target_lang TEXT, translator_type TEXT, create_time TEXT)"
                )
                statement.execute("INSERT INTO t_translation_history VALUES (1, 'world', '世界', 'en', 'zh-CN', 'google', '2026-01-02')")
            }
        }
        val originalHash = source.resolve("quick-notes/Work/API ideas.txt").readText()
        val preview = CrossProductImporter.inspect(source)
        assertEquals("electron-next", preview.sourceKind)
        assertTrue(preview.configFound)
        assertTrue(preview.databaseFound)
        assertTrue(preview.legacyJavaConfigFound)
        assertTrue(LegacyJavaSettings.hasMigratableSettings(preview.legacyJavaConfig))
        assertEquals(1, preview.customGroups)
        assertTrue(preview.notes >= 2)
        assertTrue(preview.jsonItems >= 1)

        val target = createTempDirectory("mootool-import-target-")
        val directories = AppPaths.resolve(target.toString()).also { it.ensureCreated() }
        val notes = NoteVault(directories)
        val json = JsonVault(directories)
        val regexStore = RegexFavoriteStore(directories)
        val cronStore = CronFavoriteStore(directories)
        assertEquals(1, preview.regexFavorites.size)
        assertEquals(1, preview.cronFavorites.size)
        assertEquals(1, preview.colorFavorites.size)
        assertEquals(1, preview.httpCollections.size)
        assertEquals(1, preview.hostProfiles.size)
        assertEquals(1, preview.translationWords.size)
        assertEquals(1, preview.translationHistory.size)
        assertEquals(4, preview.legacyHistory.size)
        val colorStore = ColorFavoriteStore(directories)
        val database = AppDatabase(directories)
        val historyRepo = HistoryRepository(database)
        val migrationRows = LegacyMigrationRowRepository(database)
        val httpStore = HttpCollectionStore(directories)
        val hostStore = HostProfileStore(directories)
        val translationStore = TranslationStore(directories)
        val applied = CrossProductImporter.apply(
            preview,
            notes,
            json,
            emptySet(),
            regexStore,
            cronStore,
            colorStore,
            historyRepo,
            httpStore,
            hostStore,
            translationStore,
            migrationRows
        )
        assertTrue(applied.importedNotes >= 2)
        assertTrue(applied.importedJson >= 1)
        assertEquals(1, applied.importedRegex)
        assertEquals(1, applied.importedCron)
        assertEquals(1, applied.importedColor)
        assertEquals(4, applied.importedHistory)
        assertEquals(1, applied.importedHttp)
        assertEquals(1, applied.importedHosts)
        assertEquals(1, applied.importedTranslationWords)
        assertEquals(1, applied.importedTranslationHistory)
        assertTrue(applied.importedSettings)
        assertTrue(applied.importedElectronSettings)
        assertNotNull(preview.electronSettings)
        assertEquals("^moo$", regexStore.list("moo").first().pattern)
        assertEquals("0 0 * * *", cronStore.list("Daily").first().expression)
        assertEquals("#de8f7d", colorStore.items(colorStore.folders().first().id).first().value)
        assertTrue(historyRepo.list(ToolId.Json.id).any { it.input.contains("\"a\":1") })
        assertTrue(historyRepo.list(ToolId.Regex.id).any { it.input == "^draft$" })
        assertTrue(historyRepo.list(ToolId.QrCode.id).any { it.input.contains("mootool.test") })
        assertTrue(historyRepo.list(ToolId.Http.id).any { it.input.contains("example.com") })
        assertEquals("https://example.com", httpStore.list("API").first().draft.url)
        assertEquals("Dev", hostStore.list("Dev").first().name)
        assertEquals("hello", translationStore.listWords("hello").first().sourceText)
        assertEquals("world", translationStore.listHistory("world").first().sourceText)
        database.close()
        assertEquals(originalHash, source.resolve("quick-notes/Work/API ideas.txt").readText())
        assertTrue(notes.list("needle", includeContent = true).isNotEmpty())
        assertTrue(json.list("database", includeContent = true).isNotEmpty())
        val skipped = CrossProductImporter.apply(preview, notes, json, setOf(preview.fingerprint))
        assertEquals(0, skipped.importedNotes)
    }
}
