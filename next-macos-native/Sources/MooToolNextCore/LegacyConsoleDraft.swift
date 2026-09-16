import Foundation

enum LegacyConsoleDraft {
    private static let timestampLine = try! NSRegularExpression(pattern: #"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\s*$"#)

    static func extractMessages(_ raw: String) -> [String] {
        paragraphBlocks(raw).compactMap { block in
            let lines = block.split(whereSeparator: \.isNewline).map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
            let body = lines.filter { line in
                let range = NSRange(line.startIndex..., in: line)
                return timestampLine.firstMatch(in: line, range: range) == nil
            }
            if body.isEmpty { return nil }
            return body.joined(separator: "\n")
        }
    }

    static func extractQrGenerateContent(_ raw: String) -> String? {
        for message in extractMessages(raw).reversed() {
            if let value = parseGenerateLabel(message), !value.isEmpty { return value }
        }
        return extractMessages(raw).last?
            .split(whereSeparator: \.isNewline)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .first { !$0.isEmpty }
    }

    private static func paragraphBlocks(_ raw: String) -> [String] {
        var result: [String] = []
        var chunk: [Substring] = []
        for line in raw.split(omittingEmptySubsequences: false, whereSeparator: \.isNewline) {
            if line.isEmpty {
                if !chunk.isEmpty {
                    result.append(chunk.joined(separator: "\n"))
                    chunk = []
                }
            } else {
                chunk.append(line)
            }
        }
        if !chunk.isEmpty { result.append(chunk.joined(separator: "\n")) }
        return result
    }

    private static func parseGenerateLabel(_ message: String) -> String? {
        let lines = message.split(whereSeparator: \.isNewline).map(String.init)
        guard let head = lines.first?.trimmingCharacters(in: .whitespacesAndNewlines) else { return nil }
        let prefixes = ["生成:", "Generate:", "生成："]
        guard let matched = prefixes.first(where: { head.lowercased().hasPrefix($0.lowercased()) }) else { return nil }
        let inline = head.dropFirst(matched.count).trimmingCharacters(in: .whitespacesAndNewlines)
        let tail = lines.dropFirst().joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
        return tail.isEmpty ? (inline.isEmpty ? nil : String(inline)) : tail
    }
}
