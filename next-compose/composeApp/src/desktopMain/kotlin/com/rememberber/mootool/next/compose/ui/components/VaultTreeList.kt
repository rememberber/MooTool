package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.NoteColors
import com.rememberber.mootool.next.compose.domain.VaultMove
import com.rememberber.mootool.next.compose.domain.VaultSort
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

data class VaultTreeNode(
    val entry: VaultEntry,
    val children: List<VaultTreeNode> = emptyList()
)

fun buildVaultTree(entries: List<VaultEntry>, sort: String = VaultSort.NAME): List<VaultTreeNode> {
    val directories = entries.filter { it.directory }.map { it.relativePath }.toSet()
    val byParent = linkedMapOf<String, MutableList<VaultEntry>>()
    for (entry in entries) {
        val parent = VaultMove.parentDirectory(entry.relativePath)
        val key = if (parent.isEmpty() || parent in directories) parent else ""
        byParent.getOrPut(key) { mutableListOf() }.add(entry)
    }
    fun childrenOf(parent: String): List<VaultTreeNode> =
        (byParent[parent] ?: emptyList())
            .sortedWith { left, right -> VaultSort.compare(left, right, sort) }
            .map { entry ->
                VaultTreeNode(entry, if (entry.directory) childrenOf(entry.relativePath) else emptyList())
            }
    return childrenOf("")
}

data class VaultTreeKeyResult(
    val focusedPath: String,
    val expanded: Map<String, Boolean>? = null,
    val openPath: String? = null
)

fun visibleVaultEntries(nodes: List<VaultTreeNode>, expanded: Map<String, Boolean>): List<VaultEntry> {
    val visible = mutableListOf<VaultEntry>()
    fun walk(node: VaultTreeNode) {
        visible += node.entry
        val open = !node.entry.directory || (expanded[node.entry.relativePath] ?: true)
        if (node.entry.directory && open) node.children.forEach(::walk)
    }
    nodes.forEach(::walk)
    return visible
}

enum class VaultContextId { Rename, Move, Duplicate, Delete, Reveal, Export, Git }

data class VaultContextAction(
    val id: VaultContextId,
    val label: String,
    val filesOnly: Boolean = false
)

fun visibleVaultContextActions(actions: List<VaultContextAction>, directory: Boolean): List<VaultContextAction> =
    actions.filter { !it.filesOnly || !directory }

fun applyVaultTreeKey(
    key: String,
    focusedPath: String,
    nodes: List<VaultTreeNode>,
    expanded: Map<String, Boolean>
): VaultTreeKeyResult {
    val visible = visibleVaultEntries(nodes, expanded)
    if (visible.isEmpty()) return VaultTreeKeyResult(focusedPath)
    val index = visible.indexOfFirst { it.relativePath == focusedPath }.let { if (it < 0) 0 else it }
    val current = visible[index]
    return when (key) {
        "up" -> VaultTreeKeyResult(visible[maxOf(0, index - 1)].relativePath)
        "down" -> VaultTreeKeyResult(visible[minOf(visible.lastIndex, index + 1)].relativePath)
        "home" -> VaultTreeKeyResult(visible.first().relativePath)
        "end" -> VaultTreeKeyResult(visible.last().relativePath)
        "left" -> {
            if (current.directory && (expanded[current.relativePath] ?: true)) {
                VaultTreeKeyResult(current.relativePath, expanded + (current.relativePath to false))
            } else {
                val parent = VaultMove.parentDirectory(current.relativePath)
                VaultTreeKeyResult(parent.ifBlank { current.relativePath })
            }
        }
        "right" -> {
            if (current.directory && !(expanded[current.relativePath] ?: true)) {
                VaultTreeKeyResult(current.relativePath, expanded + (current.relativePath to true))
            } else {
                val next = visible.getOrNull(index + 1)
                if (next != null && next.relativePath.startsWith("${current.relativePath}/")) {
                    VaultTreeKeyResult(next.relativePath)
                } else {
                    VaultTreeKeyResult(current.relativePath)
                }
            }
        }
        "enter" -> if (current.directory) {
            val open = expanded[current.relativePath] ?: true
            VaultTreeKeyResult(current.relativePath, expanded + (current.relativePath to !open))
        } else {
            VaultTreeKeyResult(current.relativePath, openPath = current.relativePath)
        }
        else -> VaultTreeKeyResult(current.relativePath)
    }
}

@Composable
fun VaultSortMenu(
    current: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == current }?.second ?: current
    Box {
        MooButton(label, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(onClick = {
                    onChange(id)
                    open = false
                }) {
                    Text(text, color = if (id == current) MooTheme.colors.accent else MooTheme.colors.textPrimary)
                }
            }
        }
    }
}

@Composable
fun VaultTreeList(
    items: List<VaultEntry>,
    selectedPath: String,
    emptyLabel: String,
    onOpen: (VaultEntry) -> Unit,
    onMove: (from: String, toDirectory: String) -> Unit,
    modifier: Modifier = Modifier,
    expandMode: String = "smart",
    sort: String = VaultSort.NAME,
    contextActions: List<VaultContextAction> = emptyList(),
    onContextAction: (VaultEntry, VaultContextId) -> Unit = { _, _ -> }
) {
    val colors = MooTheme.colors
    val nodes = remember(items, sort) { buildVaultTree(items, sort) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val directoryBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingPath by remember { mutableStateOf<String?>(null) }
    var dropTarget by remember { mutableStateOf<String?>(null) }
    var lastRoot by remember { mutableStateOf(Offset.Zero) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    var focusedPath by remember { mutableStateOf(selectedPath) }
    var contextPath by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(selectedPath) {
        if (selectedPath.isNotBlank()) focusedPath = selectedPath
    }

    androidx.compose.runtime.LaunchedEffect(expandMode, items) {
        val directories = items.filter { it.directory }.map { it.relativePath }
        expanded.clear()
        directories.forEach { path ->
            expanded[path] = when (expandMode) {
                "collapseAll" -> false
                "expandAll" -> true
                else -> !path.contains('/')
            }
        }
    }

    fun hitDirectory(position: Offset): String {
        val matches = directoryBounds.entries.filter { it.value.contains(position) }
        if (matches.isEmpty()) return if (rootBounds.contains(position)) "" else ""
        return matches.minBy { it.value.width * it.value.height }.key
    }

    fun finishDrag() {
        val source = draggingPath
        val target = dropTarget ?: hitDirectory(lastRoot)
        draggingPath = null
        dropTarget = null
        if (source != null && VaultMove.canMoveToDirectory(source, target)) onMove(source, target)
    }

    Box(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val key = when (event.key) {
                    Key.DirectionUp -> "up"
                    Key.DirectionDown -> "down"
                    Key.DirectionLeft -> "left"
                    Key.DirectionRight -> "right"
                    Key.MoveHome -> "home"
                    Key.MoveEnd -> "end"
                    Key.Enter, Key.NumPadEnter -> "enter"
                    else -> return@onPreviewKeyEvent false
                }
                val result = applyVaultTreeKey(key, focusedPath, nodes, expanded.toMap())
                focusedPath = result.focusedPath
                result.expanded?.forEach { (path, open) -> expanded[path] = open }
                result.openPath?.let { path ->
                    items.firstOrNull { it.relativePath == path }?.let(onOpen)
                }
                true
            }
            .onGloballyPositioned { rootBounds = it.boundsInRoot() }
    ) {
        if (nodes.isEmpty()) {
            Text(emptyLabel, color = colors.textSecondary, fontSize = 12.sp)
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                nodes.forEach { node ->
                    VaultTreeRow(
                        node = node,
                        depth = 0,
                        selectedPath = selectedPath,
                        focusedPath = focusedPath,
                        expanded = expanded,
                        draggingPath = draggingPath,
                        dropTarget = dropTarget,
                        directoryBounds = directoryBounds,
                        onToggle = {
                            path ->
                            expanded[path] = !(expanded[path] ?: true)
                            focusedPath = path
                            focusRequester.requestFocus()
                        },
                        onOpen = { entry ->
                            focusedPath = entry.relativePath
                            focusRequester.requestFocus()
                            onOpen(entry)
                        },
                        onDragStart = { path, position ->
                            draggingPath = path
                            lastRoot = position
                            dropTarget = hitDirectory(position)
                        },
                        onDrag = { position ->
                            lastRoot = position
                            dropTarget = hitDirectory(position)
                        },
                        onDragEnd = { finishDrag() },
                        contextPath = contextPath,
                        contextActions = contextActions,
                        onContext = { entry ->
                            focusedPath = entry.relativePath
                            contextPath = entry.relativePath
                            focusRequester.requestFocus()
                        },
                        onContextAction = { entry, id ->
                            contextPath = null
                            onContextAction(entry, id)
                        },
                        onDismissContext = { contextPath = null }
                    )
                }
            }
        }
        if (draggingPath != null) {
            Text(
                when (val target = dropTarget) {
                    "" -> "→ /"
                    null -> draggingPath.orEmpty()
                    else -> "→ $target"
                },
                color = colors.accent,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
            )
        }
    }
}

@Composable
private fun VaultTreeRow(
    node: VaultTreeNode,
    depth: Int,
    selectedPath: String,
    focusedPath: String,
    expanded: MutableMap<String, Boolean>,
    draggingPath: String?,
    dropTarget: String?,
    directoryBounds: MutableMap<String, Rect>,
    onToggle: (String) -> Unit,
    onOpen: (VaultEntry) -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    contextPath: String?,
    contextActions: List<VaultContextAction>,
    onContext: (VaultEntry) -> Unit,
    onContextAction: (VaultEntry, VaultContextId) -> Unit,
    onDismissContext: () -> Unit
) {
    val colors = MooTheme.colors
    val open = !node.entry.directory || (expanded[node.entry.relativePath] ?: true)
    val selected = node.entry.relativePath == selectedPath || node.entry.relativePath == focusedPath
    val dropping = node.entry.directory && dropTarget == node.entry.relativePath && draggingPath != node.entry.relativePath
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (6 + depth * 12).dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    dropping -> colors.selected
                    selected -> colors.selected.copy(alpha = 0.7f)
                    draggingPath == node.entry.relativePath -> colors.control
                    else -> colors.sidebar.copy(alpha = 0f)
                }
            )
            .onGloballyPositioned { layout ->
                coordinates = layout
                if (node.entry.directory) directoryBounds[node.entry.relativePath] = layout.boundsInRoot()
            }
            .pointerInput(node.entry.relativePath) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            onContext(node.entry)
                        }
                    }
                }
            }
            .pointerInput(node.entry.relativePath) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onDragStart(node.entry.relativePath, coordinates?.localToRoot(offset) ?: offset)
                    },
                    onDrag = { change, _ ->
                        onDrag(coordinates?.localToRoot(change.position) ?: change.position)
                        change.consume()
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .clickable {
                if (node.entry.directory) onToggle(node.entry.relativePath) else onOpen(node.entry)
            }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when {
                !node.entry.directory -> " "
                open -> "▾"
                else -> "▸"
            },
            color = colors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
        if (!node.entry.directory && NoteColors.tintsTree(node.entry.color)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(NoteColors.argb(node.entry.color))))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            node.entry.name,
            color = when {
                selected -> colors.accent
                !node.entry.directory && NoteColors.tintsTree(node.entry.color) -> Color(NoteColors.argb(node.entry.color))
                else -> colors.textPrimary
            },
            fontSize = 12.sp
        )
    }
        DropdownMenu(expanded = contextPath == node.entry.relativePath && contextActions.isNotEmpty(), onDismissRequest = onDismissContext) {
            visibleVaultContextActions(contextActions, node.entry.directory).forEach { action ->
                DropdownMenuItem(onClick = { onContextAction(node.entry, action.id) }) {
                    Text(action.label)
                }
            }
        }
    }
    if (node.entry.directory && open) {
        node.children.forEach { child ->
            VaultTreeRow(
                node = child,
                depth = depth + 1,
                selectedPath = selectedPath,
                focusedPath = focusedPath,
                expanded = expanded,
                draggingPath = draggingPath,
                dropTarget = dropTarget,
                directoryBounds = directoryBounds,
                onToggle = onToggle,
                onOpen = onOpen,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                contextPath = contextPath,
                contextActions = contextActions,
                onContext = onContext,
                onContextAction = onContextAction,
                onDismissContext = onDismissContext
            )
        }
    }
}
