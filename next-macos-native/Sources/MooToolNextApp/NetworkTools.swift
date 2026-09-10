import SwiftUI
import MooToolNextCore

struct SystemTool: View {
    let id: String
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    var body: some View {
        ToolPage(tool: Catalog.tool(id), draft: draft) {
            if id == "java" {
                Picker("语言", selection: $draft.mode) { ForEach(["Python", "JavaScript", "Swift", "Java", "Groovy"], id: \.self) { Text($0) } }.frame(width: 165)
                PrimaryButton(title: "运行代码", action: run)
                Button("示例") { draft.input = example }
                Text("使用本机运行时 · 最长 20 秒").font(.caption).foregroundStyle(.secondary)
            } else if id == "net" {
                Picker("工具", selection: $draft.mode) { ForEach(["DNS", "Ping", "Whois", "网络接口"], id: \.self) { Text($0) } }.frame(width: 150)
                TextField("域名或 IP", text: $draft.input).textFieldStyle(.roundedBorder).frame(minWidth: 180, maxWidth: 400)
                PrimaryButton(title: "查询", action: run)
            } else {
                PrimaryButton(title: "刷新系统信息", symbol: "arrow.clockwise", action: run)
                Button("详细硬件报告") { draft.mode = "详细"; run() }
            }
        } content: {
            HSplitView {
                if id == "java" { EditorPane(title: "代码", text: $draft.input) }
                EditorPane(title: id == "hardware" ? "此 Mac" : "输出", text: $draft.output, editable: false)
            }
        }.onAppear {
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
        Task {
            defer { draft.busy = false }
            do {
                let output: String
                if id == "java" {
                    let runtimes = ["Python": ("python3", "py"), "JavaScript": ("node", "js"), "Swift": ("swift", "swift"), "Java": ("java", "java"), "Groovy": ("groovy", "groovy")]
                    guard let runtime = runtimes[mode] else { throw ToolError("请选择运行语言。") }
                    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(Product.id + "-" + UUID().uuidString)
                    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
                    defer { try? FileManager.default.removeItem(at: directory) }
                    let file = directory.appendingPathComponent("Main." + runtime.1)
                    try input.write(to: file, atomically: true, encoding: .utf8)
                    var env = ["PATH": (ProcessInfo.processInfo.environment["PATH"] ?? "/usr/bin:/bin") + ":/opt/homebrew/bin:/usr/local/bin"]
                    for line in environment.components(separatedBy: .newlines) {
                        let line = line.trimmingCharacters(in: .whitespaces)
                        if line.isEmpty || line.hasPrefix("#") { continue }
                        guard let separator = line.firstIndex(of: "=") else { throw ToolError("环境变量需要 KEY=value。") }
                        let key = String(line[..<separator])
                        guard key.range(of: #"^[A-Za-z_][A-Za-z0-9_]*$"#, options: .regularExpression) != nil else { throw ToolError("无效环境变量名：\(key)") }
                        env[key] = String(line[line.index(after: separator)...])
                    }
                    output = try await ProcessRunner.run(executable: "/usr/bin/env", arguments: [runtime.0, file.path], environment: env)
                } else if id == "net" {
                    let host = input.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard mode == "网络接口" || (!host.hasPrefix("-") && host.range(of: #"^[A-Za-z0-9.:_-]+$"#, options: .regularExpression) != nil) else { throw ToolError("请输入域名或 IP，不包含空格和命令参数。") }
                    switch mode {
                    case "DNS": output = try await ProcessRunner.run(executable: "/usr/bin/dig", arguments: ["+time=3", "+tries=1", host])
                    case "Ping": output = try await ProcessRunner.run(executable: host.contains(":") ? "/sbin/ping6" : "/sbin/ping", arguments: ["-c", "4", host], timeout: 12)
                    case "Whois": output = try await ProcessRunner.run(executable: "/usr/bin/whois", arguments: [host])
                    default: output = try await ProcessRunner.run(executable: "/sbin/ifconfig", arguments: [])
                    }
                } else {
                    let info = ProcessInfo.processInfo
                    var text = "\(Host.current().localizedName ?? "Mac")\n\n系统      \(info.operatingSystemVersionString)\n处理器    \(info.processorCount) 核（\(info.activeProcessorCount) 活跃）\n物理内存  \(ByteCountFormatter.string(fromByteCount: Int64(info.physicalMemory), countStyle: .memory))\n运行时间  \(Int(info.systemUptime / 3600)) 小时\n"
                    if let attributes = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory()), let free = attributes[.systemFreeSize] as? Int64, let total = attributes[.systemSize] as? Int64 { text += "磁盘空间  \(ByteCountFormatter.string(fromByteCount: free, countStyle: .file)) 可用 / \(ByteCountFormatter.string(fromByteCount: total, countStyle: .file))\n" }
                    if mode == "详细" { text += "\n" + (try await ProcessRunner.run(executable: "/usr/sbin/system_profiler", arguments: ["SPHardwareDataType", "SPDisplaysDataType"], timeout: 30)) }
                    output = text
                }
                draft.output = output; draft.status = "已完成"; if id != "hardware" { store.record(id) }
            } catch { draft.error = error.localizedDescription }
        }
    }
}
