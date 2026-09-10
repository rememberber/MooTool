package com.rememberber.mootool.next.compose.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun EditorHost(
    buffer: EditorBuffer,
    dark: Boolean,
    fontName: String,
    fontSize: Int,
    wrap: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (dark) Color(0xFF1C1C1E) else Color.White
    remember(buffer, dark, fontName, fontSize, wrap) {
        buffer.applyTheme(dark, fontName, fontSize, wrap)
        buffer
    }
    SwingPanel(
        factory = {
            buffer.scrollPane.apply { this.background = java.awt.Color(background.red, background.green, background.blue) }
        },
        modifier = modifier.fillMaxSize(),
        update = {
            buffer.applyTheme(dark, fontName, fontSize, wrap)
        }
    )
    DisposableEffect(buffer) {
        onDispose { }
    }
}
