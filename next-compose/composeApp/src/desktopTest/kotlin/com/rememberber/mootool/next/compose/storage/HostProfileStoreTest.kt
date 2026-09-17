package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostProfileStoreTest {
    @Test
    fun listFiltersByProfileIdWithRootLocale() {
        val root = Files.createTempDirectory("mootool-compose-host-store-")
        try {
            val directories = AppDirectories(root, root.resolve("data"), root.resolve("cache"), root.resolve("logs"))
            directories.ensureCreated()
            val store = HostProfileStore(directories)
            val profile = store.save(null, "Local dev", "127.0.0.1 localhost\n")
            val idPrefix = profile.id.take(8)
            val hits = store.list(idPrefix, includeContent = false)
            assertEquals(1, hits.size)
            assertEquals(profile.id, hits.first().id)
            assertEquals("", hits.first().content)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun listIncludeContentToggle() {
        val root = Files.createTempDirectory("mootool-compose-host-store-")
        try {
            val directories = AppDirectories(root, root.resolve("data"), root.resolve("cache"), root.resolve("logs"))
            directories.ensureCreated()
            val store = HostProfileStore(directories)
            store.save(null, "Secret hosts", "10.0.0.1 internal\n")
            assertTrue(store.list("internal", includeContent = true).isNotEmpty())
            assertTrue(store.list("internal", includeContent = false).isEmpty())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
