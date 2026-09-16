package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId

object LegacyToolIdMapper {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ToolId.Json.id
        ToolId.fromId(trimmed)?.let { return it.id }
        val compact = trimmed.lowercase().replace("_", "").replace("-", "")
        return when (compact) {
            "json", "jsonbeauty" -> ToolId.Json.id
            "qrcode", "qr" -> ToolId.QrCode.id
            "quicknote" -> ToolId.QuickNote.id
            "textdiff" -> ToolId.TextDiff.id
            "reformat" -> ToolId.Reformat.id
            "ymlproperties", "yml", "properties", "yaml" -> ToolId.YmlProperties.id
            "protobuf", "proto" -> ToolId.Protobuf.id
            "variables", "env", "environment" -> ToolId.Variables.id
            "http" -> ToolId.Http.id
            "host", "hosts" -> ToolId.Host.id
            "net" -> ToolId.Net.id
            "uaparse", "ua" -> ToolId.UaParse.id
            "encode" -> ToolId.Encode.id
            "crypto" -> ToolId.Crypto.id
            "regex" -> ToolId.Regex.id
            "cron" -> ToolId.Cron.id
            "timeconvert", "time" -> ToolId.TimeConvert.id
            "messageboard", "message" -> ToolId.MessageBoard.id
            "translation", "translate" -> ToolId.Translation.id
            "calculator", "calc" -> ToolId.Calculator.id
            "colorboard", "color" -> ToolId.ColorBoard.id
            "image" -> ToolId.Image.id
            "pdf" -> ToolId.Pdf.id
            "hardware", "system", "systeminfo" -> ToolId.Hardware.id
            "java", "groovy", "coderun", "javaconsole" -> ToolId.Java.id
            else -> ToolId.fromId(trimmed.replaceFirstChar { it.uppercase() })?.id ?: ToolId.Json.id
        }
    }
}
