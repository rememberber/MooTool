package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.NoteVault
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock as GfmTableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import java.nio.file.Files
import java.util.Locale

data class MarkdownDocument(val blocks: List<MarkdownBlock>)

data class MarkdownListItem(
    val checked: Boolean?,
    val children: List<MarkdownBlock>
)

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val inlines: List<MarkdownInline>) : MarkdownBlock
    data class Paragraph(val inlines: List<MarkdownInline>) : MarkdownBlock
    data class Code(val language: String, val text: String) : MarkdownBlock
    data class Quote(val children: List<MarkdownBlock>) : MarkdownBlock
    data class ListBlock(val ordered: Boolean, val start: Int, val items: List<MarkdownListItem>) : MarkdownBlock
    data class Table(val header: List<List<MarkdownInline>>, val rows: List<List<List<MarkdownInline>>>) : MarkdownBlock
    data class ThematicBreak(val unused: Unit = Unit) : MarkdownBlock
    data class Html(val raw: String) : MarkdownBlock
}

sealed interface MarkdownInline {
    data class Text(val value: String) : MarkdownInline
    data class Code(val value: String) : MarkdownInline
    data class Strong(val children: List<MarkdownInline>) : MarkdownInline
    data class Emphasis(val children: List<MarkdownInline>) : MarkdownInline
    data class Strike(val children: List<MarkdownInline>) : MarkdownInline
    data class Link(val destination: String, val children: List<MarkdownInline>) : MarkdownInline
    data class Image(val destination: String, val alt: String) : MarkdownInline
    data class Break(val hard: Boolean) : MarkdownInline
    data class Html(val raw: String) : MarkdownInline
}

enum class MarkdownImageKind { Local, Missing, Remote, Unsafe }

object MarkdownPreviewEngine {
    private val extensions = listOf(
        TablesExtension.create(),
        TaskListItemsExtension.create(),
        StrikethroughExtension.create()
    )
    private val parser: Parser = Parser.builder().extensions(extensions).build()

    fun parse(markdown: String): MarkdownDocument = MarkdownDocument(convertBlocks(parser.parse(markdown)))

    fun dump(document: MarkdownDocument): String = document.blocks.joinToString("\n") { dumpBlock(it, 0) }

    fun inlineText(inlines: List<MarkdownInline>): String = inlines.joinToString("") { inline ->
        when (inline) {
            is MarkdownInline.Text -> inline.value
            is MarkdownInline.Code -> inline.value
            is MarkdownInline.Strong -> inlineText(inline.children)
            is MarkdownInline.Emphasis -> inlineText(inline.children)
            is MarkdownInline.Strike -> inlineText(inline.children)
            is MarkdownInline.Link -> inlineText(inline.children)
            is MarkdownInline.Image -> inline.alt.ifBlank { inline.destination }
            is MarkdownInline.Break -> if (inline.hard) "\n" else " "
            is MarkdownInline.Html -> inline.raw
        }
    }

    fun classifyImage(destination: String, vault: NoteVault): MarkdownImageKind {
        val dest = destination.trim()
        if (dest.isEmpty()) return MarkdownImageKind.Missing
        val lower = dest.lowercase(Locale.ROOT)
        if (lower.startsWith("javascript:") || lower.startsWith("vbscript:") ||
            lower.startsWith("data:") || lower.startsWith("file:")
        ) {
            return MarkdownImageKind.Unsafe
        }
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")) {
            return MarkdownImageKind.Remote
        }
        if (dest.contains("..") || dest.contains('\u0000') || dest.startsWith("/") || dest.startsWith("\\") ||
            (dest.length >= 2 && dest[1] == ':')
        ) {
            return MarkdownImageKind.Unsafe
        }
        return runCatching {
            val path = vault.resolve(dest.replace('\\', '/'))
            if (Files.isRegularFile(path)) MarkdownImageKind.Local else MarkdownImageKind.Missing
        }.getOrElse { MarkdownImageKind.Unsafe }
    }

    private fun convertBlocks(node: Node): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        var child = node.firstChild
        while (child != null) {
            convertBlock(child)?.let(blocks::add)
            child = child.next
        }
        return blocks
    }

    private fun convertBlock(node: Node): MarkdownBlock? = when (node) {
        is Heading -> MarkdownBlock.Heading(node.level, convertInlines(node))
        is Paragraph -> MarkdownBlock.Paragraph(convertInlines(node))
        is FencedCodeBlock -> MarkdownBlock.Code(node.info.orEmpty().trim(), node.literal.orEmpty())
        is IndentedCodeBlock -> MarkdownBlock.Code("", node.literal.orEmpty())
        is BlockQuote -> MarkdownBlock.Quote(convertBlocks(node))
        is BulletList -> MarkdownBlock.ListBlock(false, 1, convertListItems(node))
        is OrderedList -> MarkdownBlock.ListBlock(true, node.markerStartNumber, convertListItems(node))
        is GfmTableBlock -> convertTable(node)
        is ThematicBreak -> MarkdownBlock.ThematicBreak()
        is HtmlBlock -> MarkdownBlock.Html(node.literal.orEmpty())
        else -> null
    }

    private fun convertListItems(node: Node): List<MarkdownListItem> {
        val items = mutableListOf<MarkdownListItem>()
        var child = node.firstChild
        while (child != null) {
            if (child is ListItem) {
                items += MarkdownListItem(taskChecked(child), convertBlocks(child))
            }
            child = child.next
        }
        return items
    }

    private fun taskChecked(node: Node): Boolean? {
        var child = node.firstChild
        while (child != null) {
            if (child is TaskListItemMarker) return child.isChecked
            taskChecked(child)?.let { return it }
            child = child.next
        }
        return null
    }

    private fun convertTable(node: GfmTableBlock): MarkdownBlock.Table {
        var header = emptyList<List<MarkdownInline>>()
        val rows = mutableListOf<List<List<MarkdownInline>>>()
        var child = node.firstChild
        while (child != null) {
            when (child) {
                is TableHead -> header = tableRows(child).firstOrNull().orEmpty()
                is TableBody -> rows += tableRows(child)
            }
            child = child.next
        }
        return MarkdownBlock.Table(header, rows)
    }

    private fun tableRows(section: Node): List<List<List<MarkdownInline>>> {
        val rows = mutableListOf<List<List<MarkdownInline>>>()
        var rowNode = section.firstChild
        while (rowNode != null) {
            if (rowNode is TableRow) {
                val cells = mutableListOf<List<MarkdownInline>>()
                var cell = rowNode.firstChild
                while (cell != null) {
                    if (cell is TableCell) cells += convertInlines(cell)
                    cell = cell.next
                }
                rows += cells
            }
            rowNode = rowNode.next
        }
        return rows
    }

    private fun convertInlines(node: Node): List<MarkdownInline> {
        val inlines = mutableListOf<MarkdownInline>()
        var child = node.firstChild
        while (child != null) {
            when (child) {
                is Text -> inlines += MarkdownInline.Text(child.literal.orEmpty())
                is Code -> inlines += MarkdownInline.Code(child.literal.orEmpty())
                is Emphasis -> inlines += MarkdownInline.Emphasis(convertInlines(child))
                is StrongEmphasis -> inlines += MarkdownInline.Strong(convertInlines(child))
                is Strikethrough -> inlines += MarkdownInline.Strike(convertInlines(child))
                is Link -> inlines += MarkdownInline.Link(child.destination.orEmpty(), convertInlines(child))
                is Image -> inlines += MarkdownInline.Image(child.destination.orEmpty(), inlineText(convertInlines(child)))
                is SoftLineBreak -> inlines += MarkdownInline.Break(false)
                is HardLineBreak -> inlines += MarkdownInline.Break(true)
                is HtmlInline -> inlines += MarkdownInline.Html(child.literal.orEmpty())
                is TaskListItemMarker -> Unit
                else -> inlines += convertInlines(child)
            }
            child = child.next
        }
        return inlines
    }

    private fun dumpBlock(block: MarkdownBlock, indent: Int): String {
        val pad = "  ".repeat(indent)
        return when (block) {
            is MarkdownBlock.Heading -> "${pad}H${block.level} ${inlineText(block.inlines)}"
            is MarkdownBlock.Paragraph -> "${pad}P ${dumpInlines(block.inlines)}"
            is MarkdownBlock.Code -> "${pad}CODE ${block.language}\n${block.text.trimEnd().prependIndent("$pad  ")}"
            is MarkdownBlock.Quote -> buildString {
                append("${pad}QUOTE")
                block.children.forEach { append('\n').append(dumpBlock(it, indent + 1)) }
            }
            is MarkdownBlock.ListBlock -> buildString {
                append(pad).append(if (block.ordered) "OL" else "UL")
                block.items.forEach { item ->
                    val marker = when (item.checked) {
                        true -> "[x] "
                        false -> "[ ] "
                        null -> ""
                    }
                    append('\n').append(pad).append("LI ").append(marker).append(item.children.joinToString(" / ") {
                        dumpBlock(it, 0).trim()
                    })
                }
            }
            is MarkdownBlock.Table -> {
                val header = block.header.joinToString("|") { inlineText(it) }
                val body = block.rows.joinToString(" / ") { row -> row.joinToString("|") { inlineText(it) } }
                "${pad}TABLE $header" + if (body.isBlank()) "" else " / $body"
            }
            is MarkdownBlock.ThematicBreak -> "${pad}HR"
            is MarkdownBlock.Html -> "${pad}HTML ${block.raw.replace('\n', ' ').trim()}"
        }
    }

    private fun dumpInlines(inlines: List<MarkdownInline>): String = inlines.joinToString("") { inline ->
        when (inline) {
            is MarkdownInline.Image -> "[IMG ${inline.destination}]"
            is MarkdownInline.Link -> "[LINK ${inline.destination} ${inlineText(inline.children)}]"
            is MarkdownInline.Html -> "[HTML ${inline.raw}]"
            is MarkdownInline.Code -> "`${inline.value}`"
            is MarkdownInline.Break -> if (inline.hard) "\n" else " "
            else -> inlineText(listOf(inline))
        }
    }
}
