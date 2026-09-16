import Foundation

public struct SavedHostProfile: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var name: String
    public var content: String
    public var modified = Date()

    public init(name: String, content: String) {
        self.name = name
        self.content = content
    }

    public func validate(language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw ToolError(AppLocalization.string("host.error.nameEmpty", language: language))
        }
        guard name.utf8.count <= 120, content.utf8.count <= 512 * 1024 else {
            throw ToolError(AppLocalization.string("host.error.limitExceeded", language: language))
        }
    }
}
