import SwiftUI
import MooToolNextCore

extension AppStore {
    var vault: DocumentVault { DocumentVault(documents: documents, folders: folders) }
    func vaultPreference(_ id: String) -> VaultPreferences {
        if let value = vaultPreferences[id] { return value }
        var value = VaultPreferences(); value.sort = id == "quickNote" ? .modified : .name
        value.selectedEntryID = draft(id).documentID
        if let selected = value.selectedEntryID { value.expanded.formUnion(vault.ancestors(of: selected)) }
        return value
    }
    func updateVaultPreference(_ id: String, _ change: (inout VaultPreferences) -> Void) {
        var value = vaultPreference(id); change(&value); vaultPreferences[id] = value; scheduleSave()
    }
    func synchronizeDocument(_ toolID: String) {
        guard !suppressDocumentSync, ["json", "quickNote"].contains(toolID), let id = draft(toolID).documentID,
              let index = documents.firstIndex(where: { $0.id == id && $0.toolID == toolID }) else { return }
        let draft = draft(toolID)
        if documents[index].content != draft.input { documents[index].content = draft.input; documents[index].modified = Date() }
        if toolID == "json" { documents[index].output = draft.output; documents[index].query = draft.option }
        if toolID == "quickNote", documents[index].noteOptions != draft.noteOptions { documents[index].noteOptions = draft.noteOptions; documents[index].modified = Date() }
    }
    func openDocument(_ id: UUID) throws {
        guard let file = documents.first(where: { $0.id == id }) else { throw ToolError("文档已不存在。") }
        let draft = draft(file.toolID)
        if draft.documentID != id {
            if draft.documentID == nil { scratchDrafts[file.toolID] = draft.record }
            synchronizeDocument(file.toolID)
            if file.toolID == "quickNote" { draft.noteTask?.cancel(); draft.noteTask = nil; draft.noteOperationID = nil; draft.busy = false }
            suppressDocumentSync = true
            draft.documentID = file.id; draft.input = file.content; draft.output = file.output ?? ""; draft.option = file.query ?? ""
            draft.inputEditor = file.inputEditor; draft.outputEditor = file.outputEditor; draft.error = nil; draft.status = ""
            if file.toolID == "quickNote" { draft.noteOptions = file.noteOptions }
            suppressDocumentSync = false
        }
        updateVaultPreference(file.toolID) { $0.selectedEntryID = id; $0.expanded.formUnion(vault.ancestors(of: id)) }
    }
    func openScratch(_ toolID: String) {
        if draft(toolID).documentID == nil { return }
        synchronizeDocument(toolID)
        suppressDocumentSync = true; draft(toolID).apply(scratchDrafts[toolID] ?? DraftRecord()); suppressDocumentSync = false
        updateVaultPreference(toolID) { $0.selectedEntryID = nil }
    }
    func restoreDraft(_ toolID: String, record: DraftRecord) {
        editorRestoreGeneration += 1
        suppressDocumentSync = true; draft(toolID).apply(record); suppressDocumentSync = false
        synchronizeDocument(toolID)
        if let id = draft(toolID).documentID { updateVaultPreference(toolID) { $0.selectedEntryID = id; $0.expanded.formUnion(vault.ancestors(of: id)) } }
    }
    func commitVault(_ value: DocumentVault) throws {
        try value.validate()
        var candidate = snapshot(); candidate.documents = value.documents; candidate.folders = value.folders
        _ = try WorkspaceRepository.encode(candidate)
        documents = value.documents; folders = value.folders; scheduleSave()
    }
    @discardableResult func createVaultDocument(_ toolID: String, name: String, parent: UUID?, content: String = "") throws -> UUID {
        var next = vault; let id = try next.createDocument(toolID: toolID, name: name, content: content, parent: parent)
        try commitVault(next); try openDocument(id); return id
    }
    @discardableResult func createVaultFolder(_ toolID: String, name: String, parent: UUID?) throws -> UUID {
        var next = vault; let id = try next.createFolder(toolID: toolID, name: name, parent: parent); try commitVault(next)
        updateVaultPreference(toolID) { $0.selectedEntryID = id; $0.expanded.formUnion(vault.ancestors(of: id) + [id]) }; return id
    }
    func moveVaultEntry(_ id: UUID, to parent: UUID?) throws {
        var next = vault; try next.move(id, to: parent); try commitVault(next)
        if let toolID = vault.tool(of: id) { updateVaultPreference(toolID) { $0.expanded.formUnion(vault.ancestors(of: id)); $0.selectedEntryID = id } }
    }
    func renameVaultEntry(_ id: UUID, name: String) throws { var next = vault; try next.rename(id, to: name); try commitVault(next) }
    func duplicateVaultDocument(_ id: UUID) throws {
        var next = vault; let newID = try next.duplicate(id); try commitVault(next); try openDocument(newID)
    }
    func deleteVaultEntry(_ id: UUID) throws {
        var next = vault; let removed = next.delete(id); try commitVault(next)
        for toolID in ["json", "quickNote"] {
            let draft = draft(toolID)
            if let active = draft.documentID, removed.contains(active) { draft.documentID = nil; draft.status = "已删除文档，当前内容保留为草稿" }
            updateVaultPreference(toolID) { value in
                value.expanded.subtract(removed)
                if let selected = value.selectedEntryID, removed.contains(selected) { value.selectedEntryID = nil }
            }
        }
    }
    func importVaultDocuments(_ items: [DocumentImportItem], toolID: String, parent: UUID?) throws -> [UUID] {
        var next = vault; let ids = try next.importDocuments(items, toolID: toolID, parent: parent); try commitVault(next)
        if let first = ids.first { try openDocument(first) }; return ids
    }
    func editorPersistence(_ toolID: String, output: Bool = false) -> EditorPersistence {
        let draft = draft(toolID), documentID = draft.documentID
        let restoreGeneration = editorRestoreGeneration
        return EditorPersistence(identity: toolID + ":" + (documentID?.uuidString ?? "draft") + ":\(draft.editorRevision)" + (output ? ":output" : ":input"),
                                 state: (output ? draft.outputEditor : draft.inputEditor) ?? EditorViewState()) { [weak self] value in
            guard let self, self.editorRestoreGeneration == restoreGeneration else { return }
            // The captured ID ensures a late selection/scroll event cannot update a newly opened document.
            if let documentID, let index = self.documents.firstIndex(where: { $0.id == documentID && $0.toolID == toolID }) {
                if output { self.documents[index].outputEditor = value } else { self.documents[index].inputEditor = value }
            }
            if documentID == nil, self.draft(toolID).documentID != nil {
                if output { self.scratchDrafts[toolID]?.outputEditor = value } else { self.scratchDrafts[toolID]?.inputEditor = value }
            }
            if self.draft(toolID).documentID == documentID {
                if output { self.draft(toolID).outputEditor = value } else { self.draft(toolID).inputEditor = value }
            }
            self.scheduleSave()
        }
    }
}
