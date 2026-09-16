import SwiftUI
import UniformTypeIdentifiers
import MooToolNextCore

struct ReformatWorkspace: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var textEditor = NativeEditorBridge()
    @State private var fileEditor = NativeEditorBridge()
    @State private var historyOpen = false
    @State private var operationID: UUID?
    @State private var pickerID: UUID?

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }

    private var options: ReformatOptions { draft.reformat ?? ReformatOptions.migrating(draft.record) }
    private var typeSelection: Binding<ReformatType> {
        Binding(get: { options.type }, set: { type in
            let previous = options.type
            var next = options; next.type = type; next.fileResult = ""; draft.reformat = next
            if next.tab == .text, (draft.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || draft.input == previous.sample) {
                draft.input = type.sample
            }
            draft.error = nil
        })
    }
    private func option<Value>(_ key: WritableKeyPath<ReformatOptions, Value>) -> Binding<Value> {
        Binding(get: { options[keyPath: key] }, set: { value in
            var next = options; next[keyPath: key] = value
            if key == \.type || key == \.indent { next.fileResult = "" }
            draft.reformat = next; draft.error = nil
        })
    }
    private var fileSource: Binding<String> {
        Binding(get: { options.fileSource }, set: { value in
            var next = options; next.fileSource = value; next.fileResult = ""; draft.reformat = next; draft.error = nil
        })
    }
    private var chosenText: String { options.tab == .text ? draft.input : options.fileResult }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(Catalog.localizedTool("reformat", language: language).title).font(.system(size: 23, weight: .semibold))
                Spacer()
                Button { historyOpen = true } label: {
                    Label(AppLocalization.string("workbench.history", language: language), systemImage: "clock.arrow.circlepath")
                }.buttonStyle(.borderless)
            }.padding(.horizontal, 18).frame(height: 52)
            Divider()
            HStack {
                Picker(loc("reformat.mode"), selection: option(\.tab)) {
                    Text(loc("reformat.tab.text")).tag(ReformatTab.text)
                    Text(loc("reformat.tab.file")).tag(ReformatTab.file)
                }.pickerStyle(.segmented).labelsHidden().frame(width: 240)
                Spacer()
            }.padding(.horizontal, 18).frame(height: 48)
            Divider()
            toolbar
            Divider()
            if options.tab == .text {
                CodeEditor(text: $draft.input, syntax: options.type != .nginx, persistence: store.editorPersistence("reformat"), bridge: textEditor,
                           softWrap: true, language: options.type.editorSyntax)
                    .accessibilityIdentifier("reformat.text.editor")
            } else {
                fileWorkspace
            }
            Divider()
            statusBar
        }
        .background(Color(nsColor: .textBackgroundColor))
        .onAppear {
            if draft.reformat == nil {
                let fresh = draft.mode.isEmpty && draft.input.isEmpty
                draft.reformat = ReformatOptions.migrating(draft.record)
                if fresh { draft.input = ReformatType.nginx.sample }
            }
        }
        .onDisappear { pickerID = nil; cancelOperation() }
        .sheet(isPresented: $historyOpen) {
            HistoryView(toolID: "reformat").environment(store).environment(\.appLanguage, language)
        }
    }

    private var toolbar: some View {
        HStack(spacing: 8) {
            primaryControls
            Spacer(minLength: 0)
            secondaryControls
        }.padding(.horizontal, 16).frame(height: 54)
            .background(Color(nsColor: .windowBackgroundColor))
    }
    private var primaryControls: some View {
        Group {
            Picker(loc("reformat.type"), selection: typeSelection) {
                ForEach(ReformatType.allCases, id: \.self) { type in Text(type.title).tag(type) }
            }.frame(width: 126).accessibilityIdentifier("reformat.type")
            Picker(loc("reformat.indent"), selection: option(\.indent)) {
                ForEach(2...6, id: \.self) { count in Text(locf("reformat.indentSpaces", count)).tag(count) }
            }.frame(width: 112).accessibilityIdentifier("reformat.indent")
            Button { format() } label: { Label(loc("tool.format"), systemImage: "paintbrush") }
                .buttonStyle(.borderedProminent).disabled(draft.busy)
                .keyboardShortcut("f", modifiers: [.command, .shift])
                .accessibilityIdentifier("reformat.format")
                .reformatAcceptanceControl("格式化")
            if draft.busy { ProgressView().controlSize(.small).accessibilityLabel(loc("reformat.formatting")) }
        }
    }
    private var secondaryControls: some View {
        Group {
            Button(action: copy) { Image(systemName: "doc.on.doc") }.help(loc("reformat.copyHelp"))
            Button(action: export) { Image(systemName: "square.and.arrow.up") }.help(loc("reformat.exportHelp")).disabled(chosenText.isEmpty)
            Button(action: clear) { Image(systemName: "trash") }.help(loc("reformat.clearHelp"))
        }.buttonStyle(.borderless)
    }

    private var fileWorkspace: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                HStack(spacing: 10) {
                    Button { openFile() } label: { Label(loc("reformat.openFile"), systemImage: "folder") }
                        .accessibilityIdentifier("reformat.openFile")
                    Text(options.fileName.isEmpty ? loc("reformat.fileHint") : options.fileName)
                        .font(.system(size: 12)).foregroundStyle(.secondary).lineLimit(1)
                    Spacer(minLength: 0)
                    if !options.fileResult.isEmpty { Button(loc("reformat.exportResult")) { export() } }
                }.padding(.horizontal, 18).frame(height: 48)
                Divider()
                Group {
                    if geometry.size.width >= 630 {
                        PersistedHSplit(toolID: "reformat", defaultLeading: 400, minLeading: 260, maxLeading: 900) {
                            filePane(title: loc("reformat.fileOriginal"), result: false)
                        } trailing: {
                            filePane(title: loc("reformat.fileResult"), result: true)
                        }
                    } else {
                        VSplitView {
                            filePane(title: loc("reformat.fileOriginal"), result: false).frame(minHeight: 150)
                            filePane(title: loc("reformat.fileResult"), result: true).frame(minHeight: 150)
                        }
                    }
                }
            }
        }
    }
    private func filePane(title: String, result: Bool) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(title).font(.system(size: 12, weight: .semibold))
                Spacer()
                Text(locf("textDiff.charCount", (result ? options.fileResult : options.fileSource).count))
                    .font(.system(size: 10, design: .monospaced)).foregroundStyle(.tertiary)
            }.padding(.horizontal, 14).frame(height: 35).background(.quaternary.opacity(0.2))
            Divider()
            CodeEditor(text: result ? option(\.fileResult) : fileSource, editable: !result,
                       syntax: options.type != .nginx,
                       persistence: filePersistence(result: result), bridge: result ? nil : fileEditor,
                       softWrap: true, language: options.type.editorSyntax)
                .accessibilityIdentifier(result ? "reformat.file.result" : "reformat.file.source")
        }
    }
    private func filePersistence(result: Bool) -> EditorPersistence {
        let current = options, generation = store.editorRestoreGeneration
        return EditorPersistence(identity: "reformat:file:\(current.fileIdentity):\(draft.editorRevision):\(result)",
                                 state: result ? current.resultEditor : current.fileEditor) { value in
            guard store.editorRestoreGeneration == generation, draft.reformat?.fileIdentity == current.fileIdentity else { return }
            var next = draft.reformat ?? current
            if result { next.resultEditor = value } else { next.fileEditor = value }
            if next != draft.reformat { draft.reformat = next }
        }
    }
    private var statusBar: some View {
        HStack(spacing: 7) {
            Image(systemName: draft.error == nil ? "checkmark.circle" : "exclamationmark.circle")
                .foregroundStyle(draft.error == nil ? Color.secondary : .red)
            Text(draft.error ?? (draft.status.isEmpty ? loc("reformat.status.ready") : draft.status))
                .foregroundStyle(draft.error == nil ? Color.secondary : .red).lineLimit(2).textSelection(.enabled)
            Spacer()
            Text(options.type.title).foregroundStyle(.secondary)
            Text("·").foregroundStyle(.tertiary)
            Text(locf("textDiff.charCount", chosenText.count)).foregroundStyle(.secondary)
        }.font(.system(size: 11)).padding(.horizontal, 16).frame(height: 30)
            .background(Color(nsColor: .windowBackgroundColor))
    }

    private func format() {
        guard !draft.busy else { return }
        let current = options, source = current.tab == .text ? draft.input : current.fileSource
        guard !source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { draft.error = loc("reformat.error.empty"); return }
        let generation = store.editorRestoreGeneration, input = draft.input, id = UUID()
        draft.error = nil; draft.busy = true; draft.reformatOperationID = id; operationID = id
        draft.reformatTask = Task {
            defer {
                if draft.reformatOperationID == id { draft.busy = false; draft.reformatTask = nil; draft.reformatOperationID = nil }
                if operationID == id { operationID = nil }
            }
            do {
                let result = try await ReformatEngine.format(source, type: current.type, indent: current.indent)
                try Task.checkCancellation()
                guard store.editorRestoreGeneration == generation, draft.reformatOperationID == id,
                      draft.input == input, options.tab == current.tab, options.type == current.type,
                      options.indent == current.indent, options.fileIdentity == current.fileIdentity,
                      options.fileSource == current.fileSource else { return }
                var history = draft.record; history.output = result
                if current.tab == .text {
                    if !textEditor.replace(result, expected: source, action: "格式化"), draft.input == source { draft.input = result }
                } else {
                    var next = options; next.fileResult = result; draft.reformat = next
                    history.reformat = next
                }
                draft.status = locf("reformat.status.done", result.count)
                store.record("reformat", snapshot: history)
            } catch { if !Task.isCancelled, draft.reformatOperationID == id { draft.error = error.localizedDescription } }
        }
    }
    private func cancelOperation() {
        draft.reformatTask?.cancel()
        if draft.reformatOperationID == operationID { draft.reformatTask = nil; draft.reformatOperationID = nil; draft.busy = false }
        operationID = nil
    }
    private func openFile() {
        let generation = store.editorRestoreGeneration
        let request = UUID(); pickerID = request
        FilePanels.open(types: [.text, .data]) { urls in
            guard pickerID == request, store.editorRestoreGeneration == generation, let url = urls.first else { return }
            pickerID = nil
            do {
                let source = try ReformatEngine.readFile(url)
                var next = options; next.tab = .file; next.fileName = url.lastPathComponent
                next.fileSource = source; next.fileResult = ""; next.fileIdentity = UUID()
                next.fileEditor = EditorViewState(); next.resultEditor = EditorViewState()
                draft.reformat = next; draft.error = nil; draft.status = locf("reformat.status.opened", url.lastPathComponent)
            } catch { draft.error = error.localizedDescription }
        }
    }
    private func copy() { FilePanels.copy(chosenText); draft.status = loc("reformat.status.copied") }
    private func export() { guard !chosenText.isEmpty else { return }; FilePanels.saveText(chosenText, name: options.exportName) }
    private func clear() {
        if options.tab == .text {
            if !textEditor.replace("", expected: draft.input, action: "清空") { draft.input = "" }
        } else {
            var next = options; next.fileSource = ""; next.fileResult = ""; next.fileName = ""
            next.fileIdentity = UUID(); next.fileEditor = EditorViewState(); next.resultEditor = EditorViewState()
            draft.reformat = next
        }
        draft.error = nil; draft.status = loc("reformat.status.cleared")
    }
}
