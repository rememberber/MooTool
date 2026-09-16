package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class VaultGitCheckpointMessagesTest {
    @Test
    fun mapsJsonVaultDialogModes() {
        assertEquals(VaultGitCheckpointMessages.CREATE_JSON_FOLDER, VaultGitCheckpointMessages.jsonDialogMode("json-folder"))
        assertEquals(VaultGitCheckpointMessages.MOVE_JSON_ENTRY, VaultGitCheckpointMessages.jsonDialogMode("json-move"))
        assertEquals(VaultGitCheckpointMessages.RENAME_JSON_ENTRY, VaultGitCheckpointMessages.jsonDialogMode("json-rename"))
    }

    @Test
    fun mapsQuickNoteDialogModes() {
        assertEquals(VaultGitCheckpointMessages.CREATE_QUICK_NOTE_FOLDER, VaultGitCheckpointMessages.quickNoteDialogMode("folder"))
        assertEquals(VaultGitCheckpointMessages.CREATE_QUICK_NOTE, VaultGitCheckpointMessages.quickNoteDialogMode("note"))
    }
}
