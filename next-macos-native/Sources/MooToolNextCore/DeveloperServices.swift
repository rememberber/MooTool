import Foundation

public enum DeveloperServices {
    public static func timestamp(_ input: String, zone: String) throws -> String {
        guard let timezone = TimeZone(identifier: zone) else { throw ToolError("无效时区：\(zone)") }
        let value = input.trimmingCharacters(in: .whitespacesAndNewlines)
        let date: Date
        if let number = Double(value), number.isFinite {
            date = Date(timeIntervalSince1970: abs(number) >= 100_000_000_000 ? number / 1000 : number)
        } else {
            let iso = ISO8601DateFormatter()
            iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            let formatter = DateFormatter(); formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.timeZone = timezone; formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            formatter.isLenient = false
            if let parsed = iso.date(from: value) ?? ISO8601DateFormatter().date(from: value) ?? formatter.date(from: value) { date = parsed }
            else { throw ToolError("请输入秒/毫秒时间戳、ISO 8601 或 yyyy-MM-dd HH:mm:ss。") }
        }
        guard abs(date.timeIntervalSince1970) < 253_402_300_800 else { throw ToolError("日期超出支持范围。") }
        let formatter = DateFormatter(); formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timezone; formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS ZZZZZ"
        let iso = ISO8601DateFormatter(); iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return "日期    \(formatter.string(from: date))\n时区    \(zone)\n秒      \(Int64(floor(date.timeIntervalSince1970)))\n毫秒    \(Int64((date.timeIntervalSince1970 * 1000).rounded()))\nUTC     \(iso.string(from: date))"
    }
    public static func protobuf(_ text: String, base64: Bool) throws -> String {
        let data: Data
        if base64 { guard let decoded = Data(base64Encoded: text.filter { !$0.isWhitespace }) else { throw ToolError("无效 Base64。") }; data = decoded }
        else { data = try Data(hex: text) }
        let bytes = Array(data); var position = 0; var fields: [[String: Any]] = []
        func varint() throws -> UInt64 {
            var value: UInt64 = 0
            for shift in stride(from: 0, through: 63, by: 7) {
                guard position < bytes.count else { throw ToolError("Protobuf varint 被截断。") }
                let byte = bytes[position]; position += 1
                guard shift < 63 || byte <= 1 else { throw ToolError("Protobuf varint 溢出。") }
                value |= UInt64(byte & 127) << shift
                if byte & 128 == 0 { return value }
            }; throw ToolError("Protobuf varint 过长。")
        }
        func take(_ count: Int) throws -> Data {
            guard count >= 0, count <= bytes.count - position else { throw ToolError("Protobuf 字段长度超出数据范围。") }
            defer { position += count }; return Data(bytes[position..<position+count])
        }
        while position < bytes.count {
            let tag = try varint(), field = tag >> 3, wire = tag & 7
            guard field > 0, field <= 536_870_911 else { throw ToolError("无效 Protobuf 字段号。") }
            var item: [String: Any] = ["field": field, "wireType": wire]
            switch wire {
            case 0: item["varint"] = String(try varint())
            case 1, 5: item["hexLittleEndian"] = try take(wire == 1 ? 8 : 4).hex
            case 2:
                let length = try varint()
                guard length <= UInt64(bytes.count - position) else { throw ToolError("Protobuf 字段长度超出数据范围。") }
                let payload = try take(Int(length)); item["hex"] = payload.hex; item["utf8"] = String(data: payload, encoding: .utf8)
            default: throw ToolError("不支持已废弃的 group wire 类型或无效 wire 类型：\(wire)")
            }
            fields.append(item)
        }
        return try TextServices.serialize(fields)
    }
    public static func userAgent(_ text: String) throws -> String {
        func capture(_ pattern: String) -> String? {
            guard let regex = try? NSRegularExpression(pattern: pattern), let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)) else { return nil }
            return (text as NSString).substring(with: match.range(at: 1)).replacingOccurrences(of: "_", with: ".")
        }
        var browser = "未知", version = "未知"
        for (name, pattern) in [("Edge", #"(?:Edg|EdgiOS|EdgA)/([\d.]+)"#), ("Opera", #"OPR/([\d.]+)"#), ("Firefox", #"(?:Firefox|FxiOS)/([\d.]+)"#), ("Chrome", #"(?:Chrome|CriOS)/([\d.]+)"#), ("Safari", #"Version/([\d.]+).*Safari"#)] {
            if let match = capture(pattern) { browser = name; version = match; break }
        }
        let os: String
        if let version = capture(#"(?:iPhone OS|CPU OS) ([\d_]+)"#) { os = "iOS / iPadOS " + version }
        else if let version = capture(#"Android ([\d.]+)"#) { os = "Android " + version }
        else if let version = capture(#"Mac OS X ([\d_]+)"#) { os = "macOS " + version }
        else if let version = capture(#"Windows NT ([\d.]+)"#) { os = "Windows NT " + version }
        else if text.contains("Linux") { os = "Linux" } else { os = "未知" }
        return try TextServices.serialize(["browser": browser, "version": version, "os": os,
            "device": text.contains("iPad") ? "Tablet" : (text.contains("Mobile") || text.contains("Android") ? "Mobile" : "Desktop"),
            "note": "基于 UA 字符串识别，伪装或未收录的 UA 可能无法准确识别。"])
    }
    public static func pageIndices(_ text: String, count: Int) throws -> [Int] {
        if text.trimmingCharacters(in: .whitespaces).isEmpty { return Array(0..<count) }
        var result: [Int] = []
        for part in text.split(separator: ",", omittingEmptySubsequences: false) {
            let bounds = part.trimmingCharacters(in: .whitespaces).split(separator: "-", omittingEmptySubsequences: false)
            guard (1...2).contains(bounds.count), let start = Int(bounds[0]), let end = Int(bounds.last!), start > 0, end >= start, end <= count else { throw ToolError("页码需为 1–\(count)，例如 1-3,5。") }
            result += (start...end).map { $0 - 1 }
        }; return result
    }
}

public struct CronExpression {
    private let fields: [Set<Int>]
    private let anyDay: Bool
    private let anyWeekday: Bool
    public init(_ input: String) throws {
        let parts = input.split(whereSeparator: \.isWhitespace).map(String.init)
        guard parts.count == 5 else { throw ToolError("使用五段 Cron：分 时 日 月 周；支持 *、范围、列表和 / 步长。") }
        anyDay = parts[2].hasPrefix("*"); anyWeekday = parts[4].hasPrefix("*")
        fields = try zip(parts, [0...59, 0...23, 1...31, 1...12, 0...7]).map { try Self.parse($0, range: $1) }
    }
    private static func parse(_ value: String, range: ClosedRange<Int>) throws -> Set<Int> {
        var output = Set<Int>()
        for item in value.split(separator: ",", omittingEmptySubsequences: false) {
            let pair = item.split(separator: "/", omittingEmptySubsequences: false)
            guard (1...2).contains(pair.count), let step = pair.count == 2 ? Int(pair[1]) : 1, step > 0, step <= range.upperBound + 1 else { throw ToolError("无效 Cron 步长。") }
            let bounds = pair[0].split(separator: "-", omittingEmptySubsequences: false)
            let lower: Int; let upper: Int
            if pair[0] == "*" { lower = range.lowerBound; upper = range.upperBound }
            else if bounds.count == 1, let number = Int(bounds[0]) { lower = number; upper = pair.count == 2 ? range.upperBound : number }
            else if bounds.count == 2, let a = Int(bounds[0]), let b = Int(bounds[1]) { lower = a; upper = b }
            else { throw ToolError("无效 Cron 字段：\(item)") }
            guard range.contains(lower), range.contains(upper), lower <= upper else { throw ToolError("Cron 字段越界：\(item)") }
            output.formUnion(stride(from: lower, through: upper, by: step))
        }; return output
    }
    public func next(after date: Date, count: Int = 10, timeZone: TimeZone = .current) throws -> [Date] {
        guard (1...100).contains(count) else { throw ToolError("执行次数需在 1–100 之间。") }
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = timeZone
        var cursor = Date(timeIntervalSince1970: floor(date.timeIntervalSince1970 / 60) * 60 + 60)
        let deadline = calendar.date(byAdding: .year, value: 5, to: date)!
        var result: [Date] = []
        while cursor < deadline && result.count < count {
            let c = calendar.dateComponents([.minute, .hour, .day, .month, .weekday], from: cursor)
            let weekday = c.weekday! - 1
            let dayMatch = fields[2].contains(c.day!), weekMatch = fields[4].contains(weekday) || (weekday == 0 && fields[4].contains(7))
            let dayAllowed = anyDay || anyWeekday ? dayMatch && weekMatch : dayMatch || weekMatch
            if fields[3].contains(c.month!) && dayAllowed {
                if fields[1].contains(c.hour!) && fields[0].contains(c.minute!) { result.append(cursor) }
                cursor.addTimeInterval(60)
            } else {
                cursor = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: cursor))!
            }
        }
        guard result.count == count else { throw ToolError("未来五年内找不到足够执行时间，请检查日期组合。") }
        return result
    }
}

/// A small arithmetic parser, never an evaluator for shell, JavaScript or Objective-C.
public struct Calculator {
    private var tokens: [String]; private var position = 0
    public init(_ text: String) throws {
        let regex = try NSRegularExpression(pattern: #"(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?|[a-zA-Z]+|[+\-*/%^(),]"#)
        let stripped = text.filter { !$0.isWhitespace }
        let ns = stripped as NSString
        let matches = regex.matches(in: stripped, range: NSRange(location: 0, length: ns.length))
        guard matches.reduce(0, { $0 + $1.range.length }) == ns.length, ns.length <= 4096 else { throw ToolError("表达式包含不支持的字符或过长。") }
        tokens = matches.map { ns.substring(with: $0.range) }
    }
    public mutating func evaluate() throws -> Double {
        let result = try expression()
        guard position == tokens.count, result.isFinite else { throw ToolError("无效表达式、除以零或计算结果超出范围。") }; return result
    }
    private var peek: String { position < tokens.count ? tokens[position] : "" }
    private mutating func consume(_ value: String) -> Bool {
        if peek == value { position += 1; return true }; return false
    }
    private mutating func expression() throws -> Double {
        var value = try term()
        while true { if consume("+") { value += try term() } else if consume("-") { value -= try term() } else { break } }; return value
    }
    private mutating func term() throws -> Double {
        var value = try unary()
        while true {
            if consume("*") { value *= try unary() } else if consume("/") { value /= try unary() }
            else if consume("%") { value = value.truncatingRemainder(dividingBy: try unary()) } else { break }
        }; return value
    }
    private mutating func unary() throws -> Double {
        if consume("+") { return try unary() }; if consume("-") { return try -unary() }
        var value = try atom()
        if consume("^") { value = pow(value, try unary()) }; return value
    }
    private mutating func atom() throws -> Double {
        if consume("(") { let value = try expression(); guard consume(")") else { throw ToolError("缺少右括号。") }; return value }
        let token = peek; position += 1
        if let value = Double(token) { return value }
        if token == "pi" { return .pi }; if token == "e" { return exp(1) }
        guard ["sin", "cos", "tan", "sqrt", "abs", "ln", "log", "floor", "ceil", "round"].contains(token), consume("(") else { throw ToolError("未知数字或函数：\(token)") }
        let value = try expression(); guard consume(")") else { throw ToolError("缺少右括号。") }
        switch token {
        case "sin": return sin(value); case "cos": return cos(value); case "tan": return tan(value)
        case "sqrt": return sqrt(value); case "abs": return abs(value); case "ln": return log(value)
        case "log": return log10(value); case "floor": return floor(value); case "ceil": return ceil(value)
        default: return value.rounded()
        }
    }
}
