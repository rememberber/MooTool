import Foundation
import SQLite3

public struct ElectronTranslationImportPreview: Equatable, Sendable {
    public var wordCount: Int
    public var historyCount: Int
    public var warnings: [String]
}

public enum ElectronTranslationImport {
    public static let rowLimit = 500

    public static func preview(at url: URL) throws -> ElectronTranslationImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else { throw ToolError("未找到数据库文件。") }
        let words = try loadWords(at: url, warnings: &warnings)
        let history = try loadHistory(at: url, warnings: &warnings)
        if words.isEmpty && history.isEmpty { warnings.append("未找到可导入的翻译词条或历史。") }
        return ElectronTranslationImportPreview(wordCount: words.count, historyCount: history.count, warnings: warnings)
    }

    public static func loadWords(at url: URL) throws -> [SavedTranslationWord] {
        var warnings: [String] = []
        return try loadWords(at: url, warnings: &warnings)
    }

    public static func loadHistory(at url: URL) throws -> [HistoryRecord] {
        var warnings: [String] = []
        return try loadHistory(at: url, warnings: &warnings)
    }

    public static func mergeWords(importing items: [SavedTranslationWord], into existing: [SavedTranslationWord]) -> (merged: [SavedTranslationWord], added: Int, skipped: Int) {
        var result = existing
        var keys = Set(existing.map(wordKey))
        var added = 0, skipped = 0
        for item in items {
            let key = wordKey(item)
            if keys.contains(key) { skipped += 1; continue }
            keys.insert(key)
            result.append(item)
            added += 1
        }
        return (result, added, skipped)
    }

    private static func wordKey(_ word: SavedTranslationWord) -> String {
        let source = word.sourceText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let targetLang = word.targetLang.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return "\(source)\u{0}\(targetLang)"
    }

    private static func loadWords(at url: URL, warnings: inout [String]) throws -> [SavedTranslationWord] {
        try withCopiedDatabase(at: url) { copy in
            try queryWords(copy, warnings: &warnings)
        }
    }

    private static func loadHistory(at url: URL, warnings: inout [String]) throws -> [HistoryRecord] {
        try withCopiedDatabase(at: url) { copy in
            try queryHistory(copy, warnings: &warnings)
        }
    }

    private static func withCopiedDatabase<T>(at url: URL, _ body: (URL) throws -> T) throws -> T {
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-translation-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        return try body(copy)
    }

    private static func queryWords(_ url: URL, warnings: inout [String]) throws -> [SavedTranslationWord] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw ToolError("无法打开 SQLite 数据库。")
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_translation_word") else {
            warnings.append("数据库中没有 t_translation_word 表。")
            return []
        }
        let sql = "SELECT source_text, target_text, source_lang, target_lang, remark, modified_time FROM t_translation_word ORDER BY id LIMIT \(rowLimit)"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [SavedTranslationWord] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let source = sqliteColumnText(statement, index: 0).trimmingCharacters(in: .whitespacesAndNewlines)
            if source.isEmpty { continue }
            let languages = normalizeLanguages(source: sqliteColumnText(statement, index: 2), target: sqliteColumnText(statement, index: 3))
            var word = SavedTranslationWord(
                sourceText: source,
                targetText: sqliteColumnText(statement, index: 1),
                sourceLang: languages.source,
                targetLang: languages.target,
                remark: sqliteColumnText(statement, index: 4))
            word.modified = parseLegacyTime(sqliteColumnText(statement, index: 5))
            items.append(word)
        }
        return items
    }

    private static func queryHistory(_ url: URL, warnings: inout [String]) throws -> [HistoryRecord] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw ToolError("无法打开 SQLite 数据库。")
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_translation_history") else {
            warnings.append("数据库中没有 t_translation_history 表。")
            return []
        }
        let sql = "SELECT source_text, target_text, source_lang, target_lang, create_time FROM t_translation_history ORDER BY id DESC LIMIT \(rowLimit)"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [HistoryRecord] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let source = sqliteColumnText(statement, index: 0).trimmingCharacters(in: .whitespacesAndNewlines)
            if source.isEmpty { continue }
            let languages = normalizeLanguages(source: sqliteColumnText(statement, index: 2), target: sqliteColumnText(statement, index: 3))
            var draft = DraftRecord()
            draft.input = String(source.prefix(16_384))
            draft.output = String(sqliteColumnText(statement, index: 1).prefix(16_384))
            draft.mode = languages.target
            draft.option = languages.source
            var record = HistoryRecord(toolID: "translation", draft: draft)
            record.date = parseLegacyTime(sqliteColumnText(statement, index: 4))
            items.append(record)
        }
        return items
    }

    private static func normalizeLanguages(source: String, target: String) -> (source: String, target: String) {
        let normalizedSource = source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "auto" : source
        let normalizedTarget = target.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "zh-Hans" : target
            .replacingOccurrences(of: "zh-CN", with: "zh-Hans")
        return (normalizedSource, normalizedTarget)
    }

    private static func tableExists(_ database: OpaquePointer, name: String) -> Bool {
        var statement: OpaquePointer?
        let sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1"
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else { return false }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, name, -1, SQLITE_TRANSLATION_TRANSIENT)
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

private let SQLITE_TRANSLATION_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
