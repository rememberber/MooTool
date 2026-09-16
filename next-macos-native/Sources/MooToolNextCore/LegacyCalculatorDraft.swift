import Foundation

enum LegacyCalculatorDraft {
    private static let arithmetic = try! NSRegularExpression(pattern: #"^(.+?)\s*=\s*([^=]+)$"#)
    private static let decFromHex = try! NSRegularExpression(pattern: #"^DEC\(([^)]+)\)\s*=\s*(\d+)$"#, options: [.caseInsensitive])
    private static let hexFromDec = try! NSRegularExpression(pattern: #"^HEX\((\d+)\)\s*=\s*([0-9a-fA-F]+)$"#, options: [.caseInsensitive])
    private static let binFromDec = try! NSRegularExpression(pattern: #"^BIN\((\d+)\)\s*=\s*([01]+)$"#, options: [.caseInsensitive])
    private static let decFromBin = try! NSRegularExpression(pattern: #"^DEC\(([01]+)\)\s*=\s*(\d+)$"#, options: [.caseInsensitive])

    static func apply(to draft: inout DraftRecord, raw: String) {
        let messages = LegacyConsoleDraft.extractMessages(raw)
        let lines = messages.isEmpty
            ? raw.split(whereSeparator: \.isNewline).map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
            : messages
        guard let message = lines.last else { return }
        draft.output = lines.suffix(12).joined(separator: "\n")
        applyMessage(to: &draft, message: message)
    }

    private static func applyMessage(to draft: inout DraftRecord, message: String) {
        let line = message.split(whereSeparator: \.isNewline).map { $0.trimmingCharacters(in: .whitespaces) }.last { !$0.isEmpty } ?? ""
        if line.isEmpty { return }
        let range = NSRange(line.startIndex..., in: line)
        if let match = decFromHex.firstMatch(in: line, range: range) {
            draft.mode = "十六进制 → 十进制"
            draft.input = capture(line, match: match, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            draft.secondary = capture(line, match: match, index: 2).trimmingCharacters(in: .whitespacesAndNewlines)
            return
        }
        if let match = hexFromDec.firstMatch(in: line, range: range) {
            draft.mode = "十进制 → 其他进制"
            draft.input = capture(line, match: match, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            return
        }
        if let match = binFromDec.firstMatch(in: line, range: range) {
            draft.mode = "十进制 → 其他进制"
            draft.input = capture(line, match: match, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            return
        }
        if let match = decFromBin.firstMatch(in: line, range: range) {
            draft.mode = "二进制 → 十进制"
            draft.input = capture(line, match: match, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            return
        }
        if let match = arithmetic.firstMatch(in: line, range: range) {
            draft.mode = "表达式"
            draft.input = capture(line, match: match, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            draft.secondary = capture(line, match: match, index: 2).trimmingCharacters(in: .whitespacesAndNewlines)
            return
        }
        draft.mode = "表达式"
        draft.input = line
    }

    private static func capture(_ line: String, match: NSTextCheckingResult, index: Int) -> String {
        guard match.numberOfRanges > index, let range = Range(match.range(at: index), in: line) else { return "" }
        return String(line[range])
    }
}
