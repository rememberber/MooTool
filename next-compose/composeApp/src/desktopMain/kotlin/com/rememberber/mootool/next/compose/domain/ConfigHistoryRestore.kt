package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ConfigSession

/** F10 配置转换历史恢复（convert / validate / format）。 */
object ConfigHistoryRestore {
    fun apply(session: ConfigSession, item: HistoryRecord) {
        when (item.options) {
            ConfigHistoryMetadata.TO_YAML -> {
                session.tab = "convert"
                session.properties = item.input
                session.yaml = item.output
            }
            ConfigHistoryMetadata.TO_PROPERTIES -> {
                session.tab = "convert"
                session.yaml = item.input
                session.properties = item.output
            }
            ConfigHistoryMetadata.VALIDATE, ConfigHistoryMetadata.FORMAT -> {
                session.tab = "validate"
                session.validateSource = item.input
                session.validation = item.output
                session.valid = item.options == ConfigHistoryMetadata.FORMAT || item.output.isNotEmpty()
            }
            else -> {
                session.tab = "convert"
                session.properties = item.input
                session.yaml = item.output
            }
        }
        session.error = ""
    }
}
