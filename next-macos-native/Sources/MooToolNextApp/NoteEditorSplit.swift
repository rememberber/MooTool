import SwiftUI
import MooToolNextCore

/// Keep the editor alive across all modes; the divider ratio is independent of text width.
struct NoteEditorSplit<Editor: View, Preview: View>: View {
    let mode: NoteViewMode
    @ViewBuilder let editor: () -> Editor
    @ViewBuilder let preview: () -> Preview
    @State private var fraction: CGFloat = 0.5
    @State private var dragStart: CGFloat?

    var body: some View {
        GeometryReader { geometry in
            let available = max(0, geometry.size.width - (mode == .split ? 7 : 0))
            let minimum = min(180, available / 2)
            let splitWidth = min(max(minimum, available * fraction), available - minimum)
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
                            fraction = min(max(minimum, (dragStart ?? splitWidth) + value.translation.width), available - minimum) / max(1, available)
                        }.onEnded { _ in dragStart = nil })
                        .onTapGesture(count: 2) { fraction = 0.5 }
                        .accessibilityElement().accessibilityLabel("编辑与预览分隔条")
                        .accessibilityAdjustableAction { direction in
                            let step: CGFloat = direction == .increment ? 0.05 : -0.05
                            fraction = min(max(minimum / max(1, available), fraction + step), 1 - minimum / max(1, available))
                        }
                        .jsonAcceptanceControl("note.split.divider")
                }
                if mode != .editor {
                    preview().frame(width: mode == .preview ? available : available - splitWidth, height: geometry.size.height)
                        .clipped().jsonAcceptanceControl("note.preview.viewport")
                }
            }
        }
    }
}
