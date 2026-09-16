import Foundation

public struct ImportedLegacyToolDraft: Equatable, Sendable {
    public var legacyFunc: String
    public var content: String
    public var dedupeKey: String

    public init(legacyFunc: String, content: String, dedupeKey: String) {
        self.legacyFunc = legacyFunc
        self.content = content
        self.dedupeKey = dedupeKey
    }
}

public struct LegacyToolDraftApplyResult: Equatable, Sendable {
    public var drafts: [String: DraftRecord]
    public var history: [HistoryRecord]
    public var applied: Int
}

public enum LegacyToolDraftApplier {
    public static let maxCodeBytes = 512_000

    public static func apply(
        rows: [ImportedLegacyToolDraft],
        merging drafts: [String: DraftRecord],
        existingHistory: [HistoryRecord] = []
    ) -> LegacyToolDraftApplyResult {
        let candidates = rows.filter { !$0.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if candidates.isEmpty {
            return LegacyToolDraftApplyResult(drafts: drafts, history: existingHistory, applied: 0)
        }

        var result = drafts
        var history = existingHistory
        var applied = 0

        func patch(_ toolID: String, _ block: (inout DraftRecord) -> Void) {
            var draft = result[toolID] ?? DraftRecord()
            block(&draft)
            result[toolID] = draft
        }

        var codeByMode: [String: String] = [:]
        for row in candidates {
            guard let runtime = codeRuntime(row.legacyFunc) else { continue }
            codeByMode[runtime] = clip(row.content)
        }
        if let lastCode = candidates.last(where: { codeRuntime($0.legacyFunc) != nil }),
           let runtime = codeRuntime(lastCode.legacyFunc) {
            patch("java") {
                $0.mode = runtime
                $0.input = codeByMode[runtime] ?? clip(lastCode.content)
            }
            applied += 1
        }

        if let row = lastRow(candidates, keys: ["regex"]) {
            patch("regex") { $0.input = clip(row.content) }
            applied += 1
        }
        if let row = lastRow(candidates, keys: ["jsonbeauty", "json"]) {
            patch("json") { $0.input = clip(row.content) }
            applied += 1
        }
        if let row = lastRow(candidates, keys: ["textdiffleft"]) {
            patch("textDiff") { $0.input = clip(row.content) }
            applied += 1
        }
        if let row = lastRow(candidates, keys: ["textdiffright"]) {
            patch("textDiff") { $0.secondary = clip(row.content) }
            applied += 1
        }
        if let row = lastRow(candidates, keys: ["timeconvert", "time"]) {
            applied += applyTime(row: row, patch: patch, history: &history)
        }
        if let row = lastRow(candidates, keys: ["calculator", "calc"]) {
            patch("calculator") { LegacyCalculatorDraft.apply(to: &$0, raw: row.content) }
            applied += 1
        }
        if let row = lastRow(candidates, keys: ["qrcode", "qr"]) {
            let content = LegacyConsoleDraft.extractQrGenerateContent(row.content)
                ?? row.content.split(whereSeparator: \.isNewline).map { $0.trimmingCharacters(in: .whitespaces) }.first { !$0.isEmpty }
                ?? row.content.trimmingCharacters(in: .whitespacesAndNewlines)
            patch("qrCode") { $0.input = clip(content) }
            applied += 1
        }

        return LegacyToolDraftApplyResult(drafts: result, history: history, applied: applied)
    }

    private static func applyTime(row: ImportedLegacyToolDraft, patch: (String, (inout DraftRecord) -> Void) -> Void, history: inout [HistoryRecord]) -> Int {
        let parsed = LegacyTimeConvertDraft.parse(row.content)
        if !parsed.isEmpty {
            for entry in parsed {
                var record = HistoryRecord(toolID: "timeConvert", draft: DraftRecord())
                record.draft.input = entry.input
                record.draft.output = entry.output
                record.draft.option = entry.zone
                record.draft.mode = entry.unit
                record.draft.secondary = entry.summary
                history.append(record)
            }
            applyParsedTime(parsed.last!, patch: patch)
            return 1
        }
        patch("timeConvert") { draft in
            let line = row.content.split(whereSeparator: \.isNewline).map { $0.trimmingCharacters(in: .whitespaces) }.first { !$0.isEmpty } ?? ""
            if isIntegerToken(line) {
                draft.input = line
                draft.mode = line.count >= 13 ? "millisecond" : "second"
            } else {
                draft.secondary = line
            }
        }
        return 1
    }

    private static func applyParsedTime(_ entry: LegacyTimeConvertDraft.ParsedEntry, patch: (String, (inout DraftRecord) -> Void) -> Void) {
        patch("timeConvert") { draft in
            if !entry.zone.isEmpty { draft.option = entry.zone }
            draft.mode = entry.unit
            let input = entry.input.trimmingCharacters(in: .whitespacesAndNewlines)
            let output = entry.output.trimmingCharacters(in: .whitespacesAndNewlines)
            if isIntegerToken(input) {
                draft.input = input
                if !output.isEmpty { draft.secondary = output }
            } else {
                draft.secondary = input
                draft.input = output
            }
        }
    }

    private static func lastRow(_ rows: [ImportedLegacyToolDraft], keys: [String]) -> ImportedLegacyToolDraft? {
        let wanted = Set(keys.map(normalizedFunc))
        return rows.last { wanted.contains(normalizedFunc($0.legacyFunc)) }
    }

    private static func normalizedFunc(_ legacyFunc: String) -> String {
        legacyFunc.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            .replacingOccurrences(of: "_", with: "")
            .replacingOccurrences(of: "-", with: "")
    }

    private static func codeRuntime(_ legacyFunc: String) -> String? {
        switch normalizedFunc(legacyFunc) {
        case "java", "javaconsole", "coderun": return "Java"
        case "groovy": return "Groovy"
        case "python": return "Python"
        case "node", "nodejs": return "JavaScript"
        default: return nil
        }
    }

    private static func clip(_ text: String) -> String {
        if text.utf8.count <= maxCodeBytes { return text }
        return String(text.prefix(maxCodeBytes))
    }

    private static func isIntegerToken(_ text: String) -> Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return false }
        return Int64(trimmed) != nil
    }
}
