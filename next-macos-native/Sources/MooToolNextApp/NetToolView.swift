import SwiftUI
import MooToolNextCore

struct NetToolView: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var pingHost = "127.0.0.1"
    @State private var ipRange = "192.168.1"
    @State private var portHost = "127.0.0.1"
    @State private var portSpec = ""
    @State private var resolveHost = "localhost"
    @State private var whoisHost = "example.com"
    @State private var dnsHost = "example.com"
    @State private var ipv4 = "192.168.0.1"
    @State private var longValue = ""
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        ToolPage(tool: Catalog.tool("net"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.stop", language: language), symbol: "stop.fill") { draft.busy = false }.disabled(!draft.busy)
            Text(loc("net.policyHint")).font(.caption).foregroundStyle(.secondary)
        } content: {
            PersistedHSplit(toolID: "net", defaultLeading: 300, minLeading: 240, maxLeading: 480) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        section(loc("net.ipv4Long")) {
                            TextField(loc("net.ipv4"), text: $ipv4).textFieldStyle(.roundedBorder)
                            HStack {
                                Button(loc("net.convertToLong")) { convertFromIPv4() }
                                Button(loc("net.convertToIPv4")) { convertFromLong() }
                            }
                            TextField(loc("net.long"), text: $longValue).textFieldStyle(.roundedBorder)
                        }
                        section(loc("net.ping")) {
                            TextField(loc("net.host"), text: $pingHost).textFieldStyle(.roundedBorder)
                            Button(loc("net.ping")) { run { try await ProcessRunner.run(executable: pingHost.contains(":") ? "/sbin/ping6" : "/sbin/ping", arguments: ["-c", "4", pingHost], timeout: 12) } }
                        }
                        section(loc("net.ipRangeScan")) {
                            TextField(loc("net.ipRangePlaceholder"), text: $ipRange).textFieldStyle(.roundedBorder)
                            Button(loc("net.scan")) { run { try await NetworkDiagnostics.scanIPv4Range(ipRange) } }
                            Text(loc("net.ipRangeHint")).font(.caption).foregroundStyle(.secondary)
                        }
                        section(loc("net.portScan")) {
                            TextField(loc("net.portScanTargetPlaceholder"), text: $portHost).textFieldStyle(.roundedBorder)
                            TextField(loc("net.portScanPortsPlaceholder"), text: $portSpec).textFieldStyle(.roundedBorder)
                            Button(loc("net.scan")) { run { try await NetworkDiagnostics.scanPorts(host: portHost, portSpec: portSpec) } }
                        }
                        section(loc("net.section.resolve")) {
                            TextField(loc("net.resolveHost"), text: $resolveHost).textFieldStyle(.roundedBorder)
                            Button(loc("net.resolveAction")) { run { try NetworkDiagnostics.resolveHost(resolveHost) } }
                            TextField(loc("net.whois"), text: $whoisHost).textFieldStyle(.roundedBorder)
                            Button(loc("net.whoisQuery")) { run { try await ProcessRunner.run(executable: "/usr/bin/whois", arguments: [whoisHost]) } }
                            TextField(loc("net.dnsQuery"), text: $dnsHost).textFieldStyle(.roundedBorder)
                            Button(loc("net.digQuery")) { run { try await ProcessRunner.run(executable: "/usr/bin/dig", arguments: ["+time=3", "+tries=1", dnsHost]) } }
                            Button(loc("net.flushDns")) { run { try await ProcessRunner.run(executable: "/usr/bin/dscacheutil", arguments: ["-flushcache"]) + "\n" + (try await ProcessRunner.run(executable: "/usr/bin/killall", arguments: ["-HUP", "mDNSResponder"])) } }
                        }
                        section(loc("net.section.local")) {
                            Button(loc("net.localAddresses")) { run { NetworkDiagnostics.localAddresses() } }
                            Button(loc("net.ifconfig")) { run { try await ProcessRunner.run(executable: "/sbin/ifconfig", arguments: []) } }
                            Button(loc("net.netstat")) { run { try await ProcessRunner.run(executable: "/usr/sbin/netstat", arguments: ["-nat"]) } }
                        }
                    }.padding()
                }
            } trailing: {
                EditorPane(title: loc("net.output"), text: $draft.output, editable: false)
            }
        }
    }
    @ViewBuilder private func section(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline)
            content()
        }.padding(12).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 10)).overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(.quaternary))
    }
    private func convertFromIPv4() {
        do { longValue = String(try NetworkDiagnostics.ipv4ToLong(ipv4)) } catch { draft.error = error.localizedDescription }
    }
    private func convertFromLong() {
        do { ipv4 = try NetworkDiagnostics.longToIPv4(longValue) } catch { draft.error = error.localizedDescription }
    }
    private func run(_ operation: @escaping () async throws -> String) {
        guard !draft.busy else { return }
        draft.busy = true; draft.error = nil
        Task {
            defer { draft.busy = false }
            do { draft.output = try await operation(); draft.status = AppLocalization.string("tool.status.done", language: language); store.record("net") }
            catch { draft.error = error.localizedDescription }
        }
    }
    private func run(_ operation: @escaping () throws -> String) {
        guard !draft.busy else { return }
        draft.busy = true; draft.error = nil
        Task {
            defer { draft.busy = false }
            do { draft.output = try operation(); draft.status = AppLocalization.string("tool.status.done", language: language); store.record("net") }
            catch { draft.error = error.localizedDescription }
        }
    }
}
