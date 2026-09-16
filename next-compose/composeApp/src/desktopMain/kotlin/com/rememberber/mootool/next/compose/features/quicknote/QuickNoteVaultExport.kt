package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal fun quickNoteDirty(session: QuickNoteSession): Boolean {
    if (session.currentFile.isBlank()) return false
    return session.editor.text != session.savedText || session.metadata != session.savedMetadata
}

/** Electron `exportNote` writes plain note body (not vault frontmatter) to a user path. */
internal fun quickNoteExportBody(
    session: QuickNoteSession,
    vault: NoteVault,
    relativePath: String,
): String {
    if (relativePath == session.currentFile) {
        return session.editor.text
    }
    return vault.readNote(relativePath).content
}

internal fun quickNoteExportDefaultFileName(relativePath: String, metadataTitle: String?): String {
    val title = metadataTitle?.trim()?.takeIf { it.isNotEmpty() }
    if (title != null) {
        val safe = title.replace('/', '-').replace('\\', '-')
        return "$safe.txt"
    }
    val leaf = relativePath.substringAfterLast('/')
    return leaf.ifBlank { "note.txt" }
}

internal fun exportQuickNoteVaultEntryToPath(
    session: QuickNoteSession,
    vault: NoteVault,
    relativePath: String,
    destination: Path,
) {
    Files.writeString(
        destination,
        quickNoteExportBody(session, vault, relativePath),
        StandardCharsets.UTF_8,
    )
}
