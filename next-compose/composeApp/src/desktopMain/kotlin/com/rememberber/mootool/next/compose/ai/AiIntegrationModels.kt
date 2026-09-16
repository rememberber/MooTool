package com.rememberber.mootool.next.compose.ai

import kotlinx.serialization.Serializable

enum class AiClient { Codex, ClaudeCode, Cursor }

enum class AiInstallMode { Mcp, Skill, Both }

enum class AiComponentState { NotInstalled, Installed, NeedsRepair, Conflict }

enum class AiFileAction { Create, Update, Delete, Unchanged }

@Serializable
data class McpLaunch(
    val command: String,
    val args: List<String>,
    val env: Map<String, String> = emptyMap()
)

data class ClaudeMcpLaunch(
    val type: String = "stdio",
    val command: String,
    val args: List<String>,
    val env: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "type" to type,
        "command" to command,
        "args" to args,
        "env" to env
    )
}

data class AiInstallRequest(
    val client: AiClient,
    val mode: AiInstallMode,
    val operation: AiOperation = AiOperation.Install
)

enum class AiOperation { Install, Uninstall }

data class AiInstallPreviewFile(
    val path: String,
    val action: AiFileAction,
    val content: String
)

data class AiInstallPreview(
    val id: String,
    val files: List<AiInstallPreviewFile>,
    val configuration: String
)

data class AiInstallResult(
    val paths: List<String>,
    val backups: List<String>
)

data class AiIntegrationStatus(
    val mcp: AiComponentState,
    val skill: AiComponentState,
    val version: String
)

data class AiDataAccess(
    val notes: String? = null,
    val json: String? = null
)

data class AiDataAccessRequest(
    val notes: Boolean,
    val json: Boolean
)

data class AiConnectionResult(
    val serverName: String,
    val tools: List<String>
)

@Serializable
data class ReceiptMcp(
    val command: String,
    val args: List<String>,
    val env: Map<String, String> = emptyMap(),
    val type: String? = null
)

@Serializable
data class AiInstallReceipt(
    val schemaVersion: Int = 1,
    val appVersion: String,
    val mcp: ReceiptMcp? = null,
    val skills: Map<String, String>? = null
)
