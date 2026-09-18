package com.rememberber.mootool.next.compose.features.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.QrEngine
import com.rememberber.mootool.next.compose.domain.QrHistoryMetadata
import com.rememberber.mootool.next.compose.domain.QrHistoryRestore
import com.rememberber.mootool.next.compose.domain.QrErrorCorrection
import com.rememberber.mootool.next.compose.domain.QrException
import com.rememberber.mootool.next.compose.domain.QrWiringPresentation
import com.rememberber.mootool.next.compose.ui.components.mooQrGenerateButton
import com.rememberber.mootool.next.compose.ui.components.mooQrOptionsRow
import com.rememberber.mootool.next.compose.ui.components.mooQrPreviewActions
import com.rememberber.mootool.next.compose.domain.QrTab
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.IoTwoPaneRow
import com.rememberber.mootool.next.compose.sessions.QrSession
import com.rememberber.mootool.next.compose.ui.components.FileDropRow
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.chooseFileWithExportDirectory
import com.rememberber.mootool.next.compose.ui.persistToolsExportDirectory
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.Image as AwtImage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

private fun pngToDataUrl(bytes: ByteArray): String =
    "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes)

@Composable
fun QrCodeScreen(container: AppContainer, detached: Boolean) {
    val session = remember {
        val tools = container.settings.value.tools
        container.sessionManager.qrSession(tools.qrCodeSize, tools.qrErrorCorrection)
    }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistQr()
    }

    LaunchedEffect(settings.tools.qrCodeSize, settings.tools.qrErrorCorrection) {
        val defaults = QrWiringPresentation.fromSettings(settings.tools.qrCodeSize, settings.tools.qrErrorCorrection)
        var changed = false
        if (session.size != defaults.size) {
            session.size = defaults.size
            changed = true
        }
        if (session.correction != defaults.correction) {
            session.correction = defaults.correction
            changed = true
        }
        if (changed) refresh()
    }

    LaunchedEffect(session.tab, session.historyTick, revision) {
        if (session.tab == QrTab.History) historyItems = container.history.list(ToolId.QrCode.id)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("qrcode.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.QrCode) })
                }
            )
        }
        MooToolTabsRow {
            QrTab.entries.forEach { tab ->
                MooToolTab(container.t(tabTitleKey(tab)), selected = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    if (tab == QrTab.History) session.historyTick += 1
                    refresh()
                })
            }
        }
        when (session.tab) {
            QrTab.Generate -> GeneratePanel(container, settings, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            QrTab.Recognize -> RecognizePanel(container, settings, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            QrTab.History -> HistoryPanel(container, session, historyItems, Modifier.weight(1f).fillMaxWidth()) { refresh() }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
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
}

@Composable
private fun GeneratePanel(
    container: AppContainer,
    settings: AppSettings,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val preview = remember(session.pngBytes) { session.pngBytes?.toImageBitmap() }
    IoTwoPaneRow(
        container = container,
        settings = settings,
        paneKey = "qrcode-generate",
        minLeft = 300f,
        minRight = 280f,
        defaultLeftFraction = 0.5f,
        modifier = modifier.padding(12.dp),
        left = {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("qrcode.content"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            MooTextField(
                session.content,
                {
                    applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                        session.content = it
                        session.error = ""
                    }
                    onChanged()
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                singleLine = false
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.mooQrOptionsRow(),
            ) {
                Text(container.t("qrcode.size"), color = colors.textMuted, fontSize = 11.sp)
                MooTextField(session.size.toString(), { value ->
                    session.size = QrWiringPresentation.parseSizeField(value, session.size)
                    onChanged()
                }, modifier = Modifier.width(80.dp), compact = true)
                Text("px", color = colors.textMuted, fontSize = 11.sp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.mooQrOptionsRow(),
            ) {
                Text(container.t("qrcode.correction"), color = colors.textMuted, fontSize = 11.sp)
                Box {
                    var correctionOpen by remember { mutableStateOf(false) }
                    MooButton(container.t("qrcode.level.${session.correction.name}"), onClick = { correctionOpen = true }, p5Toolbar = true)
                    MooMenu(expanded = correctionOpen, onDismissRequest = { correctionOpen = false }) {
                        QrErrorCorrection.entries.forEach { level ->
                            MooMenuItem(onClick = {
                                correctionOpen = false
                                session.correction = level
                                onChanged()
                            }) {
                                Text(container.t("qrcode.level.${level.name}"))
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(container.t("qrcode.logo"), color = colors.textMuted, fontSize = 11.sp)
                FileDropRow(
                    chooseLabel = container.t("qrcode.chooseLogo"),
                    fileName = session.logoName,
                    emptyLabel = "—",
                    modifier = Modifier.weight(1f),
                    p5Toolbar = true,
                    onChoose = {
                        val file = pickImageFile(container, container.t("qrcode.chooseLogo")) ?: return@FileDropRow
                        persistToolsExportDirectory(container, file)
                        applyLogoFile(container, session, file, onChanged)
                    },
                    onDropFiles = { files ->
                        applyLogoFile(container, session, files.first(), onChanged)
                    }
                )
                if (session.logoName.isNotEmpty()) {
                    MooButton(
                        container.t("common.clear"),
                        onClick = {
                            session.logoName = ""
                            session.logoPath = ""
                            session.logoImage = null
                            onChanged()
                        },
                        p5Toolbar = true
                    )
                }
            }
            MooButton(
                if (session.busy) container.t("common.processing") else container.t("qrcode.generate"),
                prominent = true,
                p5Toolbar = true,
                enabled = QrWiringPresentation.canGenerate(session.busy, session.content),
                modifier = Modifier.mooQrGenerateButton(),
                onClick = {
                    generateQr(container, session, scope, onChanged)
                }
            )
        }
        },
        right = {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PreviewBox(preview, container.t("qrcode.preview"), Modifier.weight(1f).fillMaxWidth())
            Row(
                Modifier.mooQrPreviewActions(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MooButton(container.t("common.save"), enabled = QrWiringPresentation.hasPngOutput(session.pngBytes), p5Toolbar = true, onClick = {
                    val bytes = session.pngBytes ?: return@MooButton
                    val file = pickSavePngFile(container, container.t("common.save")) ?: return@MooButton
                    when (val outcome = QrWiringPresentation.runWritePngFile(file, bytes)) {
                        QrWiringPresentation.WritePngOutcome.Success -> {
                            persistToolsExportDirectory(container, file)
                            session.notice = container.t("common.save")
                            container.toastSuccess(container.t("common.save"))
                            session.error = ""
                        }
                        is QrWiringPresentation.WritePngOutcome.Failure -> {
                            notifyQrIoFailure(container, session, outcome.error, file.path, onChanged)
                        }
                    }
                    onChanged()
                })
                MooButton(container.t("common.action.copy"), enabled = QrWiringPresentation.hasPngOutput(session.pngBytes), p5Toolbar = true, onClick = {
                    val bytes = session.pngBytes
                    session.notice = if (bytes != null) copyImage(bytes, container) else container.t("qrcode.nothingToCopy")
                    onChanged()
                })
            }
        }
        }
    )
}

@Composable
private fun RecognizePanel(
    container: AppContainer,
    settings: AppSettings,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val sourcePreview = remember(session.recognitionBytes) { session.recognitionBytes?.toImageBitmap() }
    IoTwoPaneRow(
        container = container,
        settings = settings,
        paneKey = "qrcode-recognize",
        minLeft = 300f,
        minRight = 280f,
        defaultLeftFraction = 0.5f,
        modifier = modifier.padding(12.dp),
        left = {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FileDropRow(
                    chooseLabel = container.t("qrcode.chooseImage"),
                    fileName = session.recognitionName,
                    emptyLabel = container.t("qrcode.chooseImage"),
                    modifier = Modifier.weight(1f),
                    p5Toolbar = true,
                    onChoose = {
                        val file = pickImageFile(container, container.t("qrcode.chooseImage")) ?: return@FileDropRow
                        persistToolsExportDirectory(container, file)
                        applyRecognitionFile(container, session, scope, file, onChanged)
                    },
                    onDropFiles = { files ->
                        applyRecognitionFile(container, session, scope, files.first(), onChanged)
                    }
                )
                MooButton(container.t("qrcode.fromClipboard"), p5Toolbar = true, onClick = {
                    val image = readClipboardImage()
                    if (image == null) {
                        notifyQrFailure(container, session, container.t("qrcode.clipboardEmpty"), onChanged)
                        return@MooButton
                    }
                    session.recognitionName = container.t("qrcode.clipboard")
                    session.recognitionBytes = QrEngine.toPng(QrEngine.flattenOnWhite(image))
                    recognizeQr(container, session, scope, onChanged)
                })
                MooButton(
                    if (session.busy) container.t("common.processing") else container.t("qrcode.recognize"),
                    prominent = true,
                    p5Toolbar = true,
                    enabled = QrWiringPresentation.canRecognize(session.recognitionBytes, session.busy),
                    onClick = { recognizeQr(container, session, scope, onChanged) }
                )
            }
            PreviewBox(sourcePreview, session.recognitionName.ifBlank { container.t("qrcode.chooseImage") }, Modifier.weight(1f).fillMaxWidth())
        }
        },
        right = {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("qrcode.result"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            MooTextField(
                session.recognitionResult,
                { session.recognitionResult = it; onChanged() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                singleLine = false
            )
            MooButton(container.t("common.action.copy"), enabled = QrWiringPresentation.canCopyRecognition(session.recognitionResult), p5Toolbar = true, onClick = {
                session.notice = copyText(session.recognitionResult, container)
                onChanged()
            })
        }
        }
    )
}

@Composable
private fun HistoryPanel(
    container: AppContainer,
    session: QrSession,
    items: List<HistoryRecord>,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(items, query) {
        val keyword = query.trim()
        if (keyword.isEmpty()) items
        else items.filter { item ->
            listOf(item.summary, item.input, item.output, item.operation).any { it.contains(keyword, ignoreCase = true) }
        }
    }
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MooTextField(
            query,
            { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = container.t("history.searchPlaceholder")
        )
        if (visible.isEmpty()) {
            Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(visible, key = { it.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            Modifier.weight(1f).mooFocusClickable {
                                applyHistory(session, item)
                                onChanged()
                            }.padding(4.dp)
                        ) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.input.take(240), color = MooTheme.colors.textSecondary, fontSize = 12.sp, maxLines = 2)
                            Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 11.sp)
                        }
                        MooButton(container.t("common.delete"), onClick = {
                            container.history.delete(item.id)
                            session.historyTick += 1
                            onChanged()
                        })
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
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
    val flatten = !colors.restoresWorkspaceChrome()
    val radius = if (flatten) 0.dp else MooTheme.dimens.radius
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.background(colors.surfaceSubtle, shape).border(1.dp, colors.borderSoft, shape),
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
    val size = QrWiringPresentation.generateSize(session.size)
    val correction = session.correction
    val logo = session.logoImage
    scope.launch(Dispatchers.Default) {
        val result = QrWiringPresentation.runGeneratePng(content, session.size, correction, logo)
        withContext(Dispatchers.Swing) {
            session.busy = false
            when (result) {
                is QrWiringPresentation.GenerateOutcome.Success -> {
                val bytes = result.png
                session.size = result.size
                session.pngBytes = bytes
                session.notice = container.t("qrcode.generated")
                container.toastSuccess(container.t("qrcode.generated"))
                session.error = ""
                container.updateSettings { current ->
                    current.copy(tools = current.tools.copy(qrCodeSize = size, qrErrorCorrection = correction.name))
                }
                container.history.save(
                    ToolId.QrCode.id,
                    container.t("qrcode.history.generate"),
                    container.t("qrcode.history.generate"),
                    content,
                    pngToDataUrl(bytes),
                    QrHistoryMetadata.encodeGenerate(result.size, correction),
                )
                }
                is QrWiringPresentation.GenerateOutcome.Failure -> {
                notifyQrFailure(container, session, result.error, onChanged)
                }
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
                container.toastSuccess(container.t("qrcode.recognized"))
                session.error = ""
                container.history.save(
                    ToolId.QrCode.id,
                    container.t("qrcode.history.recognize"),
                    container.t("qrcode.history.recognize"),
                    name,
                    text,
                    QrHistoryMetadata.encodeRecognize(),
                )
            }.onFailure { error ->
                notifyQrFailure(container, session, error, onChanged)
            }
            onChanged()
        }
    }
}

private fun applyHistory(session: QrSession, item: HistoryRecord) {
    QrHistoryRestore.apply(session, item)
}

private fun notifyQrFailure(
    container: AppContainer,
    session: QrSession,
    message: String,
    onChanged: () -> Unit,
) {
    session.notice = ""
    session.error = message
    if (QrWiringPresentation.shouldToastErrorMessage()) {
        container.toastError(message)
    }
    onChanged()
}

private fun notifyQrIoFailure(
    container: AppContainer,
    session: QrSession,
    error: Throwable,
    path: String,
    onChanged: () -> Unit,
) {
    val message = container.t(
        "reformat.error.write",
        mapOf("message" to (error.message ?: path)),
    )
    session.error = message
    session.notice = message
    if (QrWiringPresentation.shouldToastOperationFailure(error)) {
        container.toastError(message)
    }
    onChanged()
}

private fun notifyQrFailure(
    container: AppContainer,
    session: QrSession,
    error: Throwable,
    onChanged: () -> Unit,
) {
    if (!QrWiringPresentation.shouldToastOperationFailure(error)) {
        session.notice = ""
        session.error = messageFor(container, error)
        onChanged()
        return
    }
    notifyQrFailure(container, session, messageFor(container, error), onChanged)
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
    return if (container.copyText(value)) container.t("json.notice.copied") else container.t("qrcode.error.generic")
}

private fun copyImage(png: ByteArray, container: AppContainer): String = runCatching {
    val image = ImageIO.read(ByteArrayInputStream(png)) ?: return container.t("qrcode.error.image")
    Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageSelection(image), null)
    container.toastCopied(true)
    container.t("json.notice.copied")
}.getOrElse {
    container.toastCopied(false)
    container.t("qrcode.error.generic")
}

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

private fun applyLogoFile(container: AppContainer, session: QrSession, file: File, onChanged: () -> Unit) {
    runCatching { QrEngine.readImageFile(file.toPath()) }
        .onSuccess { image ->
            session.logoName = file.name
            session.logoPath = file.absolutePath
            session.logoImage = image
            session.error = ""
            onChanged()
        }
        .onFailure { error ->
            notifyQrFailure(container, session, error, onChanged)
        }
}

private fun applyRecognitionFile(
    container: AppContainer,
    session: QrSession,
    scope: kotlinx.coroutines.CoroutineScope,
    file: File,
    onChanged: () -> Unit
) {
    session.recognitionName = file.name
    session.recognitionBytes = file.readBytes()
    recognizeQr(container, session, scope, onChanged)
}

private fun pickImageFile(container: AppContainer, title: String): File? =
    chooseFileWithExportDirectory(container, save = false, title = title)

private fun pickSavePngFile(container: AppContainer, title: String): File? {
    val chosen = chooseFileWithExportDirectory(
        container,
        save = true,
        title = title,
        defaultFileName = "mootool-qrcode.png",
    ) ?: return null
    return if (chosen.extension.isBlank()) File(chosen.path + ".png") else chosen
}
