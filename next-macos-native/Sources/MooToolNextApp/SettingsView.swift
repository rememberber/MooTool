import SwiftUI
import MooToolNextCore

struct SettingsView: View {
    @Environment(AppStore.self) private var store
    @AppStorage("appearance", store: nativeDefaults) private var appearance = "system"
    @AppStorage("editorSize", store: nativeDefaults) private var editorSize = 13.0
    @AppStorage("wrapLines", store: nativeDefaults) private var wrapLines = true
    @State private var pendingBackup: WorkspaceSnapshot?
    @State private var confirmRestore = false
    @State private var backupBusy = false
    var body: some View {
        Form {
            Section("外观") {
                Picker("主题", selection: $appearance) { Text("跟随系统").tag("system"); Text("浅色").tag("light"); Text("深色").tag("dark") }.pickerStyle(.segmented)
                HStack { Text("编辑器字号"); Slider(value: $editorSize, in: 11...22, step: 1); Text("\(Int(editorSize)) pt").monospacedDigit().frame(width: 42) }
                Toggle("编辑器自动换行", isOn: $wrapLines)
            }
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
            Section("关于") {
                LabeledContent("版本", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Development")
                LabeledContent("技术", value: "SwiftUI · AppKit · macOS 14+")
                Link("查看项目与反馈问题 ↗", destination: URL(string: "https://github.com/rememberber/MooTool")!)
            }
        }.formStyle(.grouped).frame(width: 590, height: 535)
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
}
