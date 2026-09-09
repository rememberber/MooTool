import SwiftUI
import MooToolNextCore

/// Native block rendering; inline emphasis and links use Foundation Markdown.
struct MarkdownPreview: View {
    let text: String
    var fontName = ""
    var pointSize = 14.0
    @State private var document: MarkdownDocument?
    @State private var error: String?
    var body: some View {
        GeometryReader { geometry in
        ScrollView {
            if let error { ContentUnavailableView("无法预览", systemImage: "doc.text", description: Text(error)) }
            else if let document {
                LazyVStack(alignment: .leading, spacing: 11) {
                    ForEach(document.blocks) { block in
                        switch block.kind {
                        case "heading": inline(block.text).font(.system(size: max(pointSize + 1, 31 - Double(block.level) * 3), weight: .semibold)).padding(.top, 7)
                        case "code":
                            VStack(alignment: .leading, spacing: 8) {
                                HStack { Text(block.language.isEmpty ? "代码" : block.language).font(.caption).foregroundStyle(.secondary); Spacer(); Button { FilePanels.copy(block.text) } label: { Image(systemName: "doc.on.doc") }.buttonStyle(.borderless).help("复制代码") }
                                ScrollView(.horizontal) { Text(block.text).font(.system(size: pointSize, design: .monospaced)).fixedSize(horizontal: true, vertical: false) }
                            }.padding(13).background(.quaternary.opacity(0.4), in: RoundedRectangle(cornerRadius: 8))
                        case "quote": HStack { RoundedRectangle(cornerRadius: 2).fill(.tint).frame(width: 3); inline(block.text).foregroundStyle(.secondary) }.fixedSize(horizontal: false, vertical: true)
                        case "list":
                            HStack(alignment: .top, spacing: 8) {
                                if let checked = block.checked { Image(systemName: checked ? "checkmark.square" : "square").foregroundStyle(.secondary).accessibilityLabel(checked ? "已完成" : "未完成") }
                                else { Text(block.marker).foregroundStyle(.secondary) }
                                inline(block.text)
                            }.padding(.leading, CGFloat(block.level) * 14)
                        case "table": table(block)
                        case "image": NoteAttachmentImage(path: block.destination, alt: block.text, availableWidth: max(1, geometry.size.width - 48))
                        case "divider": Divider().padding(.vertical, 7)
                        default: inline(block.text)
                        }
                    }
                }.font(Font(NativeNoteStyle.font(fontName, size: pointSize))).textSelection(.enabled).frame(width: max(1, geometry.size.width - 48), alignment: .leading).padding(24)
                    .jsonAcceptanceControl("note.preview.ready")
            } else { ProgressView().padding(24) }
        }
        .task(id: text) {
            do {
                try await Task.sleep(for: .milliseconds(150))
                let parsing = Task.detached(priority: .userInitiated) { try MarkdownDocument(text) }
                let result = try await withTaskCancellationHandler { try await parsing.value } onCancel: { parsing.cancel() }
                try Task.checkCancellation(); document = result; error = nil
            } catch { if !Task.isCancelled { self.error = error.localizedDescription; document = nil } }
        }
        .environment(\.openURL, OpenURLAction { url in ["http", "https", "mailto"].contains(url.scheme?.lowercased() ?? "") ? .systemAction : .discarded })
        }
    }
    private func table(_ block: MarkdownBlock) -> some View {
        ScrollView(.horizontal) {
            Grid(horizontalSpacing: 0, verticalSpacing: 0) {
                ForEach(block.cells.indices, id: \.self) { row in
                    GridRow {
                        ForEach(block.cells[row].indices, id: \.self) { column in
                            inline(block.cells[row][column]).fontWeight(row == 0 ? .semibold : .regular)
                                .padding(.horizontal, 11).padding(.vertical, 8)
                                .frame(minWidth: 90, maxWidth: 300, alignment: block.alignments[column] == "right" ? .trailing : block.alignments[column] == "center" ? .center : .leading)
                                .gridColumnAlignment(block.alignments[column] == "right" ? .trailing : block.alignments[column] == "center" ? .center : .leading)
                                .background(row == 0 ? Color.primary.opacity(0.07) : row.isMultiple(of: 2) ? Color.primary.opacity(0.025) : Color.clear)
                                .overlay(Rectangle().strokeBorder(.quaternary, lineWidth: 0.5))
                        }
                    }
                }
            }.clipShape(RoundedRectangle(cornerRadius: 6)).overlay(RoundedRectangle(cornerRadius: 6).strokeBorder(.quaternary))
        }
    }
    private func inline(_ text: String) -> Text {
        Text((try? AttributedString(markdown: text, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace))) ?? AttributedString(text))
    }
}
