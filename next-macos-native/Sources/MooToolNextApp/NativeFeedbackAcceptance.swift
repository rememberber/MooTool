import SwiftUI
import MooToolNextCore

@MainActor enum NativeFeedbackAcceptance {
    static func run(store: AppStore, output: URL) async throws {
        let queue = NativeFeedbackQueue()
        let original = NativeFeedbackItem(message: "same result", tone: .success)
        queue.receive(original, busy: true)
        try require(queue.items.isEmpty, "Busy feedback must wait for completion")
        queue.receive(original, busy: false); queue.receive(original, busy: false)
        try require(queue.items.count == 1, "One event must not appear twice")
        for _ in 0..<5 { queue.receive(NativeFeedbackItem(message: "same result", tone: .success), busy: false) }
        try require(queue.items.count == 4 && !queue.items.contains(where: { $0.id == original.id }), "Keep the latest four events, including repeated messages")
        queue.dismiss(queue.items[0].id)
        try require(queue.items.count == 3, "Dismiss must remove only its card")
        queue.reset(to: original); queue.receive(original, busy: false)
        try require(queue.items.isEmpty, "Reopening a tool must not replay stale feedback")

        guard let window = NSApp.windows.first(where: { $0.isVisible && $0.frame.width > 700 }) else { throw ToolError("No acceptance window") }
        let originalContent = window.contentView
        let originalMinSize = window.minSize
        let originalFrame = window.frame
        let originalAppearance = window.appearance
        defer { window.contentView = originalContent; window.minSize = originalMinSize; window.appearance = originalAppearance; window.setFrame(originalFrame, display: true) }
        window.minSize = .zero
        let draft = store.draft("calculator")
        window.contentView = NSHostingView(rootView: ToolRouter(id: "calculator").environment(store))
        window.makeKeyAndOrderFront(nil)
        try await settle(window)
        let before = draft.record
        draft.status = "已复制到剪贴板"
        try await settle(window)
        try require(cards(window).count == 1, "Tool action must display a floating card")
        let dismiss = views(window).first { $0.identifier?.rawValue.hasPrefix("feedback.dismiss.") == true }
        try require(dismiss != nil, "Dismiss button must exist")
        if let dismiss { try click(dismiss, window: window) }
        try await settle(window)
        try require(cards(window).isEmpty, "Real close button must dismiss the card")
        draft.status = "已复制到剪贴板"
        try await settle(window)
        try require(cards(window).count == 1, "Repeating the same action must notify again")
        try await Task.sleep(for: .milliseconds(3300)); try await settle(window)
        try require(cards(window).isEmpty, "Feedback must expire automatically")
        draft.error = "无法读取文件，请检查路径。"
        try await settle(window)
        try require(cards(window).count == 1, "Errors must display a card")
        draft.documentID = UUID()
        try await settle(window)
        try require(cards(window).isEmpty, "Document switch must remove stale feedback")
        draft.documentID = before.documentID
        try require(draft.record == before, "Transient feedback must not change saved content")
        let reopened = ToolDraft(draft.record)
        try require(reopened.feedback == nil, "Recreated drafts must not replay notifications")

        var reports: [[String: Any]] = []
        for scheme in [ColorScheme.light, .dark] {
            for variant in ["glass", "legacy", "reduced-transparency", "high-contrast"] {
                for width in [940.0, 420.0] {
                    queue.reset(to: nil)
                    queue.receive(NativeFeedbackItem(message: "已复制到剪贴板", tone: .success), busy: false)
                    queue.receive(NativeFeedbackItem(message: "无法读取文件，请检查路径后重试。This is a longer message to verify wrapping in a narrow window.", tone: .error), busy: false)
                    let preview = NativeFeedbackPreview(queue: queue, legacy: variant == "legacy", accessibility: NativeFeedbackAccessibility(reduceTransparency: variant == "reduced-transparency", highContrast: variant == "high-contrast"))
                        .preferredColorScheme(scheme)
                    window.appearance = NSAppearance(named: scheme == .light ? .aqua : .darkAqua)
                    window.contentView = NSHostingView(rootView: preview)
                    window.setContentSize(NSSize(width: width, height: 500))
                    try await settle(window)
                    guard let content = window.contentView else { throw ToolError("No preview content") }
                    try require(abs(content.bounds.width - width) < 2, "Capture must use the requested narrow width")
                    try require(cards(window).count == 2, "Both preview cards must be visible")
                    for card in cards(window) {
                        let frame = card.convert(card.bounds, to: content)
                        try require(frame.minX >= 20 && frame.maxX <= content.bounds.maxX - 20 && frame.minY >= 20 && frame.maxY <= content.bounds.maxY - 20, "Card must fit the narrow window")
                    }
                    let name = "feedback-\(scheme == .light ? "light" : "dark")-\(variant)-\(Int(width)).png"
                    if CommandLine.arguments.contains("--window-capture") {
                        let capture = Process(); capture.executableURL = URL(fileURLWithPath: "/usr/sbin/screencapture")
                        capture.arguments = ["-x", "-o", "-l", String(window.windowNumber), output.appendingPathComponent(name).path]
                        try capture.run(); capture.waitUntilExit()
                        try require(capture.terminationStatus == 0, "Window capture failed")
                    } else {
                        guard let bitmap = content.bitmapImageRepForCachingDisplay(in: content.bounds) else { throw ToolError("No feedback bitmap") }
                        content.cacheDisplay(in: content.bounds, to: bitmap)
                        guard let data = bitmap.representation(using: .png, properties: [:]) else { throw ToolError("No feedback image") }
                        try data.write(to: output.appendingPathComponent(name))
                    }
                    reports.append(["screenshot": name, "material": variant, "width": width])
                }
            }
        }
        try JSONSerialization.data(withJSONObject: reports, options: [.prettyPrinted, .sortedKeys]).write(to: output.appendingPathComponent("feedback-report.json"))
    }
    private static func require(_ condition: Bool, _ message: String) throws { if !condition { throw ToolError("Feedback acceptance: " + message) } }
    private static func settle(_ window: NSWindow) async throws {
        window.contentView?.layoutSubtreeIfNeeded(); window.displayIfNeeded()
        try await Task.sleep(for: .milliseconds(300))
        window.contentView?.layoutSubtreeIfNeeded()
    }
    private static func views(_ window: NSWindow) -> [NSView] {
        var pending = [window.contentView].compactMap { $0 }, result: [NSView] = []
        while let view = pending.popLast() { result.append(view); pending.append(contentsOf: view.subviews) }
        return result
    }
    private static func cards(_ window: NSWindow) -> [NSView] { views(window).filter { $0.identifier?.rawValue.hasPrefix("feedback.card.") == true } }
    private static func click(_ view: NSView, window: NSWindow) throws {
        let point = view.convert(NSPoint(x: view.bounds.midX, y: view.bounds.midY), to: nil)
        for type in [NSEvent.EventType.leftMouseDown, .leftMouseUp] {
            guard let event = NSEvent.mouseEvent(with: type, location: point, modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                windowNumber: window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1) else { throw ToolError("No click event") }
            window.sendEvent(event)
        }
    }
}

private struct NativeFeedbackPreview: View {
    let queue: NativeFeedbackQueue
    let legacy: Bool
    let accessibility: NativeFeedbackAccessibility
    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            LinearGradient(colors: [.blue.opacity(0.3), .purple.opacity(0.2), Color(nsColor: .windowBackgroundColor)], startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(alignment: .leading, spacing: 16) {
                Text("MooTool").font(.largeTitle)
                Text("浮动提示 · Floating feedback").font(.title3)
                Spacer()
            }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading).padding(24)
            NativeFeedbackViewport(queue: queue, forceLegacyMaterial: legacy, autoDismiss: false, accessibilityOverride: accessibility).padding(22)
        }
    }
}
