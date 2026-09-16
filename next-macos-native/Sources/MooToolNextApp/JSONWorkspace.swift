import SwiftUI
import MooToolNextCore

struct JSONWorkspace: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var editor = NativeEditorBridge()
    @State private var operation: Task<Void, Never>?
    @State private var operationID: UUID?
    @State private var validation = ""
    @State private var invalid = false
    @State private var matchCount = 0
    @State private var findError: String?
    @State private var dialog: JSONDialog?
    @State private var conversionInput = ""
    @State private var dialogError: String?
    @State private var pickedPath = "$"
    @State private var pathPreview = ""
    @State private var resultSource = ""
    @State private var resultDocument: UUID?
    @State private var resultGeneration = 0
    @State private var inspectorPopup = false
    @State private var historyOpen = false
    @FocusState private var findFocused: Bool
    private var options: JSONOptions { draft.json ?? JSONOptions() }
    private func option<Value>(_ key: WritableKeyPath<JSONOptions, Value>) -> Binding<Value> {
        Binding(get: { options[keyPath: key] }, set: { value in var next = options; next[keyPath: key] = value; draft.json = next })
    }
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    private func localizedValidation(_ raw: String?) -> String {
        guard let raw, !raw.isEmpty else { return loc("json.valid.idle") }
        for prefix in ["有效 JSON · ", "Valid JSON · ", "有効な JSON · "] {
            if raw.hasPrefix(prefix), raw.count > prefix.count {
                return locf("json.valid.ok", String(raw.dropFirst(prefix.count)))
            }
        }
        return raw
    }
    var body: some View {
        GeometryReader { geometry in
            let expanded = options.inspectorOpen ?? (geometry.size.width >= 720)
            let editorColumn = VStack(spacing: 0) {
                toolbar(compact: expanded && geometry.size.width >= 660, availableWidth: geometry.size.width)
                if options.findOpen { findBar }
                Divider()
                CodeEditor(text: $draft.input, syntax: true, persistence: store.editorPersistence("json"), bridge: editor,
                           softWrap: options.wrapLines, fontName: options.fontName)
                Divider()
                statusBar
            }
            Group {
                if expanded && geometry.size.width >= 660 {
                    PersistedHSplit(toolID: "json", paneIndex: 0, defaultLeading: 520, minLeading: 350, maxLeading: 900) {
                        editorColumn
                    } trailing: {
                        inspector
                    }
                } else {
                    editorColumn.frame(minWidth: 350)
                }
            }
            .onChange(of: expanded) { _, value in if value && geometry.size.width < 660 { inspectorPopup = true } }
        }
        .background(Color(nsColor: .textBackgroundColor))
        .popover(isPresented: $inspectorPopup) { inspector.frame(width: 300, height: 660) }
        .sheet(item: $dialog, onDismiss: { var next = options; next.showsTree = false; draft.json = next }) { sheet($0) }
        .sheet(isPresented: $historyOpen) { JSONHistorySheet(draft: draft, editor: editor).environment(store).environment(\.appLanguage, language) }
        .task(id: draft.input) {
            let source = draft.input
            if source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { validation = ""; invalid = false; return }
            do {
                try await Task.sleep(for: .milliseconds(220))
                let reply = try await JSONEngine.execute(JSONEngineRequest("validate", input: source))
                try Task.checkCancellation(); validation = localizedValidation(reply.value); invalid = false
            } catch { if !Task.isCancelled { validation = error.localizedDescription; invalid = true } }
        }
        .task(id: findKey) {
            if !options.findOpen || options.findQuery.isEmpty { matchCount = 0; findError = nil; editor.highlight([]); return }
            do {
                try await Task.sleep(for: .milliseconds(180))
                let reply = try await JSONEngine.execute(request("find"))
                try Task.checkCancellation(); matchCount = reply.count ?? 0; findError = nil; editor.highlight(reply.matches ?? [])
            } catch { if !Task.isCancelled { findError = error.localizedDescription; matchCount = 0; editor.highlight([]) } }
        }
        .onChange(of: draft.documentID) { _, _ in dialog = nil; cancelOwnOperation() }
        .onAppear { if options.showsTree { pickedPath = "$"; dialog = JSONDialog(kind: .paths, titleKey: "json.pathPicker.title") } }
        .onChange(of: options.showsTree) { _, value in
            if value { pickedPath = "$"; dialog = JSONDialog(kind: .paths, titleKey: "json.pathPicker.title") }
            else if case .paths = dialog?.kind { dialog = nil }
        }
        .onDisappear { cancelOwnOperation() }
    }
    private var findKey: String { [draft.input,options.findQuery,options.replacement,String(options.findOpen),String(options.matchCase),String(options.wholeWord),String(options.regex),draft.documentID?.uuidString ?? ""].joined(separator: "\u{0}") }
    private func toolbar(compact: Bool, availableWidth: CGFloat) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 8) { primaryActions; fontPicker; wrapButton; copyButton; Divider().frame(height: 17); findButton; importButton; exportButton; historyButton; inspectorButton(availableWidth); clearButton }.fixedSize(horizontal: true, vertical: false)
            HStack(spacing: 7) {
                primaryActions; wrapButton; findButton
                Menu {
                    Picker(loc("json.font"), selection: option(\.fontName)) { fontChoices }
                    Button(loc("json.workspace.copyBody"), action: copy)
                    Button(loc("json.workspace.importFile"), action: importFile)
                    Button(loc("json.workspace.exportFile"), action: exportFile)
                    Button(loc("workbench.history")) { historyOpen = true }
                    Button(loc("json.workspace.viewLastResult"), action: showLastResult).disabled(draft.output.isEmpty)
                    Button(loc("json.workspace.clearBody")) { replaceImmediately("", actionKey: "json.workspace.clearBody") }
                } label: { Image(systemName: "ellipsis.circle") }.menuStyle(.borderlessButton).menuIndicator(.hidden).fixedSize().help(loc("json.workspace.moreEdit"))
                Spacer(minLength: 0); inspectorButton(availableWidth)
            }
        }.controlSize(.small).padding(.horizontal, 12).frame(maxWidth: .infinity, alignment: .leading).frame(height: 43).background(.bar).disabled(draft.busy)
    }
    @ViewBuilder private var primaryActions: some View {
        Button { perform("format", titleKey: "json.action.format") } label: { Label(loc("json.action.format"), systemImage: "sparkles") }.buttonStyle(.borderedProminent).keyboardShortcut(.return, modifiers: .command).accessibilityIdentifier("json.format")
        Button(loc("json.action.compress")) { perform("compress", titleKey: "json.action.compress") }.accessibilityIdentifier("json.compress")
    }
    private var fontPicker: some View { Picker(loc("json.font"), selection: option(\.fontName)) { fontChoices }.labelsHidden().frame(width: 95) }
    @ViewBuilder private var fontChoices: some View { ForEach(["Menlo", "Monaco", "SFMono-Regular", "CourierNewPSMT"], id: \.self) { Text($0 == "SFMono-Regular" ? "SF Mono" : $0 == "CourierNewPSMT" ? "Courier New" : $0).tag($0) } }
    private var wrapButton: some View { icon(loc("json.workspace.wrapAuto"), "arrow.turn.down.left") { var next = options; next.wrapLines = !(next.wrapLines ?? nativeDefaults.object(forKey: "wrapLines") as? Bool ?? true); draft.json = next } }
    private var copyButton: some View { icon(loc("json.workspace.copyBody"), "doc.on.doc", action: copy) }
    private var findButton: some View { icon(loc("json.workspace.findReplace"), "magnifyingglass", action: openFind).keyboardShortcut("f", modifiers: .command).accessibilityIdentifier("json.find") }
    private var importButton: some View { icon(loc("json.action.importFile"), "folder", action: importFile) }
    private var exportButton: some View { icon(loc("json.workspace.exportFile"), "square.and.arrow.up", action: exportFile) }
    private var historyButton: some View { icon(loc("workbench.history"), "clock.arrow.circlepath") { historyOpen = true } }
    private var clearButton: some View { icon(loc("json.workspace.clearBody"), "eraser") { replaceImmediately("", actionKey: "json.workspace.clearBody") } }
    private func inspectorButton(_ width: CGFloat) -> some View { icon(loc("json.workspace.showInspector"), "sidebar.right") { if width < 660 { inspectorPopup.toggle() } else { var next = options; next.inspectorOpen = !(next.inspectorOpen ?? (width >= 720)); draft.json = next } }.accessibilityIdentifier("json.inspector") }
    private func icon(_ title: String, _ symbol: String, action: @escaping () -> Void) -> some View { Button(action: action) { Image(systemName: symbol).frame(width: 17, height: 20) }.buttonStyle(.borderless).help(title).accessibilityLabel(title) }
    private var findBar: some View {
        VStack(spacing: 7) {
            HStack(spacing: 6) {
                TextField(loc("json.find.query"), text: option(\.findQuery)).focused($findFocused).onSubmit { find(forward: true) }
                Toggle("Aa", isOn: option(\.matchCase)).help(loc("json.find.matchCase"))
                Toggle(loc("json.find.wholeWordAbbr"), isOn: option(\.wholeWord)).help(loc("json.find.wholeWord"))
                Toggle(".*", isOn: option(\.regex)).help(loc("json.find.regex"))
                icon(loc("json.find.prev"), "chevron.up") { find(forward: false) }
                icon(loc("json.find.next"), "chevron.down") { find(forward: true) }
                icon(loc("json.find.close"), "xmark") { var next = options; next.findOpen = false; draft.json = next }
            }
            HStack(spacing: 6) {
                TextField(loc("json.find.replaceWith"), text: option(\.replacement)).onSubmit { perform("replace", titleKey: "common.replace") }
                Text(locf("json.find.matches", matchCount)).font(.caption).foregroundStyle(.secondary).fixedSize()
                Button(loc("common.replace")) { perform("replace", titleKey: "common.replace") }
                Button(loc("json.find.replaceAll")) { perform("replaceAll", titleKey: "json.find.replaceAll") }
            }
            if let findError { Text(findError).font(.caption).foregroundStyle(.red).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading) }
        }.textFieldStyle(.roundedBorder).toggleStyle(.button).controlSize(.small).padding(10).background(.quaternary.opacity(0.15))
    }
    private var statusBar: some View {
        HStack(spacing: 7) {
            Image(systemName: invalid ? "xmark.circle" : "checkmark.circle").foregroundStyle(invalid ? Color.red : .green)
            Text(draft.error ?? (validation.isEmpty ? loc("json.valid.idle") : validation)).lineLimit(2).textSelection(.enabled).foregroundStyle(draft.error == nil ? Color.secondary : .red)
            Spacer(minLength: 0)
            if draft.busy { ProgressView().controlSize(.mini); Button(loc("common.cancel")) { draft.jsonTask?.cancel() } }
            Text("\(draft.input.count) \(loc("editor.chars"))").foregroundStyle(.tertiary).fixedSize()
        }.font(.system(size: 10)).padding(.horizontal, 12).frame(minHeight: 31)
    }
    private var inspector: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                section(loc("json.panel.format")) {
                    HStack { Text(loc("json.format.indent")); Spacer(); Picker(loc("json.format.indent"), selection: option(\.indent)) { Text("2").tag(2); Text("4").tag(4) }.labelsHidden().frame(width: 75) }
                    Toggle(loc("json.format.sortKeys"), isOn: option(\.sortKeys))
                    Toggle(loc("json.format.ignoreCase"), isOn: option(\.ignoreCase))
                    Toggle(loc("json.format.duplicateKeys"), isOn: option(\.checkDuplicateKeys))
                    Button { perform("advanced", titleKey: "json.format.apply") } label: { Label(loc("json.format.apply"), systemImage: "sparkles").frame(maxWidth: .infinity) }.buttonStyle(.borderedProminent)
                }
                section(loc("json.panel.convert")) {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 7) {
                        conversionButton("json.action.jsonToXml", "jsonToXml", acceptance: "JSON → XML", output: true)
                        Button { showInput("xmlToJson", titleKey: "json.action.xmlToJson") } label: { Text(loc("json.action.xmlToJson")).lineLimit(1).minimumScaleFactor(0.8) }.help(loc("json.action.xmlToJson")).jsonAcceptanceControl("XML → JSON")
                        Button { showInput("beanToJson", titleKey: "json.action.beanToJson") } label: { Text(loc("json.action.beanToJson")).lineLimit(1).minimumScaleFactor(0.8) }.help(loc("json.action.beanToJson"))
                        conversionButton("json.action.jsonToBean", "jsonToBean", acceptance: "JSON → JavaBean", output: true)
                        conversionButton("json.action.swap", "swap", acceptance: "键值互换")
                        conversionButton("json.action.escape", "escape", acceptance: "JSON 转义")
                        conversionButton("json.action.unescape", "unescape", acceptance: "JSON 反转义")
                        conversionButton("json.action.escapeText", "escapeText", acceptance: "文本转义")
                        conversionButton("json.action.unescapeText", "unescapeText", acceptance: "文本反转义")
                    }.buttonStyle(.bordered).font(.system(size: 10))
                    Text(loc("json.className.label")).foregroundStyle(.secondary)
                    TextField("Root", text: option(\.className)).textFieldStyle(.roundedBorder)
                }
                section(loc("json.panel.jsonPath")) {
                    TextField("$.store.books[*].title", text: $draft.option).textFieldStyle(.roundedBorder).font(.system(.body, design: .monospaced)).onSubmit { perform("query", titleKey: "json.path.queryTitle", output: true) }
                    HStack {
                        Button(loc("json.path.query")) { perform("query", titleKey: "json.path.queryTitle", output: true) }.buttonStyle(.borderedProminent)
                        Button { pickedPath = "$"; dialog = JSONDialog(kind: .paths, titleKey: "json.pathPicker.title") } label: { Label(loc("json.path.pick"), systemImage: "list.bullet.indent") }
                    }
                }
                section(loc("json.panel.result")) {
                    Text(draft.error ?? (draft.status.isEmpty ? (validation.isEmpty ? loc("json.valid.idle") : validation) : draft.status)).foregroundStyle(draft.error == nil ? Color.secondary : .red).textSelection(.enabled)
                    if !draft.output.isEmpty { Button(loc("json.workspace.viewLastResult"), action: showLastResult) }
                }
            }.font(.system(size: 11)).controlSize(.small).toggleStyle(.checkbox)
        }.background(Color(nsColor: .controlBackgroundColor)).disabled(draft.busy)
    }
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View { VStack(alignment: .leading, spacing: 10) { Text(title).font(.system(size: 12, weight: .semibold)); content() }.padding(15).frame(maxWidth: .infinity, alignment: .leading).overlay(alignment: .bottom) { Divider() } }
    private func conversionButton(_ titleKey: String, _ action: String, acceptance: String, output: Bool = false) -> some View {
        let title = loc(titleKey)
        return Button { perform(action, titleKey: titleKey, output: output) } label: { Text(title).lineLimit(1).minimumScaleFactor(0.8) }.jsonAcceptanceControl(acceptance).frame(maxWidth: .infinity).help(title)
    }
    private func request(_ action: String, input: String? = nil) -> JSONEngineRequest {
        var r = JSONEngineRequest(action, input: input ?? draft.input)
        r.path = draft.option.isEmpty ? "$" : draft.option; r.className = options.className; r.indent = options.indent
        r.sortKeys = options.sortKeys; r.ignoreCase = options.ignoreCase; r.checkDuplicateKeys = options.checkDuplicateKeys
        r.query = options.findQuery; r.replacement = options.replacement; r.matchCase = options.matchCase; r.wholeWord = options.wholeWord; r.regex = options.regex
        r.selectionStart = editor.selection.location; r.selectionEnd = NSMaxRange(editor.selection)
        r.language = language.rawValue
        return r
    }
    private func perform(_ action: String, titleKey: String, output: Bool = false, input: String? = nil) {
        guard !draft.busy else { return }
        let title = loc(titleKey)
        let source = draft.input, id = draft.documentID, generation = store.editorRestoreGeneration, revision = draft.editorRevision
        let request = request(action, input: input); draft.busy = true; draft.error = nil; dialogError = nil
        let token = UUID(); operationID = token; draft.jsonOperationID = token
        let lang = language
        operation = Task { @MainActor in
            defer { if draft.jsonOperationID == token { draft.busy = false; draft.jsonTask = nil; draft.jsonOperationID = nil } }
            do {
                let reply = try await JSONEngine.execute(request); try Task.checkCancellation()
                guard draft.documentID == id, store.editorRestoreGeneration == generation, draft.editorRevision == revision else { return }
                guard draft.input == source else { draft.error = AppLocalization.string("json.error.contentChanged", language: lang); return }
                guard let value = reply.value else { throw ToolError(AppLocalization.string("json.error.noTextResult", language: lang)) }
                if output {
                    draft.output = value; resultSource = source; resultDocument = id; resultGeneration = generation
                    dialog = JSONDialog(kind: .output, titleKey: titleKey)
                } else {
                    guard editor.replace(value, expected: source, action: title) else { throw ToolError(AppLocalization.string("json.error.editorSwitched", language: lang)) }
                    if let match = reply.match { editor.select(match.range) }
                    if input != nil { dialog = nil }
                }
                if let count = reply.count {
                    draft.status = String(format: AppLocalization.string("json.status.doneCount", language: lang), title, count)
                } else {
                    draft.status = String(format: AppLocalization.string("json.status.done", language: lang), title)
                }
                var record = draft.record; record.input = request.input; record.output = value; record.mode = title
                store.record("json", snapshot: record)
            } catch { if !Task.isCancelled, draft.documentID == id, store.editorRestoreGeneration == generation { draft.error = error.localizedDescription; dialogError = error.localizedDescription } }
        }
        draft.jsonTask = operation
    }
    private func cancelOwnOperation() {
        operation?.cancel()
        if let operationID, draft.jsonOperationID == operationID { draft.jsonTask = nil; draft.jsonOperationID = nil; draft.busy = false }
        operationID = nil
    }
    private func find(forward: Bool) {
        let source = draft.input, id = draft.documentID, generation = store.editorRestoreGeneration
        var r = request("find"); r.forward = forward
        Task { do {
            let reply = try await JSONEngine.execute(r)
            guard draft.input == source, draft.documentID == id, store.editorRestoreGeneration == generation else { return }
            if let match = reply.match { editor.select(match.range) }; matchCount = reply.count ?? 0; findError = nil
        } catch { if draft.documentID == id { findError = error.localizedDescription } } }
    }
    private func openFind() {
        var next = options; next.findOpen = true
        let range = editor.selection
        if range.length > 0, NSMaxRange(range) <= (draft.input as NSString).length { next.findQuery = (draft.input as NSString).substring(with: range) }
        draft.json = next; findFocused = true
    }
    private func replaceImmediately(_ value: String, actionKey: String) {
        let action = loc(actionKey)
        if editor.replace(value, expected: draft.input, action: action) {
            draft.error = nil
            draft.status = locf("json.status.done", action)
        }
    }
    private func copy() { FilePanels.copy(draft.input); draft.status = loc("json.notice.copiedBody") }
    private func importFile() {
        let id = draft.documentID, source = draft.input, generation = store.editorRestoreGeneration, lang = language
        FilePanels.readText { value in
            guard draft.documentID == id, draft.input == source, store.editorRestoreGeneration == generation else {
                draft.error = AppLocalization.string("json.error.contentSwitched", language: lang)
                return
            }
            replaceImmediately(value, actionKey: "json.action.importFile")
        }
    }
    private func exportFile() { FilePanels.saveText(draft.input, name: "mootool.json") }
    private func showInput(_ action: String, titleKey: String) { conversionInput = ""; dialogError = nil; dialog = JSONDialog(kind: .input(action), titleKey: titleKey) }
    private func showLastResult() { resultSource = draft.input; resultDocument = draft.documentID; resultGeneration = store.editorRestoreGeneration; dialog = JSONDialog(kind: .output, titleKey: "json.lastResult") }
    @ViewBuilder private func sheet(_ value: JSONDialog) -> some View {
        VStack(spacing: 14) {
            HStack { Text(loc(value.titleKey)).font(.title2); Spacer(); Button(loc("common.close")) { dialog = nil }.keyboardShortcut(.cancelAction) }
            switch value.kind {
            case .input:
                CodeEditor(text: $conversionInput).frame(minHeight: 330)
            case .output:
                CodeEditor(text: .constant(draft.output), editable: false, syntax: true, persistence: store.editorPersistence("json", output: true)).frame(minHeight: 330)
            case .paths:
                HSplitView {
                    JSONTreePane(text: draft.input, path: $pickedPath)
                    VStack(alignment: .leading, spacing: 8) {
                        Text(loc("json.pathPicker.path")).foregroundStyle(.secondary)
                        Text(pickedPath).font(.system(.body, design: .monospaced)).textSelection(.enabled)
                        Text(loc("json.pathPicker.preview")).foregroundStyle(.secondary)
                        CodeEditor(text: .constant(pathPreview), editable: false, syntax: true)
                    }.padding(12).frame(minWidth: 220)
                }.frame(minHeight: 380)
                .task(id: pickedPath) { do { var r = JSONEngineRequest("query", input: draft.input); r.path = pickedPath; let result = try await JSONEngine.execute(r); try Task.checkCancellation(); pathPreview = result.value ?? "" } catch { if !Task.isCancelled { pathPreview = error.localizedDescription } } }
            }
            if let dialogError { Text(dialogError).foregroundStyle(.red).font(.caption).textSelection(.enabled) }
            HStack {
                if case .output = value.kind {
                    Button(loc("json.dialog.copyResult")) { FilePanels.copy(draft.output) }
                    Button(loc("json.dialog.exportResult")) { FilePanels.saveText(draft.output) }
                }
                Spacer()
                switch value.kind {
                case .input(let action):
                    Button(loc("json.dialog.run")) { perform(action, titleKey: value.titleKey, input: conversionInput) }.buttonStyle(.borderedProminent).disabled(draft.busy).jsonAcceptanceControl("转换")
                case .output:
                    Button(loc("json.dialog.useOutput")) {
                        guard draft.documentID == resultDocument, draft.input == resultSource, store.editorRestoreGeneration == resultGeneration else {
                            dialogError = loc("json.error.regenerate")
                            return
                        }
                        replaceImmediately(draft.output, actionKey: "json.dialog.useResult")
                        dialog = nil
                    }.buttonStyle(.borderedProminent).jsonAcceptanceControl("使用此结果")
                case .paths:
                    Button(loc("json.pathPicker.use")) { draft.option = pickedPath; dialog = nil }.buttonStyle(.borderedProminent)
                }
            }
        }.padding(22).frame(width: 740, height: 530)
    }
}
private struct JSONDialog: Identifiable {
    enum Kind { case input(String), output, paths }
    var id = UUID()
    var kind: Kind
    var titleKey: String
}
private struct JSONHistorySheet: View {
    @Bindable var draft: ToolDraft
    let editor: NativeEditorBridge
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @Environment(\.dismiss) private var dismiss
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private var jsonTitle: String { Catalog.localizedTool("json", language: language).title }
    var body: some View {
        VStack {
            HStack {
                Text("\(jsonTitle) · \(loc("workbench.history"))").font(.title2)
                Spacer()
                Button(loc("common.done")) { dismiss() }
            }.padding()
            List(store.history.filter { $0.toolID == "json" }) { item in
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.draft.mode.isEmpty ? loc("json.history.defaultMode") : item.draft.mode).font(.headline)
                        Text(item.draft.output).font(.system(.caption, design: .monospaced)).lineLimit(2)
                        Text(item.date.formatted()).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button(loc("json.dialog.useResult")) {
                        let action = loc("json.history.restore")
                        if editor.replace(item.draft.output, expected: draft.input, action: action) { dismiss() }
                    }
                }.padding(.vertical, 5)
            }.overlay {
                if !store.history.contains(where: { $0.toolID == "json" }) {
                    ContentUnavailableView(
                        loc("history.empty.title"),
                        systemImage: "clock",
                        description: Text(loc("history.empty.description")))
                }
            }
        }.frame(width: 680, height: 470)
    }
}
