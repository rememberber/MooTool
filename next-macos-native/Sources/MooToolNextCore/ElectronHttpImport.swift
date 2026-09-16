import Foundation
import SQLite3

public struct ElectronHttpImportPreview: Equatable, Sendable {
    public var databasePath: String
    public var requestCount: Int
    public var historyCount: Int
    public var warnings: [String]
}

public enum ElectronHttpImport {
    public static let defaultCollection = "Electron 导入"
    public static let rowLimit = 500

    public static func preview(
        at url: URL,
        collection: String = defaultCollection,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> ElectronHttpImportPreview {
        var warnings: [String] = []
        guard FileManager.default.fileExists(atPath: url.path) else {
            throw MigrationImportErrors.sqliteNotFound(language)
        }
        let count = try readRequests(at: url, collection: collection, limit: rowLimit, language: language, warnings: &warnings).count
        let historyCount = try readHistory(at: url, limit: rowLimit, language: language, warnings: &warnings).count
        if count == 0 && historyCount == 0 { warnings.append(MigrationImportErrors.warning("migration.warning.httpEmpty", language: language)) }
        return ElectronHttpImportPreview(databasePath: url.path, requestCount: count, historyCount: historyCount, warnings: warnings)
    }

    public static func loadRequests(
        at url: URL,
        collection: String = defaultCollection,
        limit: Int = rowLimit,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> [SavedHTTPRequest] {
        var warnings: [String] = []
        return try readRequests(at: url, collection: collection, limit: limit, language: language, warnings: &warnings)
    }

    public static func loadHttpHistory(
        at url: URL,
        limit: Int = rowLimit,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> [HistoryRecord] {
        var warnings: [String] = []
        return try readHistory(at: url, limit: limit, language: language, warnings: &warnings)
    }

    public static func mergeHistory(importing items: [HistoryRecord], into existing: [HistoryRecord]) -> (merged: [HistoryRecord], added: Int, skipped: Int) {
        var result = existing
        var keys = Set(existing.map(historyFingerprint))
        var added = 0, skipped = 0
        for item in items {
            let key = historyFingerprint(item)
            if keys.contains(key) { skipped += 1; continue }
            keys.insert(key)
            result.append(item)
            added += 1
        }
        return (result.sorted { $0.date > $1.date }, added, skipped)
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

    private static func readRequests(
        at url: URL,
        collection: String,
        limit: Int,
        language: AppLanguage,
        warnings: inout [String]
    ) throws -> [SavedHTTPRequest] {
        try withCopiedDatabase(at: url, language: language) { copy in
            try queryRequests(copy, collection: collection, limit: limit, language: language, warnings: &warnings)
        }
    }

    private static func readHistory(at url: URL, limit: Int, language: AppLanguage, warnings: inout [String]) throws -> [HistoryRecord] {
        try withCopiedDatabase(at: url, language: language) { copy in
            try queryHistory(copy, limit: limit, language: language, warnings: &warnings)
        }
    }

    private static func withCopiedDatabase<T>(at url: URL, language: AppLanguage, _ body: (URL) throws -> T) throws -> T {
        _ = language
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-http-import-\(UUID().uuidString).db")
        try FileManager.default.copyItem(at: url, to: copy)
        defer { try? FileManager.default.removeItem(at: copy) }
        return try body(copy)
    }

    private static func queryRequests(
        _ url: URL,
        collection: String,
        limit: Int,
        language: AppLanguage,
        warnings: inout [String]
    ) throws -> [SavedHTTPRequest] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw MigrationImportErrors.sqliteOpen(language)
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_msg_http") else {
            warnings.append(MigrationImportErrors.warning("migration.warning.noHttpTable", language: language))
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
            let draft = buildDraft(
                method: sqliteColumnText(statement, index: 1),
                url: urlText,
                paramsJSON: sqliteColumnText(statement, index: 3),
                headersJSON: sqliteColumnText(statement, index: 4),
                cookiesJSON: sqliteColumnText(statement, index: 5),
                body: sqliteColumnText(statement, index: 6),
                bodyType: sqliteColumnText(statement, index: 7),
                responseBody: "",
                responseHeaders: "",
                responseCookies: "",
                status: "",
                costMillis: 0)
            var saved = SavedHTTPRequest(name: name.isEmpty ? urlText : name, collection: collection, draft: draft)
            saved.modified = parseLegacyTime(sqliteColumnText(statement, index: 8))
            items.append(saved)
        }
        return items
    }

    private static func queryHistory(_ url: URL, limit: Int, language: AppLanguage, warnings: inout [String]) throws -> [HistoryRecord] {
        var database: OpaquePointer?
        guard sqlite3_open_v2(url.path, &database, SQLITE_OPEN_READONLY, nil) == SQLITE_OK, let database else {
            throw MigrationImportErrors.sqliteOpen(language)
        }
        defer { sqlite3_close(database) }
        guard tableExists(database, name: "t_http_request_history") else {
            warnings.append(MigrationImportErrors.warning("migration.warning.noHttpHistoryTable", language: language))
            return []
        }
        let sql = """
        SELECT title, method, url, params, headers, cookies, body, body_type,
               response_body, response_headers, response_cookies, status, cost_time, create_time
        FROM t_http_request_history ORDER BY id DESC LIMIT \(max(1, min(limit, rowLimit)))
        """
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw ToolError(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }
        var items: [HistoryRecord] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let urlText = sqliteColumnText(statement, index: 2).trimmingCharacters(in: .whitespacesAndNewlines)
            let responseBody = sqliteColumnText(statement, index: 8)
            if urlText.isEmpty && responseBody.isEmpty { continue }
            let draft = historySized(buildDraft(
                method: sqliteColumnText(statement, index: 1),
                url: urlText,
                paramsJSON: sqliteColumnText(statement, index: 3),
                headersJSON: sqliteColumnText(statement, index: 4),
                cookiesJSON: sqliteColumnText(statement, index: 5),
                body: sqliteColumnText(statement, index: 6),
                bodyType: sqliteColumnText(statement, index: 7),
                responseBody: responseBody,
                responseHeaders: sqliteColumnText(statement, index: 9),
                responseCookies: sqliteColumnText(statement, index: 10),
                status: sqliteColumnText(statement, index: 11),
                costMillis: Int(sqlite3_column_int64(statement, 12))))
            var record = HistoryRecord(toolID: "http", draft: draft)
            record.date = parseLegacyTime(sqliteColumnText(statement, index: 13))
            items.append(record)
        }
        return items
    }

    private static func buildDraft(
        method: String,
        url: String,
        paramsJSON: String,
        headersJSON: String,
        cookiesJSON: String,
        body: String,
        bodyType: String,
        responseBody: String,
        responseHeaders: String,
        responseCookies: String,
        status: String,
        costMillis: Int
    ) -> DraftRecord {
        let params = parseFields(paramsJSON)
        let headers = parseFields(headersJSON)
        let cookies = parseFields(cookiesJSON)
        var options = HTTPOptions()
        options.params = params
        options.cookies = cookies
        options.bodyKind = bodyKind(bodyType: bodyType, body: body)
        if options.bodyKind == .form { options.form = parseFields(body) }
        var draft = DraftRecord()
        draft.mode = parseMethod(method)
        draft.option = url
        draft.input = body
        draft.secondary = serializeHeaders(headers)
        draft.http = options
        draft.output = responseBody
        if !responseBody.isEmpty || !status.isEmpty {
            let code = parseStatusCode(status)
            draft.httpResult = HTTPResultMetadata(
                status: code,
                headers: responseHeaders,
                cookies: responseCookies,
                url: url,
                elapsed: Double(max(0, costMillis)) / 1000,
                bytes: responseBody.utf8.count)
        }
        return draft
    }

    private static func historySized(_ draft: DraftRecord) -> DraftRecord {
        var value = draft
        let limit = 120_000
        if value.input.utf8.count > limit { value.input = String(value.input.prefix(limit)) }
        if value.output.utf8.count > limit { value.output = String(value.output.prefix(limit)) }
        return value
    }

    private static func historyFingerprint(_ record: HistoryRecord) -> String {
        let output = record.draft.output.prefix(240)
        return "\(record.toolID)\u{0}\(record.draft.mode)\u{0}\(record.draft.option)\u{0}\(output)"
    }

    private static func parseStatusCode(_ raw: String) -> Int {
        let token = raw.split(separator: " ").first.map(String.init) ?? raw
        return Int(token.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
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
