import Foundation

public struct HTTPField: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var name: String
    public var value: String
    public var enabled: Bool
    public init(_ name: String = "", _ value: String = "", enabled: Bool = true) {
        self.name = name; self.value = value; self.enabled = enabled
    }
}

public enum HTTPBodyKind: String, Codable, CaseIterable {
    case raw, json, form, multipart, none
    public var title: String {
        switch self {
        case .raw: return "原始文本"; case .json: return "JSON"; case .form: return "URL 编码表单"
        case .multipart: return "Multipart"; case .none: return "无正文"
        }
    }
}

public struct HTTPMultipartPart: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var name = ""
    public var value = ""
    public var filePath = ""
    public var enabled = true
    public var isFile = false
    public init(name: String = "", value: String = "", filePath: String = "", enabled: Bool = true, isFile: Bool = false) {
        self.name = name; self.value = value; self.filePath = filePath; self.enabled = enabled; self.isFile = isFile
    }
}

public struct HTTPOptions: Codable, Equatable {
    public var params: [HTTPField] = []
    public var cookies: [HTTPField] = []
    public var form: [HTTPField] = []
    public var multipart: [HTTPMultipartPart] = []
    public var bodyKind: HTTPBodyKind = .raw
    public var timeout: Double = 30
    public var followRedirects = true
    public init() {}
}

public struct HTTPResultMetadata: Codable, Equatable {
    public var status: Int
    public var headers: String
    public var cookies: String
    public var url: String
    public var elapsed: Double
    public var bytes: Int
    public init(status: Int, headers: String, cookies: String, url: String, elapsed: Double, bytes: Int) {
        self.status = status; self.headers = headers; self.cookies = cookies; self.url = url; self.elapsed = elapsed; self.bytes = bytes
    }
}

public struct SavedHTTPRequest: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var name: String
    public var collection: String
    public var draft: DraftRecord
    public var modified = Date()
    public init(name: String, collection: String = "我的请求", draft: DraftRecord) {
        self.name = name; self.collection = collection; self.draft = draft
    }
}

public enum HTTPFields {
    public static func active(_ fields: [HTTPField]) -> [HTTPField] { fields.filter { $0.enabled && !$0.name.isEmpty } }
    public static func percentEncode(_ text: String) -> String {
        text.addingPercentEncoding(withAllowedCharacters: CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~")) ?? ""
    }
    public static func query(_ fields: [HTTPField]) -> String {
        active(fields).map { percentEncode($0.name) + "=" + percentEncode($0.value) }.joined(separator: "&")
    }
    public static func appendingQuery(_ query: String, to url: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        guard URLComponents(string: "https://example.invalid/?" + query)?.percentEncodedQuery == query else { throw err("http.field.queryUnencoded", language) }
        guard var components = URLComponents(string: url.trimmingCharacters(in: .whitespacesAndNewlines)) else { throw err("http.field.urlInvalid", language) }
        if !query.isEmpty { components.percentEncodedQuery = [components.percentEncodedQuery, query].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: "&") }
        guard let result = components.url?.absoluteString else { throw err("http.field.urlParamsInvalid", language) }; return result
    }
    public static func headerLines(_ text: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [(String, String)] {
        let normalized = text.replacingOccurrences(of: "\r\n", with: "\n")
        guard !normalized.contains("\r") else { throw err("http.field.headerNewline", language) }
        return try normalized.components(separatedBy: "\n").filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }.map { line in
            guard let colon = line.firstIndex(of: ":") else { throw err("http.field.headerFormat", language) }
            let key = String(line[..<colon]).trimmingCharacters(in: .whitespaces)
            let value = String(line[line.index(after: colon)...]).trimmingCharacters(in: .whitespaces)
            let allowed = CharacterSet(charactersIn: "!#$%&'*+-.^_`|~0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
            guard !key.isEmpty, key.unicodeScalars.allSatisfy(allowed.contains),
                  value.unicodeScalars.allSatisfy({ $0.value == 9 || ($0.value >= 32 && $0.value != 127) }) else { throw err("http.field.headerInvalidChars", language) }
            return (key, value)
        }
    }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }
}
