package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.domain.ColorEngine
import com.rememberber.mootool.next.compose.domain.ImageEngine
import com.rememberber.mootool.next.compose.domain.ScreenCaptureAccess
import com.rememberber.mootool.next.compose.domain.ScreenColorPicker
import com.rememberber.mootool.next.compose.domain.ScreenColorSampler
import com.rememberber.mootool.next.compose.domain.ScreenPickerCopy
import com.rememberber.mootool.next.compose.domain.ScreenRegionPicker
import com.rememberber.mootool.next.compose.model.ToolId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

object TrayDesktopActions {
    fun pickColor(container: AppContainer, showWindow: () -> Unit) {
        showWindow()
        container.openTool(ToolId.ColorBoard)
        val session = container.sessionManager.colorSession()
        session.picking = true
        session.error = ""
        session.notice = container.t("common.processing")
        container.sessionManager.bump()
        val copy = ScreenPickerCopy(container.t("color.pickerOverlayHint"), container.t("color.pickerOverlayKeys"))
        container.scope.launch(Dispatchers.Default) {
            val capture = runCatching { ScreenColorSampler.captureAllScreens() }
            withContext(Dispatchers.Swing) {
                capture.onSuccess { image ->
                    ScreenColorPicker.show(
                        image,
                        copy,
                        onPicked = { color ->
                            session.picking = false
                            session.primary = color
                            session.code = ColorEngine.formatColor(color, session.format)
                            session.notice = container.t("color.picker")
                            session.error = ""
                            container.sessionManager.bump()
                        },
                        onCancel = {
                            session.picking = false
                            session.notice = container.t("color.pickerCancelled")
                            session.error = ""
                            container.sessionManager.bump()
                        }
                    )
                }.onFailure { error ->
                    session.picking = false
                    session.notice = ""
                    session.error = ScreenCaptureAccess.userMessage({ container.t(it) }, error)
                    container.sessionManager.bump()
                }
            }
        }
    }

    fun captureScreenshot(container: AppContainer, showWindow: () -> Unit) {
        showWindow()
        container.openTool(ToolId.Image)
        val session = container.sessionManager.imageSession()
        session.error = ""
        session.notice = container.t("common.processing")
        container.sessionManager.bump()
        container.scope.launch(Dispatchers.Default) {
            val capture = runCatching { ScreenColorSampler.captureAllScreens() }
            withContext(Dispatchers.Swing) {
                capture.onSuccess { image ->
                    ScreenRegionPicker.show(
                        image,
                        container.t("image.captureHint"),
                        onPicked = { region ->
                            runCatching {
                                val saved = container.imageLibrary.save(ImageEngine.timestampName("Screenshot"), region, false)
                                session.currentName = saved.name
                                session.selectedNames = listOf(saved.name)
                                session.notice = container.t("image.imported")
                                session.error = ""
                            }.onFailure {
                                session.error = it.message.orEmpty()
                            }
                            container.sessionManager.bump()
                        },
                        onCancel = {
                            session.notice = container.t("image.captureCancelled")
                            session.error = ""
                            container.sessionManager.bump()
                        }
                    )
                }.onFailure { error ->
                    session.notice = ""
                    session.error = ScreenCaptureAccess.userMessage({ container.t(it) }, error)
                    container.sessionManager.bump()
                }
            }
        }
    }

    fun openHostProfile(container: AppContainer, id: String, showWindow: () -> Unit) {
        showWindow()
        val profile = container.hostProfiles.get(id) ?: return
        val session = container.sessionManager.hostSession()
        session.selectedId = profile.id
        session.name = profile.name
        session.content = profile.content
        session.savedName = profile.name
        session.savedContent = profile.content
        session.applyConfirm = false
        container.openTool(ToolId.Host)
        container.sessionManager.bump()
    }
}
