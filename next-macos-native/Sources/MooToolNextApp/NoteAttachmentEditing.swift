import SwiftUI
import UniformTypeIdentifiers
import MooToolNextCore

/// Only the note editor supplies an image handler; ordinary text paste stays native.
final class NoteTextView: NSTextView {
    var imageTransfer: (([NoteImageSource], NSRange) -> Void)?
    static func sources(from pasteboard: NSPasteboard) -> [NoteImageSource] {
        let files = (pasteboard.readObjects(forClasses: [NSURL.self], options: [.urlReadingFileURLsOnly: true]) as? [URL] ?? [])
            .filter { ["png", "jpg", "jpeg", "gif", "bmp", "webp"].contains($0.pathExtension.lowercased()) }
        if !files.isEmpty { return files.map(NoteImageSource.file) }
        for type in [NSPasteboard.PasteboardType.png, .tiff, .init(UTType.jpeg.identifier), .init(UTType.gif.identifier), .init(UTType.webP.identifier), .init(UTType.bmp.identifier)] {
            if let data = pasteboard.data(forType: type) { return [.data(data)] }
        }
        return []
    }
    @discardableResult func pasteImages(from pasteboard: NSPasteboard) -> Bool {
        guard let imageTransfer, isEditable, let first = Self.sources(from: pasteboard).first else { return false }
        imageTransfer([first], selectedRange()); return true
    }
    override func paste(_ sender: Any?) { if !pasteImages(from: .general) { super.paste(sender) } }
    override func draggingEntered(_ sender: NSDraggingInfo) -> NSDragOperation {
        guard imageTransfer != nil, isEditable else { return super.draggingEntered(sender) }
        return sender.draggingPasteboard.types?.contains(.fileURL) == true || !Self.sources(from: sender.draggingPasteboard).isEmpty ? .copy : super.draggingEntered(sender)
    }
    override func draggingUpdated(_ sender: NSDraggingInfo) -> NSDragOperation { draggingEntered(sender) }
    override func performDragOperation(_ sender: NSDraggingInfo) -> Bool {
        guard let imageTransfer, isEditable else { return super.performDragOperation(sender) }
        let sources = Self.sources(from: sender.draggingPasteboard)
        if !sources.isEmpty {
            let point = convert(sender.draggingLocation, from: nil)
            let location = min((string as NSString).length, characterIndexForInsertion(at: point))
            imageTransfer(sources, NSRange(location: location, length: 0)); return true
        }
        if sender.draggingPasteboard.types?.contains(.fileURL) == true { return true }
        return super.performDragOperation(sender)
    }
}

enum NoteImageSource: Sendable {
    case file(URL), data(Data)
    func prepare() throws -> NoteImagePayload { switch self { case .file(let url): return try NoteImagePayload(file: url); case .data(let data): return try NoteImagePayload(data: data) } }
}

/// Serializes multiple drops and rapid pastes without applying stale reads to another document.
@MainActor final class NoteAttachmentInsertionQueue {
    private struct Request {
        let sources: [NoteImageSource]
        let selection: NSRange
        let source: String
        let document: UUID
        let generation: Int
        let revision: Int
    }
    private struct Applied {
        let selection: NSRange
        let source: String
        let content: String
        let caret: Int
    }
    private var requests: [Request] = []
    private var task: Task<Void, Never>?
    private var token: UUID?
    private weak var owner: ToolDraft?
    func enqueue(_ sources: [NoteImageSource], selection: NSRange, store: AppStore, draft: ToolDraft, editor: NativeEditorBridge) {
        guard let document = draft.documentID else { draft.error = "请先保存或新建笔记，再插入图片附件。"; return }
        guard !store.persistenceBlocked else { draft.error = "工作区保存已暂停，暂时无法添加附件。"; return }
        guard !draft.busy || (task != nil && draft.noteOperationID == token) else { draft.error = "请等待当前操作完成后插入图片。"; return }
        guard !sources.isEmpty, sources.count <= 20, requests.count < 20 else { draft.error = "一次最多插入 20 张图片。"; return }
        requests.append(Request(sources: sources, selection: selection, source: draft.input, document: document, generation: store.editorRestoreGeneration, revision: draft.editorRevision))
        guard task == nil else { return }
        let current = UUID(); token = current; owner = draft; draft.noteOperationID = current; draft.busy = true; draft.error = nil
        task = Task { [weak self] in
            guard let self else { return }
            defer { if self.token == current { self.finish() } }
            var previous: Applied?
            while !self.requests.isEmpty, !Task.isCancelled, self.token == current {
                let request = self.requests.removeFirst()
                do {
                    var expected = request.source, selection = request.selection
                    if let previous, previous.source == request.source, previous.selection == request.selection {
                        expected = previous.content; selection = NSRange(location: previous.caret, length: 0)
                    }
                    guard self.isCurrent(request, store: store, draft: draft), draft.input == expected else { throw ToolError("正文或文档已变化，未插入过期的图片。") }
                    let preparation = Task.detached(priority: .userInitiated) {
                        try request.sources.map { source in try Task.checkCancellation(); return try source.prepare() }
                    }
                    let payloads = try await withTaskCancellationHandler { try await preparation.value } onCancel: { preparation.cancel() }
                    try Task.checkCancellation()
                    guard self.isCurrent(request, store: store, draft: draft), draft.input == expected else { throw ToolError("正文或文档已变化，未插入过期的图片。") }
                    var manifest = store.noteAttachments
                    for image in payloads where !manifest.contains(where: { $0.path == image.attachment.path }) { manifest.append(image.attachment) }
                    try NoteAttachmentRepository.validateManifest(manifest)
                    var content = expected, caret = selection.location
                    for image in payloads {
                        let insertion = NoteImageInsertion(content: content, selection: selection, markdown: image.attachment.markdown)
                        content = insertion.applying(to: content); caret = insertion.caret; selection = NSRange(location: caret, length: 0)
                    }
                    guard content.utf8.count <= 10 * 1024 * 1024 else { throw ToolError("插入图片后的正文超过 10 MB。") }
                    let repository = store.repository.attachmentRepository
                    let writing = Task.detached(priority: .userInitiated) { for image in payloads { try Task.checkCancellation(); try repository.write(image) } }
                    try await withTaskCancellationHandler { try await writing.value } onCancel: { writing.cancel() }
                    try Task.checkCancellation()
                    guard self.isCurrent(request, store: store, draft: draft), draft.input == expected,
                          editor.replace(content, expected: expected, action: "插入图片附件") else { throw ToolError("正文或文档已变化，未插入过期的图片。") }
                    store.noteAttachments = manifest; store.attachmentGeneration += 1
                    var options = draft.noteOptions ?? QuickNoteOptions(); options.syntax = .markdown; draft.noteOptions = options
                    editor.select(NSRange(location: caret, length: 0)); store.scheduleSave()
                    draft.status = "已插入 \(payloads.count) 张图片"; draft.error = nil
                    previous = Applied(selection: request.selection, source: request.source, content: content, caret: caret)
                } catch {
                    if !Task.isCancelled, self.isCurrent(request, store: store, draft: draft) { draft.error = error.localizedDescription }
                    previous = nil
                }
            }
        }
        draft.noteTask = task
    }
    private func isCurrent(_ request: Request, store: AppStore, draft: ToolDraft) -> Bool {
        draft.documentID == request.document && store.editorRestoreGeneration == request.generation && draft.editorRevision == request.revision && draft.noteOperationID == token
    }
    private func finish() {
        requests.removeAll(); task = nil
        if let owner, owner.noteOperationID == token { owner.noteTask = nil; owner.noteOperationID = nil; owner.busy = false }
        token = nil; owner = nil
    }
    func cancel() { task?.cancel(); finish() }
}
