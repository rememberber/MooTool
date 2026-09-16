import SwiftUI
import MooToolNextCore

struct TimeConvertView: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @State private var now = Date()
    @State private var historyOpen = false
    @State private var clockOpen = false
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    private var zone: String { draft.option.isEmpty ? TimeZone.current.identifier : draft.option }
    private var unit: TimeConversion.TimestampUnit { draft.mode == "millisecond" ? .millisecond : .second }
    private var zones: [String] { TimeConversion.timezones(including: TimeZone.current.identifier) }

    var body: some View {
        ToolPage(tool: Catalog.tool("timeConvert"), draft: draft) {
            Button { historyOpen = true } label: { Label("历史记录", systemImage: "clock.arrow.circlepath") }
            Button { clockOpen = true } label: { Label("大时钟", systemImage: "arrow.up.left.and.arrow.down.right") }
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
        .sheet(isPresented: $historyOpen) { HistoryView(toolID: "timeConvert").environment(store) }
        .sheet(isPresented: $clockOpen) { TimeClockSheet(zone: zone, now: now) { clockOpen = false } }
    }

    private var currentBand: some View {
        let timestamp = String(Int64(now.timeIntervalSince1970))
        let local = TimeConversion.formatLocalTime(date: now, zone: zone)
        return GroupBox("当前时间") {
            VStack(alignment: .leading, spacing: 12) {
                timeValueRow(label: "时间戳（秒）", value: timestamp)
                timeValueRow(label: "本地时间 · \(zone)", value: local)
                Text(TimeConversion.formatTimezoneLabel(zone: zone, date: now))
                    .font(.caption).foregroundStyle(.secondary)
            }.padding(4)
        }
    }

    private var zoneBand: some View {
        GroupBox("时区") {
            VStack(alignment: .leading, spacing: 10) {
                Picker("时区", selection: Binding(get: { zone }, set: { draft.option = $0; refreshLocalFromNow() })) {
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
        GroupBox("转换") {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("时间戳").font(.subheadline).foregroundStyle(.secondary)
                    HStack {
                        TextField("1700000000", text: $draft.input)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(.body, design: .monospaced))
                            .onSubmit(convertToLocal)
                        Picker("单位", selection: Binding(get: { unit.rawValue }, set: { draft.mode = $0 })) {
                            Text("秒").tag("second")
                            Text("毫秒").tag("millisecond")
                        }.frame(width: 88)
                        Button { copy(draft.input) } label: { Image(systemName: "doc.on.doc") }.help("复制")
                    }
                }
                HStack(spacing: 12) {
                    Button { convertToLocal() } label: { Label("转为本地时间", systemImage: "arrow.down") }
                    Button { convertToTimestamp() } label: { Label("转为时间戳", systemImage: "arrow.up") }
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text("本地时间 · \(zone)").font(.subheadline).foregroundStyle(.secondary)
                    HStack {
                        TextField("yyyy-MM-dd HH:mm:ss", text: $draft.secondary)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(.body, design: .monospaced))
                            .onSubmit(convertToTimestamp)
                        Text("yyyy-MM-dd HH:mm:ss").font(.caption).foregroundStyle(.tertiary)
                        Button { copy(draft.secondary) } label: { Image(systemName: "doc.on.doc") }.help("复制")
                    }
                }
                Text("仍可在输入框使用 ISO 8601 或混合格式，通过下方「详细解析」查看秒/毫秒与 UTC。")
                    .font(.caption).foregroundStyle(.secondary)
                Button("详细解析（当前时间戳输入）") { parseDetail() }
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
            Button { copy(value) } label: { Image(systemName: "doc.on.doc") }.help("复制")
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

    private func convertToLocal() {
        do {
            let result = try TimeConversion.timestampToLocal(draft.input, unit: unit, zone: zone)
            draft.secondary = result.localTime
            draft.mode = result.unit.rawValue
            draft.error = nil
            draft.status = "已转为 \(zone) 本地时间"
            store.record("timeConvert")
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func convertToTimestamp() {
        do {
            draft.input = try TimeConversion.localToTimestamp(draft.secondary, unit: unit, zone: zone)
            draft.error = nil
            draft.status = "已转为时间戳（\(unit == .second ? "秒" : "毫秒")）"
            store.record("timeConvert")
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func parseDetail() {
        do {
            draft.output = try DeveloperServices.timestamp(draft.input, zone: zone)
            draft.error = nil
            draft.status = "已生成详细解析"
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func copy(_ text: String) {
        FilePanels.copy(text)
        draft.status = "已复制"
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
