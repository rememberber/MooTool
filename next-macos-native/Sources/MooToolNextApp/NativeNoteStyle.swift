import SwiftUI
import MooToolNextCore

enum NativeNoteStyle {
    static func font(_ name: String, size: Double) -> NSFont {
        if name.isEmpty { return .systemFont(ofSize: size) }
        if name == "ui-monospace" { return .monospacedSystemFont(ofSize: size, weight: .regular) }
        return NSFont(name: name, size: size) ?? NSFontManager.shared.font(withFamily: name, traits: [], weight: 5, size: size) ?? .monospacedSystemFont(ofSize: size, weight: .regular)
    }
    static func color(_ value: NoteColor) -> Color {
        let rgb: UInt32
        switch value { case .default: return .secondary; case .coral: rgb = 0xd97868; case .yellow: rgb = 0xc99535; case .green: rgb = 0x4e9275; case .blue: rgb = 0x4f83cc; case .purple: rgb = 0x8a72b5; case .red: rgb = 0xc96761 }
        return Color(red: Double((rgb >> 16) & 255) / 255, green: Double((rgb >> 8) & 255) / 255, blue: Double(rgb & 255) / 255)
    }
    static func patterns(_ language: NoteSyntax) -> [(String, NSColor)] {
        if language == .plain { return [] }
        let quoted = #"\"(?:[^\"\\]|\\.)*\"|'(?:[^'\\]|\\.)*'"#
        let number = #"\b(?:true|false|null|None|True|False|-?\d+(?:\.\d+)?)\b"#
        switch language {
        case .plain: return []
        case .markdown: return [(#"(?m)^#{1,6}\s.*$"#, .systemBlue), (#"`[^`\n]+`|\*\*[^*\n]+\*\*"#, .systemPurple), (#"\[[^\]\n]+\]\([^\)\n]*\)"#, .systemTeal)]
        case .json: return [(number, .systemOrange), (quoted, .systemGreen), (#"\"(?:[^\"\\]|\\.)*\"(?=\s*:)"#, .systemBlue)]
        case .xml: return [(#"</?[\w:.-]+|/?>"#, .systemBlue), (quoted, .systemGreen), (#"<!--[\s\S]*?-->"#, .secondaryLabelColor)]
        case .yaml: return [(number, .systemOrange), (quoted, .systemGreen), (#"(?m)^\s*[^\s:#][^:\n]*(?=:)"#, .systemBlue), (#"(?m)#.*$"#, .secondaryLabelColor)]
        case .sql: return [(#"(?i)\b(SELECT|FROM|WHERE|JOIN|ON|AS|GROUP|ORDER|BY|AND|OR|NOT|NULL|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|LIMIT|DISTINCT)\b"#, .systemPurple), (number, .systemOrange), (quoted, .systemGreen), (#"(?m)--.*$"#, .secondaryLabelColor)]
        default:
            let keywords = language == .python ? "def|class|import|from|return|if|elif|else|for|while|in|is|not|and|or|try|except|with|as|pass|yield|async|await|lambda" : "class|interface|public|private|protected|static|final|void|int|long|double|boolean|new|return|if|else|for|while|try|catch|throw|throws|import|package|extends|implements|function|const|let|var|export|default|async|await|type"
            return [("\\b(" + keywords + ")\\b", .systemPurple), (number, .systemOrange), (quoted, .systemGreen), (language == .python ? #"(?m)#.*$"# : #"(?m)//.*$|/\*[\s\S]*?\*/"#, .secondaryLabelColor)]
        }
    }
}
