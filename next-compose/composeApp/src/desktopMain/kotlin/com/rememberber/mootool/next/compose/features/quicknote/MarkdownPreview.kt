package com.rememberber.mootool.next.compose.features.quicknote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.MarkdownBlock
import com.rememberber.mootool.next.compose.domain.MarkdownDocument
import com.rememberber.mootool.next.compose.domain.MarkdownImageKind
import com.rememberber.mootool.next.compose.domain.MarkdownInline
import com.rememberber.mootool.next.compose.domain.MarkdownListItem
import com.rememberber.mootool.next.compose.domain.MarkdownPreviewEngine
import com.rememberber.mootool.next.compose.domain.NoteAttachmentEngine
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun MarkdownPreviewPane(markdown: String, vault: NoteVault, missingLabel: String, remoteLabel: String, unsafeLabel: String) {
    var document by remember { mutableStateOf(MarkdownDocument(emptyList())) }
    LaunchedEffect(markdown) {
        document = withContext(Dispatchers.Default) { MarkdownPreviewEngine.parse(markdown) }
    }
    val colors = MooTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().background(colors.workspace).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        document.blocks.forEach { block ->
            MarkdownBlockView(block, vault, missingLabel, remoteLabel, unsafeLabel)
        }
    }
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    vault: NoteVault,
    missingLabel: String,
    remoteLabel: String,
    unsafeLabel: String
) {
    val colors = MooTheme.colors
    when (block) {
        is MarkdownBlock.Heading -> Text(
            MarkdownPreviewEngine.inlineText(block.inlines),
            color = colors.textPrimary,
            fontSize = headingSize(block.level),
            fontWeight = FontWeight.SemiBold
        )
        is MarkdownBlock.Paragraph -> MarkdownInlineRow(block.inlines, vault, missingLabel, remoteLabel, unsafeLabel)
        is MarkdownBlock.Code -> Text(
            block.text.trimEnd().ifBlank { " " },
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle, RoundedCornerShape(6.dp)).padding(10.dp)
        )
        is MarkdownBlock.Quote -> Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(6.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            block.children.forEach { MarkdownBlockView(it, vault, missingLabel, remoteLabel, unsafeLabel) }
        }
        is MarkdownBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEachIndexed { index, item ->
                MarkdownListItemView(block.ordered, block.start + index, item, vault, missingLabel, remoteLabel, unsafeLabel)
            }
        }
        is MarkdownBlock.Table -> Row(Modifier.horizontalScroll(rememberScrollState())) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (block.header.isNotEmpty()) MarkdownTableRow(block.header, true, vault, missingLabel, remoteLabel, unsafeLabel)
                block.rows.forEach { MarkdownTableRow(it, false, vault, missingLabel, remoteLabel, unsafeLabel) }
            }
        }
        is MarkdownBlock.ThematicBreak -> Spacer(
            Modifier.fillMaxWidth().height(1.dp).background(colors.border)
        )
        is MarkdownBlock.Html -> Text(
            block.raw.trim(),
            color = colors.textSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MarkdownListItemView(
    ordered: Boolean,
    number: Int,
    item: MarkdownListItem,
    vault: NoteVault,
    missingLabel: String,
    remoteLabel: String,
    unsafeLabel: String
) {
    val marker = when (item.checked) {
        true -> "[x]"
        false -> "[ ]"
        null -> if (ordered) "$number." else "•"
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(marker, color = MooTheme.colors.textSecondary, fontSize = 13.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item.children.forEach { MarkdownBlockView(it, vault, missingLabel, remoteLabel, unsafeLabel) }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<List<MarkdownInline>>,
    header: Boolean,
    vault: NoteVault,
    missingLabel: String,
    remoteLabel: String,
    unsafeLabel: String
) {
    val colors = MooTheme.colors
    Row(Modifier.background(if (header) colors.surfaceSubtle else colors.workspace)) {
        cells.forEach { cell ->
            Column(
                modifier = Modifier.width(140.dp).border(1.dp, colors.border).padding(6.dp)
            ) {
                MarkdownInlineRow(cell, vault, missingLabel, remoteLabel, unsafeLabel)
            }
        }
    }
}

@Composable
private fun MarkdownInlineRow(
    inlines: List<MarkdownInline>,
    vault: NoteVault,
    missingLabel: String,
    remoteLabel: String,
    unsafeLabel: String
) {
    FlowInlines(inlines, vault, missingLabel, remoteLabel, unsafeLabel)
}

@Composable
private fun FlowInlines(
    inlines: List<MarkdownInline>,
    vault: NoteVault,
    missingLabel: String,
    remoteLabel: String,
    unsafeLabel: String
) {
    val colors = MooTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            inlines.forEach { inline ->
                when (inline) {
                    is MarkdownInline.Image -> MarkdownImageView(inline, vault, missingLabel, remoteLabel, unsafeLabel)
                    is MarkdownInline.Link -> Text(
                        MarkdownPreviewEngine.inlineText(inline.children).ifBlank { inline.destination },
                        color = colors.accent,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(enabled = isHttpLink(inline.destination)) {
                            runCatching { Desktop.getDesktop().browse(URI(inline.destination)) }
                        }
                    )
                    is MarkdownInline.Html -> Text(inline.raw, color = colors.textSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    is MarkdownInline.Code -> Text(
                        inline.value,
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.background(colors.surfaceSubtle, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    is MarkdownInline.Strong -> Text(
                        MarkdownPreviewEngine.inlineText(inline.children),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    is MarkdownInline.Emphasis -> Text(
                        MarkdownPreviewEngine.inlineText(inline.children),
                        color = colors.textPrimary,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                    is MarkdownInline.Strike -> Text(
                        MarkdownPreviewEngine.inlineText(inline.children),
                        color = colors.textPrimary,
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 14.sp
                    )
                    is MarkdownInline.Break -> Spacer(Modifier.width(4.dp))
                    is MarkdownInline.Text -> Text(inline.value, color = colors.textPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun MarkdownImageView(
    image: MarkdownInline.Image,
    vault: NoteVault,
    missingLabel: String,
    remoteLabel: String,
    unsafeLabel: String
) {
    val colors = MooTheme.colors
    val kind = remember(image.destination, vault.root()) { MarkdownPreviewEngine.classifyImage(image.destination, vault) }
    when (kind) {
        MarkdownImageKind.Remote -> Text(
            "$remoteLabel ${image.alt.ifBlank { image.destination }}",
            color = colors.warning,
            fontSize = 12.sp
        )
        MarkdownImageKind.Unsafe -> Text(
            "$unsafeLabel ${image.destination}",
            color = colors.danger,
            fontSize = 12.sp
        )
        MarkdownImageKind.Missing -> Text(
            "$missingLabel ${image.destination}",
            color = colors.danger,
            fontSize = 12.sp
        )
        MarkdownImageKind.Local -> {
            val path = remember(image.destination) { runCatching { vault.resolve(image.destination) }.getOrNull() }
            val bitmap by produceState<ImageBitmap?>(null, path) {
                value = if (path == null) null else withContext(Dispatchers.IO) {
                    runCatching {
                        val bytes = Files.readAllBytes(path)
                        if (bytes.size > NoteAttachmentEngine.MAX_BYTES) null
                        else SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    }.getOrNull()
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = image.alt.ifBlank { image.destination },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                )
            } else {
                Text("$missingLabel ${image.destination}", color = colors.danger, fontSize = 12.sp)
            }
        }
    }
}

private fun headingSize(level: Int) = when (level) {
    1 -> 26.sp
    2 -> 22.sp
    3 -> 18.sp
    4 -> 16.sp
    else -> 14.sp
}

private fun isHttpLink(destination: String): Boolean {
    val lower = destination.trim().lowercase()
    return lower.startsWith("https://") || lower.startsWith("http://")
}
