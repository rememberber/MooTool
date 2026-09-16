import SwiftUI
import MooToolNextCore

extension Notification.Name {
    /// Smoke acceptance drives the same split math as the divider drag gesture.
    static let acceptanceNoteSplitDrag = Notification.Name("mootool.acceptance.note.split.drag")
}

/// Keep the editor alive across all modes; split width persists in `layoutPaneSizes` (`quick-note-editor-preview`).
struct NoteEditorSplit<Editor: View, Preview: View>: View {
    @Environment(AppStore.self) private var store
    let mode: NoteViewMode
    @ViewBuilder let editor: () -> Editor
    @ViewBuilder let preview: () -> Preview
    private let paneKey = "quick-note-editor-preview"
    @State private var dragStart: CGFloat?
    @State private var layoutAvailable: CGFloat = 0

    var body: some View {
        GeometryReader { geometry in
            let available = max(0, geometry.size.width - (mode == .split ? 7 : 0))
            let minimum = min(180, available / 2)
            let defaultSplit = min(max(minimum, available * 0.5), available - minimum)
            let splitWidth = mode == .split
                ? store.paneWidth(toolID: paneKey, index: 0, default: defaultSplit, min: minimum, max: available - minimum)
                : defaultSplit
            let editorWidth = mode == .editor ? available : mode == .preview ? 0 : splitWidth
            HStack(spacing: 0) {
                editor().frame(width: editorWidth, height: geometry.size.height)
                    .clipped().opacity(mode == .preview ? 0 : 1)
                    .allowsHitTesting(mode != .preview).accessibilityHidden(mode == .preview)
                if mode == .split {
                    Color.clear.frame(width: 7)
                        .overlay { Rectangle().fill(Color(nsColor: .separatorColor)).frame(width: 1) }
                        .contentShape(Rectangle())
                        .onHover { ( $0 ? NSCursor.resizeLeftRight : NSCursor.arrow ).set() }
                        .gesture(DragGesture(minimumDistance: 0).onChanged { value in
                            if dragStart == nil { dragStart = splitWidth }
                            let next = min(max(minimum, (dragStart ?? splitWidth) + value.translation.width), available - minimum)
                            store.setPaneWidth(toolID: paneKey, index: 0, value: next, slots: 2)
                        }.onEnded { _ in dragStart = nil })
                        .onTapGesture(count: 2) { store.setPaneWidth(toolID: paneKey, index: 0, value: defaultSplit, slots: 2) }
                        .accessibilityElement().accessibilityLabel("编辑与预览分隔条")
                        .accessibilityAdjustableAction { direction in
                            let step: CGFloat = direction == .increment ? 24 : -24
                            let next = (splitWidth + step).clamped(to: minimum...(available - minimum))
                            store.setPaneWidth(toolID: paneKey, index: 0, value: next, slots: 2)
                        }
                        .jsonAcceptanceControl("note.split.divider")
                }
                if mode != .editor {
                    preview().frame(width: mode == .preview ? available : available - splitWidth, height: geometry.size.height)
                        .clipped().jsonAcceptanceControl("note.preview.viewport")
                }
            }
            .onChange(of: available) { _, value in layoutAvailable = value }
            .onReceive(NotificationCenter.default.publisher(for: .acceptanceNoteSplitDrag)) { note in
                guard CommandLine.arguments.contains("--smoke-test"), mode == .split,
                      let delta = note.userInfo?["delta"] as? CGFloat else { return }
                let width = max(layoutAvailable, 1)
                let minimum = min(180, width / 2)
                let current = store.paneWidth(toolID: paneKey, index: 0, default: width * 0.5, min: minimum, max: width - minimum)
                let next = min(max(minimum, current + delta), width - minimum)
                store.setPaneWidth(toolID: paneKey, index: 0, value: next, slots: 2)
            }
        }
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self { min(max(self, range.lowerBound), range.upperBound) }
}
