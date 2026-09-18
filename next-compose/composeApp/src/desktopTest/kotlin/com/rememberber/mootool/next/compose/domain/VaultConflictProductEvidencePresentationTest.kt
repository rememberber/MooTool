package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class VaultConflictProductEvidencePresentationTest {
    @Test
    fun constantsAlignWithPrepareScript() {
        assertTrue(VaultConflictProductEvidencePresentation.matchesEvidenceInitialJson(
            VaultConflictProductEvidencePresentation.evidenceJsonInitialBody,
        ))
        assertTrue(
            VaultConflictProductEvidencePresentation.matchesEvidenceWalkthroughPath(
                VaultConflictProductEvidencePresentation.EVIDENCE_JSON_RELATIVE_PATH,
            ),
        )
        assertTrue(
            VaultConflictProductEvidencePresentation.matchesEvidenceInitialQuickNote(
                VaultConflictProductEvidencePresentation.evidenceQuickNoteInitialBody,
            ),
        )
        assertTrue(
            VaultConflictProductEvidencePresentation.matchesEvidenceWalkthroughPath(
                VaultConflictProductEvidencePresentation.EVIDENCE_QUICKNOTE_RELATIVE_PATH,
            ),
        )
    }
}
