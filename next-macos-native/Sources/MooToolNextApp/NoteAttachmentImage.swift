import SwiftUI
import MooToolNextCore

struct NoteAttachmentImage: View {
    let path: String
    let alt: String
    let availableWidth: CGFloat
    @Environment(AppStore.self) private var store
    @State private var image: CGImage?
    @State private var failure: String?
    @State private var zoomed = false
    private var key: String { path + ":\(store.attachmentGeneration)" }
    var body: some View {
        Group {
            if let image {
                Button { zoomed = true } label: {
                    let width = min(availableWidth, CGFloat(image.width))
                    Image(decorative: image, scale: 1).resizable()
                        .frame(width: width, height: width * CGFloat(image.height) / CGFloat(image.width)).clipShape(RoundedRectangle(cornerRadius: 6))
                }.buttonStyle(.plain).help("点击查看图片并缩放").accessibilityLabel(alt.isEmpty ? "图片附件" : alt)
                    .jsonAcceptanceControl("note.image.ready." + path)
            } else if let failure {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "photo.badge.exclamationmark").foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: 5) {
                        Text(alt.isEmpty ? "图片不可用" : alt).font(.callout.weight(.medium))
                        Text(failure).font(.caption).foregroundStyle(.secondary)
                        Text(path).font(.caption2.monospaced()).foregroundStyle(.tertiary).lineLimit(2).textSelection(.enabled)
                    }
                    Spacer(minLength: 0)
                }.padding(14).background(.quaternary.opacity(0.3), in: RoundedRectangle(cornerRadius: 8)).jsonAcceptanceControl("note.image.missing." + path)
            } else { ProgressView("加载图片…").controlSize(.small).padding(12) }
        }.frame(maxWidth: .infinity, alignment: .leading)
        .task(id: key) {
            image = nil; failure = nil
            guard NoteAttachment.isManagedPath(path) else { failure = "仅预览本工作区的图片附件。"; return }
            guard let attachment = store.noteAttachments.first(where: { $0.path == path }) else { failure = "未找到图片附件，正文引用仍保留。"; return }
            let repository = store.repository.attachmentRepository
            do {
                let loading = Task.detached(priority: .userInitiated) { try Task.checkCancellation(); return try repository.thumbnail(attachment, maximumPixels: 4096) }
                let value = try await withTaskCancellationHandler { try await loading.value } onCancel: { loading.cancel() }
                try Task.checkCancellation(); image = value
            } catch { if !Task.isCancelled { failure = "图片文件丢失、损坏或无法读取。" } }
        }
        .sheet(isPresented: $zoomed) { if let image { NoteImageDetail(image: image, title: alt.isEmpty ? "图片附件" : alt) } }
    }
}
private struct NoteImageDetail: View {
    let image: CGImage
    let title: String
    @State private var scale = 1.0
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(title).font(.headline).lineLimit(1); Spacer()
                Text("缩放").font(.caption)
                Slider(value: $scale, in: 0.25...3).frame(width: 140)
                Text("\(Int(scale * 100))%").monospacedDigit().font(.caption).frame(width: 40)
                Button("完成") { dismiss() }.keyboardShortcut(.cancelAction).jsonAcceptanceControl("note.image.done")
            }.padding(14)
            Divider()
            ScrollView([.horizontal, .vertical]) {
                Image(decorative: image, scale: 1).resizable().aspectRatio(contentMode: .fit)
                    .frame(width: min(720, CGFloat(image.width)) * scale).padding(20)
            }.frame(maxWidth: .infinity, maxHeight: .infinity).background(.quaternary.opacity(0.2))
        }.frame(width: 780, height: 620)
    }
}
