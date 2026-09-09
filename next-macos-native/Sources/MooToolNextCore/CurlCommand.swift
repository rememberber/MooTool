import Foundation

/// Imports a single literal request. This parser never starts a shell or reads @files.
public enum CurlCommand {
    public static func parse(_ command: String) throws -> DraftRecord {
        guard command.utf8.count <= 1024 * 1024 else { throw ToolError("cURL 命令超过 1 MB。") }
        let tokens = try tokenize(command)
        guard let first = tokens.first, first == "curl" || first == "/usr/bin/curl" else { throw ToolError("请粘贴以 curl 开头的单条命令。") }
        var result = DraftRecord(), options = HTTPOptions()
        options.followRedirects = false
        var explicitMethod: String?, head = false, get = false, headers: [(String, String)] = []
        var bodyParts: [String] = [], jsonBody = false, dataBody = false, index = 1
        func setHeader(_ name: String, _ value: String) throws {
            guard !value.contains("\r"), !value.contains("\n") else { throw ToolError("请求头参数不能包含换行。") }
            headers.removeAll { $0.0.caseInsensitiveCompare(name) == .orderedSame }; headers.append((name, value))
        }
        func literalData(_ value: String) throws -> String {
            guard !value.hasPrefix("@") else { throw ToolError("不导入 @文件或标准输入；请将文件内容粘贴到正文编辑器。") }; return value
        }
        while index < tokens.count {
            var token = tokens[index]; index += 1
            var inline: String?
            if token.hasPrefix("--"), let equal = token.firstIndex(of: "=") { inline = String(token[token.index(after: equal)...]); token = String(token[..<equal]) }
            else if token.hasPrefix("-"), !token.hasPrefix("--"), token.count > 2,
                    ["-X", "-H", "-d", "-b", "-u", "-A", "-e", "-m"].contains(String(token.prefix(2))) {
                inline = String(token.dropFirst(2)); token = String(token.prefix(2))
            }
            func argument() throws -> String {
                if let inline { return inline }
                guard index < tokens.count else { throw ToolError("\(token) 缺少参数。") }
                defer { index += 1 }; return tokens[index]
            }
            switch token {
            case "-X", "--request": explicitMethod = try argument().uppercased()
            case "--url":
                guard result.option.isEmpty else { throw ToolError("一次只能导入一个 URL。") }; result.option = try argument()
            case "-H", "--header":
                let line = try argument()
                guard !line.contains("\n"), !line.contains("\r") else { throw ToolError("单个请求头不能包含换行。") }
                let pairs = try HTTPFields.headerLines(line)
                guard let pair = pairs.first, !pair.1.isEmpty else { throw ToolError("暂不支持 cURL 的请求头移除语法。") }
                headers.append(pair)
            case "-d", "--data", "--data-ascii", "--data-raw", "--data-binary", "--data-urlencode":
                guard !jsonBody else { throw ToolError("请不要混用 --json 与 --data 选项。") }; dataBody = true
                let value = try argument()
                if token == "--data-urlencode" {
                    if let equal = value.firstIndex(of: "=") {
                        let name = String(value[..<equal]), content = String(value[value.index(after: equal)...])
                        bodyParts.append((name.isEmpty ? "" : name + "=") + HTTPFields.percentEncode(content))
                    } else {
                        guard !value.contains("@") else { throw ToolError("--data-urlencode 不支持文件输入。") }
                        bodyParts.append(HTTPFields.percentEncode(value))
                    }
                } else { bodyParts.append(token == "--data-raw" ? value : try literalData(value)) }
            case "--json":
                guard !dataBody else { throw ToolError("请不要混用 --json 与 --data 选项。") }; jsonBody = true
                bodyParts.append(try literalData(argument()))
            case "-b", "--cookie":
                let value = try argument()
                guard value.contains("=") else { throw ToolError("Cookie 请使用 name=value，不支持 Cookie 文件。") }
                for part in value.components(separatedBy: ";") {
                    guard let equal = part.firstIndex(of: "=") else { throw ToolError("Cookie 格式应为 name=value。") }
                    options.cookies.append(HTTPField(String(part[..<equal]).trimmingCharacters(in: .whitespaces), String(part[part.index(after: equal)...]).trimmingCharacters(in: .whitespaces)))
                }
            case "-u", "--user":
                let value = try argument(); guard value.contains(":") else { throw ToolError("Basic 认证需要 user:password。") }
                try setHeader("Authorization", "Basic " + Data(value.utf8).base64EncodedString())
            case "-A", "--user-agent": try setHeader("User-Agent", argument())
            case "-e", "--referer": try setHeader("Referer", argument())
            case "-m", "--max-time":
                guard let value = Double(try argument()), value.isFinite, (1...120).contains(value) else { throw ToolError("请求超时需在 1 至 120 秒之间。") }; options.timeout = value
            case "-G", "--get": get = true
            case "-I", "--head": head = true
            case "-L", "--location": options.followRedirects = true
            case "--compressed", "-s", "-S", "-sS", "-Ss", "--silent", "--show-error", "-i", "--include", "--show-headers", "-v", "--verbose", "-g", "--globoff":
                guard inline == nil else { throw ToolError("\(token) 不接受参数。") }
            default:
                guard !token.hasPrefix("-") else { throw ToolError("暂不支持 cURL 选项 \(token)，请在导入前移除或手动配置请求。") }
                guard result.option.isEmpty else { throw ToolError("一次只能导入一个 URL。") }; result.option = token
            }
        }
        result.input = bodyParts.joined(separator: jsonBody ? "" : "&")
        if jsonBody {
            if !headers.contains(where: { $0.0.lowercased() == "content-type" }) { headers.append(("Content-Type", "application/json")) }
            if !headers.contains(where: { $0.0.lowercased() == "accept" }) { headers.append(("Accept", "application/json")) }
        } else if dataBody, !headers.contains(where: { $0.0.lowercased() == "content-type" }) { headers.append(("Content-Type", "application/x-www-form-urlencoded")) }
        if get {
            // Percent-encoded payload is already query syntax. Validate it before assigning to URLComponents.
            guard result.input.unicodeScalars.allSatisfy({ $0.value >= 33 && $0.value < 127 }), !result.input.contains("#"), validPercentEscapes(result.input) else { throw ToolError("-G 的数据需为已编码的查询参数，请使用 --data-urlencode。") }
            result.option = try HTTPFields.appendingQuery(result.input, to: result.option); result.input = ""
        }
        result.mode = explicitMethod ?? (head ? "HEAD" : get ? "GET" : (dataBody || jsonBody ? "POST" : "GET"))
        guard result.input.isEmpty || !["GET", "HEAD"].contains(result.mode) else { throw ToolError("原生请求不支持 GET / HEAD 正文；可使用 -G 将数据放入 URL 参数。") }
        result.secondary = headers.map { "\($0.0): \($0.1)" }.joined(separator: "\n")
        result.http = options
        _ = try NetworkServices.request(result)
        return result
    }

    public static func export(_ draft: DraftRecord) throws -> String {
        let request = try NetworkServices.request(draft)
        let options = draft.http ?? HTTPOptions()
        var parts = ["curl", "--globoff", "--request", request.httpMethod ?? "GET", quote(request.url!.absoluteString)]
        if options.followRedirects { parts.append("--location") }
        parts += ["--max-time", String(options.timeout)]
        for (key, value) in (request.allHTTPHeaderFields ?? [:]).sorted(by: { $0.key.lowercased() < $1.key.lowercased() }) { parts += ["--header", quote(key + ": " + value)] }
        if let data = request.httpBody { parts += ["--data-raw", quote(String(decoding: data, as: UTF8.self))] }
        return parts.joined(separator: " ")
    }

    public static func quote(_ value: String) -> String { "'" + value.replacingOccurrences(of: "'", with: "'\"'\"'") + "'" }
    private static func validPercentEscapes(_ text: String) -> Bool {
        let bytes = Array(text.utf8); var i = 0
        while i < bytes.count {
            if bytes[i] == 37 {
                guard i + 2 < bytes.count, UInt8(String(decoding: bytes[(i + 1)...(i + 2)], as: UTF8.self), radix: 16) != nil else { return false }; i += 2
            }; i += 1
        }; return true
    }
    static func tokenize(_ text: String) throws -> [String] {
        let chars = Array(text); var tokens: [String] = [], token = "", quote: Character?, active = false, i = 0
        while i < chars.count {
            let c = chars[i]; i += 1
            guard c != "\0" else { throw ToolError("命令包含空字符。") }
            if quote == "'" { if c == "'" { quote = nil } else { token.append(c) }; continue }
            if c == "\\" {
                guard i < chars.count else { throw ToolError("命令末尾有未完成的转义。") }
                let next = chars[i]; i += 1
                if next == "\n" || next == "\r\n" { continue }
                if quote == "\"", !["$", "`", "\"", "\\"].contains(next) { token.append("\\") }
                token.append(next); active = true; continue
            }
            if c == "$" || c == "`" { throw ToolError("请使用字面量参数：不支持变量、命令替换或 $'…' 引号。") }
            if let current = quote { if c == current { quote = nil } else { token.append(c) }; continue }
            if c == "'" || c == "\"" { quote = c; active = true }
            else if c.isWhitespace { if active { tokens.append(token); token = ""; active = false } }
            else if ";|&<>()".contains(c) || (c == "#" && !active) { throw ToolError("不支持 shell 运算符、管道或注释；URL 和正文请用引号包围。") }
            else { token.append(c); active = true }
        }
        guard quote == nil else { throw ToolError("cURL 参数的引号未闭合。") }
        if active { tokens.append(token) }; return tokens
    }
}
