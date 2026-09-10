import SwiftUI
import MooToolNextCore

struct JSONWorkspace: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @State private var editor = NativeEditorBridge()
    @State private var operation: Task<Void, Never>?
    @State private var operationID: UUID?
    @State private var validation = "输入 JSON 开始"
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
    var body: some View {
        GeometryReader { geometry in
            let expanded = options.inspectorOpen ?? (geometry.size.width >= 720)
            HSplitView {
                VStack(spacing: 0) {
                    toolbar(compact: expanded && geometry.size.width >= 660, availableWidth: geometry.size.width)
                    if options.findOpen { findBar }
                    Divider()
                    CodeEditor(text: $draft.input, syntax: true, persistence: store.editorPersistence("json"), bridge: editor,
                               softWrap: options.wrapLines, fontName: options.fontName)
                    Divider()
                    statusBar
                }.frame(minWidth: 350)
                if expanded && geometry.size.width >= 660 { inspector.frame(minWidth: 240, idealWidth: 260, maxWidth: 310) }
            }
            .onChange(of: expanded) { _, value in if value && geometry.size.width < 660 { inspectorPopup = true } }
        }
        .background(Color(nsColor: .textBackgroundColor))
        .popover(isPresented: $inspectorPopup) { inspector.frame(width: 300, height: 660) }
        .sheet(item: $dialog, onDismiss: { var next = options; next.showsTree = false; draft.json = next }) { sheet($0) }
        .sheet(isPresented: $historyOpen) { JSONHistorySheet(draft: draft, editor: editor).environment(store) }
        .task(id: draft.input) {
            let source = draft.input
            if source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { validation = "输入 JSON 开始"; invalid = false; return }
            do {
                try await Task.sleep(for: .milliseconds(220))
                let reply = try await JSONEngine.execute(JSONEngineRequest("validate", input: source))
                try Task.checkCancellation(); validation = reply.value ?? "有效 JSON"; invalid = false
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
        .onAppear { if options.showsTree { pickedPath = "$"; dialog = JSONDialog(kind: .paths, title: "选择 JSON 路径") } }
        .onChange(of: options.showsTree) { _, value in
            if value { pickedPath = "$"; dialog = JSONDialog(kind: .paths, title: "选择 JSON 路径") }
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
                    Picker("字体", selection: option(\.fontName)) { fontChoices }
                    Button("复制正文") { copy() }; Button("导入文件…", action: importFile); Button("导出文件…", action: exportFile)
                    Button("历史记录") { historyOpen = true }; Button("查看上次结果") { showLastResult() }.disabled(draft.output.isEmpty)
                    Button("清空正文") { replaceImmediately("", action: "清空") }
                } label: { Image(systemName: "ellipsis.circle") }.menuStyle(.borderlessButton).menuIndicator(.hidden).fixedSize().help("更多编辑操作")
                Spacer(minLength: 0); inspectorButton(availableWidth)
            }
        }.controlSize(.small).padding(.horizontal, 12).frame(maxWidth: .infinity, alignment: .leading).frame(height: 43).background(.bar).disabled(draft.busy)
    }
    @ViewBuilder private var primaryActions: some View {
        Button { perform("format", title: "格式化") } label: { Label("格式化", systemImage: "sparkles") }.buttonStyle(.borderedProminent).keyboardShortcut(.return, modifiers: .command).accessibilityIdentifier("json.format")
        Button("压缩") { perform("compress", title: "压缩") }.accessibilityIdentifier("json.compress")
    }
    private var fontPicker: some View { Picker("字体", selection: option(\.fontName)) { fontChoices }.labelsHidden().frame(width: 95) }
    @ViewBuilder private var fontChoices: some View { ForEach(["Menlo", "Monaco", "SFMono-Regular", "CourierNewPSMT"], id: \.self) { Text($0 == "SFMono-Regular" ? "SF Mono" : $0 == "CourierNewPSMT" ? "Courier New" : $0).tag($0) } }
    private var wrapButton: some View { icon("自动换行", "arrow.turn.down.left") { var next = options; next.wrapLines = !(next.wrapLines ?? nativeDefaults.object(forKey: "wrapLines") as? Bool ?? true); draft.json = next } }
    private var copyButton: some View { icon("复制正文", "doc.on.doc", action: copy) }
    private var findButton: some View { icon("查找和替换", "magnifyingglass", action: openFind).keyboardShortcut("f", modifiers: .command).accessibilityIdentifier("json.find") }
    private var importButton: some View { icon("导入文件", "folder", action: importFile) }
    private var exportButton: some View { icon("导出文件", "square.and.arrow.up", action: exportFile) }
    private var historyButton: some View { icon("历史记录", "clock.arrow.circlepath") { historyOpen = true } }
    private var clearButton: some View { icon("清空正文", "eraser") { replaceImmediately("", action: "清空") } }
    private func inspectorButton(_ width: CGFloat) -> some View { icon("显示或隐藏检查器", "sidebar.right") { if width < 660 { inspectorPopup.toggle() } else { var next = options; next.inspectorOpen = !(next.inspectorOpen ?? (width >= 720)); draft.json = next } }.accessibilityIdentifier("json.inspector") }
    private func icon(_ title: String, _ symbol: String, action: @escaping () -> Void) -> some View { Button(action: action) { Image(systemName: symbol).frame(width: 17, height: 20) }.buttonStyle(.borderless).help(title).accessibilityLabel(title) }
    private var findBar: some View {
        VStack(spacing: 7) {
            HStack(spacing: 6) {
                TextField("查找", text: option(\.findQuery)).focused($findFocused).onSubmit { find(forward: true) }
                Toggle("Aa", isOn: option(\.matchCase)).help("区分大小写")
                Toggle("词", isOn: option(\.wholeWord)).help("全词匹配")
                Toggle(".*", isOn: option(\.regex)).help("正则表达式")
                icon("上一个", "chevron.up") { find(forward: false) }; icon("下一个", "chevron.down") { find(forward: true) }
                icon("关闭查找", "xmark") { var next = options; next.findOpen = false; draft.json = next }
            }
            HStack(spacing: 6) {
                TextField("替换为", text: option(\.replacement)).onSubmit { perform("replace", title: "替换") }
                Text("\(matchCount) 项").font(.caption).foregroundStyle(.secondary).fixedSize()
                Button("替换") { perform("replace", title: "替换") }; Button("全部替换") { perform("replaceAll", title: "全部替换") }
            }
            if let findError { Text(findError).font(.caption).foregroundStyle(.red).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading) }
        }.textFieldStyle(.roundedBorder).toggleStyle(.button).controlSize(.small).padding(10).background(.quaternary.opacity(0.15))
    }
    private var statusBar: some View {
        HStack(spacing: 7) {
            Image(systemName: invalid ? "xmark.circle" : "checkmark.circle").foregroundStyle(invalid ? Color.red : .green)
            Text(draft.error ?? validation).lineLimit(2).textSelection(.enabled).foregroundStyle(draft.error == nil ? Color.secondary : .red)
            Spacer(minLength: 0)
            if draft.busy { ProgressView().controlSize(.mini); Button("取消") { draft.jsonTask?.cancel() } }
            Text("\(draft.input.count) 字符").foregroundStyle(.tertiary).fixedSize()
        }.font(.system(size: 10)).padding(.horizontal, 12).frame(minHeight: 31)
    }
    private var inspector: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                section("格式化") {
                    HStack { Text("缩进"); Spacer(); Picker("缩进", selection: option(\.indent)) { Text("2").tag(2); Text("4").tag(4) }.labelsHidden().frame(width: 75) }
                    Toggle("按键名排序", isOn: option(\.sortKeys)); Toggle("忽略大小写", isOn: option(\.ignoreCase)); Toggle("检查重复键", isOn: option(\.checkDuplicateKeys))
                    Button { perform("advanced", title: "应用格式化") } label: { Label("应用格式化", systemImage: "sparkles").frame(maxWidth: .infinity) }.buttonStyle(.borderedProminent)
                }
                section("转换") {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 7) {
                        conversionButton("JSON → XML", "jsonToXml", output: true)
                        Button("XML → JSON") { showInput("xmlToJson", title: "XML → JSON") }.jsonAcceptanceControl("XML → JSON")
                        Button { showInput("beanToJson", title: "JavaBean → JSON") } label: { Text("JavaBean → JSON").lineLimit(1).minimumScaleFactor(0.8) }.help("JavaBean → JSON")
                        conversionButton("JSON → JavaBean", "jsonToBean", output: true)
                        conversionButton("键值互换", "swap"); conversionButton("JSON 转义", "escape")
                        conversionButton("JSON 反转义", "unescape"); conversionButton("文本转义", "escapeText")
                        conversionButton("文本反转义", "unescapeText")
                    }.buttonStyle(.bordered).font(.system(size: 10))
                    Text("Java 类名").foregroundStyle(.secondary); TextField("Root", text: option(\.className)).textFieldStyle(.roundedBorder)
                }
                section("JSONPath") {
                    TextField("$.store.books[*].title", text: $draft.option).textFieldStyle(.roundedBorder).font(.system(.body, design: .monospaced)).onSubmit { perform("query", title: "JSONPath 查询", output: true) }
                    HStack { Button("查询") { perform("query", title: "JSONPath 查询", output: true) }.buttonStyle(.borderedProminent); Button { pickedPath = "$"; dialog = JSONDialog(kind: .paths, title: "选择 JSON 路径") } label: { Label("选择路径", systemImage: "list.bullet.indent") } }
                }
                section("结果") {
                    Text(draft.error ?? (draft.status.isEmpty ? validation : draft.status)).foregroundStyle(draft.error == nil ? Color.secondary : .red).textSelection(.enabled)
                    if !draft.output.isEmpty { Button("查看上次结果", action: showLastResult) }
                }
            }.font(.system(size: 11)).controlSize(.small).toggleStyle(.checkbox)
        }.background(Color(nsColor: .controlBackgroundColor)).disabled(draft.busy)
    }
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View { VStack(alignment: .leading, spacing: 10) { Text(title).font(.system(size: 12, weight: .semibold)); content() }.padding(15).frame(maxWidth: .infinity, alignment: .leading).overlay(alignment: .bottom) { Divider() } }
    private func conversionButton(_ title: String, _ action: String, output: Bool = false) -> some View { Button { perform(action, title: title, output: output) } label: { Text(title).lineLimit(1).minimumScaleFactor(0.8) }.jsonAcceptanceControl(title).frame(maxWidth: .infinity).help(title) }
    private func request(_ action: String, input: String? = nil) -> JSONEngineRequest {
        var r = JSONEngineRequest(action, input: input ?? draft.input)
        r.path = draft.option.isEmpty ? "$" : draft.option; r.className = options.className; r.indent = options.indent
        r.sortKeys = options.sortKeys; r.ignoreCase = options.ignoreCase; r.checkDuplicateKeys = options.checkDuplicateKeys
        r.query = options.findQuery; r.replacement = options.replacement; r.matchCase = options.matchCase; r.wholeWord = options.wholeWord; r.regex = options.regex
        r.selectionStart = editor.selection.location; r.selectionEnd = NSMaxRange(editor.selection); return r
    }
    private func perform(_ action: String, title: String, output: Bool = false, input: String? = nil) {
        guard !draft.busy else { return }
        let source = draft.input, id = draft.documentID, generation = store.editorRestoreGeneration, revision = draft.editorRevision
        let request = request(action, input: input); draft.busy = true; draft.error = nil; dialogError = nil
        let token = UUID(); operationID = token; draft.jsonOperationID = token
        operation = Task {
            defer { if draft.jsonOperationID == token { draft.busy = false; draft.jsonTask = nil; draft.jsonOperationID = nil } }
            do {
                let reply = try await JSONEngine.execute(request); try Task.checkCancellation()
                guard draft.documentID == id, store.editorRestoreGeneration == generation, draft.editorRevision == revision else { return }
                guard draft.input == source else { draft.error = "正文已变化，请重新执行操作。"; return }
                guard let value = reply.value else { throw ToolError("操作没有返回文本结果。") }
                if output {
                    draft.output = value; resultSource = source; resultDocument = id; resultGeneration = generation
                    dialog = JSONDialog(kind: .output, title: title)
                } else {
                    guard editor.replace(value, expected: source, action: title) else { throw ToolError("编辑器已切换，未应用结果。") }
                    if let match = reply.match { editor.select(match.range) }
                    if input != nil { dialog = nil }
                }
                draft.status = title + "已完成" + (reply.count.map { " · \($0) 项" } ?? "")
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
    private func replaceImmediately(_ value: String, action: String) { if editor.replace(value, expected: draft.input, action: action) { draft.error = nil; draft.status = action + "已完成" } }
    private func copy() { FilePanels.copy(draft.input); draft.status = "已复制正文" }
    private func importFile() {
        let id = draft.documentID, source = draft.input, generation = store.editorRestoreGeneration
        FilePanels.readText { value in
            guard draft.documentID == id, draft.input == source, store.editorRestoreGeneration == generation else { draft.error = "正文已切换，未替换当前内容。"; return }
            replaceImmediately(value, action: "导入文件")
        }
    }
    private func exportFile() { FilePanels.saveText(draft.input, name: "mootool.json") }
    private func showInput(_ action: String, title: String) { conversionInput = ""; dialogError = nil; dialog = JSONDialog(kind: .input(action), title: title) }
    private func showLastResult() { resultSource = draft.input; resultDocument = draft.documentID; resultGeneration = store.editorRestoreGeneration; dialog = JSONDialog(kind: .output, title: "上次结果") }
    @ViewBuilder private func sheet(_ value: JSONDialog) -> some View {
        VStack(spacing: 14) {
            HStack { Text(value.title).font(.title2); Spacer(); Button("关闭") { dialog = nil }.keyboardShortcut(.cancelAction) }
            switch value.kind {
            case .input:
                CodeEditor(text: $conversionInput).frame(minHeight: 330)
            case .output:
                CodeEditor(text: .constant(draft.output), editable: false, syntax: true, persistence: store.editorPersistence("json", output: true)).frame(minHeight: 330)
            case .paths:
                HSplitView {
                    JSONTreePane(text: draft.input, path: $pickedPath)
                    VStack(alignment: .leading, spacing: 8) { Text("路径").foregroundStyle(.secondary); Text(pickedPath).font(.system(.body, design: .monospaced)).textSelection(.enabled); Text("值预览").foregroundStyle(.secondary); CodeEditor(text: .constant(pathPreview), editable: false, syntax: true) }.padding(12).frame(minWidth: 220)
                }.frame(minHeight: 380)
                .task(id: pickedPath) { do { var r = JSONEngineRequest("query", input: draft.input); r.path = pickedPath; let result = try await JSONEngine.execute(r); try Task.checkCancellation(); pathPreview = result.value ?? "" } catch { if !Task.isCancelled { pathPreview = error.localizedDescription } } }
            }
            if let dialogError { Text(dialogError).foregroundStyle(.red).font(.caption).textSelection(.enabled) }
            HStack {
                if case .output = value.kind { Button("复制结果") { FilePanels.copy(draft.output) }; Button("导出结果") { FilePanels.saveText(draft.output) } }
                Spacer()
                switch value.kind {
                case .input(let action): Button("转换") { perform(action, title: value.title, input: conversionInput) }.buttonStyle(.borderedProminent).disabled(draft.busy).jsonAcceptanceControl("转换")
                case .output: Button("使用此结果") {
                    guard draft.documentID == resultDocument, draft.input == resultSource, store.editorRestoreGeneration == resultGeneration else { dialogError = "正文已变化，请重新生成结果。"; return }
                    replaceImmediately(draft.output, action: "使用结果"); dialog = nil
                }.buttonStyle(.borderedProminent).jsonAcceptanceControl("使用此结果")
                case .paths: Button("使用此路径") { draft.option = pickedPath; dialog = nil }.buttonStyle(.borderedProminent)
                }
            }
        }.padding(22).frame(width: 740, height: 530)
    }
}
private struct JSONDialog: Identifiable {
    enum Kind { case input(String), output, paths }
    var id = UUID()
    var kind: Kind
    var title: String
}
private struct JSONHistorySheet: View {
    @Bindable var draft: ToolDraft
    let editor: NativeEditorBridge
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack {
            HStack { Text("JSON · 历史记录").font(.title2); Spacer(); Button("完成") { dismiss() } }.padding()
            List(store.history.filter { $0.toolID == "json" }) { item in
                HStack {
                    VStack(alignment: .leading, spacing: 6) { Text(item.draft.mode.isEmpty ? "JSON 操作" : item.draft.mode).font(.headline); Text(item.draft.output).font(.system(.caption, design: .monospaced)).lineLimit(2); Text(item.date.formatted()).font(.caption).foregroundStyle(.secondary) }
                    Spacer(); Button("使用结果") { if editor.replace(item.draft.output, expected: draft.input, action: "恢复历史") { dismiss() } }
                }.padding(.vertical, 5)
            }.overlay { if !store.history.contains(where: { $0.toolID == "json" }) { ContentUnavailableView("暂无历史记录", systemImage: "clock") } }
        }.frame(width: 680, height: 470)
    }
}
