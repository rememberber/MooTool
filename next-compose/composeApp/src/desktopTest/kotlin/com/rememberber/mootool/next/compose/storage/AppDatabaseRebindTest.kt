package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppDatabaseRebindTest {
    @Test
    fun rebindSwitchesSqlitePath() {
        val bootstrap = AppPaths.resolve(createTempDirectory("mootool-db-rebind-").toString()).also { it.ensureCreated() }
        val altRoot = createTempDirectory("alt-data-root-")
        val database = AppDatabase(bootstrap)
        val originalFile = bootstrap.databaseFile
        val nextDirs = bootstrap.copy(dataRoot = altRoot)
        database.rebindDataDirectories(nextDirs)
        assertNotEquals(originalFile, nextDirs.databaseFile)
        assertTrue(nextDirs.databaseFile.parent.exists())
        database.close()
    }
}
