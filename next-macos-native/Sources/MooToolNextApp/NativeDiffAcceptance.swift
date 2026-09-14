import SwiftUI
import MooToolNextCore

@MainActor enum NativeDiffAcceptance {
    static func run(store: AppStore, window: NSWindow) async throws {
        let original = store.snapshot(); defer { store.restore(original) }
        let draft = store.draft("textDiff")
        store.selected = "textDiff"
        draft.input = "one\ntwo\n"; draft.secondary = "one\nthree\nplus\n"
        draft.textDiff = TextDiffOptions()
        window.setContentSize(NSSize(width: 1200, height: 800)); window.makeKeyAndOrderFront(nil)
        try await settle(window)
        guard draft.output == "--- old\n+++ new\n@@ -1,3 +1,4 @@\n one\n-two\n+three\n+plus\n " else {
            throw ToolError("文本对比验收：统一差异没有自动更新。")
        }
        let left = try editor(window, side: "left"), right = try editor(window, side: "right")
        guard left.layoutManager?.temporaryAttribute(.backgroundColor, atCharacterIndex: 5, effectiveRange: nil) != nil,
              right.layoutManager?.temporaryAttribute(.backgroundColor, atCharacterIndex: 5, effectiveRange: nil) != nil else {
            throw ToolError("文本对比验收：字符差异未在编辑器中高亮。")
        }
        try press("比较", in: window); try await settle(window)
        guard store.history.first(where: { $0.toolID == "textDiff" })?.draft.secondary == draft.secondary else { throw ToolError("文本对比验收：比较历史未保存右侧正文。") }
        try press("下一处", in: window); try await settle(window)
        guard left.selectedRange().location == 4, right.selectedRange().location == 4 else { throw ToolError("文本对比验收：下一处未定位双侧编辑器。") }
        var options = draft.textDiff ?? TextDiffOptions(); options.display = .unified; draft.textDiff = options
        try await settle(window)
        guard let unified = try? editor(window, side: "unified"), !unified.isEditable,
              unified.string == draft.output else { throw ToolError("文本对比验收：统一视图未显示只读补丁。") }
        options.ignoreWhitespace = true; draft.textDiff = options
        draft.input = "one  two\n"; draft.secondary = "one two\n"
        try await settle(window)
        guard draft.output.contains("-one  two") && draft.output.contains("+one two") else { throw ToolError("文本对比验收：忽略空白错误地删除了统一补丁。") }
        store.restoreDraft("textDiff", record: store.history.first { $0.toolID == "textDiff" }!.draft)
        guard draft.input == "one\ntwo\n", draft.secondary == "one\nthree\nplus\n" else { throw ToolError("文本对比验收：历史恢复失败。") }
        store.saveNow()
        guard try store.repository.load().drafts["textDiff"]?.secondary == draft.secondary else { throw ToolError("文本对比验收：工作区保存失败。") }
    }
    static func verifyCompactLayout(_ window: NSWindow) throws {
        guard let content = window.contentView else { throw ToolError("文本对比验收：找不到窄窗口内容。") }
        for side in ["left", "right", "unified"] {
            let view = try editor(window, side: side)
            guard let scroll = view.enclosingScrollView else { throw ToolError("文本对比验收：找不到 \(side) 滚动区域。") }
            let frame = scroll.convert(scroll.bounds, to: content)
            guard frame.minX >= 180, frame.maxX <= content.bounds.maxX + 2 else {
                throw ToolError("文本对比验收：940px 窗口裁切了 \(side) 编辑区域。")
            }
        }
    }
    private static func editor(_ window: NSWindow, side: String) throws -> NSTextView {
        guard let view = allViews(window).compactMap({ $0 as? NSTextView }).first(where: { $0.identifier?.rawValue.hasPrefix("textDiff:\(side):") == true }) else {
            throw ToolError("文本对比验收：找不到 \(side) 编辑器。")
        }
        return view
    }
    private static func settle(_ window: NSWindow) async throws {
        window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded()
        try await Task.sleep(for: .milliseconds(600))
        window.contentView?.layoutSubtreeIfNeeded()
    }
    private static func allViews(_ window: NSWindow) -> [NSView] {
        var pending = [window.contentView].compactMap { $0 }, result: [NSView] = []
        while let view = pending.popLast() { result.append(view); pending.append(contentsOf: view.subviews) }
        return result
    }
    private static func press(_ title: String, in window: NSWindow) throws {
        guard let anchor = allViews(window).first(where: { $0.identifier?.rawValue == "diff.acceptance." + title }), let owner = anchor.window else {
            throw ToolError("文本对比验收：找不到 \(title) 按钮。")
        }
        let point = anchor.convert(NSPoint(x: anchor.bounds.midX, y: anchor.bounds.midY), to: nil)
        guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: point, modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                                            windowNumber: owner.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
              let up = NSEvent.mouseEvent(with: .leftMouseUp, location: point, modifierFlags: [], timestamp: down.timestamp + 0.01,
                                          windowNumber: owner.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else {
            throw ToolError("文本对比验收：无法创建 \(title) 点击事件。")
        }
        owner.makeKeyAndOrderFront(nil); NSApp.postEvent(up, atStart: true); owner.sendEvent(down)
    }
}
private struct DiffAcceptanceAnchor: NSViewRepresentable {
    let title: String
    func makeNSView(context: Context) -> NSView { let view = AnchorView(); view.identifier = NSUserInterfaceItemIdentifier("diff.acceptance." + title); return view }
    func updateNSView(_ view: NSView, context: Context) { view.identifier = NSUserInterfaceItemIdentifier("diff.acceptance." + title) }
    private final class AnchorView: NSView { override func hitTest(_ point: NSPoint) -> NSView? { nil } }
}
extension View {
    @ViewBuilder func diffAcceptanceControl(_ title: String) -> some View {
        if CommandLine.arguments.contains("--smoke-test") { self.background(DiffAcceptanceAnchor(title: title)) }
        else { self }
    }
}
