import SwiftUI
import Translation
import MooToolNextCore

struct MessageBoardTool: View {
    @Bindable var draft: ToolDraft
    @Environment(\.appLanguage) private var language
    @State private var presenter = MessageBoardFullscreenPresenter()
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
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
            Menu(loc("messageBoard.presets")) {
                ForEach(MessageBoardThemes.presets) { preset in
                    Button {
                        draft.input = loc("messageBoard.preset.\(preset.id)")
                        board.wrappedValue.backgroundHex = preset.backgroundHex
                        board.wrappedValue.foregroundHex = preset.foregroundHex
                    } label: {
                        Text(loc("messageBoard.preset.\(preset.id)"))
                    }
                }
            }
            Picker(loc("messageBoard.alignment"), selection: Binding(get: { board.wrappedValue.alignment }, set: { board.wrappedValue.alignment = $0 })) {
                Text(loc("messageBoard.alignCenter")).tag("center")
                Text(loc("messageBoard.alignLeft")).tag("left")
            }.frame(width: 140)
            ColorPicker(loc("messageBoard.background"), selection: Binding(
                get: { background },
                set: { board.wrappedValue.backgroundHex = MessageBoardColorCoding.hex($0) }))
                .frame(width: 90)
            ColorPicker(loc("messageBoard.foreground"), selection: Binding(
                get: { foreground },
                set: { board.wrappedValue.foregroundHex = MessageBoardColorCoding.hex($0) }))
                .frame(width: 90)
            Slider(value: Binding(get: { board.wrappedValue.fontSize }, set: { board.wrappedValue.fontSize = $0 }), in: 28...160)
                .frame(width: 130)
            PrimaryButton(title: AppLocalization.string("tool.fullscreen", language: language), symbol: "arrow.up.left.and.arrow.down.right") {
                presenter.present(
                    text: draft.input,
                    size: board.wrappedValue.fontSize,
                    background: background,
                    foreground: foreground,
                    alignment: textAlignment,
                    windowTitle: loc("messageBoard.windowTitle"))
            }
        } content: {
            VStack(spacing: 14) {
                TextField(loc("messageBoard.placeholder"), text: $draft.input).textFieldStyle(.roundedBorder).font(.title3)
                BoardDisplay(text: draft.input, size: board.wrappedValue.fontSize, background: background, foreground: foreground, alignment: textAlignment)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                Text(loc("messageBoard.hint")).font(.caption).foregroundStyle(.secondary)
            }
        }
        .onAppear {
            if draft.messageBoard == nil { draft.messageBoard = MessageBoardOptions() }
            if draft.input.isEmpty { draft.input = loc("messageBoard.preset.away") }
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
    @Environment(\.appLanguage) private var language
    @State private var tab = "translate"
    var body: some View {
        VStack(spacing: 0) {
            Picker(AppLocalization.string("translation.viewPicker", language: language), selection: $tab) {
                Text(AppLocalization.string("translation.tab.translate", language: language)).tag("translate")
                Text(AppLocalization.string("translation.tab.words", language: language)).tag("words")
                Text(AppLocalization.string("translation.tab.history", language: language)).tag("history")
            }.pickerStyle(.segmented).padding(.horizontal, 16).padding(.top, 8)
            Group {
                switch tab {
                case "words": TranslationWordBook(draft: draft)
                case "history": TranslationHistoryPane(draft: draft)
                default:
                    if #available(macOS 15, *) { ModernTranslationTool(draft: draft) }
                    else { LegacyTranslationTool(draft: draft) }
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

private struct LegacyTranslationTool: View {
    @Bindable var draft: ToolDraft
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        ToolPage(tool: Catalog.tool("translation"), draft: draft) {
            Button(loc("translation.systemDictionary")) { openDictionary(draft.input) }
        } content: {
            VStack(alignment: .leading, spacing: 15) {
                Text(loc("translation.legacyHint")).font(.callout).foregroundStyle(.secondary)
                EditorPane(title: loc("translation.source"), text: $draft.input)
            }
        }
    }
}
@available(macOS 15, *)
private struct ModernTranslationTool: View {
    @Bindable var draft: ToolDraft
    @State private var configuration: TranslationSession.Configuration?
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        ToolPage(tool: Catalog.tool("translation"), draft: draft) {
            Picker(loc("translation.targetLanguage"), selection: $draft.mode) {
                Text(loc("translation.lang.zhHans")).tag("zh-Hans")
                Text(loc("translation.lang.en")).tag("en")
                Text(loc("translation.lang.ja")).tag("ja")
                Text(loc("translation.lang.ko")).tag("ko")
                Text(loc("translation.lang.fr")).tag("fr")
                Text(loc("translation.lang.de")).tag("de")
            }.frame(width: 200)
            PrimaryButton(title: AppLocalization.string("tool.translate", language: language), symbol: "character.bubble") {
                guard !draft.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { draft.error = loc("translation.error.emptyInput"); return }
                draft.error = nil
                if configuration?.target == Locale.Language(identifier: draft.mode) { configuration?.invalidate() }
                else { configuration = .init(source: nil, target: Locale.Language(identifier: draft.mode)) }
            }
            Button(AppLocalization.string("translation.systemDictionary", language: language)) { openDictionary(draft.input) }
        } content: {
            PersistedHSplit(toolID: "translation", defaultLeading: 360, minLeading: 240, maxLeading: 720) {
                EditorPane(title: AppLocalization.string("translation.sourceAuto", language: language), text: $draft.input)
            } trailing: {
                EditorPane(title: AppLocalization.string("translation.target", language: language), text: $draft.output, editable: false)
            }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "zh-Hans" } }
            .translationTask(configuration) { session in
                do {
                    let response = try await session.translate(draft.input)
                    draft.output = response.targetText
                    if draft.option.isEmpty { draft.option = "auto" }
                    store.record("translation")
                } catch { draft.error = error.localizedDescription }
            }
    }
}

private func openDictionary(_ query: String) {
    let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return }
    NSWorkspace.shared.open(URL(string: "dict://\(trimmed.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? trimmed)")!)
}
