package com.rememberber.mootool.next.compose.features.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.QrEngine
import com.rememberber.mootool.next.compose.domain.QrErrorCorrection
import com.rememberber.mootool.next.compose.domain.QrException
import com.rememberber.mootool.next.compose.domain.QrTab
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.QrSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image as AwtImage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

@Serializable
private data class QrHistoryMeta(
    val operation: String,
    val size: Int = QrEngine.DEFAULT_SIZE,
    val correction: String = "M"
)

@Composable
fun QrCodeScreen(container: AppContainer, detached: Boolean) {
    val session = remember {
        val tools = container.settings.value.tools
        container.sessionManager.qrSession(tools.qrCodeSize, tools.qrErrorCorrection)
    }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistQr()
    }

    LaunchedEffect(session.tab, session.historyTick, revision) {
        if (session.tab == QrTab.History) historyItems = container.history.list(ToolId.QrCode.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("qrcode.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.QrCode) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QrTab.entries.forEach { tab ->
                MooButton(container.t(tabTitleKey(tab)), primary = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    if (tab == QrTab.History) session.historyTick += 1
                    refresh()
                })
            }
        }
        when (session.tab) {
            QrTab.Generate -> GeneratePanel(container, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            QrTab.Recognize -> RecognizePanel(container, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            QrTab.History -> HistoryPanel(container, session, historyItems, Modifier.weight(1f).fillMaxWidth()) { refresh() }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                session.error.ifEmpty { session.notice },
                color = if (session.error.isNotEmpty()) colors.danger else colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GeneratePanel(
    container: AppContainer,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val preview = remember(session.pngBytes) { session.pngBytes?.toImageBitmap() }
    Row(modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("qrcode.content"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(
                session.content,
                { session.content = it; session.error = ""; onChanged() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                singleLine = false
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(container.t("qrcode.size"), color = colors.textSecondary, fontSize = 12.sp)
                MooTextField(session.size.toString(), { value ->
                    session.size = value.toIntOrNull() ?: session.size
                    onChanged()
                }, modifier = Modifier.width(80.dp))
                Text("px", color = colors.textSecondary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(container.t("qrcode.correction"), color = colors.textSecondary, fontSize = 12.sp)
                QrErrorCorrection.entries.forEach { level ->
                    MooButton(container.t("qrcode.level.${level.name}"), primary = session.correction == level, onClick = {
                        session.correction = level
                        onChanged()
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(container.t("qrcode.logo"), color = colors.textSecondary, fontSize = 12.sp)
                MooButton(session.logoName.ifBlank { container.t("qrcode.chooseLogo") }, onClick = {
                    val file = chooseImage(container.t("qrcode.chooseLogo")) ?: return@MooButton
                    runCatching { QrEngine.readImageFile(file.toPath()) }
                        .onSuccess { image ->
                            session.logoName = file.name
                            session.logoPath = file.absolutePath
                            session.logoImage = image
                            session.error = ""
                            onChanged()
                        }
                        .onFailure { error ->
                            session.error = messageFor(container, error)
                            session.notice = ""
                            onChanged()
                        }
                })
                if (session.logoName.isNotEmpty()) {
                    MooButton(container.t("common.clear"), onClick = {
                        session.logoName = ""
                        session.logoPath = ""
                        session.logoImage = null
                        onChanged()
                    })
                }
            }
            MooButton(
                if (session.busy) container.t("common.processing") else container.t("qrcode.generate"),
                primary = true,
                enabled = !session.busy && session.content.isNotBlank(),
                onClick = {
                    generateQr(container, session, scope, onChanged)
                }
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PreviewBox(preview, container.t("qrcode.preview"), Modifier.weight(1f).fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.save"), enabled = session.pngBytes != null, onClick = {
                    val bytes = session.pngBytes ?: return@MooButton
                    val file = chooseSave(container.t("common.save")) ?: return@MooButton
                    runCatching { file.writeBytes(bytes) }
                        .onSuccess { session.notice = container.t("common.save"); session.error = "" }
                        .onFailure { session.error = it.message ?: container.t("qrcode.error.generic") }
                    onChanged()
                })
                MooButton(container.t("common.action.copy"), enabled = session.pngBytes != null, onClick = {
                    val bytes = session.pngBytes
                    session.notice = if (bytes != null) copyImage(bytes, container) else container.t("qrcode.nothingToCopy")
                    onChanged()
                })
            }
        }
    }
}

@Composable
private fun RecognizePanel(
    container: AppContainer,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val sourcePreview = remember(session.recognitionBytes) { session.recognitionBytes?.toImageBitmap() }
    Row(modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("qrcode.chooseImage"), onClick = {
                    val file = chooseImage(container.t("qrcode.chooseImage")) ?: return@MooButton
                    session.recognitionName = file.name
                    session.recognitionBytes = file.readBytes()
                    recognizeQr(container, session, scope, onChanged)
                })
                MooButton(container.t("qrcode.fromClipboard"), onClick = {
                    val image = readClipboardImage()
                    if (image == null) {
                        session.error = container.t("qrcode.clipboardEmpty")
                        session.notice = ""
                        onChanged()
                        return@MooButton
                    }
                    session.recognitionName = container.t("qrcode.clipboard")
                    session.recognitionBytes = QrEngine.toPng(QrEngine.flattenOnWhite(image))
                    recognizeQr(container, session, scope, onChanged)
                })
                MooButton(
                    if (session.busy) container.t("common.processing") else container.t("qrcode.recognize"),
                    primary = true,
                    enabled = session.recognitionBytes != null && !session.busy,
                    onClick = { recognizeQr(container, session, scope, onChanged) }
                )
            }
            PreviewBox(sourcePreview, session.recognitionName.ifBlank { container.t("qrcode.chooseImage") }, Modifier.weight(1f).fillMaxWidth())
            if (session.recognitionName.isNotEmpty()) {
                Text(session.recognitionName, color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("qrcode.result"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(
                session.recognitionResult,
                { session.recognitionResult = it; onChanged() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                singleLine = false
            )
            MooButton(container.t("common.action.copy"), enabled = session.recognitionResult.isNotEmpty(), onClick = {
                session.notice = copyText(session.recognitionResult, container)
                onChanged()
            })
        }
    }
}

@Composable
private fun HistoryPanel(
    container: AppContainer,
    session: QrSession,
    items: List<HistoryRecord>,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (items.isEmpty()) {
            Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(items) { item ->
                    Column(Modifier.fillMaxWidth().clickable {
                        applyHistory(session, item)
                        session.notice = container.t("json.notice.restored")
                        onChanged()
                    }.padding(8.dp)) {
                        Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                        Text(item.input, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            MooButton(container.t("common.clear"), onClick = {
                container.history.clear(ToolId.QrCode.id)
                session.historyTick += 1
                onChanged()
            })
        }
    }
}

@Composable
private fun PreviewBox(bitmap: ImageBitmap?, placeholder: String, modifier: Modifier) {
    val colors = MooTheme.colors
    Box(
        modifier.border(1.dp, colors.border, RoundedCornerShape(8.dp)).background(colors.surfaceSubtle, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, placeholder, modifier = Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
        } else {
            Text(placeholder, color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}

private fun tabTitleKey(tab: QrTab): String = when (tab) {
    QrTab.Generate -> "qrcode.tab.generate"
    QrTab.Recognize -> "qrcode.tab.recognize"
    QrTab.History -> "qrcode.tab.history"
}

private val historyCodec = Json { ignoreUnknownKeys = true }

private fun generateQr(
    container: AppContainer,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit
) {
    session.busy = true
    session.error = ""
    session.notice = container.t("common.processing")
    onChanged()
    val content = session.content
    val size = QrEngine.normalizeSize(session.size)
    val correction = session.correction
    val logo = session.logoImage
    scope.launch(Dispatchers.Default) {
        val result = runCatching { QrEngine.generatePng(content, size, correction, logo) }
        withContext(Dispatchers.Swing) {
            session.busy = false
            result.onSuccess { bytes ->
                session.size = size
                session.pngBytes = bytes
                session.notice = container.t("qrcode.generated")
                session.error = ""
                container.updateSettings { current ->
                    current.copy(tools = current.tools.copy(qrCodeSize = size, qrErrorCorrection = correction.name))
                }
                val meta = QrHistoryMeta("generate", size, correction.name)
                container.history.save(
                    ToolId.QrCode.id,
                    container.t("qrcode.history.generate"),
                    container.t("qrcode.history.generate"),
                    content,
                    "${size}x${size} PNG",
                    historyCodec.encodeToString(meta)
                )
            }.onFailure { error ->
                session.notice = ""
                session.error = messageFor(container, error)
            }
            onChanged()
        }
    }
}

private fun recognizeQr(
    container: AppContainer,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit
) {
    val bytes = session.recognitionBytes ?: return
    session.busy = true
    session.error = ""
    session.notice = container.t("common.processing")
    onChanged()
    val name = session.recognitionName
    scope.launch(Dispatchers.Default) {
        val result = runCatching { QrEngine.decodePng(bytes) }
        withContext(Dispatchers.Swing) {
            session.busy = false
            result.onSuccess { text ->
                session.recognitionResult = text
                session.notice = container.t("qrcode.recognized")
                session.error = ""
                val meta = QrHistoryMeta("recognize")
                container.history.save(
                    ToolId.QrCode.id,
                    container.t("qrcode.history.recognize"),
                    container.t("qrcode.history.recognize"),
                    name,
                    text,
                    historyCodec.encodeToString(meta)
                )
            }.onFailure { error ->
                session.notice = ""
                session.error = messageFor(container, error)
            }
            onChanged()
        }
    }
}

private fun applyHistory(session: QrSession, item: HistoryRecord) {
    val meta = runCatching { historyCodec.decodeFromString<QrHistoryMeta>(item.options) }.getOrNull()
    if (meta?.operation == "recognize") {
        session.tab = QrTab.Recognize
        session.recognitionName = item.input
        session.recognitionResult = item.output
    } else {
        session.tab = QrTab.Generate
        session.content = item.input
        session.size = meta?.size ?: session.size
        session.correction = QrErrorCorrection.entries.find { it.name == meta?.correction } ?: session.correction
        session.pngBytes = null
    }
    session.error = ""
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? QrException)?.code
    return when (code) {
        "empty" -> container.t("qrcode.error.empty")
        "capacity" -> container.t("qrcode.error.capacity")
        "invalid-image" -> container.t("qrcode.error.image")
        "not-found" -> container.t("qrcode.error.notFound")
        "too-large" -> container.t("qrcode.error.large")
        else -> error.message ?: container.t("qrcode.error.generic")
    }
}

private fun ByteArray.toImageBitmap(): ImageBitmap =
    Image.makeFromEncoded(this).toComposeImageBitmap()

private fun copyText(value: String, container: AppContainer): String {
    if (value.isEmpty()) return container.t("qrcode.nothingToCopy")
    return runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("json.notice.copied")
    }.getOrElse { container.t("qrcode.error.generic") }
}

private fun copyImage(png: ByteArray, container: AppContainer): String = runCatching {
    val image = ImageIO.read(ByteArrayInputStream(png)) ?: return container.t("qrcode.error.image")
    Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageSelection(image), null)
    container.t("json.notice.copied")
}.getOrElse { container.t("qrcode.error.generic") }

private fun readClipboardImage(): BufferedImage? = runCatching {
    val contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null) ?: return null
    if (!contents.isDataFlavorSupported(DataFlavor.imageFlavor)) return null
    when (val data = contents.getTransferData(DataFlavor.imageFlavor)) {
        is BufferedImage -> data
        is AwtImage -> {
            val buffered = BufferedImage(data.getWidth(null), data.getHeight(null), BufferedImage.TYPE_INT_ARGB)
            val graphics = buffered.createGraphics()
            graphics.drawImage(data, 0, 0, null)
            graphics.dispose()
            buffered
        }
        else -> null
    }
}.getOrNull()

private class ImageSelection(private val image: BufferedImage) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
    override fun getTransferData(flavor: DataFlavor): Any = image
}

private fun chooseImage(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}

private fun chooseSave(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.file = "mootool-qrcode.png"
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    val chosen = File(directory, file)
    return if (chosen.extension.isBlank()) File(chosen.path + ".png") else chosen
}
