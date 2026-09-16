import SwiftUI
import UniformTypeIdentifiers
import MooToolNextCore

struct MigrationSettingsPanel: View {
    @Environment(AppStore.self) private var store
    @AppStorage("appearance", store: nativeDefaults) private var appearance = "system"
    @AppStorage("editorSize", store: nativeDefaults) private var editorSize = 13.0
    @AppStorage("wrapLines", store: nativeDefaults) private var wrapLines = true
    @AppStorage("network.proxyEnabled", store: nativeDefaults) private var proxyEnabled = false
    @AppStorage("network.proxyHost", store: nativeDefaults) private var proxyHost = ""
    @AppStorage("network.proxyPort", store: nativeDefaults) private var proxyPort = 8080
    @AppStorage("network.proxyUsername", store: nativeDefaults) private var proxyUsername = ""
    @AppStorage("network.proxyPassword", store: nativeDefaults) private var proxyPassword = ""
    @AppStorage("vaultAutoCommit", store: nativeDefaults) private var vaultAutoCommit = true
    @AppStorage("vaultAutoCommitIdleSeconds", store: nativeDefaults) private var vaultAutoCommitIdleSeconds = 30
    @AppStorage("vaultAutoCommitInactiveSeconds", store: nativeDefaults) private var vaultAutoCommitInactiveSeconds = 120
    @AppStorage("vaultAutoPullMinutes", store: nativeDefaults) private var vaultAutoPullMinutes = 0
    @State private var storePath = ElectronStoreImport.defaultStoreURL()?.path ?? ""
    @State private var preview: ElectronImportPreview?
    @State private var busy = false
    @State private var error: String?
    @State private var notice: String?
    @State private var confirmImport = false
    private var knownToolIDs: Set<String> { Set(Catalog.tools.map(\.id)) }

    var body: some View {
        Form {
            Text("从 next Electron 的 `mootool-next.json` 合并工作台布局（侧栏、自定义分组、分栏宽度）、HTTP 代理、编辑器字号/换行与文档库 Git 自动检查点。不会导入文档正文、HTTP 集合或加密密钥。")
                .font(.caption).foregroundStyle(.secondary).padding(.vertical, 4)
            HStack {
                TextField("Electron 数据文件", text: $storePath)
                Button("选择…") { chooseFile() }
            }
            HStack {
                Button("扫描") { scan() }.disabled(busy || storePath.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Button("导入布局与代理…") { confirmImport = true }
                    .disabled(busy || preview == nil)
            }
            if busy { ProgressView().controlSize(.small) }
            if let error { Text(error).foregroundStyle(.red).font(.caption) }
            if let notice { Text(notice).foregroundStyle(.secondary).font(.caption) }
            if let preview {
                LabeledContent("自定义分组", value: "\(preview.customGroupCount)")
                LabeledContent("隐藏侧栏工具", value: "\(preview.hiddenToolCount)")
                LabeledContent("分栏键", value: "\(preview.paneKeyCount)")
                ForEach(preview.warnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
            }
            Divider()
            Section("Java 版数据（仅提示）") {
                let root = LegacyJavaDataPaths.defaultDirectory
                LabeledContent("默认目录", value: root.path)
                if LegacyJavaDataPaths.hasLegacyInstall(at: root) {
                    Text("检测到旧版 SQLite 数据库。原生版暂不支持一键导入笔记/JSON/收藏；请使用 Compose 桌面版的迁移面板，或通过文档库批量导入 Markdown/JSON 文件。").font(.caption).foregroundStyle(.secondary)
                } else {
                    Text("未在默认目录找到 Java 版数据库。若数据在其他位置，请用 Compose 迁移或手动复制文件到文档库。").font(.caption).foregroundStyle(.secondary)
                }
                Button("在 Finder 中打开…") { NSWorkspace.shared.open(root) }
            }
        }
        .formStyle(.grouped).frame(width: 560, height: 360)
        .onAppear {
            if storePath.isEmpty { storePath = ElectronStoreImport.defaultStoreURL()?.path ?? "" }
        }
        .confirmationDialog("导入 Electron 设置？", isPresented: $confirmImport) {
            Button("合并到当前原生工作区") { importSettings() }
        } message: {
            Text("将覆盖匹配的工作台字段，并写入 HTTP 代理与编辑器偏好。导入前会自动保存当前工作区。")
        }
    }

    private func chooseFile() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.allowedContentTypes = [.json]
        panel.prompt = "选择"
        if !storePath.isEmpty { panel.directoryURL = URL(fileURLWithPath: storePath).deletingLastPathComponent() }
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            storePath = url.path
            preview = nil
            error = nil
            notice = nil
        }
    }

    private func scan() {
        busy = true
        error = nil
        notice = nil
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: storePath.trimmingCharacters(in: .whitespacesAndNewlines))
            preview = try ElectronStoreImport.preview(at: url, knownToolIDs: knownToolIDs, currentPaneSizes: store.layoutPaneSizes)
        } catch {
            preview = nil
            self.error = error.localizedDescription
        }
    }

    private func importSettings() {
        busy = true
        error = nil
        notice = nil
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: storePath.trimmingCharacters(in: .whitespacesAndNewlines))
            let patch = try ElectronStoreImport.loadPatch(at: url, knownToolIDs: knownToolIDs, merging: store.layoutPaneSizes)
            var snapshot = store.snapshot()
            try ElectronStoreImport.apply(patch, to: &snapshot)
            store.restore(snapshot)
            if let value = patch.proxyEnabled { proxyEnabled = value }
            if let value = patch.proxyHost { proxyHost = value }
            if let value = patch.proxyPort { proxyPort = value }
            if let value = patch.proxyUsername { proxyUsername = value }
            if let value = patch.proxyPassword { proxyPassword = value }
            if let value = patch.editorFontSize { editorSize = value.clamped(to: 11...22) }
            if let value = patch.wrapLines { wrapLines = value }
            if let value = patch.vaultAutoCommit { vaultAutoCommit = value }
            if let value = patch.vaultAutoCommitIdleSeconds { vaultAutoCommitIdleSeconds = value }
            if let value = patch.vaultAutoCommitInactiveSeconds { vaultAutoCommitInactiveSeconds = value }
            if let value = patch.vaultAutoPullMinutes { vaultAutoPullMinutes = value }
            store.saveNow()
            notice = "已合并 Electron 工作台与网络/编辑器偏好。"
            preview = try ElectronStoreImport.preview(at: url, knownToolIDs: knownToolIDs, currentPaneSizes: store.layoutPaneSizes)
        } catch {
            self.error = error.localizedDescription
        }
    }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double { min(max(self, range.lowerBound), range.upperBound) }
}
