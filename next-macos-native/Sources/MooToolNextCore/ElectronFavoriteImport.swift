import Foundation
import SQLite3

public struct ElectronFavoriteImportPreview: Equatable, Sendable {
    public var colorCount: Int
    public var regexCount: Int
    public var cronCount: Int
    public var warnings: [String]
}

public enum ElectronFavoriteImport {
    public static let rowLimit = 2_000

    public static func preview(at url: URL) throws -> ElectronFavoriteImportPreview {
        var warnings: [String] = []
        let items = try loadFavorites(at: url, warnings: &warnings)
        let color = items.filter { $0.kind == .color }.count
        let regex = items.filter { $0.kind == .regex }.count
        let cron = items.filter { $0.kind == .cron }.count
        if items.isEmpty { warnings.append("未找到可导入的收藏。") }
        return ElectronFavoriteImportPreview(colorCount: color, regexCount: regex, cronCount: cron, warnings: warnings)
    }

    public static func loadFavorites(at url: URL) throws -> [SavedToolFavorite] {
        var warnings: [String] = []
        return try loadFavorites(at: url, warnings: &warnings)
    }

    public static func merge(importing items: [SavedToolFavorite], into existing: [SavedToolFavorite]) -> (merged: [SavedToolFavorite], added: Int, skipped: Int) {
        var result = existing
        var keys = Set(existing.map(favoriteKey))
        var added = 0, skipped = 0
        for item in items {
            let key = favoriteKey(item)
            if keys.contains(key) { skipped += 1; continue }
            keys.insert(key)
            result.append(item)
            added += 1
        }
        return (result, added, skipped)
    }

    private static func favoriteKey(_ item: SavedToolFavorite) -> String {
        "\(item.kind.rawValue)\u{0}\(item.folder.lowercased())\u{0}\(item.name.lowercased())"
    }

    private static func loadFavorites(at url: URL, warnings: inout [String]) throws -> [SavedToolFavorite] {
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-favorite-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        var database: OpaquePointer?
        guard sqlite3_open_v2(copy.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw ToolError("无法打开 SQLite 数据库。")
        }
        defer { sqlite3_close(database) }
        var items: [SavedToolFavorite] = []
        if tableExists(database, name: "t_next_favorite") {
            items += try queryNextFavorites(database)
        } else {
            items += try queryLegacyFavorites(database, itemTable: "t_favorite_color_item", listTable: "t_favorite_color_list", kind: .color, warnings: &warnings)
            items += try queryLegacyFavorites(database, itemTable: "t_favorite_regex_item", listTable: "t_favorite_regex_list", kind: .regex, warnings: &warnings)
            items += try queryLegacyFavorites(database, itemTable: "t_favorite_cron_item", listTable: "t_favorite_cron_list", kind: .cron, warnings: &warnings)
        }
        return Array(items.prefix(rowLimit))
    }

    private static func queryNextFavorites(_ database: OpaquePointer) throws -> [SavedToolFavorite] {
        let sql = """
        SELECT f.kind, f.name, f.value, f.description, COALESCE(folder.title, '默认收藏夹') AS folder_title, f.create_time
        FROM t_next_favorite f
        LEFT JOIN t_next_favorite_folder folder ON folder.id = f.folder_id
        ORDER BY f.id LIMIT \(rowLimit)
        """
        return try queryRows(database, sql: sql) { statement in
            let kind = ToolFavoriteKind(rawValue: sqliteColumnText(statement, index: 0)) ?? .regex
            let name = sqliteColumnText(statement, index: 1)
            let value = sqliteColumnText(statement, index: 2)
            let remark = sqliteColumnText(statement, index: 3)
            let folder = sqliteColumnText(statement, index: 4)
            var item = SavedToolFavorite(kind: kind, folder: folder.isEmpty ? "默认收藏夹" : folder, name: name.isEmpty ? value : name, value: value, remark: remark)
            item.modified = parseLegacyTime(sqliteColumnText(statement, index: 5))
            return item
        }.filter { !$0.value.isEmpty }
    }

    private static func queryLegacyFavorites(_ database: OpaquePointer, itemTable: String, listTable: String, kind: ToolFavoriteKind, warnings: inout [String]) throws -> [SavedToolFavorite] {
        guard tableExists(database, name: itemTable) else { return [] }
        let hasList = tableExists(database, name: listTable)
        let sql = hasList ? """
        SELECT item.name AS fav_name, item.value AS fav_value, COALESCE(list.title, '默认收藏夹') AS list_title, item.remark AS fav_remark, item.create_time
        FROM \(itemTable) item LEFT JOIN \(listTable) list ON list.id = item.list_id ORDER BY item.id LIMIT \(rowLimit)
        """ : """
        SELECT name AS fav_name, value AS fav_value, '默认收藏夹' AS list_title, remark AS fav_remark, create_time FROM \(itemTable) ORDER BY id LIMIT \(rowLimit)
        """
        return try queryRows(database, sql: sql) { statement in
            let name = sqliteColumnText(statement, index: 0)
            let value = sqliteColumnText(statement, index: 1)
            let folder = sqliteColumnText(statement, index: 2)
            let remark = sqliteColumnText(statement, index: 3)
            var item = SavedToolFavorite(kind: kind, folder: folder, name: name.isEmpty ? value : name, value: value, remark: remark)
            item.modified = parseLegacyTime(sqliteColumnText(statement, index: 4))
            return item
        }.filter { !$0.value.isEmpty }
    }

    private static func queryRows(_ database: OpaquePointer, sql: String, map: (OpaquePointer) -> SavedToolFavorite) throws -> [SavedToolFavorite] {
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [SavedToolFavorite] = []
        while sqlite3_step(statement) == SQLITE_ROW { items.append(map(statement)) }
        return items
    }

    private static func tableExists(_ database: OpaquePointer, name: String) -> Bool {
        var statement: OpaquePointer?
        let sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1"
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else { return false }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, name, -1, SQLITE_FAVORITE_TRANSIENT)
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

private let SQLITE_FAVORITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
