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
import com.rememberber.mootool.next.compose.features.json.JsonScreen
import com.rememberber.mootool.next.compose.features.time.TimeConvertScreen
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
