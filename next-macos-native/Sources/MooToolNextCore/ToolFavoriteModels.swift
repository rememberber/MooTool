import Foundation

public enum ToolFavoriteKind: String, Codable, CaseIterable {
    case color, regex, cron
}

public struct SavedToolFavorite: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var kind: ToolFavoriteKind
    public var folder: String
    public var name: String
    public var value: String
    public var remark: String
    public var modified = Date()

    public init(kind: ToolFavoriteKind, folder: String, name: String, value: String, remark: String = "") {
        self.kind = kind
        self.folder = folder
        self.name = name
        self.value = value
        self.remark = remark
    }

    public func validate() throws {
        guard !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw ToolError("收藏内容不能为空。") }
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw ToolError("收藏名称不能为空。") }
        guard folder.utf8.count <= 120, name.utf8.count <= 120, value.utf8.count <= 16_384, remark.utf8.count <= 1024 else {
            throw ToolError("收藏字段超过限制。")
        }
    }
}
