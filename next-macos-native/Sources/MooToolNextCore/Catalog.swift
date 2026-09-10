import Foundation

public enum Product {
    public static let id = "next-macos-native"
    public static let bundleID = "com.rememberber.mootool.next.macos-native"
    public static let name = "MooTool Next Native"
    public static var dataDirectory: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(bundleID, isDirectory: true)
    }
}

public struct Tool: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let symbol: String
    public let group: String
    public let subtitle: String
    public let keywords: String
    public init(_ id: String, _ title: String, _ symbol: String, _ group: String, _ subtitle: String, _ keywords: String = "") {
        self.id = id; self.title = title; self.symbol = symbol; self.group = group; self.subtitle = subtitle; self.keywords = keywords
    }
    public func matches(_ query: String) -> Bool {
        query.isEmpty || "\(title) \(id) \(subtitle) \(keywords)".localizedCaseInsensitiveContains(query)
    }
}

public enum Catalog {
    public static let groups = ["笔记与文本", "开发与格式", "网络与请求", "编码与加密", "实用工具", "系统信息"]
    public static let tools: [Tool] = [
        Tool("mootool", "首页", "house", "首页", "触手可及的开发与日常工具", "home about"),
        Tool("quickNote", "随手记", "square.and.pencil", "笔记与文本", "记录想法，整理 Markdown 笔记", "note memo markdown"),
        Tool("textDiff", "文本对比", "rectangle.split.2x1", "笔记与文本", "逐行比较文本，查看新增与删除", "diff compare"),
        Tool("reformat", "格式化", "paintbrush", "笔记与文本", "整理 JSON、XML 和 HTML 文档", "format xml html"),
        Tool("json", "JSON", "curlybraces", "开发与格式", "格式化、压缩、校验与路径查询", "jsonpath xml"),
        Tool("java", "代码运行", "terminal", "开发与格式", "运行本机的 JavaScript、Python、Swift 与 Java", "node python swift groovy"),
        Tool("ymlProperties", "配置转换", "doc.badge.gearshape", "开发与格式", "YAML、JSON 与 Properties 互转", "yaml yml properties"),
        Tool("protobuf", "Protobuf", "chevron.left.forwardslash.chevron.right", "开发与格式", "查看 Protobuf wire 字段及二进制数据", "proto wire hex"),
        Tool("variables", "环境变量", "function", "开发与格式", "查看进程环境，编辑独立的运行变量", "env environment"),
        Tool("http", "HTTP", "globe", "网络与请求", "发送请求，检查响应头、耗时和正文", "curl api request"),
        Tool("host", "Host", "server.rack", "网络与请求", "读取系统 Hosts，编辑和导出配置", "dns hosts"),
        Tool("net", "网络工具", "network", "网络与请求", "DNS、Ping、Whois 与网络接口", "ip ping whois dns"),
        Tool("uaParse", "UA 解析", "safari", "网络与请求", "识别 User-Agent 中的浏览器、系统与设备", "user-agent browser"),
        Tool("encode", "编码转换", "arrow.left.arrow.right", "编码与加密", "Base64、URL、Hex、Unicode 与 HTML 实体", "encode decode"),
        Tool("crypto", "加密工具", "lock.shield", "编码与加密", "摘要、HMAC、AES-GCM、UUID 与随机数据", "hash md5 sha256 sha512"),
        Tool("regex", "正则表达式", "text.magnifyingglass", "编码与加密", "匹配、捕获分组与替换", "regexp match"),
        Tool("cron", "Cron", "calendar.badge.clock", "编码与加密", "解析五段 Cron，预览接下来的执行时间", "schedule"),
        Tool("qrCode", "二维码", "qrcode", "编码与加密", "生成、保存与识别二维码", "qr scan"),
        Tool("timeConvert", "时间转换", "clock", "实用工具", "时间戳、日期和时区转换", "timestamp timezone"),
        Tool("messageBoard", "留言板", "text.bubble", "实用工具", "用全屏大字展示你的留言", "message sign notice"),
        Tool("translation", "翻译", "character.bubble", "实用工具", "使用 macOS 系统翻译或查询词典", "translate dictionary"),
        Tool("calculator", "计算器", "plus.forwardslash.minus", "实用工具", "数学表达式、函数与进制转换", "calc math"),
        Tool("colorBoard", "调色板", "paintpalette", "实用工具", "颜色选择、屏幕取色与 HEX / RGB / HSL", "color picker"),
        Tool("image", "图片工具", "photo", "实用工具", "预览、缩放、转换格式与压缩图片", "resize compress png jpeg"),
        Tool("pdf", "PDF", "doc.richtext", "实用工具", "预览、合并、提取页面与导出文本", "merge split"),
        Tool("hardware", "系统信息", "cpu", "系统信息", "查看系统版本、处理器、内存与磁盘", "hardware memory")
    ]
    public static func tool(_ id: String) -> Tool { tools.first { $0.id == id } ?? tools[0] }
}

public struct ToolError: LocalizedError, Equatable {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var errorDescription: String? { message }
}
