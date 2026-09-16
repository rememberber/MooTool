import Foundation
import CoreFoundation
import CryptoKit
import Yams

public enum TextServices {
    public static func json(_ text: String, pretty: Bool = true, sorted: Bool = true, indent: Int = 2, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        guard [2, 4].contains(indent) else { throw ToolError(AppLocalization.string("json.error.indentInvalid", language: language)) }
        let object = try JSONSerialization.jsonObject(with: Data(text.utf8), options: [.fragmentsAllowed])
        let result = try serialize(object, pretty: pretty, sorted: sorted)
        guard pretty && indent == 4 else { return result }
        return result.components(separatedBy: "\n").map { line in String(repeating: " ", count: line.prefix(while: { $0 == " " }).count) + line }.joined(separator: "\n")
    }
    public static func serialize(_ object: Any, pretty: Bool = true, sorted: Bool = true) throws -> String {
        var options: JSONSerialization.WritingOptions = [.fragmentsAllowed, .withoutEscapingSlashes]
        if pretty { options.insert(.prettyPrinted) }; if sorted { options.insert(.sortedKeys) }
        return String(decoding: try JSONSerialization.data(withJSONObject: object, options: options), as: UTF8.self)
    }
    /// Deliberately bounded path grammar: $.key[0].nested, with JSON Pointer for unusual keys.
    public static func jsonPath(_ text: String, path: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        var object = try JSONSerialization.jsonObject(with: Data(text.utf8), options: [.fragmentsAllowed])
        var keys: [String]
        if path.isEmpty { return try serialize(object) }
        if path.hasPrefix("/") {
            keys = try path.dropFirst().components(separatedBy: "/").map { key in
                guard key.range(of: #"~(?![01])"#, options: .regularExpression) == nil else {
                    throw ToolError(AppLocalization.string("json.error.pathPointerEscape", language: language))
                }
                return key.replacingOccurrences(of: "~1", with: "/").replacingOccurrences(of: "~0", with: "~")
            }
        } else {
            guard path.hasPrefix("$") else { throw ToolError(AppLocalization.string("json.error.pathFormat", language: language)) }
            let suffix = String(path.dropFirst())
            let regex = try NSRegularExpression(pattern: #"(?:\.([\p{L}\p{N}_-]+)|\[(\d+)\])"#)
            let ns = suffix as NSString
            let matches = regex.matches(in: suffix, range: NSRange(location: 0, length: ns.length))
            guard matches.reduce(0, { $0 + $1.range.length }) == ns.length else {
                throw ToolError(AppLocalization.string("json.error.pathLiteralOnly", language: language))
            }
            keys = matches.map { ns.substring(with: $0.range(at: $0.range(at: 1).location == NSNotFound ? 2 : 1)) }
        }
        for key in keys {
            if let dict = object as? [String: Any], let child = dict[key] { object = child }
            else if let array = object as? [Any], key == "0" || (key.first != "0" && key.allSatisfy({ $0.isASCII && $0.isNumber })), let index = Int(key), array.indices.contains(index) { object = array[index] }
            else { throw ToolError(AppLocalization.format("json.error.pathNodeMissing", language: language, replacements: ["key": key])) }
        }
        return try serialize(object)
    }
    public static func encode(_ text: String, format: String, decode: Bool, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        switch (format, decode) {
        case ("Base64", false): return Data(text.utf8).base64EncodedString()
        case ("Base64", true):
            guard let data = Data(base64Encoded: text.filter { !$0.isWhitespace }), let result = String(data: data, encoding: .utf8) else {
                throw ToolError(AppLocalization.string("encode.error.base64Utf8", language: language))
            }; return result
        case ("Base32", false): return Base32Codec.encode(Data(text.utf8))
        case ("Base32", true):
            guard let result = String(data: try Base32Codec.decode(text), encoding: .utf8) else {
                throw ToolError(AppLocalization.string("encode.error.base32Utf8", language: language))
            }
            return result
        case ("URL", false): return text.addingPercentEncoding(withAllowedCharacters: CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~")) ?? ""
        case ("URL", true): guard let result = text.removingPercentEncoding else { throw ToolError(AppLocalization.string("encode.error.urlPercent", language: language)) }; return result
        case ("Hex", false): return Data(text.utf8).hex
        case ("Hex", true): guard let result = String(data: try Data(hex: text, language: language), encoding: .utf8) else { throw ToolError(AppLocalization.string("encode.error.hexUtf8", language: language)) }; return result
        case ("Unicode", false): return text.utf16.map { String(format: "\\u%04x", $0) }.joined()
        case ("Unicode", true):
            guard let result = try JSONSerialization.jsonObject(with: Data(("\"" + text + "\"").utf8), options: [.fragmentsAllowed]) as? String else {
                throw ToolError(AppLocalization.string("encode.error.unicodeEscape", language: language))
            }; return result
        case ("HTML", false):
            return text.replacingOccurrences(of: "&", with: "&amp;").replacingOccurrences(of: "<", with: "&lt;").replacingOccurrences(of: ">", with: "&gt;").replacingOccurrences(of: "\"", with: "&quot;").replacingOccurrences(of: "'", with: "&#39;")
        case ("HTML", true):
            let regex = try NSRegularExpression(pattern: #"&(?:#x([0-9a-fA-F]+)|#([0-9]+)|(amp|lt|gt|quot|apos));"#)
            var result = text; let ns = text as NSString
            for match in regex.matches(in: text, range: NSRange(location: 0, length: ns.length)).reversed() {
                var replacement: String?
                if match.range(at: 1).location != NSNotFound { replacement = UInt32(ns.substring(with: match.range(at: 1)), radix: 16).flatMap(UnicodeScalar.init).map(String.init) }
                else if match.range(at: 2).location != NSNotFound { replacement = UInt32(ns.substring(with: match.range(at: 2))).flatMap(UnicodeScalar.init).map(String.init) }
                else { replacement = ["amp": "&", "lt": "<", "gt": ">", "quot": "\"", "apos": "'"][ns.substring(with: match.range(at: 3))] }
                if let replacement, let range = Range(match.range, in: result) { result.replaceSubrange(range, with: replacement) }
            }; return result
        default: throw ToolError(AppLocalization.string("encode.error.unsupportedFormat", language: language))
        }
    }
    public static func digest(_ text: String, algorithm: String, key: String = "", language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let data = Data(text.utf8)
        switch algorithm {
        case "MD5": return Insecure.MD5.hash(data: data).map { String(format: "%02x", $0) }.joined()
        case "SHA-1": return Insecure.SHA1.hash(data: data).map { String(format: "%02x", $0) }.joined()
        case "SHA-256": return SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
        case "SHA-384": return SHA384.hash(data: data).map { String(format: "%02x", $0) }.joined()
        case "SHA-512": return SHA512.hash(data: data).map { String(format: "%02x", $0) }.joined()
        case "SM3": return CryptoServices.sm3(text)
        case "HMAC-SHA256": return Data(HMAC<SHA256>.authenticationCode(for: data, using: SymmetricKey(data: Data(key.utf8)))).hex
        case "UUID": return UUID().uuidString.lowercased()
        case "随机 32 字节":
            var generator = SystemRandomNumberGenerator()
            return Data((0..<32).map { _ in UInt8.random(in: .min ... .max, using: &generator) }).hex
        case "AES-GCM 加密", "AES-GCM 解密":
            let keyData = try Data(hex: key, language: language)
            guard [16, 24, 32].contains(keyData.count) else {
                throw ToolError(AppLocalization.string("textCrypto.error.aesKeyHex", language: language))
            }
            let symmetricKey = SymmetricKey(data: keyData)
            if algorithm == "AES-GCM 加密" {
                return try AES.GCM.seal(data, using: symmetricKey).combined!.base64EncodedString()
            }
            guard let bytes = Data(base64Encoded: text) else {
                throw ToolError(AppLocalization.string("textCrypto.error.cipherBase64", language: language))
            }
            let decrypted = try AES.GCM.open(AES.GCM.SealedBox(combined: bytes), using: symmetricKey)
            guard let output = String(data: decrypted, encoding: .utf8) else {
                throw ToolError(AppLocalization.string("textCrypto.error.decryptUtf8", language: language))
            }
            return output
        default: throw ToolError(AppLocalization.string("textCrypto.error.unknownAlgorithm", language: language))
        }
    }
    public static func regex(_ input: String, pattern: String, replacement: String? = nil, flags: String = "", language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        var options: NSRegularExpression.Options = []
        if flags.contains("i") { options.insert(.caseInsensitive) }
        if flags.contains("m") { options.insert(.anchorsMatchLines) }
        if flags.contains("s") { options.insert(.dotMatchesLineSeparators) }
        let regex = try NSRegularExpression(pattern: pattern, options: options)
        let range = NSRange(input.startIndex..., in: input)
        let ns = input as NSString
        var matches: [NSTextCheckingResult] = []
        let deadline = Date().addingTimeInterval(2)
        var exceeded = false
        regex.enumerateMatches(in: input, options: [.reportProgress], range: range) { match, _, stop in
            if Date() > deadline || matches.count >= 10_000 { exceeded = true; stop.pointee = true; return }
            if let match { matches.append(match) }
        }
        guard !exceeded else { throw ToolError(AppLocalization.string("regex.error.limits", language: language)) }
        if let replacement {
            let output = NSMutableString(string: input)
            for match in matches.reversed() {
                let text = regex.replacementString(for: match, in: input, offset: 0, template: replacement)
                output.replaceCharacters(in: match.range, with: text)
            }
            return output as String
        }
        return try serialize(matches.enumerated().map { index, match -> [String: Any] in
            ["match": index + 1, "text": ns.substring(with: match.range), "utf16Offset": match.range.location,
             "groups": (1..<match.numberOfRanges).map { match.range(at: $0).location == NSNotFound ? NSNull() as Any : ns.substring(with: match.range(at: $0)) }]
        })
    }
    public static func config(_ input: String, from: String, to: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let object: Any
        switch from {
        case "JSON": object = try JSONSerialization.jsonObject(with: Data(input.utf8), options: [.fragmentsAllowed])
        case "YAML": object = try Yams.load(yaml: input) ?? NSNull()
        case "Properties":
            var root: [String: Any] = [:]
            for line in input.components(separatedBy: .newlines) {
                let line = line.trimmingCharacters(in: .whitespaces)
                if line.isEmpty || line.hasPrefix("#") || line.hasPrefix("!") { continue }
                guard let separator = line.firstIndex(where: { $0 == "=" || $0 == ":" }) else {
                    throw ToolError(AppLocalization.string("config.error.propertiesKeyValue", language: language))
                }
                let key = String(line[..<separator]).trimmingCharacters(in: .whitespaces)
                guard !key.isEmpty, !key.contains("\\"), !line.hasSuffix("\\") else {
                    throw ToolError(AppLocalization.string("config.error.propertiesInvalidKey", language: language))
                }
                let value = String(line[line.index(after: separator)...]).trimmingCharacters(in: .whitespaces)
                guard !value.contains("\\") else { throw ToolError(AppLocalization.string("config.error.propertiesEscapeValue", language: language)) }
                try insert(value, keys: key.components(separatedBy: "."), into: &root, language: language)
            }; object = root
        default: throw ToolError(AppLocalization.string("config.error.unknownInputFormat", language: language))
        }
        switch to {
        case "JSON": return try serialize(object)
        case "YAML": return try Yams.dump(object: object, sortKeys: true)
        case "Properties":
            var lines: [String] = []
            func flatten(_ value: Any, _ prefix: String) throws {
                if let dict = value as? [String: Any] {
                    for key in dict.keys.sorted() {
                        guard !key.contains(where: { ".=:\\\n".contains($0) }) else {
                            throw ToolError(AppLocalization.string("config.error.propertiesKeyChars", language: language))
                        }
                        try flatten(dict[key]!, prefix.isEmpty ? key : prefix + "." + key)
                    }
                } else {
                    guard !(value is [Any]), !(value is NSNull), !prefix.isEmpty else {
                        throw ToolError(AppLocalization.string("config.error.propertiesScalarOnly", language: language))
                    }
                    let text: String
                    if let number = value as? NSNumber, CFGetTypeID(number) == CFBooleanGetTypeID() { text = number.boolValue ? "true" : "false" }
                    else { text = String(describing: value) }
                    guard !text.contains("\n"), !text.contains("\r"), !text.contains("\\"), text == text.trimmingCharacters(in: .whitespaces) else {
                        throw ToolError(AppLocalization.string("config.error.propertiesMultiline", language: language))
                    }
                    lines.append("\(prefix)=\(text)")
                }
            }
            try flatten(object, ""); return lines.joined(separator: "\n")
        default: throw ToolError(AppLocalization.string("config.error.unknownOutputFormat", language: language))
        }
    }
    private static func insert(_ value: String, keys: [String], into root: inout [String: Any], language: AppLanguage) throws {
        guard let first = keys.first, !first.isEmpty else { throw ToolError(AppLocalization.string("config.error.propertiesEmptyPath", language: language)) }
        if keys.count == 1 {
            guard root[first] == nil else { throw ToolError(String(format: AppLocalization.string("config.error.propertiesDuplicate", language: language), first)) }
            root[first] = value
        } else {
            guard root[first] == nil || root[first] is [String: Any] else {
                throw ToolError(String(format: AppLocalization.string("config.error.propertiesPathConflict", language: language), first))
            }
            var child = root[first] as? [String: Any] ?? [:]
            try insert(value, keys: Array(keys.dropFirst()), into: &child, language: language); root[first] = child
        }
    }
    public static func formatXML(_ text: String) throws -> String {
        let document = try XMLDocument(xmlString: text, options: [.nodePreserveCDATA, .nodeLoadExternalEntitiesNever])
        return String(decoding: document.xmlData(options: [.nodePrettyPrint]), as: UTF8.self)
    }
    public static func diff(_ original: String, _ revised: String) -> String {
        let a = original.components(separatedBy: "\n"), b = revised.components(separatedBy: "\n")
        let changes = b.difference(from: a)
        var removed: [Int: String] = [:], added: [Int: String] = [:]
        for change in changes {
            switch change { case .remove(let i, let text, _): removed[i] = text
            case .insert(let i, let text, _): added[i] = text }
        }
        var result: [String] = []; var i = 0; var j = 0
        while i < a.count || j < b.count {
            if let line = removed[i] { result.append("− " + line); i += 1 }
            else if let line = added[j] { result.append("+ " + line); j += 1 }
            else if i < a.count && j < b.count { result.append("  " + a[i]); i += 1; j += 1 }
            else { break }
        }
        return "新增 \(added.count) 行 · 删除 \(removed.count) 行\n\n" + result.joined(separator: "\n")
    }
}

public extension Data {
    var hex: String { map { String(format: "%02x", $0) }.joined() }
    init(hex: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        let chars = Array(hex.filter { !$0.isWhitespace })
        guard chars.count.isMultiple(of: 2) else { throw ToolError(AppLocalization.string("hex.error.evenLength", language: language)) }
        var bytes: [UInt8] = []
        for i in stride(from: 0, to: chars.count, by: 2) {
            guard let byte = UInt8(String(chars[i...i+1]), radix: 16) else { throw ToolError(AppLocalization.string("hex.error.invalidChar", language: language)) }
            bytes.append(byte)
        }
        self.init(bytes)
    }
}
