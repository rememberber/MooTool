import Foundation

public struct HTTPResponse {
    public let status: Int
    public let headers: String
    public let body: String
    public let elapsed: TimeInterval
    public let bytes: Int
    public let cookies: String
    public let url: String
    public var metadata: HTTPResultMetadata { HTTPResultMetadata(status: status, headers: headers, cookies: cookies, url: url, elapsed: elapsed, bytes: bytes) }
}
public enum NetworkServices {
    public static func request(_ draft: DraftRecord) throws -> URLRequest {
        let options = draft.http ?? HTTPOptions()
        guard options.timeout.isFinite, (1...120).contains(options.timeout) else { throw ToolError("请求超时需在 1 至 120 秒之间。") }
        for fields in [options.params, options.cookies, options.form] { guard fields.count <= 1000 else { throw ToolError("每组最多支持 1000 个参数。") } }
        let url = try HTTPFields.appendingQuery(HTTPFields.query(options.params), to: draft.option)
        var body = draft.input
        if options.bodyKind == .form { body = HTTPFields.query(options.form) }
        if options.bodyKind == .none { body = "" }
        guard body.utf8.count + draft.secondary.utf8.count <= 10 * 1024 * 1024 else { throw ToolError("请求正文和请求头超过 10 MB。") }
        var result = try request(method: draft.mode.isEmpty ? "GET" : draft.mode, url: url, headers: draft.secondary, body: body)
        result.timeoutInterval = options.timeout
        if result.value(forHTTPHeaderField: "Content-Type") == nil {
            if options.bodyKind == .json { result.setValue("application/json", forHTTPHeaderField: "Content-Type") }
            if options.bodyKind == .form { result.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type") }
        }
        let cookies = try HTTPFields.active(options.cookies).map { field -> String in
            let nameAllowed = CharacterSet(charactersIn: "!#$%&'*+-.^_`|~0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
            guard field.name.unicodeScalars.allSatisfy(nameAllowed.contains), field.value.unicodeScalars.allSatisfy({ scalar in
                let n = scalar.value; return n == 33 || (35...43).contains(n) || (45...58).contains(n) || (60...91).contains(n) || (93...126).contains(n)
            }) else { throw ToolError("Cookie 名称或值包含无效字符；中文和空格请先进行 URL 编码。") }
            return field.name + "=" + field.value
        }
        if !cookies.isEmpty {
            let values = ([result.value(forHTTPHeaderField: "Cookie")] + cookies.map(Optional.some)).compactMap { $0 }.filter { !$0.isEmpty }
            result.setValue(values.joined(separator: "; "), forHTTPHeaderField: "Cookie")
        }
        return result
    }
    public static func request(method: String, url: String, headers: String, body: String) throws -> URLRequest {
        guard let url = URL(string: url.trimmingCharacters(in: .whitespacesAndNewlines)),
              ["http", "https"].contains(url.scheme?.lowercased() ?? ""), let host = url.host, !host.isEmpty else { throw ToolError("请输入有效的 HTTP / HTTPS URL。") }
        guard ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"].contains(method) else { throw ToolError("不支持的 HTTP 方法。") }
        var request = URLRequest(url: url, timeoutInterval: 30); request.httpMethod = method
        for (key, value) in try HTTPFields.headerLines(headers) {
            if key.lowercased() == "cookie", let previous = request.value(forHTTPHeaderField: key) { request.setValue(previous + "; " + value, forHTTPHeaderField: key) }
            else { request.addValue(value, forHTTPHeaderField: key) }
        }
        if !body.isEmpty && method != "GET" && method != "HEAD" { request.httpBody = Data(body.utf8) }
        return request
    }
    public static func send(_ request: URLRequest, followRedirects: Bool = true) async throws -> HTTPResponse {
        // No shared cookies, disk cache, or credentials with other applications.
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForResource = request.timeoutInterval
        let session = URLSession(configuration: config, delegate: RedirectPolicy(follow: followRedirects), delegateQueue: nil)
        defer { session.invalidateAndCancel() }
        let start = Date(); let (stream, response) = try await session.bytes(for: request)
        var data = Data()
        for try await byte in stream {
            if data.count >= 10 * 1024 * 1024 { throw ToolError("响应超过 10 MB，请改用文件下载工具。") }
            data.append(byte)
        }
        guard let http = response as? HTTPURLResponse else { throw ToolError("服务器没有返回 HTTP 响应。") }
        let fields = http.allHeaderFields.reduce(into: [String: String]()) { $0[String(describing: $1.key)] = String(describing: $1.value) }
        let cookies = HTTPCookie.cookies(withResponseHeaderFields: fields, for: http.url ?? request.url!).map { cookie in
            "\(cookie.name)=\(cookie.value)\n  Domain: \(cookie.domain) · Path: \(cookie.path)" + (cookie.isSecure ? " · Secure" : "") + (cookie.isHTTPOnly ? " · HttpOnly" : "")
        }.joined(separator: "\n\n")
        return HTTPResponse(status: http.statusCode,
            headers: http.allHeaderFields.map { "\($0.key): \($0.value)" }.sorted().joined(separator: "\n"),
            body: String(data: data, encoding: .utf8) ?? "二进制响应（Base64）\n" + data.base64EncodedString(),
            elapsed: Date().timeIntervalSince(start), bytes: data.count, cookies: cookies, url: http.url?.absoluteString ?? "")
    }
}

private final class RedirectPolicy: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    let follow: Bool
    init(follow: Bool) { self.follow = follow }
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(follow ? request : nil) }
}

/// Runs only on explicit user action. Arguments are never interpolated into a shell command.
public enum ProcessRunner {
    public static func run(executable: String, arguments: [String], environment: [String: String] = [:], timeout: TimeInterval = 20) async throws -> String {
        try await Task.detached(priority: .userInitiated) {
            try runSynchronously(executable: executable, arguments: arguments, environment: environment, timeout: timeout)
        }.value
    }
    private static func runSynchronously(executable: String, arguments: [String], environment: [String: String], timeout: TimeInterval) throws -> String {
        // A file-backed stream prevents a spawned descendant holding a pipe open from hanging the UI.
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(Product.id + "-process-" + UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        defer { try? FileManager.default.removeItem(at: directory) }
        let file = directory.appendingPathComponent("output")
        FileManager.default.createFile(atPath: file.path, contents: nil, attributes: [.posixPermissions: 0o600])
        let handle = try FileHandle(forWritingTo: file)
        defer { try? handle.close() }
        var attributes: posix_spawnattr_t?
        var actions: posix_spawn_file_actions_t?
        posix_spawnattr_init(&attributes); posix_spawn_file_actions_init(&actions)
        defer { posix_spawnattr_destroy(&attributes); posix_spawn_file_actions_destroy(&actions) }
        posix_spawnattr_setflags(&attributes, Int16(POSIX_SPAWN_SETPGROUP))
        posix_spawnattr_setpgroup(&attributes, 0)
        posix_spawn_file_actions_adddup2(&actions, handle.fileDescriptor, STDOUT_FILENO)
        posix_spawn_file_actions_adddup2(&actions, handle.fileDescriptor, STDERR_FILENO)
        posix_spawn_file_actions_addopen(&actions, STDIN_FILENO, "/dev/null", O_RDONLY, 0)
        if #available(macOS 26, *) { posix_spawn_file_actions_addchdir(&actions, directory.path) }
        else { posix_spawn_file_actions_addchdir_np(&actions, directory.path) }
        var argv = ([executable] + arguments).map { strdup($0) } + [nil]
        let mergedEnvironment = ProcessInfo.processInfo.environment.merging(environment) { _, new in new }
        var envp = mergedEnvironment.map { strdup("\($0.key)=\($0.value)") } + [nil]
        defer { argv.forEach { free($0) }; envp.forEach { free($0) } }
        var pid: pid_t = 0
        let spawned = posix_spawn(&pid, executable, &actions, &attributes, &argv, &envp)
        guard spawned == 0 else { throw ToolError("无法启动进程：" + String(cString: strerror(spawned))) }
        // Every invocation owns a process group, so timeout also stops its child processes.
        defer { kill(-pid, SIGKILL) }
        let deadline = Date().addingTimeInterval(timeout)
        var stopReason: String?; var status: Int32 = 0
        while true {
            let result = waitpid(pid, &status, WNOHANG)
            if result == pid { break }
            if result < 0 && errno != EINTR { throw ToolError("无法读取进程退出状态。") }
            if Date() > deadline { stopReason = "运行超过 \(timeout) 秒，已停止。" }
            if (try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0 > 2 * 1024 * 1024 { stopReason = "输出超过 2 MB，已停止。" }
            if stopReason != nil {
                kill(-pid, SIGTERM)
                Thread.sleep(forTimeInterval: 0.1)
                kill(-pid, SIGKILL)
                while waitpid(pid, &status, 0) < 0 && errno == EINTR {}
                break
            }
            Thread.sleep(forTimeInterval: 0.025)
        }
        let exitStatus = (status & 0x7f) == 0 ? (status >> 8) & 0xff : 128 + (status & 0x7f)
        let reader = try FileHandle(forReadingFrom: file); defer { try? reader.close() }
        let data = try reader.read(upToCount: 2 * 1024 * 1024) ?? Data()
        let text = String(decoding: data, as: UTF8.self)
        if let stopReason { throw ToolError(stopReason + "\n" + text) }
        return text + "\n\n进程退出码：\(exitStatus)"
    }
}
