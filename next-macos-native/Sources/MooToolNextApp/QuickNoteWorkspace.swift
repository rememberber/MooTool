import SwiftUI
import MooToolNextCore

struct QuickNoteWorkspace: View {
    @Bindable var draft: ToolDraft
    let onSave: () -> Void
    let onDelete: () -> Void
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var editor = NativeEditorBridge()
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    private func viewModeHelp(_ mode: NoteViewMode) -> String {
        switch mode {
        case .editor: return loc("quickNote.view.editor")
        case .split: return loc("quickNote.view.split")
        case .preview: return loc("quickNote.view.preview")
        }
    }
    private func noteColorTitle(_ color: NoteColor) -> String {
        loc("quickNote.color.\(color.rawValue == "default" ? "default" : color.rawValue)")
    }
    private func quickActionTitle(_ action: QuickNoteAction) -> String { loc("quickNote.quick.\(action.rawValue)") }
    @State private var attachments = NoteAttachmentInsertionQueue()
    @State private var operation: Task<Void, Never>?
    @State private var operationID: UUID?
    @State private var quickPopover = false
    @State private var matchCount = 0
    @State private var findError: String?
    @FocusState private var findFocused: Bool
    private static let fonts = ["", "ui-monospace"] + NSFontManager.shared.availableFontFamilies.sorted()
    private var options: QuickNoteOptions { draft.noteOptions ?? QuickNoteOptions() }
    private var workspace: QuickNoteWorkspaceOptions { draft.noteWorkspace ?? QuickNoteWorkspaceOptions() }
    private var viewMode: NoteViewMode { store.vaultPreference("quickNote").noteViewMode }
    private var viewBinding: Binding<NoteViewMode> { Binding(get: { viewMode }, set: { value in store.updateVaultPreference("quickNote") { $0.noteViewMode = value } }) }
    private func option<T>(_ key: WritableKeyPath<QuickNoteOptions, T>) -> Binding<T> {
        Binding(get: { options[keyPath: key] }, set: { value in
            var next = options; next[keyPath: key] = value
            do { try next.validate(); draft.noteOptions = next } catch { draft.error = error.localizedDescription }
        })
    }
    private func setting<T>(_ key: WritableKeyPath<QuickNoteWorkspaceOptions, T>) -> Binding<T> {
        Binding(get: { workspace[keyPath: key] }, set: { value in
            var next = workspace; next[keyPath: key] = value
            do { try next.validate(); draft.noteWorkspace = next } catch { draft.error = error.localizedDescription }
        })
    }
    var body: some View {
        GeometryReader { geometry in
            let replaceOpen = workspace.quickReplaceOpen && geometry.size.width >= 650
            Group {
                if replaceOpen {
                    PersistedHSplit(toolID: "quick-note-no-tree-replace", defaultLeading: min(520, geometry.size.width - 220), minLeading: 360, maxLeading: min(900, geometry.size.width - 200)) {
                        noteColumn(width: geometry.size.width)
                    } trailing: {
                        quickPanel.frame(minWidth: 185, maxWidth: 280)
                    }
                } else {
                    noteColumn(width: geometry.size.width)
                }
            }
            .onChange(of: geometry.size.width) { _, width in if width < 650 && workspace.quickReplaceOpen { quickPopover = true } }
        }
        .background(Color(nsColor: .textBackgroundColor))
        .popover(isPresented: $quickPopover) { quickPanel.frame(width: 240, height: 600) }
        .task(id: findKey) {
            guard workspace.findOpen && !workspace.findQuery.isEmpty else { editor.highlight([]); matchCount = 0; findError = nil; return }
            do {
                try await Task.sleep(for: .milliseconds(180))
                let reply = try await JSONEngine.execute(request("find")); try Task.checkCancellation()
                matchCount = reply.count ?? 0; findError = nil; editor.highlight(reply.matches ?? [])
            } catch { if !Task.isCancelled { findError = error.localizedDescription; matchCount = 0; editor.highlight([]) } }
        }
        .onChange(of: draft.documentID) { _, _ in cancelOwnOperation(); attachments.cancel() }
        .onChange(of: viewMode) { _, mode in
            if mode == .preview, let view = editor.view, view.window?.firstResponder === view { view.window?.makeFirstResponder(nil) }
        }
        .onDisappear { cancelOwnOperation(); attachments.cancel() }
    }
    private var findKey: String { [draft.input, workspace.findQuery, String(workspace.findOpen), String(workspace.matchCase), String(workspace.wholeWord), String(workspace.regex), draft.documentID?.uuidString ?? ""].joined(separator: "\u{0}") }
    private func toolbar(width: CGFloat) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 7) {
                modes; Divider().frame(height: 18); colors; syntaxPicker; fontPicker; sizeField; spacingPicker
                wrapButton; formatButton; listButtons; Spacer(minLength: 12); findButton; saveButton; attachmentButton; quickButton(width); deleteButton
            }.fixedSize(horizontal: true, vertical: false)
            VStack(spacing: 8) {
                HStack(spacing: 7) { modes; colors; syntaxPicker; Spacer(minLength: 0); findButton; saveButton; attachmentButton; quickButton(width); deleteButton }
                HStack(spacing: 7) { fontPicker; sizeField; spacingPicker; wrapButton; formatButton; listButtons; Spacer(minLength: 0) }
            }
        }.controlSize(.small).padding(.horizontal, 10).padding(.vertical, 9).frame(maxWidth: .infinity, alignment: .leading).background(.bar)
    }
    private var modes: some View {
        Picker(loc("quickNote.viewMode"), selection: viewBinding) {
            ForEach(NoteViewMode.allCases, id: \.self) { mode in
                Image(systemName: mode == .editor ? "square.and.pencil" : mode == .split ? "rectangle.split.2x1" : "eye").help(viewModeHelp(mode)).tag(mode)
            }
        }.pickerStyle(.segmented).labelsHidden().frame(width: 91)
    }
    private var colors: some View {
        Menu {
            Picker(loc("quickNote.color"), selection: option(\.color)) {
                ForEach(NoteColor.allCases, id: \.self) { color in
                    Label(noteColorTitle(color), systemImage: options.color == color ? "checkmark.circle.fill" : "circle.fill").foregroundStyle(NativeNoteStyle.color(color)).tag(color)
                }
            }
        } label: { Image(systemName: "circle.fill").foregroundStyle(NativeNoteStyle.color(options.color)).frame(width: 20) }
        .menuStyle(.borderlessButton).menuIndicator(.hidden).fixedSize().help(loc("quickNote.color"))
    }
    private var syntaxPicker: some View { Picker(loc("quickNote.syntax"), selection: option(\.syntax)) { ForEach(NoteSyntax.allCases, id: \.self) { Text($0.title).tag($0) } }.labelsHidden().frame(width: 96).help(loc("quickNote.syntax")) }
    private var fontPicker: some View {
        Picker(loc("quickNote.font"), selection: option(\.fontName)) {
            if !Self.fonts.contains(options.fontName) { Text(options.fontName).tag(options.fontName) }
            ForEach(Self.fonts, id: \.self) { Text($0.isEmpty ? loc("quickNote.font.system") : $0 == "ui-monospace" ? loc("quickNote.font.mono") : $0).tag($0) }
        }.labelsHidden().frame(width: 115).help(loc("quickNote.font"))
    }
    private var sizeField: some View { TextField(loc("quickNote.fontSize"), value: option(\.fontSize), format: .number.precision(.fractionLength(0))).frame(width: 37).textFieldStyle(.roundedBorder).help(loc("quickNote.fontSizeHint")) }
    private var spacingPicker: some View { Picker(loc("quickNote.lineSpacing"), selection: option(\.lineSpacing)) { ForEach(QuickNoteOptions.lineSpacings, id: \.self) { Text(String(format: "%.1f×", $0)).tag($0) } }.labelsHidden().frame(width: 64).help(loc("quickNote.lineSpacing")) }
    private var wrapButton: some View { icon(loc("quickNote.wrap"), "arrow.turn.down.left", active: options.lineWrap) { option(\.lineWrap).wrappedValue.toggle() } }
    private var formatButton: some View {
        let canFormat = [NoteSyntax.json, .xml].contains(options.syntax)
        return icon(loc("tool.format"), "wand.and.stars") { format() }.disabled(draft.busy || draft.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !canFormat)
            .keyboardShortcut(.return, modifiers: .command).help(canFormat ? loc("tool.format") : loc("quickNote.formatUnsupported"))
    }
    @ViewBuilder private var listButtons: some View {
        icon(loc("quickNote.bulletList"), "list.bullet") { perform(request("noteBullet"), title: loc("quickNote.bulletList")) }.disabled(draft.busy).jsonAcceptanceControl("note.bullet")
        icon(loc("quickNote.numberedList"), "list.number") { perform(request("noteNumbered"), title: loc("quickNote.numberedList")) }.disabled(draft.busy)
    }
    private var findButton: some View { icon(loc("quickNote.find"), "magnifyingglass", active: workspace.findOpen, action: openFind).keyboardShortcut("f", modifiers: .command).jsonAcceptanceControl("note.find") }
    private var saveButton: some View { icon(loc("tool.save"), "square.and.arrow.down", action: onSave).keyboardShortcut("s", modifiers: .command) }
    private var attachmentButton: some View { icon(loc("quickNote.attachment"), "photo.badge.plus", action: importAttachment).disabled(draft.documentID == nil || draft.busy).jsonAcceptanceControl("note.attachment") }
    private var deleteButton: some View { icon(loc("tool.delete"), "trash", action: onDelete).disabled(draft.documentID == nil) }
    private func quickButton(_ width: CGFloat) -> some View { icon(loc("quickNote.quickReplace"), "arrow.left.arrow.right", active: workspace.quickReplaceOpen) {
        if width < 650 { quickPopover.toggle(); setting(\.quickReplaceOpen).wrappedValue = quickPopover }
        else { setting(\.quickReplaceOpen).wrappedValue.toggle() }
    }.jsonAcceptanceControl("note.quickPanel") }
    private func icon(_ title: String, _ image: String, active: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) { Image(systemName: image).frame(width: 18, height: 20).foregroundStyle(active ? Color.accentColor : .secondary) }.buttonStyle(.borderless).help(title).accessibilityLabel(title)
    }
    private var findBar: some View {
        VStack(spacing: 7) {
            HStack(spacing: 6) {
                TextField(loc("json.find.query"), text: setting(\.findQuery)).focused($findFocused).onSubmit { find(true) }
                Toggle("Aa", isOn: setting(\.matchCase)).help(loc("json.find.matchCase"))
                Toggle(loc("json.find.wholeWordAbbr"), isOn: setting(\.wholeWord)).help(loc("json.find.wholeWord"))
                Toggle(".*", isOn: setting(\.regex)).help(loc("json.find.regex"))
                icon(loc("json.find.prev"), "chevron.up") { find(false) }
                icon(loc("json.find.next"), "chevron.down") { find(true) }
                icon(loc("json.find.close"), "xmark") { setting(\.findOpen).wrappedValue = false }
            }
            HStack(spacing: 6) {
                TextField(loc("json.find.replaceWith"), text: setting(\.replacement)).onSubmit { perform(request("replace"), title: loc("common.replace")) }
                Text(locf("json.find.matches", matchCount)).font(.caption).foregroundStyle(.secondary).fixedSize().jsonAcceptanceControl("note.find.count.\(matchCount)")
                Button(loc("common.replace")) { perform(request("replace"), title: loc("common.replace")) }.disabled(draft.busy)
                Button(loc("json.find.replaceAll")) { perform(request("replaceAll"), title: loc("json.find.replaceAll")) }.disabled(draft.busy).jsonAcceptanceControl("note.replaceAll")
            }
            if let findError { Text(findError).font(.caption).foregroundStyle(.red).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading) }
        }.textFieldStyle(.roundedBorder).toggleStyle(.button).controlSize(.small).padding(10).background(.quaternary.opacity(0.15))
    }
    private var quickPanel: some View {
        VStack(spacing: 0) {
            HStack {
                Text(loc("quickNote.quickReplace")).font(.headline)
                Spacer()
                icon(loc("quickNote.closeQuickReplace"), "xmark") { setting(\.quickReplaceOpen).wrappedValue = false; quickPopover = false }
            }.padding(13)
            Divider()
            ScrollView {
                VStack(spacing: 6) {
                    ForEach(QuickNoteAction.allCases) { action in
                        let title = quickActionTitle(action)
                        Button { var r = request("quickReplace"); r.path = action.rawValue; perform(r, title: title) } label: { Text(title).frame(maxWidth: .infinity, alignment: .leading) }
                            .buttonStyle(.bordered).controlSize(.small).disabled(draft.busy).jsonAcceptanceControl("note." + action.rawValue)
                    }
                }.padding(12)
            }
            Text(loc("quickNote.quickReplaceHint")).font(.system(size: 10)).foregroundStyle(.secondary).padding(10)
        }.background(Color(nsColor: .controlBackgroundColor))
    }
    private func noteColumn(width: CGFloat) -> some View {
        VStack(spacing: 0) {
            toolbar(width: width)
            if workspace.findOpen { findBar }
            Divider()
            NoteEditorSplit(mode: viewMode) {
                CodeEditor(text: $draft.input, persistence: store.editorPersistence("quickNote"), bridge: editor,
                           softWrap: options.lineWrap, fontName: options.fontName, pointSize: options.fontSize,
                           lineHeightMultiple: 1.65 * options.lineSpacing, language: options.syntax,
                           imageTransfer: { sources, selection in attachments.enqueue(sources, selection: selection, store: store, draft: draft, editor: editor) })
            } preview: {
                Group {
                    if options.syntax == .markdown { MarkdownPreview(text: draft.input, fontName: options.fontName, pointSize: options.fontSize) }
                    else { CodeEditor(text: .constant(draft.input), editable: false, softWrap: options.lineWrap, fontName: options.fontName, pointSize: options.fontSize, lineHeightMultiple: 1.65 * options.lineSpacing) }
                }.background(Color(nsColor: .textBackgroundColor))
            }
            Divider(); statusBar
        }.frame(minWidth: 420)
    }
    private var statusBar: some View {
        HStack(spacing: 8) {
            Text(locf("quickNote.status", draft.input.components(separatedBy: "\n").count, draft.input.count)).fixedSize()
            Spacer(minLength: 0)
            Text(draft.error ?? draft.status).lineLimit(2).foregroundStyle(draft.error == nil ? Color.secondary : .red)
            if draft.busy { ProgressView().controlSize(.mini); Button(loc("common.cancel")) { draft.noteTask?.cancel(); attachments.cancel() } }
            Text(options.syntax.title).foregroundStyle(.tertiary).fixedSize()
        }.font(.system(size: 10)).padding(.horizontal, 12).frame(minHeight: 31)
    }
    private var editorText: String {
        if viewMode != .preview, let live = editor.view?.string { return live }
        return draft.input
    }
    private func effectiveEditorSelection() -> NSRange {
        guard viewMode != .preview else { return NSRange(location: 0, length: 0) }
        let textLen = (editorText as NSString).length
        func clamped(_ range: NSRange) -> NSRange {
            let start = max(0, min(range.location, textLen))
            let end = max(start, min(NSMaxRange(range), textLen))
            return NSRange(location: start, length: end - start)
        }
        let live = editor.view?.selectedRange() ?? editor.selection
        if live.length > 0 { return clamped(live) }
        // Keep selection when a panel button steals focus; honor collapsed caret when the editor is still first responder.
        if let view = editor.view, view.window?.firstResponder as? NSTextView !== view,
           let state = draft.inputEditor {
            let persisted = NSRange(location: state.location, length: state.length)
            if persisted.length > 0 { return clamped(persisted) }
        }
        return NSRange(location: 0, length: 0)
    }
    private func request(_ action: String) -> JSONEngineRequest {
        var r = JSONEngineRequest(action, input: editorText)
        r.query = workspace.findQuery; r.replacement = workspace.replacement; r.matchCase = workspace.matchCase; r.wholeWord = workspace.wholeWord; r.regex = workspace.regex
        if viewMode != .preview {
            let selection = effectiveEditorSelection()
            r.selectionStart = selection.location
            r.selectionEnd = NSMaxRange(selection)
        }
        r.language = language.rawValue
        return r
    }
    private func importAttachment() {
        let document = draft.documentID, source = draft.input, generation = store.editorRestoreGeneration
        let selection = viewMode == .preview ? NSRange(location: (source as NSString).length, length: 0) : editor.selection
        let panel = NSOpenPanel(); panel.allowedContentTypes = [.png, .jpeg, .gif, .bmp, .webP]
        panel.canChooseDirectories = false; panel.allowsMultipleSelection = false; panel.prompt = loc("quickNote.insertImage")
        let staleMessage = loc("quickNote.error.imageStale")
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            guard draft.documentID == document, store.editorRestoreGeneration == generation, draft.input == source else { draft.error = staleMessage; return }
            attachments.enqueue([.file(url)], selection: selection, store: store, draft: draft, editor: editor)
        }
    }
    private func perform(_ request: JSONEngineRequest, title: String) {
        guard !draft.busy else { return }
        let source = request.input
        let id = draft.documentID, generation = store.editorRestoreGeneration, revision = draft.editorRevision, token = UUID()
        operationID = token; draft.noteOperationID = token; draft.busy = true; draft.error = nil
        let lang = language
        operation = Task { @MainActor in
            defer { if draft.noteOperationID == token { draft.busy = false; draft.noteTask = nil; draft.noteOperationID = nil } }
            do {
                let reply = try await JSONEngine.execute(request); try Task.checkCancellation()
                guard draft.documentID == id, store.editorRestoreGeneration == generation, draft.editorRevision == revision else { return }
                let stale = AppLocalization.string("json.error.contentChanged", language: lang)
                guard editorText == source, let value = reply.value, editor.replace(value, expected: source, action: title) else { throw ToolError(stale) }
                if let match = reply.match { editor.select(match.range) }
                if request.action == "replace", (reply.count ?? 0) > 0 { find(true) }
                if let count = reply.count {
                    draft.status = String(format: AppLocalization.string("json.status.doneCount", language: lang), title, count)
                } else {
                    draft.status = String(format: AppLocalization.string("json.status.done", language: lang), title)
                }
            } catch { if !Task.isCancelled, draft.documentID == id, store.editorRestoreGeneration == generation { draft.error = error.localizedDescription } }
        }
        draft.noteTask = operation
    }
    private func format() {
        let title = loc("tool.format")
        if options.syntax == .json { var r = request("advanced"); r.indent = 4; r.checkDuplicateKeys = false; perform(r, title: title) }
        else if options.syntax == .xml { perform(request("formatXML"), title: title) }
    }
    private func find(_ forward: Bool) {
        if viewMode == .preview { viewBinding.wrappedValue = .split }
        var r = request("find"); r.forward = forward
        let id = draft.documentID, generation = store.editorRestoreGeneration
        Task { do {
            let reply = try await JSONEngine.execute(r)
            guard draft.documentID == id, store.editorRestoreGeneration == generation, draft.input == r.input else { return }
            if let match = reply.match { editor.select(match.range) }; matchCount = reply.count ?? 0
        } catch { if draft.documentID == id { findError = error.localizedDescription } } }
    }
    private func openFind() {
        if workspace.findOpen { setting(\.findOpen).wrappedValue = false; return }
        var next = workspace; next.findOpen = true
        let range = editor.selection
        if viewMode != .preview, range.length > 0, NSMaxRange(range) <= (draft.input as NSString).length { next.findQuery = (draft.input as NSString).substring(with: range) }
        do { try next.validate(); draft.noteWorkspace = next; findFocused = true } catch { draft.error = error.localizedDescription }
    }
    private func cancelOwnOperation() {
        operation?.cancel()
        if let operationID, draft.noteOperationID == operationID { draft.noteTask = nil; draft.noteOperationID = nil; draft.busy = false }
        operationID = nil
    }
}
