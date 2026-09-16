import SwiftUI
import MooToolNextCore

struct TextDiffWorkspace: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @State private var leftEditor = NativeEditorBridge()
    @State private var rightEditor = NativeEditorBridge()
    @State private var comparison = TextDiffResult.empty
    @State private var status = "准备比较"
    @State private var navigation = -1
    @State private var historyOpen = false

    private var options: TextDiffOptions { draft.textDiff ?? TextDiffOptions() }
    private func option<Value>(_ key: WritableKeyPath<TextDiffOptions, Value>) -> Binding<Value> {
        Binding(get: { options[keyPath: key] }, set: { value in
            var next = options; next[keyPath: key] = value; draft.textDiff = next
            navigation = -1; status = draft.input.isEmpty && draft.secondary.isEmpty ? "请输入文本" : "比较完成"
        })
    }
    private var comparisonKey: String { draft.input + "\u{0}" + draft.secondary + "\u{0}" + String(options.ignoreWhitespace) }
    private var visibleSegments: [TextDiffSegment] {
        options.highlight == .characters ? comparison.segments.filter { !$0.wholeLine } : comparison.segments
    }
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("文本对比").font(.system(size: 23, weight: .semibold))
                Spacer()
                Button { historyOpen = true } label: { Label("历史记录", systemImage: "clock.arrow.circlepath") }.buttonStyle(.borderless)
            }.padding(.horizontal, 18).frame(height: 54)
            Divider()
            toolbar
            Divider()
            if options.display == .side {
                editors.frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    editors.frame(maxHeight: .infinity)
                    Divider()
                    pane("统一差异", text: .constant(comparison.unified), editable: false, highlights: unifiedHighlights,
                         persistence: editorPersistence(.unified)).frame(maxHeight: .infinity)
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            Divider()
            statusBar
        }
        .background(Color(nsColor: .textBackgroundColor))
        .onAppear {
            if draft.textDiff == nil {
                draft.textDiff = TextDiffOptions()
                if draft.input.isEmpty && draft.secondary.isEmpty {
                    draft.input = "MooTool\nquiet desktop tools\nold line\n"
                    draft.secondary = "MooTool\nquiet desktop toolkit\nnew line\n"
                }
            }
        }
        .task(id: comparisonKey) {
            do { try await Task.sleep(for: .milliseconds(160)); await calculate(recordHistory: false) }
            catch { }
        }
        .sheet(isPresented: $historyOpen) { HistoryView(toolID: "textDiff").environment(store) }
    }
    private var toolbar: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 7) { commands; Divider().frame(height: 19); navigationButtons; Spacer(minLength: 4); choices }
                .fixedSize(horizontal: true, vertical: false).frame(minWidth: 960)
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 7) { commands; Spacer(minLength: 0) }
                HStack(spacing: 10) { navigationButtons; Divider().frame(height: 19); choices; Spacer(minLength: 0) }
            }
        }.frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 16).padding(.vertical, 9)
            .background(Color(nsColor: .windowBackgroundColor))
    }
    private var commands: some View {
        Group {
            Button { Task { await calculate(recordHistory: true) } } label: { Label("比较", systemImage: "play.fill") }
                .buttonStyle(.borderedProminent).keyboardShortcut(.return, modifiers: .command)
                .accessibilityIdentifier("textDiff.compare").diffAcceptanceControl("比较")
            Button { clear() } label: { Label("清空", systemImage: "trash") }.diffAcceptanceControl("清空")
            Button { swapText() } label: { Label("交换", systemImage: "arrow.left.arrow.right") }.diffAcceptanceControl("交换")
            Button { copyDiff() } label: { Label("复制差异", systemImage: "doc.on.doc") }
                .disabled(comparison.unified.isEmpty)
        }
    }
    private var navigationButtons: some View {
        Group {
            Button { navigate(-1) } label: { Label("上一处", systemImage: "chevron.up") }
                .disabled(visibleSegments.isEmpty)
            Button { navigate(1) } label: { Label("下一处", systemImage: "chevron.down") }
                .disabled(visibleSegments.isEmpty).diffAcceptanceControl("下一处")
        }
    }
    private var choices: some View {
        Group {
            Toggle("忽略空白", isOn: option(\.ignoreWhitespace)).toggleStyle(.checkbox)
            Picker("高亮", selection: option(\.highlight)) {
                Text("行与字符").tag(TextDiffHighlight.both)
                Text("仅字符").tag(TextDiffHighlight.characters)
                Text("仅行").tag(TextDiffHighlight.lines)
            }.frame(width: 126)
            Picker("视图", selection: option(\.display)) {
                Text("左右对比").tag(TextDiffDisplay.side)
                Text("统一差异").tag(TextDiffDisplay.unified)
            }.frame(width: 130)
        }
    }
    private var editors: some View {
        PersistedHSplit(toolID: "textDiff", defaultLeading: 420, minLeading: 240, maxLeading: 900) {
            pane("原始文本", text: $draft.input, editable: true, highlights: sideHighlights(left: true),
                 persistence: editorPersistence(.left), bridge: leftEditor)
        } trailing: {
            pane("修改后", text: $draft.secondary, editable: true, highlights: sideHighlights(left: false),
                 persistence: editorPersistence(.right), bridge: rightEditor)
        }
    }
    private func pane(_ title: String, text: Binding<String>, editable: Bool, highlights: [EditorHighlight],
                      persistence: EditorPersistence, bridge: NativeEditorBridge? = nil) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(title).font(.system(size: 12, weight: .semibold))
                Spacer()
                Text("\(text.wrappedValue.count) 字符").font(.system(size: 10, design: .monospaced)).foregroundStyle(.tertiary)
            }.padding(.horizontal, 14).frame(height: 36).background(.quaternary.opacity(0.2))
            Divider()
            CodeEditor(text: text, editable: editable, persistence: persistence, bridge: bridge,
                       softWrap: false, diffHighlights: highlights)
                .accessibilityIdentifier("textDiff." + (editable ? title : "unified"))
        }
    }
    private enum Side { case left, right, unified }
    private func editorPersistence(_ side: Side) -> EditorPersistence {
        let current = options, generation = store.editorRestoreGeneration
        let state = side == .left ? current.leftEditor : side == .right ? current.rightEditor : current.unifiedEditor
        return EditorPersistence(identity: "textDiff:\(side):\(draft.editorRevision)", state: state) { value in
            guard store.editorRestoreGeneration == generation else { return }
            var next = draft.textDiff ?? current
            let old = side == .left ? next.leftEditor : side == .right ? next.rightEditor : next.unifiedEditor
            if old != value {
                if side == .left { next.leftEditor = value }
                else if side == .right { next.rightEditor = value }
                else { next.unifiedEditor = value }
                draft.textDiff = next
            }
            if side != .unified && options.display == .side && (old.scrollX != value.scrollX || old.scrollY != value.scrollY) {
                let target = side == .left ? rightEditor.view : leftEditor.view
                guard let scroll = target?.enclosingScrollView else { return }
                let origin = scroll.contentView.bounds.origin
                if abs(origin.x - value.scrollX) > 1 || abs(origin.y - value.scrollY) > 1 {
                    let point = scroll.contentView.constrainBoundsRect(NSRect(x: value.scrollX, y: value.scrollY,
                                                                             width: scroll.contentView.bounds.width, height: scroll.contentView.bounds.height)).origin
                    scroll.contentView.scroll(to: point); scroll.reflectScrolledClipView(scroll.contentView)
                }
            }
        }
    }
    private func sideHighlights(left: Bool) -> [EditorHighlight] {
        let text = (left ? comparison.left : comparison.right) as NSString
        var values: [EditorHighlight] = []
        for segment in visibleSegments {
            let range = left ? segment.left : segment.right
            guard range.location != NSNotFound, range.length > 0, NSMaxRange(range) <= text.length else { continue }
            if options.highlight != .characters {
                let line = text.lineRange(for: range)
                values.append(EditorHighlight(range: line, color: color(segment.kind, strong: false)))
            }
            if options.highlight != .lines { values.append(EditorHighlight(range: range, color: color(segment.kind, strong: true))) }
        }
        return values
    }
    private var unifiedHighlights: [EditorHighlight] {
        var values = comparison.unifiedLines.map { span in EditorHighlight(range: span.range, color: color(span.kind, strong: false)) }
        if options.highlight != .lines {
            values += comparison.unifiedCharacters.map { span in EditorHighlight(range: span.range, color: color(span.kind, strong: true)) }
        }
        return values
    }
    private func color(_ kind: TextDiffKind, strong: Bool) -> NSColor {
        let base: NSColor = kind == .insert ? .systemGreen : kind == .delete ? .systemRed : .systemOrange
        return base.withAlphaComponent(strong ? 0.34 : 0.16)
    }
    private func color(_ kind: TextDiffSpanKind, strong: Bool) -> NSColor {
        let base: NSColor = kind == .added ? .systemGreen : kind == .removed ? .systemRed : kind == .changed ? .systemOrange : .systemBlue
        return base.withAlphaComponent(strong ? 0.34 : 0.16)
    }
    private var statusBar: some View {
        HStack(spacing: 9) {
            Image(systemName: draft.error == nil ? "checkmark.circle" : "exclamationmark.circle")
                .foregroundStyle(draft.error == nil ? Color.secondary : .red)
            Text(draft.error ?? status).foregroundStyle(draft.error == nil ? Color.secondary : .red).lineLimit(1)
            Spacer()
            Text("新增 \(comparison.added) · 删除 \(comparison.removed) · 修改 \(comparison.changed)")
                .foregroundStyle(.secondary)
        }.font(.system(size: 11)).padding(.horizontal, 16).frame(height: 31)
            .background(Color(nsColor: .windowBackgroundColor))
    }
    private func calculate(recordHistory: Bool) async {
        let left = draft.input, right = draft.secondary, ignore = options.ignoreWhitespace
        do {
            let value = try await Task.detached(priority: .userInitiated) { try TextDiffEngine.compare(left, right, ignoreWhitespace: ignore) }.value
            guard !Task.isCancelled, draft.input == left, draft.secondary == right, options.ignoreWhitespace == ignore else { return }
            comparison = value; draft.output = value.unified; draft.error = nil; navigation = -1
            status = left.isEmpty && right.isEmpty ? "请输入文本" : "比较完成 · \(visibleSegments.count) 处差异"
            if recordHistory && (!left.isEmpty || !right.isEmpty) { store.record("textDiff") }
        } catch { if !Task.isCancelled, draft.input == left, draft.secondary == right { draft.error = error.localizedDescription } }
    }
    private func navigate(_ step: Int) {
        guard !visibleSegments.isEmpty else { return }
        navigation = (navigation + step + visibleSegments.count) % visibleSegments.count
        let segment = visibleSegments[navigation]
        if segment.left.location != NSNotFound { leftEditor.select(NSRange(location: lineStart(comparison.left, segment.left.location), length: 0)) }
        if segment.right.location != NSNotFound { rightEditor.select(NSRange(location: lineStart(comparison.right, segment.right.location), length: 0)) }
        status = "第 \(navigation + 1) / \(visibleSegments.count) 处差异"
    }
    private func lineStart(_ text: String, _ location: Int) -> Int {
        let source = text as NSString; var index = min(location, source.length)
        while index > 0 && source.character(at: index - 1) != 10 { index -= 1 }
        return index
    }
    private func clear() { draft.input = ""; draft.secondary = ""; draft.output = ""; comparison = .empty; navigation = -1; status = "已清空"; draft.error = nil }
    private func swapText() { swap(&draft.input, &draft.secondary); navigation = -1; status = "已交换"; draft.error = nil }
    private func copyDiff() {
        guard !comparison.unified.isEmpty else { status = "暂无可复制的差异"; return }
        FilePanels.copy(comparison.unified); status = "已复制统一差异"
    }
}
