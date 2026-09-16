import SwiftUI
import MooToolNextCore

extension AppStore {
    func startVaultCheckpointSchedulers() {
        guard jsonCheckpointScheduler == nil else { return }
        let defaults = nativeDefaults
        let idleSeconds = {
            let value = defaults.integer(forKey: "vaultAutoCommitIdleSeconds")
            return max(5, min(3600, value == 0 ? 30 : value))
        }
        let inactiveSeconds = {
            let value = defaults.integer(forKey: "vaultAutoCommitInactiveSeconds")
            return max(5, min(3600, value == 0 ? 120 : value))
        }
        let enabled = {
            defaults.object(forKey: "vaultAutoCommit") == nil ? true : defaults.bool(forKey: "vaultAutoCommit")
        }
        jsonCheckpointScheduler = VaultGitCheckpointScheduler(options: .init(
            enabled: enabled,
            hasUnsavedEditorChanges: { [weak self] in await MainActor.run { self?.hasUnsavedVaultEdits(toolID: "json") ?? true } },
            idleMilliseconds: { idleSeconds() * 1000 },
            inactiveMilliseconds: { inactiveSeconds() * 1000 },
            checkpoint: { [weak self] message in await self?.runVaultCheckpoint(toolID: "json", message: message) ?? VaultGitActionResult(success: false, message: "应用未就绪") }
        ))
        quickNoteCheckpointScheduler = VaultGitCheckpointScheduler(options: .init(
            enabled: enabled,
            hasUnsavedEditorChanges: { [weak self] in await MainActor.run { self?.hasUnsavedVaultEdits(toolID: "quickNote") ?? true } },
            idleMilliseconds: { idleSeconds() * 1000 },
            inactiveMilliseconds: { inactiveSeconds() * 1000 },
            checkpoint: { [weak self] message in await self?.runVaultCheckpoint(toolID: "quickNote", message: message) ?? VaultGitActionResult(success: false, message: "应用未就绪") }
        ))
        jsonCheckpointScheduler?.start()
        quickNoteCheckpointScheduler?.start()
    }

    func recordVaultGitActivity(_ toolID: String, message: String) {
        switch toolID {
        case "json": jsonCheckpointScheduler?.recordActivity(message)
        case "quickNote": quickNoteCheckpointScheduler?.recordActivity(message)
        default: break
        }
    }

    func setVaultCheckpointWindowActive(_ active: Bool) {
        jsonCheckpointScheduler?.setWindowActive(active)
        quickNoteCheckpointScheduler?.setWindowActive(active)
    }

    func hasUnsavedVaultEdits(toolID: String) -> Bool {
        if savePending { return true }
        guard let id = draft(toolID).documentID,
              let document = documents.first(where: { $0.id == id && $0.toolID == toolID }) else { return false }
        let current = draft(toolID)
        if document.content != current.input { return true }
        if toolID == "quickNote", document.noteOptions != current.noteOptions { return true }
        return false
    }

    func runVaultCheckpoint(toolID: String, message: String) async -> VaultGitActionResult {
        do {
            try await MainActor.run { try syncVaultFilesystem() }
        } catch {
            return VaultGitActionResult(success: false, message: error.localizedDescription)
        }
        let directory = repository.directory
        let remote = nativeDefaults.string(forKey: "vaultGitRemote")
        return await Task.detached {
            let service = VaultGitService(
                rootDirectory: VaultFilesystemSync.root(toolID: toolID, workspace: directory),
                credentials: VaultGitCredentials(username: NSFullUserName())
            )
            if let remote, !remote.isEmpty { _ = try? service.perform(VaultGitActionInput(action: .configureRemote, remote: remote)) }
            do { return try service.automaticCheckpoint(message) }
            catch { return VaultGitActionResult(success: false, message: error.localizedDescription) }
        }.value
    }
}
