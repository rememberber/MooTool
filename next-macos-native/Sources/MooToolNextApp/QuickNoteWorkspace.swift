import SwiftUI
import MooToolNextCore

struct QuickNoteWorkspace: View {
    @Bindable var draft: ToolDraft
    let onSave: () -> Void
    let onDelete: () -> Void
    @Environment(AppStore.self) private var store
    @State private var editor = NativeEditorBridge()
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
            HSplitView {
                VStack(spacing: 0) {
                    toolbar(width: geometry.size.width)
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
                if workspace.quickReplaceOpen && geometry.size.width >= 650 { quickPanel.frame(minWidth: 185, idealWidth: 218, maxWidth: 250) }
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
        Picker("笔记视图", selection: viewBinding) {
            ForEach(NoteViewMode.allCases, id: \.self) { mode in
                Image(systemName: mode == .editor ? "square.and.pencil" : mode == .split ? "rectangle.split.2x1" : "eye").help(mode.title).tag(mode)
            }
        }.pickerStyle(.segmented).labelsHidden().frame(width: 91)
    }
    private var colors: some View {
        Menu {
            Picker("笔记颜色", selection: option(\.color)) { ForEach(NoteColor.allCases, id: \.self) { color in Label(color.title, systemImage: options.color == color ? "checkmark.circle.fill" : "circle.fill").foregroundStyle(NativeNoteStyle.color(color)).tag(color) } }
        } label: { Image(systemName: "circle.fill").foregroundStyle(NativeNoteStyle.color(options.color)).frame(width: 20) }
        .menuStyle(.borderlessButton).menuIndicator(.hidden).fixedSize().help("笔记颜色")
    }
    private var syntaxPicker: some View { Picker("语法类型", selection: option(\.syntax)) { ForEach(NoteSyntax.allCases, id: \.self) { Text($0.title).tag($0) } }.labelsHidden().frame(width: 96).help("语法类型") }
    private var fontPicker: some View {
        Picker("字体", selection: option(\.fontName)) {
            if !Self.fonts.contains(options.fontName) { Text(options.fontName).tag(options.fontName) }
            ForEach(Self.fonts, id: \.self) { Text($0.isEmpty ? "系统字体" : $0 == "ui-monospace" ? "等宽字体" : $0).tag($0) }
        }.labelsHidden().frame(width: 115).help("字体")
    }
    private var sizeField: some View { TextField("字号", value: option(\.fontSize), format: .number.precision(.fractionLength(0))).frame(width: 37).textFieldStyle(.roundedBorder).help("字号：8–48") }
    private var spacingPicker: some View { Picker("行距", selection: option(\.lineSpacing)) { ForEach(QuickNoteOptions.lineSpacings, id: \.self) { Text(String(format: "%.1f×", $0)).tag($0) } }.labelsHidden().frame(width: 64).help("行间距") }
    private var wrapButton: some View { icon("自动换行", "arrow.turn.down.left", active: options.lineWrap) { option(\.lineWrap).wrappedValue.toggle() } }
    private var formatButton: some View {
        icon("格式化", "wand.and.stars") { format() }.disabled(draft.busy || draft.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || ![NoteSyntax.json, .xml].contains(options.syntax))
            .keyboardShortcut(.return, modifiers: .command).help([NoteSyntax.json, .xml].contains(options.syntax) ? "格式化" : "当前语法暂不支持格式化")
    }
    @ViewBuilder private var listButtons: some View {
        icon("无序列表", "list.bullet") { perform(request("noteBullet"), title: "无序列表") }.disabled(draft.busy).jsonAcceptanceControl("note.bullet")
        icon("有序列表", "list.number") { perform(request("noteNumbered"), title: "有序列表") }.disabled(draft.busy)
    }
    private var findButton: some View { icon("查找替换", "magnifyingglass", active: workspace.findOpen, action: openFind).keyboardShortcut("f", modifiers: .command).jsonAcceptanceControl("note.find") }
    private var saveButton: some View { icon("保存", "square.and.arrow.down", action: onSave).keyboardShortcut("s", modifiers: .command) }
    private var attachmentButton: some View { icon("插入图片附件（也可从剪贴板粘贴）", "photo.badge.plus", action: importAttachment).disabled(draft.documentID == nil || draft.busy).jsonAcceptanceControl("note.attachment") }
    private var deleteButton: some View { icon("删除", "trash", action: onDelete).disabled(draft.documentID == nil) }
    private func quickButton(_ width: CGFloat) -> some View { icon("快速替换", "arrow.left.arrow.right", active: workspace.quickReplaceOpen) {
        if width < 650 { quickPopover.toggle(); setting(\.quickReplaceOpen).wrappedValue = quickPopover }
        else { setting(\.quickReplaceOpen).wrappedValue.toggle() }
    }.jsonAcceptanceControl("note.quickPanel") }
    private func icon(_ title: String, _ image: String, active: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) { Image(systemName: image).frame(width: 18, height: 20).foregroundStyle(active ? Color.accentColor : .secondary) }.buttonStyle(.borderless).help(title).accessibilityLabel(title)
    }
    private var findBar: some View {
        VStack(spacing: 7) {
            HStack(spacing: 6) {
                TextField("查找", text: setting(\.findQuery)).focused($findFocused).onSubmit { find(true) }
                Toggle("Aa", isOn: setting(\.matchCase)).help("区分大小写"); Toggle("词", isOn: setting(\.wholeWord)).help("全词匹配"); Toggle(".*", isOn: setting(\.regex)).help("正则表达式")
                icon("上一个", "chevron.up") { find(false) }; icon("下一个", "chevron.down") { find(true) }
                icon("关闭查找", "xmark") { setting(\.findOpen).wrappedValue = false }
            }
            HStack(spacing: 6) {
                TextField("替换为", text: setting(\.replacement)).onSubmit { perform(request("replace"), title: "替换") }
                Text("\(matchCount) 项").font(.caption).foregroundStyle(.secondary).fixedSize().jsonAcceptanceControl("note.find.count.\(matchCount)")
                Button("替换") { perform(request("replace"), title: "替换") }.disabled(draft.busy)
                Button("全部替换") { perform(request("replaceAll"), title: "全部替换") }.disabled(draft.busy).jsonAcceptanceControl("note.replaceAll")
            }
            if let findError { Text(findError).font(.caption).foregroundStyle(.red).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading) }
        }.textFieldStyle(.roundedBorder).toggleStyle(.button).controlSize(.small).padding(10).background(.quaternary.opacity(0.15))
    }
    private var quickPanel: some View {
        VStack(spacing: 0) {
            HStack { Text("快速替换").font(.headline); Spacer(); icon("关闭快速替换", "xmark") { setting(\.quickReplaceOpen).wrappedValue = false; quickPopover = false } }.padding(13)
            Divider()
            ScrollView {
                VStack(spacing: 6) {
                    ForEach(QuickNoteAction.allCases) { action in
                        Button { var r = request("quickReplace"); r.path = action.rawValue; perform(r, title: action.title) } label: { Text(action.title).frame(maxWidth: .infinity, alignment: .leading) }
                            .buttonStyle(.bordered).controlSize(.small).disabled(draft.busy).jsonAcceptanceControl("note." + action.rawValue)
                    }
                }.padding(12)
            }
            Text("有选区时处理选区，否则处理全文").font(.system(size: 10)).foregroundStyle(.secondary).padding(10)
        }.background(Color(nsColor: .controlBackgroundColor))
    }
    private var statusBar: some View {
        HStack(spacing: 8) {
            Text("\(draft.input.count) 字符 · \(draft.input.components(separatedBy: "\n").count) 行").fixedSize()
            Spacer(minLength: 0)
            Text(draft.error ?? draft.status).lineLimit(2).foregroundStyle(draft.error == nil ? Color.secondary : .red)
            if draft.busy { ProgressView().controlSize(.mini); Button("取消") { draft.noteTask?.cancel(); attachments.cancel() } }
            Text(options.syntax.title).foregroundStyle(.tertiary).fixedSize()
        }.font(.system(size: 10)).padding(.horizontal, 12).frame(minHeight: 31)
    }
    private func request(_ action: String) -> JSONEngineRequest {
        var r = JSONEngineRequest(action, input: draft.input)
        r.query = workspace.findQuery; r.replacement = workspace.replacement; r.matchCase = workspace.matchCase; r.wholeWord = workspace.wholeWord; r.regex = workspace.regex
        if viewMode != .preview { r.selectionStart = editor.selection.location; r.selectionEnd = NSMaxRange(editor.selection) }
        return r
    }
    private func importAttachment() {
        let document = draft.documentID, source = draft.input, generation = store.editorRestoreGeneration
        let selection = viewMode == .preview ? NSRange(location: (source as NSString).length, length: 0) : editor.selection
        let panel = NSOpenPanel(); panel.allowedContentTypes = [.png, .jpeg, .gif, .bmp, .webP]
        panel.canChooseDirectories = false; panel.allowsMultipleSelection = false; panel.prompt = "插入图片"
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            guard draft.documentID == document, store.editorRestoreGeneration == generation, draft.input == source else { draft.error = "正文或文档已变化，请重新选择图片。"; return }
            attachments.enqueue([.file(url)], selection: selection, store: store, draft: draft, editor: editor)
        }
    }
    private func perform(_ request: JSONEngineRequest, title: String) {
        guard !draft.busy else { return }
        let id = draft.documentID, generation = store.editorRestoreGeneration, revision = draft.editorRevision, token = UUID()
        operationID = token; draft.noteOperationID = token; draft.busy = true; draft.error = nil
        operation = Task {
            defer { if draft.noteOperationID == token { draft.busy = false; draft.noteTask = nil; draft.noteOperationID = nil } }
            do {
                let reply = try await JSONEngine.execute(request); try Task.checkCancellation()
                guard draft.documentID == id, store.editorRestoreGeneration == generation, draft.editorRevision == revision else { return }
                guard draft.input == request.input, let value = reply.value, editor.replace(value, expected: request.input, action: title) else { throw ToolError("正文已变化，请重新执行操作。") }
                if let match = reply.match { editor.select(match.range) }
                if request.action == "replace", (reply.count ?? 0) > 0 { find(true) }
                draft.status = title + "已完成" + (reply.count.map { " · \($0) 项" } ?? "")
            } catch { if !Task.isCancelled, draft.documentID == id, store.editorRestoreGeneration == generation { draft.error = error.localizedDescription } }
        }
        draft.noteTask = operation
    }
    private func format() {
        if options.syntax == .json { var r = request("advanced"); r.indent = 4; r.checkDuplicateKeys = false; perform(r, title: "格式化") }
        else if options.syntax == .xml { perform(request("formatXML"), title: "格式化") }
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
