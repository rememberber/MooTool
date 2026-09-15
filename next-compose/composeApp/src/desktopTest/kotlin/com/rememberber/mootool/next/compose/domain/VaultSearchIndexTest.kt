package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.storage.VaultIndexRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultSearchIndexTest {
    @Test
    fun filtersFromMemoryWithoutNeedingSiblingFiles() {
        val records = listOf(
            VaultIndexRecord(VaultEntry("Work", "Work", true, 0)),
            VaultIndexRecord(
                VaultEntry("Work/api.md", "api.md", false, 12),
                title = "API",
                content = "needle in the body"
            ),
            VaultIndexRecord(
                VaultEntry("readme.md", "readme.md", false, 8),
                title = "Readme",
                content = "unrelated"
            )
        )
        val byBody = VaultSearchIndex.filter(records, "needle", includeContent = true)
        assertEquals(listOf("Work", "Work/api.md"), byBody.map { it.relativePath })
        val withoutBody = VaultSearchIndex.filter(records, "needle", includeContent = false)
        assertEquals(emptyList(), withoutBody.map { it.relativePath })
        val byTitle = VaultSearchIndex.filter(records, "readme", includeContent = false)
        assertEquals(listOf("readme.md"), byTitle.map { it.relativePath })
    }

    @Test
    fun keepsDirectoryWhenItsNameMatches() {
        val records = listOf(
            VaultIndexRecord(VaultEntry("Secrets", "Secrets", true, 0)),
            VaultIndexRecord(VaultEntry("Secrets/a.md", "a.md", false, 1), content = "plain")
        )
        val hits = VaultSearchIndex.filter(records, "secret", includeContent = false)
        assertEquals(listOf("Secrets", "Secrets/a.md"), hits.map { it.relativePath })
    }
}
