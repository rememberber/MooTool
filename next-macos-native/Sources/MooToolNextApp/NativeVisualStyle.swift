import SwiftUI
import AppKit

/// Shared metrics for inset tool pages and edge-to-edge document workspaces.
enum NativeVisualStyle {
    static let pageInset: CGFloat = 20
    static let contentSpacing: CGFloat = 12
    static let workspaceInset: CGFloat = 16
    static let toolbarHeight: CGFloat = 44
    static let paneHeaderHeight: CGFloat = 36
    static let statusHeight: CGFloat = 32
    static let panelRadius: CGFloat = 8
    static let panelTitle = Font.system(size: 12, weight: .medium)
    static let auxiliary = Font.system(size: 11)
}

/// Use system control rendering for hover, focus, selection and accessibility.
struct NativeToolbarButton: View {
    let title: String
    let symbol: String
    var active: Bool? = nil
    let action: () -> Void

    var body: some View {
        Group {
            if let active {
                Toggle(isOn: Binding(get: { active }, set: { _ in action() })) {
                    Label(title, systemImage: symbol)
                }
                .toggleStyle(.button)
            } else {
                Button(action: action) { Label(title, systemImage: symbol) }
                    .buttonStyle(.borderless)
            }
        }
        .labelStyle(.iconOnly)
        .controlSize(.regular)
        .frame(minWidth: 28, minHeight: 28)
        .help(title)
        .accessibilityLabel(title)
    }
}

/// Dedicated syntax colors, separate from the system accent and UI status colors.
/// Dynamic providers follow the containing window's effective appearance.
enum NativeSyntaxPalette {
    static let key = adaptive("key", light: 0x365f91, dark: 0x93b9e8)
    static let string = adaptive("string", light: 0x28704f, dark: 0x99c7a6)
    static let literal = adaptive("literal", light: 0x92531d, dark: 0xdcb583)
    static let keyword = adaptive("keyword", light: 0x79529b, dark: 0xc6a5df)
    static let link = adaptive("link", light: 0x236d78, dark: 0x8bc6cf)
    static let comment = adaptive("comment", light: 0x6a6c73, dark: 0xa0a3ab)

    private static func adaptive(_ name: String, light: UInt32, dark: UInt32) -> NSColor {
        NSColor(name: NSColor.Name("MooTool.syntax." + name)) { appearance in
            let rgb = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua ? dark : light
            return NSColor(srgbRed: CGFloat((rgb >> 16) & 255) / 255,
                           green: CGFloat((rgb >> 8) & 255) / 255,
                           blue: CGFloat(rgb & 255) / 255, alpha: 1)
        }
    }
}

/// AppKit supplies the search icon, clear control, focus ring and search semantics.
struct NativeSearchField: NSViewRepresentable {
    @Binding var text: String
    let prompt: String

    func makeCoordinator() -> Coordinator { Coordinator(self) }
    func makeNSView(context: Context) -> NSSearchField {
        let field = NSSearchField()
        field.delegate = context.coordinator
        field.sendsSearchStringImmediately = true
        field.sendsWholeSearchString = false
        field.controlSize = .regular
        field.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return field
    }
    func updateNSView(_ field: NSSearchField, context: Context) {
        context.coordinator.parent = self
        field.placeholderString = prompt
        field.setAccessibilityLabel(prompt)
        if field.stringValue != text { field.stringValue = text }
    }
    final class Coordinator: NSObject, NSSearchFieldDelegate {
        var parent: NativeSearchField
        init(_ parent: NativeSearchField) { self.parent = parent }
        func controlTextDidChange(_ notification: Notification) {
            guard let field = notification.object as? NSSearchField else { return }
            parent.text = field.stringValue
        }
    }
}
