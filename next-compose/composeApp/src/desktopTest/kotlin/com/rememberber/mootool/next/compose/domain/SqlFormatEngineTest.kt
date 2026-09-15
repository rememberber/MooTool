package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlFormatEngineTest {
    @Test
    fun uppercasesClausesAndBreaksSelectList() {
        val formatted = SqlFormatEngine.format("select a, b from users where id = 1", "MySQL", 2)
        assertTrue(formatted.startsWith("SELECT"), formatted)
        assertTrue(formatted.contains("\nFROM"), formatted)
        assertTrue(formatted.contains("\nWHERE"), formatted)
        assertTrue(formatted.contains("\n  a,"), formatted)
    }

    @Test
    fun convertsIdentifierQuotesByDialect() {
        val postgres = SqlFormatEngine.format("select `name` from `user`", "PostgreSQL", 2)
        assertTrue(postgres.contains("\"name\""), postgres)
        assertTrue(postgres.contains("\"user\""), postgres)
        val mysql = SqlFormatEngine.format("select \"name\" from \"user\"", "MySQL", 2)
        assertTrue(mysql.contains("`name`"), mysql)
        val tsql = SqlFormatEngine.format("select \"name\" from \"user\"", "SQL Server Transact-SQL", 2)
        assertTrue(tsql.contains("[name]"), tsql)
    }

    @Test
    fun emptyInputStaysEmpty() {
        assertEquals("", SqlFormatEngine.format("   ", "Standard SQL"))
    }
}

class DocumentFormatEngineTest {
    @Test
    fun formatsJsonAndSqlBySyntax() {
        val json = DocumentFormatEngine.format("{\"a\":1}", "application/json", "MySQL", 2)
        assertTrue(json.contains("\n"))
        val sql = DocumentFormatEngine.format("select 1", "text/sql", "MySQL", 2)
        assertTrue(sql.uppercase().startsWith("SELECT"))
    }

    @Test
    fun mapsMimeToRstaAndEditorFont() {
        assertEquals(org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_SQL, DocumentFormatEngine.rstaSyntax("text/sql"))
        assertEquals("Monospaced", DocumentFormatEngine.editorFont("ui-monospace"))
        assertEquals("SansSerif", DocumentFormatEngine.editorFont("sans-serif"))
    }
}
