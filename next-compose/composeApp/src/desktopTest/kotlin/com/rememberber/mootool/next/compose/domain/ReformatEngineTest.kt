package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReformatEngineTest {
    @Test
    fun formatsNginxBlocksAndStatementsLikeElectron() {
        assertEquals(
            "server {\n  listen 80;\n  location / {\n    proxy_pass http://app;\n  }\n}",
            ReformatEngine.formatNginx("server { listen 80; location / { proxy_pass http://app; } }", 2)
        )
    }

    @Test
    fun keepsNginxQuotesCommentsEscapesAndUnicode() {
        val input = """
            server {
            listen 80; # 端口
            set ${'$'}msg "brace { inside }";
            location /moo {
            add_header X-Name "Moo工具 😺";
            return 200 'ok\'d';
            }
            }
        """.trimIndent()
        val once = ReformatEngine.formatNginx(input, 2)
        assertTrue(once.contains("listen 80;"))
        assertTrue(once.contains("# 端口"))
        assertTrue(once.contains("\"brace { inside }\""))
        assertTrue(once.contains("Moo工具 😺"))
        assertTrue(once.contains("ok\\'d"))
        assertEquals(once, ReformatEngine.formatNginx(once, 2))
    }

    @Test
    fun formatsJavaWithRealParserAndHonorsIndent() {
        val output = ReformatEngine.format(
            "class Demo{public static void main(String[] args){System.out.println(\"moo\");}}",
            ReformatType.Java,
            2
        )
        assertTrue(output.contains("class Demo"))
        assertTrue(output.contains("System.out.println(\"moo\");"))
        assertTrue(output.contains("\n  "))
        assertEquals(output, ReformatEngine.format(output, ReformatType.Java, 2))
    }

    @Test
    fun formatsXmlAndHtmlAndKeepsTextNodes() {
        val xml = ReformatEngine.format("<root><item id=\"1\">moo 文本</item></root>", ReformatType.Xml, 2)
        assertTrue(xml.contains("\n  <item"))
        assertTrue(xml.contains(">moo 文本</item>"))
        assertEquals(xml, ReformatEngine.format(xml, ReformatType.Xml, 2))

        val mixed = ReformatEngine.format("<p>hello <b>世界</b>!</p>", ReformatType.Xml, 2)
        assertTrue(mixed.contains("hello <b>世界</b>!"))

        val html = ReformatEngine.format("<main><strong>Moo</strong></main>", ReformatType.Html, 2)
        assertTrue(html.contains("<strong>Moo</strong>"))
        assertEquals(html, ReformatEngine.format(html, ReformatType.Html, 2))
    }

    @Test
    fun keepsOriginalOnJavaAndXmlSyntaxErrorsAndReportsLocation() {
        val javaError = assertFailsWith<ReformatException> {
            ReformatEngine.format("class Demo { void broken(", ReformatType.Java, 4)
        }
        assertEquals("syntax", javaError.code)
        assertTrue(javaError.line >= 1)

        val xmlError = assertFailsWith<ReformatException> {
            ReformatEngine.format("<root><item></root>", ReformatType.Xml, 2)
        }
        assertEquals("syntax", xmlError.code)
        assertTrue(xmlError.line >= 1)
    }

    @Test
    fun returnsEmptyForBlankInput() {
        assertEquals("", ReformatEngine.format("   \n  ", ReformatType.Nginx, 4))
        assertEquals("", ReformatEngine.format("", ReformatType.Java, 2))
    }
}
