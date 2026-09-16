package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object McpLaunchResolver {
    fun accessFile(directories: AppDirectories): Path =
        directories.configRoot.resolve("ai-integration").resolve("access.json")

    fun stateDirectory(directories: AppDirectories): Path =
        directories.configRoot.resolve("ai-integration")

    fun clientLaunch(accessFile: Path): McpLaunch {
        val command = ProcessHandle.current().info().command().orElse(javaBinary())
        val args = listOf("--mcp", "--access-file", accessFile.absolutePathString())
        return McpLaunch(command = command, args = args)
    }

    fun claudeDesired(launch: McpLaunch): ClaudeMcpLaunch =
        ClaudeMcpLaunch(command = launch.command, args = launch.args, env = launch.env)

    private fun javaBinary(): String {
        val home = System.getProperty("java.home")
        val name = if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) "java.exe" else "java"
        return Path.of(home, "bin", name).absolutePathString()
    }
}
