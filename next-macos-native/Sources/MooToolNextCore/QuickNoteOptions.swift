import Foundation

public enum NoteSyntax: String, Codable, CaseIterable, Sendable {
    case plain = "text/plain", markdown = "text/markdown", json = "application/json"
    case java = "text/java", javascript = "text/javascript", typescript = "text/typescript", python = "text/python"
    case xml = "text/xml", yaml = "text/yaml", sql = "text/sql"
    public var title: String {
        switch self { case .plain: return "Text"; case .markdown: return "Markdown"; case .json: return "JSON"; case .java: return "Java"; case .javascript: return "JavaScript"; case .typescript: return "TypeScript"; case .python: return "Python"; case .xml: return "XML"; case .yaml: return "YAML"; case .sql: return "SQL" }
    }
}
public enum NoteColor: String, Codable, CaseIterable, Sendable {
    case `default`, coral, yellow, green, blue, purple, red
    public var title: String { switch self { case .default: return "默认"; case .coral: return "珊瑚"; case .yellow: return "黄色"; case .green: return "绿色"; case .blue: return "蓝色"; case .purple: return "紫色"; case .red: return "红色" } }
}
public struct QuickNoteOptions: Codable, Equatable, Sendable {
    public var syntax: NoteSyntax = .markdown
    public var color: NoteColor = .default
    public var fontName = ""
    public var fontSize = 14.0
    public var lineSpacing = 1.0
    public var lineWrap = true
    public static let lineSpacings = [1.0, 1.2, 1.4, 1.6, 1.8, 2.0]
    public init() {}
    public static func forDocument(_ name: String) -> Self {
        var value = Self()
        switch (name as NSString).pathExtension.lowercased() {
        case "md", "markdown": value.syntax = .markdown
        case "json": value.syntax = .json
        case "xml": value.syntax = .xml
        default: value.syntax = .plain
        }
        return value
    }
    public func validate() throws {
        guard fontName.utf8.count <= 256, fontSize.isFinite, (8...48).contains(fontSize), Self.lineSpacings.contains(lineSpacing) else { throw ToolError("随手记字体、字号或行距设置无效。") }
    }
}
public struct QuickNoteWorkspaceOptions: Codable, Equatable, Sendable {
    public var quickReplaceOpen = false
    public var findOpen = false
    public var findQuery = ""
    public var replacement = ""
    public var matchCase = false
    public var wholeWord = false
    public var regex = false
    public init() {}
    public func validate() throws {
        guard findQuery.utf8.count <= 16_384, replacement.utf8.count <= 1024 * 1024 else { throw ToolError("随手记查找或替换内容超过限制。") }
    }
}
public enum QuickNoteAction: String, CaseIterable, Identifiable {
    case trim, removeBlankLines, removeTabs, scientificToNormal, normalToScientific, thousandsToNormal, normalToThousands
    case underscoreToCamel, camelToUnderscore, uppercase, lowercase, linesToComma, linesToSingleQuoted, linesToDoubleQuoted
    case commaToLines, tabsToLines, clearNewlines, deduplicateLines, deduplicateWithCount, escape, unescape, reverseLines, sortAscending, sortDescending
    public var id: String { rawValue }
    public var title: String {
        switch self {
        case .trim: return "去除首尾空格"; case .removeBlankLines: return "删除空行"; case .removeTabs: return "删除 Tab"
        case .scientificToNormal: return "科学计数转普通数字"; case .normalToScientific: return "普通数字转科学计数"
        case .thousandsToNormal: return "去除千分位"; case .normalToThousands: return "添加千分位"
        case .underscoreToCamel: return "下划线转驼峰"; case .camelToUnderscore: return "驼峰转下划线"
        case .uppercase: return "转大写"; case .lowercase: return "转小写"; case .linesToComma: return "换行转逗号"
        case .linesToSingleQuoted: return "换行转单引号列表"; case .linesToDoubleQuoted: return "换行转双引号列表"
        case .commaToLines: return "逗号转换行"; case .tabsToLines: return "Tab 转换行"; case .clearNewlines: return "清除换行"
        case .deduplicateLines: return "按行去重"; case .deduplicateWithCount: return "按行去重并计数"
        case .escape: return "转义文本"; case .unescape: return "还原转义"; case .reverseLines: return "反转行顺序"
        case .sortAscending: return "按行升序"; case .sortDescending: return "按行降序"
        }
    }
}
