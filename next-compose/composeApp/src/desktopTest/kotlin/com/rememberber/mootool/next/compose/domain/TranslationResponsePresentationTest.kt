package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationResponsePresentationTest {
    @Test
    fun applyResultRequiresMatchingSeqAndRequestId() {
        assertTrue(
            TranslationResponsePresentation.shouldApplyResult(
                expectedSeq = 3,
                currentSeq = 3,
                activeRequestId = "translation-1",
                resultRequestId = "translation-1",
            ),
        )
        assertFalse(
            TranslationResponsePresentation.shouldApplyResult(
                expectedSeq = 3,
                currentSeq = 4,
                activeRequestId = "translation-1",
                resultRequestId = "translation-1",
            ),
        )
        assertFalse(
            TranslationResponsePresentation.shouldApplyResult(
                expectedSeq = 3,
                currentSeq = 3,
                activeRequestId = "translation-1",
                resultRequestId = "translation-2",
            ),
        )
    }

    @Test
    fun autoDebounceProceedsOnlyWhenSeqUnchanged() {
        assertTrue(TranslationResponsePresentation.shouldProceedAutoDebounce(5, 5))
        assertFalse(TranslationResponsePresentation.shouldProceedAutoDebounce(5, 6))
    }

    @Test
    fun abortedErrorsAreNotShown() {
        assertFalse(TranslationResponsePresentation.shouldShowError(TranslationErrorCode.ABORTED))
        assertTrue(TranslationResponsePresentation.shouldShowError(TranslationErrorCode.TIMEOUT))
    }
}
