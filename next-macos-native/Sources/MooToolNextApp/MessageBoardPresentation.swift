import AppKit
import IOKit.pwr_mgt
import SwiftUI

@MainActor
final class MessageBoardFullscreenPresenter: NSObject, NSWindowDelegate {
    private var window: NSWindow?
    private var keyMonitor: Any?
    private var assertionID: IOPMAssertionID = 0

    func present(text: String, size: Double, background: Color, foreground: Color, alignment: TextAlignment, windowTitle: String) {
        dismiss()
        let panel = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1100, height: 720),
            styleMask: [.titled, .closable, .resizable, .miniaturizable],
            backing: .buffered,
            defer: false)
        panel.title = windowTitle
        panel.isReleasedWhenClosed = false
        panel.delegate = self
        panel.contentView = NSHostingView(rootView: BoardDisplay(text: text, size: size, background: background, foreground: foreground, alignment: alignment))
        panel.center()
        panel.makeKeyAndOrderFront(nil)
        window = panel
        var id = IOPMAssertionID(0)
        let status = IOPMAssertionCreateWithName(
            kIOPMAssertPreventUserIdleDisplaySleep as CFString,
            IOPMAssertionLevel(kIOPMAssertionLevelOn),
            "MooTool Message Board" as CFString,
            &id)
        assertionID = status == kIOReturnSuccess ? id : 0
        keyMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self] event in
            if event.keyCode == 53 {
                self?.dismiss()
                return nil
            }
            return event
        }
        panel.toggleFullScreen(nil)
    }

    func dismiss() {
        if assertionID != 0 {
            IOPMAssertionRelease(assertionID)
            assertionID = 0
        }
        if let keyMonitor {
            NSEvent.removeMonitor(keyMonitor)
            self.keyMonitor = nil
        }
        window?.delegate = nil
        window?.close()
        window = nil
    }

    func windowWillClose(_ notification: Notification) {
        if assertionID != 0 {
            IOPMAssertionRelease(assertionID)
            assertionID = 0
        }
        if let keyMonitor {
            NSEvent.removeMonitor(keyMonitor)
            self.keyMonitor = nil
        }
        window = nil
    }
}

enum MessageBoardColorCoding {
    static func color(hex: String) -> Color? {
        guard hex.count == 6, let value = UInt32(hex, radix: 16) else { return nil }
        let r = Double((value >> 16) & 0xFF) / 255
        let g = Double((value >> 8) & 0xFF) / 255
        let b = Double(value & 0xFF) / 255
        return Color(red: r, green: g, blue: b)
    }

    static func hex(_ color: Color) -> String {
        let ns = NSColor(color).usingColorSpace(.deviceRGB) ?? NSColor(color)
        let r = Int((ns.redComponent * 255).rounded())
        let g = Int((ns.greenComponent * 255).rounded())
        let b = Int((ns.blueComponent * 255).rounded())
        return String(format: "%02X%02X%02X", r, g, b)
    }
}
