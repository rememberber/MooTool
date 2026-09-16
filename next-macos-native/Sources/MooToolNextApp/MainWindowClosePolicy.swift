import AppKit
import SwiftUI

struct MainWindowAccessor: NSViewRepresentable {
    let onWindow: (NSWindow) -> Void
    func makeNSView(context: Context) -> NSView {
        let view = NSView(frame: .zero)
        DispatchQueue.main.async { [weak view] in
            if let window = view?.window { onWindow(window) }
        }
        return view
    }
    func updateNSView(_ nsView: NSView, context: Context) {
        DispatchQueue.main.async {
            if let window = nsView.window { onWindow(window) }
        }
    }
}

extension AppDelegate: NSWindowDelegate {
    func attachMainWindow(_ window: NSWindow) {
        guard window !== mainWindow else { return }
        mainWindow = window
        window.delegate = self
    }

    func windowShouldClose(_ sender: NSWindow) -> Bool {
        if isQuitting || CommandLine.arguments.contains("--smoke-test") || CommandLine.arguments.contains("--verify-workspace") {
            return true
        }
        let behavior = nativeDefaults.string(forKey: "general.closeBehavior") ?? "ask"
        switch behavior {
        case "quit":
            isQuitting = true
            NSApp.terminate(nil)
            return true
        case "hide":
            sender.orderOut(nil)
            return false
        default:
            return handleClosePrompt(sender)
        }
    }

    private func handleClosePrompt(_ window: NSWindow) -> Bool {
        let alert = NSAlert()
        alert.alertStyle = .informational
        alert.messageText = "关闭 MooTool？"
        alert.informativeText = "可隐藏到菜单栏图标继续运行，或完全退出应用。"
        alert.addButton(withTitle: "隐藏窗口")
        alert.addButton(withTitle: "退出")
        alert.addButton(withTitle: "取消")
        switch alert.runModal() {
        case .alertFirstButtonReturn:
            window.orderOut(nil)
            return false
        case .alertSecondButtonReturn:
            isQuitting = true
            NSApp.terminate(nil)
            return true
        default:
            return false
        }
    }
}
