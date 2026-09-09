package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.model.ToolGroupId
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.model.ToolStatus

data class ToolDefinition(
    val id: ToolId,
    val groupId: ToolGroupId,
    val titleKey: String,
    val keywords: List<String>,
    val glyph: String,
    val status: ToolStatus,
    val supportsHistory: Boolean,
    val supportsFavorites: Boolean
)

data class ToolGroupDefinition(
    val id: ToolGroupId,
    val titleKey: String,
    val toolIds: List<ToolId>
)

object ToolRegistry {
    val tools: List<ToolDefinition> = listOf(
        tool(ToolId.Mootool, ToolGroupId.Home, "app.nav.home", "⌂", listOf("home", "about", "首页", "主页", "ホーム")),
        tool(ToolId.QuickNote, ToolGroupId.Text, "app.nav.quickNote", "✎", listOf("note", "memo", "markdown", "随手记", "笔记")),
        tool(ToolId.TextDiff, ToolGroupId.Text, "app.nav.textDiff", "⇄", listOf("diff", "compare", "对比")),
        tool(ToolId.Reformat, ToolGroupId.Text, "app.nav.reformat", "✦", listOf("format", "java", "xml", "html", "格式化")),
        tool(ToolId.Json, ToolGroupId.Dev, "app.nav.json", "{}", listOf("json", "xml", "javabean", "jsonpath", "格式化"), ToolStatus.Available),
        tool(ToolId.Java, ToolGroupId.Dev, "app.nav.java", ">_", listOf("java", "groovy", "python", "node", "运行")),
        tool(ToolId.YmlProperties, ToolGroupId.Dev, "app.nav.ymlProperties", "☰", listOf("yaml", "yml", "properties", "配置")),
        tool(ToolId.Protobuf, ToolGroupId.Dev, "app.nav.protobuf", "⬡", listOf("protobuf", "proto")),
        tool(ToolId.Variables, ToolGroupId.Dev, "app.nav.variables", "∑", listOf("env", "environment", "环境变量")),
        tool(ToolId.Http, ToolGroupId.Network, "app.nav.http", "⇄", listOf("http", "curl", "api", "请求")),
        tool(ToolId.Host, ToolGroupId.Network, "app.nav.host", "◎", listOf("host", "dns", "域名")),
        tool(ToolId.Net, ToolGroupId.Network, "app.nav.net", "◈", listOf("network", "ip", "ping", "网络")),
        tool(ToolId.UaParse, ToolGroupId.Network, "app.nav.uaParse", "UA", listOf("ua", "user-agent", "浏览器")),
        tool(ToolId.Encode, ToolGroupId.Encode, "app.nav.encode", "⌁", listOf("encode", "decode", "base64", "编码")),
        tool(ToolId.Crypto, ToolGroupId.Encode, "app.nav.crypto", "🔒", listOf("crypto", "hash", "md5", "加密")),
        tool(ToolId.Regex, ToolGroupId.Encode, "app.nav.regex", ".*", listOf("regex", "regular", "正则")),
        tool(ToolId.Cron, ToolGroupId.Encode, "app.nav.cron", "⏱", listOf("cron", "schedule", "定时")),
        tool(ToolId.QrCode, ToolGroupId.Encode, "app.nav.qrCode", "▦", listOf("qr", "qrcode", "二维码")),
        tool(ToolId.TimeConvert, ToolGroupId.Daily, "app.nav.timeConvert", "◷", listOf("time", "timestamp", "时间"), ToolStatus.Available),
        tool(ToolId.MessageBoard, ToolGroupId.Daily, "app.nav.messageBoard", "✉", listOf("message", "board", "留言")),
        tool(ToolId.Translation, ToolGroupId.Daily, "app.nav.translation", "文", listOf("translate", "word", "翻译")),
        tool(ToolId.Calculator, ToolGroupId.Daily, "app.nav.calculator", "=", listOf("calculator", "calc", "计算")),
        tool(ToolId.ColorBoard, ToolGroupId.Daily, "app.nav.colorBoard", "◐", listOf("color", "palette", "调色")),
        tool(ToolId.Image, ToolGroupId.Daily, "app.nav.image", "▣", listOf("image", "图片")),
        tool(ToolId.Pdf, ToolGroupId.Daily, "app.nav.pdf", "▤", listOf("pdf", "merge", "拆分")),
        tool(ToolId.Hardware, ToolGroupId.System, "app.nav.hardware", "⚙", listOf("hardware", "system", "cpu", "系统"))
    )

    val groups: List<ToolGroupDefinition> = listOf(
        ToolGroupId.Text, ToolGroupId.Dev, ToolGroupId.Network, ToolGroupId.Encode, ToolGroupId.Daily, ToolGroupId.System
    ).map { group ->
        ToolGroupDefinition(group, "app.group.${group.id}", tools.filter { it.groupId == group }.map { it.id })
    }

    val byId: Map<ToolId, ToolDefinition> = tools.associateBy { it.id }

    fun search(query: String, translate: (String) -> String): List<ToolDefinition> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return tools
        return tools.filter { tool ->
            tool.id.id.lowercase().contains(needle) ||
                translate(tool.titleKey).lowercase().contains(needle) ||
                tool.keywords.any { it.lowercase().contains(needle) }
        }
    }

    private fun tool(
        id: ToolId,
        group: ToolGroupId,
        titleKey: String,
        glyph: String,
        keywords: List<String>,
        status: ToolStatus = if (id == ToolId.Mootool) ToolStatus.Available else ToolStatus.NotImplemented
    ): ToolDefinition {
        val history = id != ToolId.Mootool && id != ToolId.Hardware && id != ToolId.MessageBoard
        val favorites = id == ToolId.Regex || id == ToolId.Cron || id == ToolId.ColorBoard
        return ToolDefinition(id, group, titleKey, keywords, glyph, status, history, favorites)
    }
}
