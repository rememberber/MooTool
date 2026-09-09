import SwiftUI
import AppKit
import UniformTypeIdentifiers
import MooToolNextCore

let nativeDefaults = UserDefaults(suiteName: Product.bundleID + (ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_TEST_DATA"] == nil ? "" : ".acceptance"))!

enum AppResources {
    static var bundle: Bundle {
        if let url = Bundle.main.url(forResource: "MooToolNextNative_MooToolNextApp", withExtension: "bundle"), let bundle = Bundle(url: url) { return bundle }
        return Bundle.module
    }
}

struct EditorPersistence {
    let identity: String
    let state: EditorViewState
    let onChange: (EditorViewState) -> Void
}

@MainActor final class NativeEditorBridge {
    weak var view: NSTextView?
    var identity: String?
    var selection: NSRange { view?.selectedRange() ?? NSRange(location: 0, length: 0) }
    @discardableResult func replace(_ value: String, expected: String, action: String) -> Bool {
        guard let view, view.string == expected, view.isEditable else { return false }
        let selected = view.selectedRange(), origin = view.enclosingScrollView?.contentView.bounds.origin
        view.insertText(value, replacementRange: NSRange(location: 0, length: (expected as NSString).length))
        view.undoManager?.setActionName(action)
        let location = min(selected.location, (value as NSString).length)
        view.setSelectedRange(NSRange(location: location, length: min(selected.length, (value as NSString).length - location)))
        if let scroll = view.enclosingScrollView, let origin { scroll.contentView.scroll(to: origin); scroll.reflectScrolledClipView(scroll.contentView) }
        return true
    }
    func select(_ range: NSRange) {
        guard let view, range.location >= 0, range.length >= 0, NSMaxRange(range) <= (view.string as NSString).length else { return }
        view.setSelectedRange(range); view.scrollRangeToVisible(range)
    }
    func highlight(_ matches: [JSONMatch]) {
        guard let view, let manager = view.layoutManager else { return }
        let full = NSRange(location: 0, length: (view.string as NSString).length)
        manager.removeTemporaryAttribute(.backgroundColor, forCharacterRange: full)
        for match in matches where match.start >= 0 && match.end <= full.length && match.end >= match.start {
            manager.addTemporaryAttribute(.backgroundColor, value: NSColor.systemYellow.withAlphaComponent(0.24), forCharacterRange: match.range)
        }
    }
}

struct CodeEditor: NSViewRepresentable {
    @Binding var text: String
    var editable = true
    var syntax = false
    var persistence: EditorPersistence?
    var bridge: NativeEditorBridge?
    var softWrap: Bool?
    var fontName: String?
    var pointSize: Double?
    var lineHeightMultiple: Double?
    var language: NoteSyntax?
    var imageTransfer: (([NoteImageSource], NSRange) -> Void)?
    @Environment(\.isEnabled) private var isEnabled
    @AppStorage("editorSize", store: nativeDefaults) private var fontSize = 13.0
    @AppStorage("wrapLines", store: nativeDefaults) private var wrapLines = true
    func makeCoordinator() -> Coordinator { Coordinator(self) }
    func makeNSView(context: Context) -> NSScrollView {
        let scroll = NoteTextView.scrollableTextView()
        let view = scroll.documentView as! NSTextView
        view.delegate = context.coordinator; view.isRichText = false; view.allowsUndo = true
        view.isAutomaticQuoteSubstitutionEnabled = false; view.isAutomaticDashSubstitutionEnabled = false
        view.isAutomaticTextReplacementEnabled = false; view.isAutomaticSpellingCorrectionEnabled = false
        view.isContinuousSpellCheckingEnabled = false; view.usesFindBar = true
        view.drawsBackground = true; view.backgroundColor = .textBackgroundColor
        view.textContainerInset = NSSize(width: 14, height: 14)
        view.autoresizingMask = [.width]; view.isVerticallyResizable = true
        view.minSize = .zero; view.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        scroll.hasVerticalScroller = true; scroll.borderType = .noBorder
        context.coordinator.observe(scroll)
        return scroll
    }
    func updateNSView(_ scroll: NSScrollView, context: Context) {
        let coordinator = context.coordinator
        let view = scroll.documentView as! NSTextView
        view.identifier = persistence.map { NSUserInterfaceItemIdentifier($0.identity) }
        let identityChanged = coordinator.identity != persistence?.identity
        if identityChanged { coordinator.record(view) }
        coordinator.parent = self
        coordinator.updating = true
        defer { coordinator.updating = false }
        view.isEditable = editable && isEnabled; view.isSelectable = true
        bridge?.view = view; bridge?.identity = persistence?.identity
        if let note = view as? NoteTextView {
            note.imageTransfer = imageTransfer
            if imageTransfer != nil { note.registerForDraggedTypes([.fileURL, .png, .tiff]) }
        }
        let size = pointSize ?? fontSize
        view.font = fontName.map { NativeNoteStyle.font($0, size: size) } ?? .monospacedSystemFont(ofSize: size, weight: .regular)
        let paragraph = NSMutableParagraphStyle()
        if let lineHeightMultiple { paragraph.minimumLineHeight = size * lineHeightMultiple; paragraph.maximumLineHeight = size * lineHeightMultiple }
        view.defaultParagraphStyle = paragraph
        view.typingAttributes = [.font: view.font!, .foregroundColor: NSColor.textColor, .paragraphStyle: paragraph]
        let wraps = softWrap ?? wrapLines
        view.isHorizontallyResizable = !wraps; scroll.hasHorizontalScroller = !wraps
        view.textContainer?.widthTracksTextView = wraps
        view.textContainer?.containerSize = NSSize(width: wraps ? max(1, scroll.contentSize.width - 28) : CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        if view.string != text {
            if bridge != nil { view.undoManager?.removeAllActions() }
            let selection = view.selectedRanges
            view.string = text
            if let selected = selection.first?.rangeValue, NSMaxRange(selected) <= (text as NSString).length { view.selectedRanges = selection }
        }
        if identityChanged || !coordinator.initialized {
            coordinator.initialized = true; coordinator.identity = persistence?.identity
            view.undoManager?.removeAllActions()
            if let persistence { coordinator.restore(persistence, in: scroll) }
        }
        coordinator.highlight(view)
        if let storage = view.textStorage { storage.addAttribute(.paragraphStyle, value: paragraph, range: NSRange(location: 0, length: storage.length)) }
    }
    static func dismantleNSView(_ scroll: NSScrollView, coordinator: Coordinator) {
        if let view = scroll.documentView as? NSTextView { coordinator.record(view) }
        coordinator.stopObserving()
    }
    final class Coordinator: NSObject, NSTextViewDelegate {
        var parent: CodeEditor; var updating = false
        var identity: String?
        var initialized = false
        private var observer: NSObjectProtocol?
        private var lastReported: EditorViewState?
        private var generation = 0
        init(_ parent: CodeEditor) { self.parent = parent }
        func observe(_ scroll: NSScrollView) {
            scroll.contentView.postsBoundsChangedNotifications = true
            observer = NotificationCenter.default.addObserver(forName: NSView.boundsDidChangeNotification, object: scroll.contentView, queue: .main) { [weak self, weak scroll] _ in
                guard let view = scroll?.documentView as? NSTextView else { return }; self?.record(view)
            }
        }
        func stopObserving() { if let observer { NotificationCenter.default.removeObserver(observer) }; observer = nil; generation += 1 }
        func restore(_ context: EditorPersistence, in scroll: NSScrollView) {
            generation += 1; let current = generation; lastReported = context.state
            DispatchQueue.main.async { [weak self, weak scroll] in
                guard let self, self.generation == current, self.identity == context.identity, let scroll,
                      let view = scroll.documentView as? NSTextView else { return }
                self.updating = true
                let state = context.state.clamped(toUTF16Length: (view.string as NSString).length)
                if let container = view.textContainer { view.layoutManager?.ensureLayout(for: container) }
                view.setSelectedRange(NSRange(location: state.location, length: state.length))
                var bounds = scroll.contentView.bounds; bounds.origin = NSPoint(x: state.scrollX, y: state.scrollY)
                scroll.contentView.scroll(to: scroll.contentView.constrainBoundsRect(bounds).origin)
                scroll.reflectScrolledClipView(scroll.contentView)
                self.updating = false
                self.record(view)
            }
        }
        func record(_ view: NSTextView) {
            guard !updating, initialized, let context = parent.persistence else { return }
            let selection = view.selectedRange(), origin = view.enclosingScrollView?.contentView.bounds.origin ?? .zero
            let value = EditorViewState(location: selection.location, length: selection.length, scrollX: origin.x, scrollY: origin.y).clamped(toUTF16Length: (view.string as NSString).length)
            guard value != lastReported else { return }; lastReported = value
            DispatchQueue.main.async { context.onChange(value) }
        }
        func textViewDidChangeSelection(_ notification: Notification) { if let view = notification.object as? NSTextView { record(view) } }
        func textDidChange(_ notification: Notification) {
            guard !updating, let view = notification.object as? NSTextView else { return }
            parent.text = view.string; highlight(view); record(view)
        }
        func highlight(_ view: NSTextView) {
            guard let storage = view.textStorage else { return }
            let range = NSRange(location: 0, length: storage.length)
            storage.beginEditing(); defer { storage.endEditing() }
            storage.addAttribute(.foregroundColor, value: NSColor.textColor, range: range)
            if let language = parent.language {
                guard storage.length < 150_000 else { return }
                for (pattern, color) in NativeNoteStyle.patterns(language) {
                    guard let expression = try? NSRegularExpression(pattern: pattern) else { continue }
                    for match in expression.matches(in: view.string, range: range) { storage.addAttribute(.foregroundColor, value: color, range: match.range) }
                }
                return
            }
            guard parent.syntax, storage.length < 150_000 else { return }
            for (pattern, color) in [(#"\"(?:[^\"\\]|\\.)*\""#, NSColor.systemGreen), (#"\b(?:true|false|null|-?\d+(?:\.\d+)?)\b"#, NSColor.systemOrange), (#"\"(?:[^\"\\]|\\.)*\"(?=\s*:)"#, NSColor.systemBlue)] {
                guard let expression = try? NSRegularExpression(pattern: pattern) else { continue }
                for match in expression.matches(in: view.string, range: range) { storage.addAttribute(.foregroundColor, value: color, range: match.range) }
            }
        }
    }
}

struct EditorPane: View {
    let title: String
    @Binding var text: String
    var editable = true
    var syntax = false
    var persistence: EditorPersistence?
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Text(title).font(.system(size: 12, weight: .medium)).lineLimit(1).layoutPriority(1); Spacer(minLength: 0)
                Text("\(text.count) 字符").font(.system(size: 10, design: .monospaced)).foregroundStyle(.tertiary).lineLimit(1)
                if editable {
                    Button { if let value = NSPasteboard.general.string(forType: .string) { text = value } } label: { Image(systemName: "doc.on.clipboard") }.help("粘贴")
                    Button { FilePanels.readText { text = $0 } } label: { Image(systemName: "folder") }.help("打开文本文件")
                }
                Button { FilePanels.copy(text) } label: { Image(systemName: "doc.on.doc") }.help("复制")
                Button { FilePanels.saveText(text) } label: { Image(systemName: "square.and.arrow.up") }.help("导出文本")
            }.buttonStyle(.borderless).padding(.horizontal, 14).frame(height: 37).background(.quaternary.opacity(0.25))
            Divider()
            CodeEditor(text: $text, editable: editable, syntax: syntax, persistence: persistence)
        }.frame(minWidth: 180, minHeight: 100)
            .background(Color(nsColor: .textBackgroundColor))
            .clipShape(RoundedRectangle(cornerRadius: 9))
            .overlay(RoundedRectangle(cornerRadius: 9).strokeBorder(.quaternary))
    }
}
struct ToolPage<Controls: View, Content: View>: View {
    let tool: Tool
    @Bindable var draft: ToolDraft
    var showsHeading = true
    @ViewBuilder var controls: () -> Controls
    @ViewBuilder var content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if showsHeading { HStack {
                VStack(alignment: .leading, spacing: 5) {
                    Text(tool.title).font(.system(size: 23, weight: .semibold))
                    Text(tool.subtitle).font(.system(size: 12)).foregroundStyle(.secondary)
                }
                Spacer()
                if draft.busy { ProgressView().controlSize(.small) }
            }.padding(.bottom, 3) }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) { controls() }
                ScrollView(.horizontal) { HStack(spacing: 8) { controls() } }.scrollIndicators(.hidden)
            }.controlSize(.regular).disabled(draft.busy)
            content().frame(maxWidth: .infinity, maxHeight: .infinity)
            HStack {
                Image(systemName: draft.error == nil ? "checkmark.circle" : "exclamationmark.circle")
                    .foregroundStyle(draft.error == nil ? Color.secondary : .red)
                Text(draft.error ?? (draft.status.isEmpty ? "就绪" : draft.status))
                    .foregroundStyle(draft.error == nil ? Color.secondary : .red).lineLimit(3).textSelection(.enabled)
                Spacer()
            }.font(.system(size: 11)).frame(minHeight: 18)
        }.padding(22).background(Color(nsColor: .windowBackgroundColor))
    }
}
struct PrimaryButton: View {
    let title: String
    var symbol = "play.fill"
    let action: () -> Void
    var body: some View { Button(action: action) { Label(title, systemImage: symbol) }.buttonStyle(.borderedProminent).keyboardShortcut(.return, modifiers: .command) }
}

enum FilePanels {
    static func copy(_ text: String) { NSPasteboard.general.clearContents(); NSPasteboard.general.setString(text, forType: .string) }
    @MainActor static func error(_ error: Error) { let alert = NSAlert(error: error); alert.runModal() }
    @MainActor static func readText(_ completion: @escaping (String) -> Void) {
        open(types: [.text, .json, .data]) { urls in
            do {
                let url = urls[0]
                guard (try url.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0) < 10 * 1024 * 1024 else { throw ToolError("文本文件超过 10 MB。") }
                completion(try String(contentsOf: url, encoding: .utf8))
            } catch { Self.error(error) }
        }
    }
    @MainActor static func open(types: [UTType], multiple: Bool = false, completion: @escaping ([URL]) -> Void) {
        let panel = NSOpenPanel(); panel.allowedContentTypes = types; panel.allowsMultipleSelection = multiple
        panel.canChooseDirectories = false
        panel.begin { response in if response == .OK { completion(panel.urls) } }
    }
    @MainActor static func saveText(_ text: String, name: String = "MooTool.txt") { save(Data(text.utf8), name: name) }
    @MainActor static func save(_ data: Data, name: String) {
        let panel = NSSavePanel(); panel.nameFieldStringValue = name
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            do { try data.write(to: url, options: .atomic) } catch { Self.error(error) }
        }
    }
}
