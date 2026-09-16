import Foundation

public struct MessageBoardOptions: Codable, Equatable {
    public var fontSize = 72.0
    public var backgroundHex = "F4CE57"
    public var foregroundHex = "183832"
    public var alignment = "center"
    public init() {}

    public func validate() throws {
        guard fontSize.isFinite, (28...160).contains(fontSize) else { throw ToolError("留言板字号无效。") }
        guard alignment == "left" || alignment == "center" else { throw ToolError("留言板对齐方式无效。") }
        for hex in [backgroundHex, foregroundHex] {
            guard hex.count == 6, UInt32(hex, radix: 16) != nil else { throw ToolError("留言板颜色无效。") }
        }
    }
}

public enum MessageBoardThemes {
    public static let presets: [(title: String, message: String, backgroundHex: String, foregroundHex: String)] = [
        ("离开一会", "马上回来", "F4CE57", "183832"),
        ("暂停营业", "暂停营业", "F36B55", "FFF7EC"),
        ("休息中", "休息中，请稍候", "EFE9DC", "29241F"),
        ("请勿打扰", "请勿打扰", "3459D4", "F3F5FF"),
        ("正在开会", "正在开会", "0F4A3A", "E8F0C2"),
        ("保持安静", "保持安静", "151821", "E8F0FF"),
    ]
}
