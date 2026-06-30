import Foundation

public enum QRCodePayloadError: Error, Equatable, Sendable {
    case emptyPayload
}

public struct QRCodePayload: Equatable, Sendable {
    public let text: String
    public let byteCount: Int
}

public enum QRCodePayloadService {
    public static func prepare(_ rawValue: String) throws -> QRCodePayload {
        let text = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            throw QRCodePayloadError.emptyPayload
        }

        return QRCodePayload(text: text, byteCount: Data(text.utf8).count)
    }
}
