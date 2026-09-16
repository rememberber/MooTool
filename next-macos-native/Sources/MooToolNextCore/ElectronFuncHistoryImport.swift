import Foundation
import SQLite3

public struct ElectronFuncHistoryImportPreview: Equatable, Sendable {
    public var count: Int
    public var warnings: [String]
}

public enum ElectronFuncHistoryImport {
    public static let rowLimit = 500

    public static func preview(at url: URL) throws -> ElectronFuncHistoryImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else { throw ToolError("未找到数据库文件。") }
        let count = try load(at: url, warnings: &warnings).count
        if count == 0 { warnings.append("未找到可导入的通用工具历史（t_func_history）。") }
        return ElectronFuncHistoryImportPreview(count: count, warnings: warnings)
    }

    public static func load(at url: URL) throws -> [HistoryRecord] {
        var warnings: [String] = []
        return try load(at: url, warnings: &warnings)
    }

    public static func merge(importing items: [HistoryRecord], into existing: [HistoryRecord]) -> (merged: [HistoryRecord], added: Int, skipped: Int) {
        var result = existing
        var legacyKeys = Set(existing.compactMap(legacyDedupeKey))
        var added = 0, skipped = 0
        for item in items {
            if let key = legacyDedupeKey(item) {
                if legacyKeys.contains(key) { skipped += 1; continue }
                legacyKeys.insert(key)
            }
            result.append(item)
            added += 1
        }
        return (result.sorted { $0.date > $1.date }, added, skipped)
    }

    private static func legacyDedupeKey(_ record: HistoryRecord) -> String? {
        guard record.draft.mode == "legacy", record.draft.option.hasPrefix("t_func_history:") else { return nil }
        return record.draft.option
    }

    private static func load(at url: URL, warnings: inout [String]) throws -> [HistoryRecord] {
        try withCopiedDatabase(at: url) { copy in
            try query(copy, warnings: &warnings)
        }
    }

    private static func withCopiedDatabase<T>(at url: URL, _ body: (URL) throws -> T) throws -> T {
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-func-history-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        return try body(copy)
    }

    private static func query(_ url: URL, warnings: inout [String]) throws -> [HistoryRecord] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw ToolError("无法打开 SQLite 数据库。")
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_func_history") else {
            warnings.append("数据库中没有 t_func_history 表。")
            return []
        }
        let sql = """
        SELECT id, func_type, summary, input_text, output_text, extra_data, create_time
        FROM t_func_history ORDER BY id DESC LIMIT \(max(1, rowLimit))
        """
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [HistoryRecord] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let rowID = sqliteColumnText(statement, index: 0)
            let input = sqliteColumnText(statement, index: 3)
            let output = sqliteColumnText(statement, index: 4)
            if input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                && output.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { continue }
            let toolID = LegacyToolIdMapper.normalize(sqliteColumnText(statement, index: 1))
            var draft = DraftRecord()
            draft.mode = "legacy"
            draft.option = "t_func_history:\(rowID.isEmpty ? String(items.count) : rowID)"
            draft.input = truncate(input, limit: 120_000)
            draft.output = truncate(output, limit: 120_000)
            let extra = sqliteColumnText(statement, index: 5)
            if !extra.isEmpty { draft.secondary = truncate(extra, limit: 8_000) }
            var record = HistoryRecord(toolID: toolID, draft: draft)
            record.date = parseLegacyTime(sqliteColumnText(statement, index: 6))
            items.append(record)
        }
        return items
    }

    private static func truncate(_ text: String, limit: Int) -> String {
        if text.utf8.count <= limit { return text }
        return String(text.prefix(limit))
    }

    private static func tableExists(_ database: OpaquePointer, name: String) -> Bool {
        var statement: OpaquePointer?
        let sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1"
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else { return false }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, name, -1, SQLITE_TRANSIENT)
        return sqlite3_step(statement) == SQLITE_ROW
    }

    private static func sqliteColumnText(_ statement: OpaquePointer, index: Int32) -> String {
        guard let cString = sqlite3_column_text(statement, index) else { return "" }
        return String(cString: cString)
    }

    private static func parseLegacyTime(_ raw: String) -> Date {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return Date() }
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = iso.date(from: text) { return date }
        iso.formatOptions = [.withInternetDateTime]
        if let date = iso.date(from: text) { return date }
        let normalized = text.contains("T") ? text : text.replacingOccurrences(of: " ", with: "T")
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return formatter.date(from: normalized) ?? Date()
    }
}

private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
