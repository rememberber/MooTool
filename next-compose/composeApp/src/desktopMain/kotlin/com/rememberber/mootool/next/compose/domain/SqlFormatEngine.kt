package com.rememberber.mootool.next.compose.domain

object SqlFormatEngine {
    val dialects: List<String> = listOf(
        "Standard SQL",
        "MySQL",
        "MariaDB",
        "PostgreSQL",
        "Oracle PL/SQL",
        "SQL Server Transact-SQL",
        "IBM DB2",
        "Couchbase N1QL",
        "Amazon Redshift",
        "Spark"
    )

    private val keywords = setOf(
        "SELECT", "FROM", "WHERE", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET",
        "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL", "CROSS", "ON", "AS", "AND", "OR", "NOT",
        "IN", "IS", "NULL", "LIKE", "BETWEEN", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END",
        "UNION", "ALL", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "ALTER",
        "DROP", "TABLE", "VIEW", "INDEX", "WITH", "RETURNING", "FETCH", "FIRST", "ROWS", "ONLY",
        "DISTINCT", "TOP", "ASC", "DESC", "INNER", "USING", "NATURAL", "EXCEPT", "INTERSECT",
        "OVER", "PARTITION", "WINDOW", "TRUE", "FALSE", "CAST", "COALESCE"
    )

    private val clauseStarters = setOf(
        "SELECT", "FROM", "WHERE", "GROUP", "ORDER", "HAVING", "LIMIT", "OFFSET",
        "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL", "CROSS", "UNION",
        "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "WITH", "RETURNING", "FETCH", "VALUES", "SET"
    )

    fun format(input: String, dialect: String, indent: Int = 2): String {
        if (input.trim().isEmpty()) return ""
        val tab = " ".repeat(indent.coerceIn(1, 8))
        val quotes = identifierQuotes(dialect)
        val tokens = tokenize(input)
        val out = StringBuilder()
        var depth = 0
        var extraIndent = 0
        var pendingNewline = false
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val upper = token.text.uppercase()
            val isKeyword = token.kind == SqlTokenKind.Word && upper in keywords
            val rendered = when {
                token.kind == SqlTokenKind.Quoted -> rewriteQuoted(token.text, quotes)
                isKeyword -> upper
                else -> token.text
            }
            val startsClause = isKeyword && upper in clauseStarters && (upper != "BY")
            val startsBy = isKeyword && upper == "BY" && out.toString().trimEnd().uppercase().endsWith("GROUP").not()
                && i > 0 && tokens[i - 1].text.uppercase() in setOf("GROUP", "ORDER", "PARTITION")
            if ((startsClause || startsBy) && out.isNotEmpty()) {
                pendingNewline = true
            }
            if (isKeyword && upper == "SELECT") extraIndent = 1
            if (isKeyword && upper in setOf("FROM", "WHERE", "GROUP", "ORDER", "HAVING", "LIMIT", "OFFSET", "JOIN", "UNION", "INSERT", "UPDATE", "DELETE")) extraIndent = 0
            if (token.text == ")" && depth > 0) depth -= 1
            if (pendingNewline && token.text != ",") {
                out.append('\n').append(tab.repeat(depth + extraIndent))
                pendingNewline = false
            }
            if (token.text == "," && depth == 0) {
                out.append(',')
                pendingNewline = true
                i += 1
                continue
            }
            if (out.isNotEmpty() && !pendingNewline && !out.endsWith('\n') && !out.endsWith(' ') &&
                token.text != "," && token.text != ")" && !out.endsWith('(')
            ) {
                out.append(' ')
            }
            out.append(rendered)
            if (token.text == "(") {
                depth += 1
                pendingNewline = true
            }
            if (isKeyword && upper in setOf("SELECT", "SET", "VALUES", "RETURNING")) {
                pendingNewline = true
            }
            i += 1
        }
        return out.toString().trimEnd()
    }

    fun identifierQuotes(dialect: String): Pair<Char, Char> = when (normalize(dialect)) {
        "mysql", "mariadb" -> '`' to '`'
        "sql server transact-sql", "transact-sql", "tsql" -> '[' to ']'
        else -> '"' to '"'
    }

    fun normalize(dialect: String): String = dialect.trim().lowercase()

    private fun rewriteQuoted(raw: String, quotes: Pair<Char, Char>): String {
        val inner = when {
            raw.length >= 2 && raw.startsWith('`') && raw.endsWith('`') -> raw.substring(1, raw.length - 1).replace("``", "`")
            raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"') -> raw.substring(1, raw.length - 1).replace("\"\"", "\"")
            raw.length >= 2 && raw.startsWith('[') && raw.endsWith(']') -> raw.substring(1, raw.length - 1)
            else -> return raw
        }
        val (open, close) = quotes
        val escaped = when (open) {
            '`' -> inner.replace("`", "``")
            '"' -> inner.replace("\"", "\"\"")
            else -> inner
        }
        return "$open$escaped$close"
    }

    private fun tokenize(input: String): List<SqlToken> {
        val tokens = ArrayList<SqlToken>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isWhitespace() -> i += 1
                ch == '-' && i + 1 < input.length && input[i + 1] == '-' -> {
                    val end = input.indexOf('\n', i).let { if (it < 0) input.length else it }
                    tokens += SqlToken(input.substring(i, end).trimEnd(), SqlTokenKind.Comment)
                    i = end
                }
                ch == '/' && i + 1 < input.length && input[i + 1] == '*' -> {
                    val end = input.indexOf("*/", i + 2).let { if (it < 0) input.length else it + 2 }
                    tokens += SqlToken(input.substring(i, end), SqlTokenKind.Comment)
                    i = end
                }
                ch == '\'' || ch == '"' || ch == '`' -> {
                    val quote = ch
                    val start = i
                    i += 1
                    while (i < input.length) {
                        if (input[i] == quote) {
                            if (i + 1 < input.length && input[i + 1] == quote) {
                                i += 2
                                continue
                            }
                            i += 1
                            break
                        }
                        i += 1
                    }
                    tokens += SqlToken(input.substring(start, i), if (quote == '\'') SqlTokenKind.String else SqlTokenKind.Quoted)
                }
                ch == '[' -> {
                    val end = input.indexOf(']', i + 1).let { if (it < 0) input.length else it + 1 }
                    tokens += SqlToken(input.substring(i, end), SqlTokenKind.Quoted)
                    i = end
                }
                ch.isLetter() || ch == '_' -> {
                    val start = i
                    i += 1
                    while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) i += 1
                    tokens += SqlToken(input.substring(start, i), SqlTokenKind.Word)
                }
                ch.isDigit() -> {
                    val start = i
                    i += 1
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i += 1
                    tokens += SqlToken(input.substring(start, i), SqlTokenKind.Number)
                }
                else -> {
                    tokens += SqlToken(ch.toString(), SqlTokenKind.Symbol)
                    i += 1
                }
            }
        }
        return tokens
    }
}

private data class SqlToken(val text: String, val kind: SqlTokenKind)

private enum class SqlTokenKind { Word, Quoted, String, Number, Symbol, Comment }
