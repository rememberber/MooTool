import SwiftUI
import AppKit
import MooToolNextCore

struct PersistedHSplit<Leading: View, Trailing: View>: View {
    @Environment(AppStore.self) private var store
    let toolID: String
    var paneIndex = 0
    var defaultLeading: CGFloat
    var minLeading: CGFloat
    var maxLeading: CGFloat
    var minTrailing: CGFloat? = nil
    var persistsTrailing = false
    @ViewBuilder var leading: () -> Leading
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        GeometryReader { geometry in
            // Geometry can briefly be zero while restoring a window. Also reserve
            // the divider before sharing a narrow container between both panes.
            let available = max(0, geometry.size.width - 6)
            let minimum = min(minLeading, available / 2)
            let trailingMinimum = min(minTrailing ?? minLeading, available - minimum)
            let maximum = max(minimum, min(maxLeading, available - trailingMinimum))
            let width = persistsTrailing
                ? available - store.paneWidth(toolID: toolID, index: paneIndex, default: available - defaultLeading, min: available - maximum, max: available - minimum)
                : store.paneWidth(toolID: toolID, index: paneIndex, default: defaultLeading, min: minimum, max: maximum)
            HStack(spacing: 0) {
                leading().frame(width: width)
                PaneResizeDivider(vertical: true, current: width, min: minimum, max: maximum) { next in
                    store.setPaneWidth(toolID: toolID, index: paneIndex, value: persistsTrailing ? available - next : next, slots: 2)
                } onReset: {
                    store.setPaneWidth(toolID: toolID, index: paneIndex, value: persistsTrailing ? available - defaultLeading : defaultLeading, slots: 2)
                }
                .jsonAcceptanceControl(toolID == "json" && paneIndex == 1 ? "json.inspector.divider" : "split.\(toolID).\(paneIndex)")
                trailing().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }
}

struct PaneResizeDivider: View {
    var vertical = true
    @Environment(\.appLanguage) private var language
    let current: CGFloat
    let min: CGFloat
    let max: CGFloat
    let onResize: (CGFloat) -> Void
    let onReset: () -> Void
    @State private var origin: CGFloat?

    var body: some View {
        Color.clear
            .frame(width: vertical ? 6 : nil, height: vertical ? nil : 6)
            .overlay {
                Rectangle().fill(Color(nsColor: .separatorColor))
                    .frame(width: vertical ? 1 : nil, height: vertical ? nil : 1)
            }
            .contentShape(Rectangle())
            .onTapGesture(count: 2, perform: onReset)
            .gesture(DragGesture(minimumDistance: 1).onChanged { value in
                if origin == nil { origin = current }
                let delta = vertical ? value.translation.width : value.translation.height
                onResize((origin! + delta).clamped(to: min...max))
            }.onEnded { _ in origin = nil })
            .onHover { hovering in
                if hovering { (vertical ? NSCursor.resizeLeftRight : NSCursor.resizeUpDown).push() }
                else { NSCursor.pop() }
            }
            .accessibilityElement()
            .accessibilityLabel(AppLocalization.string("layout.resizePane", language: language))
            .accessibilityValue("\(Int(current.rounded()))")
            .accessibilityAdjustableAction { direction in
                let delta: CGFloat = direction == .increment ? 24 : -24
                onResize((current + delta).clamped(to: min...max))
            }
            .accessibilityAction(named: Text(AppLocalization.string("layout.resetPane", language: language)), onReset)
            .help(AppLocalization.string("layout.resizeHint", language: language))
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self { min(max(self, range.lowerBound), range.upperBound) }
}
