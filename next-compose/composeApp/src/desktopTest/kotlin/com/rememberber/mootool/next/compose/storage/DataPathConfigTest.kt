package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataPathConfigTest {
    @Test
    fun emptyKeepsDefaultDataRoot() {
        val bootstrap = AppPaths.resolve(createTempDirectory("mootool-data-path-").toString()).also { it.ensureCreated() }
        val custom = createTempDirectory("custom-data-")
        val effective = DataPathConfig.withEffectiveDataRoot(bootstrap, custom.toString())
        assertEquals(custom.toRealPath(), effective.dataRoot.toRealPath())
    }

    @Test
    fun blankUsesBootstrapDataRoot() {
        val bootstrap = AppPaths.resolve(createTempDirectory("mootool-data-blank-").toString()).also { it.ensureCreated() }
        val effective = DataPathConfig.withEffectiveDataRoot(bootstrap, "")
        assertEquals(bootstrap.dataRoot, effective.dataRoot)
    }

    @Test
    fun relativeRejected() {
        assertNull(DataPathConfig.normalizedCustomDataRoot("relative/data"))
    }
}
