import SwiftUI
import Observation
import MooToolNextCore

@Observable @MainActor
final class ToolDraft {
    var input = "" { didSet { documentChanged?(); changed?() } }
    var secondary = "" { didSet { changed?() } }
    var output = "" { didSet { documentChanged?(); changed?() } }
    var mode = "" { didSet { changed?() } }
    var option = "" { didSet { documentChanged?(); changed?() } }
    var documentID: UUID? { didSet { changed?() } }
    var http: HTTPOptions? { didSet { changed?() } }
    var httpResult: HTTPResultMetadata? { didSet { changed?() } }
    var json: JSONOptions? { didSet { changed?() } }
    var noteOptions: QuickNoteOptions? { didSet { documentChanged?(); changed?() } }
    var noteWorkspace: QuickNoteWorkspaceOptions? { didSet { changed?() } }
    var inputEditor: EditorViewState? { didSet { changed?() } }
    var outputEditor: EditorViewState? { didSet { changed?() } }
    var editorRevision = 0
    var busy = false
    var status = ""
    var error: String?
    @ObservationIgnored var changed: (() -> Void)?
    @ObservationIgnored var documentChanged: (() -> Void)?
    @ObservationIgnored var httpTask: Task<Void, Never>?
    @ObservationIgnored var jsonTask: Task<Void, Never>?
    @ObservationIgnored var jsonOperationID: UUID?
    @ObservationIgnored var noteTask: Task<Void, Never>?
    @ObservationIgnored var noteOperationID: UUID?
    init(_ record: DraftRecord = DraftRecord()) {
        apply(record)
    }
    func apply(_ record: DraftRecord) {
        httpTask?.cancel()
        if noteTask != nil { noteTask?.cancel(); noteTask = nil; noteOperationID = nil; busy = false }
        if jsonTask != nil { jsonTask?.cancel(); jsonTask = nil; jsonOperationID = nil; busy = false }
        documentID = record.documentID
        input = record.input; secondary = record.secondary; output = record.output; mode = record.mode; option = record.option
        http = record.http; httpResult = record.httpResult; json = record.json
        noteOptions = record.noteOptions; noteWorkspace = record.noteWorkspace
        inputEditor = record.inputEditor; outputEditor = record.outputEditor
        editorRevision += 1
        error = nil; status = ""
    }
    var record: DraftRecord {
        var value = DraftRecord(); value.input = input; value.secondary = secondary; value.output = output
        value.mode = mode; value.option = option; value.documentID = documentID
        value.http = http; value.httpResult = httpResult; value.json = json
        value.noteOptions = noteOptions; value.noteWorkspace = noteWorkspace
        value.inputEditor = inputEditor; value.outputEditor = outputEditor; return value
    }
}

@Observable @MainActor
final class AppStore {
    var selected = "mootool" { didSet { scheduleSave() } }
    var recent: [String] = []
    var pinned: [String] = []
    var documents: [SavedDocument] = []
    var folders: [DocumentFolder] = []
    var vaultPreferences: [String: VaultPreferences] = [:]
    var scratchDrafts: [String: DraftRecord] = [:]
    var noteAttachments: [NoteAttachment] = []
    var attachmentGeneration = 0
    var history: [HistoryRecord] = []
    var httpRequests: [SavedHTTPRequest] = []
    var searchPresented = false
    var historyPresented = false
    var error: String?
    var persistenceBlocked = false
    var savePending = false
    @ObservationIgnored let repository: WorkspaceRepository
    @ObservationIgnored private var drafts: [String: ToolDraft] = [:]
    @ObservationIgnored private var saveTask: Task<Void, Never>?
    @ObservationIgnored var suppressDocumentSync = false
    var editorRestoreGeneration = 0
    init(directory: URL = Product.dataDirectory) {
        repository = WorkspaceRepository(directory: directory)
        do { restore(try repository.load()) }
        catch { self.error = "无法读取工作区，已暂停自动保存以保留原文件。\n" + error.localizedDescription; persistenceBlocked = true }
    }
    func draft(_ toolID: String) -> ToolDraft {
        if let draft = drafts[toolID] { return draft }
        let draft = ToolDraft(); draft.changed = { [weak self] in self?.scheduleSave() }
        draft.documentChanged = { [weak self] in self?.synchronizeDocument(toolID) }
        drafts[toolID] = draft; return draft
    }
    func select(_ id: String) {
        selected = Catalog.tool(id).id
        if id != "mootool" { recent = Array(([id] + recent.filter { $0 != id }).prefix(8)) }
        scheduleSave()
    }
    func togglePin(_ id: String) {
        if pinned.contains(id) { pinned.removeAll { $0 == id } } else { pinned.append(id) }; scheduleSave()
    }
    func record(_ id: String, snapshot: DraftRecord? = nil) {
        let value = snapshot ?? draft(id).record
        guard (try? JSONEncoder().encode(value).count) ?? Int.max <= 256 * 1024 else {
            draft(id).status += " · 大文本保留在草稿，不加入历史"; return
        }
        if history.first?.toolID == id && history.first?.draft == value { return }
        history.insert(HistoryRecord(toolID: id, draft: value), at: 0)
        let favorites = history.filter(\.favorite); let ordinary = Array(history.filter { !$0.favorite }.prefix(100))
        history = (favorites + ordinary).sorted { $0.date > $1.date }; scheduleSave()
    }
    func snapshot() -> WorkspaceSnapshot {
        var value = WorkspaceSnapshot(); value.selectedTool = selected; value.recent = recent; value.pinned = pinned
        value.documents = documents; value.folders = folders; value.vaultPreferences = vaultPreferences
        value.scratchDrafts = scratchDrafts
        value.noteAttachments = noteAttachments.isEmpty ? nil : noteAttachments
        value.history = history; value.httpRequests = httpRequests; value.drafts = drafts.mapValues(\.record); return value
    }
    func restore(_ value: WorkspaceSnapshot) {
        editorRestoreGeneration += 1
        suppressDocumentSync = true; defer { suppressDocumentSync = false }
        selected = Catalog.tool(value.selectedTool).id; recent = value.recent; pinned = value.pinned
        documents = value.documents; history = value.history; httpRequests = value.httpRequests ?? []
        folders = value.folders ?? []; vaultPreferences = value.vaultPreferences ?? [:]
        scratchDrafts = value.scratchDrafts ?? [:]
        noteAttachments = value.noteAttachments ?? []; attachmentGeneration += 1
        // Retain observed draft objects so already-open windows see restored content.
        for id in Set(drafts.keys).union(value.drafts.keys) {
            let record = value.drafts[id] ?? DraftRecord(); let draft = draft(id)
            draft.apply(record)
            if let documentID = draft.documentID, !documents.contains(where: { $0.id == documentID && $0.toolID == id }) { draft.documentID = nil }
        }
    }
    func scheduleSave() {
        guard !persistenceBlocked else { return }
        savePending = true
        saveTask?.cancel()
        saveTask = Task { [weak self] in
            do { try await Task.sleep(for: .milliseconds(400)) } catch { return }
            self?.saveNow()
        }
    }
    func saveNow() {
        guard !persistenceBlocked else { return }
        do { try repository.save(snapshot()); savePending = false } catch { self.error = "工作区保存失败：" + error.localizedDescription }
    }
    func run(_ id: String, operation: @escaping (DraftRecord) throws -> String) {
        let draft = draft(id); guard !draft.busy else { return }
        let value = draft.record
        guard value.input.utf8.count + value.secondary.utf8.count <= 10 * 1024 * 1024 else { draft.error = "输入总大小超过 10 MB。"; return }
        draft.busy = true; draft.error = nil
        Task {
            do {
                let result = try await Task.detached(priority: .userInitiated) { try operation(value) }.value
                if draft.documentID == value.documentID { draft.output = result; draft.status = "已完成 · \(result.count) 字符" }
                else if let documentID = value.documentID, let index = documents.firstIndex(where: { $0.id == documentID && $0.toolID == id }) { documents[index].output = result; scheduleSave() }
                var completed = value; completed.output = result; record(id, snapshot: completed)
            } catch { if draft.documentID == value.documentID { draft.error = error.localizedDescription } }
            draft.busy = false
        }
    }
}
