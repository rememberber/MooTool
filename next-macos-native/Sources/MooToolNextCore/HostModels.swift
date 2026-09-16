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

    public func validate() throws {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw ToolError("Host 配置名称不能为空。") }
        guard name.utf8.count <= 120, content.utf8.count <= 512 * 1024 else { throw ToolError("Host 配置名称或内容超过限制。") }
    }
}
