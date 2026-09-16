import AppKit
import MooToolNextCore

@MainActor
enum UpdateChecker {
    private static var isTestRun: Bool {
        CommandLine.arguments.contains("--smoke-test") || CommandLine.arguments.contains("--verify-workspace") || CommandLine.arguments.contains("--verify-bundle")
    }

    static func currentVersion() -> String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Development"
    }

    static func runAutomaticIfEnabled() {
        guard !isTestRun else { return }
        let enabled = nativeDefaults.object(forKey: "general.autoCheckUpdates") == nil ? true : nativeDefaults.bool(forKey: "general.autoCheckUpdates")
        guard enabled else { return }
        Task { await run(automatic: true) }
    }

    static func runManual() {
        Task { await run(automatic: false) }
    }

    private static func run(automatic: Bool) async {
        if !automatic { try? await Task.sleep(for: .milliseconds(100)) }
        if automatic { try? await Task.sleep(for: .seconds(2.5)) }
        let language = AppLanguage.normalized(nativeDefaults.string(forKey: "general.language"))
        let result = await GitHubUpdateService.check(currentVersion: currentVersion())
        switch result.status {
        case .available:
            if automatic && nativeDefaults.bool(forKey: "general.autoDownloadUpdates") {
                NSWorkspace.shared.open(result.releaseURL ?? GitHubUpdateService.releasesPage)
                return
            }
            presentAvailable(result, language: language, automatic: automatic)
        case .upToDate where !automatic:
            presentInfo(AppLocalization.string("update.upToDate", language: language))
        case .failed where !automatic:
            presentInfo(AppLocalization.string("update.failed", language: language))
        default:
            break
        }
    }

    private static func presentAvailable(_ result: UpdateCheckResult, language: AppLanguage, automatic: Bool) {
        let alert = NSAlert()
        alert.alertStyle = .informational
        alert.messageText = AppLocalization.string("update.available.title", language: language)
        alert.informativeText = "\(result.currentVersion) → \(result.latestVersion ?? "?")"
        alert.addButton(withTitle: AppLocalization.string("update.openDownload", language: language))
        alert.addButton(withTitle: AppLocalization.string("update.later", language: language))
        if alert.runModal() == .alertFirstButtonReturn {
            let url = result.releaseURL ?? GitHubUpdateService.releasesPage
            NSWorkspace.shared.open(url)
        }
    }

    private static func presentInfo(_ message: String) {
        let alert = NSAlert()
        alert.alertStyle = .informational
        alert.messageText = message
        alert.runModal()
    }
}
