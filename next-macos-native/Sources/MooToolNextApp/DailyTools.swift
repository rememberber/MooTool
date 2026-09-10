import SwiftUI
import Translation
import MooToolNextCore

struct MessageBoardTool: View {
    @Bindable var draft: ToolDraft
    @State private var fontSize = 72.0
    @State private var background = Color(red: 0.97, green: 0.80, blue: 0.29)
    @State private var foreground = Color.black
    @State private var board: NSWindow?
    var body: some View {
        ToolPage(tool: Catalog.tool("messageBoard"), draft: draft) {
            Menu("常用留言") { ForEach(["马上回来", "请勿打扰", "正在开会", "欢迎光临", "今天也要开心"], id: \.self) { text in Button(text) { draft.input = text } } }
            ColorPicker("背景", selection: $background).frame(width: 90)
            ColorPicker("文字", selection: $foreground).frame(width: 90)
            Slider(value: $fontSize, in: 28...160).frame(width: 130)
            PrimaryButton(title: "全屏展示", symbol: "arrow.up.left.and.arrow.down.right") {
                let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 1100, height: 720), styleMask: [.titled, .closable, .resizable, .miniaturizable], backing: .buffered, defer: false)
                window.title = "MooTool Native · 留言板"; window.isReleasedWhenClosed = false
                window.contentView = NSHostingView(rootView: BoardDisplay(text: draft.input, size: fontSize, background: background, foreground: foreground))
                window.center(); window.makeKeyAndOrderFront(nil); window.toggleFullScreen(nil); board = window
            }
        } content: {
            VStack(spacing: 14) {
                TextField("写下你的留言", text: $draft.input).textFieldStyle(.roundedBorder).font(.title3)
                BoardDisplay(text: draft.input, size: fontSize, background: background, foreground: foreground).clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }.onAppear { if draft.input.isEmpty { draft.input = "马上回来" } }
    }
}
private struct BoardDisplay: View {
    let text: String; let size: Double; let background: Color; let foreground: Color
    var body: some View {
        ZStack { background; Text(text).font(.system(size: size, weight: .bold, design: .rounded)).foregroundStyle(foreground).multilineTextAlignment(.center).minimumScaleFactor(0.15).padding(40) }.frame(maxWidth: .infinity, maxHeight: .infinity)
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
            HSplitView { EditorPane(title: "原文 · 自动识别语言", text: $draft.input); EditorPane(title: "译文", text: $draft.output, editable: false) }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "zh-Hans" } }
            .translationTask(configuration) { session in
                draft.busy = true
                defer { draft.busy = false }
                do { let response = try await session.translate(draft.input); draft.output = response.targetText; draft.status = "已通过 macOS 翻译"; store.record("translation") }
                catch { draft.error = error.localizedDescription }
            }
    }
}
private func openDictionary(_ text: String) {
    let encoded = text.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? ""
    if let url = URL(string: "dict://" + encoded) { NSWorkspace.shared.open(url) }
}
