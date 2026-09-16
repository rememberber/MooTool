import SwiftUI
import MooToolNextCore

private enum SettingsCategory: String, CaseIterable, Identifiable {
    case sidebar, proxy, appearance, vault, migration, backup, shortcuts, about
    var id: String { rawValue }
    var title: String {
        switch self {
        case .sidebar: return "侧边栏"
        case .proxy: return "HTTP 代理"
        case .appearance: return "外观"
        case .vault: return "文档库 Git"
        case .migration: return "数据迁移"
        case .backup: return "工作区与备份"
        case .shortcuts: return "快捷键"
        case .about: return "关于"
        }
    }
    var symbol: String {
        switch self {
        case .sidebar: return "sidebar.left"
        case .proxy: return "network"
        case .appearance: return "paintbrush"
        case .vault: return "arrow.triangle.branch"
        case .migration: return "arrow.down.doc"
        case .backup: return "externaldrive"
        case .shortcuts: return "command"
        case .about: return "info.circle"
        }
    }
}

struct SettingsView: View {
    @Environment(AppStore.self) private var store
    @AppStorage("appearance", store: nativeDefaults) private var appearance = "system"
    @AppStorage("editorSize", store: nativeDefaults) private var editorSize = 13.0
    @AppStorage("wrapLines", store: nativeDefaults) private var wrapLines = true
    @AppStorage("vaultAutoCommit", store: nativeDefaults) private var vaultAutoCommit = true
    @AppStorage("vaultAutoCommitIdleSeconds", store: nativeDefaults) private var vaultAutoCommitIdleSeconds = 30
    @AppStorage("vaultAutoCommitInactiveSeconds", store: nativeDefaults) private var vaultAutoCommitInactiveSeconds = 120
    @AppStorage("vaultAutoPullMinutes", store: nativeDefaults) private var vaultAutoPullMinutes = 0
    @AppStorage("network.proxyEnabled", store: nativeDefaults) private var proxyEnabled = false
    @AppStorage("network.proxyHost", store: nativeDefaults) private var proxyHost = ""
    @AppStorage("network.proxyPort", store: nativeDefaults) private var proxyPort = 8080
    @AppStorage("network.proxyUsername", store: nativeDefaults) private var proxyUsername = ""
    @AppStorage("network.proxyPassword", store: nativeDefaults) private var proxyPassword = ""
    @State private var category: SettingsCategory = .sidebar
    @State private var pendingBackup: WorkspaceSnapshot?
    @State private var confirmRestore = false
    @State private var backupBusy = false
    @State private var customGroupsOpen = false
    @State private var navigationToolsOpen = false
    @State private var migrationOpen = false

    var body: some View {
        PersistedHSplit(toolID: "settings-page", defaultLeading: 220, minLeading: 180, maxLeading: 360) {
            List(SettingsCategory.allCases, selection: $category) { item in
                Label(item.title, systemImage: item.symbol).tag(item)
            }.listStyle(.sidebar).frame(minWidth: 160)
        } trailing: {
            Form { detail }.formStyle(.grouped).frame(minWidth: 420)
        }
        .frame(width: 820, height: 580)
        .sheet(isPresented: $customGroupsOpen) { CustomGroupsSettings().environment(store) }
        .sheet(isPresented: $navigationToolsOpen) { NavigationToolVisibilitySettings().environment(store) }
        .sheet(isPresented: $migrationOpen) { MigrationSettingsPanel().environment(store) }
        .confirmationDialog("恢复原生版工作区？", isPresented: $confirmRestore) {
            Button("保存当前副本并恢复") {
                guard let value = pendingBackup else { return }
                do {
                    if store.persistenceBlocked { throw ToolError("当前工作区文件读取失败。请先在 Finder 中备份并处理原文件后重启，再导入备份。") }
                    let backup = store.repository.directory.appendingPathComponent("before-restore-\(UUID().uuidString).json")
                    try FileManager.default.createDirectory(at: store.repository.directory, withIntermediateDirectories: true)
                    try store.repository.backup(store.snapshot()).write(to: backup, options: .atomic)
                    try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: backup.path)
                    let restored = try store.repository.installBackup(value)
                    store.restore(restored); store.saveNow(); pendingBackup = nil
                } catch { FilePanels.error(error) }
            }
        } message: { Text("备份包含 \(pendingBackup?.documents.count ?? 0) 份文档、\(pendingBackup?.noteAttachments?.count ?? 0) 张图片和 \(pendingBackup?.history.count ?? 0) 条历史记录。") }
    }

    @ViewBuilder private var detail: some View {
        switch category {
        case .sidebar:
            Section("侧边栏") {
                LabeledContent("自定义分组", value: store.customGroups.isEmpty ? "未配置" : "\(store.customGroups.count) 个")
                Button("管理自定义分组…") { customGroupsOpen = true }
                LabeledContent("侧栏隐藏工具", value: store.hiddenNavigationToolIds.isEmpty ? "无" : "\(store.hiddenNavigationToolIds.count) 个")
                Button("管理侧栏工具显示…") { navigationToolsOpen = true }
                Toggle("显示分组分隔线", isOn: Binding(
                    get: { store.showNavigationSeparators },
                    set: { store.showNavigationSeparators = $0; store.scheduleSave() }))
                Toggle("仅显示导航图标", isOn: Binding(
                    get: { store.hideNavigationTitles },
                    set: { store.hideNavigationTitles = $0; store.scheduleSave() }))
                Toggle("显示最近使用", isOn: Binding(
                    get: { store.showRecent },
                    set: { store.showRecent = $0; store.scheduleSave() }))
                LabeledContent("侧栏宽度", value: "\(Int(store.sidebarWidth)) pt")
                Text("在主窗口侧栏右缘拖动调整宽度，双击恢复默认 215 pt。").font(.caption).foregroundStyle(.secondary)
            }
        case .proxy:
            Section("HTTP 代理") {
                Toggle("使用 HTTP 代理", isOn: $proxyEnabled)
                TextField("主机", text: $proxyHost).disabled(!proxyEnabled)
                Stepper(value: $proxyPort, in: 1...65535) { Text("端口：\(proxyPort)") }.disabled(!proxyEnabled)
                TextField("用户名（可选）", text: $proxyUsername).disabled(!proxyEnabled)
                SecureField("密码（可选）", text: $proxyPassword).disabled(!proxyEnabled)
                Text("与 Electron 设置相同：HTTP / HTTPS 请求经此代理发送（含 HTTP 工具与网络诊断中的 HTTP 类调用）。不支持 SOCKS 与系统代理自动发现。").font(.caption).foregroundStyle(.secondary)
            }
        case .appearance:
            Section("外观") {
                Picker("主题", selection: $appearance) { Text("跟随系统").tag("system"); Text("浅色").tag("light"); Text("深色").tag("dark") }.pickerStyle(.segmented)
                HStack { Text("编辑器字号"); Slider(value: $editorSize, in: 11...22, step: 1); Text("\(Int(editorSize)) pt").monospacedDigit().frame(width: 42) }
                Toggle("编辑器自动换行", isOn: $wrapLines)
            }
        case .vault:
            Section("文档库 Git") {
                Toggle("编辑空闲后自动创建检查点", isOn: $vaultAutoCommit)
                Stepper(value: $vaultAutoCommitIdleSeconds, in: 5...3600, step: 5) {
                    Text("编辑空闲提交：\(vaultAutoCommitIdleSeconds) 秒")
                }.disabled(!vaultAutoCommit)
                Stepper(value: $vaultAutoCommitInactiveSeconds, in: 5...3600, step: 5) {
                    Text("窗口失焦提交：\(vaultAutoCommitInactiveSeconds) 秒")
                }.disabled(!vaultAutoCommit)
                Stepper(value: $vaultAutoPullMinutes, in: 0...1440, step: 5) {
                    Text(vaultAutoPullMinutes == 0 ? "自动 Pull：关闭" : "自动 Pull：每 \(vaultAutoPullMinutes) 分钟")
                }
                Text("与 Electron 相同：在 JSON / 随手记文档库已初始化 Git 且编辑器已保存到工作区后，空闲或失焦时自动 commit；配置远程后会尝试 push。自动 Pull 仅在无本地未保存修改且工作区干净时执行。").font(.caption).foregroundStyle(.secondary)
            }
        case .migration:
            Section("数据迁移") {
                Button("从 Electron 导入工作台设置…") { migrationOpen = true }
                Text("合并侧栏布局、自定义分组、分栏宽度、HTTP 代理、编辑器字号/换行与文档库 Git 自动检查点。不包含文档、历史或 Java 版 SQLite 数据。").font(.caption).foregroundStyle(.secondary)
            }
        case .shortcuts:
            Section("常用快捷键") {
                shortcutRow("搜索工具", "⌘K")
                shortcutRow("设置", "⌘,")
                shortcutRow("JSON / 随手记保存", "⌘S")
                shortcutRow("JSON 格式化", "⌘↩")
                shortcutRow("文本对比比较", "⌘↩")
                shortcutRow("格式化工具", "⌘⇧F")
                shortcutRow("查找替换", "⌘F")
                Text("与 Electron 设置页相同：展示主要全局/工具快捷键；暂不支持自定义绑定或系统托盘全局热键。").font(.caption).foregroundStyle(.secondary)
            }
        case .backup:
            Section("工作区与备份") {
                LabeledContent("产品线", value: "MooTool Next macOS Native")
                Text(store.repository.directory.path).font(.caption).foregroundStyle(.secondary).textSelection(.enabled)
                HStack {
                    Button("在 Finder 中显示") { try? FileManager.default.createDirectory(at: store.repository.directory, withIntermediateDirectories: true); NSWorkspace.shared.open(store.repository.directory) }
                    Button("导出备份") {
                        let snapshot = store.snapshot(), repository = store.repository; backupBusy = true
                        Task {
                            defer { backupBusy = false }
                            do {
                                let data = try await Task.detached { try repository.backup(snapshot) }.value
                                FilePanels.save(data, name: "MooTool-Next-Native-backup.json")
                            } catch { FilePanels.error(error) }
                        }
                    }
                    Button("导入备份…") {
                        FilePanels.open(types: [.json]) { urls in
                            backupBusy = true
                            Task {
                                defer { backupBusy = false }
                                do { pendingBackup = try await Task.detached { try WorkspaceRepository.readSnapshot(at: urls[0]) }.value; confirmRestore = true }
                                catch { FilePanels.error(error) }
                            }
                        }
                    }
                }.disabled(backupBusy)
                if backupBusy { ProgressView().controlSize(.small) }
                Text("原生版拥有独立的应用标识、偏好设置与数据目录，可与 Java、Electron、Tauri 版同时安装。备份包含本产品工作区及已登记的图片附件；恢复前保存当前工作区和附件副本。").font(.caption).foregroundStyle(.secondary)
            }
        case .about:
            Section("关于") {
                LabeledContent("版本", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Development")
                LabeledContent("技术", value: "SwiftUI · AppKit · macOS 14+")
                Link("查看项目与反馈问题 ↗", destination: URL(string: "https://github.com/rememberber/MooTool")!)
            }
        }
    }

    private func shortcutRow(_ title: String, _ keys: String) -> some View {
        LabeledContent(title) { Text(keys).font(.body.monospaced()).foregroundStyle(.secondary) }
    }
}
