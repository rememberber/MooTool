import Foundation

public struct EditorViewState: Codable, Equatable {
    public var location = 0
    public var length = 0
    public var scrollX = 0.0
    public var scrollY = 0.0
    public init(location: Int = 0, length: Int = 0, scrollX: Double = 0, scrollY: Double = 0) {
        self.location = location; self.length = length; self.scrollX = scrollX; self.scrollY = scrollY
    }
    public func clamped(toUTF16Length count: Int) -> Self {
        let start = max(0, min(location, max(0, count)))
        return Self(location: start, length: max(0, min(length, max(0, count) - start)),
                    scrollX: scrollX.isFinite ? max(0, scrollX) : 0, scrollY: scrollY.isFinite ? max(0, scrollY) : 0)
    }
}

public struct DocumentFolder: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var toolID: String
    public var parentID: UUID?
    public var name: String
    public var created = Date()
    public var modified = Date()
    public init(toolID: String, name: String, parentID: UUID? = nil) { self.toolID = toolID; self.name = name; self.parentID = parentID }
}

public enum VaultSort: String, Codable, CaseIterable { case name, modified, created
    public var title: String { switch self { case .name: return "名称"; case .modified: return "修改时间"; case .created: return "创建时间" } }
}
public enum NoteViewMode: String, Codable, CaseIterable { case editor, split, preview
    public var title: String { switch self { case .editor: return "编辑"; case .split: return "分栏"; case .preview: return "预览" } }
}
public struct VaultPreferences: Codable, Equatable {
    public var selectedEntryID: UUID?
    public var expanded: Set<UUID> = []
    public var query = ""
    public var includeContent = true
    public var sort: VaultSort = .name
    public var treeVisible = true
    public var noteViewMode: NoteViewMode = .editor
    public init() {}
}

public struct VaultNode: Identifiable {
    public let id: UUID
    public let title: String
    public let path: String
    public let isFolder: Bool
    public let modified: Date
    public let created: Date
    public let children: [VaultNode]
}
public struct VaultRow: Identifiable {
    public var id: UUID { node.id }
    public let node: VaultNode
    public let depth: Int
}
public struct DocumentImportItem: Sendable {
    public let relativePath: String
    public let content: String
    public init(relativePath: String, content: String) { self.relativePath = relativePath; self.content = content }
}

/// An independent, ID-based document library. Names and paths never address another product's files.
public struct DocumentVault {
    public var documents: [SavedDocument]
    public var folders: [DocumentFolder]
    public init(documents: [SavedDocument] = [], folders: [DocumentFolder] = []) { self.documents = documents; self.folders = folders }
    public func validate() throws {
        let allIDs = documents.map(\.id) + folders.map(\.id)
        guard Set(allIDs).count == allIDs.count else { throw ToolError("文档库包含重复标识。") }
        let byID = Dictionary(uniqueKeysWithValues: folders.map { ($0.id, $0) })
        for folder in folders {
            guard ["json", "quickNote"].contains(folder.toolID) else { throw ToolError("文件夹所属工具无效。") }
            _ = try Self.validName(folder.name)
            var visited: Set<UUID> = [folder.id], parent = folder.parentID
            while let id = parent {
                guard let ancestor = byID[id], ancestor.toolID == folder.toolID, visited.insert(id).inserted, visited.count <= 64 else { throw ToolError("文件夹层级无效、包含循环或超过 64 层。") }
                parent = ancestor.parentID
            }
        }
        for file in documents {
            guard ["json", "quickNote"].contains(file.toolID) else { throw ToolError("文档所属工具无效。") }
            if let parent = file.parentID { guard byID[parent]?.toolID == file.toolID else { throw ToolError("文档的父文件夹不存在或属于其他工具。") } }
        }
    }
    public func parent(of id: UUID) -> UUID? { folders.first { $0.id == id }?.parentID ?? documents.first { $0.id == id }?.parentID }
    public func tool(of id: UUID) -> String? { folders.first { $0.id == id }?.toolID ?? documents.first { $0.id == id }?.toolID }
    public func name(of id: UUID) -> String? { folders.first { $0.id == id }?.name ?? documents.first { $0.id == id }?.title }
    public func ancestors(of id: UUID) -> [UUID] {
        var result: [UUID] = [], current = parent(of: id)
        while let value = current, !result.contains(value), result.count < 64 { result.append(value); current = parent(of: value) }
        return result.reversed()
    }
    public func path(of id: UUID) -> String { (ancestors(of: id) + [id]).compactMap(name).joined(separator: "/") }
    public func descendants(of id: UUID) -> Set<UUID> {
        var ids: Set<UUID> = [id], pending = [id]
        while let parent = pending.popLast() {
            for folder in folders where folder.parentID == parent { if ids.insert(folder.id).inserted { pending.append(folder.id) } }
            ids.formUnion(documents.filter { $0.parentID == parent }.map(\.id))
        }; return ids
    }
    public static func validName(_ value: String) throws -> String {
        let name = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty, name != ".", name != "..", name.utf8.count <= 240,
              !name.contains(where: { $0 == "/" || $0 == "\\" || $0 == ":" || $0.isNewline }),
              !name.unicodeScalars.contains(where: { CharacterSet.controlCharacters.contains($0) }) else { throw ToolError("名称不能为空，不能包含路径分隔符、控制字符或超过 240 字节。") }
        return name
    }
    private func checkParent(_ parent: UUID?, toolID: String) throws {
        guard ["json", "quickNote"].contains(toolID) else { throw ToolError("此工具不支持文档库。") }
        if let parent { guard folders.contains(where: { $0.id == parent && $0.toolID == toolID }) else { throw ToolError("目标文件夹不存在或属于其他工具。") } }
    }
    private func available(_ name: String, toolID: String, parent: UUID?, excluding: UUID? = nil) -> Bool {
        func key(_ text: String) -> String { text.precomposedStringWithCanonicalMapping.lowercased() }
        return !folders.contains { $0.id != excluding && $0.toolID == toolID && $0.parentID == parent && key($0.name) == key(name) }
            && !documents.contains { $0.id != excluding && $0.toolID == toolID && $0.parentID == parent && key($0.title) == key(name) }
    }
    public func uniqueName(_ proposed: String, toolID: String, parent: UUID?) -> String {
        if available(proposed, toolID: toolID, parent: parent) { return proposed }
        let url = URL(fileURLWithPath: proposed), ext = url.pathExtension
        let stem = ext.isEmpty ? proposed : String(proposed.dropLast(ext.count + 1))
        var index = 2
        while true {
            let name = "\(stem) (\(index))" + (ext.isEmpty ? "" : "." + ext)
            if available(name, toolID: toolID, parent: parent) { return name }; index += 1
        }
    }
    @discardableResult public mutating func createFolder(toolID: String, name: String, parent: UUID? = nil) throws -> UUID {
        try checkParent(parent, toolID: toolID); let name = try Self.validName(name)
        guard available(name, toolID: toolID, parent: parent) else { throw ToolError("目标位置已有同名文件或文件夹。") }
        if let parent { guard ancestors(of: parent).count < 63 else { throw ToolError("文件夹不能超过 64 层。") } }
        let folder = DocumentFolder(toolID: toolID, name: name, parentID: parent); folders.append(folder); return folder.id
    }
    @discardableResult public mutating func createDocument(toolID: String, name: String, content: String = "", parent: UUID? = nil) throws -> UUID {
        try checkParent(parent, toolID: toolID); let name = try Self.validName(name)
        guard available(name, toolID: toolID, parent: parent) else { throw ToolError("目标位置已有同名文件或文件夹。") }
        var file = SavedDocument(toolID: toolID, title: name, content: content); file.parentID = parent; file.created = Date()
        documents.append(file); return file.id
    }
    public mutating func rename(_ id: UUID, to value: String) throws {
        guard let toolID = tool(of: id) else { throw ToolError("所选项目已不存在。") }
        let name = try Self.validName(value)
        guard available(name, toolID: toolID, parent: parent(of: id), excluding: id) else { throw ToolError("目标位置已有同名文件或文件夹。") }
        if let index = folders.firstIndex(where: { $0.id == id }) { folders[index].name = name; folders[index].modified = Date() }
        if let index = documents.firstIndex(where: { $0.id == id }) { documents[index].title = name; documents[index].modified = Date() }
    }
    public mutating func move(_ id: UUID, to parent: UUID?) throws {
        guard let toolID = tool(of: id), let name = name(of: id) else { throw ToolError("所选项目已不存在。") }
        try checkParent(parent, toolID: toolID)
        if let parent, descendants(of: id).contains(parent) { throw ToolError("不能移动到自身或子文件夹。") }
        guard available(name, toolID: toolID, parent: parent, excluding: id) else { throw ToolError("目标文件夹已有同名项目。") }
        var copy = self
        if let index = copy.folders.firstIndex(where: { $0.id == id }) { copy.folders[index].parentID = parent; copy.folders[index].modified = Date() }
        if let index = copy.documents.firstIndex(where: { $0.id == id }) { copy.documents[index].parentID = parent; copy.documents[index].modified = Date() }
        try copy.validate(); self = copy
    }
    @discardableResult public mutating func duplicate(_ id: UUID) throws -> UUID {
        guard var file = documents.first(where: { $0.id == id }) else { throw ToolError("请选择要复制的文档。") }
        file.id = UUID(); file.title = uniqueName(file.title, toolID: file.toolID, parent: file.parentID)
        file.created = Date(); file.modified = Date(); documents.append(file); return file.id
    }
    @discardableResult public mutating func delete(_ id: UUID) -> Set<UUID> {
        let ids = descendants(of: id); folders.removeAll { ids.contains($0.id) }; documents.removeAll { ids.contains($0.id) }; return ids
    }
    /// Batch operations commit only after every item has been validated.
    public mutating func importDocuments(_ items: [DocumentImportItem], toolID: String, parent: UUID? = nil) throws -> [UUID] {
        try checkParent(parent, toolID: toolID)
        guard items.count <= 500, items.reduce(0, { $0 + $1.content.utf8.count }) <= 32 * 1024 * 1024 else { throw ToolError("一次最多导入 500 份文档，总内容不超过 32 MB。") }
        var copy = self, imported: [UUID] = []
        for item in items {
            let parts = item.relativePath.components(separatedBy: "/")
            guard !parts.isEmpty, parts.count <= 64, item.content.utf8.count <= 10 * 1024 * 1024 else { throw ToolError("导入路径层级或单个文件大小超限。") }
            for part in parts { _ = try Self.validName(part) }
            var directory = parent
            for part in parts.dropLast() {
                if let existing = copy.folders.first(where: { $0.toolID == toolID && $0.parentID == directory && $0.name.precomposedStringWithCanonicalMapping.lowercased() == part.precomposedStringWithCanonicalMapping.lowercased() }) { directory = existing.id }
                else { directory = try copy.createFolder(toolID: toolID, name: part, parent: directory) }
            }
            let name = copy.uniqueName(parts.last!, toolID: toolID, parent: directory)
            imported.append(try copy.createDocument(toolID: toolID, name: name, content: item.content, parent: directory))
        }
        try copy.validate(); self = copy; return imported
    }
    public func tree(toolID: String, preferences: VaultPreferences) -> [VaultNode] {
        let query = preferences.query.trimmingCharacters(in: .whitespacesAndNewlines)
        let files = Dictionary(grouping: documents.filter { $0.toolID == toolID }, by: \.parentID)
        let directories = Dictionary(grouping: folders.filter { $0.toolID == toolID }, by: \.parentID)
        func build(parent: UUID?, prefix: String, includeAll: Bool, depth: Int) -> [VaultNode] {
            guard depth <= 64 else { return [] }
            var nodes: [VaultNode] = []
            for folder in directories[parent] ?? [] {
                let path = prefix + folder.name, matches = includeAll || folder.name.localizedCaseInsensitiveContains(query)
                let children = build(parent: folder.id, prefix: path + "/", includeAll: matches, depth: depth + 1)
                if matches || !children.isEmpty { nodes.append(VaultNode(id: folder.id, title: folder.name, path: path, isFolder: true, modified: folder.modified, created: folder.created, children: children)) }
            }
            for file in files[parent] ?? [] where includeAll || file.title.localizedCaseInsensitiveContains(query) || (preferences.includeContent && file.content.localizedCaseInsensitiveContains(query)) {
                nodes.append(VaultNode(id: file.id, title: file.title, path: prefix + file.title, isFolder: false, modified: file.modified, created: file.created ?? file.modified, children: []))
            }
            return nodes.sorted {
                if $0.isFolder != $1.isFolder { return $0.isFolder }
                if preferences.sort == .modified && $0.modified != $1.modified { return $0.modified > $1.modified }
                if preferences.sort == .created && $0.created != $1.created { return $0.created > $1.created }
                let order = $0.title.localizedStandardCompare($1.title)
                return order == .orderedSame ? $0.id.uuidString < $1.id.uuidString : order == .orderedAscending
            }
        }
        return build(parent: nil, prefix: "", includeAll: query.isEmpty, depth: 0)
    }
    public func rows(toolID: String, preferences: VaultPreferences) -> [VaultRow] {
        func flatten(_ nodes: [VaultNode], depth: Int) -> [VaultRow] {
            nodes.flatMap { node in
                [VaultRow(node: node, depth: depth)] + (node.isFolder && (preferences.expanded.contains(node.id) || !preferences.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) ? flatten(node.children, depth: depth + 1) : [])
            }
        }; return flatten(tree(toolID: toolID, preferences: preferences), depth: 0)
    }
}
