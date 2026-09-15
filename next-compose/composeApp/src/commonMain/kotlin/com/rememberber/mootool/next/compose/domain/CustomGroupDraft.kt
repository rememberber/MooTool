package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.CustomToolGroup

object CustomGroupDraft {
    fun copyOf(groups: List<CustomToolGroup>): List<CustomToolGroup> =
        groups.map { it.copy(toolIds = it.toolIds.toList()) }

    fun invalid(groups: List<CustomToolGroup>): CustomToolGroup? =
        groups.find { it.name.trim().isEmpty() || it.toolIds.isEmpty() }

    fun persistable(groups: List<CustomToolGroup>): List<CustomToolGroup> =
        groups.map { it.copy(name = it.name.trim(), toolIds = it.toolIds.toList()) }
}
