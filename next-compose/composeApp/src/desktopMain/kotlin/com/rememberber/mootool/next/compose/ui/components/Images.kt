package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

fun loadClasspathImage(path: String): ImageBitmap? {
    val bytes = Thread.currentThread().contextClassLoader.getResourceAsStream(path)?.readBytes()
        ?: AppContainerHolder.javaClass.classLoader.getResourceAsStream(path)?.readBytes()
        ?: return null
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

private object AppContainerHolder
