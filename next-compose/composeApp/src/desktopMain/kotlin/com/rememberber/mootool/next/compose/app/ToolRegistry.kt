package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.model.ToolGroupId
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.model.ToolStatus
import java.util.Locale

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
        tool(ToolId.Mootool, ToolGroupId.Home, "app.nav.home", "⌂", listOf("home", "about", "contributor", "sponsor", "首页", "主页", "贡献者", "ホーム")),
        tool(ToolId.QuickNote, ToolGroupId.Text, "app.nav.quickNote", "✎", listOf("note", "memo", "markdown", "随手记", "笔记"), ToolStatus.Available),
        tool(ToolId.TextDiff, ToolGroupId.Text, "app.nav.textDiff", "⇄", listOf("diff", "compare", "whitespace", "history", "对比"), ToolStatus.Available),
        tool(ToolId.Reformat, ToolGroupId.Text, "app.nav.reformat", "✦", listOf("format", "java", "xml", "html", "nginx", "prettier", "history", "格式化"), ToolStatus.Available),
        tool(
            ToolId.Json,
            ToolGroupId.Dev,
            "app.nav.json",
            "{}",
            listOf("json", "xml", "javabean", "jsonpath", "vault", "snippet", "文档库", "格式化"),
            ToolStatus.Available,
        ),
        tool(
            ToolId.Java,
            ToolGroupId.Dev,
            "app.nav.java",
            ">_",
            listOf("java", "groovy", "python", "node", "runtime", "detect", "codrun", "运行"),
            ToolStatus.Available,
        ),
        tool(ToolId.YmlProperties, ToolGroupId.Dev, "app.nav.ymlProperties", "☰", listOf("yaml", "yml", "properties", "snakeyaml", "配置"), ToolStatus.Available),
        tool(ToolId.Protobuf, ToolGroupId.Dev, "app.nav.protobuf", "⬡", listOf("protobuf", "proto", "wire", "hex", "base64"), ToolStatus.Available),
        tool(ToolId.Variables, ToolGroupId.Dev, "app.nav.variables", "∑", listOf("env", "environment", "环境变量"), ToolStatus.Available),
        tool(ToolId.Http, ToolGroupId.Network, "app.nav.http", "⇄", listOf("http", "curl", "api", "请求"), ToolStatus.Available),
        tool(ToolId.Host, ToolGroupId.Network, "app.nav.host", "◎", listOf("host", "hosts", "profile", "dns", "域名", "方案"), ToolStatus.Available),
        tool(ToolId.Net, ToolGroupId.Network, "app.nav.net", "◈", listOf("network", "ip", "ping", "网络"), ToolStatus.Available),
        tool(ToolId.UaParse, ToolGroupId.Network, "app.nav.uaParse", "UA", listOf("ua", "user-agent", "browser", "chrome", "浏览器"), ToolStatus.Available),
        tool(ToolId.Encode, ToolGroupId.Encode, "app.nav.encode", "⌁", listOf("encode", "decode", "base64", "编码"), ToolStatus.Available),
        tool(ToolId.Crypto, ToolGroupId.Encode, "app.nav.crypto", "🔒", listOf("crypto", "hash", "md5", "加密", "aes", "rsa"), ToolStatus.Available),
        tool(ToolId.Regex, ToolGroupId.Encode, "app.nav.regex", ".*", listOf("regex", "regular", "pattern", "正则", "捕获"), ToolStatus.Available),
        tool(ToolId.Cron, ToolGroupId.Encode, "app.nav.cron", "⏱", listOf("cron", "schedule", "quartz", "favorite", "history", "定时"), ToolStatus.Available),
        tool(ToolId.QrCode, ToolGroupId.Encode, "app.nav.qrCode", "▦", listOf("qr", "qrcode", "二维码"), ToolStatus.Available),
        tool(ToolId.TimeConvert, ToolGroupId.Daily, "app.nav.timeConvert", "◷", listOf("time", "timestamp", "时间"), ToolStatus.Available),
        tool(ToolId.MessageBoard, ToolGroupId.Daily, "app.nav.messageBoard", "✉", listOf("message", "board", "留言板", "留言", "沉浸"), ToolStatus.Available),
        tool(ToolId.Translation, ToolGroupId.Daily, "app.nav.translation", "文", listOf("translate", "word", "翻译"), ToolStatus.Available),
        tool(ToolId.Calculator, ToolGroupId.Daily, "app.nav.calculator", "=", listOf("calculator", "calc", "gcd", "lcm", "计算", "进制"), ToolStatus.Available),
        tool(ToolId.ColorBoard, ToolGroupId.Daily, "app.nav.colorBoard", "◐", listOf("color", "palette", "hex", "rgb", "调色", "取色"), ToolStatus.Available),
        tool(ToolId.Image, ToolGroupId.Daily, "app.nav.image", "▣", listOf("image", "图片", "svg", "watermark", "compress", "截图"), ToolStatus.Available),
        tool(ToolId.Pdf, ToolGroupId.Daily, "app.nav.pdf", "▤", listOf("pdf", "merge", "split", "拆分", "合并"), ToolStatus.Available),
        tool(ToolId.Hardware, ToolGroupId.System, "app.nav.hardware", "⚙", listOf("hardware", "system", "cpu", "系统", "oshi"), ToolStatus.Available)
    )

    val groups: List<ToolGroupDefinition> = listOf(
        ToolGroupId.Text, ToolGroupId.Dev, ToolGroupId.Network, ToolGroupId.Encode, ToolGroupId.Daily, ToolGroupId.System
    ).map { group ->
        ToolGroupDefinition(group, "app.group.${group.id}", tools.filter { it.groupId == group }.map { it.id })
    }

    val byId: Map<ToolId, ToolDefinition> = tools.associateBy { it.id }

    fun search(query: String, translate: (String) -> String): List<ToolDefinition> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return tools
        return tools.filter { tool -> matchesSearch(needle, tool, translate) }
    }

    /** Locale-stable id/keyword matching; localized titles use default locale (Electron `toLocaleLowerCase`). */
    internal fun matchesSearch(needle: String, tool: ToolDefinition, translate: (String) -> String): Boolean {
        if (tool.id.id.lowercase(Locale.ROOT).contains(needle)) return true
        if (translate(tool.titleKey).lowercase(Locale.getDefault()).contains(needle)) return true
        return tool.keywords.any { it.lowercase(Locale.ROOT).contains(needle) }
    }

    fun groupTitleKey(id: ToolGroupId): String? =
        if (id == ToolGroupId.Home) null else "app.group.${id.id}"

    private fun tool(
        id: ToolId,
        group: ToolGroupId,
        titleKey: String,
        glyph: String,
        keywords: List<String>,
        status: ToolStatus = ToolStatus.Available
    ): ToolDefinition {
        val history = id != ToolId.Mootool && id != ToolId.Hardware && id != ToolId.MessageBoard && id != ToolId.Variables && id != ToolId.Translation
        val favorites = id == ToolId.Regex || id == ToolId.Cron || id == ToolId.ColorBoard
        val navKeywords = buildList {
            addAll(keywords)
            if (id.detachable) {
                add("detach")
                add("分离")
            }
        }
        return ToolDefinition(id, group, titleKey, navKeywords, glyph, status, history, favorites)
    }
}
