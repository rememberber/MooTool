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
    public var textDiff: TextDiffOptions?
    public var noteOptions: QuickNoteOptions?
    public var noteWorkspace: QuickNoteWorkspaceOptions?
    public var messageBoard: MessageBoardOptions?
    public var media: MediaWorkspaceState?
    public var cryptoAsymmetric = "RSA"
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
    public var customGroups: [CustomToolGroup] = []
    public var showNavigationSeparators = true
    public var showRecent = false
    public var sidebarWidth = 215.0
    public var hideNavigationTitles = false
    public var hiddenNavigationToolIds: [String] = []
    public var layoutPaneSizes: [String: [Double]] = [:]
    public var drafts: [String: DraftRecord] = [:]
    public var documents: [SavedDocument] = []
    public var history: [HistoryRecord] = []
    public var httpRequests: [SavedHTTPRequest]?
    public var hostProfiles: [SavedHostProfile]?
    public var translationWords: [SavedTranslationWord]?
    public var toolFavorites: [SavedToolFavorite]?
    public var folders: [DocumentFolder]?
    public var vaultPreferences: [String: VaultPreferences]?
    public var scratchDrafts: [String: DraftRecord]?
    public var noteAttachments: [NoteAttachment]?
    /// Populated only in portable backups; normal workspace saves keep image bytes in attachments/.
    public var attachmentData: [String: Data]?
    public init() {}
    public func validated(language: AppLanguage = AppLocalization.preferredLanguage()) throws -> Self {
        func err(_ key: String) -> ToolError { ToolError(AppLocalization.string(key, language: language)) }
        guard product == Product.id, schema == 1 else { throw err("backup.error.unsupported") }
        try NoteAttachmentRepository.validateManifest(noteAttachments ?? [], language: language)
        if let attachmentData {
            guard Set(attachmentData.keys) == Set((noteAttachments ?? []).map(\.path)),
                  attachmentData.values.allSatisfy({ !$0.isEmpty && $0.count <= NoteImagePayload.maximumBytes }),
                  attachmentData.values.reduce(0, { $0 + $1.count }) <= 64 * 1024 * 1024 else { throw err("backup.error.attachmentInvalid") }
        }
        try DocumentVault(documents: documents, folders: folders ?? []).validate(language: language)
        for document in documents { try document.noteOptions?.validate(language: language) }
        guard (vaultPreferences ?? [:]).keys.allSatisfy({ ["json", "quickNote"].contains($0) }),
              (scratchDrafts ?? [:]).allSatisfy({ ["json", "quickNote"].contains($0.key) && $0.value.documentID == nil }) else { throw err("backup.error.vaultInvalid") }
        guard Set(documents.map(\.id)).count == documents.count,
              Set(history.map(\.id)).count == history.count,
              Set((httpRequests ?? []).map(\.id)).count == (httpRequests ?? []).count,
              Set((hostProfiles ?? []).map(\.id)).count == (hostProfiles ?? []).count else { throw err("backup.error.duplicateRecords") }
        for profile in hostProfiles ?? [] { try profile.validate() }
        guard (hostProfiles ?? []).count <= 200 else { throw err("backup.error.hostTooMany") }
        for word in translationWords ?? [] { try word.validate(language: language) }
        guard (translationWords ?? []).count <= 5_000,
              Set((translationWords ?? []).map(\.id)).count == (translationWords ?? []).count else { throw err("backup.error.translationInvalid") }
        for favorite in toolFavorites ?? [] { try favorite.validate(language: language) }
        guard (toolFavorites ?? []).count <= 2_000,
              Set((toolFavorites ?? []).map(\.id)).count == (toolFavorites ?? []).count else { throw err("backup.error.favoritesInvalid") }
        for draft in Array(drafts.values) + history.map(\.draft) + (httpRequests ?? []).map(\.draft) + Array((scratchDrafts ?? [:]).values) {
            try draft.noteOptions?.validate(language: language); try draft.noteWorkspace?.validate(language: language)
            try draft.messageBoard?.validate()
            try draft.media?.validate(language: language)
            guard ["RSA", "SM2"].contains(draft.cryptoAsymmetric) else { throw err("backup.error.cryptoAsymmetricInvalid") }
            try draft.reformat?.validate()
            if let http = draft.http {
                guard http.timeout.isFinite, (1...120).contains(http.timeout) else { throw err("backup.error.httpTimeoutInvalid") }
                for fields in [http.params, http.cookies, http.form] {
                    guard fields.count <= 1000, Set(fields.map(\.id)).count == fields.count else { throw err("backup.error.httpFieldsInvalid") }
                }
            }
            if let json = draft.json {
                guard [2, 4].contains(json.indent), json.fontName.utf8.count <= 256, json.className.utf8.count <= 240,
                      json.findQuery.utf8.count <= 16_384, json.replacement.utf8.count <= 1024 * 1024 else { throw err("backup.error.jsonSettingsInvalid") }
            }
        }
        let knownIDs = Set(Catalog.tools.map(\.id))
        try CustomToolGroupRules.validate(customGroups, knownToolIDs: knownIDs, language: language)
        guard hiddenNavigationToolIds.count <= knownIDs.count,
              Set(hiddenNavigationToolIds).count == hiddenNavigationToolIds.count,
              hiddenNavigationToolIds.allSatisfy({ knownIDs.contains($0) && $0 != "mootool" }) else { throw err("backup.error.hiddenNavInvalid") }
        guard layoutPaneSizes.count <= 32,
              layoutPaneSizes.values.allSatisfy({ $0.count <= 4 && $0.allSatisfy({ $0.isFinite && $0 >= 0 }) }) else { throw err("backup.error.paneSizesInvalid") }
        guard sidebarWidth.isFinite, (185...300).contains(sidebarWidth) else { throw err("backup.error.sidebarWidthInvalid") }
        guard knownIDs.contains(selectedTool), pinned.allSatisfy(knownIDs.contains), recent.allSatisfy(knownIDs.contains),
              Set(pinned).count == pinned.count, Set(recent).count == recent.count,
              drafts.keys.allSatisfy(knownIDs.contains), history.allSatisfy({ knownIDs.contains($0.toolID) }),
              documents.allSatisfy({ ["quickNote", "json"].contains($0.toolID) }) else { throw err("backup.error.documentsInvalid") }
        return self
    }
}

/// This product never searches for, imports, or writes another product's data.
public struct WorkspaceRepository {
    public let directory: URL
    public init(directory: URL = Product.dataDirectory) { self.directory = directory }
    public var file: URL { directory.appendingPathComponent("workspace.json") }
    public func load(language: AppLanguage = AppLocalization.preferredLanguage()) throws -> WorkspaceSnapshot {
        guard FileManager.default.fileExists(atPath: file.path) else { return WorkspaceSnapshot() }
        let value = try Self.readSnapshot(at: file, language: language)
        guard value.attachmentData == nil else { throw ToolError(AppLocalization.string("backup.error.portableUseImport", language: language)) }
        return value
    }
    public func save(_ snapshot: WorkspaceSnapshot, language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        _ = try snapshot.validated(language: language)
        guard snapshot.attachmentData == nil else { throw ToolError(AppLocalization.string("backup.error.portableNeedsRestore", language: language)) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true,
                                               attributes: [.posixPermissions: 0o700])
        try Self.encode(snapshot).write(to: file, options: [.atomic])
        try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: file.path)
    }
    public static func encode(_ snapshot: WorkspaceSnapshot, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> Data {
        let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(snapshot)
        let maximum = snapshot.attachmentData == nil ? 64 : 192
        guard data.count < maximum * 1024 * 1024 else {
            throw ToolError(String(format: AppLocalization.string("workspace.error.encodeTooLarge", language: language), maximum))
        }
        return data
    }
    public static func readSnapshot(at url: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> WorkspaceSnapshot {
        guard (try url.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0) < 192 * 1024 * 1024 else {
            throw ToolError(AppLocalization.string("workspace.error.fileTooLarge192", language: language))
        }
        let handle = try FileHandle(forReadingFrom: url); defer { try? handle.close() }
        return try decode(handle.read(upToCount: 192 * 1024 * 1024) ?? Data(), language: language)
    }
    public static func decode(_ data: Data, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> WorkspaceSnapshot {
        guard data.count < 192 * 1024 * 1024 else { throw ToolError(AppLocalization.string("backup.error.tooLarge192", language: language)) }
        let value = try JSONDecoder().decode(WorkspaceSnapshot.self, from: data).validated(language: language)
        guard value.attachmentData != nil || data.count < 64 * 1024 * 1024 else {
            throw ToolError(AppLocalization.string("workspace.error.tooLarge64", language: language))
        }
        return value
    }
}
