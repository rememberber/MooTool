import SwiftUI
import MooToolNextCore

struct VaultGitDialog: View {
    let toolID: String
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @Environment(\.dismiss) private var dismiss
    @AppStorage("vaultGitRemote", store: nativeDefaults) private var savedRemote = ""
    @State private var status: VaultGitStatus?
    @State private var history: [VaultGitCommit] = []
    @State private var remote = ""
    @State private var commitMessage = ""
    @State private var busy = false
    @State private var notice: String?
    @State private var noticeIsError = false
    @State private var tab = "changes"
    @State private var selection = ""
    @State private var diff: VaultGitDiffResult?
    @State private var diffFile: VaultGitDiffFile?

    private func loc(_ key: String, _ args: [String: String] = [:]) -> String {
        var value = AppLocalization.string(key, language: language)
        for (placeholder, replacement) in args {
            value = value.replacingOccurrences(of: "{\(placeholder)}", with: replacement)
        }
        return value
    }

    private var dialogTitle: String {
        loc(toolID == "json" ? "json.git.title" : "quickNote.git.title")
    }

    private func makeService() -> VaultGitService {
        VaultGitService(rootDirectory: VaultFilesystemSync.root(toolID: toolID, workspace: store.repository.directory))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Label(dialogTitle, systemImage: "arrow.triangle.branch").font(.title2.bold())
                Spacer()
                Button(loc("common.done")) { dismiss() }.keyboardShortcut(.cancelAction)
            }
            if let status {
                if !status.available {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(loc("git.unavailable")).foregroundStyle(.secondary)
                        Text(loc("git.unavailableMac")).foregroundStyle(.secondary)
                    }
                } else if !status.repository {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(loc("git.noRepo")).foregroundStyle(.secondary)
                        Text(loc("git.noRepoHint")).foregroundStyle(.secondary)
                        Button(loc("git.init")) { run(VaultGitAction.initRepo) }.disabled(busy)
                    }
                } else {
                    toolbar(status)
                    TextField(loc("git.remotePlaceholder"), text: $remote).textFieldStyle(.roundedBorder)
                    HStack {
                        Button(loc("git.saveRemote")) { run(.configureRemote, remote: remote); savedRemote = remote }.disabled(busy)
                        if !status.remote.isEmpty {
                            Button(loc("git.removeRemote")) { remote = ""; run(.configureRemote, remote: ""); savedRemote = "" }.disabled(busy)
                        }
                    }
                    HSplitView {
                        browser(status).frame(minWidth: 220, maxWidth: 280)
                        diffPane.frame(minWidth: 320)
                    }.frame(minHeight: 320)
                    HStack {
                        TextField(loc("git.commitMessage"), text: $commitMessage).textFieldStyle(.roundedBorder)
                        Button(loc("git.commit")) { run(.commit, message: commitMessage) }
                            .disabled(busy || commitMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            } else {
                ProgressView(loc("git.loadingStatus"))
            }
            if let notice {
                Text(notice).font(.caption).foregroundStyle(noticeIsError ? .red : .secondary)
            }
        }.padding(24).frame(width: 900, height: 620)
        .onAppear {
            remote = savedRemote
            commitMessage = loc(toolID == "quickNote" ? "quickNote.git.defaultMessage" : "json.git.defaultMessage")
            refresh()
        }
    }

    @ViewBuilder private func toolbar(_ status: VaultGitStatus) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if status.merging {
                HStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.orange)
                    Text(status.operation == "rebase" ? loc("git.rebaseInProgress") : loc("git.mergeInProgress"))
                    if status.conflicts > 0 {
                        Text(loc("git.conflictCount", ["count": "\(status.conflicts)"])).foregroundStyle(.red)
                    }
                    Spacer()
                    Button(loc("git.abortShort")) { run(.abortMerge) }.disabled(busy)
                    if status.conflicts == 0 {
                        Button(loc("git.continue")) { run(.continueOperation) }.disabled(busy)
                    }
                }.font(.subheadline)
            }
            HStack(spacing: 10) {
                Text(loc("git.branch", ["branch": status.branch])).font(.headline)
                if status.ahead > 0 || status.behind > 0 {
                    Text("↑\(status.ahead) ↓\(status.behind)").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Button(loc("git.refresh"), action: refresh).disabled(busy)
                Button(loc("git.fetch")) { run(.fetch) }.disabled(busy || status.remote.isEmpty)
                Button(loc("git.pull")) { run(.pull) }.disabled(busy || status.remote.isEmpty || status.merging)
                Button(loc("git.push")) { run(.push) }.disabled(busy || status.remote.isEmpty)
            }
        }
    }

    @ViewBuilder private func browser(_ status: VaultGitStatus) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Picker("", selection: $tab) {
                Text(loc("git.changesCount", ["count": "\(status.changes.count)"])).tag("changes")
                Text(loc("git.history")).tag("history")
            }.pickerStyle(.segmented).labelsHidden()
            .onChange(of: tab) { selection = ""; diff = nil; diffFile = nil }
            List(selection: Binding(get: { selection }, set: { value in if let value { select(value) } })) {
                if tab == "changes" {
                    if status.changes.isEmpty { Text(loc("git.emptyChanges")).foregroundStyle(.secondary) }
                    ForEach(status.changes, id: \.path) { change in
                        HStack {
                            Text(change.status).font(.system(.caption, design: .monospaced)).foregroundStyle(.secondary)
                            Text(change.path).lineLimit(1)
                            if change.conflict { Text(loc("git.conflict")).font(.caption2).foregroundStyle(.red) }
                        }.tag(change.path)
                    }
                } else {
                    if history.isEmpty { Text(loc("git.emptyHistory")).foregroundStyle(.secondary) }
                    ForEach(history, id: \.hash) { item in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.message).lineLimit(1)
                            Text("\(item.shortHash) · \(item.author)").font(.caption).foregroundStyle(.secondary)
                        }.tag(item.hash)
                    }
                }
            }
            if tab == "changes", let change = status.changes.first(where: { $0.path == selection }) {
                if change.conflict {
                    HStack {
                        Button(loc("git.ours")) { run(.resolveConflict, path: change.path, strategy: .ours) }.disabled(busy)
                        Button(loc("git.theirs")) { run(.resolveConflict, path: change.path, strategy: .theirs) }.disabled(busy)
                    }
                } else {
                    Button(loc("git.discardPath"), role: .destructive) { run(.discard, path: change.path) }.disabled(busy)
                }
            }
        }
    }

    @ViewBuilder private var diffPane: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(diffFile?.path ?? loc("git.diff")).font(.headline).lineLimit(1)
            if let diffFile {
                if diffFile.kind != "text" {
                    Text(
                        diffFile.kind == "binary" ? loc("git.diffBinary")
                            : diffFile.kind == "too-large" ? loc("git.diffTooLarge")
                            : loc("git.diffMissing")
                    ).foregroundStyle(.secondary)
                } else {
                    HSplitView {
                        EditorPane(title: loc("git.diffBefore"), text: .constant(diffFile.before), editable: false).frame(minWidth: 140)
                        EditorPane(title: loc("git.diffAfter"), text: .constant(diffFile.after), editable: false).frame(minWidth: 140)
                    }
                }
            } else {
                Text(selection.isEmpty ? loc("git.diffEmpty") : loc("git.diffLoading"))
                    .foregroundStyle(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private func select(_ value: String) {
        selection = value
        busy = true
        Task {
            defer { busy = false }
            do {
                let service = makeService()
                if tab == "history" {
                    diff = try await Task.detached { try service.diff(commit: value) }.value
                } else {
                    diff = try await Task.detached { try service.diff(path: value) }.value
                }
                diffFile = diff?.files.first
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
                diff = nil
                diffFile = nil
            }
        }
    }

    private func refresh() {
        busy = true
        Task {
            defer { busy = false }
            do {
                try store.syncVaultFilesystem()
                let service = makeService()
                status = try await Task.detached { try service.status() }.value
                history = try await Task.detached { try service.history() }.value
                if remote.isEmpty, let value = status?.remote, !value.isEmpty { remote = value }
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
            }
        }
    }

    private func run(_ action: VaultGitAction, message: String? = nil, remote: String? = nil, path: String? = nil, strategy: VaultGitConflictStrategy? = nil) {
        busy = true
        Task {
            defer { busy = false }
            do {
                try store.syncVaultFilesystem()
                let input = VaultGitActionInput(action: action, message: message, remote: remote, path: path, strategy: strategy)
                let service = makeService()
                let result = try await Task.detached { try service.perform(input) }.value
                notice = result.message
                noticeIsError = !result.success
                let refreshDisk = result.success && [.discard, .pull, .abortMerge, .resolveConflict, .continueOperation].contains(action)
                if refreshDisk { store.refreshVaultFromDisk(toolID: toolID) }
                if action == .discard || action == .abortMerge { selection = ""; diff = nil; diffFile = nil }
                refresh()
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
            }
        }
    }
}
