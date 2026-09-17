package com.rememberber.mootool.next.compose.app

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class HostProfileMenuRevisionTest {
    @Test
    fun notifyHostProfileMenuChangedIncrementsRevision() = runBlocking {
        val directory = Files.createTempDirectory("mootool-host-menu-rev-")
        val container = AppContainer.create(directory.toString())
        try {
            val before = container.hostProfileMenuRevision.first()
            container.notifyHostProfileMenuChanged()
            assertEquals(before + 1, container.hostProfileMenuRevision.first())
        } finally {
            container.close()
            deleteRecursively(directory)
        }
    }

    private fun deleteRecursively(root: Path) {
        if (!root.isDirectory()) {
            Files.deleteIfExists(root)
            return
        }
        Files.walk(root).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}
