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
    public static func request(
        _ draft: DraftRecord,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> URLRequest {
        let options = draft.http ?? HTTPOptions()
        guard options.timeout.isFinite, (1...120).contains(options.timeout) else { throw err("http.error.timeoutRange", language) }
        for fields in [options.params, options.cookies, options.form] { guard fields.count <= 1000 else { throw err("http.error.fieldLimit", language) } }
        guard options.multipart.count <= 1000 else { throw err("http.error.multipartLimit", language) }
        let url = try HTTPFields.appendingQuery(HTTPFields.query(options.params), to: draft.option)
        var body = draft.input
        var bodyData: Data?
        var multipartType: String?
        if options.bodyKind == .form { body = HTTPFields.query(options.form) }
        if options.bodyKind == .none { body = "" }
        if options.bodyKind == .multipart {
            body = ""
            let encoded = try HTTPMultipartBuilder.encode(options.multipart)
            bodyData = encoded.0
            multipartType = encoded.1
        }
        guard body.utf8.count + (bodyData?.count ?? 0) + draft.secondary.utf8.count <= 10 * 1024 * 1024 else { throw err("http.error.bodyTooLarge", language) }
        var result = try request(method: draft.mode.isEmpty ? "GET" : draft.mode, url: url, headers: draft.secondary, body: body, bodyData: bodyData, language: language)
        result.timeoutInterval = options.timeout
        if let multipartType {
            if result.value(forHTTPHeaderField: "Content-Type") == nil { result.setValue(multipartType, forHTTPHeaderField: "Content-Type") }
        } else if result.value(forHTTPHeaderField: "Content-Type") == nil {
            if options.bodyKind == .json { result.setValue("application/json", forHTTPHeaderField: "Content-Type") }
            if options.bodyKind == .form { result.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type") }
        }
        let cookies = try HTTPFields.active(options.cookies).map { field -> String in
            let nameAllowed = CharacterSet(charactersIn: "!#$%&'*+-.^_`|~0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
            guard field.name.unicodeScalars.allSatisfy(nameAllowed.contains), field.value.unicodeScalars.allSatisfy({ scalar in
                let n = scalar.value; return n == 33 || (35...43).contains(n) || (45...58).contains(n) || (60...91).contains(n) || (93...126).contains(n)
            }) else { throw err("http.error.cookieInvalid", language) }
            return field.name + "=" + field.value
        }
        if !cookies.isEmpty {
            let values = ([result.value(forHTTPHeaderField: "Cookie")] + cookies.map(Optional.some)).compactMap { $0 }.filter { !$0.isEmpty }
            result.setValue(values.joined(separator: "; "), forHTTPHeaderField: "Cookie")
        }
        return result
    }

    public static func request(
        method: String,
        url: String,
        headers: String,
        body: String,
        bodyData: Data? = nil,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> URLRequest {
        guard let url = URL(string: url.trimmingCharacters(in: .whitespacesAndNewlines)),
              ["http", "https"].contains(url.scheme?.lowercased() ?? ""), let host = url.host, !host.isEmpty else { throw err("http.error.urlInvalid", language) }
        guard ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"].contains(method) else { throw err("http.error.methodUnsupported", language) }
        var request = URLRequest(url: url, timeoutInterval: 30); request.httpMethod = method
        for (key, value) in try HTTPFields.headerLines(headers) {
            if key.lowercased() == "cookie", let previous = request.value(forHTTPHeaderField: key) { request.setValue(previous + "; " + value, forHTTPHeaderField: key) }
            else { request.addValue(value, forHTTPHeaderField: key) }
        }
        if let bodyData, !bodyData.isEmpty, method != "GET", method != "HEAD" { request.httpBody = bodyData }
        else if !body.isEmpty && method != "GET" && method != "HEAD" { request.httpBody = Data(body.utf8) }
        return request
    }

    public static func send(
        _ request: URLRequest,
        followRedirects: Bool = true,
        proxy: NetworkProxySettings? = nil,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) async throws -> HTTPResponse {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForResource = request.timeoutInterval
        if let dictionary = (proxy ?? NetworkProxySettings.current()).connectionProxyDictionary() {
            config.connectionProxyDictionary = dictionary
        }
        let session = URLSession(configuration: config, delegate: RedirectPolicy(follow: followRedirects), delegateQueue: nil)
        defer { session.invalidateAndCancel() }
        let start = Date(); let (stream, response) = try await session.bytes(for: request)
        var data = Data()
        for try await byte in stream {
            if data.count >= 10 * 1024 * 1024 { throw err("http.error.responseTooLarge", language) }
            data.append(byte)
        }
        guard let http = response as? HTTPURLResponse else { throw err("http.error.noHttpResponse", language) }
        let fields = http.allHeaderFields.reduce(into: [String: String]()) { $0[String(describing: $1.key)] = String(describing: $1.value) }
        let formatter = ISO8601DateFormatter()
        let cookies = HTTPCookie.cookies(withResponseHeaderFields: fields, for: http.url ?? request.url!).map { cookie in
            var lines = ["\(cookie.name)=\(cookie.value)", "  Domain: \(cookie.domain) · Path: \(cookie.path)"]
            if let expires = cookie.expiresDate { lines.append("  Expires: \(formatter.string(from: expires))") }
            if cookie.isSecure { lines.append("  Secure") }
            if cookie.isHTTPOnly { lines.append("  HttpOnly") }
            return lines.joined(separator: "\n")
        }.joined(separator: "\n\n")
        let binaryPrefix = AppLocalization.string("http.error.binaryResponse", language: language)
        return HTTPResponse(status: http.statusCode,
            headers: http.allHeaderFields.map { "\($0.key): \($0.value)" }.sorted().joined(separator: "\n"),
            body: String(data: data, encoding: .utf8) ?? binaryPrefix + "\n" + data.base64EncodedString(),
            elapsed: Date().timeIntervalSince(start), bytes: data.count, cookies: cookies, url: http.url?.absoluteString ?? "")
    }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }
}

private final class RedirectPolicy: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    let follow: Bool
    init(follow: Bool) { self.follow = follow }
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(follow ? request : nil) }
}

/// Runs only on explicit user action. Arguments are never interpolated into a shell command.
public enum ProcessRunner {
    public static func runSync(
        executable: String,
        arguments: [String],
        environment: [String: String] = [:],
        timeout: TimeInterval = 20,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        try runSynchronously(executable: executable, arguments: arguments, environment: environment, timeout: timeout, language: language)
    }

    public static func run(
        executable: String,
        arguments: [String],
        environment: [String: String] = [:],
        timeout: TimeInterval = 20,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) async throws -> String {
        try await Task.detached(priority: .userInitiated) {
            try runSynchronously(executable: executable, arguments: arguments, environment: environment, timeout: timeout, language: language)
        }.value
    }

    private static func runSynchronously(
        executable: String,
        arguments: [String],
        environment: [String: String],
        timeout: TimeInterval,
        language: AppLanguage
    ) throws -> String {
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
        guard spawned == 0 else {
            throw ToolError(String(format: AppLocalization.string("process.error.spawnFailed", language: language), String(cString: strerror(spawned))))
        }
        defer { kill(-pid, SIGKILL) }
        let deadline = Date().addingTimeInterval(timeout)
        var stopReason: String?; var status: Int32 = 0
        while true {
            let result = waitpid(pid, &status, WNOHANG)
            if result == pid { break }
            if result < 0 && errno != EINTR { throw ToolError(AppLocalization.string("process.error.waitFailed", language: language)) }
            if Date() > deadline { stopReason = String(format: AppLocalization.string("process.error.timeout", language: language), timeout) }
            if (try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0 > 2 * 1024 * 1024 {
                stopReason = AppLocalization.string("process.error.outputTooLarge", language: language)
            }
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
        let footer = String(format: AppLocalization.string("process.error.exitCode", language: language), exitStatus)
        return text + "\n\n" + footer
    }
}
