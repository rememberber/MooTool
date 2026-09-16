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

    public func validate(language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        func err(_ key: String) -> ToolError { ToolError(AppLocalization.string(key, language: language)) }
        guard !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw err("favorites.error.emptyValue") }
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw err("favorites.error.emptyName") }
        guard folder.utf8.count <= 120, name.utf8.count <= 120, value.utf8.count <= 16_384, remark.utf8.count <= 1024 else {
            throw err("favorites.error.fieldLimit")
        }
    }
}
