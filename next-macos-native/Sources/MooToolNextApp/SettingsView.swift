import SwiftUI
import MooToolNextCore

private enum SettingsCategory: String, CaseIterable, Identifiable {
    case sidebar, proxy, appearance, vault, migration, backup, shortcuts, about
    var id: String { rawValue }
    func title(language: AppLanguage) -> String {
        AppLocalization.string("settings.category.\(rawValue)", language: language)
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
    @Environment(\.appLanguage) private var language
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
    @AppStorage("general.trayEnabled", store: nativeDefaults) private var trayEnabled = true
    @AppStorage("general.closeBehavior", store: nativeDefaults) private var closeBehavior = "ask"
    @AppStorage("general.autoDownloadUpdates", store: nativeDefaults) private var autoDownloadUpdates = true
    @AppStorage("general.autoCheckUpdates", store: nativeDefaults) private var autoCheckUpdates = true
    @AppStorage("general.language", store: nativeDefaults) private var languageCode = "zh-CN"
    @AppStorage("shortcuts.search", store: nativeDefaults) private var shortcutSearch = "CommandOrControl+K"
    @AppStorage("shortcuts.settings", store: nativeDefaults) private var shortcutSettings = "CommandOrControl+,"
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
                Label(item.title(language: language), systemImage: item.symbol).tag(item)
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
            Section(SettingsCategory.sidebar.title(language: language)) {
                Picker(AppLocalization.string("settings.language", language: language), selection: $languageCode) {
                    Text("简体中文").tag("zh-CN")
                    Text("English").tag("en-US")
                    Text("日本語").tag("ja-JP")
                }
                Toggle(AppLocalization.string("settings.autoCheckUpdates", language: language), isOn: $autoCheckUpdates)
                LabeledContent(AppLocalization.string("settings.customGroups", language: language), value: settingsCount(store.customGroups.count, emptyKey: "settings.notConfigured"))
                Button(AppLocalization.string("settings.manageCustomGroups", language: language)) { customGroupsOpen = true }
                LabeledContent(AppLocalization.string("settings.hiddenNavTools", language: language), value: settingsCount(store.hiddenNavigationToolIds.count, emptyKey: "settings.none"))
                Button(AppLocalization.string("settings.manageHiddenNav", language: language)) { navigationToolsOpen = true }
                Toggle(AppLocalization.string("settings.showGroupSeparators", language: language), isOn: Binding(
                    get: { store.showNavigationSeparators },
                    set: { store.showNavigationSeparators = $0; store.scheduleSave() }))
                Toggle(AppLocalization.string("workbench.sidebar.iconsOnly", language: language), isOn: Binding(
                    get: { store.hideNavigationTitles },
                    set: { store.hideNavigationTitles = $0; store.scheduleSave() }))
                Toggle(AppLocalization.string("settings.showRecent", language: language), isOn: Binding(
                    get: { store.showRecent },
                    set: { store.showRecent = $0; store.scheduleSave() }))
                LabeledContent(AppLocalization.string("settings.sidebarWidth", language: language), value: "\(Int(store.sidebarWidth)) pt")
                Text("在主窗口侧栏右缘拖动调整宽度，双击恢复默认 215 pt。").font(.caption).foregroundStyle(.secondary)
                Toggle(AppLocalization.string("settings.trayEnabled", language: language), isOn: $trayEnabled)
                    .onChange(of: trayEnabled) { _, _ in NotificationCenter.default.post(name: .menuBarTrayRefresh, object: nil) }
                Text("与 Electron「系统托盘」相同：提供打开主窗口、设置、屏幕取色、区域截图、翻译与 Host 配置切换；不含自定义全局热键。").font(.caption).foregroundStyle(.secondary)
                Picker(AppLocalization.string("settings.closeWindow", language: language), selection: $closeBehavior) {
                    Text(AppLocalization.string("settings.close.ask", language: language)).tag("ask")
                    Text(AppLocalization.string("settings.close.hide", language: language)).tag("hide")
                    Text(AppLocalization.string("settings.close.quit", language: language)).tag("quit")
                }
                Toggle(AppLocalization.string("settings.autoDownloadPref", language: language), isOn: $autoDownloadUpdates)
                Text("关闭行为与 Electron `general.closeBehavior` 一致；自动下载偏好会随 Electron 设置迁移，原生版仍需在「检查更新」中手动获取安装包。").font(.caption).foregroundStyle(.secondary)
            }
        case .proxy:
            Section(SettingsCategory.proxy.title(language: language)) {
                Toggle(AppLocalization.string("settings.proxyEnabled", language: language), isOn: $proxyEnabled)
                TextField(AppLocalization.string("settings.host", language: language), text: $proxyHost).disabled(!proxyEnabled)
                Stepper(value: $proxyPort, in: 1...65535) { Text("\(AppLocalization.string("settings.port", language: language))：\(proxyPort)") }.disabled(!proxyEnabled)
                TextField(AppLocalization.string("settings.usernameOptional", language: language), text: $proxyUsername).disabled(!proxyEnabled)
                SecureField(AppLocalization.string("settings.passwordOptional", language: language), text: $proxyPassword).disabled(!proxyEnabled)
                Text("与 Electron 设置相同：HTTP / HTTPS 请求经此代理发送（含 HTTP 工具与网络诊断中的 HTTP 类调用）。不支持 SOCKS 与系统代理自动发现。").font(.caption).foregroundStyle(.secondary)
            }
        case .appearance:
            Section(SettingsCategory.appearance.title(language: language)) {
                Picker(AppLocalization.string("settings.theme", language: language), selection: $appearance) {
                    Text(AppLocalization.string("settings.theme.system", language: language)).tag("system")
                    Text(AppLocalization.string("settings.theme.light", language: language)).tag("light")
                    Text(AppLocalization.string("settings.theme.dark", language: language)).tag("dark")
                }.pickerStyle(.segmented)
                HStack { Text(AppLocalization.string("settings.editorFontSize", language: language)); Slider(value: $editorSize, in: 11...22, step: 1); Text("\(Int(editorSize)) pt").monospacedDigit().frame(width: 42) }
                Toggle(AppLocalization.string("settings.wrapLines", language: language), isOn: $wrapLines)
            }
        case .vault:
            Section(SettingsCategory.vault.title(language: language)) {
                Toggle(AppLocalization.string("settings.vaultAutoCommit", language: language), isOn: $vaultAutoCommit)
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
            Section(SettingsCategory.migration.title(language: language)) {
                Button(AppLocalization.string("settings.importElectron", language: language)) { migrationOpen = true }
                Text("合并 Electron 工作台与代理/编辑器/语言偏好；SQLite 与磁盘 `quick-notes` / `json-vault` 可合并文档与附件。").font(.caption).foregroundStyle(.secondary)
            }
        case .shortcuts:
            Section(SettingsCategory.shortcuts.title(language: language)) {
                shortcutRow(AppLocalization.string("settings.shortcut.search", language: language), ElectronShortcutFormat.display(shortcutSearch))
                shortcutRow(AppLocalization.string("settings.shortcut.settings", language: language), ElectronShortcutFormat.display(shortcutSettings))
                shortcutRow("JSON / 随手记保存", "⌘S")
                shortcutRow("JSON 格式化", "⌘↩")
                shortcutRow("文本对比比较", "⌘↩")
                shortcutRow("格式化工具", "⌘⇧F")
                shortcutRow("查找替换", "⌘F")
                Text("与 Electron 设置页相同：只读展示主要快捷键（可从 Electron 设置迁移）；不提供自定义绑定或托盘全局热键。").font(.caption).foregroundStyle(.secondary)
            }
        case .backup:
            Section(SettingsCategory.backup.title(language: language)) {
                LabeledContent(AppLocalization.string("settings.productLine", language: language), value: "MooTool Next macOS Native")
                Text(store.repository.directory.path).font(.caption).foregroundStyle(.secondary).textSelection(.enabled)
                HStack {
                    Button(AppLocalization.string("settings.showInFinder", language: language)) { try? FileManager.default.createDirectory(at: store.repository.directory, withIntermediateDirectories: true); NSWorkspace.shared.open(store.repository.directory) }
                    Button(AppLocalization.string("settings.exportBackup", language: language)) {
                        let snapshot = store.snapshot(), repository = store.repository; backupBusy = true
                        Task {
                            defer { backupBusy = false }
                            do {
                                let data = try await Task.detached { try repository.backup(snapshot) }.value
                                FilePanels.save(data, name: "MooTool-Next-Native-backup.json")
                            } catch { FilePanels.error(error) }
                        }
                    }
                    Button(AppLocalization.string("settings.importBackup", language: language)) {
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
            Section(SettingsCategory.about.title(language: language)) {
                LabeledContent(AppLocalization.string("settings.version", language: language), value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Development")
                LabeledContent(AppLocalization.string("settings.tech", language: language), value: "SwiftUI · AppKit · macOS 14+")
                Link(AppLocalization.string("settings.viewProject", language: language), destination: URL(string: "https://github.com/rememberber/MooTool")!)
                Button(AppLocalization.string("app.menu.checkUpdate", language: language)) { UpdateChecker.runManual() }
                Text("通过 GitHub Releases API 检查新版本；有更新时可打开下载页面。不含 Electron 安装包后台下载与静默安装。").font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private func shortcutRow(_ title: String, _ keys: String) -> some View {
        LabeledContent(title) { Text(keys).font(.body.monospaced()).foregroundStyle(.secondary) }
    }

    private func settingsCount(_ count: Int, emptyKey: String) -> String {
        if count == 0 { return AppLocalization.string(emptyKey, language: language) }
        switch language {
        case .enUS: return "\(count) item\(count == 1 ? "" : "s")"
        default:
            let unit = AppLocalization.string("settings.countUnit", language: language)
            return unit.isEmpty ? "\(count)" : "\(count) \(unit)"
        }
    }
}
