package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.features.settings.SettingsNavCategory
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandSearchCatalogTest {
    private val zh = Translator(AppLanguage.ZhCN)

    @Test
    fun emptyQueryReturnsNoSettingsHits() {
        assertTrue(CommandSearchCatalog.search("", zh::t).isEmpty())
    }

    @Test
    fun mcpQueryOpensAiSettingsCategory() {
        val hits = CommandSearchCatalog.search("mcp", zh::t)
        assertEquals(1, hits.size)
        assertEquals("ai", hits.first().categoryId)
    }

    @Test
    fun gitQueryOpensVaultSettingsCategory() {
        val hits = CommandSearchCatalog.search("git", zh::t)
        assertTrue(hits.any { it.categoryId == "vault" })
    }

    @Test
    fun commandSearchEntriesMergeToolsAndSettings() {
        val entries = commandSearchEntries("mcp", zh::t)
        assertTrue(entries.any { it is CommandSearchEntry.Settings && it.target.categoryId == "ai" })
        assertTrue(entries.none { it is CommandSearchEntry.Tool && it.definition.id.id == "ai" })
    }

    @Test
    fun proxyQueryOpensNetworkSettings() {
        val hits = CommandSearchCatalog.search("proxy", Translator(AppLanguage.EnUS)::t)
        assertEquals("network", hits.single().categoryId)
    }

    @Test
    fun languageQueryOpensGeneralSettings() {
        val hits = CommandSearchCatalog.search("language", Translator(AppLanguage.EnUS)::t)
        assertEquals("general", hits.single().categoryId)
    }

    @Test
    fun editorFontQueryOpensEditorSettings() {
        val hits = CommandSearchCatalog.search("font", Translator(AppLanguage.EnUS)::t)
        assertTrue(hits.any { it.categoryId == "editor" })
    }

    @Test
    fun exportDirectoryQueryOpensToolsDefaults() {
        val hits = CommandSearchCatalog.search("export", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun integrationQueryOpensAiSettings() {
        val hits = CommandSearchCatalog.search("integration", Translator(AppLanguage.EnUS)::t)
        assertEquals("ai", hits.single().categoryId)
    }

    @Test
    fun catalogCoversEverySettingsNavCategory() {
        val covered = CommandSearchCatalog.targets.map { it.categoryId }.toSet()
        SettingsNavCategory.entries.forEach { category ->
            assertTrue(category.storageId() in covered, "missing command search for ${category.storageId()}")
        }
    }
}
