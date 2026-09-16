package com.rememberber.mootool.next.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppTray
import com.rememberber.mootool.next.compose.app.AppTrayHostProfile
import com.rememberber.mootool.next.compose.app.AppTrayModel
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.app.TrayDesktopActions
import com.rememberber.mootool.next.compose.features.config.ConfigConvertScreen
import com.rememberber.mootool.next.compose.features.calculator.CalculatorScreen
import com.rememberber.mootool.next.compose.features.color.ColorBoardScreen
import com.rememberber.mootool.next.compose.features.cron.CronScreen
import com.rememberber.mootool.next.compose.features.crypto.CryptoScreen
import com.rememberber.mootool.next.compose.features.diff.TextDiffScreen
import com.rememberber.mootool.next.compose.features.encode.EncodeScreen
import com.rememberber.mootool.next.compose.features.hardware.HardwareScreen
import com.rememberber.mootool.next.compose.features.host.HostScreen
import com.rememberber.mootool.next.compose.features.http.HttpScreen
import com.rememberber.mootool.next.compose.features.image.ImageScreen
import com.rememberber.mootool.next.compose.features.json.JsonScreen
import com.rememberber.mootool.next.compose.features.messageboard.MessageBoardScreen
import com.rememberber.mootool.next.compose.features.translation.TranslationScreen
import com.rememberber.mootool.next.compose.features.net.NetScreen
import com.rememberber.mootool.next.compose.features.pdf.PdfScreen
import com.rememberber.mootool.next.compose.features.protobuf.ProtobufScreen
import com.rememberber.mootool.next.compose.features.quicknote.QuickNoteScreen
import com.rememberber.mootool.next.compose.features.runtime.CodeRunScreen
import com.rememberber.mootool.next.compose.features.qrcode.QrCodeScreen
import com.rememberber.mootool.next.compose.features.regex.RegexScreen
import com.rememberber.mootool.next.compose.features.reformat.ReformatScreen
import com.rememberber.mootool.next.compose.features.time.TimeConvertScreen
import com.rememberber.mootool.next.compose.features.ua.UaParseScreen
import com.rememberber.mootool.next.compose.features.variables.VariablesScreen
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.MooToastHost
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.editor.ImeShortcutGate
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LocalAwtWindow
import com.rememberber.mootool.next.compose.ui.workbench.WindowBoundsPolicy
import com.rememberber.mootool.next.compose.ui.workbench.Workbench
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.ai.McpBootstrap

fun main(args: Array<String>) {
    if (args.contains("--mcp")) {
        McpBootstrap.run(args)
        return
    }
    application {
    DisposableEffect(Unit) {
        ImeShortcutGate.install()
        onDispose { ImeShortcutGate.uninstall() }
    }
    val container = remember { AppContainer.create() }
    val settings by container.settings.collectAsState()
    val detached by container.sessionManager.detached.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    var visible by remember { mutableStateOf(true) }
    var closeDialog by remember { mutableStateOf(false) }
    var mainFocused by remember { mutableStateOf(true) }
    val tray = remember { AppTray() }
    val restored = remember {
        WindowBoundsPolicy.clamp(
            x = settings.workspace.windowX,
            y = settings.workspace.windowY,
            width = settings.workspace.windowWidth,
            height = settings.workspace.windowHeight,
            screens = WindowBoundsPolicy.currentWorkAreas()
        )
    }
    val windowState = rememberWindowState(
        size = DpSize(restored.width.dp, restored.height.dp),
        position = if (restored.x != null && restored.y != null) {
            WindowPosition(restored.x.dp, restored.y.dp)
        } else {
            WindowPosition(Alignment.Center)
        },
        placement = if (settings.general.startMaximized) {
            androidx.compose.ui.window.WindowPlacement.Maximized
        } else {
            androidx.compose.ui.window.WindowPlacement.Floating
        }
    )
    val systemDark = isSystemInDarkTheme()

    fun persistBounds() {
        val x = (windowState.position as? WindowPosition.Absolute)?.x?.value?.toInt()
        val y = (windowState.position as? WindowPosition.Absolute)?.y?.value?.toInt()
        container.updateSettings {
            it.copy(
                workspace = it.workspace.copy(
                    windowX = x,
                    windowY = y,
                    windowWidth = windowState.size.width.value.toInt().coerceAtLeast(960),
                    windowHeight = windowState.size.height.value.toInt().coerceAtLeast(640)
                )
            )
        }
        container.persistWorkspace()
    }

    fun quit() {
        persistBounds()
        tray.remove()
        container.close()
        exitApplication()
    }

    fun showMain() {
        visible = true
    }

    fun trayModel(): AppTrayModel {
        val host = container.sessionManager.hostSession()
        return AppTrayModel(
            openLabel = container.t("app.tray.open"),
            settingsLabel = container.t("app.tray.settings"),
            colorLabel = container.t("app.tray.color"),
            screenshotLabel = container.t("app.tray.screenshot"),
            translationLabel = container.t("app.tray.translation"),
            quitLabel = container.t("app.close.quit"),
            hostProfiles = container.hostProfiles.list().map { profile ->
                AppTrayHostProfile(profile.id, profile.name, profile.id == host.selectedId)
            },
            onOpen = ::showMain,
            onSettings = {
                showMain()
                container.openSettings(true)
            },
            onColorPicker = { TrayDesktopActions.pickColor(container, ::showMain) },
            onScreenshot = { TrayDesktopActions.captureScreenshot(container, ::showMain) },
            onTranslation = {
                showMain()
                container.openTool(ToolId.Translation)
            },
            onHost = { id -> TrayDesktopActions.openHostProfile(container, id, ::showMain) },
            onQuit = ::quit
        )
    }

    fun hideToBackground() {
        closeDialog = false
        persistBounds()
        visible = false
        if (settings.general.trayEnabled && AppTray.supported()) {
            tray.sync(true, trayModel())
        }
    }

    fun handleClose() {
        if (closeDialog) return
        when (settings.general.closeBehavior) {
            "quit" -> quit()
            "hide" -> hideToBackground()
            else -> closeDialog = true
        }
    }

    LaunchedEffect(settings.general.trayEnabled, settings.general.language, revision) {
        tray.sync(settings.general.trayEnabled, trayModel())
    }
    DisposableEffect(Unit) {
        onDispose { tray.remove() }
    }

    MooTheme(
        preference = container.themePreference(),
        systemDark = systemDark,
        interfaceStyle = settings.appearance.interfaceStyle,
        accentColor = settings.appearance.accentColor,
        unifiedBackground = settings.appearance.unifiedBackground,
        fontFamily = settings.appearance.fontFamily
    ) {
        if (visible) {
            Window(
                onCloseRequest = ::handleClose,
                title = ProductIdentity.DISPLAY_NAME,
                state = windowState
            ) {
                DisposableEffect(window) {
                    window.minimumSize = java.awt.Dimension(960, 640)
                    val listener = object : java.awt.event.WindowFocusListener {
                        override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                            mainFocused = true
                            container.setWindowActive(true)
                        }
                        override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                            mainFocused = false
                            container.setWindowActive(false)
                        }
                    }
                    window.addWindowFocusListener(listener)
                    onDispose { window.removeWindowFocusListener(listener) }
                }
                CompositionLocalProvider(LocalAwtWindow provides window) {
                Box(
                    Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                        if (closeDialog && !event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            closeDialog = false
                            true
                        } else {
                            false
                        }
                    }
                ) {
                    Workbench(container, showSidebar = true)
                    MooToastHost(container, windowFocused = mainFocused)
                    if (closeDialog) {
                        MooOverlay(onDismiss = { closeDialog = false }) {
                            Column(
                                Modifier.width(420.dp).mooDialogSurface().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    container.t("app.close.askTitle"),
                                    color = MooTheme.colors.textPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    container.t("app.close.askBody"),
                                    color = MooTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MooButton(container.t("app.close.hide"), prominent = true, onClick = ::hideToBackground)
                                    MooButton(container.t("app.close.quit"), onClick = ::quit)
                                    MooButton(container.t("app.close.cancel"), onClick = { closeDialog = false })
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        if (ToolId.TextDiff in detached) {
            DetachedToolWindow(container, systemDark, ToolId.TextDiff, container.t("app.nav.textDiff"), DpSize(1100.dp, 760.dp)) {
                TextDiffScreen(container, detached = true)
            }
        }

        if (ToolId.Reformat in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Reformat, container.t("app.nav.reformat"), DpSize(1100.dp, 760.dp)) {
                ReformatScreen(container, detached = true)
            }
        }

        if (ToolId.QuickNote in detached) {
            DetachedToolWindow(container, systemDark, ToolId.QuickNote, container.t("app.nav.quickNote"), DpSize(1100.dp, 760.dp)) {
                QuickNoteScreen(container, detached = true)
            }
        }

        if (ToolId.Json in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Json, container.t("app.nav.json"), DpSize(1100.dp, 760.dp)) {
                JsonScreen(container, detached = true)
            }
        }

        if (ToolId.YmlProperties in detached) {
            DetachedToolWindow(container, systemDark, ToolId.YmlProperties, container.t("app.nav.ymlProperties"), DpSize(1100.dp, 760.dp)) {
                ConfigConvertScreen(container, detached = true)
            }
        }

        if (ToolId.Protobuf in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Protobuf, container.t("app.nav.protobuf"), DpSize(1100.dp, 760.dp)) {
                ProtobufScreen(container, detached = true)
            }
        }

        if (ToolId.Java in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Java, container.t("app.nav.java"), DpSize(1100.dp, 760.dp)) {
                CodeRunScreen(container, detached = true)
            }
        }

        if (ToolId.Variables in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Variables, container.t("app.nav.variables"), DpSize(1100.dp, 760.dp)) {
                VariablesScreen(container, detached = true)
            }
        }

        if (ToolId.TimeConvert in detached) {
            DetachedToolWindow(container, systemDark, ToolId.TimeConvert, container.t("app.nav.timeConvert"), DpSize(880.dp, 720.dp)) {
                TimeConvertScreen(container, detached = true, active = true)
            }
        }

        if (ToolId.Calculator in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Calculator, container.t("app.nav.calculator"), DpSize(960.dp, 760.dp)) {
                CalculatorScreen(container, detached = true)
            }
        }

        if (ToolId.Encode in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Encode, container.t("app.nav.encode"), DpSize(1100.dp, 760.dp)) {
                EncodeScreen(container, detached = true)
            }
        }

        if (ToolId.Crypto in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Crypto, container.t("app.nav.crypto"), DpSize(1100.dp, 760.dp)) {
                CryptoScreen(container, detached = true)
            }
        }

        if (ToolId.UaParse in detached) {
            DetachedToolWindow(container, systemDark, ToolId.UaParse, container.t("app.nav.uaParse"), DpSize(960.dp, 720.dp)) {
                UaParseScreen(container, detached = true)
            }
        }

        if (ToolId.Regex in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Regex, container.t("app.nav.regex"), DpSize(1100.dp, 760.dp)) {
                RegexScreen(container, detached = true)
            }
        }

        if (ToolId.Cron in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Cron, container.t("app.nav.cron"), DpSize(1100.dp, 760.dp)) {
                CronScreen(container, detached = true)
            }
        }

        if (ToolId.QrCode in detached) {
            DetachedToolWindow(container, systemDark, ToolId.QrCode, container.t("app.nav.qrCode"), DpSize(1100.dp, 760.dp)) {
                QrCodeScreen(container, detached = true)
            }
        }

        if (ToolId.ColorBoard in detached) {
            DetachedToolWindow(container, systemDark, ToolId.ColorBoard, container.t("app.nav.colorBoard"), DpSize(1100.dp, 760.dp)) {
                ColorBoardScreen(container, detached = true)
            }
        }

        if (ToolId.MessageBoard in detached) {
            DetachedToolWindow(container, systemDark, ToolId.MessageBoard, container.t("app.nav.messageBoard"), DpSize(1100.dp, 760.dp)) {
                MessageBoardScreen(container, detached = true)
            }
        }

        if (ToolId.Translation in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Translation, container.t("app.nav.translation"), DpSize(1100.dp, 760.dp)) {
                TranslationScreen(container, detached = true)
            }
        }

        if (ToolId.Image in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Image, container.t("app.nav.image"), DpSize(1100.dp, 760.dp)) {
                ImageScreen(container, detached = true)
            }
        }

        if (ToolId.Pdf in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Pdf, container.t("app.nav.pdf"), DpSize(1100.dp, 760.dp)) {
                PdfScreen(container, detached = true)
            }
        }

        if (ToolId.Http in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Http, container.t("app.nav.http"), DpSize(1100.dp, 760.dp)) {
                HttpScreen(container, detached = true)
            }
        }

        if (ToolId.Host in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Host, container.t("app.nav.host"), DpSize(1100.dp, 760.dp)) {
                HostScreen(container, detached = true)
            }
        }

        if (ToolId.Net in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Net, container.t("app.nav.net"), DpSize(1100.dp, 760.dp)) {
                NetScreen(container, detached = true)
            }
        }

        if (ToolId.Hardware in detached) {
            DetachedToolWindow(container, systemDark, ToolId.Hardware, container.t("app.nav.hardware"), DpSize(1100.dp, 760.dp)) {
                HardwareScreen(container, detached = true)
            }
        }

    }
    }
}


@Composable
private fun ApplicationScope.DetachedToolWindow(
    container: AppContainer,
    systemDark: Boolean,
    tool: ToolId,
    title: String,
    size: DpSize = DpSize(1100.dp, 760.dp),
    content: @Composable () -> Unit
) {
    Window(
        onCloseRequest = { container.sessionManager.reattach(tool) },
        title = "$title · ${ProductIdentity.DISPLAY_NAME}",
        state = rememberWindowState(size = size)
    ) {
        val focus by container.detachedFocus.collectAsState()
        DisposableEffect(window) {
            window.minimumSize = java.awt.Dimension(960, 640)
            onDispose { }
        }
        LaunchedEffect(focus) {
            val request = focus ?: return@LaunchedEffect
            if (request.toolId != tool) return@LaunchedEffect
            window.isVisible = true
            if (window.extendedState and java.awt.Frame.ICONIFIED != 0) {
                window.extendedState = window.extendedState and java.awt.Frame.ICONIFIED.inv()
            }
            window.toFront()
            window.requestFocus()
        }
        Themed(container, systemDark) {
            var focused by remember { mutableStateOf(window.isActive) }
            DisposableEffect(window) {
                val listener = object : java.awt.event.WindowFocusListener {
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) { focused = true }
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) { focused = false }
                }
                window.addWindowFocusListener(listener)
                onDispose { window.removeWindowFocusListener(listener) }
            }
            CompositionLocalProvider(LocalAwtWindow provides window) {
                Box(Modifier.fillMaxSize()) {
                    content()
                    MooToastHost(container, windowFocused = focused)
                }
            }
        }
    }
}

@Composable
private fun Themed(container: AppContainer, systemDark: Boolean, content: @Composable () -> Unit) {
    val settings by container.settings.collectAsState()
    MooTheme(
        preference = container.themePreference(),
        systemDark = systemDark,
        interfaceStyle = settings.appearance.interfaceStyle,
        accentColor = settings.appearance.accentColor,
        unifiedBackground = settings.appearance.unifiedBackground,
        fontFamily = settings.appearance.fontFamily,
        content = content
    )
}
