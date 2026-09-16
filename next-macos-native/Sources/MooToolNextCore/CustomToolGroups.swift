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
    public static func validate(
        _ groups: [CustomToolGroup],
        knownToolIDs: Set<String>,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws {
        func err(_ key: String) -> ToolError { ToolError(AppLocalization.string(key, language: language)) }
        func errf(_ key: String, _ name: String) -> ToolError {
            ToolError(String(format: AppLocalization.string(key, language: language), name))
        }
        guard Set(groups.map(\.id)).count == groups.count else { throw err("customGroup.error.duplicateId") }
        for group in groups {
            let name = group.name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { throw err("customGroup.error.emptyName") }
            guard !group.toolIds.isEmpty else { throw errf("customGroup.error.needsTools", name) }
            guard Set(group.toolIds).count == group.toolIds.count else { throw errf("customGroup.error.duplicateTools", name) }
            guard group.toolIds.allSatisfy(knownToolIDs.contains) else { throw errf("customGroup.error.invalidTools", name) }
        }
    }
}
