import SwiftUI
import MooToolNextCore

struct VaultGitDialog: View {
    let toolID: String
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @AppStorage("vaultGitRemote", store: nativeDefaults) private var savedRemote = ""
    @State private var status: VaultGitStatus?
    @State private var history: [VaultGitCommit] = []
    @State private var remote = ""
    @State private var commitMessage = ""
    @State private var busy = false
    @State private var notice: String?
    @State private var tab = "changes"
    @State private var selection = ""
    @State private var diff: VaultGitDiffResult?
    @State private var diffFile: VaultGitDiffFile?
    private func makeService() -> VaultGitService {
        VaultGitService(rootDirectory: VaultFilesystemSync.root(toolID: toolID, workspace: store.repository.directory))
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Label(toolID == "json" ? "JSON 文档库 Git" : "随手记 Git", systemImage: "arrow.triangle.branch").font(.title2.bold())
                Spacer()
                Button("完成") { dismiss() }.keyboardShortcut(.cancelAction)
            }
            if let status {
                if !status.available {
                    Text("未检测到本机 git 命令。请安装 Xcode Command Line Tools。").foregroundStyle(.secondary)
                } else if !status.repository {
                    Text("当前文档库目录尚未初始化 Git。保存文档后会同步到磁盘，可在此初始化并提交。").foregroundStyle(.secondary)
                    Button("初始化 Git") { run(VaultGitAction.initRepo) }.disabled(busy)
                } else {
                    toolbar(status)
                    TextField("远程仓库地址", text: $remote).textFieldStyle(.roundedBorder)
                    HStack {
                        Button("保存远程地址") { run(.configureRemote, remote: remote); savedRemote = remote }.disabled(busy)
                        if !status.remote.isEmpty { Button("删除远程") { remote = ""; run(.configureRemote, remote: ""); savedRemote = "" }.disabled(busy) }
                    }
                    HSplitView {
                        browser(status).frame(minWidth: 220, maxWidth: 280)
                        diffPane.frame(minWidth: 320)
                    }.frame(minHeight: 320)
                    HStack {
                        TextField("提交说明", text: $commitMessage).textFieldStyle(.roundedBorder)
                        Button("提交") { run(.commit, message: commitMessage) }.disabled(busy || commitMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            } else {
                ProgressView("正在读取 Git 状态…")
            }
            if let notice { Text(notice).font(.caption).foregroundStyle(notice.contains("失败") || notice.contains("无效") ? .red : .secondary) }
        }.padding(24).frame(width: 900, height: 620)
        .onAppear {
            remote = savedRemote
            commitMessage = toolID == "quickNote" ? "Update quick notes" : "Update JSON vault"
            refresh()
        }
    }
    @ViewBuilder private func toolbar(_ status: VaultGitStatus) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if status.merging {
                HStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.orange)
                    Text(status.operation == "rebase" ? "变基进行中" : "合并进行中")
                    if status.conflicts > 0 { Text("· \(status.conflicts) 个冲突").foregroundStyle(.red) }
                    Spacer()
                    Button("中止") { run(.abortMerge) }.disabled(busy)
                    if status.conflicts == 0 {
                        Button("继续") { run(.continueOperation) }.disabled(busy)
                    }
                }.font(.subheadline)
            }
            HStack(spacing: 10) {
                Text("分支 \(status.branch)").font(.headline)
                if status.ahead > 0 || status.behind > 0 { Text("↑\(status.ahead) ↓\(status.behind)").font(.caption).foregroundStyle(.secondary) }
                Spacer()
                Button("刷新", action: refresh).disabled(busy)
                Button("Fetch") { run(.fetch) }.disabled(busy || status.remote.isEmpty)
                Button("Pull") { run(.pull) }.disabled(busy || status.remote.isEmpty || status.merging)
                Button("Push") { run(.push) }.disabled(busy || status.remote.isEmpty)
            }
        }
    }
    @ViewBuilder private func browser(_ status: VaultGitStatus) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Picker("列表", selection: $tab) {
                Text("变更 \(status.changes.count)").tag("changes")
                Text("历史").tag("history")
            }.pickerStyle(.segmented).labelsHidden()
            .onChange(of: tab) { selection = ""; diff = nil; diffFile = nil }
            List(selection: Binding(get: { selection }, set: { value in if let value { select(value) } })) {
                if tab == "changes" {
                    if status.changes.isEmpty { Text("工作区很干净").foregroundStyle(.secondary) }
                    ForEach(status.changes, id: \.path) { change in
                        HStack {
                            Text(change.status).font(.system(.caption, design: .monospaced)).foregroundStyle(.secondary)
                            Text(change.path).lineLimit(1)
                            if change.conflict { Text("冲突").font(.caption2).foregroundStyle(.red) }
                        }.tag(change.path)
                    }
                } else {
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
                        Button("保留本地 (ours)") { run(.resolveConflict, path: change.path, strategy: .ours) }.disabled(busy)
                        Button("保留远端 (theirs)") { run(.resolveConflict, path: change.path, strategy: .theirs) }.disabled(busy)
                    }
                } else {
                    Button("丢弃此变更", role: .destructive) { run(.discard, path: change.path) }.disabled(busy)
                }
            }
        }
    }
    @ViewBuilder private var diffPane: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(diffFile?.path ?? "变更详情").font(.headline).lineLimit(1)
            if let diffFile {
                if diffFile.kind != "text" {
                    Text(diffFile.kind == "binary" ? "二进制文件" : diffFile.kind == "too-large" ? "文件过大，无法预览" : "文件缺失").foregroundStyle(.secondary)
                } else {
                    HSplitView {
                        EditorPane(title: "之前", text: .constant(diffFile.before), editable: false).frame(minWidth: 140)
                        EditorPane(title: "之后", text: .constant(diffFile.after), editable: false).frame(minWidth: 140)
                    }
                }
            } else {
                Text(selection.isEmpty ? "选择变更或提交查看 Diff" : "正在加载…").foregroundStyle(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
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
            } catch { notice = error.localizedDescription; diff = nil; diffFile = nil }
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
            } catch { notice = error.localizedDescription }
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
                let refreshDisk = result.success && [.discard, .pull, .abortMerge, .resolveConflict, .continueOperation].contains(action)
                if refreshDisk { store.refreshVaultFromDisk(toolID: toolID) }
                if action == .discard || action == .abortMerge { selection = ""; diff = nil; diffFile = nil }
                refresh()
            } catch { notice = error.localizedDescription }
        }
    }
}
