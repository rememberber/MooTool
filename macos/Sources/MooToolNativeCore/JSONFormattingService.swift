import Foundation

public enum JSONFormattingError: Error, Equatable, Sendable {
    case invalidJSON
}

public enum JSONFormattingService {
    public static func format(_ rawValue: String) throws -> String {
        let object = try parse(rawValue)
        let data = try JSONSerialization.data(
            withJSONObject: object,
            options: [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        )
        return String(decoding: data, as: UTF8.self)
    }

    public static func minify(_ rawValue: String) throws -> String {
        let object = try parse(rawValue)
        let data = try JSONSerialization.data(
            withJSONObject: object,
            options: [.withoutEscapingSlashes]
        )
        return String(decoding: data, as: UTF8.self)
    }

    private static func parse(_ rawValue: String) throws -> Any {
        guard let data = rawValue.data(using: .utf8) else {
            throw JSONFormattingError.invalidJSON
        }

        do {
            return try JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
        } catch {
            throw JSONFormattingError.invalidJSON
        }
    }
}
