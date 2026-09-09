import Foundation
import JavaScriptCore

public struct JSONEngineRequest: Codable, Sendable {
    public var action: String
    public var input: String
    public var path = "$"
    public var className = "Root"
    public var indent = 2
    public var sortKeys = false
    public var ignoreCase = false
    public var checkDuplicateKeys = true
    public var query = ""
    public var replacement = ""
    public var matchCase = false
    public var wholeWord = false
    public var regex = false
    public var selectionStart = 0
    public var selectionEnd = 0
    public var forward = true
    public init(_ action: String, input: String) { self.action = action; self.input = input }
}
public struct JSONMatch: Codable, Equatable, Sendable {
    public var start: Int
    public var end: Int
    public var range: NSRange { NSRange(location: start, length: max(0, end - start)) }
}
public struct JSONEngineReply: Codable, Sendable {
    public var value: String?
    public var error: String?
    public var count: Int?
    public var match: JSONMatch?
    public var matches: [JSONMatch]?
}

public enum JSONEngine {
    public static var resources: Bundle {
        let name = "MooToolNextNative_MooToolNextCore"
        if let url = Bundle.main.url(forResource: name, withExtension: "bundle"), let bundle = Bundle(url: url) { return bundle }
        if let executable = Bundle.main.executableURL {
            let url = executable.deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("Resources/\(name).bundle")
            if let bundle = Bundle(url: url) { return bundle }
        }
        return Bundle.module
    }
    public static var workerURL: URL {
        let sibling = Bundle.main.executableURL!.deletingLastPathComponent().appendingPathComponent("MooToolJSONWorker")
        if FileManager.default.isExecutableFile(atPath: sibling.path) { return sibling }
        return resources.bundleURL.deletingLastPathComponent().appendingPathComponent("MooToolJSONWorker")
    }
    /// UI callers always use the bounded helper process. No host objects are exposed to JavaScript.
    public static func execute(_ request: JSONEngineRequest, timeout: TimeInterval = 3) async throws -> JSONEngineReply {
        let state = WorkerCancellation()
        return try await withTaskCancellationHandler {
            try await Task.detached(priority: .userInitiated) { try runWorker(request, timeout: timeout, state: state) }.value
        } onCancel: { state.cancel() }
    }
    private static func runWorker(_ request: JSONEngineRequest, timeout: TimeInterval, state: WorkerCancellation) throws -> JSONEngineReply {
        let data = try JSONEncoder().encode(request)
        guard data.count <= 16 * 1024 * 1024 else { throw ToolError("JSON 操作输入超过 16 MB。") }
        guard FileManager.default.isExecutableFile(atPath: workerURL.path) else { throw ToolError("找不到原生 JSON 辅助程序，请重新构建或安装应用。") }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(Product.id + "-json-" + UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        defer { try? FileManager.default.removeItem(at: directory) }
        let input = directory.appendingPathComponent("input"), output = directory.appendingPathComponent("output")
        FileManager.default.createFile(atPath: input.path, contents: data, attributes: [.posixPermissions: 0o600])
        FileManager.default.createFile(atPath: output.path, contents: nil, attributes: [.posixPermissions: 0o600])
        let reader = try FileHandle(forReadingFrom: input), writer = try FileHandle(forWritingTo: output)
        defer { try? reader.close(); try? writer.close() }
        let process = Process(); process.executableURL = workerURL; process.currentDirectoryURL = directory
        process.standardInput = reader; process.standardOutput = writer; process.standardError = FileHandle.nullDevice
        // Keep the worker independent of shell startup, injected runtime paths and user environment secrets.
        process.environment = ["LANG": "en_US.UTF-8"]
        try state.launch(process)
        let deadline = Date().addingTimeInterval(timeout)
        var failure: Error?
        while process.isRunning {
            if state.cancelled { failure = CancellationError() }
            else if Date() >= deadline { failure = ToolError("JSON 操作超过 \(timeout) 秒，已停止；正文保持原样。") }
            else if (try? output.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0 > 16 * 1024 * 1024 { failure = ToolError("JSON 操作结果超过 16 MB。") }
            if failure != nil { kill(process.processIdentifier, SIGKILL); break }
            Thread.sleep(forTimeInterval: 0.01)
        }
        process.waitUntilExit(); state.finish()
        if state.cancelled { throw CancellationError() }
        if let failure { throw failure }
        guard process.terminationStatus == 0 else { throw ToolError("JSON 辅助程序未能完成操作，正文保持原样。") }
        let handle = try FileHandle(forReadingFrom: output); defer { try? handle.close() }
        let result = try handle.read(upToCount: 16 * 1024 * 1024 + 1) ?? Data()
        guard result.count <= 16 * 1024 * 1024 else { throw ToolError("JSON 操作结果超过 16 MB。") }
        let reply = try JSONDecoder().decode(JSONEngineReply.self, from: result)
        if let error = reply.error { throw ToolError(error) }
        return reply
    }
    /// Only the helper process and deterministic core tests call this synchronous engine.
    public static func evaluateLocally(_ request: JSONEngineRequest) throws -> JSONEngineReply {
        guard request.input.utf8.count <= 10 * 1024 * 1024, request.path.utf8.count <= 16_384,
              request.query.utf8.count <= 16_384, request.replacement.utf8.count <= 1024 * 1024,
              request.className.utf8.count <= 240 else { throw ToolError("输入、查询或替换内容超过限制。") }
        if request.action == "formatXML" { return JSONEngineReply(value: try TextServices.formatXML(request.input)) }
        guard let context = JSContext() else { throw ToolError("无法初始化系统 JavaScriptCore。") }
        for name in ["jsonpath-plus", "fast-xml-parser", "JSONTools", "QuickNoteTools", "JSONDispatch"] {
            guard let url = resources.url(forResource: name, withExtension: "js") else { throw ToolError("缺少 JSON 解析资源：\(name)") }
            context.evaluateScript(try String(contentsOf: url, encoding: .utf8))
            if let exception = context.exception { throw ToolError(exception.toString() ?? "JSON 引擎初始化失败。") }
        }
        let payload = String(decoding: try JSONEncoder().encode(request), as: UTF8.self)
        let value = context.objectForKeyedSubscript("runJSONRequest")?.call(withArguments: [payload])
        if let exception = context.exception { throw ToolError(exception.toString() ?? "JSON 操作失败。") }
        guard let string = value?.toString(), string.utf8.count <= 16 * 1024 * 1024 else { throw ToolError("JSON 结果无效或超过 16 MB。") }
        let reply = try JSONDecoder().decode(JSONEngineReply.self, from: Data(string.utf8))
        if let error = reply.error { throw ToolError(error) }
        return reply
    }
}

private final class WorkerCancellation: @unchecked Sendable {
    private let lock = NSLock()
    private var stopped = false
    private var process: Process?
    var cancelled: Bool { lock.lock(); defer { lock.unlock() }; return stopped }
    func launch(_ process: Process) throws {
        lock.lock(); defer { lock.unlock() }
        guard !stopped else { throw CancellationError() }
        try process.run(); self.process = process
    }
    func finish() { lock.lock(); process = nil; lock.unlock() }
    func cancel() { lock.lock(); stopped = true; if let process, process.isRunning { kill(process.processIdentifier, SIGKILL) }; lock.unlock() }
}
