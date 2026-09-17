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
    fun sqlDialectQueryOpensEditorSettings() {
        val hits = CommandSearchCatalog.search("sql", Translator(AppLanguage.EnUS)::t)
        assertEquals("editor", hits.single().categoryId)
    }

    @Test
    fun javascriptFormatQueryOpensEditorSettings() {
        val hits = CommandSearchCatalog.search("javascript", Translator(AppLanguage.EnUS)::t)
        assertEquals("editor", hits.single().categoryId)
    }

    @Test
    fun exportDirectoryQueryOpensToolsDefaults() {
        val hits = CommandSearchCatalog.search("export", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun translateQueryOpensToolsDefaults() {
        val hits = CommandSearchCatalog.search("translate", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun encodeAndEnvQueriesOpenExpectedSettings() {
        val en = Translator(AppLanguage.EnUS)
        assertEquals("tools", CommandSearchCatalog.search("unicode", en::t).single().categoryId)
        assertEquals("data", CommandSearchCatalog.search("environment", en::t).single().categoryId)
        assertEquals("runtime", CommandSearchCatalog.search("hardware", en::t).single().categoryId)
        assertTrue(CommandSearchCatalog.search("validate", en::t).any { it.categoryId == "vault" })
    }

    @Test
    fun mcpEncodeKeywordsOpenAiSettings() {
        val hits = CommandSearchCatalog.search("json_format", Translator(AppLanguage.EnUS)::t)
        assertEquals("ai", hits.single().categoryId)
    }

    @Test
    fun curlAndPdfQueryOpenNetworkSettings() {
        val en = Translator(AppLanguage.EnUS)
        assertEquals("network", CommandSearchCatalog.search("curl", en::t).single().categoryId)
        assertTrue(CommandSearchCatalog.search("pdf", en::t).any { it.categoryId == "network" })
        assertEquals("network", CommandSearchCatalog.search("httpbin", en::t).single().categoryId)
        assertEquals("tools", CommandSearchCatalog.search("debounce", en::t).single().categoryId)
        assertEquals("network", CommandSearchCatalog.search("ping", en::t).single().categoryId)
        assertEquals("network", CommandSearchCatalog.search("portscan", en::t).single().categoryId)
    }

    @Test
    fun jsonpathQueryOpensVaultOrEditorSettings() {
        val en = Translator(AppLanguage.EnUS)
        val hits = CommandSearchCatalog.search("jsonpath", en::t).map { it.categoryId }.toSet()
        assertTrue("vault" in hits || "editor" in hits)
    }

    @Test
    fun qrQueryOpensToolsDefaults() {
        val hits = CommandSearchCatalog.search("qr", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun rebaseQueryOpensVaultGitSettings() {
        val hits = CommandSearchCatalog.search("rebase", Translator(AppLanguage.EnUS)::t)
        assertEquals("vault", hits.single().categoryId)
    }

    @Test
    fun integrationQueryOpensAiSettings() {
        val hits = CommandSearchCatalog.search("integration", Translator(AppLanguage.EnUS)::t)
        assertEquals("ai", hits.single().categoryId)
    }

    @Test
    fun historyQueryOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("history", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun hiddenToolsQueryOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("hidden", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun upgradeQueryOpensAboutSettings() {
        val hits = CommandSearchCatalog.search("upgrade", Translator(AppLanguage.EnUS)::t)
        assertEquals("about", hits.single().categoryId)
    }

    @Test
    fun classicNavigationQueryOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("classic", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun chineseCheckUpdateQueryOpensAboutSettings() {
        val hits = CommandSearchCatalog.search("检查更新", Translator(AppLanguage.ZhCN)::t)
        assertEquals("about", hits.single().categoryId)
    }

    @Test
    fun screenshotQueryOpensGeneralSettings() {
        val hits = CommandSearchCatalog.search("screenshot", Translator(AppLanguage.EnUS)::t)
        assertEquals("general", hits.single().categoryId)
    }

    @Test
    fun structureQueryOpensVaultSettings() {
        val hits = CommandSearchCatalog.search("structure", Translator(AppLanguage.EnUS)::t)
        assertTrue(hits.any { it.categoryId == "vault" })
    }

    @Test
    fun hostsQueryOpensNetworkSettings() {
        val hits = CommandSearchCatalog.search("hosts", Translator(AppLanguage.EnUS)::t)
        assertEquals("network", hits.single().categoryId)
    }

    @Test
    fun calculatorDefaultsQueryOpensToolsSettings() {
        val hits = CommandSearchCatalog.search("calculator", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun autoDownloadKeywordOpensAboutSettings() {
        val hits = CommandSearchCatalog.search("auto", Translator(AppLanguage.EnUS)::t)
        assertTrue(hits.any { it.categoryId == "about" })
    }

    @Test
    fun oursQueryOpensVaultGitSettings() {
        val hits = CommandSearchCatalog.search("ours", Translator(AppLanguage.EnUS)::t)
        assertEquals("vault", hits.single().categoryId)
    }

    @Test
    fun favoritesQueryOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("favorites", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun detachQueryOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("detach", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun columnEditQueryOpensEditorSettings() {
        val hits = CommandSearchCatalog.search("column", Translator(AppLanguage.EnUS)::t)
        assertEquals("editor", hits.single().categoryId)
    }

    @Test
    fun mergeProductQueryOpensVaultSettings() {
        val hits = CommandSearchCatalog.search("mergeflow", Translator(AppLanguage.EnUS)::t)
        assertEquals("vault", hits.single().categoryId)
    }

    @Test
    fun logicalColumnKeywordOpensEditorSettings() {
        val hits = CommandSearchCatalog.search("logical", Translator(AppLanguage.EnUS)::t)
        assertEquals("editor", hits.single().categoryId)
    }

    @Test
    fun jsonQueryKeywordOpensAiSettings() {
        val hits = CommandSearchCatalog.search("json_query", Translator(AppLanguage.EnUS)::t)
        assertEquals("ai", hits.single().categoryId)
    }

    @Test
    fun detectQueryOpensRuntimeSettings() {
        val hits = CommandSearchCatalog.search("detect", Translator(AppLanguage.EnUS)::t)
        assertEquals("runtime", hits.single().categoryId)
    }

    @Test
    fun protobufWireQueryOpensAiSettings() {
        val hits = CommandSearchCatalog.search("protobuf_wire", Translator(AppLanguage.EnUS)::t)
        assertEquals("ai", hits.single().categoryId)
    }

    @Test
    fun fetchQueryOpensVaultSettings() {
        val hits = CommandSearchCatalog.search("fetch", Translator(AppLanguage.EnUS)::t)
        assertTrue("vault" in hits.map { it.categoryId })
    }

    @Test
    fun cryptoKeywordOpensToolsDefaults() {
        val hits = CommandSearchCatalog.search("aes", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun autocheckKeywordOpensAboutSettings() {
        val hits = CommandSearchCatalog.search("autocheck", Translator(AppLanguage.EnUS)::t)
        assertEquals("about", hits.single().categoryId)
    }

    @Test
    fun yamlQueryOpensToolsSettings() {
        val hits = CommandSearchCatalog.search("yaml", Translator(AppLanguage.EnUS)::t)
        assertEquals("tools", hits.single().categoryId)
    }

    @Test
    fun quicknoteQueryOpensVaultSettings() {
        val hits = CommandSearchCatalog.search("quicknote", Translator(AppLanguage.EnUS)::t)
        assertTrue(hits.any { it.categoryId == "vault" })
    }

    @Test
    fun externalConflictKeywordsOpenVaultSettings() {
        val hits = CommandSearchCatalog.search("savecopy", Translator(AppLanguage.EnUS)::t)
        assertTrue(hits.any { it.categoryId == "vault" })
        val external = CommandSearchCatalog.search("external", Translator(AppLanguage.EnUS)::t)
        assertTrue(external.any { it.categoryId == "vault" })
    }

    @Test
    fun myersQueryOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("myers", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun vaultSearchKeywordOpensVaultSettings() {
        val hits = CommandSearchCatalog.search("vaultsearch", Translator(AppLanguage.EnUS)::t)
        assertTrue(hits.any { it.categoryId == "vault" })
    }

    @Test
    fun hostProfileSearchKeywordOpensNetworkSettings() {
        val hits = CommandSearchCatalog.search("profilesearch", Translator(AppLanguage.EnUS)::t)
        assertEquals("network", hits.single().categoryId)
    }

    @Test
    fun groupLabelKeywordOpensLayoutSettings() {
        val hits = CommandSearchCatalog.search("grouplabel", Translator(AppLanguage.EnUS)::t)
        assertEquals("layout", hits.single().categoryId)
    }

    @Test
    fun catalogCoversEverySettingsNavCategory() {
        val covered = CommandSearchCatalog.targets.map { it.categoryId }.toSet()
        SettingsNavCategory.entries.forEach { category ->
            assertTrue(category.storageId() in covered, "missing command search for ${category.storageId()}")
        }
    }
}
