import Foundation
import SQLite3

public struct ElectronVaultSqliteImportPreview: Equatable, Sendable {
    public var quickNoteCount: Int
    public var jsonCount: Int
    public var warnings: [String]
}

public struct ImportedVaultQuickNote: Equatable, Sendable {
    public var dedupeKey: String
    public var title: String
    public var content: String
    public var options: QuickNoteOptions
    public var modified: Date?
}

public struct ImportedVaultJsonDocument: Equatable, Sendable {
    public var dedupeKey: String
    public var title: String
    public var content: String
}

public enum ElectronVaultSqliteImport {
    public static let rowLimit = 500

    public static func preview(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> ElectronVaultSqliteImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else { throw MigrationImportErrors.sqliteNotFound(language) }
        let notes = try loadQuickNotes(at: url, language: language, warnings: &warnings)
        let json = try loadJsonDocuments(at: url, language: language, warnings: &warnings)
        if notes.isEmpty && json.isEmpty { warnings.append(MigrationImportErrors.warning("migration.warning.vaultSqliteEmpty", language: language)) }
        return ElectronVaultSqliteImportPreview(quickNoteCount: notes.count, jsonCount: json.count, warnings: warnings)
    }

    public static func loadQuickNotes(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [ImportedVaultQuickNote] {
        var warnings: [String] = []
        return try loadQuickNotes(at: url, language: language, warnings: &warnings)
    }

    public static func loadJsonDocuments(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [ImportedVaultJsonDocument] {
        var warnings: [String] = []
        return try loadJsonDocuments(at: url, language: language, warnings: &warnings)
    }

    public static func filterNewQuickNotes(_ items: [ImportedVaultQuickNote], existing: [SavedDocument]) -> [ImportedVaultQuickNote] {
        items.filter { item in
            let title = sanitizedTitle(item.title)
            return !existing.contains { $0.toolID == "quickNote" && titlesMatch($0.title, title, extensions: ["md", "markdown", "txt"]) && $0.content == item.content }
        }
    }

    public static func filterNewJson(_ items: [ImportedVaultJsonDocument], existing: [SavedDocument]) -> [ImportedVaultJsonDocument] {
        items.filter { item in
            let title = sanitizedTitle(item.title, defaultExtension: "json")
            return !existing.contains { $0.toolID == "json" && titlesMatch($0.title, title, extensions: ["json"]) && $0.content == item.content }
        }
    }

    private static func titlesMatch(_ saved: String, _ imported: String, extensions: [String]) -> Bool {
        if saved == imported { return true }
        let savedBase = (saved as NSString).deletingPathExtension
        let importedBase = (imported as NSString).deletingPathExtension
        if savedBase.caseInsensitiveCompare(importedBase) == .orderedSame { return true }
        for ext in extensions {
            if saved.caseInsensitiveCompare("\(importedBase).\(ext)") == .orderedSame { return true }
            if "\(savedBase).\(ext)".caseInsensitiveCompare(imported) == .orderedSame { return true }
        }
        return false
    }

    public static func documentImportItems(from notes: [ImportedVaultQuickNote]) -> [DocumentImportItem] {
        notes.map { note in
            let base = sanitizedTitle(note.title)
            let name = base.hasSuffix(".md") || base.hasSuffix(".markdown") ? base : base + ".md"
            return DocumentImportItem(relativePath: name, content: note.content)
        }
    }

    public static func documentImportItems(from json: [ImportedVaultJsonDocument]) -> [DocumentImportItem] {
        json.map { doc in
            let base = sanitizedTitle(doc.title, defaultExtension: "json")
            let name = base.hasSuffix(".json") ? base : base + ".json"
            return DocumentImportItem(relativePath: name, content: doc.content)
        }
    }

    private static func loadQuickNotes(at url: URL, language: AppLanguage, warnings: inout [String]) throws -> [ImportedVaultQuickNote] {
        try withCopiedDatabase(at: url, language: language) { copy in
            try queryQuickNotes(copy, language: language, warnings: &warnings)
        }
    }

    private static func loadJsonDocuments(at url: URL, language: AppLanguage, warnings: inout [String]) throws -> [ImportedVaultJsonDocument] {
        try withCopiedDatabase(at: url, language: language) { copy in
            try queryJsonDocuments(copy, language: language, warnings: &warnings)
        }
    }

    private static func withCopiedDatabase<T>(at url: URL, language: AppLanguage, _ body: (URL) throws -> T) throws -> T {
        _ = language
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-vault-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        return try body(copy)
    }

    private static func queryQuickNotes(_ url: URL, language: AppLanguage, warnings: inout [String]) throws -> [ImportedVaultQuickNote] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw MigrationImportErrors.sqliteOpen(language)
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_quick_note") else {
            warnings.append(MigrationImportErrors.warning("migration.warning.noQuickNoteTable", language: language))
            return []
        }
        let sql = """
        SELECT id, name, content, create_time, modified_time, color, style, font_name, font_size, syntax, line_wrap
        FROM t_quick_note ORDER BY id LIMIT \(max(1, rowLimit))
        """
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [ImportedVaultQuickNote] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let rowID = sqliteColumnText(statement, index: 0)
            let content = sqliteColumnText(statement, index: 2)
            if content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { continue }
            let title = sqliteColumnText(statement, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            var options = QuickNoteOptions.forDocument(title.isEmpty ? "note.md" : title + ".md")
            options.color = mapNoteColor(sqliteColumnText(statement, index: 5))
            options.syntax = mapSyntax(sqliteColumnText(statement, index: 9))
            options.fontName = sqliteColumnText(statement, index: 7)
            options.fontSize = clampFontSize(sqliteColumnText(statement, index: 8))
            options.lineWrap = sqliteColumnText(statement, index: 10) == "1" || sqliteColumnText(statement, index: 10).lowercased() == "true"
            let modified = parseLegacyTime(sqliteColumnText(statement, index: 4))
            let key = "t_quick_note:\(rowID.isEmpty ? String(items.count) : rowID)"
            items.append(ImportedVaultQuickNote(
                dedupeKey: key,
                title: title.isEmpty ? "Legacy Note \(items.count + 1)" : title,
                content: content,
                options: options,
                modified: modified))
        }
        return items
    }

    private static func queryJsonDocuments(_ url: URL, language: AppLanguage, warnings: inout [String]) throws -> [ImportedVaultJsonDocument] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw MigrationImportErrors.sqliteOpen(language)
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_json_beauty") else {
            warnings.append(MigrationImportErrors.warning("migration.warning.noJsonBeautyTable", language: language))
            return []
        }
        let sql = "SELECT id, name, content FROM t_json_beauty ORDER BY id LIMIT \(max(1, rowLimit))"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [ImportedVaultJsonDocument] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let rowID = sqliteColumnText(statement, index: 0)
            let content = sqliteColumnText(statement, index: 2)
            if content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { continue }
            let name = sqliteColumnText(statement, index: 1).trimmingCharacters(in: .whitespacesAndNewlines)
            let key = "t_json_beauty:\(rowID.isEmpty ? String(items.count) : rowID)"
            items.append(ImportedVaultJsonDocument(
                dedupeKey: key,
                title: name.isEmpty ? "json-\(items.count + 1)" : name,
                content: content))
        }
        return items
    }

    private static func sanitizedTitle(_ raw: String, defaultExtension: String = "md") -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let fallback = "import-\(UUID().uuidString.prefix(6))"
        let base = trimmed.isEmpty ? fallback : trimmed
        let invalid = CharacterSet(charactersIn: "/:\\")
        let cleaned = base.components(separatedBy: invalid).joined(separator: "-")
        return cleaned.isEmpty ? "\(fallback).\(defaultExtension)" : cleaned
    }

    private static func mapNoteColor(_ raw: String) -> NoteColor {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "coral", "orange": return .coral
        case "yellow": return .yellow
        case "green": return .green
        case "blue": return .blue
        case "purple": return .purple
        case "red": return .red
        default: return .default
        }
    }

    private static func mapSyntax(_ raw: String) -> NoteSyntax {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "text/markdown", "markdown": return .markdown
        case "application/json", "json": return .json
        case "text/xml", "xml": return .xml
        case "text/yaml", "yaml": return .yaml
        case "text/java", "java": return .java
        case "text/javascript", "javascript": return .javascript
        case "text/typescript", "typescript": return .typescript
        case "text/python", "python": return .python
        case "text/sql", "sql": return .sql
        default: return .plain
        }
    }

    private static func clampFontSize(_ raw: String) -> Double {
        let value = Double(raw.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 14
        return min(48, max(8, value))
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

    private static func parseLegacyTime(_ raw: String) -> Date? {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return nil }
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = iso.date(from: text) { return date }
        iso.formatOptions = [.withInternetDateTime]
        if let date = iso.date(from: text) { return date }
        let normalized = text.contains("T") ? text : text.replacingOccurrences(of: " ", with: "T")
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return formatter.date(from: normalized)
    }
}

private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
