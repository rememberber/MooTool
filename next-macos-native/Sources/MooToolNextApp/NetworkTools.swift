import SwiftUI
import MooToolNextCore

struct SystemTool: View {
    let id: String
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    private static let netModes = ["DNS", "Ping", "Whois", "interfaces"]
    private func netModeLabel(_ mode: String) -> String {
        switch mode {
        case "DNS": return loc("net.mode.dns")
        case "Ping": return loc("net.mode.ping")
        case "Whois": return loc("net.mode.whois")
        case "interfaces", "网络接口": return loc("net.mode.interfaces")
        default: return mode
        }
    }
    var body: some View {
        ToolPage(tool: Catalog.tool(id), draft: draft) {
            if id == "java" {
                Picker(loc("codeRun.language"), selection: $draft.mode) { ForEach(["Python", "JavaScript", "Swift", "Java", "Groovy"], id: \.self) { Text($0) } }.frame(width: 165)
                PrimaryButton(title: AppLocalization.string("tool.runCode", language: language), action: run)
                Button(AppLocalization.string("tool.example", language: language)) { draft.input = example }
                Text(loc("codeRun.runtimeHint")).font(.caption).foregroundStyle(.secondary)
            } else if id == "net" {
                Picker(loc("net.tool"), selection: $draft.mode) { ForEach(Self.netModes, id: \.self) { Text(netModeLabel($0)).tag($0) } }.frame(width: 150)
                TextField(loc("net.hostPlaceholder"), text: $draft.input).textFieldStyle(.roundedBorder).frame(minWidth: 180, maxWidth: 400)
                PrimaryButton(title: AppLocalization.string("tool.query", language: language), action: run)
            } else {
                PrimaryButton(title: AppLocalization.string("tool.refreshSystemInfo", language: language), symbol: "arrow.clockwise", action: run)
                Button(loc("hardware.detailedReport")) { draft.mode = "detailed"; run() }
            }
        } content: {
            if id == "java" {
                PersistedHSplit(toolID: "java", defaultLeading: 420, minLeading: 280, maxLeading: 900) {
                    EditorPane(title: loc("tool.input"), text: $draft.input)
                } trailing: {
                    EditorPane(title: loc("tool.output"), text: $draft.output, editable: false)
                }
            } else {
                EditorPane(title: id == "hardware" ? loc("hardware.thisMac") : loc("tool.output"), text: $draft.output, editable: false)
            }
        }.onAppear {
            if id == "net", draft.mode == "网络接口" { draft.mode = "interfaces" }
            if id == "hardware", draft.mode == "详细" { draft.mode = "detailed" }
            if draft.mode.isEmpty { draft.mode = id == "java" ? "Python" : "DNS" }
            if id == "hardware", draft.output.isEmpty { run() }
        }
    }
    private var example: String {
        switch draft.mode {
        case "JavaScript": return "console.log('Hello, MooTool!');\nconsole.log(process.version);"
        case "Swift": return "import Foundation\nprint(\"Hello, MooTool!\")\nprint(Date())"
        case "Java": return "class Main { public static void main(String[] args) { System.out.println(\"Hello, MooTool!\"); } }"
        case "Groovy": return "println 'Hello, MooTool!'"
        default: return "import sys\nprint('Hello, MooTool!')\nprint(sys.version)"
        }
    }
    private func run() {
        guard !draft.busy else { return }; draft.busy = true; draft.error = nil
        let mode = draft.mode, input = draft.input
        let environment = store.draft("variables").input
        let lang = language
        Task { @MainActor in
            defer { draft.busy = false }
            do {
                let output: String
                if id == "java" {
                    let runtimes = ["Python": ("python3", "py"), "JavaScript": ("node", "js"), "Swift": ("swift", "swift"), "Java": ("java", "java"), "Groovy": ("groovy", "groovy")]
                    guard let runtime = runtimes[mode] else { throw ToolError(AppLocalization.string("codeRun.error.pickRuntime", language: lang)) }
                    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(Product.id + "-" + UUID().uuidString)
                    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
                    defer { try? FileManager.default.removeItem(at: directory) }
                    let file = directory.appendingPathComponent("Main." + runtime.1)
                    try input.write(to: file, atomically: true, encoding: .utf8)
                    var env = ["PATH": (ProcessInfo.processInfo.environment["PATH"] ?? "/usr/bin:/bin") + ":/opt/homebrew/bin:/usr/local/bin"]
                    for line in environment.components(separatedBy: .newlines) {
                        let line = line.trimmingCharacters(in: .whitespaces)
                        if line.isEmpty || line.hasPrefix("#") { continue }
                        guard let separator = line.firstIndex(of: "=") else { throw ToolError(AppLocalization.string("codeRun.error.envLine", language: lang)) }
                        let key = String(line[..<separator])
                        guard key.range(of: #"^[A-Za-z_][A-Za-z0-9_]*$"#, options: .regularExpression) != nil else {
                            throw ToolError(String(format: AppLocalization.string("codeRun.error.envKey", language: lang), key))
                        }
                        env[key] = String(line[line.index(after: separator)...])
                    }
                    output = try await ProcessRunner.run(executable: "/usr/bin/env", arguments: [runtime.0, file.path], environment: env)
                } else if id == "net" {
                    let host = input.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard mode == "interfaces" || mode == "网络接口" || (!host.hasPrefix("-") && host.range(of: #"^[A-Za-z0-9.:_-]+$"#, options: .regularExpression) != nil) else {
                        throw ToolError(AppLocalization.string("codeRun.error.hostTarget", language: lang))
                    }
                    switch mode {
                    case "DNS": output = try await ProcessRunner.run(executable: "/usr/bin/dig", arguments: ["+time=3", "+tries=1", host])
                    case "Ping": output = try await ProcessRunner.run(executable: host.contains(":") ? "/sbin/ping6" : "/sbin/ping", arguments: ["-c", "4", host], timeout: 12)
                    case "Whois": output = try await ProcessRunner.run(executable: "/usr/bin/whois", arguments: [host])
                    default: output = try await ProcessRunner.run(executable: "/sbin/ifconfig", arguments: [])
                    }
                } else {
                    let info = ProcessInfo.processInfo
                    let freeFormatter = ByteCountFormatter()
                    freeFormatter.countStyle = .file
                    let memoryFormatter = ByteCountFormatter()
                    memoryFormatter.countStyle = .memory
                    let name = Host.current().localizedName ?? "Mac"
                    var text = "\(name)\n\n"
                    text += "\(AppLocalization.string("hardware.field.system", language: lang))      \(info.operatingSystemVersionString)\n"
                    text += "\(AppLocalization.string("hardware.field.processor", language: lang))    \(String(format: AppLocalization.string("hardware.field.processorFormat", language: lang), info.processorCount, info.activeProcessorCount))\n"
                    text += "\(AppLocalization.string("hardware.field.memory", language: lang))  \(memoryFormatter.string(fromByteCount: Int64(info.physicalMemory)))\n"
                    text += "\(AppLocalization.string("hardware.field.uptime", language: lang))  \(String(format: AppLocalization.string("hardware.field.uptimeFormat", language: lang), Int(info.systemUptime / 3600)))\n"
                    if let attributes = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory()), let free = attributes[.systemFreeSize] as? Int64, let total = attributes[.systemSize] as? Int64 {
                        let freeText = freeFormatter.string(fromByteCount: free)
                        let totalText = freeFormatter.string(fromByteCount: total)
                        text += "\(AppLocalization.string("hardware.field.disk", language: lang))  \(String(format: AppLocalization.string("hardware.field.diskFormat", language: lang), freeText, totalText))\n"
                    }
                    if mode == "detailed" || mode == "详细" { text += "\n" + (try await ProcessRunner.run(executable: "/usr/sbin/system_profiler", arguments: ["SPHardwareDataType", "SPDisplaysDataType"], timeout: 30)) }
                    output = text
                }
                draft.output = output
                draft.status = AppLocalization.string("tool.status.done", language: lang)
                if id != "hardware" { store.record(id) }
            } catch { draft.error = error.localizedDescription }
        }
    }
}
