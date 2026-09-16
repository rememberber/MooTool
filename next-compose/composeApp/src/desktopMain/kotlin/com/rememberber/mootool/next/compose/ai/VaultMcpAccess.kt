package com.rememberber.mootool.next.compose.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

enum class VaultMcpKind { Notes, Json }

@Serializable
data class VaultMcpAccessFile(
    val version: Int = 1,
    val notes: String? = null,
    val json: String? = null
)

object VaultMcpAccess {
    /** 对齐 Electron `accessSchema` strictObject，未知字段拒绝。 */
    private val strictJson = Json { ignoreUnknownKeys = false }

    fun read(accessFile: Path?): VaultMcpAccessFile {
        if (accessFile == null || !accessFile.exists()) return VaultMcpAccessFile()
        if (!accessFile.isRegularFile() || Files.isSymbolicLink(accessFile)) {
            throw IllegalArgumentException("Invalid MooTool access settings")
        }
        if (Files.size(accessFile) > 16_000) throw IllegalArgumentException("Invalid MooTool access settings")
        return try {
            val parsed = strictJson.decodeFromString<VaultMcpAccessFile>(accessFile.readText(Charsets.UTF_8))
            if (parsed.version != 1) {
                throw IllegalArgumentException("Invalid MooTool access settings")
            }
            parsed
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (_: Exception) {
            throw IllegalArgumentException("Cannot read MooTool access settings. Check AI integration settings.")
        }
    }
}
