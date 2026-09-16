import Foundation

public struct CustomToolGroup: Codable, Equatable, Identifiable, Sendable {
    public var id: String
    public var name: String
    public var toolIds: [String]
    public init(id: String = UUID().uuidString, name: String, toolIds: [String]) {
        self.id = id; self.name = name; self.toolIds = toolIds
    }
}

public enum CustomToolGroupRules {
    public static func sanitized(_ groups: [CustomToolGroup]) -> [CustomToolGroup] {
        groups.map { CustomToolGroup(id: $0.id, name: $0.name.trimmingCharacters(in: .whitespacesAndNewlines), toolIds: $0.toolIds) }
    }
    public static func validate(_ groups: [CustomToolGroup], knownToolIDs: Set<String>) throws {
        guard Set(groups.map(\.id)).count == groups.count else { throw ToolError("自定义分组包含重复标识。") }
        for group in groups {
            let name = group.name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { throw ToolError("自定义分组名称不能为空。") }
            guard !group.toolIds.isEmpty else { throw ToolError("自定义分组「\(name)」需要至少选择一个工具。") }
            guard Set(group.toolIds).count == group.toolIds.count else { throw ToolError("自定义分组「\(name)」包含重复工具。") }
            guard group.toolIds.allSatisfy(knownToolIDs.contains) else { throw ToolError("自定义分组「\(name)」包含无效工具。") }
        }
    }
}
