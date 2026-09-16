package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.model.ToolId

/** Electron `navigationToolIds` equivalent for settings layout visibility. */
object NavigationToolVisibility {
    val navigationToolIds: List<String> =
        ToolRegistry.groups.flatMap { group -> group.toolIds.map { it.id } }

    fun showAll(): List<String> = emptyList()

    fun hideAll(): List<String> = navigationToolIds

    /** Mirrors Electron `normalizeNavigationToolIds`: drop home, unknown ids, and duplicates. */
    fun normalizeHiddenNavigationToolIds(raw: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        return raw.filter { id ->
            id != ToolId.Mootool.id && ToolId.fromId(id) != null && seen.add(id)
        }
    }

    fun visibleNavigationToolCount(hiddenNavigationToolIds: Collection<String>): Int {
        val hidden = hiddenNavigationToolIds.toSet()
        return navigationToolIds.count { it !in hidden }
    }
}
