import Foundation
import SQLite3

public struct ElectronHttpImportPreview: Equatable, Sendable {
    public var databasePath: String
    public var requestCount: Int
    public var warnings: [String]
}

public enum ElectronHttpImport {
    public static let defaultCollection = "Electron 导入"
    public static let rowLimit = 500

    public static func preview(at url: URL, collection: String = defaultCollection) throws -> ElectronHttpImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else {
            throw ToolError("未找到数据库文件。")
        }
        let count = try readRequests(at: url, collection: collection, limit: rowLimit, warnings: &warnings).count
        if count == 0 { warnings.append("未在 t_msg_http 中找到可导入的请求。") }
        return ElectronHttpImportPreview(databasePath: url.path, requestCount: count, warnings: warnings)
    }

    public static func loadRequests(at url: URL, collection: String = defaultCollection, limit: Int = rowLimit) throws -> [SavedHTTPRequest] {
        var warnings: [String] = []
        return try readRequests(at: url, collection: collection, limit: limit, warnings: &warnings)
    }

    public static func merge(importing items: [SavedHTTPRequest], into existing: [SavedHTTPRequest]) -> (merged: [SavedHTTPRequest], added: Int, skipped: Int) {
        var result = existing
        var keys = Set(existing.map { "\($0.collection)\u{0}\($0.name)" })
        var added = 0, skipped = 0
        for item in items {
            let key = "\(item.collection)\u{0}\(item.name)"
            if keys.contains(key) { skipped += 1; continue }
            keys.insert(key)
            result.append(item)
            added += 1
        }
        return (result, added, skipped)
    }

    private static func readRequests(at url: URL, collection: String, limit: Int, warnings: inout [String]) throws -> [SavedHTTPRequest] {
        try withCopiedDatabase(at: url) { copy in
            try queryRequests(copy, collection: collection, limit: limit, warnings: &warnings)
        }
    }

    private static func withCopiedDatabase<T>(at url: URL, _ body: (URL) throws -> T) throws -> T {
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-http-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        return try body(copy)
    }

    private static func queryRequests(_ url: URL, collection: String, limit: Int, warnings: inout [String]) throws -> [SavedHTTPRequest] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw ToolError("无法打开 SQLite 数据库。")
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_msg_http") else {
            warnings.append("数据库中没有 t_msg_http 表。")
            return []
        }
        let sql = "SELECT msg_name, method, url, params, headers, cookies, body, body_type, modified_time FROM t_msg_http ORDER BY id LIMIT \(max(1, min(limit, rowLimit)))"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [SavedHTTPRequest] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let name = sqliteColumnText(statement, index: 0).trimmingCharacters(in: .whitespacesAndNewlines)
            let urlText = sqliteColumnText(statement, index: 2).trimmingCharacters(in: .whitespacesAndNewlines)
            if name.isEmpty && urlText.isEmpty { continue }
            let method = parseMethod(sqliteColumnText(statement, index: 1))
            let params = parseFields(sqliteColumnText(statement, index: 3))
            let headers = parseFields(sqliteColumnText(statement, index: 4))
            let cookies = parseFields(sqliteColumnText(statement, index: 5))
            let body = sqliteColumnText(statement, index: 6)
            let bodyType = sqliteColumnText(statement, index: 7)
            var options = HTTPOptions()
            options.params = params
            options.cookies = cookies
            options.bodyKind = bodyKind(bodyType: bodyType, body: body)
            if options.bodyKind == .form {
                options.form = parseFields(body)
            }
            var draft = DraftRecord()
            draft.mode = method
            draft.option = urlText
            draft.input = body
            draft.secondary = serializeHeaders(headers)
            draft.http = options
            var saved = SavedHTTPRequest(name: name.isEmpty ? urlText : name, collection: collection, draft: draft)
            saved.modified = parseLegacyTime(sqliteColumnText(statement, index: 8))
            items.append(saved)
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

    private static func parseFields(_ raw: String) -> [HTTPField] {
        guard let data = raw.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data),
              let array = json as? [[String: Any]] else { return [] }
        return array.enumerated().map { index, entry in
            let name = (entry["name"] as? String) ?? ""
            let value = (entry["value"] as? String) ?? ""
            let enabled = (entry["enabled"] as? Bool) ?? true
            return HTTPField(name, value, enabled: enabled)
        }.filter { !$0.name.isEmpty || !$0.value.isEmpty }
    }

    private static func serializeHeaders(_ fields: [HTTPField]) -> String {
        HTTPFields.active(fields).map { "\($0.name): \($0.value)" }.joined(separator: "\n")
    }

    private static func parseMethod(_ raw: String) -> String {
        let value = raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let allowed = Set(["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
        return allowed.contains(value) ? value : "GET"
    }

    private static func bodyKind(bodyType: String, body: String) -> HTTPBodyKind {
        let lower = bodyType.lowercased()
        if lower.contains("json") { return .json }
        if lower.contains("multipart") { return .multipart }
        if lower.contains("urlencoded") { return .form }
        if lower.isEmpty && body.isEmpty { return .none }
        return .raw
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
