import Foundation

public enum AppLanguage: String, CaseIterable, Sendable, Identifiable {
    case zhCN = "zh-CN"
    case enUS = "en-US"
    case jaJP = "ja-JP"

    public var id: String { rawValue }

    public var locale: Locale { Locale(identifier: rawValue) }

    public static func normalized(_ value: String?) -> AppLanguage {
        let text = value?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        switch text {
        case "en", "en-us", "en_us": return .enUS
        case "ja", "ja-jp", "ja_jp": return .jaJP
        default: return .zhCN
        }
    }
}
