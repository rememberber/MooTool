package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.EncodeSession

object EncodeHistoryRestore {
    fun apply(session: EncodeSession, item: HistoryRecord) {
        val meta = EncodeHistoryMetadata.decode(item.options)
        if (meta != null) {
            session.tab = when (meta.tab) {
                "url" -> EncodeTab.Url
                "hex" -> EncodeTab.Hex
                "ascii" -> EncodeTab.Ascii
                else -> EncodeTab.Unicode
            }
            session.charset = if (meta.charset == "gb2312") UrlCharset.Gb2312 else UrlCharset.Utf8
            session.asciiFormat = if (meta.asciiFormat == "hex") AsciiFormat.Hex else AsciiFormat.Decimal
            if (meta.direction == "reverse") {
                session.setLeft(item.output)
                session.setRight(item.input)
            } else {
                session.setLeft(item.input)
                session.setRight(item.output)
            }
        } else {
            session.setRight(item.output.ifBlank { item.input })
        }
        session.error = ""
    }
}
