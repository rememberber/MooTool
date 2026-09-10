import SwiftUI
import ImageIO
import MooToolNextCore

@MainActor enum NativeAttachmentAcceptance {
    static let missingPath = "attachments/" + String(repeating: "f", count: 64) + ".png"
    static func fixture(width: Int = 720, height: Int = 320) throws -> Data {
        let context = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.setFillColor(CGColor(red: 0.08, green: 0.14, blue: 0.22, alpha: 1)); context.fill(CGRect(x: 0, y: 0, width: width, height: height))
        for index in 0..<6 {
            context.setFillColor(CGColor(red: 0.18 + Double(index) * 0.08, green: 0.40 + Double(index) * 0.05, blue: 0.78, alpha: 1))
            context.fill(CGRect(x: 30, y: 30 + index * max(25, (height - 120) / 6), width: max(40, width - 60 - index * min(55, width / 14)), height: max(16, (height - 140) / 8)))
        }
        let previous = NSGraphicsContext.current; NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
        NSAttributedString(string: width > 300 ? "MooTool Native · 图片附件" : "长图预览", attributes: [.font: NSFont.systemFont(ofSize: width > 300 ? 25 : 18, weight: .semibold), .foregroundColor: NSColor.white]).draw(at: NSPoint(x: 30, y: height - 55))
        NSGraphicsContext.current = previous
        let output = NSMutableData(), image = context.makeImage()!, destination = CGImageDestinationCreateWithData(output, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { throw ToolError("无法创建验收图片。") }; return output as Data
    }
    static func seed(_ store: AppStore) throws -> [NoteAttachment] {
        let images = try [NoteImagePayload(data: fixture()), NoteImagePayload(data: fixture(width: 200, height: 1200))]
        for image in images { try store.repository.attachmentRepository.write(image) }
        store.noteAttachments = images.map(\.attachment); store.attachmentGeneration += 1; return images.map(\.attachment)
    }
    static func run(store: AppStore, window: NSWindow) async throws {
        let original = store.snapshot(); defer { store.restore(original) }
        func check(_ condition: @autoclosure () throws -> Bool, _ message: String) throws { if try !condition() { throw ToolError("附件原生验收：" + message) } }
        let source = "beforeselectedafter", first = try fixture(), second = try fixture(width: 200, height: 1200)
        let a = try store.createVaultDocument("quickNote", name: "图片验收.md", parent: nil, content: source)
        store.selected = "quickNote"; store.updateVaultPreference("quickNote") { $0.noteViewMode = .editor }
        store.draft("quickNote").noteWorkspace = QuickNoteWorkspaceOptions()
        var options = QuickNoteOptions(); options.syntax = .plain; store.draft("quickNote").noteOptions = options
        window.setContentSize(NSSize(width: 1440, height: 850)); window.makeKeyAndOrderFront(nil); try await settle(window)
        guard let editor = NativeJSONAcceptance.allViews(window).compactMap({ $0 as? NoteTextView }).first(where: { $0.identifier?.rawValue.contains(a.uuidString) == true }) else { throw ToolError("找不到带图片处理能力的笔记编辑器。") }
        fputs("Attachment acceptance: picker\n", stderr)
        try NativeJSONAcceptance.press("note.attachment", in: window); try await settle(window)
        guard let panel = NSApp.windows.compactMap({ $0 as? NSOpenPanel }).first(where: \.isVisible) else { throw ToolError("插入图片按钮未打开选图面板。") }
        try check(panel.allowedContentTypes.contains(.png) && !panel.canChooseDirectories, "图片面板配置错误")
        panel.cancel(nil); try await settle(window)
        fputs("Attachment acceptance: paste\n", stderr)
        let pasteboard = NSPasteboard(name: .init("native-attachment-acceptance-" + UUID().uuidString)); defer { pasteboard.releaseGlobally() }
        pasteboard.setData(first, forType: .png)
        editor.setSelectedRange(NSRange(location: 6, length: 8)); try check(editor.pasteImages(from: pasteboard), "图片粘贴没有被接收")
        try await finish(store, window)
        let attachment = try NoteImagePayload(data: first).attachment
        let expected = NoteImageInsertion(content: source, selection: NSRange(location: 6, length: 8), markdown: attachment.markdown).applying(to: source)
        try check(store.draft("quickNote").input == expected && store.draft("quickNote").noteOptions?.syntax == .markdown, "粘贴选区、换行或语法切换错误")
        try check(try store.repository.attachmentRepository.read(attachment) == first, "图片未保存到原生目录")
        fputs("Attachment acceptance: undo\n", stderr)
        editor.undoManager?.undo(); try await settle(window); try check(store.draft("quickNote").input == source, "图片插入不能撤销")
        editor.undoManager?.redo(); try await settle(window); try check(store.draft("quickNote").input == expected, "图片插入不能重做")
        fputs("Attachment acceptance: rapid paste\n", stderr)
        editor.setSelectedRange(NSRange(location: 0, length: 0)); _ = editor.pasteImages(from: pasteboard); _ = editor.pasteImages(from: pasteboard)
        try await finish(store, window)
        try check(MarkdownImageReference.parse(store.draft("quickNote").input).count == 3, "连续粘贴丢失图片或覆盖前一次结果")
        try check(store.noteAttachments.count == 1, "重复图片没有复用内容")
        fputs("Attachment acceptance: multi-file\n", stderr)
        let fileA = store.repository.directory.appendingPathComponent("fixture-a.png"), fileB = store.repository.directory.appendingPathComponent("fixture-b.png")
        try first.write(to: fileA); try second.write(to: fileB)
        let bridge = NativeEditorBridge(); bridge.view = editor
        _ = bridge.replace("", expected: editor.string, action: "准备多图验收")
        editor.imageTransfer?([.file(fileA), .file(fileB)], NSRange(location: 0, length: 0)); try await finish(store, window)
        let refs = MarkdownImageReference.parse(store.draft("quickNote").input)
        try check(refs.map(\.path) == [attachment.path, try NoteImagePayload(data: second).attachment.path], "多图文件插入顺序错误")
        let content = store.draft("quickNote").input
        fputs("Attachment acceptance: stale protection\n", stderr)
        editor.imageTransfer?([.file(fileA)], editor.selectedRange())
        let b = try store.createVaultDocument("quickNote", name: "切换保护.md", parent: nil, content: "unchanged"); try await finish(store, window)
        try check(store.draft("quickNote").documentID == b && store.draft("quickNote").input == "unchanged", "过期附件写入了其他文档")
        fputs("Attachment acceptance: duplicate\n", stderr)
        try store.duplicateVaultDocument(a); let copied = store.draft("quickNote").documentID!
        try store.deleteVaultEntry(a); try FileManager.default.removeItem(at: fileA); try FileManager.default.removeItem(at: fileB)
        try await settle(window); try check(store.draft("quickNote").input == content, "删除原文档影响了副本")
        fputs("Attachment acceptance: image preview\n", stderr)
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .split }; try await waitForImage(window, path: attachment.path)
        try verifySplitLayout(window)
        try check(store.draft("quickNote").documentID == copied, "预览切换改变文档")
        fputs("Attachment acceptance: split divider\n", stderr)
        let beforeDrag = editor.enclosingScrollView!.bounds.width
        try dragDivider(window, offset: -70); try await settle(window)
        let draggedWidth = editor.enclosingScrollView!.bounds.width
        try check(draggedWidth < beforeDrag - 30, "分隔条拖动没有改变编辑区宽度")
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .preview }; try await settle(window)
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .split }; try await waitForImage(window, path: attachment.path)
        try check(abs(editor.enclosingScrollView!.bounds.width - draggedWidth) < 2, "切换预览丢失分栏比例")
        fputs("Attachment acceptance: image zoom\n", stderr)
        try NativeJSONAcceptance.press("note.image.ready." + attachment.path, in: window); try await settle(window)
        try check(window.attachedSheet != nil, "图片缩放面板未打开")
        try NativeJSONAcceptance.press("note.image.done", in: window); try await settle(window)
        fputs("Attachment acceptance: backup\n", stderr)
        let backup = try WorkspaceRepository.decode(store.repository.backup(store.snapshot()))
        let restoredDirectory = store.repository.directory.appendingPathComponent("restored-copy")
        let repository = WorkspaceRepository(directory: restoredDirectory); _ = try repository.installBackup(backup)
        let restored = AppStore(directory: restoredDirectory)
        try check(restored.noteAttachments == store.noteAttachments && restored.documents == store.documents, "附件备份没有恢复到独立目录")
        try check(try repository.attachmentRepository.read(attachment) == first, "恢复后图片内容不一致")
        fputs("Attachment acceptance: missing image\n", stderr)
        store.draft("quickNote").input = "![丢失图片](\(missingPath))"
        store.updateVaultPreference("quickNote") { $0.noteViewMode = .preview }; try await waitForImage(window, path: missingPath, missing: true)
        store.saveNow()
        print("PASS: native attachment picker, image paste selection, undo/redo, rapid paste queue, multi-file insertion, stale-document protection, duplicate retention, image preview/zoom, divider dragging and mode restoration, missing placeholder and portable backup restore")
    }
    static func waitForImage(_ window: NSWindow, path: String, missing: Bool = false) async throws {
        let expected = "json.acceptance.note.image." + (missing ? "missing." : "ready.") + path
        var consecutive = 0
        for _ in 0..<80 {
            window.contentView?.layoutSubtreeIfNeeded()
            if NativeJSONAcceptance.allViews(window).contains(where: { $0.identifier?.rawValue == expected && !$0.isHiddenOrHasHiddenAncestor && $0.bounds.width > 0 && $0.bounds.height > 0 }) { consecutive += 1 } else { consecutive = 0 }
            if consecutive >= 4 {
                if let image = NativeJSONAcceptance.allViews(window).first(where: { $0.identifier?.rawValue == expected }),
                   let preview = NativeJSONAcceptance.allViews(window).first(where: { $0.identifier?.rawValue == "json.acceptance.note.preview.viewport" }) {
                    let bounds = image.convert(image.bounds, to: nil), viewport = preview.convert(preview.bounds, to: nil)
                    guard bounds.minX >= viewport.minX - 1, bounds.maxX <= viewport.maxX + 1 else { throw ToolError("图片超出预览区宽度。") }
                }
                window.displayIfNeeded(); return
            }
            try await Task.sleep(for: .milliseconds(100))
        }
        throw ToolError("图片预览没有展示预期内容：\(path)")
    }
    static func verifySplitLayout(_ window: NSWindow) throws {
        guard let editor = NativeJSONAcceptance.allViews(window).compactMap({ $0 as? NoteTextView }).first(where: { $0.imageTransfer != nil }),
              let scroll = editor.enclosingScrollView else { throw ToolError("图片分栏验收找不到编辑器。") }
        guard let preview = NativeJSONAcceptance.allViews(window).first(where: { $0.identifier?.rawValue == "json.acceptance.note.preview.viewport" }) else { throw ToolError("图片分栏验收找不到预览容器。") }
        let ratio = scroll.bounds.width / max(1, scroll.bounds.width + preview.bounds.width)
        guard (0.35...0.65).contains(ratio) else { throw ToolError("图片分栏宽度失衡：编辑器占 \(Int(ratio * 100))%。") }
    }
    private static func dragDivider(_ window: NSWindow, offset: CGFloat) throws {
        guard let anchor = NativeJSONAcceptance.allViews(window).first(where: { $0.identifier?.rawValue == "json.acceptance.note.split.divider" }) else { throw ToolError("找不到笔记分隔条。") }
        let start = anchor.convert(NSPoint(x: anchor.bounds.midX, y: anchor.bounds.midY), to: nil)
        let end = NSPoint(x: start.x + offset, y: start.y), now = ProcessInfo.processInfo.systemUptime
        guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: start, modifierFlags: [], timestamp: now, windowNumber: window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
              let drag = NSEvent.mouseEvent(with: .leftMouseDragged, location: end, modifierFlags: [], timestamp: now + 0.1, windowNumber: window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 1),
              let up = NSEvent.mouseEvent(with: .leftMouseUp, location: end, modifierFlags: [], timestamp: now + 0.2, windowNumber: window.windowNumber, context: nil, eventNumber: 2, clickCount: 1, pressure: 0) else { throw ToolError("无法创建分隔条拖动事件。") }
        window.makeKeyAndOrderFront(nil); NSApp.postEvent(up, atStart: true); NSApp.postEvent(drag, atStart: true); window.sendEvent(down)
    }
    private static func settle(_ window: NSWindow) async throws { window.contentView?.layoutSubtreeIfNeeded(); try await Task.sleep(for: .milliseconds(650)); window.contentView?.layoutSubtreeIfNeeded() }
    private static func finish(_ store: AppStore, _ window: NSWindow) async throws {
        try await settle(window)
        for _ in 0..<80 where store.draft("quickNote").busy { try await Task.sleep(for: .milliseconds(100)) }
        try await settle(window)
        if store.draft("quickNote").busy { throw ToolError("附件导入没有结束。") }
    }
}
