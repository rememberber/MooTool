import SwiftUI
import MooToolNextCore

@MainActor enum NativeNoteAcceptance {
    static func run(store: AppStore, window: NSWindow) async throws {
        let original = store.snapshot(); defer { store.restore(original) }
        func check(_ condition: @autoclosure () throws -> Bool, _ message: String) throws { if try !condition() { throw ToolError("随手记原生验收：" + message) } }
        let source = "keep alpha\nbeta\nalpha"
        let a = try store.createVaultDocument("quickNote", name: "样式验收.md", parent: nil, content: source)
        store.selected = "quickNote"; store.updateVaultPreference("quickNote") { $0.noteViewMode = .editor }
        var options = QuickNoteOptions(); options.fontName = "Menlo"; options.fontSize = 18; options.lineSpacing = 1.4; options.lineWrap = false; options.color = .blue
        store.draft("quickNote").noteOptions = options
        var workspace = QuickNoteWorkspaceOptions(); workspace.quickReplaceOpen = true; store.draft("quickNote").noteWorkspace = workspace
        window.setContentSize(NSSize(width: 1440, height: 850)); window.makeKeyAndOrderFront(nil); try await settle(window)
        let editor = try inputEditor(window, document: a)
        try check(editor.font?.pointSize == 18 && editor.isHorizontallyResizable, "文档字号或换行没有应用到原生编辑器")
        try check(abs((editor.defaultParagraphStyle?.minimumLineHeight ?? 0) - 18 * 1.65 * 1.4) < 0.1, "行距没有应用")
        try check(store.documents.first { $0.id == a }?.noteOptions == options, "仅修改文档设置没有自动保存")
        window.makeFirstResponder(editor); editor.setSelectedRange(NSRange(location: 5, length: 5))
        try NativeJSONAcceptance.press("note.uppercase", in: window); try await finish(store, window)
        try check(store.draft("quickNote").input == "keep ALPHA\nbeta\nalpha", "快速替换没有仅处理选区")
        try check(store.documents.first { $0.id == a }?.content == store.draft("quickNote").input, "快速替换没有自动保存")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("quickNote").input == source, "选区快速替换无法撤销")
        editor.setSelectedRange(NSRange(location: 0, length: 0))
        try NativeJSONAcceptance.press("note.uppercase", in: window); try await finish(store, window)
        try check(store.draft("quickNote").input == source.uppercased(), "无选区时未处理全文")
        editor.undoManager?.undo(); try await settle(window)
        workspace.findOpen = true; workspace.findQuery = "alpha"; workspace.replacement = "native"; store.draft("quickNote").noteWorkspace = workspace
        try await settle(window); try await waitForFind(window, count: 2)
        try check(editor.layoutManager?.temporaryAttribute(.backgroundColor, atCharacterIndex: 5, effectiveRange: nil) != nil, "查找结果没有高亮")
        try NativeJSONAcceptance.press("note.replaceAll", in: window); try await finish(store, window)
        try check(store.draft("quickNote").input == "keep native\nbeta\nnative", "查找全部替换未执行")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("quickNote").input == source, "全部替换不可撤销")
        editor.setSelectedRange(NSRange(location: 0, length: 0))
        try NativeJSONAcceptance.press("note.bullet", in: window); try await finish(store, window)
        try check(store.draft("quickNote").input == "- " + source, "列表操作没有处理当前行")
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .preview }; try await settle(window)
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .editor }; try await settle(window)
        try check(try inputEditor(window, document: a) === editor, "切换预览重建了输入编辑器")
        editor.undoManager?.undo(); try await settle(window)
        try check(store.draft("quickNote").input == source, "切换预览丢失了撤销记录")
        let otherWindow = NSWindow(contentRect: NSRect(x: 40, y: 40, width: 1000, height: 700), styleMask: [.titled, .closable, .resizable], backing: .buffered, defer: false)
        otherWindow.isReleasedWhenClosed = false; otherWindow.contentView = NSHostingView(rootView: ToolRouter(id: "quickNote").environment(store)); otherWindow.makeKeyAndOrderFront(nil)
        defer { otherWindow.close() }
        options.fontSize = 22; store.draft("quickNote").noteOptions = options; try await settle(otherWindow)
        try check(try inputEditor(otherWindow, document: a).font?.pointSize == 22 && editor.font?.pointSize == 22, "跨窗口文档设置没有同步")
        otherWindow.close(); window.makeKeyAndOrderFront(nil)
        let b = try store.createVaultDocument("quickNote", name: "另一份笔记.md", parent: nil, content: "independent")
        var otherOptions = QuickNoteOptions(); otherOptions.fontName = "Monaco"; otherOptions.fontSize = 24; otherOptions.color = .red; otherOptions.syntax = .python
        store.draft("quickNote").noteOptions = otherOptions; try await settle(window)
        try store.openDocument(a); try await settle(window)
        try check(store.draft("quickNote").noteOptions == options && editor.font?.pointSize == 22, "文档 A 设置被文档 B 覆盖")
        try check(editor.undoManager?.canUndo != true, "撤销记录跨文档泄漏")
        try store.openDocument(b); try await settle(window)
        try check(store.draft("quickNote").noteOptions == otherOptions, "文档 B 设置没有恢复")
        store.saveNow(); let fresh = AppStore(directory: store.repository.directory)
        try check(fresh.draft("quickNote").noteOptions == otherOptions && fresh.draft("quickNote").noteWorkspace == workspace, "重载工作区没有恢复文档设置与面板选项")
        print("PASS: native note metadata, font/line spacing/wrap, selected/full quick replacements, find/replace, list actions, preview undo, multi-window settings and document isolation")
    }
    static func waitForPreview(_ window: NSWindow) async throws {
        for _ in 0..<60 {
            if NativeJSONAcceptance.allViews(window).contains(where: { $0.identifier?.rawValue == "json.acceptance.note.preview.ready" }) { return }
            try await Task.sleep(for: .milliseconds(100))
        }
        throw ToolError("Markdown 预览未就绪。")
    }
    static func waitForFind(_ window: NSWindow, count: Int) async throws {
        for _ in 0..<60 {
            if NativeJSONAcceptance.allViews(window).contains(where: { $0.identifier?.rawValue == "json.acceptance.note.find.count.\(count)" }) { return }
            try await Task.sleep(for: .milliseconds(100))
        }
        throw ToolError("随手记查找计数没有更新为 \(count)。")
    }
    private static func inputEditor(_ window: NSWindow, document: UUID) throws -> NSTextView {
        guard let editor = NativeJSONAcceptance.allViews(window).compactMap({ $0 as? NSTextView }).first(where: { $0.identifier?.rawValue.contains(document.uuidString) == true && $0.identifier?.rawValue.hasSuffix(":input") == true }) else { throw ToolError("找不到随手记输入编辑器。") }; return editor
    }
    private static func settle(_ window: NSWindow) async throws { window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded(); try await Task.sleep(for: .milliseconds(650)); window.contentView?.layoutSubtreeIfNeeded() }
    private static func finish(_ store: AppStore, _ window: NSWindow) async throws {
        try await settle(window)
        for _ in 0..<40 where store.draft("quickNote").busy { try await Task.sleep(for: .milliseconds(100)) }
        try await settle(window)
    }
}
