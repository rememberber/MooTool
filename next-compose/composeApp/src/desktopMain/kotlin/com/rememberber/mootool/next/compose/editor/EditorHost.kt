package com.rememberber.mootool.next.compose.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.rememberber.mootool.next.compose.ui.components.ModalOverlayState
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.theme.editorPalette
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor
import java.io.File

@Composable
fun EditorHost(
    buffer: EditorBuffer,
    dark: Boolean,
    fontName: String,
    fontSize: Int,
    wrap: Boolean,
    modifier: Modifier = Modifier,
    columnEditing: Boolean = false,
    columnDragWithoutAlt: Boolean = false,
    lineSpacing: Double = 1.0,
    shortcuts: EditorAppShortcuts = EditorAppShortcuts(),
    onFilesDropped: ((List<File>) -> Boolean)? = null
) {
    val colors = MooTheme.colors
    val palette = editorPalette(dark, colors)
    remember(buffer, dark, fontName, fontSize, wrap, columnEditing, columnDragWithoutAlt, lineSpacing, onFilesDropped, colors.styleId, colors.workspace) {
        buffer.applyTheme(dark, fontName, fontSize, wrap, palette, lineSpacing)
        buffer.setColumnEditing(columnEditing, columnDragWithoutAlt)
        buffer.bindAppShortcuts(shortcuts)
        if (onFilesDropped != null) buffer.installFileDrop(onFilesDropped)
        buffer
    }
    if (ModalOverlayState.count > 0) {
        Box(modifier.fillMaxSize().background(colors.workspace))
    } else {
        SwingPanel(
            factory = {
                buffer.scrollPane.apply { this.background = palette.background.toAwtColor() }
            },
            modifier = modifier.fillMaxSize(),
            update = {
                buffer.applyTheme(dark, fontName, fontSize, wrap, palette, lineSpacing)
                buffer.setColumnEditing(columnEditing, columnDragWithoutAlt)
                buffer.bindAppShortcuts(shortcuts)
            }
        )
    }
    DisposableEffect(buffer) {
        onDispose { }
    }
}
