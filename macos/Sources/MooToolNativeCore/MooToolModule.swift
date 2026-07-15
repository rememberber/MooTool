public enum MooToolModuleStatus: String, Sendable {
    case planned
    case preview
}

public struct MooToolModule: Identifiable, Equatable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let symbolName: String
    public let status: MooToolModuleStatus

    public init(id: String, title: String, symbolName: String, status: MooToolModuleStatus) {
        self.id = id
        self.title = title
        self.symbolName = symbolName
        self.status = status
    }
}
