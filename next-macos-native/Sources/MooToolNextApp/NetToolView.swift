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
    var body: some View {
        ToolPage(tool: Catalog.tool("net"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.stop", language: language), symbol: "stop.fill") { draft.busy = false }.disabled(!draft.busy)
            Text("网络探测使用本机命令与 TCP 连接，请遵守网络策略。").font(.caption).foregroundStyle(.secondary)
        } content: {
            PersistedHSplit(toolID: "net", defaultLeading: 300, minLeading: 240, maxLeading: 480) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        section("IPv4 ↔ Long") {
                            TextField("IPv4", text: $ipv4).textFieldStyle(.roundedBorder)
                            HStack {
                                Button("转换为数值") { convertFromIPv4() }
                                Button("转换为 IPv4") { convertFromLong() }
                            }
                            TextField("Long", text: $longValue).textFieldStyle(.roundedBorder)
                        }
                        section("Ping") {
                            TextField("主机", text: $pingHost).textFieldStyle(.roundedBorder)
                            Button("Ping") { run { try await ProcessRunner.run(executable: pingHost.contains(":") ? "/sbin/ping6" : "/sbin/ping", arguments: ["-c", "4", pingHost], timeout: 12) } }
                        }
                        section("IP 段探测 (/24)") {
                            TextField("例如 192.168.1", text: $ipRange).textFieldStyle(.roundedBorder)
                            Button("扫描可达主机") { run { try await NetworkDiagnostics.scanIPv4Range(ipRange) } }
                            Text("对 x.x.x.1–254 各发 1 次 Ping，可能较慢。").font(.caption).foregroundStyle(.secondary)
                        }
                        section("端口扫描") {
                            TextField("主机", text: $portHost).textFieldStyle(.roundedBorder)
                            TextField("端口（空=常见端口）", text: $portSpec).textFieldStyle(.roundedBorder)
                            Button("扫描") { run { try await NetworkDiagnostics.scanPorts(host: portHost, portSpec: portSpec) } }
                        }
                        section("解析 / Whois / DNS") {
                            TextField("解析主机", text: $resolveHost).textFieldStyle(.roundedBorder)
                            Button("解析") { run { try NetworkDiagnostics.resolveHost(resolveHost) } }
                            TextField("Whois", text: $whoisHost).textFieldStyle(.roundedBorder)
                            Button("Whois 查询") { run { try await ProcessRunner.run(executable: "/usr/bin/whois", arguments: [whoisHost]) } }
                            TextField("DNS 查询", text: $dnsHost).textFieldStyle(.roundedBorder)
                            Button("dig 查询") { run { try await ProcessRunner.run(executable: "/usr/bin/dig", arguments: ["+time=3", "+tries=1", dnsHost]) } }
                            Button("刷新 DNS 缓存") { run { try await ProcessRunner.run(executable: "/usr/bin/dscacheutil", arguments: ["-flushcache"]) + "\n" + (try await ProcessRunner.run(executable: "/usr/bin/killall", arguments: ["-HUP", "mDNSResponder"])) } }
                        }
                        section("本机与连接") {
                            Button("本机地址") { run { NetworkDiagnostics.localAddresses() } }
                            Button("ifconfig") { run { try await ProcessRunner.run(executable: "/sbin/ifconfig", arguments: []) } }
                            Button("netstat") { run { try await ProcessRunner.run(executable: "/usr/sbin/netstat", arguments: ["-nat"]) } }
                        }
                    }.padding()
                }
            } trailing: {
                EditorPane(title: "输出", text: $draft.output, editable: false)
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
            do { draft.output = try await operation(); draft.status = "已完成"; store.record("net") }
            catch { draft.error = error.localizedDescription }
        }
    }
    private func run(_ operation: @escaping () throws -> String) {
        guard !draft.busy else { return }
        draft.busy = true; draft.error = nil
        Task {
            defer { draft.busy = false }
            do { draft.output = try operation(); draft.status = "已完成"; store.record("net") }
            catch { draft.error = error.localizedDescription }
        }
    }
}
