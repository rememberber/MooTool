import SwiftUI
import MooToolNextCore

struct TimeConvertView: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var now = Date()
    @State private var historyOpen = false
    @State private var clockOpen = false
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    private var zone: String { draft.option.isEmpty ? TimeZone.current.identifier : draft.option }
    private var unit: TimeConversion.TimestampUnit { draft.mode == "millisecond" ? .millisecond : .second }
    private var zones: [String] { TimeConversion.timezones(including: TimeZone.current.identifier) }

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }

    var body: some View {
        ToolPage(tool: Catalog.tool("timeConvert"), draft: draft) {
            Button { historyOpen = true } label: { Label(AppLocalization.string("workbench.history", language: language), systemImage: "clock.arrow.circlepath") }
            Button { clockOpen = true } label: { Label(loc("tool.bigClock"), systemImage: "arrow.up.left.and.arrow.down.right") }
            Spacer(minLength: 0)
        } content: {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    currentBand
                    zoneBand
                    converterBand
                }.frame(maxWidth: 720, alignment: .leading).padding(.vertical, 4)
            }
        }
        .onReceive(timer) { now = $0 }
        .onAppear(perform: bootstrap)
        .sheet(isPresented: $historyOpen) { HistoryView(toolID: "timeConvert").environment(store).environment(\.appLanguage, language) }
        .sheet(isPresented: $clockOpen) { TimeClockSheet(zone: zone, now: now) { clockOpen = false } }
    }

    private var currentBand: some View {
        let timestamp = String(Int64(now.timeIntervalSince1970))
        let local = TimeConversion.formatLocalTime(date: now, zone: zone)
        return GroupBox(loc("timeConvert.currentTime")) {
            VStack(alignment: .leading, spacing: 12) {
                timeValueRow(label: loc("timeConvert.timestampSeconds"), value: timestamp)
                timeValueRow(label: locf("timeConvert.localTimeZone", zone), value: local)
                Text(TimeConversion.formatTimezoneLabel(zone: zone, date: now))
                    .font(.caption).foregroundStyle(.secondary)
            }.padding(4)
        }
    }

    private var zoneBand: some View {
        GroupBox(loc("timeConvert.timezone")) {
            VStack(alignment: .leading, spacing: 10) {
                Picker(loc("timeConvert.timezone"), selection: Binding(get: { zone }, set: { draft.option = $0; refreshLocalFromNow() })) {
                    ForEach(zones, id: \.self) { Text(TimeConversion.formatTimezoneLabel(zone: $0, date: now)).tag($0) }
                }
                HStack(spacing: 8) {
                    ForEach(TimeConversion.quickTimezones, id: \.zone) { item in
                        Button(item.label) {
                            draft.option = item.zone
                            refreshLocalFromNow()
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .tint(zone == item.zone ? .accentColor : .secondary)
                    }
                }
            }.padding(4)
        }
    }

    private var converterBand: some View {
        GroupBox(loc("timeConvert.convert")) {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(loc("timeConvert.timestamp")).font(.subheadline).foregroundStyle(.secondary)
                    HStack {
                        TextField("1700000000", text: $draft.input)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(.body, design: .monospaced))
                            .onSubmit(convertToLocal)
                        Picker(loc("timeConvert.unit"), selection: Binding(get: { unit.rawValue }, set: { draft.mode = $0 })) {
                            Text(loc("timeConvert.unitSecond")).tag("second")
                            Text(loc("timeConvert.unitMillisecond")).tag("millisecond")
                        }.frame(width: 120)
                        Button { copy(draft.input) } label: { Image(systemName: "doc.on.doc") }.help(loc("timeConvert.copy"))
                    }
                }
                HStack(spacing: 12) {
                    Button { convertToLocal() } label: { Label(loc("timeConvert.toLocal"), systemImage: "arrow.down") }
                    Button { convertToTimestamp() } label: { Label(loc("timeConvert.toTimestamp"), systemImage: "arrow.up") }
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(locf("timeConvert.localTimeZone", zone)).font(.subheadline).foregroundStyle(.secondary)
                    HStack {
                        TextField("yyyy-MM-dd HH:mm:ss", text: $draft.secondary)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(.body, design: .monospaced))
                            .onSubmit(convertToTimestamp)
                        Text("yyyy-MM-dd HH:mm:ss").font(.caption).foregroundStyle(.tertiary)
                        Button { copy(draft.secondary) } label: { Image(systemName: "doc.on.doc") }.help(loc("timeConvert.copy"))
                    }
                }
                Text(loc("timeConvert.isoHint"))
                    .font(.caption).foregroundStyle(.secondary)
                Button(loc("timeConvert.parseDetail")) { parseDetail() }
                if !draft.output.isEmpty {
                    Text(draft.output).font(.system(.callout, design: .monospaced)).textSelection(.enabled)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(Color(nsColor: .textBackgroundColor))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }.padding(4)
        }
    }

    private func timeValueRow(label: String, value: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 4) {
                Text(label).font(.caption).foregroundStyle(.secondary)
                Text(value).font(.system(.title3, design: .monospaced)).textSelection(.enabled)
            }
            Spacer()
            Button { copy(value) } label: { Image(systemName: "doc.on.doc") }.help(loc("timeConvert.copy"))
        }
    }

    private func bootstrap() {
        if draft.option.isEmpty { draft.option = TimeZone.current.identifier }
        if draft.mode.isEmpty { draft.mode = "second" }
        let stamp = Int64(now.timeIntervalSince1970)
        if draft.input.isEmpty { draft.input = String(stamp) }
        if draft.secondary.isEmpty { draft.secondary = TimeConversion.formatLocalTime(date: now, zone: zone) }
    }

    private func refreshLocalFromNow() {
        draft.secondary = TimeConversion.formatLocalTime(date: now, zone: zone)
    }

    private func unitLabel() -> String {
        unit == .second ? loc("timeConvert.unitSecond") : loc("timeConvert.unitMillisecond")
    }

    private func convertToLocal() {
        do {
            let result = try TimeConversion.timestampToLocal(draft.input, unit: unit, zone: zone, language: language)
            draft.secondary = result.localTime
            draft.mode = result.unit.rawValue
            draft.error = nil
            draft.status = locf("timeConvert.statusToLocal", zone)
            store.record("timeConvert")
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func convertToTimestamp() {
        do {
            draft.input = try TimeConversion.localToTimestamp(draft.secondary, unit: unit, zone: zone, language: language)
            draft.error = nil
            draft.status = locf("timeConvert.statusToTimestamp", unitLabel())
            store.record("timeConvert")
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func parseDetail() {
        do {
            draft.output = try DeveloperServices.timestamp(draft.input, zone: zone, language: language)
            draft.error = nil
            draft.status = loc("timeConvert.statusParsed")
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func copy(_ text: String) {
        FilePanels.copy(text)
        draft.status = loc("timeConvert.statusCopied")
        draft.error = nil
    }
}

private struct TimeClockSheet: View {
    let zone: String
    let now: Date
    let onClose: () -> Void
    private var local: String { TimeConversion.formatLocalTime(date: now, zone: zone) }
    private var parts: (date: String, time: String) {
        let pieces = local.split(separator: " ", maxSplits: 1).map(String.init)
        return (pieces.first ?? local, pieces.count > 1 ? pieces[1] : "")
    }

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Spacer()
                Button { onClose() } label: { Image(systemName: "xmark.circle.fill") }.buttonStyle(.borderless)
            }
            Text(zone).font(.title3).foregroundStyle(.secondary)
            Text(parts.time).font(.system(size: 56, weight: .medium, design: .rounded)).monospacedDigit()
            Text(parts.date).font(.title2).foregroundStyle(.secondary)
        }
        .padding(32)
        .frame(minWidth: 360, minHeight: 220)
    }
}
