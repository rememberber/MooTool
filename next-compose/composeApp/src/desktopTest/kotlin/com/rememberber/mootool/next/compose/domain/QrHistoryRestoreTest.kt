package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.QrSession
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QrHistoryRestoreTest {
    @Test
    fun generateRestoresFromDataUrlOutput() {
        val bytes = QrEngine.generatePng("hi", 128, QrErrorCorrection.M, logo = null)
        val output = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes)
        val session = QrSession()
        QrHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "qrCode",
                operation = "gen",
                summary = "gen",
                input = "hi",
                output = output,
                options = QrHistoryMetadata.encodeGenerate(128, QrErrorCorrection.M),
                createdAt = "0",
            ),
        )
        assertEquals(QrTab.Generate, session.tab)
        assertEquals("hi", session.content)
        assertNotNull(session.pngBytes)
        assertTrue(session.pngBytes!!.isNotEmpty())
    }

    @Test
    fun recognizeRestoresTabAndFields() {
        val session = QrSession()
        QrHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 2,
                toolId = "qrCode",
                operation = "rec",
                summary = "rec",
                input = "pic.png",
                output = "decoded",
                options = QrHistoryMetadata.encodeRecognize(),
                createdAt = "0",
            ),
        )
        assertEquals(QrTab.Recognize, session.tab)
        assertEquals("pic.png", session.recognitionName)
        assertEquals("decoded", session.recognitionResult)
    }

    @Test
    fun legacySummaryOutputRegeneratesPng() {
        val decoded = QrHistoryRestore.pngBytesFromHistoryOutput(
            output = "128x128 PNG",
            content = "legacy",
            size = 128,
            correction = QrErrorCorrection.M,
        )
        assertNotNull(decoded)
    }
}
