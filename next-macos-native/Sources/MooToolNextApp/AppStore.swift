import AppKit
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
    var reformat: ReformatOptions? { didSet { changed?() } }
    var textDiff: TextDiffOptions? { didSet { changed?() } }
    var noteOptions: QuickNoteOptions? { didSet { documentChanged?(); changed?() } }
    var noteWorkspace: QuickNoteWorkspaceOptions? { didSet { changed?() } }
    var messageBoard: MessageBoardOptions? { didSet { changed?() } }
    var media: MediaWorkspaceState? { didSet { changed?() } }
    var cryptoAsymmetric = "RSA" { didSet { changed?() } }
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
    @ObservationIgnored var reformatTask: Task<Void, Never>?
    @ObservationIgnored var reformatOperationID: UUID?
    init(_ record: DraftRecord = DraftRecord()) {
        apply(record)
    }
    func apply(_ record: DraftRecord) {
        httpTask?.cancel()
        if noteTask != nil { noteTask?.cancel(); noteTask = nil; noteOperationID = nil; busy = false }
        if jsonTask != nil { jsonTask?.cancel(); jsonTask = nil; jsonOperationID = nil; busy = false }
        if reformatTask != nil { reformatTask?.cancel(); reformatTask = nil; reformatOperationID = nil; busy = false }
        documentID = record.documentID
        input = record.input; secondary = record.secondary; output = record.output; mode = record.mode; option = record.option
        http = record.http; httpResult = record.httpResult; json = record.json
        reformat = record.reformat; textDiff = record.textDiff
        noteOptions = record.noteOptions; noteWorkspace = record.noteWorkspace
        messageBoard = record.messageBoard
        media = record.media
        cryptoAsymmetric = record.cryptoAsymmetric
        inputEditor = record.inputEditor; outputEditor = record.outputEditor
        editorRevision += 1
        error = nil; status = ""
    }
    var record: DraftRecord {
        var value = DraftRecord(); value.input = input; value.secondary = secondary; value.output = output
        value.mode = mode; value.option = option; value.documentID = documentID
        value.http = http; value.httpResult = httpResult; value.json = json
        value.reformat = reformat; value.textDiff = textDiff
        value.noteOptions = noteOptions; value.noteWorkspace = noteWorkspace
        value.messageBoard = messageBoard
        value.media = media
        value.cryptoAsymmetric = cryptoAsymmetric
        value.inputEditor = inputEditor; value.outputEditor = outputEditor; return value
    }
}

@Observable @MainActor
final class AppStore {
    var selected = "mootool" { didSet { scheduleSave() } }
    var recent: [String] = []
    var pinned: [String] = []
    var customGroups: [CustomToolGroup] = []
    var showNavigationSeparators = true
    var showRecent = false
    var sidebarWidth = 215.0
    var hideNavigationTitles = false
    var hiddenNavigationToolIds: [String] = []
    var layoutPaneSizes: [String: [Double]] = [:]
    var documents: [SavedDocument] = []
    var folders: [DocumentFolder] = []
    var vaultPreferences: [String: VaultPreferences] = [:]
    var scratchDrafts: [String: DraftRecord] = [:]
    var noteAttachments: [NoteAttachment] = []
    var attachmentGeneration = 0
    var history: [HistoryRecord] = []
    var httpRequests: [SavedHTTPRequest] = []
    var hostProfiles: [SavedHostProfile] = []
    var searchPresented = false
    var historyPresented = false
    var error: String?
    var persistenceBlocked = false
    var savePending = false
    @ObservationIgnored let repository: WorkspaceRepository
    @ObservationIgnored private var drafts: [String: ToolDraft] = [:]
    @ObservationIgnored private var saveTask: Task<Void, Never>?
    @ObservationIgnored var suppressDocumentSync = false
    @ObservationIgnored private var vaultDiskBaseline: [String: [String: String]] = [:]
    @ObservationIgnored private var vaultMirrorSuppressedUntil = Date.distantPast
    @ObservationIgnored private var vaultDiskRefreshSuspended = false
    @ObservationIgnored private var vaultWatcher: VaultFilesystemWatcher?
    @ObservationIgnored var jsonCheckpointScheduler: VaultGitCheckpointScheduler?
    @ObservationIgnored var quickNoteCheckpointScheduler: VaultGitCheckpointScheduler?
    @ObservationIgnored var vaultAutoPullTimer: DispatchSourceTimer?
    @ObservationIgnored var vaultAutoPullLastRun: [String: TimeInterval] = [:]
    var editorRestoreGeneration = 0
    enum Bootstrap { case full, workspaceOnly }
    init(directory: URL = Product.dataDirectory, bootstrap: Bootstrap = .full) {
        repository = WorkspaceRepository(directory: directory)
        do { restore(try repository.load()) }
        catch { self.error = "无法读取工作区，已暂停自动保存以保留原文件。\n" + error.localizedDescription; persistenceBlocked = true }
        if !persistenceBlocked {
            try? syncVaultFilesystem()
            if bootstrap == .full {
                startVaultFilesystemWatcher()
                startVaultCheckpointSchedulers()
                startVaultAutoPullTimers()
            }
        }
    }
    func startVaultFilesystemWatcher() {
        guard !vaultDiskRefreshSuspended else { return }
        vaultWatcher?.stop()
        vaultWatcher = VaultFilesystemWatcher(workspace: repository.directory) { [weak self] in self?.handleVaultFilesystemEvent() }
        vaultWatcher?.start()
    }
    func suspendVaultDiskRefresh() {
        vaultDiskRefreshSuspended = true
        vaultWatcher?.stop()
        vaultWatcher = nil
    }
    func resumeVaultDiskRefresh() {
        vaultDiskRefreshSuspended = false
        startVaultFilesystemWatcher()
    }
    private func handleVaultFilesystemEvent() {
        guard !vaultDiskRefreshSuspended, !persistenceBlocked, Date() >= vaultMirrorSuppressedUntil else { return }
        for toolID in ["json", "quickNote"] where !draft(toolID).busy { refreshVaultFromDisk(toolID: toolID) }
    }
    func draft(_ toolID: String) -> ToolDraft {
        if let draft = drafts[toolID] { return draft }
        let draft = ToolDraft(); draft.changed = { [weak self] in self?.scheduleSave() }
        draft.documentChanged = { [weak self] in self?.synchronizeDocument(toolID) }
        drafts[toolID] = draft; return draft
    }
    func paneWidth(toolID: String, index: Int, default defaultValue: CGFloat, min: CGFloat, max: CGFloat) -> CGFloat {
        CGFloat(LayoutPaneSizes.pane(layoutPaneSizes, toolId: toolID, index: index, default: Double(defaultValue), min: Double(min), max: Double(max)))
    }
    func setPaneWidth(toolID: String, index: Int, value: CGFloat, slots: Int) {
        layoutPaneSizes = LayoutPaneSizes.withPane(layoutPaneSizes, toolId: toolID, index: index, value: Double(value), slots: slots)
        scheduleSave()
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
        value.customGroups = customGroups; value.showNavigationSeparators = showNavigationSeparators
        value.showRecent = showRecent
        value.sidebarWidth = sidebarWidth
        value.hideNavigationTitles = hideNavigationTitles
        value.hiddenNavigationToolIds = hiddenNavigationToolIds
        value.layoutPaneSizes = layoutPaneSizes
        value.documents = documents; value.folders = folders; value.vaultPreferences = vaultPreferences
        value.scratchDrafts = scratchDrafts
        value.noteAttachments = noteAttachments.isEmpty ? nil : noteAttachments
        value.history = history; value.httpRequests = httpRequests; value.hostProfiles = hostProfiles.isEmpty ? nil : hostProfiles
        value.drafts = drafts.mapValues(\.record); return value
    }
    func restore(_ value: WorkspaceSnapshot) {
        editorRestoreGeneration += 1
        suppressDocumentSync = true; defer { suppressDocumentSync = false }
        selected = Catalog.tool(value.selectedTool).id; recent = value.recent; pinned = value.pinned
        customGroups = value.customGroups; showNavigationSeparators = value.showNavigationSeparators
        showRecent = value.showRecent
        sidebarWidth = value.sidebarWidth
        hideNavigationTitles = value.hideNavigationTitles
        hiddenNavigationToolIds = value.hiddenNavigationToolIds
        layoutPaneSizes = value.layoutPaneSizes
        documents = value.documents; history = value.history; httpRequests = value.httpRequests ?? []
        hostProfiles = value.hostProfiles ?? []
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
        do {
            try repository.save(snapshot())
            try syncVaultFilesystem()
            savePending = false
        } catch { self.error = "工作区保存失败：" + error.localizedDescription }
    }
    func syncVaultFilesystem() throws {
        vaultMirrorSuppressedUntil = Date().addingTimeInterval(0.6)
        try VaultFilesystemSync.sync(
            vault: vault,
            workspace: repository.directory,
            noteAttachments: noteAttachments,
            readNoteAttachment: { try self.repository.attachmentRepository.read($0) }
        )
        vaultDiskBaseline["json"] = VaultFilesystemSync.baseline(for: "json", vault: vault)
        vaultDiskBaseline["quickNote"] = VaultFilesystemSync.baseline(for: "quickNote", vault: vault)
    }
    func refreshVaultFromDisk(toolID: String) {
        guard !persistenceBlocked else { return }
        do {
            let baseline = vaultDiskBaseline[toolID] ?? [:]
            let changes = try VaultFilesystemSync.detectChanges(toolID: toolID, vault: vault, workspace: repository.directory, baseline: baseline)
            guard !changes.isEmpty else { return }
            var next = vault
            var conflicts: [String] = []
            for change in changes {
                switch change.kind {
                case .updated:
                    guard let id = change.documentID, let content = change.content else { continue }
                    if hasUnmirroredEdits(toolID: toolID, documentID: id) { conflicts.append(change.relativePath); continue }
                    guard let index = next.documents.firstIndex(where: { $0.id == id }) else { continue }
                    next.documents[index].content = content
                    next.documents[index].modified = Date()
                    if draft(toolID).documentID == id {
                        suppressDocumentSync = true; draft(toolID).input = content; suppressDocumentSync = false
                    }
                case .imported:
                    guard let content = change.content else { continue }
                    _ = try next.importDocuments([DocumentImportItem(relativePath: change.relativePath, content: content)], toolID: toolID, parent: nil)
                case .deleted:
                    guard let id = change.documentID else { continue }
                    if hasUnmirroredEdits(toolID: toolID, documentID: id) { conflicts.append(change.relativePath); continue }
                    _ = next.delete(id)
                    if draft(toolID).documentID == id, !draft(toolID).busy, draft(toolID).noteOperationID == nil {
                        draft(toolID).documentID = nil
                        draft(toolID).status = "磁盘上的文件已删除，当前内容保留为草稿"
                    }
                }
            }
            try commitVault(next)
            try syncVaultFilesystem()
            if !conflicts.isEmpty {
                error = "磁盘变更未自动合并（编辑器中有未同步修改）：" + conflicts.joined(separator: "、")
            } else {
                draft(toolID).status = "已从磁盘刷新 \(changes.count) 项"
            }
        } catch { self.error = "读取磁盘文档库失败：" + error.localizedDescription }
    }
    private func hasUnmirroredEdits(toolID: String, documentID: UUID) -> Bool {
        guard let doc = documents.first(where: { $0.id == documentID && $0.toolID == toolID }) else { return false }
        let path = vault.path(of: documentID)
        let baseline = vaultDiskBaseline[toolID]?[path] ?? doc.content
        return doc.content != baseline
    }
    func revealVaultEntry(_ entryID: UUID) throws {
        try syncVaultFilesystem()
        guard let url = VaultFilesystemSync.entryURL(entryID: entryID, vault: vault, workspace: repository.directory) else {
            throw ToolError("无法在 Finder 中定位该项目。")
        }
        guard FileManager.default.fileExists(atPath: url.path) else { throw ToolError("磁盘副本尚未生成，请先保存文档。") }
        NSWorkspace.shared.activateFileViewerSelecting([url])
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
