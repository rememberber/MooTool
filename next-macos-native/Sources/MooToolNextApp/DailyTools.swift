import SwiftUI
import Translation
import MooToolNextCore

struct MessageBoardTool: View {
    @Bindable var draft: ToolDraft
    @State private var presenter = MessageBoardFullscreenPresenter()
    private var board: Binding<MessageBoardOptions> {
        Binding(
            get: {
                if draft.messageBoard == nil { draft.messageBoard = MessageBoardOptions() }
                return draft.messageBoard!
            },
            set: { draft.messageBoard = $0 })
    }
    var body: some View {
        let options = board.wrappedValue
        let background = MessageBoardColorCoding.color(hex: options.backgroundHex) ?? .yellow
        let foreground = MessageBoardColorCoding.color(hex: options.foregroundHex) ?? .black
        let textAlignment: TextAlignment = options.alignment == "left" ? .leading : .center
        ToolPage(tool: Catalog.tool("messageBoard"), draft: draft) {
            Menu("常用留言") {
                ForEach(MessageBoardThemes.presets, id: \.title) { preset in
                    Button(preset.title) {
                        draft.input = preset.message
                        board.wrappedValue.backgroundHex = preset.backgroundHex
                        board.wrappedValue.foregroundHex = preset.foregroundHex
                    }
                }
            }
            Picker("对齐", selection: Binding(get: { board.wrappedValue.alignment }, set: { board.wrappedValue.alignment = $0 })) {
                Text("居中").tag("center")
                Text("左对齐").tag("left")
            }.frame(width: 140)
            ColorPicker("背景", selection: Binding(
                get: { background },
                set: { board.wrappedValue.backgroundHex = MessageBoardColorCoding.hex($0) }))
                .frame(width: 90)
            ColorPicker("文字", selection: Binding(
                get: { foreground },
                set: { board.wrappedValue.foregroundHex = MessageBoardColorCoding.hex($0) }))
                .frame(width: 90)
            Slider(value: Binding(get: { board.wrappedValue.fontSize }, set: { board.wrappedValue.fontSize = $0 }), in: 28...160)
                .frame(width: 130)
            PrimaryButton(title: "全屏展示", symbol: "arrow.up.left.and.arrow.down.right") {
                presenter.present(
                    text: draft.input,
                    size: board.wrappedValue.fontSize,
                    background: background,
                    foreground: foreground,
                    alignment: textAlignment)
            }
        } content: {
            VStack(spacing: 14) {
                TextField("写下你的留言", text: $draft.input).textFieldStyle(.roundedBorder).font(.title3)
                BoardDisplay(text: draft.input, size: board.wrappedValue.fontSize, background: background, foreground: foreground, alignment: textAlignment)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                Text("全屏展示时按 Esc 退出；展示期间会阻止显示器休眠。").font(.caption).foregroundStyle(.secondary)
            }
        }
        .onAppear {
            if draft.messageBoard == nil { draft.messageBoard = MessageBoardOptions() }
            if draft.input.isEmpty { draft.input = "马上回来" }
        }
        .onDisappear { presenter.dismiss() }
    }
}

struct BoardDisplay: View {
    let text: String
    let size: Double
    let background: Color
    let foreground: Color
    var alignment: TextAlignment = .center
    var body: some View {
        ZStack {
            background
            Text(text)
                .font(.system(size: size, weight: .bold, design: .rounded))
                .foregroundStyle(foreground)
                .multilineTextAlignment(alignment)
                .frame(maxWidth: .infinity, alignment: alignment == .leading ? .leading : .center)
                .minimumScaleFactor(0.15)
                .padding(40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct TranslationTool: View {
    @Bindable var draft: ToolDraft
    var body: some View {
        if #available(macOS 15, *) { ModernTranslationTool(draft: draft) }
        else {
            ToolPage(tool: Catalog.tool("translation"), draft: draft) {
                Button("在系统词典中查找") { openDictionary(draft.input) }
            } content: {
                VStack(alignment: .leading, spacing: 15) {
                    Text("自动翻译需要 macOS 15 或更高版本。当前系统可选中文本，使用右键菜单的翻译/查询服务，或打开系统词典。").font(.callout).foregroundStyle(.secondary)
                    EditorPane(title: "原文", text: $draft.input)
                }
            }
        }
    }
}
@available(macOS 15, *)
private struct ModernTranslationTool: View {
    @Bindable var draft: ToolDraft
    @State private var configuration: TranslationSession.Configuration?
    @Environment(AppStore.self) private var store
    var body: some View {
        ToolPage(tool: Catalog.tool("translation"), draft: draft) {
            Picker("目标语言", selection: $draft.mode) { Text("简体中文").tag("zh-Hans"); Text("English").tag("en"); Text("日本語").tag("ja"); Text("한국어").tag("ko"); Text("Français").tag("fr"); Text("Deutsch").tag("de") }.frame(width: 200)
            PrimaryButton(title: "翻译", symbol: "character.bubble") {
                guard !draft.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { draft.error = "请输入需要翻译的文本。"; return }
                draft.error = nil
                if configuration?.target == Locale.Language(identifier: draft.mode) { configuration?.invalidate() }
                else { configuration = .init(source: nil, target: Locale.Language(identifier: draft.mode)) }
            }
            Button("系统词典") { openDictionary(draft.input) }
        } content: {
            PersistedHSplit(toolID: "translation", defaultLeading: 360, minLeading: 240, maxLeading: 720) {
                EditorPane(title: "原文 · 自动识别语言", text: $draft.input)
            } trailing: {
                EditorPane(title: "译文", text: $draft.output, editable: false)
            }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "zh-Hans" } }
            .translationTask(configuration) { session in
                do {
                    let response = try await session.translate(draft.input)
                    draft.output = response.targetText
                } catch { draft.error = error.localizedDescription }
            }
    }
}

private func openDictionary(_ query: String) {
    let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return }
    NSWorkspace.shared.open(URL(string: "dict://\(trimmed.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? trimmed)")!)
}
