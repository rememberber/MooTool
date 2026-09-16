package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.SessionManager
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyToolDraftApplierTest {
    @Test
    fun appliesCodeRunRegexAndJsonDrafts() {
        val root = createTempDirectory("legacy-draft-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        val rows = listOf(
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:1",
                toolId = "java",
                legacyFunc = "JavaConsole",
                operation = "draft",
                summary = "Java",
                input = "class Legacy {}",
                output = "",
                options = "",
                createdAt = "2026-01-01"
            ),
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:2",
                toolId = "regex",
                legacyFunc = "Regex",
                operation = "draft",
                summary = "Regex",
                input = "^moo$",
                output = "",
                options = "",
                createdAt = "2026-01-02"
            ),
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:3",
                toolId = "json",
                legacyFunc = "JsonBeauty",
                operation = "draft",
                summary = "JSON",
                input = """{"from":"legacy"}""",
                output = "",
                options = "",
                createdAt = "2026-01-03"
            )
        )
        val applied = LegacyToolDraftApplier.apply(sessions, rows)
        assertEquals(3, applied)
        assertTrue(sessions.codeRunSession().javaEditor.text.contains("class Legacy"))
        assertEquals("^moo$", sessions.regexSession().source)
        assertTrue(sessions.jsonSession().editor.text.contains("from"))
        database.close()
    }

    @Test
    fun appliesTextDiffTimeAndCalculatorDrafts() {
        val root = createTempDirectory("legacy-draft-misc-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        val rows = listOf(
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:10",
                toolId = "textDiff",
                legacyFunc = "TextDiff_left",
                operation = "draft",
                summary = "L",
                input = "left text",
                output = "",
                options = "",
                createdAt = ""
            ),
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:11",
                toolId = "textDiff",
                legacyFunc = "TextDiff_right",
                operation = "draft",
                summary = "R",
                input = "right text",
                output = "",
                options = "",
                createdAt = ""
            ),
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:12",
                toolId = "timeConvert",
                legacyFunc = "TimeConvert",
                operation = "draft",
                summary = "T",
                input = "1700000000",
                output = "",
                options = "",
                createdAt = ""
            ),
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:13",
                toolId = "calculator",
                legacyFunc = "Calculator",
                operation = "draft",
                summary = "C",
                input = "1 + 1 = 2",
                output = "",
                options = "",
                createdAt = ""
            )
        )
        assertEquals(4, LegacyToolDraftApplier.apply(sessions, rows))
        assertEquals("left text", sessions.diffSession().left)
        assertEquals("right text", sessions.diffSession().right)
        assertEquals("1700000000", sessions.timeSession().timestamp)
        assertEquals("1 + 1", sessions.calculatorSession().expression)
        assertEquals("2", sessions.calculatorSession().result)
        database.close()
    }

    @Test
    fun appliesQrCodeDraftFromJavaFuncName() {
        val root = createTempDirectory("legacy-draft-qr-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        val rows = listOf(
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:20",
                toolId = "qrCode",
                legacyFunc = "QrCode",
                operation = "draft",
                summary = "QR",
                input = "https://mootool.example\nignored",
                output = "",
                options = "",
                createdAt = ""
            )
        )
        assertEquals(1, LegacyToolDraftApplier.apply(sessions, rows))
        assertEquals("https://mootool.example", sessions.qrSession().content)
        database.close()
    }

    @Test
    fun appliesQrCodeDraftFromJavaConsoleLog() {
        val root = createTempDirectory("legacy-draft-qr-log-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        val log = """

            2026-01-01 12:00:00.000 

            生成:
            https://mootool.example/qr
        """.trimIndent()
        val rows = listOf(
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:21",
                toolId = "qrCode",
                legacyFunc = "QrCode",
                operation = "draft",
                summary = "QR",
                input = log,
                output = "",
                options = "",
                createdAt = ""
            )
        )
        assertEquals(1, LegacyToolDraftApplier.apply(sessions, rows))
        assertEquals("https://mootool.example/qr", sessions.qrSession().content)
        database.close()
    }

    @Test
    fun appliesTimeConvertJavaLogToSessionAndHistory() {
        val root = createTempDirectory("legacy-draft-time-log-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        val history = HistoryRepository(database)
        val log = """
            时间戳: 1700000000 --> 时间(Asia/Shanghai): 2023-11-15 06:13:20
            Time (UTC): 2023-11-15 06:13:20 --> Timestamp: 1700000001
        """.trimIndent()
        val rows = listOf(
            ImportedLegacyHistory(
                dedupeKey = "t_func_content:30",
                toolId = "timeConvert",
                legacyFunc = "TimeConvert",
                operation = "draft",
                summary = "T",
                input = log,
                output = "",
                options = "",
                createdAt = ""
            )
        )
        assertEquals(1, LegacyToolDraftApplier.apply(sessions, rows, history))
        assertEquals("2023-11-15 06:13:20", sessions.timeSession().localTime)
        assertEquals("1700000001", sessions.timeSession().timestamp)
        assertEquals("UTC", sessions.timeSession().zone)
        assertEquals(2, history.list(ToolId.TimeConvert.id).size)
        database.close()
    }
}
