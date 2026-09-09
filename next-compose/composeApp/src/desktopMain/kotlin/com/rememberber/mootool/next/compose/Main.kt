package com.rememberber.mootool.next.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ProductIdentity
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
import com.rememberber.mootool.next.compose.features.net.NetScreen
import com.rememberber.mootool.next.compose.features.pdf.PdfScreen
import com.rememberber.mootool.next.compose.features.protobuf.ProtobufScreen
import com.rememberber.mootool.next.compose.features.qrcode.QrCodeScreen
import com.rememberber.mootool.next.compose.features.regex.RegexScreen
import com.rememberber.mootool.next.compose.features.reformat.ReformatScreen
import com.rememberber.mootool.next.compose.features.time.TimeConvertScreen
import com.rememberber.mootool.next.compose.features.ua.UaParseScreen
import com.rememberber.mootool.next.compose.features.variables.VariablesScreen
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.Workbench

fun main() = application {
    val container = remember { AppContainer.create() }
    val settings by container.settings.collectAsState()
    val detached by container.sessionManager.detached.collectAsState()
    var visible by remember { mutableStateOf(true) }
    var closeDialog by remember { mutableStateOf(false) }
    val savedX = settings.workspace.windowX
    val savedY = settings.workspace.windowY
    val windowState = rememberWindowState(
        size = DpSize(
            settings.workspace.windowWidth.coerceAtLeast(1080).dp,
            settings.workspace.windowHeight.coerceAtLeast(720).dp
        ),
        position = if (savedX != null && savedY != null) {
            WindowPosition(savedX.dp, savedY.dp)
        } else {
            WindowPosition(Alignment.Center)
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
                    windowWidth = windowState.size.width.value.toInt().coerceAtLeast(1080),
                    windowHeight = windowState.size.height.value.toInt().coerceAtLeast(720)
                )
            )
        }
        container.persistWorkspace()
    }

    fun quit() {
        persistBounds()
        container.close()
        exitApplication()
    }

    fun handleClose() {
        when (settings.general.closeBehavior) {
            "quit" -> quit()
            "hide" -> {
                persistBounds()
                visible = false
            }
            else -> closeDialog = true
        }
    }

    MooTheme(container.themePreference(), systemDark) {
        if (visible) {
            Window(
                onCloseRequest = ::handleClose,
                title = ProductIdentity.DISPLAY_NAME,
                state = windowState
            ) {
                Workbench(container, showSidebar = true)
            }
        }

        if (ToolId.TextDiff in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.TextDiff) },
                title = "${container.t("app.nav.textDiff")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    TextDiffScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Reformat in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Reformat) },
                title = "${container.t("app.nav.reformat")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    ReformatScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Json in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Json) },
                title = "${container.t("app.nav.json")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    JsonScreen(container, detached = true)
                }
            }
        }

        if (ToolId.YmlProperties in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.YmlProperties) },
                title = "${container.t("app.nav.ymlProperties")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    ConfigConvertScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Protobuf in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Protobuf) },
                title = "${container.t("app.nav.protobuf")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    ProtobufScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Variables in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Variables) },
                title = "${container.t("app.nav.variables")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    VariablesScreen(container, detached = true)
                }
            }
        }

        if (ToolId.TimeConvert in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.TimeConvert) },
                title = "${container.t("app.nav.timeConvert")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(880.dp, 720.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    TimeConvertScreen(container, detached = true, active = true)
                }
            }
        }

        if (ToolId.Calculator in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Calculator) },
                title = "${container.t("app.nav.calculator")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(960.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    CalculatorScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Encode in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Encode) },
                title = "${container.t("app.nav.encode")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    EncodeScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Crypto in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Crypto) },
                title = "${container.t("app.nav.crypto")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    CryptoScreen(container, detached = true)
                }
            }
        }

        if (ToolId.UaParse in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.UaParse) },
                title = "${container.t("app.nav.uaParse")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(960.dp, 720.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    UaParseScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Regex in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Regex) },
                title = "${container.t("app.nav.regex")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    RegexScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Cron in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Cron) },
                title = "${container.t("app.nav.cron")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    CronScreen(container, detached = true)
                }
            }
        }

        if (ToolId.QrCode in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.QrCode) },
                title = "${container.t("app.nav.qrCode")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    QrCodeScreen(container, detached = true)
                }
            }
        }

        if (ToolId.ColorBoard in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.ColorBoard) },
                title = "${container.t("app.nav.colorBoard")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    ColorBoardScreen(container, detached = true)
                }
            }
        }

        if (ToolId.MessageBoard in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.MessageBoard) },
                title = "${container.t("app.nav.messageBoard")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    MessageBoardScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Image in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Image) },
                title = "${container.t("app.nav.image")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    ImageScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Pdf in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Pdf) },
                title = "${container.t("app.nav.pdf")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    PdfScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Http in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Http) },
                title = "${container.t("app.nav.http")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    HttpScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Host in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Host) },
                title = "${container.t("app.nav.host")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    HostScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Net in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Net) },
                title = "${container.t("app.nav.net")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    NetScreen(container, detached = true)
                }
            }
        }

        if (ToolId.Hardware in detached) {
            Window(
                onCloseRequest = { container.sessionManager.reattach(ToolId.Hardware) },
                title = "${container.t("app.nav.hardware")} · ${ProductIdentity.DISPLAY_NAME}",
                state = rememberWindowState(size = DpSize(1100.dp, 760.dp))
            ) {
                MooTheme(container.themePreference(), systemDark) {
                    HardwareScreen(container, detached = true)
                }
            }
        }

        if (closeDialog) {
            DialogWindow(onCloseRequest = { closeDialog = false }, title = container.t("app.close.askTitle")) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(container.t("app.close.askBody"), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton(container.t("app.close.quit"), primary = true, onClick = ::quit)
                        MooButton(container.t("app.close.hide"), onClick = {
                            closeDialog = false
                            persistBounds()
                            visible = false
                        })
                        MooButton(container.t("app.close.cancel"), onClick = { closeDialog = false })
                    }
                }
            }
        }
    }
}
