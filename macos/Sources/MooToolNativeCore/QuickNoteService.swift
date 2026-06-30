public struct QuickNoteDraft: Equatable, Sendable {
    public let title: String
    public let content: String

    public init(title: String, content: String) {
        self.title = title
        self.content = content
    }
}

public struct QuickNoteSummary: Equatable, Sendable {
    public let title: String
    public let lineCount: Int
    public let characterCount: Int
    public let preview: String
}

public enum QuickNoteService {
    public static func summary(for draft: QuickNoteDraft) -> QuickNoteSummary {
        let content = draft.content.trimmingCharacters(in: .whitespacesAndNewlines)
        let title = draft.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let lines = content.isEmpty ? [] : content.components(separatedBy: .newlines)

        return QuickNoteSummary(
            title: title.isEmpty ? "未命名随手记" : title,
            lineCount: lines.count,
            characterCount: content.count,
            preview: lines.first?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty ?? "暂无内容"
        )
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
