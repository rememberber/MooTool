package com.rememberber.mootool.next.compose.app

sealed interface CommandSearchEntry {
    data class Tool(val definition: ToolDefinition) : CommandSearchEntry
    data class Settings(val target: CommandSettingsTarget) : CommandSearchEntry
}

fun commandSearchEntries(query: String, translate: (String) -> String): List<CommandSearchEntry> {
    val tools = ToolRegistry.search(query, translate).map { CommandSearchEntry.Tool(it) }
    val settings = CommandSearchCatalog.search(query, translate).map { CommandSearchEntry.Settings(it) }
    return tools + settings
}
