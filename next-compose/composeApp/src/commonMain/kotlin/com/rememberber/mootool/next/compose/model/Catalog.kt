package com.rememberber.mootool.next.compose.model

enum class ToolId(val id: String) {
    Mootool("mootool"),
    QuickNote("quickNote"),
    TextDiff("textDiff"),
    Reformat("reformat"),
    Json("json"),
    Java("java"),
    YmlProperties("ymlProperties"),
    Protobuf("protobuf"),
    Variables("variables"),
    Http("http"),
    Host("host"),
    Net("net"),
    UaParse("uaParse"),
    Encode("encode"),
    Crypto("crypto"),
    Regex("regex"),
    Cron("cron"),
    QrCode("qrCode"),
    TimeConvert("timeConvert"),
    MessageBoard("messageBoard"),
    Translation("translation"),
    Calculator("calculator"),
    ColorBoard("colorBoard"),
    Image("image"),
    Pdf("pdf"),
    Hardware("hardware");

    val detachable: Boolean get() = this != Mootool

    companion object {
        val ordered: List<ToolId> = entries
        fun fromId(value: String): ToolId? = entries.firstOrNull { it.id == value }
        fun requireId(value: String): ToolId = fromId(value)
            ?: throw IllegalArgumentException("Unknown tool id: $value")
    }
}

enum class ToolGroupId(val id: String) {
    Home("home"),
    Text("text"),
    Dev("dev"),
    Network("network"),
    Encode("encode"),
    Daily("daily"),
    System("system");
}

enum class ToolStatus {
    Available,
    NotImplemented
}

enum class AppLanguage(val code: String) {
    ZhCN("zh-CN"),
    EnUS("en-US"),
    JaJP("ja-JP");

    companion object {
        fun fromCode(value: String): AppLanguage = entries.firstOrNull { it.code == value } ?: ZhCN
    }
}

enum class ThemePreference { System, Light, Dark }

enum class InterfaceStyle { Modern, Quiet, Hero, Smartisan, MiuiV5, Claude }

enum class CloseBehavior { Ask, Hide, Quit }

enum class NavigationStyle { Classic, Card, Grouped }

enum class TaskStatus { Idle, Running, Succeeded, Failed, Cancelled }
