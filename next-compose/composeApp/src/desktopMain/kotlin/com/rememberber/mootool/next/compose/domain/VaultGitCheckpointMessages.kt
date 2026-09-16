package com.rememberber.mootool.next.compose.domain

/** Electron `vaultGitCheckpointScheduler.recordActivity` 文案（提交说明来源）。 */
object VaultGitCheckpointMessages {
    const val UPDATE_JSON_SNIPPET = "Update JSON snippet"
    const val CREATE_JSON_FOLDER = "Create JSON Vault folder"
    const val RENAME_JSON_ENTRY = "Rename JSON Vault entry"
    const val MOVE_JSON_ENTRY = "Move JSON Vault entry"
    const val DUPLICATE_JSON_SNIPPET = "Duplicate JSON snippet"
    const val DELETE_JSON_ENTRY = "Delete JSON Vault entry"

    const val CREATE_QUICK_NOTE = "Create Quick Note"
    const val UPDATE_QUICK_NOTE = "Update Quick Note"
    const val CREATE_QUICK_NOTE_FOLDER = "Create Quick Note folder"
    const val RENAME_QUICK_NOTE_ENTRY = "Rename Quick Note entry"
    const val MOVE_QUICK_NOTE_ENTRY = "Move Quick Note entry"
    const val DUPLICATE_QUICK_NOTE = "Duplicate Quick Note"
    const val DELETE_QUICK_NOTE_ENTRY = "Delete Quick Note entry"
    const val ADD_QUICK_NOTE_ATTACHMENT = "Add Quick Note attachment"
    const val PASTE_QUICK_NOTE_ATTACHMENT = "Paste Quick Note attachment"

    fun jsonDialogMode(mode: String): String? = when (mode) {
        "json-folder" -> CREATE_JSON_FOLDER
        "json-move" -> MOVE_JSON_ENTRY
        "json-rename" -> RENAME_JSON_ENTRY
        else -> null
    }

    fun quickNoteDialogMode(mode: String): String? = when (mode) {
        "folder" -> CREATE_QUICK_NOTE_FOLDER
        "rename" -> RENAME_QUICK_NOTE_ENTRY
        "move" -> MOVE_QUICK_NOTE_ENTRY
        "note" -> CREATE_QUICK_NOTE
        else -> null
    }
}
