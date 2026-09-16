import SwiftUI
import MooToolNextCore

@MainActor enum NativeReformatAcceptance {
    static func run(store: AppStore, window: NSWindow) async throws {
        let original = store.snapshot(); defer { store.restore(original) }
        let draft = store.draft("reformat")
        store.selected = "reformat"
        draft.input = ReformatType.nginx.sample
        draft.reformat = ReformatOptions()
        window.setContentSize(NSSize(width: 1200, height: 800)); window.makeKeyAndOrderFront(nil)
        try await settle(window)
        guard let editor = allViews(window).compactMap({ $0 as? NSTextView }).first(where: { $0.identifier?.rawValue.hasPrefix("reformat:draft") == true && $0.isEditable }) else {
            throw ToolError("格式化验收：找不到文本编辑器。")
        }
        try pressFormat(window)
        try await finish(draft, window)
        try await waitForTextFormat(draft: draft, editor: editor, store: store, window: window)
        guard draft.error == nil, draft.input.contains("\n"), editor.undoManager?.canUndo == true,
              store.history.first(where: { $0.toolID == "reformat" })?.draft.output == draft.input else {
            throw ToolError("格式化验收：文本结果、撤销或历史不一致：\(draft.error ?? "")")
        }
        editor.undoManager?.undo(); try await settle(window)
        guard draft.input == ReformatType.nginx.sample else { throw ToolError("格式化验收：撤销没有还原文本。") }
        editor.undoManager?.redo(); try await settle(window)
        guard draft.input.contains("\n") else { throw ToolError("格式化验收：重做没有还原结果。") }

        var file = ReformatOptions(); file.type = .java; file.tab = .file; file.indent = 2
        file.fileName = "Demo.java"; file.fileSource = ReformatType.java.sample
        draft.reformat = file
        try await settle(window)
        try pressFormat(window)
        try await finish(draft, window)
        guard draft.error == nil, draft.reformat?.fileSource == ReformatType.java.sample,
              draft.reformat?.fileResult.contains("\n") == true,
              store.history.first(where: { $0.toolID == "reformat" })?.draft.reformat?.fileName == "Demo.java" else {
            throw ToolError("格式化验收：文件结果或历史不一致：\(draft.error ?? "")")
        }
        let history = store.history.first { $0.toolID == "reformat" }!.draft
        store.restoreDraft("reformat", record: history)
        guard draft.reformat?.tab == .file, draft.reformat?.fileResult == history.output,
              draft.reformat?.fileSource == ReformatType.java.sample else { throw ToolError("格式化验收：文件历史恢复失败。") }
        store.saveNow()
        guard try store.repository.load().drafts["reformat"]?.reformat == draft.reformat else { throw ToolError("格式化验收：文件工作区未持久化。") }
    }
    private static func finish(_ draft: ToolDraft, _ window: NSWindow) async throws {
        for _ in 0..<100 {
            try await Task.sleep(for: .milliseconds(100))
            if !draft.busy { try await settle(window); return }
        }
        throw ToolError("格式化验收：格式化操作未结束。")
    }

    private static func waitForTextFormat(draft: ToolDraft, editor: NSTextView, store: AppStore, window: NSWindow) async throws {
        for _ in 0..<40 {
            if draft.error == nil, draft.input.contains("\n"), editor.undoManager?.canUndo == true,
               store.history.first(where: { $0.toolID == "reformat" })?.draft.output == draft.input {
                return
            }
            try await Task.sleep(for: .milliseconds(100))
            try await settle(window)
        }
    }
    private static func settle(_ window: NSWindow) async throws {
        window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded()
        try await Task.sleep(for: .milliseconds(400))
        window.contentView?.layoutSubtreeIfNeeded()
    }
    private static func allViews(_ window: NSWindow) -> [NSView] {
        var pending = [window.contentView].compactMap { $0 }, found: [NSView] = []
        while let view = pending.popLast() { found.append(view); pending.append(contentsOf: view.subviews) }
        return found
    }
    private static func pressFormat(_ window: NSWindow) throws {
        guard let anchor = allViews(window).first(where: { $0.identifier?.rawValue == "reformat.acceptance.格式化" }),
              let owner = anchor.window else { throw ToolError("格式化验收：找不到格式化按钮。") }
        let point = anchor.convert(NSPoint(x: anchor.bounds.midX, y: anchor.bounds.midY), to: nil)
        guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: point, modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                                            windowNumber: owner.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
              let up = NSEvent.mouseEvent(with: .leftMouseUp, location: point, modifierFlags: [], timestamp: down.timestamp + 0.01,
                                          windowNumber: owner.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else {
            throw ToolError("格式化验收：无法创建按钮事件。")
        }
        owner.makeKeyAndOrderFront(nil); NSApp.postEvent(up, atStart: true); owner.sendEvent(down)
    }
}

private struct ReformatAcceptanceAnchor: NSViewRepresentable {
    let title: String
    func makeNSView(context: Context) -> NSView { let view = AnchorView(); view.identifier = NSUserInterfaceItemIdentifier("reformat.acceptance." + title); return view }
    func updateNSView(_ view: NSView, context: Context) { view.identifier = NSUserInterfaceItemIdentifier("reformat.acceptance." + title) }
    private final class AnchorView: NSView { override func hitTest(_ point: NSPoint) -> NSView? { nil } }
}
extension View {
    @ViewBuilder func reformatAcceptanceControl(_ title: String) -> some View {
        if CommandLine.arguments.contains("--smoke-test") { self.background(ReformatAcceptanceAnchor(title: title)) }
        else { self }
    }
}
