import SwiftUI
import MooToolNextCore

struct CronToolView: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var fields = CronFieldDraft()
    @State private var favoritesOpen = false
    private let zones = Array(Set([TimeZone.current.identifier, "UTC", "Asia/Shanghai", "Asia/Tokyo", "Europe/London", "America/New_York"])).sorted()
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        ToolPage(tool: Catalog.tool("cron"), draft: draft) {
            PrimaryButton(title: loc("cron.parse"), symbol: "play.fill") { parse() }
            Menu(loc("cron.preset")) {
                ForEach(CronFieldDraft.presets) { preset in
                    Button { apply(preset.expression) } label: {
                        Text(loc("cron.preset.\(preset.id)"))
                    }
                }
            }
            Picker(loc("cron.timezone"), selection: Binding(get: { draft.option.isEmpty ? TimeZone.current.identifier : draft.option }, set: { draft.option = $0 })) {
                ForEach(zones, id: \.self) { Text($0) }
            }.frame(width: 220)
            Button(AppLocalization.string("tool.example", language: language)) { apply("0 */15 * * * ?") }
            Button(AppLocalization.string("tool.favorites", language: language), systemImage: "star") { favoritesOpen = true }
            Spacer(minLength: 0)
            Button { draft.input = ""; draft.output = ""; draft.error = nil } label: { Image(systemName: "trash") }.help(AppLocalization.string("tool.clear", language: language))
        } content: {
            PersistedHSplit(toolID: "cron", defaultLeading: 320, minLeading: 260, maxLeading: 520) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(loc("cron.builder")).font(.headline)
                    grid(loc("cron.second"), $fields.second)
                    grid(loc("cron.minute"), $fields.minute)
                    grid(loc("cron.hour"), $fields.hour)
                    grid(loc("cron.day"), $fields.day)
                    grid(loc("cron.month"), $fields.month)
                    grid(loc("cron.week"), $fields.week)
                    grid(loc("cron.year"), $fields.year)
                    Text(loc("cron.builderHint")).font(.caption).foregroundStyle(.secondary)
                }.padding()
            } trailing: {
                VStack(alignment: .leading, spacing: 12) {
                    Text(loc("cron.expression")).font(.headline)
                    TextField("0 0 9 ? * MON-FRI", text: $draft.input).textFieldStyle(.roundedBorder).font(.system(.body, design: .monospaced))
                        .onChange(of: draft.input) { _, value in syncFields(from: value) }
                    if let description = CronExpression.describe(draft.input) {
                        Text(humanSummary(description)).font(.callout).foregroundStyle(.secondary)
                    }
                    EditorPane(title: loc("cron.nextRuns"), text: $draft.output, editable: false)
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
    private func humanSummary(_ description: String) -> String {
        let sep = language == .enUS ? ": " : "："
        return loc("cron.humanReadable") + sep + description
    }
    private func parse() {
        let lang = language
        store.run("cron") { d in
            let zone = TimeZone(identifier: d.option) ?? .current
            let runs = try CronExpression(d.input).next(after: Date(), count: 10, timeZone: zone)
            let formatter = DateFormatter()
            formatter.locale = lang.locale
            formatter.timeZone = zone
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss ZZZZ"
            let lines = runs.enumerated().map { "\($0.offset + 1).  \(formatter.string(from: $0.element))" }.joined(separator: "\n")
            if let description = CronExpression.describe(d.input) {
                let sep = lang == .enUS ? ": " : "："
                let summary = AppLocalization.string("cron.humanReadable", language: lang) + sep + description
                return "\(summary)\n\n\(lines)"
            }
            return lines
        }
    }
}
