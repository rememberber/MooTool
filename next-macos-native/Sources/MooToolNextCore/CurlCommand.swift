import Foundation

/// Imports a single literal request. This parser never starts a shell or reads @files.
public enum CurlCommand {
    public static func parse(_ command: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> DraftRecord {
        guard command.utf8.count <= 1024 * 1024 else { throw err("http.curl.commandTooLarge", language) }
        let tokens = try tokenize(command, language: language)
        guard let first = tokens.first, first == "curl" || first == "/usr/bin/curl" else { throw err("http.curl.mustStartWithCurl", language) }
        var result = DraftRecord(), options = HTTPOptions()
        options.followRedirects = false
        var explicitMethod: String?, head = false, get = false, headers: [(String, String)] = []
        var bodyParts: [String] = [], jsonBody = false, dataBody = false, index = 1
        func setHeader(_ name: String, _ value: String) throws {
            guard !value.contains("\r"), !value.contains("\n") else { throw err("http.curl.headerArgNewline", language) }
            headers.removeAll { $0.0.caseInsensitiveCompare(name) == .orderedSame }; headers.append((name, value))
        }
        func literalData(_ value: String) throws -> String {
            guard !value.hasPrefix("@") else { throw err("http.curl.noAtFile", language) }; return value
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
                guard index < tokens.count else {
                    throw ToolError(String(format: AppLocalization.string("http.curl.missingArgument", language: language), token))
                }
                defer { index += 1 }; return tokens[index]
            }
            switch token {
            case "-X", "--request": explicitMethod = try argument().uppercased()
            case "--url":
                guard result.option.isEmpty else { throw err("http.curl.singleUrlOnly", language) }; result.option = try argument()
            case "-H", "--header":
                let line = try argument()
                guard !line.contains("\n"), !line.contains("\r") else { throw err("http.curl.headerLineNewline", language) }
                let pairs = try HTTPFields.headerLines(line, language: language)
                guard let pair = pairs.first, !pair.1.isEmpty else { throw err("http.curl.headerRemoveUnsupported", language) }
                headers.append(pair)
            case "-d", "--data", "--data-ascii", "--data-raw", "--data-binary", "--data-urlencode":
                guard !jsonBody else { throw err("http.curl.jsonDataConflict", language) }; dataBody = true
                let value = try argument()
                if token == "--data-urlencode" {
                    if let equal = value.firstIndex(of: "=") {
                        let name = String(value[..<equal]), content = String(value[value.index(after: equal)...])
                        bodyParts.append((name.isEmpty ? "" : name + "=") + HTTPFields.percentEncode(content))
                    } else {
                        guard !value.contains("@") else { throw err("http.curl.dataUrlencodeNoFile", language) }
                        bodyParts.append(HTTPFields.percentEncode(value))
                    }
                } else { bodyParts.append(token == "--data-raw" ? value : try literalData(value)) }
            case "--json":
                guard !dataBody else { throw err("http.curl.jsonDataConflict", language) }; jsonBody = true
                bodyParts.append(try literalData(argument()))
            case "-b", "--cookie":
                let value = try argument()
                guard value.contains("=") else { throw err("http.curl.cookieNameValue", language) }
                for part in value.components(separatedBy: ";") {
                    guard let equal = part.firstIndex(of: "=") else { throw err("http.curl.cookieFormat", language) }
                    options.cookies.append(HTTPField(String(part[..<equal]).trimmingCharacters(in: .whitespaces), String(part[part.index(after: equal)...]).trimmingCharacters(in: .whitespaces)))
                }
            case "-u", "--user":
                let value = try argument(); guard value.contains(":") else { throw err("http.curl.basicAuthUserPassword", language) }
                try setHeader("Authorization", "Basic " + Data(value.utf8).base64EncodedString())
            case "-A", "--user-agent": try setHeader("User-Agent", argument())
            case "-e", "--referer": try setHeader("Referer", argument())
            case "-m", "--max-time":
                guard let value = Double(try argument()), value.isFinite, (1...120).contains(value) else { throw err("http.error.timeoutRange", language) }; options.timeout = value
            case "-G", "--get": get = true
            case "-I", "--head": head = true
            case "-L", "--location": options.followRedirects = true
            case "--compressed", "-s", "-S", "-sS", "-Ss", "--silent", "--show-error", "-i", "--include", "--show-headers", "-v", "--verbose", "-g", "--globoff":
                guard inline == nil else {
                    throw ToolError(String(format: AppLocalization.string("http.curl.optionNoArgument", language: language), token))
                }
            default:
                guard !token.hasPrefix("-") else {
                    throw ToolError(String(format: AppLocalization.string("http.curl.unsupportedOption", language: language), token))
                }
                guard result.option.isEmpty else { throw err("http.curl.singleUrlOnly", language) }; result.option = token
            }
        }
        result.input = bodyParts.joined(separator: jsonBody ? "" : "&")
        if jsonBody {
            if !headers.contains(where: { $0.0.lowercased() == "content-type" }) { headers.append(("Content-Type", "application/json")) }
            if !headers.contains(where: { $0.0.lowercased() == "accept" }) { headers.append(("Accept", "application/json")) }
        } else if dataBody, !headers.contains(where: { $0.0.lowercased() == "content-type" }) { headers.append(("Content-Type", "application/x-www-form-urlencoded")) }
        if get {
            // Percent-encoded payload is already query syntax. Validate it before assigning to URLComponents.
            guard result.input.unicodeScalars.allSatisfy({ $0.value >= 33 && $0.value < 127 }), !result.input.contains("#"), validPercentEscapes(result.input) else { throw err("http.curl.getEncodedQuery", language) }
            result.option = try HTTPFields.appendingQuery(result.input, to: result.option, language: language); result.input = ""
        }
        result.mode = explicitMethod ?? (head ? "HEAD" : get ? "GET" : (dataBody || jsonBody ? "POST" : "GET"))
        guard result.input.isEmpty || !["GET", "HEAD"].contains(result.mode) else { throw err("http.curl.getHeadNoBody", language) }
        result.secondary = headers.map { "\($0.0): \($0.1)" }.joined(separator: "\n")
        result.http = options
        _ = try NetworkServices.request(result, language: language)
        return result
    }

    public static func export(_ draft: DraftRecord, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let request = try NetworkServices.request(draft, language: language)
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
    static func tokenize(_ text: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [String] {
        let chars = Array(text); var tokens: [String] = [], token = "", quote: Character?, active = false, i = 0
        while i < chars.count {
            let c = chars[i]; i += 1
            guard c != "\0" else { throw err("http.curl.nullChar", language) }
            if quote == "'" { if c == "'" { quote = nil } else { token.append(c) }; continue }
            if c == "\\" {
                guard i < chars.count else { throw err("http.curl.incompleteEscape", language) }
                let next = chars[i]; i += 1
                if next == "\n" || next == "\r\n" { continue }
                if quote == "\"", !["$", "`", "\"", "\\"].contains(next) { token.append("\\") }
                token.append(next); active = true; continue
            }
            if c == "$" || c == "`" { throw err("http.curl.noVariables", language) }
            if let current = quote { if c == current { quote = nil } else { token.append(c) }; continue }
            if c == "'" || c == "\"" { quote = c; active = true }
            else if c.isWhitespace { if active { tokens.append(token); token = ""; active = false } }
            else if ";|&<>()".contains(c) || (c == "#" && !active) { throw err("http.curl.noShellOps", language) }
            else { token.append(c); active = true }
        }
        guard quote == nil else { throw err("http.curl.unclosedQuote", language) }
        if active { tokens.append(token) }; return tokens
    }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }
}
