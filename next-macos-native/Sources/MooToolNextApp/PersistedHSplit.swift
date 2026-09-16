import SwiftUI
import MooToolNextCore

struct PersistedHSplit<Leading: View, Trailing: View>: View {
    @Environment(AppStore.self) private var store
    let toolID: String
    var paneIndex = 0
    var defaultLeading: CGFloat
    var minLeading: CGFloat
    var maxLeading: CGFloat
    @ViewBuilder var leading: () -> Leading
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        GeometryReader { geometry in
            let width = store.paneWidth(toolID: toolID, index: paneIndex, default: defaultLeading, min: minLeading, max: min(maxLeading, geometry.size.width - minLeading))
            HStack(spacing: 0) {
                leading().frame(width: width)
                PaneResizeDivider(vertical: true, current: width, min: minLeading, max: min(maxLeading, geometry.size.width - minLeading)) { next in
                    store.setPaneWidth(toolID: toolID, index: paneIndex, value: next, slots: 2)
                } onReset: {
                    store.setPaneWidth(toolID: toolID, index: paneIndex, value: defaultLeading, slots: 2)
                }
                trailing().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }
}

struct PaneResizeDivider: View {
    var vertical = true
    let current: CGFloat
    let min: CGFloat
    let max: CGFloat
    let onResize: (CGFloat) -> Void
    let onReset: () -> Void
    @State private var dragging = false
    @State private var origin: CGFloat?

    var body: some View {
        Rectangle()
            .fill(Color(nsColor: .separatorColor).opacity(dragging ? 0.9 : 0.35))
            .frame(width: vertical ? 6 : nil, height: vertical ? nil : 6)
            .contentShape(Rectangle())
            .onTapGesture(count: 2, perform: onReset)
            .gesture(DragGesture(minimumDistance: 1).onChanged { value in
                dragging = true
                if origin == nil { origin = current }
                let delta = vertical ? value.translation.width : value.translation.height
                onResize((origin! + delta).clamped(to: min...max))
            }.onEnded { _ in dragging = false; origin = nil })
            .help("拖动调整分栏 · 双击恢复默认宽度")
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self { min(max(self, range.lowerBound), range.upperBound) }
}
