package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.ui.components.buildVaultTree
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultSortTest {
    @Test
    fun directoriesStayFirstWhenSortingByModifiedTime() {
        val dirB = VaultEntry("b", "b", true, 0)
        val dirA = VaultEntry("a", "a", true, 0)
        val file = VaultEntry("z.json", "z.json", false, 1, modifiedAt = "2026-09-15T00:00:00Z")
        val tree = buildVaultTree(listOf(file, dirB, dirA), VaultSort.MODIFIED)
        assertEquals(listOf("a", "b", "z.json"), tree.map { it.entry.name })
    }

    @Test
    fun modifiedSortPutsNewestFileFirst() {
        val older = VaultEntry("a.json", "a.json", false, 1, modifiedAt = "2026-01-01T00:00:00Z")
        val newer = VaultEntry("b.json", "b.json", false, 1, modifiedAt = "2026-09-01T00:00:00Z")
        val tree = buildVaultTree(listOf(older, newer), VaultSort.MODIFIED)
        assertEquals(listOf("b.json", "a.json"), tree.map { it.entry.name })
    }

    @Test
    fun createdSortPutsNewestFileFirst() {
        val older = VaultEntry("a.md", "a.md", false, 1, createdAt = "2020-01-01T00:00:00Z")
        val newer = VaultEntry("b.md", "b.md", false, 1, createdAt = "2026-01-01T00:00:00Z")
        val tree = buildVaultTree(listOf(older, newer), VaultSort.CREATED)
        assertEquals(listOf("b.md", "a.md"), tree.map { it.entry.name })
    }

    @Test
    fun nestedSiblingsUseTheSameOrder() {
        val dir = VaultEntry("d", "d", true, 0)
        val old = VaultEntry("d/old.json", "old.json", false, 1, modifiedAt = "2020-01-01T00:00:00Z")
        val newest = VaultEntry("d/new.json", "new.json", false, 1, modifiedAt = "2026-01-01T00:00:00Z")
        val tree = buildVaultTree(listOf(dir, old, newest), VaultSort.MODIFIED)
        assertEquals(listOf("new.json", "old.json"), tree.single().children.map { it.entry.name })
    }

    @Test
    fun normalizeDropsCreatedWhenJsonVaultForbidsIt() {
        assertEquals(VaultSort.NAME, VaultSort.normalize("created", allowCreated = false))
        assertEquals(VaultSort.CREATED, VaultSort.normalize("created", allowCreated = true))
        assertEquals(VaultSort.MODIFIED, VaultSort.normalize(" MODIFIED ", allowCreated = false))
        assertEquals(VaultSort.NAME, VaultSort.normalize("unknown", allowCreated = true))
    }
}
