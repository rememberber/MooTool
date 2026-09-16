@file:OptIn(ExperimentalComposeUiApi::class)

package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File

fun dragDropFiles(event: DragAndDropEvent): List<File> {
    val transferable = when (val native = event.nativeEvent) {
        is DropTargetDragEvent -> native.transferable
        is DropTargetDropEvent -> native.transferable
        else -> return emptyList()
    }
    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return emptyList()
    return runCatching {
        val data = transferable.getTransferData(DataFlavor.javaFileListFlavor)
        (data as? List<*>)?.filterIsInstance<File>()?.filter { it.isFile }.orEmpty()
    }.getOrElse { emptyList() }
}

/**
 * 系统文件拖放到 Compose 区域（对照 Electron 工具页 file-drop 工作流）。
 */
fun Modifier.desktopFileDropTarget(
    enabled: Boolean = true,
    acceptMultiple: Boolean = false,
    onDrop: (List<File>) -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this
    var dragOver by remember { mutableStateOf(false) }
    val colors = MooTheme.colors
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    this
        .then(
            if (dragOver) {
                Modifier.border(1.dp, colors.accent, shape)
            } else {
                Modifier
            }
        )
        .dragAndDropTarget(
            shouldStartDragAndDrop = { event -> dragDropFiles(event).isNotEmpty() },
            target = remember(acceptMultiple, onDrop) {
                object : DragAndDropTarget {
                    override fun onEntered(event: DragAndDropEvent) {
                        if (dragDropFiles(event).isNotEmpty()) dragOver = true
                    }

                    override fun onExited(event: DragAndDropEvent) {
                        dragOver = false
                    }

                    override fun onEnded(event: DragAndDropEvent) {
                        dragOver = false
                    }

                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        dragOver = false
                        val files = dragDropFiles(event)
                        if (files.isEmpty()) return false
                        val picked = if (acceptMultiple) files else listOf(files.first())
                        onDrop(picked)
                        return true
                    }
                }
            }
        )
}
