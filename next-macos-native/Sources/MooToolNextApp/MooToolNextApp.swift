import SwiftUI
import MooToolNextCore

@main
struct MooToolNextApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @State private var store = AppStore(directory: ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_TEST_DATA"].map { URL(fileURLWithPath: $0) } ?? Product.dataDirectory)
    @AppStorage("appearance", store: nativeDefaults) private var appearance = "system"
    @AppStorage("general.language", store: nativeDefaults) private var languageCode = "zh-CN"
    private var language: AppLanguage { AppLanguage.normalized(languageCode) }
    var scheme: ColorScheme? { appearance == "dark" ? .dark : appearance == "light" ? .light : nil }
    var body: some Scene {
        Window("MooTool Next Native", id: "main") {
            Workbench().environment(store).environment(\.appLanguage, language).environment(\.locale, language.locale).preferredColorScheme(scheme)
                .background(MainWindowAccessor { delegate.attachMainWindow($0) })
                .onAppear {
                    delegate.attachStore(store)
                    store.startVaultFilesystemWatcher()
                    UpdateChecker.runAutomaticIfEnabled()
                }
        }
        .defaultSize(width: 1200, height: 800).windowStyle(.titleBar).windowToolbarStyle(.unified)
        .commands {
            CommandGroup(replacing: .appInfo) {
                Button(AppLocalization.string("app.menu.about", language: language)) {
                    NSApplication.shared.orderFrontStandardAboutPanel(options: [.applicationName: Product.name, .applicationVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Development", .credits: NSAttributedString(string: "MooTool · macOS 原生工具箱\n作者：Zhou Bo · MIT License")])
                }
            }
            CommandGroup(after: .newItem) { Button(AppLocalization.string("app.menu.searchTools", language: language)) { store.searchPresented = true }.keyboardShortcut("k") }
            CommandGroup(replacing: .help) {
                Button(AppLocalization.string("app.menu.checkUpdate", language: language)) { UpdateChecker.runManual() }
            }
            CommandMenu(AppLocalization.string("app.menu.tools", language: language)) {
                ForEach(Catalog.localizedTools(language)) { tool in Button(tool.title) { store.select(tool.id) } }
            }
        }
        WindowGroup("工具", id: "tool", for: String.self) { $id in
            ToolRouter(id: id ?? "json").environment(store).environment(\.appLanguage, language).environment(\.locale, language.locale).preferredColorScheme(scheme)
                .navigationTitle(Catalog.localizedTool(id ?? "json", language: language).title)
                .frame(minWidth: 680, minHeight: 500)
        }.defaultSize(width: 1000, height: 730)
        Settings { SettingsView().environment(store).environment(\.appLanguage, language).environment(\.locale, language.locale).preferredColorScheme(scheme) }
    }
}

@MainActor final class AppDelegate: NSObject, NSApplicationDelegate {
    weak var store: AppStore?
    weak var mainWindow: NSWindow?
    var isQuitting = false
    private let menuTray = MenuBarTray()
    func attachStore(_ store: AppStore) {
        self.store = store
        menuTray.attach(store: store)
    }
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        if let url = AppResources.bundle.url(forResource: "AppIcon", withExtension: "icns") { NSApp.applicationIconImage = NSImage(contentsOf: url) }
        NSApp.activate(ignoringOtherApps: true)
        if CommandLine.arguments.contains("--verify-bundle") {
            guard Bundle.main.bundleIdentifier == Product.bundleID,
                  AppResources.bundle.bundleURL.path.hasPrefix(Bundle.main.bundleURL.path),
                  AppResources.bundle.url(forResource: "Brand", withExtension: "png") != nil,
                  JSONEngine.resources.bundleURL.path.hasPrefix(Bundle.main.bundleURL.path + "/"),
                  JSONEngine.workerURL.path.hasPrefix(Bundle.main.bundleURL.path + "/") else {
                fputs("Installed bundle identity/resources failed.\n", stderr); exit(1)
            }
            Task {
                do {
                    let result = try await JSONEngine.execute(JSONEngineRequest("compress", input: "{ \"standalone\": true }"))
                    guard result.value == "{\"standalone\":true}" else { throw ToolError("Bundled JSON helper returned an unexpected result.") }
                    var note = JSONEngineRequest("quickReplace", input: "a\nb"); note.path = "linesToComma"
                    guard try await JSONEngine.execute(note).value == "a,b" else { throw ToolError("Bundled note helper returned an unexpected result.") }
                    for type in [ReformatType.nginx, .java, .xml, .html] {
                        guard try await ReformatEngine.format(type.sample, type: type).contains("\n") else {
                            throw ToolError("Bundled \(type.title) formatter returned an unexpected result.")
                        }
                    }
                    let diff = try TextDiffEngine.compare("one\ntwo\n", "one\nthree\n")
                    guard diff.unified.contains("+three") && diff.changed == 1 else { throw ToolError("Bundled text diff returned an unexpected result.") }
                    guard let path = ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_TEST_DATA"] else { throw ToolError("Bundle verification requires isolated test data.") }
                    let repository = WorkspaceRepository(directory: URL(fileURLWithPath: path))
                    let payload = try NoteImagePayload(data: NativeAttachmentAcceptance.fixture())
                    try repository.attachmentRepository.write(payload)
                    var snapshot = WorkspaceSnapshot(); snapshot.noteAttachments = [payload.attachment]
                    snapshot.documents = [SavedDocument(toolID: "quickNote", title: "独立附件.md", content: payload.attachment.markdown)]
                    let backup = try WorkspaceRepository.decode(repository.backup(snapshot))
                    let restored = WorkspaceRepository(directory: repository.directory.appendingPathComponent("restored-bundle-test"))
                    _ = try restored.installBackup(backup)
                    guard try restored.attachmentRepository.thumbnail(payload.attachment).width > 0 else { throw ToolError("Restored attachment cannot be decoded.") }
                    print("PASS: standalone bundle identity, embedded resources, JSON helper, four formatters, text diff, note quick replacement, attachment backup/restore and image decoding"); exit(0)
                } catch { fputs("Installed bundle verification failed: \(error)\n", stderr); exit(1) }
            }
            return
        }
        if CommandLine.arguments.contains("--smoke-test") { Task {
            try? await Task.sleep(for: .milliseconds(500))
            if let store { await NativeSmoke.run(store: store) } else { fputs("Main window was not initialized.\n", stderr); exit(2) }
        } }
        if CommandLine.arguments.contains("--verify-workspace") { Task {
            try? await Task.sleep(for: .milliseconds(500))
            if let store { NativeSmoke.verifyRestart(store: store) } else { exit(2) }
        } }
    }
    func applicationDidBecomeActive(_ notification: Notification) { store?.setVaultCheckpointWindowActive(true) }
    func applicationDidResignActive(_ notification: Notification) { store?.setVaultCheckpointWindowActive(false) }
    func applicationWillTerminate(_ notification: Notification) { store?.saveNow() }
    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        if !flag { sender.windows.first { $0.canBecomeMain && $0.frame.width > 500 }?.makeKeyAndOrderFront(nil) }; return true
    }
}
