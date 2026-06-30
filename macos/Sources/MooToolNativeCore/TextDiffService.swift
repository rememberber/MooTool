public enum TextDiffLineKind: Equatable, Sendable {
    case unchanged
    case added
    case removed
}

public struct TextDiffLine: Equatable, Identifiable, Sendable {
    public let id: Int
    public let kind: TextDiffLineKind
    public let text: String
}

public struct TextDiffSummary: Equatable, Sendable {
    public let unchanged: Int
    public let added: Int
    public let removed: Int

    public init(unchanged: Int, added: Int, removed: Int) {
        self.unchanged = unchanged
        self.added = added
        self.removed = removed
    }
}

public struct TextDiffResult: Equatable, Sendable {
    public let lines: [TextDiffLine]
    public let summary: TextDiffSummary
}

public enum TextDiffService {
    public static func diff(original: String, revised: String) -> TextDiffResult {
        let originalLines = splitLines(original)
        let revisedLines = splitLines(revised)
        guard !originalLines.isEmpty || !revisedLines.isEmpty else {
            return TextDiffResult(lines: [], summary: TextDiffSummary(unchanged: 0, added: 0, removed: 0))
        }

        let table = lcsTable(originalLines, revisedLines)
        var lines: [TextDiffLine] = []
        var originalIndex = 0
        var revisedIndex = 0
        var nextID = 0

        while originalIndex < originalLines.count || revisedIndex < revisedLines.count {
            if originalIndex < originalLines.count,
               revisedIndex < revisedLines.count,
               originalLines[originalIndex] == revisedLines[revisedIndex] {
                lines.append(TextDiffLine(id: nextID, kind: .unchanged, text: originalLines[originalIndex]))
                originalIndex += 1
                revisedIndex += 1
            } else if revisedIndex < revisedLines.count,
                      (originalIndex == originalLines.count || table[originalIndex][revisedIndex + 1] > table[originalIndex + 1][revisedIndex]) {
                lines.append(TextDiffLine(id: nextID, kind: .added, text: revisedLines[revisedIndex]))
                revisedIndex += 1
            } else if originalIndex < originalLines.count {
                lines.append(TextDiffLine(id: nextID, kind: .removed, text: originalLines[originalIndex]))
                originalIndex += 1
            }
            nextID += 1
        }

        return TextDiffResult(lines: lines, summary: summary(for: lines))
    }

    private static func splitLines(_ text: String) -> [String] {
        text.isEmpty ? [] : text.components(separatedBy: .newlines)
    }

    private static func lcsTable(_ left: [String], _ right: [String]) -> [[Int]] {
        var table = Array(repeating: Array(repeating: 0, count: right.count + 1), count: left.count + 1)
        guard !left.isEmpty, !right.isEmpty else {
            return table
        }

        for leftIndex in stride(from: left.count - 1, through: 0, by: -1) {
            for rightIndex in stride(from: right.count - 1, through: 0, by: -1) {
                if left[leftIndex] == right[rightIndex] {
                    table[leftIndex][rightIndex] = table[leftIndex + 1][rightIndex + 1] + 1
                } else {
                    table[leftIndex][rightIndex] = max(table[leftIndex + 1][rightIndex], table[leftIndex][rightIndex + 1])
                }
            }
        }
        return table
    }

    private static func summary(for lines: [TextDiffLine]) -> TextDiffSummary {
        TextDiffSummary(
            unchanged: lines.filter { $0.kind == .unchanged }.count,
            added: lines.filter { $0.kind == .added }.count,
            removed: lines.filter { $0.kind == .removed }.count
        )
    }
}
