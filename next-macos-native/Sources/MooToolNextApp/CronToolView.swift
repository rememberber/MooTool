import SwiftUI
import MooToolNextCore

struct CronToolView: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var fields = CronFieldDraft()
    @State private var favoritesOpen = false
    private let zones = Array(Set([TimeZone.current.identifier, "UTC", "Asia/Shanghai", "Asia/Tokyo", "Europe/London", "America/New_York"])).sorted()
    var body: some View {
        ToolPage(tool: Catalog.tool("cron"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.parse", language: language), symbol: "play.fill") { parse() }
            Menu("常用表达式") {
                ForEach(CronFieldDraft.presets, id: \.1) { preset in
                    Button(preset.0) { apply(preset.1) }
                }
            }
            Picker("时区", selection: Binding(get: { draft.option.isEmpty ? TimeZone.current.identifier : draft.option }, set: { draft.option = $0 })) {
                ForEach(zones, id: \.self) { Text($0) }
            }.frame(width: 220)
            Button(AppLocalization.string("tool.example", language: language)) { apply("0 */15 * * * ?") }
            Button(AppLocalization.string("tool.favorites", language: language), systemImage: "star") { favoritesOpen = true }
            Spacer(minLength: 0)
            Button { draft.input = ""; draft.output = ""; draft.error = nil } label: { Image(systemName: "trash") }.help(AppLocalization.string("tool.clear", language: language))
        } content: {
            PersistedHSplit(toolID: "cron", defaultLeading: 320, minLeading: 260, maxLeading: 520) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("表达式构建").font(.headline)
                    grid("秒", $fields.second)
                    grid("分", $fields.minute)
                    grid("时", $fields.hour)
                    grid("日", $fields.day)
                    grid("月", $fields.month)
                    grid("周", $fields.week)
                    grid("年（可选）", $fields.year)
                    Text("与 Electron 相同：六段 Quartz 使用 ? 分隔「日」与「周」。").font(.caption).foregroundStyle(.secondary)
                }.padding()
            } trailing: {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Cron 表达式").font(.headline)
                    TextField("0 0 9 ? * MON-FRI", text: $draft.input).textFieldStyle(.roundedBorder).font(.system(.body, design: .monospaced))
                        .onChange(of: draft.input) { _, value in syncFields(from: value) }
                    if let description = CronExpression.describe(draft.input) {
                        Text("说明：\(description)").font(.callout).foregroundStyle(.secondary)
                    }
                    EditorPane(title: "下 10 次执行时间", text: $draft.output, editable: false)
                }.padding()
            }
        }.onAppear {
            if draft.option.isEmpty { draft.option = TimeZone.current.identifier }
            if draft.input.isEmpty { apply("0 */15 * * * ?") } else { syncFields(from: draft.input) }
        }
        .sheet(isPresented: $favoritesOpen) {
            ToolFavoritesSheet(kind: .cron, currentValue: draft.input) { expression in
                apply(expression)
            }.environment(store)
        }
    }
    private func grid(_ title: String, _ value: Binding<String>) -> some View {
        HStack {
            Text(title).frame(width: 72, alignment: .leading).foregroundStyle(.secondary)
            TextField(title, text: value).textFieldStyle(.roundedBorder).font(.system(.body, design: .monospaced))
                .onChange(of: value.wrappedValue) { _, _ in rebuildExpression() }
        }
    }
    private func rebuildExpression() {
        if let expression = try? fields.build() { draft.input = expression }
    }
    private func syncFields(from expression: String) {
        if let next = try? CronFieldDraft.split(expression) { fields = next }
    }
    private func apply(_ expression: String) {
        draft.input = expression
        syncFields(from: expression)
        parse()
    }
    private func parse() {
        store.run("cron") { d in
            let zone = TimeZone(identifier: d.option) ?? .current
            let runs = try CronExpression(d.input).next(after: Date(), count: 10, timeZone: zone)
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "zh_CN")
            formatter.timeZone = zone
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss ZZZZ"
            let lines = runs.enumerated().map { "\($0.offset + 1).  \(formatter.string(from: $0.element))" }.joined(separator: "\n")
            if let description = CronExpression.describe(d.input) {
                return "说明：\(description)\n\n\(lines)"
            }
            return lines
        }
    }
}
