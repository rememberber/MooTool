import Foundation
import MooToolNextCore

extension AppStore {
    func startVaultAutoPullTimers() {
        guard vaultAutoPullTimer == nil else { return }
        let timer = DispatchSource.makeTimerSource(queue: DispatchQueue.main)
        timer.schedule(deadline: .now() + .seconds(60), repeating: .seconds(60))
        timer.setEventHandler { [weak self] in self?.evaluateVaultAutoPull() }
        timer.resume()
        vaultAutoPullTimer = timer
    }

    private func evaluateVaultAutoPull() {
        let minutes = nativeDefaults.integer(forKey: "vaultAutoPullMinutes")
        guard minutes > 0, !persistenceBlocked else { return }
        let interval = TimeInterval(minutes * 60)
        let now = Date().timeIntervalSince1970
        for toolID in ["json", "quickNote"] {
            let last = vaultAutoPullLastRun[toolID] ?? 0
            guard now - last >= interval else { continue }
            vaultAutoPullLastRun[toolID] = now
            Task { await pullVaultIfIdle(toolID: toolID) }
        }
    }

    func pullVaultIfIdle(toolID: String) async {
        guard !hasUnsavedVaultEdits(toolID: toolID) else { return }
        do { try syncVaultFilesystem() } catch { return }
        let directory = repository.directory
        let result = await Task.detached {
            let service = VaultGitService(rootDirectory: VaultFilesystemSync.root(toolID: toolID, workspace: directory))
            do {
                let status = try service.status()
                guard status.repository, !status.remote.isEmpty, !status.merging, status.conflicts == 0, status.changes.isEmpty else { return false }
                let pull = try service.perform(VaultGitActionInput(action: .pull))
                guard pull.success else { return false }
                let updated = try service.status()
                return updated.merging || updated.conflicts > 0 || pull.success
            } catch { return false }
        }.value
        if result { refreshVaultFromDisk(toolID: toolID) }
    }
}
