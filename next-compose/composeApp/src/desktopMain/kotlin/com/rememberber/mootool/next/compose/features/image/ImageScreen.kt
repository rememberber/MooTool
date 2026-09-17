package com.rememberber.mootool.next.compose.features.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ToolsExportWiringPresentation
import com.rememberber.mootool.next.compose.domain.ScreenCaptureFailureMessages
import com.rememberber.mootool.next.compose.domain.CompressImageOptions
import com.rememberber.mootool.next.compose.domain.ImageEngine
import com.rememberber.mootool.next.compose.domain.ImageHistoryMetadata
import com.rememberber.mootool.next.compose.domain.ImageHistoryRestore
import com.rememberber.mootool.next.compose.domain.ImageException
import com.rememberber.mootool.next.compose.domain.ImageOutputFormat
import com.rememberber.mootool.next.compose.domain.ImageOutputMode
import com.rememberber.mootool.next.compose.domain.ImageSvgDetail
import com.rememberber.mootool.next.compose.domain.ImageSvgPreset
import com.rememberber.mootool.next.compose.domain.ImageVectorizeOptions
import com.rememberber.mootool.next.compose.domain.ScreenColorSampler
import com.rememberber.mootool.next.compose.domain.ScreenRegionPicker
import com.rememberber.mootool.next.compose.domain.WatermarkFontSize
import com.rememberber.mootool.next.compose.domain.WatermarkImageOptions
import com.rememberber.mootool.next.compose.domain.WatermarkPosition
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ImageSession
import com.rememberber.mootool.next.compose.storage.ImageAsset
import com.rememberber.mootool.next.compose.storage.ImageAssetSummary
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.desktopFileDropTarget
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image as AwtImage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlin.io.path.writeText

@Composable
fun ImageScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.imageSession() }
    var imageWorkJob by remember { mutableStateOf<Job?>(null) }
    fun startImageWork(job: Job?) {
        imageWorkJob?.cancel()
        imageWorkJob = job
    }
    DismissModalOverlaysOnDispose(container, ToolId.Image) {
        imageWorkJob?.cancel()
        imageWorkJob = null
        session.dismissModalOverlays()
    }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var assets by remember { mutableStateOf(emptyList<ImageAssetSummary>()) }
    var current by remember { mutableStateOf<ImageAsset?>(null) }

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistImage()
    }

    fun loadAssets(preferred: String? = session.currentName) {
        assets = container.imageLibrary.list()
        val name = preferred?.takeIf { candidate -> assets.any { it.name == candidate } } ?: assets.firstOrNull()?.name
        if (name == null) {
            current = null
            session.currentName = ""
            session.selectedNames = emptyList()
        } else {
            current = runCatching { container.imageLibrary.read(name) }.getOrNull()
            session.currentName = current?.name ?: ""
            session.selectedNames = session.selectedNames.filter { selected -> assets.any { it.name == selected } }
                .ifEmpty { listOfNotNull(current?.name) }
            session.fit = true
            session.zoom = 1f
        }
        refresh()
    }

    LaunchedEffect(Unit) { loadAssets() }
    LaunchedEffect(settings.data.directory, sessionGeneration) { loadAssets() }

    val processing = session.selectedNames.ifEmpty { listOfNotNull(current?.name) }
    val preview = remember(current?.bytes) { current?.bytes?.toImageBitmap() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
    var moreOpen by remember { mutableStateOf(false) }
    fun toggleList() {
        session.listVisible = !session.listVisible
        refresh()
    }
    fun importBase64() {
        session.base64Mode = "import"
        session.base64Text = ""
        refresh()
    }
    fun exportBase64() {
        session.base64Mode = "export"
        session.base64Text = current?.let {
            ImageEngine.toDataUrl(it.image, if (ImageEngine.isJpegName(it.name)) ImageOutputFormat.Jpeg else ImageOutputFormat.Png)
        }.orEmpty()
        refresh()
    }
    fun openHistory() {
        session.historyOpen = true
        refresh()
    }
    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooPageTitle(container.t("image.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            MooButton(if (session.listVisible) container.t("image.hideList") else container.t("image.showList"), onClick = { toggleList() }, p5Toolbar = true)
            MooButton(
                container.t("image.screenshot"),
                onClick = { startImageWork(capture(container, session, scope) { loadAssets(it) }) },
                enabled = !session.busy,
                p5Toolbar = true
            )
            if (!overflow) {
                MooButton(container.t("image.fromClipboard"), onClick = { importClipboard(container, session) { loadAssets(it) } }, enabled = !session.busy, p5Toolbar = true)
            }
            MooButton(container.t("image.import"), onClick = { importFiles(container, session) { loadAssets(it) } }, enabled = !session.busy, p5Toolbar = true)
            if (!overflow) {
                MooButton(container.t("image.fromBase64"), onClick = { importBase64() }, enabled = !session.busy, p5Toolbar = true)
                Spacer(Modifier.width(8.dp))
                MooButton(container.t("image.toSvg"), onClick = { session.svgOpen = true; refresh() }, enabled = processing.isNotEmpty() && !session.busy, p5Toolbar = true)
                MooButton(container.t("image.compress"), onClick = { session.compressOpen = true; refresh() }, enabled = processing.isNotEmpty() && !session.busy, p5Toolbar = true)
                MooButton(container.t("image.watermark"), onClick = { session.watermarkOpen = true; refresh() }, enabled = processing.isNotEmpty() && !session.busy, p5Toolbar = true)
            }
            MooButton(container.t("common.save"), onClick = {
                session.saveOpen = true
                session.promptValue = current?.name.orEmpty()
                refresh()
            }, enabled = current != null && !session.busy, p5Toolbar = true)
            MooButton(container.t("image.copy"), onClick = { copyCurrent(container, session, current) }, enabled = current != null, p5Toolbar = true)
            if (!overflow) {
                MooButton(container.t("image.toBase64"), onClick = { exportBase64() }, enabled = current != null, p5Toolbar = true)
            }
            if (session.busy) {
                MooButton(container.t("image.cancel"), onClick = { session.cancelled = true; refresh() }, p5Toolbar = true)
            }
            if (!overflow) {
                MooButton(container.t("common.action.history"), onClick = { openHistory() }, p5Toolbar = true)
            }
            if (overflow) {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true }, p5Toolbar = true)
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MooMenuItem(
                            onClick = { moreOpen = false; importClipboard(container, session) { loadAssets(it) } },
                            enabled = !session.busy
                        ) {
                            Text(container.t("image.fromClipboard"))
                        }
                        MooMenuItem(
                            onClick = { moreOpen = false; importBase64() },
                            enabled = !session.busy
                        ) {
                            Text(container.t("image.fromBase64"))
                        }
                        MooMenuItem(
                            onClick = { moreOpen = false; session.svgOpen = true; refresh() },
                            enabled = processing.isNotEmpty() && !session.busy
                        ) {
                            Text(container.t("image.toSvg"))
                        }
                        MooMenuItem(
                            onClick = { moreOpen = false; session.compressOpen = true; refresh() },
                            enabled = processing.isNotEmpty() && !session.busy
                        ) {
                            Text(container.t("image.compress"))
                        }
                        MooMenuItem(
                            onClick = { moreOpen = false; session.watermarkOpen = true; refresh() },
                            enabled = processing.isNotEmpty() && !session.busy
                        ) {
                            Text(container.t("image.watermark"))
                        }
                        MooMenuItem(
                            onClick = { moreOpen = false; exportBase64() },
                            enabled = current != null
                        ) {
                            Text(container.t("image.toBase64"))
                        }
                        MooMenuItem(onClick = { moreOpen = false; openHistory() }) {
                            Text(container.t("common.action.history"))
                        }
                        if (!detached) {
                            MooMenuItem(onClick = { moreOpen = false; container.sessionManager.detach(ToolId.Image) }) {
                                Text(container.t("app.tool.detach"))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!detached && !overflow) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Image) })
            }
        }
        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        } else if (session.notice.isNotEmpty()) {
            Text(session.notice, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        Row(
            Modifier.fillMaxSize().weight(1f).desktopFileDropTarget(enabled = !session.busy, acceptMultiple = true) { dropped ->
                importPickedFiles(container, session, dropped) { loadAssets(it) }
            }
        ) {
            if (session.listVisible) {
                val listWidth = settings.layout.pane(ToolId.Image.id, 0, 240f, 200f, 320f)
                Column(Modifier.width(listWidth.dp).fillMaxHeight().mooToolShell(colors.sidebar)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(container.t("image.library"), color = colors.textPrimary)
                        MooButton(container.t("image.import"), onClick = { importFiles(container, session) { loadAssets(it) } }, enabled = !session.busy, p5Toolbar = true)
                    }
                    if (assets.isEmpty()) {
                        Text(container.t("image.empty"), color = colors.textSecondary, modifier = Modifier.padding(12.dp))
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(assets, key = { it.name }) { asset ->
                                val interaction = remember(asset.name) { MutableInteractionSource() }
                                val hovered by interaction.collectIsHoveredAsState()
                                val active = asset.name == current?.name
                                val shape = RoundedCornerShape(6.dp)
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 47.dp).clip(shape)
                                        .background(if (active || hovered) colors.control else Color.Transparent)
                                        .hoverable(interaction)
                                        .mooFocusClickable(shape = shape) {
                                            current = runCatching { container.imageLibrary.read(asset.name) }.getOrNull()
                                            session.currentName = asset.name
                                            if (asset.name !in session.selectedNames) session.selectedNames = listOf(asset.name)
                                            session.fit = true
                                            session.zoom = 1f
                                            refresh()
                                        }
                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(asset.name in session.selectedNames, { checked ->
                                        session.selectedNames = if (checked) (session.selectedNames + asset.name).distinct()
                                        else session.selectedNames.filterNot { it == asset.name }
                                        refresh()
                                    })
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            asset.name,
                                            color = colors.textBody,
                                            fontSize = 10.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${asset.width} × ${asset.height} · ${ImageEngine.formatBytes(asset.size)}",
                                            color = colors.textMuted,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton(container.t("image.rename"), p5Toolbar = true, onClick = {
                            session.renameOpen = true
                            session.promptValue = current?.name.orEmpty()
                            refresh()
                        }, enabled = current != null && !session.busy)
                        MooButton(container.t("image.export"), onClick = { exportSelected(container, session, processing) }, enabled = processing.isNotEmpty() && !session.busy, p5Toolbar = true)
                        MooButton(container.t("common.delete"), onClick = { session.deleteOpen = true; refresh() }, enabled = processing.isNotEmpty() && !session.busy, p5Toolbar = true)
                    }
                }
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Image.id, 0, listWidth + it, 1) },
                    onReset = { container.setPaneSize(ToolId.Image.id, 0, 240f, 1) }
                )
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(colors.workspace)) {
                    if (preview == null || current == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(container.t("image.emptyPreview"), color = colors.textSecondary)
                        }
                    } else {
                        val density = LocalDensity.current
                        val canvasWidth = maxWidth
                        val canvasHeight = maxHeight
                        val scrollH = rememberScrollState()
                        val scrollV = rememberScrollState()
                        Box(
                            Modifier.fillMaxSize().horizontalScroll(if (session.fit) rememberScrollState() else scrollH).verticalScroll(if (session.fit) rememberScrollState() else scrollV)
                                .pointerInput(current?.name) {
                                    detectTapGestures(onDoubleTap = {
                                        current?.let { asset ->
                                            runCatching { Desktop.getDesktop().open(container.imageLibrary.pathFor(asset.name).toFile()) }
                                        }
                                    })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val width = if (session.fit) canvasWidth else with(density) { (current!!.width * session.zoom).toDp() }
                            val height = if (session.fit) canvasHeight else with(density) { (current!!.height * session.zoom).toDp() }
                            Box(Modifier.size(width, height)) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val cell = 8.dp.toPx()
                                    val cols = (size.width / cell).toInt() + 1
                                    val rows = (size.height / cell).toInt() + 1
                                    for (row in 0 until rows) {
                                        for (col in 0 until cols) {
                                            drawRect(
                                                color = if ((row + col) % 2 == 0) Color(0xFFCCCCCC) else Color(0xFFEEEEEE),
                                                topLeft = androidx.compose.ui.geometry.Offset(col * cell, row * cell),
                                                size = androidx.compose.ui.geometry.Size(cell, cell)
                                            )
                                        }
                                    }
                                }
                                Image(
                                    bitmap = preview,
                                    contentDescription = current?.name,
                                    contentScale = if (session.fit) ContentScale.Fit else ContentScale.FillBounds,
                                    filterQuality = FilterQuality.None,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(40.dp).mooStatusBarBackground().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MooButton("+", onClick = { session.fit = false; session.zoom = (session.zoom * 1.1f).coerceAtMost(5f); refresh() }, enabled = current != null, p5Toolbar = true)
                    MooButton("-", onClick = { session.fit = false; session.zoom = (session.zoom * 0.9f).coerceAtLeast(0.1f); refresh() }, enabled = current != null, p5Toolbar = true)
                    MooButton(container.t("image.original"), onClick = { session.fit = false; session.zoom = 1f; refresh() }, enabled = current != null, p5Toolbar = true)
                    MooButton(container.t("image.fit"), onClick = { session.fit = true; refresh() }, enabled = current != null, p5Toolbar = true)
                    Text(
                        current?.let {
                            val zoomLabel = if (session.fit) container.t("image.fit") else "${(session.zoom * 100).toInt()}%"
                            "${it.width} × ${it.height} · ${ImageEngine.formatBytes(it.size)} · $zoomLabel"
                        }.orEmpty(),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    }

    if (session.base64Mode != null) Base64Dialog(container, session) { loadAssets(it) }
    if (session.compressOpen) CompressDialog(container, session, processing.size) {
        startImageWork(
            processImages(
                container = container,
                session = session,
                names = processing,
                scope = scope,
                suffix = "compressed",
                transform = { asset ->
                    val format = ImageEngine.resolveFormat(session.compressFormat, ImageEngine.isJpegName(asset.name))
                    val bytes = ImageEngine.encodeCompressed(
                        asset.image,
                        ImageEngine.isJpegName(asset.name),
                        CompressImageOptions(session.compressQuality / 100f, session.compressScale / 100f, session.compressFormat)
                    )
                    ImageEngine.decode(bytes) to (format == ImageOutputFormat.Jpeg)
                },
                onLoaded = { loadAssets(it) }
            )
        )
    }
    if (session.watermarkOpen) WatermarkDialog(container, session, processing.size) {
        startImageWork(
            processImages(
                container = container,
                session = session,
                names = processing,
                scope = scope,
                suffix = "watermarked",
                transform = { asset ->
                    ImageEngine.watermark(
                        asset.image,
                        WatermarkImageOptions(
                            session.watermarkText,
                            session.watermarkOpacity / 100f,
                            session.watermarkColor,
                            session.watermarkPosition,
                            session.watermarkFont,
                            session.watermarkDiagonal
                        )
                    ) to ImageEngine.isJpegName(asset.name)
                },
                onLoaded = { loadAssets(it) }
            )
        )
    }
    if (session.svgOpen) SvgDialog(container, session, processing.size) {
        startImageWork(vectorize(container, session, processing, scope) { loadAssets() })
    }
    if (session.renameOpen) PromptDialog(container, session, container.t("image.renamePrompt"), container.t("image.rename")) {
        runCatching {
            val renamed = container.imageLibrary.rename(session.currentName, session.promptValue)
            session.renameOpen = false
            loadAssets(renamed.name)
        }.onFailure { session.error = messageFor(container, it); refresh() }
    }
    if (session.saveOpen) PromptDialog(container, session, container.t("image.saveName"), container.t("common.save")) {
        val asset = current ?: return@PromptDialog
        runCatching {
            val saved = container.imageLibrary.save(session.promptValue, asset.image, ImageEngine.isJpegName(session.promptValue))
            session.saveOpen = false
            loadAssets(saved.name)
            session.notice = container.t("image.saved")
            container.toastSuccess(container.t("image.saved"))
        }.onFailure { session.error = messageFor(container, it); refresh() }
    }
    if (session.deleteOpen) {
        MooOverlay(onDismiss = { session.deleteOpen = false; refresh() }) {
            Column(Modifier.width(420.dp).mooDialogSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(container.t("image.confirmDelete", mapOf("count" to processing.size.toString())), color = MooTheme.colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.delete"), danger = true, onClick = {
                        container.imageLibrary.delete(processing)
                        session.deleteOpen = false
                        session.currentName = ""
                        session.selectedNames = emptyList()
                        session.notice = container.t("favorite.deleted")
                        container.toastSuccess(container.t("favorite.deleted"))
                        loadAssets("")
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.deleteOpen = false; refresh() })
                }
            }
        }
    }
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Image.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                val plan = ImageHistoryRestore.plan(item, assets.map { it.name }.toSet())
                session.lastOutputs = plan.lastOutputs
                plan.preferredAsset?.let { loadAssets(it) }
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

@Composable
private fun Base64Dialog(container: AppContainer, session: ImageSession, onLoaded: (String?) -> Unit) {
    val export = session.base64Mode == "export"
    MooOverlay(onDismiss = { session.base64Mode = null; container.sessionManager.persistImage(); container.sessionManager.bump() }) {
        Column(Modifier.width(640.dp).mooDialogSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t(if (export) "image.base64Export" else "image.base64Import"), color = MooTheme.colors.textPrimary)
            MooTextField(
                session.base64Text,
                { value -> session.onUserInput { session.base64Text = value; container.sessionManager.bump() } },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                singleLine = false
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!export) {
                    MooButton(container.t("image.import"), prominent = true, onClick = {
                        runCatching {
                            val image = ImageEngine.decodeDataUrl(session.base64Text)
                            val saved = container.imageLibrary.save(ImageEngine.timestampName("Base64"), image, false)
                            session.base64Mode = null
                            onLoaded(saved.name)
                        }.onFailure {
                            session.error = messageFor(container, it)
                            container.sessionManager.bump()
                        }
                    }, enabled = session.base64Text.isNotBlank())
                }
                MooButton(container.t("common.cancel"), onClick = { session.base64Mode = null; container.sessionManager.bump() })
            }
        }
    }
}

@Composable
private fun CompressDialog(container: AppContainer, session: ImageSession, count: Int, onConfirm: () -> Unit) {
    MooOverlay(onDismiss = { session.compressOpen = false; container.sessionManager.bump() }) {
        Column(Modifier.width(460.dp).mooDialogSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(container.t("image.compressTitle"), color = MooTheme.colors.textPrimary)
            Text(container.t("image.selectedCount", mapOf("count" to count.toString())), color = MooTheme.colors.textMuted, fontSize = 11.sp)
            Text("${container.t("image.quality")} · ${session.compressQuality}%", color = MooTheme.colors.textBody, fontSize = 11.sp)
            Slider(session.compressQuality / 100f, { session.compressQuality = (it * 100).toInt().coerceIn(10, 100); container.sessionManager.bump() })
            Text("${container.t("image.scale")} · ${session.compressScale}%", color = MooTheme.colors.textBody, fontSize = 11.sp)
            Slider(session.compressScale / 100f, { session.compressScale = (it * 100).toInt().coerceIn(10, 100); container.sessionManager.bump() })
            FormatPicker(container, session)
            OutputModePicker(container, session)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("image.startProcess"), prominent = true, onClick = { session.compressOpen = false; onConfirm() })
                MooButton(container.t("common.cancel"), onClick = { session.compressOpen = false; container.sessionManager.bump() })
            }
        }
    }
}

@Composable
private fun WatermarkDialog(container: AppContainer, session: ImageSession, count: Int, onConfirm: () -> Unit) {
    MooOverlay(onDismiss = { session.watermarkOpen = false; container.sessionManager.bump() }) {
        Column(Modifier.width(480.dp).mooDialogSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(container.t("image.watermarkTitle"), color = MooTheme.colors.textPrimary)
            Text(container.t("image.selectedCount", mapOf("count" to count.toString())), color = MooTheme.colors.textMuted, fontSize = 11.sp)
            MooTextField(
                session.watermarkText,
                { value -> session.onUserInput { session.watermarkText = value; container.sessionManager.bump() } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = container.t("image.watermarkText")
            )
            Text("${container.t("image.opacity")} · ${session.watermarkOpacity}%", color = MooTheme.colors.textBody, fontSize = 11.sp)
            Slider(session.watermarkOpacity / 100f, { session.watermarkOpacity = (it * 100).toInt().coerceIn(5, 100); container.sessionManager.bump() })
            MooTextField(
                session.watermarkColor,
                { value -> session.onUserInput { session.watermarkColor = value; container.sessionManager.bump() } },
                modifier = Modifier.width(120.dp),
                placeholder = "#FFFFFF",
                compact = true
            )
            PositionPicker(container, session)
            FontPicker(container, session)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Checkbox(session.watermarkDiagonal, { session.watermarkDiagonal = it; container.sessionManager.bump() })
                Text(container.t("image.diagonal"), color = MooTheme.colors.textMuted, fontSize = 10.sp)
            }
            OutputModePicker(container, session)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("image.startProcess"), prominent = true, onClick = { session.watermarkOpen = false; onConfirm() }, enabled = session.watermarkText.isNotBlank())
                MooButton(container.t("common.cancel"), onClick = { session.watermarkOpen = false; container.sessionManager.bump() })
            }
        }
    }
}

@Composable
private fun SvgDialog(container: AppContainer, session: ImageSession, count: Int, onConfirm: () -> Unit) {
    MooOverlay(onDismiss = { session.svgOpen = false; container.sessionManager.bump() }) {
        Column(Modifier.width(480.dp).mooDialogSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("image.svgTitle"), color = MooTheme.colors.textPrimary)
            Text(container.t("image.selectedCount", mapOf("count" to count.toString())), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            PresetPicker(container, session)
            if (session.svgPreset != ImageSvgPreset.Bw) {
                Text(container.t("image.svgColors"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                MooTextField(
                    session.svgColors.toString(),
                    { value ->
                        session.onUserInput {
                            session.svgColors = value.toIntOrNull()?.coerceIn(2, 64) ?: session.svgColors
                            container.sessionManager.bump()
                        }
                    },
                    modifier = Modifier.width(80.dp)
                )
            }
            DetailPicker(container, session)
            Text(container.t("image.svgSpeckle"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            MooTextField(
                session.svgSpeckle.toString(),
                { value ->
                    session.onUserInput {
                        session.svgSpeckle = value.toIntOrNull()?.coerceIn(0, 128) ?: session.svgSpeckle
                        container.sessionManager.bump()
                    }
                },
                modifier = Modifier.width(80.dp)
            )
            Text(container.t("image.svgHint"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("image.svgStart"), prominent = true, onClick = { session.svgOpen = false; onConfirm() })
                MooButton(container.t("common.cancel"), onClick = { session.svgOpen = false; container.sessionManager.bump() })
            }
        }
    }
}

@Composable
private fun PromptDialog(container: AppContainer, session: ImageSession, title: String, confirm: String, onConfirm: () -> Unit) {
    MooOverlay(onDismiss = { session.renameOpen = false; session.saveOpen = false; container.sessionManager.bump() }) {
        Column(Modifier.width(420.dp).mooDialogSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MooTheme.colors.textPrimary)
            MooTextField(
                session.promptValue,
                { value -> session.onUserInput { session.promptValue = value; container.sessionManager.bump() } },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(confirm, prominent = true, onClick = onConfirm, enabled = session.promptValue.isNotBlank())
                MooButton(container.t("common.cancel"), onClick = { session.renameOpen = false; session.saveOpen = false; container.sessionManager.bump() })
            }
        }
    }
}

@Composable
private fun FormatPicker(container: AppContainer, session: ImageSession) {
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton("${container.t("image.outputFormat")}: ${container.t(formatKey(session.compressFormat))}", onClick = { open = true })
        MooMenu(open, onDismissRequest = { open = false }) {
            ImageOutputFormat.entries.forEach { format ->
                MooMenuItem(onClick = { session.compressFormat = format; open = false; container.sessionManager.bump() }) {
                    Text(container.t(formatKey(format)))
                }
            }
        }
    }
}

@Composable
private fun OutputModePicker(container: AppContainer, session: ImageSession) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(container.t("image.outputMode"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        MooButton(container.t("image.keepOriginal"), primary = session.outputMode == ImageOutputMode.Keep, onClick = { session.outputMode = ImageOutputMode.Keep; container.sessionManager.bump() })
        MooButton(container.t("image.overwrite"), primary = session.outputMode == ImageOutputMode.Overwrite, onClick = { session.outputMode = ImageOutputMode.Overwrite; container.sessionManager.bump() })
    }
}

@Composable
private fun PositionPicker(container: AppContainer, session: ImageSession) {
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton("${container.t("image.position")}: ${container.t(positionKey(session.watermarkPosition))}", onClick = { open = true })
        MooMenu(open, onDismissRequest = { open = false }) {
            WatermarkPosition.entries.forEach { position ->
                MooMenuItem(onClick = { session.watermarkPosition = position; open = false; container.sessionManager.bump() }) {
                    Text(container.t(positionKey(position)))
                }
            }
        }
    }
}

@Composable
private fun FontPicker(container: AppContainer, session: ImageSession) {
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton("${container.t("image.fontSize")}: ${container.t(fontKey(session.watermarkFont))}", onClick = { open = true })
        MooMenu(open, onDismissRequest = { open = false }) {
            WatermarkFontSize.entries.forEach { size ->
                MooMenuItem(onClick = { session.watermarkFont = size; open = false; container.sessionManager.bump() }) {
                    Text(container.t(fontKey(size)))
                }
            }
        }
    }
}

@Composable
private fun PresetPicker(container: AppContainer, session: ImageSession) {
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton("${container.t("image.svgPreset")}: ${container.t(presetKey(session.svgPreset))}", onClick = { open = true })
        MooMenu(open, onDismissRequest = { open = false }) {
            ImageSvgPreset.entries.forEach { preset ->
                MooMenuItem(onClick = { session.svgPreset = preset; open = false; container.sessionManager.bump() }) {
                    Text(container.t(presetKey(preset)))
                }
            }
        }
    }
}

@Composable
private fun DetailPicker(container: AppContainer, session: ImageSession) {
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton("${container.t("image.svgDetail")}: ${container.t(detailKey(session.svgDetail))}", onClick = { open = true })
        MooMenu(open, onDismissRequest = { open = false }) {
            ImageSvgDetail.entries.forEach { detail ->
                MooMenuItem(onClick = { session.svgDetail = detail; open = false; container.sessionManager.bump() }) {
                    Text(container.t(detailKey(detail)))
                }
            }
        }
    }
}

private fun processImages(
    container: AppContainer,
    session: ImageSession,
    names: List<String>,
    scope: kotlinx.coroutines.CoroutineScope,
    suffix: String,
    transform: (ImageAsset) -> Pair<BufferedImage, Boolean>,
    onLoaded: (String?) -> Unit
): Job? {
    if (names.isEmpty()) return null
    session.busy = true
    session.cancelled = false
    session.error = ""
    session.notice = container.t("common.processing")
    container.sessionManager.bump()
    return scope.launch(Dispatchers.Default) {
        var preferred: String? = null
        runCatching {
            if (names.size > ImageEngine.MAX_BATCH) throw ImageException("too-many", "At most 20 images")
            for (name in names) {
                ensureActive()
                if (session.cancelled) throw ImageException("cancelled", "Cancelled")
                val asset = container.imageLibrary.read(name)
                val (image, jpeg) = transform(asset)
                val outputName = if (session.outputMode == ImageOutputMode.Overwrite) {
                    ImageEngine.overwriteName(asset.name, if (jpeg) ImageOutputFormat.Jpeg else ImageOutputFormat.Auto)
                } else {
                    val format = if (suffix == "compressed") session.compressFormat else if (jpeg) ImageOutputFormat.Jpeg else ImageOutputFormat.Auto
                    ImageEngine.processedImageName(asset.name, suffix, format)
                }
                val saved = container.imageLibrary.save(outputName, image, jpeg)
                if (session.outputMode == ImageOutputMode.Overwrite && saved.name != asset.name) {
                    container.imageLibrary.delete(listOf(asset.name))
                }
                preferred = saved.name
            }
        }.onSuccess {
            session.busy = false
            session.notice = container.t("image.processComplete", mapOf("count" to names.size.toString()))
            container.toastSuccess(session.notice)
            container.history.save(
                ToolId.Image.id,
                session.notice,
                session.notice,
                names.joinToString(),
                preferred.orEmpty(),
                ImageHistoryMetadata.OP_PROCESS,
            )
            withContext(Dispatchers.Swing) { onLoaded(preferred) }
        }.onFailure { error ->
            if (error is CancellationException) {
                session.busy = false
                session.notice = ""
                withContext(Dispatchers.Swing) { container.sessionManager.bump(); container.sessionManager.persistImage() }
                return@launch
            }
            session.busy = false
            session.notice = ""
            session.error = messageFor(container, error)
            withContext(Dispatchers.Swing) { container.sessionManager.bump(); container.sessionManager.persistImage() }
        }
    }
}

private fun vectorize(
    container: AppContainer,
    session: ImageSession,
    names: List<String>,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: () -> Unit
): Job? {
    if (names.isEmpty()) return null
    val options = ImageEngine.normalizeVectorizeOptions(
        ImageVectorizeOptions(session.svgPreset, session.svgColors, session.svgDetail, session.svgSpeckle)
    )
    val targets = chooseSvgTargets(container, names) ?: return null
    session.busy = true
    session.cancelled = false
    session.error = ""
    session.notice = container.t("common.processing")
    container.sessionManager.bump()
    return scope.launch(Dispatchers.Default) {
        val written = mutableListOf<Path>()
        runCatching {
            if (names.size > ImageEngine.MAX_BATCH) throw ImageException("too-many", "At most 20 images")
            names.zip(targets).forEach { (name, target) ->
                ensureActive()
                if (session.cancelled) throw ImageException("cancelled", "Cancelled")
                val asset = container.imageLibrary.read(name)
                val svg = ImageEngine.vectorize(asset.image, options)
                target.writeText(svg)
                written.add(target)
            }
        }.onSuccess {
            session.busy = false
            session.lastOutputs = written.map { it.toAbsolutePath().toString() }
            session.notice = container.t("image.svgComplete", mapOf("count" to written.size.toString(), "path" to written.first().parent.toString()))
            container.toastSuccess(session.notice)
            container.history.save(
                ToolId.Image.id,
                session.notice,
                session.notice,
                names.joinToString(),
                session.lastOutputs.joinToString("\n"),
                ImageHistoryMetadata.OP_SVG,
            )
            withContext(Dispatchers.Swing) { onDone(); container.sessionManager.persistImage(); container.sessionManager.bump() }
        }.onFailure { error ->
            if (targets.size > 1) written.forEach { runCatching { java.nio.file.Files.deleteIfExists(it) } }
            if (error is CancellationException) {
                session.busy = false
                session.notice = ""
                withContext(Dispatchers.Swing) { container.sessionManager.persistImage(); container.sessionManager.bump() }
                return@launch
            }
            session.busy = false
            session.notice = ""
            session.error = messageFor(container, error)
            withContext(Dispatchers.Swing) { container.sessionManager.persistImage(); container.sessionManager.bump() }
        }
    }
}

private fun capture(
    container: AppContainer,
    session: ImageSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoaded: (String?) -> Unit
): Job? {
    if (session.busy) return null
    session.busy = true
    session.error = ""
    session.notice = container.t("common.processing")
    container.sessionManager.bump()
    return scope.launch(Dispatchers.Default) {
        val capture = runCatching { ScreenColorSampler.captureAllScreens() }
        if (!isActive) {
            session.busy = false
            return@launch
        }
        withContext(Dispatchers.Swing) {
            if (!isActive) {
                session.busy = false
                container.sessionManager.bump()
                return@withContext
            }
            capture.onSuccess { image ->
                ScreenRegionPicker.show(
                    image,
                    container::t,
                    onPicked = { region ->
                        session.busy = false
                        runCatching {
                            val saved = container.imageLibrary.save(ImageEngine.timestampName("Screenshot"), region, false)
                            session.notice = container.t("image.imported")
                            container.toastSuccess(container.t("image.imported"))
                            onLoaded(saved.name)
                        }.onFailure {
                            session.error = messageFor(container, it)
                            container.sessionManager.bump()
                        }
                    },
                    onCancel = {
                        session.busy = false
                        session.notice = container.t("image.captureCancelled")
                        session.error = ""
                        container.sessionManager.bump()
                    }
                )
            }.onFailure { error ->
                session.busy = false
                session.notice = ""
                session.error = messageFor(container, error)
                container.sessionManager.bump()
            }
        }
    }
}

private fun importClipboard(container: AppContainer, session: ImageSession, onLoaded: (String?) -> Unit) {
    val image = readClipboardImage()
    if (image == null) {
        session.error = container.t("image.clipboardEmpty")
        container.sessionManager.bump()
        return
    }
    runCatching {
        val saved = container.imageLibrary.save(ImageEngine.timestampName("Untitled"), image, false)
        session.notice = container.t("image.imported")
        container.toastSuccess(container.t("image.imported"))
        session.error = ""
        onLoaded(saved.name)
    }.onFailure {
        session.error = messageFor(container, it)
        container.sessionManager.bump()
    }
}

private fun importFiles(container: AppContainer, session: ImageSession, onLoaded: (String?) -> Unit) {
    val files = chooseImages(container.t("image.import"))
    if (files.isEmpty()) return
    importPickedFiles(container, session, files, onLoaded)
}

private fun importPickedFiles(
    container: AppContainer,
    session: ImageSession,
    files: List<File>,
    onLoaded: (String?) -> Unit
) {
    if (files.isEmpty() || session.busy) return
    runCatching {
        val imported = container.imageLibrary.importFiles(files.map { it.toPath() })
        if (imported.isEmpty()) throw ImageException("invalid-image", "Unable to read image")
        session.notice = container.t("image.imported")
        container.toastSuccess(container.t("image.imported"))
        session.error = ""
        onLoaded(imported.first().name)
    }.onFailure {
        session.error = messageFor(container, it)
        container.sessionManager.bump()
    }
}

private fun exportSelected(container: AppContainer, session: ImageSession, names: List<String>) {
    val directory = chooseDirectory(container, container.t("image.export")) ?: return
    runCatching {
        container.imageLibrary.export(names, directory.toPath())
        session.notice = container.t("image.exported", mapOf("directory" to directory.absolutePath))
        container.toastSuccess(session.notice)
        session.error = ""
        session.lastOutputs = listOf(directory.absolutePath)
        container.sessionManager.persistImage()
        container.sessionManager.bump()
    }.onFailure {
        session.error = messageFor(container, it)
        container.sessionManager.bump()
    }
}

private fun copyCurrent(container: AppContainer, session: ImageSession, current: ImageAsset?) {
    val image = current?.image ?: return
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageSelection(image), null)
        session.notice = container.t("json.notice.copied")
        session.error = ""
        container.toastCopied(true)
    }.onFailure {
        session.error = container.t("common.copyFailed")
        container.toastCopied(false)
    }
    container.sessionManager.bump()
}

private fun chooseSvgTargets(container: AppContainer, names: List<String>): List<Path>? {
    val exportDir = ToolsExportWiringPresentation.defaultExportDirectory(container.settings.value.tools.exportDirectory)
    return if (names.size == 1) {
        val chosen = chooseSave(
            container.t("image.svgTitle"),
            ToolsExportWiringPresentation.defaultSvgPath(
                container.settings.value.tools.exportDirectory,
                names[0],
            ),
        ) ?: return null
        listOf(if (chosen.extension.equals("svg", true)) chosen.toPath() else File(chosen.path + ".svg").toPath())
    } else {
        val directory = chooseDirectory(container, container.t("image.svgTitle")) ?: return null
        val reserved = mutableSetOf<String>()
        names.map { name ->
            val stem = name.substringBeforeLast('.')
            var candidate = File(directory, "$stem.svg")
            var index = 2
            while (!reserved.add(candidate.name) || candidate.exists()) {
                candidate = File(directory, "${stem}_$index.svg")
                index += 1
            }
            candidate.toPath()
        }
    }
}

private fun chooseImages(title: String): List<File> {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    val files = dialog.files?.toList().orEmpty()
    if (files.isNotEmpty()) return files.filter { ImageLibraryStoreSafe(it.extension) }
    val directory = dialog.directory ?: return emptyList()
    val file = dialog.file ?: return emptyList()
    return listOf(File(directory, file)).filter { ImageLibraryStoreSafe(it.extension) }
}

private fun ImageLibraryStoreSafe(extension: String): Boolean =
    com.rememberber.mootool.next.compose.storage.ImageLibraryStore.supported(extension)

private fun chooseSave(title: String, defaultPath: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.directory = File(defaultPath).parent
    dialog.file = File(defaultPath).name
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}

private fun chooseDirectory(container: AppContainer, title: String): File? {
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val initial = ToolsExportWiringPresentation.defaultExportDirectory(container.settings.value.tools.exportDirectory)
    if (initial.isDirectory) chooser.currentDirectory = initial
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
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

private fun ByteArray.toImageBitmap(): ImageBitmap =
    SkiaImage.makeFromEncoded(this).toComposeImageBitmap()

private fun messageFor(container: AppContainer, error: Throwable): String =
    ScreenCaptureFailureMessages.imageOperationMessage({ container.t(it) }, error)

private fun formatKey(format: ImageOutputFormat) = when (format) {
    ImageOutputFormat.Auto -> "image.format.auto"
    ImageOutputFormat.Png -> "image.format.png"
    ImageOutputFormat.Jpeg -> "image.format.jpeg"
}

private fun positionKey(position: WatermarkPosition) = when (position) {
    WatermarkPosition.BottomRight -> "image.position.bottom-right"
    WatermarkPosition.BottomLeft -> "image.position.bottom-left"
    WatermarkPosition.TopRight -> "image.position.top-right"
    WatermarkPosition.TopLeft -> "image.position.top-left"
    WatermarkPosition.Center -> "image.position.center"
    WatermarkPosition.Tile -> "image.position.tile"
}

private fun fontKey(size: WatermarkFontSize) = when (size) {
    WatermarkFontSize.Auto -> "image.font.auto"
    WatermarkFontSize.Small -> "image.font.small"
    WatermarkFontSize.Medium -> "image.font.medium"
    WatermarkFontSize.Large -> "image.font.large"
}

private fun presetKey(preset: ImageSvgPreset) = when (preset) {
    ImageSvgPreset.Poster -> "image.svgPreset.poster"
    ImageSvgPreset.Photo -> "image.svgPreset.photo"
    ImageSvgPreset.Bw -> "image.svgPreset.bw"
}

private fun detailKey(detail: ImageSvgDetail) = when (detail) {
    ImageSvgDetail.Low -> "image.svgDetail.low"
    ImageSvgDetail.Medium -> "image.svgDetail.medium"
    ImageSvgDetail.High -> "image.svgDetail.high"
}
