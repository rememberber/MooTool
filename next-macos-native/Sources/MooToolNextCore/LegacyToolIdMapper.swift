import Foundation

public enum LegacyToolIdMapper {
    private static let knownIDs: Set<String> = [
        "mootool", "quickNote", "textDiff", "reformat", "json", "java", "ymlProperties", "protobuf",
        "variables", "http", "host", "net", "uaParse", "encode", "crypto", "regex", "cron", "qrCode",
        "timeConvert", "messageBoard", "translation", "calculator", "colorBoard", "image", "pdf", "hardware",
    ]

    public static func normalize(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "json" }
        if knownIDs.contains(trimmed) { return trimmed }
        let compact = trimmed.lowercased().replacingOccurrences(of: "_", with: "").replacingOccurrences(of: "-", with: "")
        switch compact {
        case "json", "jsonbeauty": return "json"
        case "qrcode", "qr": return "qrCode"
        case "quicknote": return "quickNote"
        case "textdiff": return "textDiff"
        case "reformat": return "reformat"
        case "ymlproperties", "yml", "properties", "yaml": return "ymlProperties"
        case "protobuf", "proto": return "protobuf"
        case "variables", "env", "environment": return "variables"
        case "http": return "http"
        case "host", "hosts": return "host"
        case "net": return "net"
        case "uaparse", "ua": return "uaParse"
        case "encode": return "encode"
        case "crypto": return "crypto"
        case "regex": return "regex"
        case "cron": return "cron"
        case "timeconvert", "time": return "timeConvert"
        case "messageboard", "message": return "messageBoard"
        case "translation", "translate": return "translation"
        case "calculator", "calc": return "calculator"
        case "colorboard", "color": return "colorBoard"
        case "image": return "image"
        case "pdf": return "pdf"
        case "hardware", "system", "systeminfo": return "hardware"
        case "java", "groovy", "coderun", "javaconsole": return "java"
        default:
            if let first = trimmed.first, first.isLowercase {
                let candidate = String(first).uppercased() + trimmed.dropFirst()
                if knownIDs.contains(candidate) { return candidate }
            }
            return "json"
        }
    }
}
