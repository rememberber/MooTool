import SwiftUI
import MooToolNextCore

/// Exercises document transitions and the actual NSTextView inside this app's isolated acceptance window.
@MainActor enum NativeVaultAcceptance {
    static func run(store: AppStore, window: NSWindow) async throws {
        let original = store.snapshot()
        defer { store.restore(original) }
        func check(_ value: @autoclosure () throws -> Bool, _ message: String) throws { if try !value() { throw ToolError("文档库验收：" + message) } }
        let folder = try store.createVaultFolder("quickNote", name: "验收临时文档", parent: nil)
        let a = try store.createVaultDocument("quickNote", name: "A.md", parent: folder, content: (0..<200).map { "第 \($0) 行 · MooTool Native" }.joined(separator: "\n"))
        let aContent = store.draft("quickNote").input
        let lateEvent = store.editorPersistence("quickNote")
        let b = try store.createVaultDocument("quickNote", name: "B.md", parent: folder, content: "B 的独立内容")
        lateEvent.onChange(EditorViewState(location: 12, length: 3, scrollY: 180))
        try check(store.draft("quickNote").inputEditor == nil, "旧文档事件污染了新文档光标")
        store.draft("quickNote").input = "B 已修改"
        try store.openDocument(a)
        try check(store.draft("quickNote").input == aContent, "快速切换覆盖了 A")
        try check(store.draft("quickNote").inputEditor?.location == 12, "A 的光标未恢复")
        try check(store.documents.first { $0.id == b }?.content == "B 已修改", "B 未自动保存")

        store.selected = "quickNote"
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .editor }
        window.setContentSize(NSSize(width: 1200, height: 800))
        try await settle(window)
        let editorA = try editor(in: window, documentID: a)
        editorA.setSelectedRange(NSRange(location: 28, length: 4))
        if let scroll = editorA.enclosingScrollView {
            scroll.contentView.scroll(to: NSPoint(x: 0, y: 360)); scroll.reflectScrolledClipView(scroll.contentView)
        }
        try await Task.sleep(for: .milliseconds(100))
        let expectedA = store.draft("quickNote").inputEditor
        try check(expectedA?.location == 28 && (expectedA?.scrollY ?? 0) > 100, "真实编辑器没有记录选择和滚动位置")
        try store.openDocument(b); try await settle(window)
        let editorB = try editor(in: window, documentID: b)
        editorB.insertText("追加：", replacementRange: NSRange(location: 0, length: 0))
        try await Task.sleep(for: .milliseconds(100))
        try check(store.documents.first { $0.id == b }?.content == "追加：B 已修改", "NSTextView 编辑未写入当前文档")
        try store.openDocument(a); try await settle(window)
        let restoredA = try editor(in: window, documentID: a)
        try check(restoredA.selectedRange() == NSRange(location: 28, length: 4), "切回文档没有还原选择范围")
        try check((restoredA.enclosingScrollView?.contentView.bounds.origin.y ?? 0) > 100, "切回文档没有还原滚动位置")
        try check(restoredA.undoManager?.canUndo != true, "撤销记录跨文档泄漏")

        let second = NSWindow(contentRect: NSRect(x: 100, y: 100, width: 950, height: 700), styleMask: [.titled, .closable, .resizable], backing: .buffered, defer: false)
        second.isReleasedWhenClosed = false
        second.contentViewController = NSHostingController(rootView: DocumentsTool(id: "quickNote", draft: store.draft("quickNote")).environment(store))
        second.orderFront(nil)
        try await settle(second)
        restoredA.insertText("同步：", replacementRange: NSRange(location: 0, length: 0))
        try await settle(second)
        try check(try editor(in: second, documentID: a).string.hasPrefix("同步："), "独立窗口没有同步编辑内容")
        second.close(); window.makeKeyAndOrderFront(nil)

        try store.renameVaultEntry(folder, name: "已重命名")
        try store.moveVaultEntry(a, to: nil)
        try check(store.draft("quickNote").documentID == a, "移动或重命名改变了打开的文档")
        store.saveNow()
        let fresh = AppStore(directory: store.repository.directory)
        try check(fresh.draft("quickNote").documentID == a && fresh.draft("quickNote").input == store.draft("quickNote").input, "重新加载未恢复活动文档")
        try check(fresh.draft("quickNote").inputEditor == store.draft("quickNote").inputEditor, "重新加载未恢复编辑器状态")
        let deletingContent = store.draft("quickNote").input
        try store.deleteVaultEntry(a)
        try check(store.draft("quickNote").documentID == nil && store.draft("quickNote").input == deletingContent, "删除活动文档时丢失编辑内容")
        try store.openDocument(b); store.openScratch("quickNote")
        try check(store.draft("quickNote").input == deletingContent, "打开其他文档后无法找回草稿")

        let jsonA = try store.createVaultDocument("json", name: "验收 A.json", parent: nil, content: "{\"value\":1}")
        store.draft("json").output = "1"; store.draft("json").option = "/value"
        let jsonB = try store.createVaultDocument("json", name: "验收 B.json", parent: nil, content: "[]")
        try check(store.draft("json").output.isEmpty, "新 JSON 文档沿用了上一文档结果")
        try store.openDocument(jsonA)
        try check(store.draft("json").output == "1" && store.draft("json").option == "/value", "JSON 结果与查询未按文档恢复")
        let count = store.documents.count
        do { _ = try store.importVaultDocuments([DocumentImportItem(relativePath: "valid.json", content: "{}"), DocumentImportItem(relativePath: "../invalid.json", content: "{}")], toolID: "json", parent: nil); throw ToolError("无效批量导入没有失败") }
        catch { try check(store.documents.count == count && store.draft("json").documentID == jsonA, "失败的批量导入改变了文档库") }
        let preRestoreEvent = store.editorPersistence("json")
        let beforeRestore = store.snapshot()
        store.restore(beforeRestore)
        preRestoreEvent.onChange(EditorViewState(location: 9, scrollY: 300))
        try check(store.draft("json").inputEditor == beforeRestore.drafts["json"]?.inputEditor && store.documents == beforeRestore.documents, "恢复备份后旧编辑器事件覆盖了已恢复状态")
        _ = jsonB
        print("PASS: rapid document switching, native selection/scroll, undo isolation, multi-window edits, scratch recovery, JSON state and atomic import")
    }
    private static func editor(in window: NSWindow, documentID: UUID) throws -> NSTextView {
        func find(_ view: NSView) -> NSTextView? {
            if let text = view as? NSTextView, text.identifier?.rawValue.contains(documentID.uuidString) == true { return text }
            for child in view.subviews { if let value = find(child) { return value } }; return nil
        }
        guard let root = window.contentView, let view = find(root) else { throw ToolError("文档库验收找不到原生编辑器") }; return view
    }
    private static func settle(_ window: NSWindow) async throws {
        window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded()
        try await Task.sleep(for: .milliseconds(350))
        window.contentView?.layoutSubtreeIfNeeded()
    }
}
