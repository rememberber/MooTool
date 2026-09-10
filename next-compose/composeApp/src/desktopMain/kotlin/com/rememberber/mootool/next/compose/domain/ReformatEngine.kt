package com.rememberber.mootool.next.compose.domain

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.configuration.DefaultConfigurationOption
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption
import com.github.javaparser.printer.configuration.Indentation
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import org.w3c.dom.Document as XmlDocument
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node as XmlNode
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

enum class ReformatType { Nginx, Java, Xml, Html }

class ReformatException(
    val code: String,
    message: String,
    val line: Int = -1,
    val column: Int = -1
) : RuntimeException(message)

object ReformatEngine {
    val types: List<ReformatType> = ReformatType.entries

    val samples: Map<ReformatType, String> = mapOf(
        ReformatType.Nginx to "server { listen 80; location / { proxy_pass http://127.0.0.1:3000; } }",
        ReformatType.Java to "class Demo{public static void main(String[] args){System.out.println(\"MooTool\");}}",
        ReformatType.Xml to "<root><tool id=\"mootool\"><name>MooTool</name></tool></root>",
        ReformatType.Html to "<main><h1>MooTool</h1><p>Desktop toolbox</p></main>"
    )

    fun format(input: String, type: ReformatType, indent: Int = 4): String {
        if (input.trim().isEmpty()) return ""
        val tabWidth = indent.coerceIn(1, 8)
        return when (type) {
            ReformatType.Nginx -> formatNginx(input, tabWidth)
            ReformatType.Java -> formatJava(input, tabWidth)
            ReformatType.Xml -> formatXml(input, tabWidth)
            ReformatType.Html -> formatHtml(input, tabWidth)
        }
    }

    fun formatNginx(input: String, indent: Int = 4): String {
        val tabWidth = indent.coerceIn(1, 8)
        val tokens = tokenizeNginx(input)
        val lines = ArrayList<String>()
        var level = 0
        for (token in tokens) {
            if (token == "}") level = maxOf(0, level - 1)
            if (token.isNotEmpty()) lines += "${" ".repeat(level * tabWidth)}$token"
            if (token.endsWith("{")) level += 1
        }
        return lines.joinToString("\n")
    }

    private fun tokenizeNginx(input: String): List<String> {
        val tokens = ArrayList<String>()
        val current = StringBuilder()
        var quote = '\u0000'
        var escaped = false
        var comment = false

        fun flush() {
            val value = current.toString().trim()
            if (value.isNotEmpty()) tokens += value
            current.setLength(0)
        }

        for (index in input.indices) {
            val char = input[index]
            if (comment) {
                current.append(char)
                if (char == '\n') {
                    flush()
                    comment = false
                }
                continue
            }
            if (escaped) {
                current.append(char)
                escaped = false
                continue
            }
            if (char == '\\') {
                current.append(char)
                escaped = true
                continue
            }
            if (quote != '\u0000') {
                current.append(char)
                if (char == quote) quote = '\u0000'
                continue
            }
            if (char == '"' || char == '\'') {
                quote = char
                current.append(char)
                continue
            }
            if (char == '#') {
                comment = true
                current.append(char)
                continue
            }
            when (char) {
                '{' -> {
                    val trimmed = current.toString().trimEnd()
                    current.setLength(0)
                    current.append(trimmed).append(" {")
                    flush()
                }
                '}' -> {
                    flush()
                    tokens += "}"
                }
                ';' -> {
                    val trimmed = current.toString().trimEnd()
                    current.setLength(0)
                    current.append(trimmed).append(';')
                    flush()
                }
                '\n', '\r' -> flush()
                else -> current.append(char)
            }
        }
        flush()
        return tokens
    }

    private fun formatJava(input: String, indent: Int): String {
        val configuration = DefaultPrinterConfiguration()
            .addOption(DefaultConfigurationOption(ConfigOption.INDENTATION, Indentation(Indentation.IndentType.SPACES, indent)))
            .addOption(DefaultConfigurationOption(ConfigOption.END_OF_LINE_CHARACTER, "\n"))
        val printer = DefaultPrettyPrinter(configuration)
        val unit = try {
            StaticJavaParser.parse(input)
        } catch (error: ParseProblemException) {
            val problem = error.problems.firstOrNull()
            val begin = problem?.location?.orElse(null)?.begin?.range?.orElse(null)?.begin
            throw ReformatException(
                "syntax",
                problem?.verboseMessage ?: error.message ?: "Java syntax error",
                line = begin?.line ?: -1,
                column = begin?.column ?: -1
            )
        } catch (error: RuntimeException) {
            throw ReformatException("syntax", error.message ?: "Java syntax error")
        }
        return printer.print(unit).trimEnd()
    }

    private fun formatXml(input: String, indent: Int): String {
        val document = try {
            secureDocumentBuilder().parse(InputSource(StringReader(input)))
        } catch (error: SAXParseException) {
            throw ReformatException("syntax", error.message ?: "XML syntax error", error.lineNumber, error.columnNumber)
        } catch (error: Exception) {
            val cause = error.cause as? SAXParseException
            if (cause != null) {
                throw ReformatException("syntax", cause.message ?: "XML syntax error", cause.lineNumber, cause.columnNumber)
            }
            throw ReformatException("syntax", error.message ?: "XML syntax error")
        }
        val out = StringBuilder()
        if (input.trimStart().startsWith("<?xml")) {
            val version = document.xmlVersion ?: "1.0"
            val encoding = document.xmlEncoding ?: "UTF-8"
            out.append("<?xml version=\"").append(version).append("\" encoding=\"").append(encoding).append("\"?>\n")
        }
        writeXml(document, indent, 0, out)
        return out.toString().trimEnd()
    }

    private fun writeXml(node: XmlNode, indent: Int, level: Int, out: StringBuilder) {
        when (node.nodeType) {
            XmlNode.DOCUMENT_NODE -> {
                val document = node as XmlDocument
                val children = document.childNodes
                var first = true
                for (index in 0 until children.length) {
                    val child = children.item(index)
                    if (child.nodeType == XmlNode.TEXT_NODE && child.textContent.isNullOrBlank()) continue
                    if (!first && !out.endsWith('\n')) out.append('\n')
                    writeXml(child, indent, 0, out)
                    first = false
                }
            }
            XmlNode.ELEMENT_NODE -> {
                val pad = " ".repeat(level * indent)
                out.append(pad).append('<').append(node.nodeName)
                appendXmlAttributes(node.attributes, out)
                if (!node.hasChildNodes()) {
                    out.append("/>")
                    return
                }
                val kids = node.childNodes
                val hasElement = hasXmlElement(kids)
                val hasText = hasXmlNonWhitespaceText(kids)
                out.append('>')
                if (!hasElement) {
                    for (index in 0 until kids.length) writeXmlInline(kids.item(index), out)
                    out.append("</").append(node.nodeName).append('>')
                } else if (hasText) {
                    for (index in 0 until kids.length) writeXmlInline(kids.item(index), out)
                    out.append("</").append(node.nodeName).append('>')
                } else {
                    out.append('\n')
                    for (index in 0 until kids.length) {
                        val child = kids.item(index)
                        if (child.nodeType == XmlNode.TEXT_NODE && child.textContent.isNullOrBlank()) continue
                        writeXml(child, indent, level + 1, out)
                        out.append('\n')
                    }
                    out.append(pad).append("</").append(node.nodeName).append('>')
                }
            }
            XmlNode.COMMENT_NODE -> out.append(" ".repeat(level * indent)).append("<!--").append(node.textContent).append("-->")
            XmlNode.CDATA_SECTION_NODE -> out.append(" ".repeat(level * indent)).append("<![CDATA[").append(node.nodeValue).append("]]>")
            XmlNode.PROCESSING_INSTRUCTION_NODE ->
                out.append(" ".repeat(level * indent)).append("<?").append(node.nodeName).append(' ').append(node.nodeValue).append("?>")
            XmlNode.TEXT_NODE -> {
                val text = node.textContent.orEmpty()
                if (text.isNotBlank()) out.append(" ".repeat(level * indent)).append(escapeXml(text.trim(), attribute = false))
            }
        }
    }

    private fun writeXmlInline(node: XmlNode, out: StringBuilder) {
        when (node.nodeType) {
            XmlNode.ELEMENT_NODE -> {
                out.append('<').append(node.nodeName)
                appendXmlAttributes(node.attributes, out)
                if (!node.hasChildNodes()) {
                    out.append("/>")
                    return
                }
                out.append('>')
                val kids = node.childNodes
                for (index in 0 until kids.length) writeXmlInline(kids.item(index), out)
                out.append("</").append(node.nodeName).append('>')
            }
            XmlNode.TEXT_NODE -> out.append(escapeXml(node.textContent.orEmpty(), attribute = false))
            XmlNode.CDATA_SECTION_NODE -> out.append("<![CDATA[").append(node.nodeValue).append("]]>")
            XmlNode.COMMENT_NODE -> out.append("<!--").append(node.textContent).append("-->")
        }
    }

    private fun appendXmlAttributes(attributes: NamedNodeMap?, out: StringBuilder) {
        if (attributes == null) return
        for (index in 0 until attributes.length) {
            val attr = attributes.item(index)
            out.append(' ').append(attr.nodeName).append("=\"").append(escapeXml(attr.nodeValue.orEmpty(), attribute = true)).append('"')
        }
    }

    private fun hasXmlElement(kids: org.w3c.dom.NodeList): Boolean {
        for (index in 0 until kids.length) {
            if (kids.item(index).nodeType == XmlNode.ELEMENT_NODE) return true
        }
        return false
    }

    private fun hasXmlNonWhitespaceText(kids: org.w3c.dom.NodeList): Boolean {
        for (index in 0 until kids.length) {
            val child = kids.item(index)
            if (child.nodeType == XmlNode.TEXT_NODE && child.textContent.orEmpty().isNotBlank()) return true
            if (child.nodeType == XmlNode.CDATA_SECTION_NODE) return true
        }
        return false
    }

    private fun escapeXml(value: String, attribute: Boolean): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> if (attribute) append("&quot;") else append(ch)
                '\'' -> if (attribute) append("&apos;") else append(ch)
                else -> append(ch)
            }
        }
    }

    private fun secureDocumentBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isExpandEntityReferences = false
        factory.isXIncludeAware = false
        runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
        runCatching { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }
        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(object : ErrorHandler {
            override fun warning(exception: SAXParseException) = Unit
            override fun error(exception: SAXParseException) = throw exception
            override fun fatalError(exception: SAXParseException) = throw exception
        })
        return builder
    }

    private fun formatHtml(input: String, indent: Int): String {
        val fragment = Jsoup.parse(input, "", Parser.htmlParser())
        fragment.outputSettings()
            .prettyPrint(false)
            .syntax(Document.OutputSettings.Syntax.html)
        val body = fragment.body()
        val out = StringBuilder()
        val children = body.childNodes()
        var first = true
        for (child in children) {
            if (child is TextNode && child.text().isBlank()) continue
            if (!first) out.append('\n')
            writeHtml(child, indent, 0, out)
            first = false
        }
        return out.toString().trimEnd()
    }

    private fun writeHtml(node: Node, indent: Int, level: Int, out: StringBuilder) {
        when (node) {
            is TextNode -> {
                val text = node.wholeText
                if (text.isNotBlank()) out.append(" ".repeat(level * indent)).append(text.trim())
            }
            is Comment -> out.append(" ".repeat(level * indent)).append("<!--").append(node.data).append("-->")
            is DataNode -> out.append(node.wholeData)
            is Element -> {
                val pad = " ".repeat(level * indent)
                val tag = node.normalName()
                out.append(pad).append('<').append(node.tagName())
                for (attr in node.attributes()) {
                    out.append(' ').append(attr.key)
                    if (attr.value.isNotEmpty()) {
                        out.append("=\"").append(attr.value.replace("\"", "&quot;")).append('"')
                    }
                }
                if (node.tag().isEmpty || node.tag().isSelfClosing) {
                    out.append('>')
                    return
                }
                val kids = node.childNodes()
                val hasElement = kids.any { it is Element }
                val hasText = kids.any { it is TextNode && it.wholeText.isNotBlank() }
                out.append('>')
                if (kids.isEmpty()) {
                    out.append("</").append(node.tagName()).append('>')
                    return
                }
                if (!hasElement || hasText || tag == "script" || tag == "style") {
                    for (child in kids) writeHtmlInline(child, out)
                    out.append("</").append(node.tagName()).append('>')
                } else {
                    out.append('\n')
                    for (child in kids) {
                        if (child is TextNode && child.wholeText.isBlank()) continue
                        writeHtml(child, indent, level + 1, out)
                        out.append('\n')
                    }
                    out.append(pad).append("</").append(node.tagName()).append('>')
                }
            }
        }
    }

    private fun writeHtmlInline(node: Node, out: StringBuilder) {
        when (node) {
            is TextNode -> out.append(node.wholeText)
            is Comment -> out.append("<!--").append(node.data).append("-->")
            is DataNode -> out.append(node.wholeData)
            is Element -> {
                out.append('<').append(node.tagName())
                for (attr in node.attributes()) {
                    out.append(' ').append(attr.key)
                    if (attr.value.isNotEmpty()) {
                        out.append("=\"").append(attr.value.replace("\"", "&quot;")).append('"')
                    }
                }
                if (node.tag().isEmpty || node.tag().isSelfClosing) {
                    out.append('>')
                    return
                }
                out.append('>')
                for (child in node.childNodes()) writeHtmlInline(child, out)
                out.append("</").append(node.tagName()).append('>')
            }
        }
    }
}
