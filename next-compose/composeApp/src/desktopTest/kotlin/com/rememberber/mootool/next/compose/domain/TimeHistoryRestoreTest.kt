package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.TimeSession
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeHistoryRestoreTest {
    @Test
    fun restoresTimestampToLocalWithZoneAndUnit() {
        val session = TimeSession()
        session.zone = "UTC"
        session.unit = TimestampUnit.Second
        val options = TimeHistoryMetadata.encode("America/New_York", TimestampUnit.Millisecond)
        TimeHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "timeConvert",
                operation = "s",
                summary = "s",
                input = "0",
                output = "1970-01-01 08:00:00",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals("America/New_York", session.zone)
        assertEquals(TimestampUnit.Millisecond, session.unit)
        assertEquals("0", session.timestamp)
        assertEquals("1970-01-01 08:00:00", session.localTime)
        assertEquals("", session.error)
    }

    @Test
    fun restoresLocalToTimestampDirection() {
        val session = TimeSession()
        TimeHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "timeConvert",
                operation = "s",
                summary = "s",
                input = "2024-01-01 00:00:00",
                output = "1704067200",
                options = TimeHistoryMetadata.encode("UTC", TimestampUnit.Second),
                createdAt = "0",
            ),
        )
        assertEquals("2024-01-01 00:00:00", session.localTime)
        assertEquals("1704067200", session.timestamp)
    }
}
