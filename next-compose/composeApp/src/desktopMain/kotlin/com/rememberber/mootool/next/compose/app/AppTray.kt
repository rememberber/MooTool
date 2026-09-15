package com.rememberber.mootool.next.compose.app

import java.awt.CheckboxMenuItem
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

data class AppTrayHostProfile(
    val id: String,
    val name: String,
    val selected: Boolean
)

data class AppTrayModel(
    val openLabel: String,
    val settingsLabel: String,
    val colorLabel: String,
    val screenshotLabel: String,
    val translationLabel: String,
    val quitLabel: String,
    val hostProfiles: List<AppTrayHostProfile>,
    val onOpen: () -> Unit,
    val onSettings: () -> Unit,
    val onColorPicker: () -> Unit,
    val onScreenshot: () -> Unit,
    val onTranslation: () -> Unit,
    val onHost: (String) -> Unit,
    val onQuit: () -> Unit
)

class AppTray {
    private var icon: TrayIcon? = null

    fun sync(enabled: Boolean, model: AppTrayModel) {
        onEdt {
            if (!enabled || !supported()) removeLocked() else installLocked(model)
        }
    }

    fun remove() {
        onEdt { removeLocked() }
    }

    private fun installLocked(model: AppTrayModel) {
        if (!SystemTray.isSupported()) return
        val tray = SystemTray.getSystemTray()
        val menu = PopupMenu()
        menu.add(item(model.openLabel, model.onOpen))
        menu.add(item(model.settingsLabel, model.onSettings))
        menu.addSeparator()
        menu.add(item(model.colorLabel, model.onColorPicker))
        menu.add(item(model.screenshotLabel, model.onScreenshot))
        menu.add(item(model.translationLabel, model.onTranslation))
        if (model.hostProfiles.isNotEmpty()) {
            menu.addSeparator()
            model.hostProfiles.forEach { profile ->
                val hostItem = CheckboxMenuItem(profile.name, profile.selected)
                hostItem.addItemListener { model.onHost(profile.id) }
                menu.add(hostItem)
            }
        }
        menu.addSeparator()
        menu.add(item(model.quitLabel, model.onQuit))
        val current = icon
        if (current == null) {
            val created = TrayIcon(trayImage(), ProductIdentity.DISPLAY_NAME, menu)
            created.isImageAutoSize = true
            created.addActionListener { model.onOpen() }
            runCatching { tray.add(created) }.onFailure { return }
            icon = created
        } else {
            current.popupMenu = menu
            current.toolTip = ProductIdentity.DISPLAY_NAME
        }
    }

    private fun removeLocked() {
        val current = icon ?: return
        runCatching { SystemTray.getSystemTray().remove(current) }
        icon = null
    }

    private fun item(label: String, action: () -> Unit): MenuItem {
        val item = MenuItem(label)
        item.addActionListener { action() }
        return item
    }

    private fun onEdt(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
    }

    companion object {
        fun supported(): Boolean = runCatching { SystemTray.isSupported() }.getOrDefault(false)

        fun unavailableReason(): String? = if (supported()) null else "SystemTray is not supported on this desktop"

        internal fun trayImage(): BufferedImage {
            val stream = AppTray::class.java.classLoader.getResourceAsStream("brand/mootool-logo.png")
            val source = stream?.use { ImageIO.read(it) }
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                if (source != null) {
                    graphics.drawImage(source, 0, 0, 16, 16, null)
                } else {
                    graphics.color = java.awt.Color(0x3F, 0x6F, 0xAE)
                    graphics.fillRoundRect(1, 1, 14, 14, 6, 6)
                }
            } finally {
                graphics.dispose()
            }
            return image
        }
    }
}
