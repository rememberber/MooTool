import Foundation

public enum EncodingConversionError: Error, Equatable, Sendable {
    case invalidBase64
    case invalidPercentEncoding
}

public enum EncodingConversionService {
    public static func base64Encode(_ value: String) -> String {
        Data(value.utf8).base64EncodedString()
    }

    public static func base64Decode(_ value: String) throws -> String {
        guard let data = Data(base64Encoded: value),
              let decoded = String(data: data, encoding: .utf8) else {
            throw EncodingConversionError.invalidBase64
        }
        return decoded
    }

    public static func urlEncode(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? ""
    }

    public static func urlDecode(_ value: String) throws -> String {
        guard let decoded = value.removingPercentEncoding else {
            throw EncodingConversionError.invalidPercentEncoding
        }
        return decoded
    }
}
