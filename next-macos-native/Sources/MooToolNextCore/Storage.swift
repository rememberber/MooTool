import Foundation

public struct DraftRecord: Codable, Equatable {
    public var input = ""
    public var secondary = ""
    public var output = ""
    public var mode = ""
    public var option = ""
    public var documentID: UUID?
    public var http: HTTPOptions?
    public var httpResult: HTTPResultMetadata?
    public var json: JSONOptions?
    public var reformat: ReformatOptions?
    public var noteOptions: QuickNoteOptions?
    public var noteWorkspace: QuickNoteWorkspaceOptions?
    public var inputEditor: EditorViewState?
    public var outputEditor: EditorViewState?
    public init() {}
}
public struct SavedDocument: Codable, Identifiable, Equatable {
    public var id = UUID()
    public var toolID: String
    public var title: String
    public var content: String
    public var modified = Date()
    public var parentID: UUID?
    public var created: Date?
    public var output: String?
    public var query: String?
    public var inputEditor: EditorViewState?
    public var outputEditor: EditorViewState?
    public var noteOptions: QuickNoteOptions?
    public init(toolID: String, title: String, content: String = "") {
        self.toolID = toolID; self.title = title; self.content = content
        if toolID == "quickNote" { noteOptions = .forDocument(title) }
    }
}
public struct HistoryRecord: Codable, Identifiable, Equatable {
    public var id = UUID()
    public var toolID: String
    public var date = Date()
    public var draft: DraftRecord
    public var favorite = false
    public init(toolID: String, draft: DraftRecord) { self.toolID = toolID; self.draft = draft }
}
public struct WorkspaceSnapshot: Codable, Equatable {
    public var product = Product.id
    public var schema = 1
    public var selectedTool = "mootool"
    public var recent: [String] = []
    public var pinned: [String] = ["json", "quickNote", "timeConvert"]
    public var drafts: [String: DraftRecord] = [:]
    public var documents: [SavedDocument] = []
    public var history: [HistoryRecord] = []
    public var httpRequests: [SavedHTTPRequest]?
    public var folders: [DocumentFolder]?
    public var vaultPreferences: [String: VaultPreferences]?
    public var scratchDrafts: [String: DraftRecord]?
    public var noteAttachments: [NoteAttachment]?
    /// Populated only in portable backups; normal workspace saves keep image bytes in attachments/.
    public var attachmentData: [String: Data]?
    public init() {}
    public func validated() throws -> Self {
        guard product == Product.id, schema == 1 else { throw ToolError("这不是受支持的原生版备份，无法导入。") }
        try NoteAttachmentRepository.validateManifest(noteAttachments ?? [])
        if let attachmentData {
            guard Set(attachmentData.keys) == Set((noteAttachments ?? []).map(\.path)),
                  attachmentData.values.allSatisfy({ !$0.isEmpty && $0.count <= NoteImagePayload.maximumBytes }),
                  attachmentData.values.reduce(0, { $0 + $1.count }) <= 64 * 1024 * 1024 else { throw ToolError("备份的附件内容缺失或超过限制。") }
        }
        try DocumentVault(documents: documents, folders: folders ?? []).validate()
        for document in documents { try document.noteOptions?.validate() }
        guard (vaultPreferences ?? [:]).keys.allSatisfy({ ["json", "quickNote"].contains($0) }),
              (scratchDrafts ?? [:]).allSatisfy({ ["json", "quickNote"].contains($0.key) && $0.value.documentID == nil }) else { throw ToolError("文档库设置或草稿无效。") }
        guard Set(documents.map(\.id)).count == documents.count,
              Set(history.map(\.id)).count == history.count,
              Set((httpRequests ?? []).map(\.id)).count == (httpRequests ?? []).count else { throw ToolError("备份包含重复记录。") }
        for draft in Array(drafts.values) + history.map(\.draft) + (httpRequests ?? []).map(\.draft) + Array((scratchDrafts ?? [:]).values) {
            try draft.noteOptions?.validate(); try draft.noteWorkspace?.validate()
            try draft.reformat?.validate()
            if let http = draft.http {
                guard http.timeout.isFinite, (1...120).contains(http.timeout) else { throw ToolError("HTTP 超时设置无效。") }
                for fields in [http.params, http.cookies, http.form] {
                    guard fields.count <= 1000, Set(fields.map(\.id)).count == fields.count else { throw ToolError("HTTP 参数数量过多或包含重复记录。") }
                }
            }
            if let json = draft.json {
                guard [2, 4].contains(json.indent), json.fontName.utf8.count <= 256, json.className.utf8.count <= 240,
                      json.findQuery.utf8.count <= 16_384, json.replacement.utf8.count <= 1024 * 1024 else { throw ToolError("JSON 设置无效或超过大小限制。") }
            }
        }
        let knownIDs = Set(Catalog.tools.map(\.id))
        guard knownIDs.contains(selectedTool), pinned.allSatisfy(knownIDs.contains), recent.allSatisfy(knownIDs.contains),
              Set(pinned).count == pinned.count, Set(recent).count == recent.count,
              drafts.keys.allSatisfy(knownIDs.contains), history.allSatisfy({ knownIDs.contains($0.toolID) }),
              documents.allSatisfy({ ["quickNote", "json"].contains($0.toolID) }) else { throw ToolError("工作区包含无效工具或重复导航项。") }
        return self
    }
}

/// This product never searches for, imports, or writes another product's data.
public struct WorkspaceRepository {
    public let directory: URL
    public init(directory: URL = Product.dataDirectory) { self.directory = directory }
    public var file: URL { directory.appendingPathComponent("workspace.json") }
    public func load() throws -> WorkspaceSnapshot {
        guard FileManager.default.fileExists(atPath: file.path) else { return WorkspaceSnapshot() }
        let value = try Self.readSnapshot(at: file)
        guard value.attachmentData == nil else { throw ToolError("当前文件是便携备份，请通过“导入备份”恢复图片内容。") }; return value
    }
    public func save(_ snapshot: WorkspaceSnapshot) throws {
        _ = try snapshot.validated()
        guard snapshot.attachmentData == nil else { throw ToolError("含图片内容的备份需要通过恢复操作安装。") }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true,
                                               attributes: [.posixPermissions: 0o700])
        try Self.encode(snapshot).write(to: file, options: [.atomic])
        try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: file.path)
    }
    public static func encode(_ snapshot: WorkspaceSnapshot) throws -> Data {
        let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(snapshot)
        let maximum = snapshot.attachmentData == nil ? 64 : 192
        guard data.count < maximum * 1024 * 1024 else { throw ToolError("工作区或备份超过 \(maximum) MB。") }
        return data
    }
    public static func readSnapshot(at url: URL) throws -> WorkspaceSnapshot {
        guard (try url.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0) < 192 * 1024 * 1024 else { throw ToolError("工作区或备份文件超过 192 MB。") }
        let handle = try FileHandle(forReadingFrom: url); defer { try? handle.close() }
        return try decode(handle.read(upToCount: 192 * 1024 * 1024) ?? Data())
    }
    public static func decode(_ data: Data) throws -> WorkspaceSnapshot {
        guard data.count < 192 * 1024 * 1024 else { throw ToolError("备份超过 192 MB。") }
        let value = try JSONDecoder().decode(WorkspaceSnapshot.self, from: data).validated()
        guard value.attachmentData != nil || data.count < 64 * 1024 * 1024 else { throw ToolError("工作区超过 64 MB。") }
        return value
    }
}
