import SwiftUI
import MooToolNextCore

@MainActor enum NativeJSONAcceptance {
    static func run(store: AppStore, window: NSWindow) async throws {
        let original = store.snapshot(); defer { store.restore(original) }
        func check(_ condition: @autoclosure () -> Bool, _ message: String) throws { if !condition() { throw ToolError("JSON 原生验收：" + message) } }
        let source = #"{"z":1,"name":"MooTool","values":[1,2]}"#
        let a = try store.createVaultDocument("json", name: "JSON 操作验收.json", parent: nil, content: source)
        store.selected = "json"; var options = JSONOptions(); options.inspectorOpen = true; store.draft("json").json = options
        window.setContentSize(NSSize(width: 1400, height: 850)); window.makeKeyAndOrderFront(nil)
        try await settle(window)
        let editor = try inputEditor(window, document: a)
        window.makeFirstResponder(editor)
        guard let event = NSEvent.keyEvent(with: .keyDown, location: .zero, modifierFlags: .command, timestamp: ProcessInfo.processInfo.systemUptime, windowNumber: window.windowNumber, context: nil, characters: "\r", charactersIgnoringModifiers: "\r", isARepeat: false, keyCode: 36) else { throw ToolError("无法创建 JSON 格式化键盘事件。") }
        _ = window.performKeyEquivalent(with: event)
        try await settle(window)
        for _ in 0..<30 where store.draft("json").busy { try await Task.sleep(for: .milliseconds(100)) }
        try check(store.draft("json").input.contains("\n  \"z\": 1"), "⌘Return 没有在主编辑器中格式化")
        try check(store.documents.first { $0.id == a }?.content == store.draft("json").input, "格式化结果没有自动保存")
        try check(editor.undoManager?.canUndo == true, "格式化不可撤销")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("json").input == source, "撤销没有还原格式化前的正文")
        editor.undoManager?.redo(); try await settle(window)
        try check(store.draft("json").input.contains("\n"), "重做没有还原格式化结果")
        let beforeConversion = store.draft("json").input
        try press("JSON → XML", in: window); try await finishOperation(store, window: window)
        try check(store.draft("json").output.contains("<name>MooTool</name>"), "转换按钮未展示 XML 结果")
        try check(store.draft("json").input == beforeConversion, "结果弹窗提前覆盖了正文")
        try await captureOutput(window)
        try press("使用此结果", in: window); try await settle(window)
        try check(store.draft("json").input.contains("<root>"), "使用结果未写入正文")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("json").input == beforeConversion, "使用转换结果不可撤销")
        try press("XML → JSON", in: window); try await settle(window)
        guard let conversion = allViews(window).compactMap({ $0 as? NSTextView }).first(where: { $0.isEditable && $0.identifier == nil && $0.enclosingScrollView != nil }) else { throw ToolError("找不到 XML 转换输入框。") }
        conversion.insertText("<tool><name>Native</name></tool>", replacementRange: NSRange(location: 0, length: (conversion.string as NSString).length))
        try press("转换", in: window); try await finishOperation(store, window: window)
        try check(store.draft("json").input.contains("\"tool\""), "输入转换未替换主编辑器")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("json").input == beforeConversion, "输入转换不可撤销")
        let bridge = NativeEditorBridge(); bridge.view = editor
        var replacement = JSONEngineRequest("replaceAll", input: editor.string)
        replacement.query = "MooTool"; replacement.replacement = "Native"
        let changed = try await JSONEngine.execute(replacement)
        let beforeReplacement = editor.string
        try check(bridge.replace(changed.value!, expected: beforeReplacement, action: "全部替换"), "查找替换未应用")
        try await settle(window)
        try check(store.draft("json").input.contains("Native"), "替换正文未同步")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("json").input == beforeReplacement, "替换无法撤销")
        let other = try store.createVaultDocument("json", name: "JSON 独立文档.json", parent: nil, content: "[]")
        try await settle(window)
        try check(bridge.replace("invalid", expected: beforeReplacement, action: "旧文档结果") == false, "旧文档结果覆盖了新文档")
        try check(store.draft("json").documentID == other && store.draft("json").input == "[]", "切换文档时正文被覆盖")
        try store.openDocument(a); try await settle(window)
        options.findOpen = true; options.findQuery = "MooTool"; options.wrapLines = false; options.fontName = "Monaco"; store.draft("json").json = options
        try await settle(window); store.saveNow()
        let fresh = AppStore(directory: store.repository.directory)
        try check(fresh.draft("json").json == options, "检查器、查找或换行设置未恢复")
        print("PASS: native JSON format shortcut, in-place autosave, undo/redo, conversion dialogs and apply, replace undo, stale-document protection and workspace options")
    }
    static func allViews(_ window: NSWindow) -> [NSView] {
        var pending = [window.contentView, window.attachedSheet?.contentView].compactMap { $0 }, result: [NSView] = []
        while let view = pending.popLast() { result.append(view); pending.append(contentsOf: view.subviews) }; return result
    }
    static func waitForPathPreview(_ window: NSWindow, source: String) async throws {
        let expected = try TextServices.json(source)
        for _ in 0..<50 {
            let views = allViews(window)
            if views.contains(where: { $0.identifier?.rawValue == "json.acceptance.路径树就绪" }),
               views.compactMap({ $0 as? NSTextView }).contains(where: { !$0.isEditable && (try? TextServices.json($0.string)) == expected }) { return }
            try await Task.sleep(for: .milliseconds(100))
        }
        throw ToolError("路径选择器未能展示树和当前正文预览。")
    }
    static func press(_ title: String, in window: NSWindow) throws {
        guard let anchor = allViews(window).first(where: { $0.identifier?.rawValue == "json.acceptance." + title }),
              let root = anchor.window?.contentView else { throw ToolError("找不到 JSON 控件定位标记：\(title)") }
        let point = anchor.convert(NSPoint(x: anchor.bounds.midX, y: anchor.bounds.midY), to: root)
        var target = root.hitTest(point)
        while let view = target {
            if let button = view as? NSButton, button.isEnabled { button.performClick(nil); return }
            target = view.superview
        }
        // Some SwiftUI button styles draw directly in the hosting view. Deliver
        // a normal click to this app's window at the control's rendered center.
        guard let owner = anchor.window,
              let down = NSEvent.mouseEvent(with: .leftMouseDown, location: anchor.convert(NSPoint(x: anchor.bounds.midX, y: anchor.bounds.midY), to: nil), modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime, windowNumber: owner.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
              let up = NSEvent.mouseEvent(with: .leftMouseUp, location: down.locationInWindow, modifierFlags: [], timestamp: down.timestamp + 0.01, windowNumber: owner.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else { throw ToolError("无法创建 JSON 控件点击事件：\(title)") }
        owner.makeKeyAndOrderFront(nil); NSApp.postEvent(up, atStart: true); owner.sendEvent(down)
    }

    private static func finishOperation(_ store: AppStore, window: NSWindow) async throws {
        try await settle(window)
        for _ in 0..<40 where store.draft("json").busy { try await Task.sleep(for: .milliseconds(100)) }
        try await settle(window)
    }
    private static func captureOutput(_ window: NSWindow) async throws {
        guard CommandLine.arguments.contains("--window-capture"), let directory = ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_SCREENSHOTS"] else { return }
        for scheme in ["light", "dark"] {
            nativeDefaults.set(scheme, forKey: "appearance"); NSApp.activate(ignoringOtherApps: true); window.makeKeyAndOrderFront(nil)
            try await settle(window)
            let capture = Process(); capture.executableURL = URL(fileURLWithPath: "/usr/sbin/screencapture")
            capture.arguments = ["-x", "-o", "-l", String((window.attachedSheet ?? window).windowNumber), URL(fileURLWithPath: directory).appendingPathComponent("json-output-\(scheme).png").path]
            try capture.run(); capture.waitUntilExit()
            guard capture.terminationStatus == 0 else { throw ToolError("无法捕获原生 JSON 结果弹窗。") }
        }
        nativeDefaults.set("light", forKey: "appearance")
    }
    private static func inputEditor(_ window: NSWindow, document: UUID) throws -> NSTextView {
        func find(_ view: NSView) -> NSTextView? {
            if let text = view as? NSTextView, text.identifier?.rawValue.contains(document.uuidString) == true, text.identifier?.rawValue.hasSuffix(":input") == true { return text }
            for child in view.subviews { if let match = find(child) { return match } }; return nil
        }
        guard let root = window.contentView, let editor = find(root) else { throw ToolError("找不到 JSON 主编辑器。") }; return editor
    }
    private static func settle(_ window: NSWindow) async throws { window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded(); try await Task.sleep(for: .milliseconds(600)); window.contentView?.layoutSubtreeIfNeeded() }
}

// Only the isolated acceptance run inserts a non-interactive locator. Tests hit
// the real AppKit button at its rendered location and invoke its normal action.
private struct JSONAcceptanceAnchor: NSViewRepresentable {
    let title: String
    func makeNSView(context: Context) -> NSView { let view = AnchorView(); view.identifier = NSUserInterfaceItemIdentifier("json.acceptance." + title); return view }
    func updateNSView(_ view: NSView, context: Context) { view.identifier = NSUserInterfaceItemIdentifier("json.acceptance." + title) }
    private final class AnchorView: NSView { override func hitTest(_ point: NSPoint) -> NSView? { nil } }
}
extension View {
    @ViewBuilder func jsonAcceptanceControl(_ title: String) -> some View {
        if CommandLine.arguments.contains("--smoke-test") { self.background(JSONAcceptanceAnchor(title: title)) }
        else { self }
    }
}
