import SwiftUI
import MooToolNextCore

struct NoteAttachmentImage: View {
    let path: String
    let alt: String
    let availableWidth: CGFloat
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var image: CGImage?
    @State private var failure: String?
    @State private var zoomed = false
    private var key: String { path + ":\(store.attachmentGeneration)" }
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        Group {
            if let image {
                Button { zoomed = true } label: {
                    let width = min(availableWidth, CGFloat(image.width))
                    Image(decorative: image, scale: 1).resizable()
                        .frame(width: width, height: width * CGFloat(image.height) / CGFloat(image.width)).clipShape(RoundedRectangle(cornerRadius: 6))
                }.buttonStyle(.plain).help(loc("quickNote.image.previewHelp")).accessibilityLabel(alt.isEmpty ? loc("quickNote.image.attachmentLabel") : alt)
                    .jsonAcceptanceControl("note.image.ready." + path)
            } else if let failure {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "photo.badge.exclamationmark").foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: 5) {
                        Text(alt.isEmpty ? loc("quickNote.image.unavailable") : alt).font(.callout.weight(.medium))
                        Text(failure).font(.caption).foregroundStyle(.secondary)
                        Text(path).font(.caption2.monospaced()).foregroundStyle(.tertiary).lineLimit(2).textSelection(.enabled)
                    }
                    Spacer(minLength: 0)
                }.padding(14).background(.quaternary.opacity(0.3), in: RoundedRectangle(cornerRadius: 8)).jsonAcceptanceControl("note.image.missing." + path)
            } else { ProgressView(loc("quickNote.image.loading")).controlSize(.small).padding(12) }
        }.frame(maxWidth: .infinity, alignment: .leading)
        .task(id: key) {
            image = nil; failure = nil
            guard NoteAttachment.isManagedPath(path) else { failure = loc("quickNote.image.managedOnly"); return }
            guard let attachment = store.noteAttachments.first(where: { $0.path == path }) else { failure = loc("quickNote.image.notFound"); return }
            let repository = store.repository.attachmentRepository
            do {
                let loading = Task.detached(priority: .userInitiated) { try Task.checkCancellation(); return try repository.thumbnail(attachment, maximumPixels: 4096) }
                let value = try await withTaskCancellationHandler { try await loading.value } onCancel: { loading.cancel() }
                try Task.checkCancellation(); image = value
            } catch { if !Task.isCancelled { failure = loc("quickNote.image.loadFailed") } }
        }
        .sheet(isPresented: $zoomed) {
            if let image {
                NoteImageDetail(image: image, title: alt.isEmpty ? loc("quickNote.image.attachmentLabel") : alt)
                    .environment(\.appLanguage, language)
            }
        }
    }
}
private struct NoteImageDetail: View {
    let image: CGImage
    let title: String
    @State private var scale = 1.0
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(title).font(.headline).lineLimit(1); Spacer()
                Text(loc("quickNote.image.zoomLabel")).font(.caption)
                Slider(value: $scale, in: 0.25...3).frame(width: 140)
                Text("\(Int(scale * 100))%").monospacedDigit().font(.caption).frame(width: 40)
                Button(loc("common.done")) { dismiss() }.keyboardShortcut(.cancelAction).jsonAcceptanceControl("note.image.done")
            }.padding(14)
            Divider()
            ScrollView([.horizontal, .vertical]) {
                Image(decorative: image, scale: 1).resizable().aspectRatio(contentMode: .fit)
                    .frame(width: min(720, CGFloat(image.width)) * scale).padding(20)
            }.frame(maxWidth: .infinity, maxHeight: .infinity).background(.quaternary.opacity(0.2))
        }.frame(width: 780, height: 620)
    }
}
