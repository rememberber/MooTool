import SwiftUI
import MooToolNextCore

/// Exercises the actions moved from content rows into the actual window toolbar.
@MainActor enum NativeChromeAcceptance {
    static func run(store: AppStore, window: NSWindow, output: URL) async throws -> [[String: Any]] {
        func check(_ condition: @autoclosure () -> Bool, _ message: String) throws {
            if !condition() { throw ToolError("原生窗口验收：" + message) }
        }
        store.suspendVaultDiskRefresh()
        defer { store.resumeVaultDiskRefresh() }
        store.selected = "json"
        let document = try store.createVaultDocument("json", name: "原生工具栏.json", parent: nil, content: "{\"name\":\"MooTool\",\"value\":1}")
        var options = JSONOptions(); options.inspectorOpen = true; store.draft("json").json = options
        window.setContentSize(NSSize(width: 1440, height: 850))
        try await settle(window)
        try check(window.toolbar?.isVisible == true, "系统工具栏未显示")
        try NativeJSONAcceptance.press("json.format", in: window)
        for _ in 0..<60 {
            try await Task.sleep(for: .milliseconds(100))
            if !store.draft("json").busy && store.draft("json").input.contains("\n") { break }
        }
        try check(store.draft("json").input.contains("\n"), "窗口工具栏的格式化按钮未执行")
        try check(store.draft("json").documentID == document, "工具栏操作丢失当前文档")
        try NativeJSONAcceptance.press("json.find", in: window); try await settle(window)
        try check(store.draft("json").json?.findOpen == true, "窗口工具栏未打开查找栏")
        try NativeJSONAcceptance.press("json.find", in: window); try await settle(window)
        try check(store.draft("json").json?.findOpen == false, "窗口工具栏未关闭查找栏")

        // The two saved slots already mean library width and inspector width in imported workspaces.
        store.setPaneWidth(toolID: "json", index: 0, value: 205, slots: 2)
        store.setPaneWidth(toolID: "json", index: 1, value: 275, slots: 2)
        try await settle(window)
        let findDivider: () -> NSView? = {
            NativeJSONAcceptance.allViews(window).first { $0.identifier?.rawValue == "json.acceptance.json.inspector.divider" }
        }
        guard let firstDivider = findDivider() else { throw ToolError("检查器分隔条未渲染") }
        let initial = firstDivider.convert(firstDivider.bounds, to: window.contentView).minX
        store.setPaneWidth(toolID: "json", index: 1, value: 315, slots: 2)
        try await settle(window)
        guard let resizedDivider = findDivider() else { throw ToolError("检查器分隔条在调整后丢失") }
        let resized = resizedDivider.convert(resizedDivider.bounds, to: window.contentView).minX
        try check(abs(initial - resized - 40) < 2, "检查器未独立应用保存的宽度")
        try check(store.layoutPaneSizes["json"]?.first == 205, "检查器宽度覆盖了目录宽度")

        guard let search = NativeJSONAcceptance.allViews(window).compactMap({ $0 as? NSSearchField }).first(where: {
            $0.placeholderString == AppLocalization.string("vault.search.json", language: .zhCN)
        }) else { throw ToolError("未使用原生文档搜索控件") }
        search.stringValue = "原生工具栏"
        NotificationCenter.default.post(name: NSControl.textDidChangeNotification, object: search)
        try await settle(window)
        try check(store.vaultPreference("json").query == "原生工具栏", "原生搜索字段未同步过滤条件")
        search.stringValue = ""
        NotificationCenter.default.post(name: NSControl.textDidChangeNotification, object: search)
        try await settle(window)
        try check(store.vaultPreference("json").query.isEmpty, "原生搜索字段未清空过滤条件")

        var reports: [[String: Any]] = []
        for scheme in [ColorScheme.light, .dark] {
            nativeDefaults.set(scheme == .light ? "light" : "dark", forKey: "appearance")
            for width in [1200.0, 940.0] {
                for tool in ["json", "quickNote"] {
                    store.selected = tool
                    window.setContentSize(NSSize(width: width, height: 740)); try await settle(window)
                    if tool == "quickNote" {
                        try NativeJSONAcceptance.press("note.find", in: window); try await settle(window)
                        try check(store.draft("quickNote").noteWorkspace?.findOpen == true, "随手记工具栏未打开查找")
                        try NativeJSONAcceptance.press("note.find", in: window); try await settle(window)
                        try check(store.draft("quickNote").noteWorkspace?.findOpen == false, "随手记工具栏未关闭查找")
                    }
                    let name = "chrome-\(tool)-\(scheme == .light ? "light" : "dark")-\(Int(width))"
                    try capture(window, to: output.appendingPathComponent(name + ".png"))
                    reports.append(["tool": tool, "width": width, "appearance": scheme == .light ? "light" : "dark", "screenshot": name + ".png"])
                }
            }
        }
        for scheme in [ColorScheme.light, .dark] {
            let settings = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 820, height: 580), styleMask: [.titled, .closable, .resizable], backing: .buffered, defer: false)
            settings.isReleasedWhenClosed = false
            settings.contentView = NSHostingView(rootView: SettingsView().environment(store).environment(\.appLanguage, .zhCN).preferredColorScheme(scheme))
            settings.center(); settings.makeKeyAndOrderFront(nil)
            for width in [820.0, 680.0] {
                settings.setContentSize(NSSize(width: width, height: 580)); try await settle(settings)
                let name = "chrome-settings-\(scheme == .light ? "light" : "dark")-\(Int(width))"
                try capture(settings, to: output.appendingPathComponent(name + ".png"))
                reports.append(["tool": "settings", "width": width, "appearance": scheme == .light ? "light" : "dark", "screenshot": name + ".png"])
            }
            settings.close()
        }
        window.makeKeyAndOrderFront(nil)
        return reports
    }
    private static func settle(_ window: NSWindow) async throws {
        window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded()
        try await Task.sleep(for: .milliseconds(500)); window.contentView?.layoutSubtreeIfNeeded()
    }
    private static func capture(_ window: NSWindow, to url: URL) throws {
        guard let root = window.contentView?.superview ?? window.contentView,
              let bitmap = root.bitmapImageRepForCachingDisplay(in: root.bounds) else { throw ToolError("无法捕获窗口布局") }
        root.cacheDisplay(in: root.bounds, to: bitmap)
        guard let data = bitmap.representation(using: .png, properties: [:]) else { throw ToolError("无法编码窗口布局") }
        try data.write(to: url)
    }
}
