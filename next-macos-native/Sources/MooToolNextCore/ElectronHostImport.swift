import Foundation
import SQLite3

public struct ElectronHostImportPreview: Equatable, Sendable {
    public var databasePath: String
    public var profileCount: Int
    public var warnings: [String]
}

public enum ElectronHostImport {
    public static let defaultCollectionPrefix = "导入"
    public static let rowLimit = 500

    public static func preview(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> ElectronHostImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else { throw MigrationImportErrors.sqliteNotFound(language) }
        let count = try loadProfiles(at: url, language: language, warnings: &warnings).count
        if count == 0 { warnings.append(MigrationImportErrors.warning("migration.warning.hostEmpty", language: language)) }
        return ElectronHostImportPreview(databasePath: url.path, profileCount: count, warnings: warnings)
    }

    public static func loadProfiles(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [SavedHostProfile] {
        var warnings: [String] = []
        return try loadProfiles(at: url, language: language, warnings: &warnings)
    }

    public static func merge(importing items: [SavedHostProfile], into existing: [SavedHostProfile]) -> (merged: [SavedHostProfile], added: Int, skipped: Int) {
        var result = existing
        var names = Set(existing.map { $0.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() })
        var added = 0, skipped = 0
        for item in items {
            let key = item.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            if key.isEmpty || names.contains(key) { skipped += 1; continue }
            names.insert(key)
            result.append(item)
            added += 1
        }
        return (result, added, skipped)
    }

    private static func loadProfiles(at url: URL, language: AppLanguage, warnings: inout [String]) throws -> [SavedHostProfile] {
        guard FileManager.default.fileExists(atPath: url.path) else { throw MigrationImportErrors.sqliteNotFound(language) }
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-host-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        var database: OpaquePointer?
        guard sqlite3_open_v2(copy.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw MigrationImportErrors.sqliteOpen(language)
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_host") else {
            warnings.append(MigrationImportErrors.warning("migration.warning.noHostTable", language: language))
            return []
        }
        let sql = "SELECT name, content, modified_time FROM t_host ORDER BY id LIMIT \(rowLimit)"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [SavedHostProfile] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let name = sqliteColumnText(statement, index: 0).trimmingCharacters(in: .whitespacesAndNewlines)
            let content = sqliteColumnText(statement, index: 1)
            if name.isEmpty && content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { continue }
            var profile = SavedHostProfile(name: name.isEmpty ? "未命名" : name, content: content)
            profile.modified = parseLegacyTime(sqliteColumnText(statement, index: 2))
            items.append(profile)
        }
        return items
    }

    private static func tableExists(_ database: OpaquePointer, name: String) -> Bool {
        var statement: OpaquePointer?
        let sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1"
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else { return false }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, name, -1, SQLITE_HOST_TRANSIENT)
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

private let SQLITE_HOST_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
