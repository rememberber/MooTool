import SwiftUI
import MooToolNextCore

struct HTTPTool: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @State private var requestTab = "参数"
    @State private var responseTab = "正文"
    @State private var pretty = true
    @State private var formattedResponse = ""
    @State private var formattedSource = ""
    @State private var sheet: RequestSheet?
    private enum RequestSheet: String, Identifiable { case curl, library, save; var id: String { rawValue } }
    private func option<Value>(_ keyPath: WritableKeyPath<HTTPOptions, Value>) -> Binding<Value> {
        Binding(get: { (draft.http ?? HTTPOptions())[keyPath: keyPath] }, set: { value in var options = draft.http ?? HTTPOptions(); options[keyPath: keyPath] = value; draft.http = options })
    }
    var body: some View {
        ToolPage(tool: Catalog.tool("http"), draft: draft) {
            Picker("方法", selection: $draft.mode) { ForEach(["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"], id: \.self) { Text($0) } }.labelsHidden().frame(width: 96)
            TextField("https://example.com/api", text: $draft.option).textFieldStyle(.roundedBorder).frame(minWidth: 180).onSubmit(send)
            PrimaryButton(title: "发送", symbol: "paperplane.fill", action: send)
            Menu {
                Button("导入 cURL…") { sheet = .curl }
                Button("复制为 cURL") { do { FilePanels.copy(try CurlCommand.export(draft.record)); draft.status = "已复制 cURL" } catch { draft.error = error.localizedDescription } }
                Divider()
                Button("保存到请求集合…") { sheet = .save }
                Button("打开请求集合…") { sheet = .library }
                Divider()
                Button("新建请求") { draft.apply(DraftRecord()); draft.mode = "GET" }
            } label: { Image(systemName: "ellipsis.circle") }.help("cURL 与请求集合")
        } content: {
            VStack(spacing: 12) {
                HStack {
                    Button { sheet = .library } label: { Label("请求集合 · \(store.httpRequests.count)", systemImage: "folder") }.disabled(draft.busy)
                    Button { sheet = .save } label: { Image(systemName: "square.and.arrow.down") }.help("保存当前请求").disabled(draft.busy)
                    Spacer()
                    if draft.busy { ProgressView().controlSize(.small); Button("取消请求") { draft.httpTask?.cancel() } }
                    else {
                        Toggle("跟随重定向", isOn: option(\.followRedirects)).toggleStyle(.checkbox)
                        Picker("超时", selection: option(\.timeout)) {
                            ForEach(Array(Set([5.0, 10, 30, 60, 120, draft.http?.timeout ?? 30])).sorted(), id: \.self) { Text("\($0.formatted()) 秒").tag($0) }
                        }.frame(width: 118)
                    }
                }.font(.caption).controlSize(.small)
                HSplitView {
                    requestPane.frame(minWidth: 270).disabled(draft.busy)
                    responsePane.frame(minWidth: 270)
                }
            }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "GET" } }
        .task(id: draft.output) {
            let output = draft.output
            let formatted = await Task.detached { (try? TextServices.json(output)) ?? output }.value
            guard !Task.isCancelled else { return }; formattedResponse = formatted; formattedSource = output
        }
        .sheet(item: $sheet) { item in
            switch item {
            case .curl: CurlImportSheet { imported in draft.apply(imported); requestTab = imported.input.isEmpty ? "参数" : "正文"; draft.status = "已导入 cURL，请检查后发送" }
            case .library: HTTPRequestLibrary { draft.apply($0) }.environment(store)
            case .save: SaveHTTPRequestSheet(draft: draft.record).environment(store)
            }
        }
    }
    private var requestPane: some View {
        VStack(spacing: 10) {
            HStack { Text("请求").font(.headline); Spacer(); Button("导入 cURL") { sheet = .curl }.controlSize(.small) }
            Picker("请求内容", selection: $requestTab) { ForEach(["参数", "请求头", "Cookie", "正文"], id: \.self) { Text($0) } }.pickerStyle(.segmented).labelsHidden()
            Group {
                switch requestTab {
                case "参数": HTTPFieldsEditor(fields: option(\.params), description: "参数追加到 URL，保留已有参数与重复键。", keyHint: "参数名")
                case "Cookie": HTTPFieldsEditor(fields: option(\.cookies), description: "仅发送启用的 Cookie；每次请求使用独立会话。", keyHint: "Cookie 名")
                case "请求头": EditorPane(title: "请求头 · Name: Value", text: $draft.secondary)
                default:
                    VStack(spacing: 10) {
                        Picker("正文类型", selection: option(\.bodyKind)) { ForEach(HTTPBodyKind.allCases, id: \.self) { Text($0.title).tag($0) } }
                        if ["GET", "HEAD"].contains(draft.mode) { Text("\(draft.mode) 不发送正文；可在“参数”中编辑查询条件。").font(.caption).foregroundStyle(.secondary) }
                        Group {
                            switch draft.http?.bodyKind ?? .raw {
                            case .none: ContentUnavailableView("无请求正文", systemImage: "doc", description: Text("更换正文类型可编辑数据。"))
                            case .form: HTTPFieldsEditor(fields: option(\.form), description: "以 application/x-www-form-urlencoded 编码发送。", keyHint: "字段名")
                            case .raw, .json: EditorPane(title: "请求正文", text: $draft.input, syntax: draft.http?.bodyKind == .json)
                            }
                        }.disabled(["GET", "HEAD"].contains(draft.mode))
                    }
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        }.padding(.trailing, 7)
    }
    private var responsePane: some View {
        VStack(spacing: 10) {
            HStack {
                Text("响应").font(.headline); Spacer()
                if let result = draft.httpResult {
                    Text("\(result.status)").font(.system(.caption, design: .monospaced).bold()).foregroundStyle(result.status >= 400 ? Color.red : result.status >= 300 ? .orange : .green)
                    Text("\(Int(result.elapsed * 1000)) ms · \(ByteCountFormatter.string(fromByteCount: Int64(result.bytes), countStyle: .file))").font(.caption).foregroundStyle(.secondary)
                }
            }.frame(height: 22)
            HStack(spacing: 8) {
                Picker("响应内容", selection: $responseTab) { ForEach(["正文", "响应头", "Cookie"], id: \.self) { Text($0) } }.pickerStyle(.segmented).labelsHidden()
                if responseTab == "正文" { Toggle(isOn: $pretty) { Image(systemName: "text.alignleft") }.toggleStyle(.button).help("JSON 格式化显示") }
            }
            EditorPane(title: responseTab, text: .constant(responseText), editable: false, syntax: responseTab == "正文")
            if let result = draft.httpResult { Text(result.url).font(.system(size: 10, design: .monospaced)).foregroundStyle(.secondary).lineLimit(1).help(result.url) }
        }.padding(.leading, 7)
    }
    private var responseText: String {
        switch responseTab {
        case "响应头": return draft.httpResult?.headers ?? ""
        case "Cookie": return draft.httpResult?.cookies ?? ""
        default: return pretty && formattedSource == draft.output ? formattedResponse : draft.output
        }
    }
    private func send() {
        guard !draft.busy else { return }
        do {
            let snapshot = draft.record
            let request = try NetworkServices.request(snapshot)
            draft.busy = true; draft.error = nil; draft.status = "正在请求…"
            draft.httpTask = Task {
                defer { draft.busy = false; draft.httpTask = nil }
                do {
                    let response = try await NetworkServices.send(request, followRedirects: snapshot.http?.followRedirects ?? true)
                    try Task.checkCancellation()
                    draft.status = "HTTP \(response.status) · \(Int(response.elapsed * 1000)) ms · \(response.bytes) bytes"
                    draft.output = response.body; draft.httpResult = response.metadata
                    var completed = snapshot; completed.output = response.body; completed.httpResult = response.metadata
                    store.record("http", snapshot: completed)
                } catch {
                    if Task.isCancelled { draft.status = "请求已取消" } else { draft.error = error.localizedDescription }
                }
            }
        } catch { draft.error = error.localizedDescription }
    }
}

struct HTTPFieldsEditor: View {
    @Binding var fields: [HTTPField]
    let description: String
    let keyHint: String
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(description).font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach($fields) { $field in
                        HStack(spacing: 7) {
                            Toggle("启用", isOn: $field.enabled).labelsHidden().toggleStyle(.checkbox)
                            TextField(keyHint, text: $field.name).accessibilityLabel(keyHint)
                            TextField("值", text: $field.value).accessibilityLabel("值")
                            Button { fields.removeAll { $0.id == field.id } } label: { Image(systemName: "minus.circle") }.buttonStyle(.borderless).help("删除此行")
                        }.textFieldStyle(.roundedBorder).font(.system(size: 12, design: .monospaced))
                    }
                }.padding(1)
            }
            HStack { Button { fields.append(HTTPField()) } label: { Label("添加一行", systemImage: "plus") }.disabled(fields.count >= 1000); Spacer(); Text("\(HTTPFields.active(fields).count) 项启用").foregroundStyle(.secondary) }.font(.caption)
        }.padding(13).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 9)).overlay(RoundedRectangle(cornerRadius: 9).strokeBorder(.quaternary))
    }
}

private struct CurlImportSheet: View {
    let onImport: (DraftRecord) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var command = ""
    @State private var error: String?
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("导入 cURL").font(.title2.bold())
            Text("粘贴从浏览器或终端复制的请求，导入后可继续编辑。支持单引号、双引号和反斜杠续行。").foregroundStyle(.secondary)
            EditorPane(title: "cURL", text: $command)
            if let error { Text(error).font(.caption).foregroundStyle(.red).textSelection(.enabled) }
            HStack { Button("取消") { dismiss() }.keyboardShortcut(.cancelAction); Spacer(); Button("导入") { do { let value = try CurlCommand.parse(command); onImport(value); dismiss() } catch { self.error = error.localizedDescription } }.buttonStyle(.borderedProminent).keyboardShortcut(.defaultAction).disabled(command.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
        }.padding(24).frame(width: 660, height: 460)
    }
}

private struct SaveHTTPRequestSheet: View {
    let draft: DraftRecord
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var collection = "我的请求"
    @State private var replaceID: UUID?
    private var existing: SavedHTTPRequest? { store.httpRequests.first { $0.name == name.trimmingCharacters(in: .whitespacesAndNewlines) && $0.collection == normalizedCollection } }
    private var normalizedCollection: String { collection.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "我的请求" : collection.trimmingCharacters(in: .whitespacesAndNewlines) }
    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("保存请求").font(.title2.bold())
            TextField("请求名称", text: $name)
            HStack { TextField("集合名称", text: $collection); Menu { ForEach(Array(Set(store.httpRequests.map(\.collection))).sorted(), id: \.self) { value in Button(value) { collection = value } } } label: { Image(systemName: "folder") } }
            Text("保存方法、URL、参数、Cookie、正文和当前响应。").font(.caption).foregroundStyle(.secondary)
            HStack { Button("取消") { dismiss() }.keyboardShortcut(.cancelAction); Spacer(); Button(existing == nil ? "保存" : "替换同名请求") { if let existing { replaceID = existing.id } else { save() } }.buttonStyle(.borderedProminent).keyboardShortcut(.defaultAction).disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
        }.textFieldStyle(.roundedBorder).padding(24).frame(width: 420)
        .onAppear { name = URL(string: draft.option)?.host ?? "未命名请求" }
        .confirmationDialog("替换同一集合中的同名请求？", isPresented: Binding(get: { replaceID != nil }, set: { if !$0 { replaceID = nil } }), presenting: replaceID) { id in Button("替换", role: .destructive) { save(replacing: id) } }
    }
    private func save(replacing id: UUID? = nil) {
        var value = SavedHTTPRequest(name: name.trimmingCharacters(in: .whitespacesAndNewlines), collection: normalizedCollection, draft: draft)
        if let id, let index = store.httpRequests.firstIndex(where: { $0.id == id }) { value.id = id; store.httpRequests[index] = value }
        else { store.httpRequests.append(value) }
        store.saveNow(); dismiss()
    }
}

private struct HTTPRequestLibrary: View {
    let onOpen: (DraftRecord) -> Void
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var search = ""
    @State private var deleting: UUID?
    private var matches: [SavedHTTPRequest] { store.httpRequests.filter { search.isEmpty || [$0.name, $0.collection, $0.draft.option].contains { $0.localizedCaseInsensitiveContains(search) } } }
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack { Text("请求集合").font(.title2.bold()); Spacer(); Button("完成") { dismiss() }.keyboardShortcut(.cancelAction) }
            TextField("搜索名称、集合或 URL", text: $search).textFieldStyle(.roundedBorder)
            List {
                ForEach(Array(Set(matches.map(\.collection))).sorted(), id: \.self) { collection in
                    Section(collection) {
                        ForEach(matches.filter { $0.collection == collection }.sorted { $0.modified > $1.modified }) { item in
                            HStack(spacing: 12) {
                                Text(item.draft.mode.isEmpty ? "GET" : item.draft.mode).font(.system(.caption, design: .monospaced).bold()).foregroundStyle(.tint).frame(width: 54)
                                VStack(alignment: .leading, spacing: 4) { Text(item.name).fontWeight(.medium); Text(item.draft.option).font(.caption).foregroundStyle(.secondary).lineLimit(1) }
                                Spacer()
                                Button("打开") { onOpen(item.draft); dismiss() }
                                Button { deleting = item.id } label: { Image(systemName: "trash") }.help("删除请求")
                            }.padding(.vertical, 5)
                        }
                    }
                }
            }.overlay { if matches.isEmpty { ContentUnavailableView("暂无请求", systemImage: "folder", description: Text(search.isEmpty ? "编辑请求后，点击保存添加到集合。" : "没有匹配的请求。")) } }
        }.padding(22).frame(width: 700, height: 480)
        .confirmationDialog("删除这份保存的请求？", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), presenting: deleting) { id in Button("删除", role: .destructive) { store.httpRequests.removeAll { $0.id == id }; store.saveNow(); deleting = nil } }
    }
}
