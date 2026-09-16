import Foundation

enum LegacyTimeConvertDraft {
    struct ParsedEntry: Equatable, Sendable {
        var summary: String
        var input: String
        var output: String
        var zone: String
        var unit: String
    }

    private static let timestampToLocal = try! NSRegularExpression(
        pattern: #"^(?:时间戳|Timestamp|タイムスタンプ):\s*(\d+)\s*-->\s*(?:时间|Time|時間)\(([^)]+)\):\s*(.+)$"#,
        options: [.caseInsensitive])
    private static let localToTimestamp = try! NSRegularExpression(
        pattern: #"^(?:时间|Time|時間)\s*\(([^)]+)\):\s*(.+?)\s*-->\s*(?:时间戳|Timestamp|タイムスタンプ):\s*(\d+)$"#,
        options: [.caseInsensitive])

    static func parse(_ content: String) -> [ParsedEntry] {
        let fromConsole = LegacyConsoleDraft.extractMessages(content).compactMap { parseLine($0.trimmingCharacters(in: .whitespacesAndNewlines)) }
        if !fromConsole.isEmpty { return fromConsole }
        return content.split(whereSeparator: \.isNewline)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .compactMap(parseLine)
    }

    static func parseLine(_ line: String) -> ParsedEntry? {
        let range = NSRange(line.startIndex..., in: line)
        if let match = timestampToLocal.firstMatch(in: line, range: range) {
            let timestamp = capture(line, match: match, index: 1)
            let zone = capture(line, match: match, index: 2).trimmingCharacters(in: .whitespacesAndNewlines)
            let localTime = capture(line, match: match, index: 3).trimmingCharacters(in: .whitespacesAndNewlines)
            return ParsedEntry(summary: line, input: timestamp, output: localTime, zone: zone, unit: unitForTimestamp(timestamp))
        }
        if let match = localToTimestamp.firstMatch(in: line, range: range) {
            let zone = capture(line, match: match, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            let localTime = capture(line, match: match, index: 2).trimmingCharacters(in: .whitespacesAndNewlines)
            let timestamp = capture(line, match: match, index: 3)
            return ParsedEntry(summary: line, input: localTime, output: timestamp, zone: zone, unit: unitForTimestamp(timestamp))
        }
        return nil
    }

    private static func capture(_ line: String, match: NSTextCheckingResult, index: Int) -> String {
        guard match.numberOfRanges > index, let range = Range(match.range(at: index), in: line) else { return "" }
        return String(line[range])
    }

    private static func unitForTimestamp(_ timestamp: String) -> String {
        timestamp.count >= 13 ? "millisecond" : "second"
    }
}
