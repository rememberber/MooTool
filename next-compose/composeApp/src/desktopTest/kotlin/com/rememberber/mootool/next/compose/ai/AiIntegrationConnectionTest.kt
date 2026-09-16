package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 真 MCP stdio 子进程端到端（对齐 `AiIntegrationService.testConnection` 生产路径）。 */
class AiIntegrationConnectionTest {
    @Test
    fun testConnectionListsToolsAndVerifiesJsonFormat() {
        val productRoot = Files.createTempDirectory("mootool-ai-mcp-conn-")
        val clientHome = Files.createTempDirectory("mootool-ai-mcp-client-")
        try {
            val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
            val settings = SettingsRepository(directories).also { it.load() }
            val database = AppDatabase(directories)
            val container = AppContainer(
                directories = directories,
                settingsRepository = settings,
                database = database,
                history = HistoryRepository(database),
                migrationRows = LegacyMigrationRowRepository(database),
                sessions = SessionStore(database),
            )
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText("""{"version":1}""")
            }
            val service = AiIntegrationService(container, testUserHome = clientHome).apply {
                testLaunch = desktopTestMcpLaunch(accessFile)
            }
            val result = service.testConnection()
            assertEquals("MooTool", result.serverName)
            assertEquals(11, result.tools.size)
            assertEquals(
                MooToolMcpTools.toolNames().toSet() + VaultMcpTools.toolNames.toSet(),
                result.tools.toSet(),
            )
        } finally {
            productRoot.toFile().deleteRecursively()
            clientHome.toFile().deleteRecursively()
        }
    }
}

internal fun desktopTestMcpLaunch(accessFile: Path): McpLaunch {
    val javaHome = System.getProperty("java.home")
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val javaName = if (os.contains("win")) "java.exe" else "java"
    val javaBin = Path.of(javaHome, "bin", javaName).absolutePathString()
    val classpath = System.getProperty("java.class.path")
    return McpLaunch(
        command = javaBin,
        args = listOf(
            "-cp",
            classpath,
            "com.rememberber.mootool.next.compose.MainKt",
            "--mcp",
            "--access-file",
            accessFile.absolutePathString(),
        ),
    )
}
