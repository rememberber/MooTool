import Foundation
import SQLite3

public struct ElectronFuncContentImportPreview: Equatable, Sendable {
    public var count: Int
    public var warnings: [String]
}

public enum ElectronFuncContentImport {
    public static let rowLimit = 500

    public static func preview(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> ElectronFuncContentImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else { throw MigrationImportErrors.sqliteNotFound(language) }
        let count = try load(at: url, language: language, warnings: &warnings).count
        if count == 0 { warnings.append(MigrationImportErrors.warning("migration.warning.funcContentEmpty", language: language)) }
        return ElectronFuncContentImportPreview(count: count, warnings: warnings)
    }

    public static func load(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [ImportedLegacyToolDraft] {
        var warnings: [String] = []
        return try load(at: url, language: language, warnings: &warnings)
    }

    private static func load(at url: URL, language: AppLanguage, warnings: inout [String]) throws -> [ImportedLegacyToolDraft] {
        try withCopiedDatabase(at: url, language: language) { copy in
            try query(copy, language: language, warnings: &warnings)
        }
    }

    private static func withCopiedDatabase<T>(at url: URL, language: AppLanguage, _ body: (URL) throws -> T) throws -> T {
        _ = language
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-func-content-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        return try body(copy)
    }

    private static func query(_ url: URL, language: AppLanguage, warnings: inout [String]) throws -> [ImportedLegacyToolDraft] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw MigrationImportErrors.sqliteOpen(language)
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_func_content") else {
            warnings.append(MigrationImportErrors.warning("migration.warning.noFuncContentTable", language: language))
            return []
        }
        let sql = "SELECT id, func, content FROM t_func_content ORDER BY id LIMIT \(max(1, rowLimit))"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [ImportedLegacyToolDraft] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let rowID = sqliteColumnText(statement, index: 0)
            let content = sqliteColumnText(statement, index: 2)
            if content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { continue }
            let funcName = sqliteColumnText(statement, index: 1)
            let key = "t_func_content:\(rowID.isEmpty ? String(items.count) : rowID)"
            items.append(ImportedLegacyToolDraft(legacyFunc: funcName, content: content, dedupeKey: key))
        }
        return items
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
}

private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
