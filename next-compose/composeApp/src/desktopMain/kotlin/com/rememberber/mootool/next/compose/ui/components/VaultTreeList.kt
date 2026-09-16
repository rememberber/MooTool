package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.style.TextOverflow
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.NoteColors
import com.rememberber.mootool.next.compose.domain.VaultMove
import com.rememberber.mootool.next.compose.domain.VaultSort
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme

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
    val openPath: String? = null,
    /** Enter on a directory: toggle expand and select folder (mouse click also calls `onSelect`). */
    val selectPath: String? = null,
)

/** Expand ancestor directories so [selectedPath] is visible (Electron vault `expanded` after create/select). */
fun vaultTreeExpandForMode(expandMode: String, items: List<VaultEntry>): Map<String, Boolean> {
    val directories = items.filter { it.directory }.map { it.relativePath }
    return directories.associateWith { path ->
        when (expandMode) {
            "collapseAll" -> false
            "expandAll" -> true
            else -> !path.contains('/')
        }
    }
}

fun expandVaultPathForSelection(
    selectedPath: String,
    items: List<VaultEntry>,
    expanded: MutableMap<String, Boolean>
) {
    if (selectedPath.isBlank()) return
    val directories = items.filter { it.directory }.map { it.relativePath }.toSet()
    var segment = ""
    for (part in selectedPath.split('/').filter { it.isNotEmpty() }.dropLast(1)) {
        segment = if (segment.isEmpty()) part else "$segment/$part"
        if (segment in directories) expanded[segment] = true
    }
    if (selectedPath in directories) expanded[selectedPath] = true
}

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

enum class VaultContextId { Rename, Move, Duplicate, Delete, Reveal, Export, Info, Git }

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
            VaultTreeKeyResult(
                current.relativePath,
                expanded + (current.relativePath to !open),
                selectPath = current.relativePath,
            )
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
        MooMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, text) ->
                MooMenuItem(onClick = {
                    onChange(id)
                    open = false
                }) {
                    Text(text, fontSize = 11.sp, color = if (id == current) MooTheme.colors.accent else MooTheme.colors.textBody)
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
    contextMenuPath: String = "",
    onContextMenuPathChange: (String) -> Unit = {},
    treeExpanded: Map<String, Boolean> = emptyMap(),
    onTreeExpandedChange: (Map<String, Boolean>) -> Unit = {},
    treeScrollOffset: Int = 0,
    onTreeScrollOffsetChange: (Int) -> Unit = {},
    onContextAction: (VaultEntry, VaultContextId) -> Unit = { _, _ -> },
    onSelect: (VaultEntry) -> Unit = {},
    activeFilePath: String = "",
    activeFileDirty: Boolean = false,
) {
    val colors = MooTheme.colors
    val nodes = remember(items, sort) { buildVaultTree(items, sort) }
    val directoryBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingPath by remember { mutableStateOf<String?>(null) }
    var dropTarget by remember { mutableStateOf<String?>(null) }
    var lastRoot by remember { mutableStateOf(Offset.Zero) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    var focusedPath by remember { mutableStateOf(selectedPath) }
    val contextPath = contextMenuPath.takeIf { it.isNotBlank() }
    val focusRequester = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(selectedPath) {
        if (selectedPath.isNotBlank()) focusedPath = selectedPath
    }

    var lastExpandMode by remember { mutableStateOf(expandMode) }
    androidx.compose.runtime.LaunchedEffect(expandMode, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        if (treeExpanded.isEmpty()) {
            lastExpandMode = expandMode
            onTreeExpandedChange(vaultTreeExpandForMode(expandMode, items))
            return@LaunchedEffect
        }
        if (lastExpandMode != expandMode) {
            lastExpandMode = expandMode
            onTreeExpandedChange(vaultTreeExpandForMode(expandMode, items))
        }
    }
    androidx.compose.runtime.LaunchedEffect(selectedPath, items) {
        val copy = treeExpanded.toMutableMap()
        expandVaultPathForSelection(selectedPath, items, copy)
        val merged = copy.toMap()
        if (merged != treeExpanded) onTreeExpandedChange(merged)
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

    val treeScrollState = remember(treeScrollOffset) { ScrollState(treeScrollOffset) }
    androidx.compose.runtime.LaunchedEffect(treeScrollState) {
        snapshotFlow { treeScrollState.value }.collect { offset ->
            if (offset != treeScrollOffset) onTreeScrollOffsetChange(offset)
        }
    }
    val selectedIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selectedPath, items.size, treeExpanded) {
        if (selectedPath.isBlank()) return@LaunchedEffect
        selectedIntoView.bringIntoView()
    }

    Box(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
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
                val result = applyVaultTreeKey(key, focusedPath, nodes, treeExpanded)
                focusedPath = result.focusedPath
                result.expanded?.let { patch -> if (patch != treeExpanded) onTreeExpandedChange(patch) }
                result.selectPath?.let { path ->
                    items.firstOrNull { it.relativePath == path }?.let(onSelect)
                }
                result.openPath?.let { path ->
                    items.firstOrNull { it.relativePath == path }?.let { entry ->
                        onSelect(entry)
                        onOpen(entry)
                    }
                }
                true
            }
            .onGloballyPositioned { rootBounds = it.boundsInRoot() }
    ) {
        if (nodes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(emptyLabel, color = colors.textMuted, fontSize = 11.sp)
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(treeScrollState)) {
                nodes.forEach { node ->
                    VaultTreeRow(
                        node = node,
                        depth = 0,
                        selectedPath = selectedPath,
                        focusedPath = focusedPath,
                        selectedIntoView = selectedIntoView,
                        treeExpanded = treeExpanded,
                        draggingPath = draggingPath,
                        dropTarget = dropTarget,
                        directoryBounds = directoryBounds,
                        onToggle = { path ->
                            val open = treeExpanded[path] ?: true
                            onTreeExpandedChange(treeExpanded + (path to !open))
                            focusedPath = path
                            focusRequester.requestFocus()
                        },
                        onOpen = { entry ->
                            focusedPath = entry.relativePath
                            focusRequester.requestFocus()
                            onOpen(entry)
                        },
                        onSelect = onSelect,
                        activeFilePath = activeFilePath,
                        activeFileDirty = activeFileDirty,
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
                            onContextMenuPathChange(entry.relativePath)
                            focusRequester.requestFocus()
                        },
                        onContextAction = { entry, id ->
                            onContextMenuPathChange("")
                            onContextAction(entry, id)
                        },
                        onDismissContext = { onContextMenuPathChange("") }
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
    selectedIntoView: BringIntoViewRequester,
    treeExpanded: Map<String, Boolean>,
    draggingPath: String?,
    dropTarget: String?,
    directoryBounds: MutableMap<String, Rect>,
    onToggle: (String) -> Unit,
    onOpen: (VaultEntry) -> Unit,
    onSelect: (VaultEntry) -> Unit,
    activeFilePath: String,
    activeFileDirty: Boolean,
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
    val contextMenuOpen = contextPath == node.entry.relativePath && contextActions.isNotEmpty()
    val contextMenuFirstFocus = remember(node.entry.relativePath) { FocusRequester() }
    LaunchedEffect(contextMenuOpen) {
        if (contextMenuOpen) contextMenuFirstFocus.requestFocus()
    }
    val open = !node.entry.directory || (treeExpanded[node.entry.relativePath] ?: true)
    val selected = node.entry.relativePath == selectedPath || node.entry.relativePath == focusedPath
    val dropping = node.entry.directory && dropTarget == node.entry.relativePath && draggingPath != node.entry.relativePath
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box {
    Row(
        modifier = Modifier
            .then(
                if (node.entry.relativePath == selectedPath) {
                    Modifier.bringIntoViewRequester(selectedIntoView)
                } else {
                    Modifier
                }
            )
            .fillMaxWidth()
            .padding(start = (6 + depth * 12).dp)
            .clip(RoundedCornerShape(5.dp))
            .hoverable(interaction)
            .background(
                when {
                    dropping -> colors.selected
                    selected || hovered || draggingPath == node.entry.relativePath -> colors.control
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
                onSelect(node.entry)
                if (node.entry.directory) onToggle(node.entry.relativePath) else onOpen(node.entry)
            }
            .defaultMinSize(minHeight = 36.dp)
            .padding(start = 8.dp, end = 9.dp, top = 5.dp, bottom = 5.dp),
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
            modifier = Modifier.padding(end = 8.dp)
        )
        if (!node.entry.directory && NoteColors.tintsTree(node.entry.color)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(NoteColors.argb(node.entry.color))))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            node.entry.name,
            color = when {
                !node.entry.directory && NoteColors.tintsTree(node.entry.color) -> Color(NoteColors.argb(node.entry.color))
                else -> colors.textBody
            },
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!node.entry.directory && activeFilePath == node.entry.relativePath && activeFileDirty) {
            Text("•", color = colors.accent, fontSize = 11.sp)
        }
    }
        MooMenu(expanded = contextMenuOpen, onDismissRequest = onDismissContext) {
            visibleVaultContextActions(contextActions, node.entry.directory).forEachIndexed { index, action ->
                MooMenuItem(
                    action.label,
                    modifier = if (index == 0) Modifier.focusRequester(contextMenuFirstFocus) else Modifier,
                ) { onContextAction(node.entry, action.id) }
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
                selectedIntoView = selectedIntoView,
                treeExpanded = treeExpanded,
                draggingPath = draggingPath,
                dropTarget = dropTarget,
                directoryBounds = directoryBounds,
                onToggle = onToggle,
                onOpen = onOpen,
                onSelect = onSelect,
                activeFilePath = activeFilePath,
                activeFileDirty = activeFileDirty,
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

@Composable
fun VaultSelectionFooter(
    path: String,
    dirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    Text(
        text = buildString {
            if (dirty) append("• ")
            append(path)
        },
        color = colors.textMuted,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .height(31.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = colors.borderSoft,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = stroke,
                )
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
fun VaultDeleteConfirmOverlay(
    container: AppContainer,
    relativePath: String?,
    messageKey: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (relativePath.isNullOrBlank()) return
    val colors = MooTheme.colors
    MooOverlay(onDismiss = onDismiss) {
        Column(
            Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                container.t(messageKey, mapOf("name" to relativePath)),
                color = colors.textPrimary,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.delete"), danger = true, onClick = onConfirm)
                MooButton(container.t("common.cancel"), onClick = onDismiss)
            }
        }
    }
}
