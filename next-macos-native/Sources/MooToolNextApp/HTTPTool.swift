import AppKit
import SwiftUI
import MooToolNextCore

struct HTTPTool: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var requestTab: HTTPRequestTab = .params
    @State private var responseTab: HTTPResponseTab = .body
    @State private var pretty = true
    @State private var formattedResponse = ""
    @State private var formattedSource = ""
    @State private var sheet: RequestSheet?
    private enum RequestSheet: String, Identifiable { case curl, library, save; var id: String { rawValue } }
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    private func option<Value>(_ keyPath: WritableKeyPath<HTTPOptions, Value>) -> Binding<Value> {
        Binding(get: { (draft.http ?? HTTPOptions())[keyPath: keyPath] }, set: { value in var options = draft.http ?? HTTPOptions(); options[keyPath: keyPath] = value; draft.http = options })
    }
    var body: some View {
        ToolPage(tool: Catalog.tool("http"), draft: draft) {
            Picker(loc("http.method"), selection: $draft.mode) { ForEach(["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"], id: \.self) { Text($0) } }.labelsHidden().frame(width: 96)
            TextField("https://example.com/api", text: $draft.option).textFieldStyle(.roundedBorder).frame(minWidth: 180).onSubmit(send)
            PrimaryButton(title: AppLocalization.string("tool.send", language: language), symbol: "paperplane.fill", action: send)
            Menu {
                Button(AppLocalization.string("http.importCurl", language: language)) { sheet = .curl }
                Button(loc("http.copyCurl")) { do { FilePanels.copy(try CurlCommand.export(draft.record)); draft.status = loc("http.status.copiedCurl") } catch { draft.error = error.localizedDescription } }
                Divider()
                Button(AppLocalization.string("http.saveToCollection", language: language)) { sheet = .save }
                Button(AppLocalization.string("http.openCollection", language: language)) { sheet = .library }
                Divider()
                Button(AppLocalization.string("http.newRequest", language: language)) { draft.apply(DraftRecord()); draft.mode = "GET" }
            } label: { Image(systemName: "ellipsis.circle") }.help(AppLocalization.string("http.menuHelp", language: language))
        } content: {
            VStack(spacing: 12) {
                HStack {
                    Button { sheet = .library } label: { Label("\(AppLocalization.string("http.collection", language: language)) · \(store.httpRequests.count)", systemImage: "folder") }.disabled(draft.busy)
                    Button { sheet = .save } label: { Image(systemName: "square.and.arrow.down") }.help(AppLocalization.string("http.saveRequest", language: language)).disabled(draft.busy)
                    Spacer()
                    if draft.busy { ProgressView().controlSize(.small); Button(AppLocalization.string("http.cancelRequest", language: language)) { draft.httpTask?.cancel() } }
                    else {
                        Toggle(AppLocalization.string("http.followRedirects", language: language), isOn: option(\.followRedirects)).toggleStyle(.checkbox)
                        Picker(AppLocalization.string("http.timeout", language: language), selection: option(\.timeout)) {
                            let unit = AppLocalization.string("http.seconds", language: language)
                            ForEach(Array(Set([5.0, 10, 30, 60, 120, draft.http?.timeout ?? 30])).sorted(), id: \.self) { Text("\($0.formatted()) \(unit)").tag($0) }
                        }.frame(width: 118)
                    }
                }.font(.caption).controlSize(.small)
                PersistedHSplit(toolID: "http", defaultLeading: 420, minLeading: 270, maxLeading: 900) {
                    requestPane.disabled(draft.busy)
                } trailing: {
                    responsePane
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
            case .curl: CurlImportSheet { imported in draft.apply(imported); requestTab = imported.input.isEmpty ? .params : .body; draft.status = loc("http.status.importedCurl") }.environment(\.appLanguage, language)
            case .library: HTTPRequestLibrary { draft.apply($0) }.environment(store).environment(\.appLanguage, language)
            case .save: SaveHTTPRequestSheet(draft: draft.record).environment(store).environment(\.appLanguage, language)
            }
        }
    }
    private var requestPane: some View {
        VStack(spacing: 10) {
            HStack { Text(AppLocalization.string("http.request", language: language)).font(.headline); Spacer(); Button(AppLocalization.string("http.importCurlShort", language: language)) { sheet = .curl }.controlSize(.small) }
            Picker(loc("http.requestSections"), selection: $requestTab) { ForEach(HTTPRequestTab.allCases) { tab in Text(tab.title(language: language)).tag(tab) } }.pickerStyle(.segmented).labelsHidden()
            Group {
                switch requestTab {
                case .params: HTTPFieldsEditor(fields: option(\.params), descriptionKey: "http.params.description", keyHintKey: "http.params.name")
                case .cookies: HTTPFieldsEditor(fields: option(\.cookies), descriptionKey: "http.cookies.description", keyHintKey: "http.cookies.name")
                case .headers: EditorPane(title: loc("http.headers.editorTitle"), text: $draft.secondary)
                case .body:
                    VStack(spacing: 10) {
                        Picker(loc("http.bodyKindLabel"), selection: option(\.bodyKind)) { ForEach(HTTPBodyKind.allCases, id: \.self) { Text($0.title(language: language)).tag($0) } }
                        if ["GET", "HEAD"].contains(draft.mode) { Text(locf("http.getNoBody", draft.mode)).font(.caption).foregroundStyle(.secondary) }
                        Group {
                            switch draft.http?.bodyKind ?? .raw {
                            case .none: ContentUnavailableView(loc("http.body.none.title"), systemImage: "doc", description: Text(loc("http.body.none.description")))
                            case .form: HTTPFieldsEditor(fields: option(\.form), descriptionKey: "http.form.description", keyHintKey: "http.form.name")
                            case .multipart: HTTPMultipartEditor(parts: option(\.multipart))
                            case .raw, .json: EditorPane(title: loc("http.body.editorTitle"), text: $draft.input, syntax: draft.http?.bodyKind == .json)
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
                Text(AppLocalization.string("http.response", language: language)).font(.headline); Spacer()
                if let result = draft.httpResult {
                    Text("\(result.status)").font(.system(.caption, design: .monospaced).bold()).foregroundStyle(result.status >= 400 ? Color.red : result.status >= 300 ? .orange : .green)
                    Text("\(Int(result.elapsed * 1000)) ms · \(ByteCountFormatter.string(fromByteCount: Int64(result.bytes), countStyle: .file))").font(.caption).foregroundStyle(.secondary)
                }
            }.frame(height: 22)
            HStack(spacing: 8) {
                Picker(loc("http.responseSections"), selection: $responseTab) { ForEach(HTTPResponseTab.allCases) { tab in Text(tab.title(language: language)).tag(tab) } }.pickerStyle(.segmented).labelsHidden()
                if responseTab == .body { Toggle(isOn: $pretty) { Image(systemName: "text.alignleft") }.toggleStyle(.button).help(AppLocalization.string("http.prettyJson", language: language)) }
            }
            EditorPane(title: responseTab.title(language: language), text: .constant(responseText), editable: false, syntax: responseTab == .body)
            if let result = draft.httpResult { Text(result.url).font(.system(size: 10, design: .monospaced)).foregroundStyle(.secondary).lineLimit(1).help(result.url) }
        }.padding(.leading, 7)
    }
    private var responseText: String {
        switch responseTab {
        case .headers: return draft.httpResult?.headers ?? ""
        case .cookies: return draft.httpResult?.cookies ?? ""
        case .body: return pretty && formattedSource == draft.output ? formattedResponse : draft.output
        }
    }
    private func send() {
        guard !draft.busy else { return }
        do {
            let snapshot = draft.record
            let request = try NetworkServices.request(snapshot)
            draft.busy = true; draft.error = nil; draft.status = loc("http.status.requesting")
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
                    if Task.isCancelled { draft.status = loc("http.status.cancelled") } else { draft.error = error.localizedDescription }
                }
            }
        } catch { draft.error = error.localizedDescription }
    }
}

struct HTTPMultipartEditor: View {
    @Binding var parts: [HTTPMultipartPart]
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(loc("http.multipart.description")).font(.caption).foregroundStyle(.secondary)
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach($parts) { $part in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 7) {
                                Toggle(loc("http.enabled"), isOn: $part.enabled).labelsHidden().toggleStyle(.checkbox)
                                TextField(loc("http.multipart.fieldName"), text: $part.name).accessibilityLabel(loc("http.multipart.fieldName"))
                                Picker(loc("http.multipart.type"), selection: $part.isFile) { Text(loc("http.multipart.typeText")).tag(false); Text(loc("http.multipart.typeFile")).tag(true) }.labelsHidden().frame(width: 72)
                                Button { parts.removeAll { $0.id == part.id } } label: { Image(systemName: "minus.circle") }.buttonStyle(.borderless)
                            }
                            if part.isFile {
                                HStack {
                                    Text(part.filePath.isEmpty ? loc("http.multipart.noFile") : (part.filePath as NSString).lastPathComponent).lineLimit(1).foregroundStyle(part.filePath.isEmpty ? .secondary : .primary)
                                    Spacer()
                                    Button(loc("http.multipart.chooseFile")) { chooseFile(for: $part) }
                                }.font(.system(size: 12, design: .monospaced))
                            } else {
                                TextField(loc("http.multipart.textValue"), text: $part.value).textFieldStyle(.roundedBorder).font(.system(size: 12, design: .monospaced))
                            }
                        }.padding(8).background(.quaternary.opacity(0.25), in: RoundedRectangle(cornerRadius: 8))
                    }
                }.padding(1)
            }
            HStack {
                Button { parts.append(HTTPMultipartPart()) } label: { Label(loc("http.multipart.addField"), systemImage: "plus") }.disabled(parts.count >= 1000)
                Spacer()
                Text(locf("http.activeCount", HTTPMultipartBuilder.active(parts).count)).foregroundStyle(.secondary)
            }.font(.caption)
        }.padding(13).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 9)).overlay(RoundedRectangle(cornerRadius: 9).strokeBorder(.quaternary))
    }
    private func chooseFile(for part: Binding<HTTPMultipartPart>) {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            part.wrappedValue.filePath = url.path
            part.wrappedValue.isFile = true
        }
    }
}

struct HTTPFieldsEditor: View {
    @Binding var fields: [HTTPField]
    let descriptionKey: String
    let keyHintKey: String
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(loc(descriptionKey)).font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach($fields) { $field in
                        HStack(spacing: 7) {
                            Toggle(loc("http.enabled"), isOn: $field.enabled).labelsHidden().toggleStyle(.checkbox)
                            TextField(loc(keyHintKey), text: $field.name).accessibilityLabel(loc(keyHintKey))
                            TextField(loc("http.value"), text: $field.value).accessibilityLabel(loc("http.value"))
                            Button { fields.removeAll { $0.id == field.id } } label: { Image(systemName: "minus.circle") }.buttonStyle(.borderless).help(loc("http.deleteRow"))
                        }.textFieldStyle(.roundedBorder).font(.system(size: 12, design: .monospaced))
                    }
                }.padding(1)
            }
            HStack { Button { fields.append(HTTPField()) } label: { Label(loc("http.addRow"), systemImage: "plus") }.disabled(fields.count >= 1000); Spacer(); Text(locf("http.activeCount", HTTPFields.active(fields).count)).foregroundStyle(.secondary) }.font(.caption)
        }.padding(13).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 9)).overlay(RoundedRectangle(cornerRadius: 9).strokeBorder(.quaternary))
    }
}

private struct CurlImportSheet: View {
    let onImport: (DraftRecord) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    @State private var command = ""
    @State private var error: String?
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(loc("http.importCurl")).font(.title2.bold())
            Text(loc("http.curlImport.help")).foregroundStyle(.secondary)
            EditorPane(title: "cURL", text: $command)
            if let error { Text(error).font(.caption).foregroundStyle(.red).textSelection(.enabled) }
            HStack { Button(loc("common.cancel")) { dismiss() }.keyboardShortcut(.cancelAction); Spacer(); Button(loc("tool.import")) { do { let value = try CurlCommand.parse(command); onImport(value); dismiss() } catch { self.error = error.localizedDescription } }.buttonStyle(.borderedProminent).keyboardShortcut(.defaultAction).disabled(command.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
        }.padding(24).frame(width: 660, height: 460)
    }
}

private struct SaveHTTPRequestSheet: View {
    let draft: DraftRecord
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    @State private var name = ""
    @State private var collection = ""
    @State private var replaceID: UUID?
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private var existing: SavedHTTPRequest? { store.httpRequests.first { $0.name == name.trimmingCharacters(in: .whitespacesAndNewlines) && $0.collection == normalizedCollection } }
    private var normalizedCollection: String { collection.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? loc("http.collection.default") : collection.trimmingCharacters(in: .whitespacesAndNewlines) }
    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(loc("http.save.title")).font(.title2.bold())
            TextField(loc("http.save.name"), text: $name)
            HStack { TextField(loc("http.save.collection"), text: $collection); Menu { ForEach(Array(Set(store.httpRequests.map(\.collection))).sorted(), id: \.self) { value in Button(value) { collection = value } } } label: { Image(systemName: "folder") } }
            Text(loc("http.save.hint")).font(.caption).foregroundStyle(.secondary)
            HStack { Button(loc("common.cancel")) { dismiss() }.keyboardShortcut(.cancelAction); Spacer(); Button(existing == nil ? loc("tool.save") : loc("http.save.replaceButton")) { if let existing { replaceID = existing.id } else { save() } }.buttonStyle(.borderedProminent).keyboardShortcut(.defaultAction).disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
        }.textFieldStyle(.roundedBorder).padding(24).frame(width: 420)
        .onAppear {
            if collection.isEmpty { collection = loc("http.collection.default") }
            if name.isEmpty { name = URL(string: draft.option)?.host ?? loc("http.untitled") }
        }
        .confirmationDialog(loc("http.replace.confirm"), isPresented: Binding(get: { replaceID != nil }, set: { if !$0 { replaceID = nil } }), presenting: replaceID) { id in Button(loc("common.replace"), role: .destructive) { save(replacing: id) } }
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
    @Environment(\.appLanguage) private var language
    @State private var search = ""
    @State private var deleting: UUID?
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private var matches: [SavedHTTPRequest] { store.httpRequests.filter { search.isEmpty || [$0.name, $0.collection, $0.draft.option].contains { $0.localizedCaseInsensitiveContains(search) } } }
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack { Text(loc("http.collection")).font(.title2.bold()); Spacer(); Button(loc("common.done")) { dismiss() }.keyboardShortcut(.cancelAction) }
            TextField(loc("http.library.search"), text: $search).textFieldStyle(.roundedBorder)
            List {
                ForEach(Array(Set(matches.map(\.collection))).sorted(), id: \.self) { collection in
                    Section(collection) {
                        ForEach(matches.filter { $0.collection == collection }.sorted { $0.modified > $1.modified }) { item in
                            HStack(spacing: 12) {
                                Text(item.draft.mode.isEmpty ? "GET" : item.draft.mode).font(.system(.caption, design: .monospaced).bold()).foregroundStyle(.tint).frame(width: 54)
                                VStack(alignment: .leading, spacing: 4) { Text(item.name).fontWeight(.medium); Text(item.draft.option).font(.caption).foregroundStyle(.secondary).lineLimit(1) }
                                Spacer()
                                Button(loc("http.library.open")) { onOpen(item.draft); dismiss() }
                                Button { deleting = item.id } label: { Image(systemName: "trash") }.help(loc("http.library.deleteHelp"))
                            }.padding(.vertical, 5)
                        }
                    }
                }
            }.overlay { if matches.isEmpty { ContentUnavailableView(loc("http.library.emptyTitle"), systemImage: "folder", description: Text(search.isEmpty ? loc("http.library.empty") : loc("http.library.emptySearch"))) } }
        }.padding(22).frame(width: 700, height: 480)
        .confirmationDialog(loc("http.library.deleteConfirm"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), presenting: deleting) { id in Button(loc("common.delete"), role: .destructive) { store.httpRequests.removeAll { $0.id == id }; store.saveNow(); deleting = nil } }
    }
}
