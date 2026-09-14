package com.rememberber.mootool.next.compose.domain

import java.security.MessageDigest

enum class VaultChangeKind {
    Ignored,
    Reload,
    Conflict,
    Deleted,
    TreeChanged
}

data class VaultConflictState(
    val relativePath: String,
    val editorText: String,
    val diskText: String?,
    val deleted: Boolean
)

object VaultConflictEngine {
    const val MAX_HASH_BYTES = 8 * 1024 * 1024

    fun sha256Text(text: String): String = sha256(text.toByteArray(Charsets.UTF_8))

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun shouldIgnore(relativePath: String, ignoreAttachments: Boolean): Boolean {
        val path = relativePath.replace('\\', '/').trim('/')
        if (path.isEmpty()) return true
        val name = path.substringAfterLast('/')
        if (name.startsWith(".")) return true
        if (name.endsWith(".tmp", ignoreCase = true)) return true
        if (path == ".git" || path.startsWith(".git/")) return true
        if (ignoreAttachments && (path == "attachments" || path.startsWith("attachments/"))) return true
        return false
    }

    fun decide(
        changedPath: String,
        currentFile: String,
        editorText: String,
        savedText: String,
        diskText: String?,
        expectedOwnHash: String? = null
    ): VaultChangeKind {
        val path = changedPath.replace('\\', '/')
        val current = currentFile.replace('\\', '/')
        if (current.isBlank() || path != current) return VaultChangeKind.TreeChanged
        if (diskText == null) {
            return if (editorText == savedText) VaultChangeKind.Deleted else VaultChangeKind.Conflict
        }
        val diskHash = sha256Text(diskText)
        if (expectedOwnHash != null && diskHash == expectedOwnHash) return VaultChangeKind.Ignored
        if (diskText == editorText) return VaultChangeKind.Ignored
        if (editorText == savedText) return VaultChangeKind.Reload
        if (diskText == savedText) return VaultChangeKind.Ignored
        return VaultChangeKind.Conflict
    }

    fun canOverwrite(savedText: String, diskText: String?, editorText: String): Boolean {
        if (diskText == null) return false
        return diskText == savedText || diskText == editorText
    }

    fun conflictCopyName(relativePath: String, epochMs: Long): String {
        val path = relativePath.replace('\\', '/')
        val slash = path.lastIndexOf('/')
        val directory = if (slash >= 0) path.substring(0, slash + 1) else ""
        val name = if (slash >= 0) path.substring(slash + 1) else path
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        return "$directory$stem.local-$epochMs$ext"
    }
}
