package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.domain.CustomGroupDraft
import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.util.UUID

@Composable
fun CustomGroupManager(container: AppContainer) {
    val colors = MooTheme.colors
    val initial = container.settings.value.layout.customGroups
    var groups by remember { mutableStateOf(CustomGroupDraft.copyOf(initial)) }
    var selectedId by remember { mutableStateOf(groups.firstOrNull()?.id) }
    var pendingDelete by remember { mutableStateOf(false) }
    val selected = groups.find { it.id == selectedId }
    val invalid = CustomGroupDraft.invalid(groups)
    Box(Modifier.fillMaxSize()) {
    MooOverlay(onDismiss = { container.setGroupManagerOpen(false) }) {
        Column(
            Modifier.width(720.dp).heightIn(max = 560.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MooPageTitle(container.t("app.group.manage.title"))
            Row(Modifier.fillMaxWidth().height(360.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier.width(200.dp).fillMaxHeight().clip(RoundedCornerShape(MooTheme.dimens.radius))
                        .background(colors.surfaceSubtle).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MooButton(container.t("app.group.manage.new"), onClick = {
                        val next = CustomToolGroup(
                            id = UUID.randomUUID().toString(),
                            name = container.t("app.group.manage.defaultName", mapOf("number" to (groups.size + 1).toString())),
                            toolIds = emptyList()
                        )
                        groups = groups + next
                        selectedId = next.id
                    })
                    if (groups.isEmpty()) {
                        Text(container.t("app.group.manage.empty"), color = colors.textSecondary, fontSize = 12.sp)
                    } else {
                        groups.forEach { group ->
                            val active = group.id == selectedId
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                    .background(if (active) colors.selected else Color.Transparent)
                                    .mooFocusClickable { selectedId = group.id }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    group.name.ifBlank { container.t("app.group.manage.name") },
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(group.toolIds.size.toString(), color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    val scroll = rememberScrollState()
                    Column(Modifier.fillMaxHeight().verticalScroll(scroll).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selected == null) {
                            Text(container.t("app.group.manage.empty"), color = colors.textSecondary, fontSize = 13.sp)
                        } else {
                            Text(container.t("app.group.manage.name"), color = colors.textSecondary, fontSize = 12.sp)
                            MooTextField(
                                selected.name,
                                { value ->
                                    groups = groups.map { if (it.id == selected.id) it.copy(name = value) else it }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (selected.name.isBlank()) {
                                Text(container.t("app.group.manage.nameRequired"), color = colors.danger, fontSize = 12.sp)
                            }
                            Text(container.t("app.group.manage.tools"), color = colors.textSecondary, fontSize = 12.sp)
                            ToolRegistry.groups.forEach { grouping ->
                                Text(container.t(grouping.titleKey), color = colors.textSecondary, fontSize = 11.sp)
                                grouping.toolIds.filter { it != ToolId.Mootool }.forEach { toolId ->
                                    val tool = ToolRegistry.byId.getValue(toolId)
                                    val checked = toolId.id in selected.toolIds
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(container.t(tool.titleKey), color = colors.textPrimary, fontSize = 13.sp)
                                        MooSwitch(checked) {
                                            groups = groups.map { group ->
                                                if (group.id != selected.id) group
                                                else group.copy(
                                                    toolIds = if (checked) group.toolIds - toolId.id else group.toolIds + toolId.id
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (selected.toolIds.isEmpty()) {
                                Text(container.t("app.group.manage.toolRequired"), color = colors.danger, fontSize = 12.sp)
                            }
                        }
                    }
                    VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MooButton(container.t("app.group.manage.delete"), enabled = selected != null, onClick = { pendingDelete = true })
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                MooButton(container.t("common.cancel"), onClick = { container.setGroupManagerOpen(false) })
                MooButton(
                    container.t("common.save"),
                    prominent = true,
                    enabled = invalid == null,
                    onClick = {
                        if (invalid != null) return@MooButton
                        container.updateSettings { current ->
                            current.copy(layout = current.layout.copy(customGroups = CustomGroupDraft.persistable(groups)))
                        }
                        container.setGroupManagerOpen(false)
                    }
                )
            }
        }
    }
    if (pendingDelete && selected != null) {
        MooOverlay(onDismiss = { pendingDelete = false }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    container.t("app.group.manage.deleteConfirm", mapOf("name" to selected.name)),
                    color = colors.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("app.group.manage.delete"), danger = true, onClick = {
                        val id = selected.id
                        val index = groups.indexOfFirst { it.id == id }
                        val next = groups.filterNot { it.id == id }
                        groups = next
                        selectedId = next.getOrNull(index.coerceAtMost(next.lastIndex))?.id
                        pendingDelete = false
                    })
                    MooButton(container.t("common.cancel"), onClick = { pendingDelete = false })
                }
            }
        }
    }
}
}
