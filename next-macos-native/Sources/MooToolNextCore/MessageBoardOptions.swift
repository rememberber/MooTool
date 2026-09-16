import Foundation

public struct MessageBoardOptions: Codable, Equatable {
    public var fontSize = 72.0
    public var backgroundHex = "F4CE57"
    public var foregroundHex = "183832"
    public var alignment = "center"
    public init() {}

    public func validate(language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        guard fontSize.isFinite, (28...160).contains(fontSize) else {
            throw ToolError(AppLocalization.string("messageBoard.error.fontSize", language: language))
        }
        guard alignment == "left" || alignment == "center" else {
            throw ToolError(AppLocalization.string("messageBoard.error.alignment", language: language))
        }
        for hex in [backgroundHex, foregroundHex] {
            guard hex.count == 6, UInt32(hex, radix: 16) != nil else {
                throw ToolError(AppLocalization.string("messageBoard.error.color", language: language))
            }
        }
    }
}

public struct MessageBoardPreset: Identifiable, Equatable {
    public let id: String
    public let backgroundHex: String
    public let foregroundHex: String
    public init(id: String, backgroundHex: String, foregroundHex: String) {
        self.id = id
        self.backgroundHex = backgroundHex
        self.foregroundHex = foregroundHex
    }
}

public enum MessageBoardThemes {
    /// Preset ids match Electron `messageBoard.preset.*` keys (sunbeam … midnight themes).
    public static let presets: [MessageBoardPreset] = [
        MessageBoardPreset(id: "away", backgroundHex: "F4CE57", foregroundHex: "183832"),
        MessageBoardPreset(id: "closed", backgroundHex: "F36B55", foregroundHex: "FFF7EC"),
        MessageBoardPreset(id: "rest", backgroundHex: "EFE9DC", foregroundHex: "29241F"),
        MessageBoardPreset(id: "busy", backgroundHex: "3459D4", foregroundHex: "F3F5FF"),
        MessageBoardPreset(id: "meeting", backgroundHex: "0F4A3A", foregroundHex: "E8F0C2"),
        MessageBoardPreset(id: "quiet", backgroundHex: "151821", foregroundHex: "E8F0FF"),
        MessageBoardPreset(id: "maintenance", backgroundHex: "3459D4", foregroundHex: "F3F5FF"),
        MessageBoardPreset(id: "call", backgroundHex: "0F4A3A", foregroundHex: "E8F0C2"),
    ]
}
