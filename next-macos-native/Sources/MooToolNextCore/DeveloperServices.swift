import Foundation
#if canImport(Darwin)
import Darwin
#endif

public enum DeveloperServices {
    public static func isValidHostIP(_ text: String) -> Bool {
        text.withCString { cString in
            var ipv4 = in_addr()
            var ipv6 = in6_addr()
            return inet_pton(AF_INET, cString, &ipv4) == 1 || inet_pton(AF_INET6, cString, &ipv6) == 1
        }
    }

    public static func validateHostsContent(_ text: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let lines = text.components(separatedBy: .newlines)
        var entries: [String] = []
        for (index, raw) in lines.enumerated() {
            let line = raw.components(separatedBy: "#")[0].trimmingCharacters(in: .whitespaces)
            if line.isEmpty { continue }
            let parts = line.split(whereSeparator: \.isWhitespace).map(String.init)
            let lineNumber = String(index + 1)
            guard parts.count >= 2 else {
                throw ToolError(AppLocalization.format("host.error.lineFormat", language: language, replacements: ["line": lineNumber]))
            }
            let ip = parts[0]
            guard isValidHostIP(ip) else {
                throw ToolError(AppLocalization.format("host.error.lineIp", language: language, replacements: ["line": lineNumber, "ip": ip]))
            }
            entries.append(
                AppLocalization.format("host.validate.entry", language: language, replacements: ["ip": ip, "aliases": parts.dropFirst().joined(separator: " ")])
            )
        }
        if entries.isEmpty {
            return AppLocalization.string("host.validate.noMappings", language: language)
        }
        return AppLocalization.format("host.validate.summary", language: language, replacements: ["count": String(entries.count)]) + entries.joined(separator: "\n")
    }

    public static func timestamp(_ input: String, zone: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        guard let timezone = TimeZone(identifier: zone) else {
            throw ToolError(AppLocalization.format("timeConvert.error.invalidTimezone", language: language, replacements: ["zone": zone]))
        }
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
            else { throw ToolError(AppLocalization.string("timeConvert.error.parseInput", language: language)) }
        }
        guard abs(date.timeIntervalSince1970) < 253_402_300_800 else {
            throw ToolError(AppLocalization.string("timeConvert.error.dateOutOfRange", language: language))
        }
        let formatter = DateFormatter(); formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timezone; formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS ZZZZZ"
        let iso = ISO8601DateFormatter(); iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let dateLabel = AppLocalization.string("timeConvert.detail.date", language: language)
        let zoneLabel = AppLocalization.string("timeConvert.detail.timezone", language: language)
        let secondsLabel = AppLocalization.string("timeConvert.detail.seconds", language: language)
        let msLabel = AppLocalization.string("timeConvert.detail.milliseconds", language: language)
        let utcLabel = AppLocalization.string("timeConvert.detail.utc", language: language)
        return "\(dateLabel)    \(formatter.string(from: date))\n\(zoneLabel)    \(zone)\n\(secondsLabel)      \(Int64(floor(date.timeIntervalSince1970)))\n\(msLabel)    \(Int64((date.timeIntervalSince1970 * 1000).rounded()))\n\(utcLabel)     \(iso.string(from: date))"
    }
    public static func protobuf(_ text: String, base64: Bool, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let data: Data
        if base64 {
            guard let decoded = Data(base64Encoded: text.filter { !$0.isWhitespace }) else {
                throw ToolError(AppLocalization.string("protobuf.error.base64Invalid", language: language))
            }
            data = decoded
        } else { data = try Data(hex: text, language: language) }
        let bytes = Array(data); var position = 0; var fields: [[String: Any]] = []
        func varint() throws -> UInt64 {
            var value: UInt64 = 0
            for shift in stride(from: 0, through: 63, by: 7) {
                guard position < bytes.count else { throw ToolError(AppLocalization.string("protobuf.error.varintTruncated", language: language)) }
                let byte = bytes[position]; position += 1
                guard shift < 63 || byte <= 1 else { throw ToolError(AppLocalization.string("protobuf.error.varintOverflow", language: language)) }
                value |= UInt64(byte & 127) << shift
                if byte & 128 == 0 { return value }
            }
            throw ToolError(AppLocalization.string("protobuf.error.varintTooLong", language: language))
        }
        func take(_ count: Int) throws -> Data {
            guard count >= 0, count <= bytes.count - position else {
                throw ToolError(AppLocalization.string("protobuf.error.lengthOutOfRange", language: language))
            }
            defer { position += count }; return Data(bytes[position..<position+count])
        }
        while position < bytes.count {
            let tag = try varint(), field = tag >> 3, wire = tag & 7
            guard field > 0, field <= 536_870_911 else {
                throw ToolError(AppLocalization.string("protobuf.error.fieldNumberInvalid", language: language))
            }
            var item: [String: Any] = ["field": field, "wireType": wire]
            switch wire {
            case 0: item["varint"] = String(try varint())
            case 1, 5: item["hexLittleEndian"] = try take(wire == 1 ? 8 : 4).hex
            case 2:
                let length = try varint()
                guard length <= UInt64(bytes.count - position) else {
                    throw ToolError(AppLocalization.string("protobuf.error.lengthOutOfRange", language: language))
                }
                let payload = try take(Int(length)); item["hex"] = payload.hex; item["utf8"] = String(data: payload, encoding: .utf8)
            default:
                throw ToolError(String(format: AppLocalization.string("protobuf.error.wireUnsupported", language: language), wire))
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
    public static func pageIndices(_ text: String, count: Int, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [Int] {
        if text.trimmingCharacters(in: .whitespaces).isEmpty { return Array(0..<count) }
        var result: [Int] = []
        for part in text.split(separator: ",", omittingEmptySubsequences: false) {
            let bounds = part.trimmingCharacters(in: .whitespaces).split(separator: "-", omittingEmptySubsequences: false)
            guard (1...2).contains(bounds.count), let start = Int(bounds[0]), let end = Int(bounds.last!), start > 0, end >= start, end <= count else {
                throw ToolError(String(format: AppLocalization.string("pdf.error.pageRange", language: language), count))
            }
            result += (start...end).map { $0 - 1 }
        }; return result
    }
}

public struct CronExpression {
    let flavor: CronFlavor
    let seconds: Set<Int>
    let minutesField: Set<Int>
    let hoursField: Set<Int>
    let daysField: Set<Int>
    let monthsField: Set<Int>
    let weekdaysField: Set<Int>
    let yearsField: Set<Int>?
    let dayRule: CronDayRule
    let dayMatcher: CronDayMatcher
    let weekMatcher: CronWeekMatcher

    public init(_ input: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        let parsed = try QuartzCronParser.parse(input, language: language)
        flavor = parsed.flavor
        seconds = parsed.seconds
        minutesField = parsed.minutes
        hoursField = parsed.hours
        daysField = parsed.days
        monthsField = parsed.months
        weekdaysField = parsed.weekdays
        yearsField = parsed.years
        dayRule = parsed.dayRule
        dayMatcher = parsed.dayMatcher
        weekMatcher = parsed.weekMatcher
    }

    public func next(after date: Date, count: Int = 10, timeZone: TimeZone = .current, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> [Date] {
        guard (1...100).contains(count) else { throw ToolError(AppLocalization.string("cron.error.runCountRange", language: language)) }
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = timeZone
        let step: TimeInterval = flavor == .quartz ? 1 : 60
        var cursor = Date(timeIntervalSince1970: floor(date.timeIntervalSince1970 / step) * step + step)
        let deadline = calendar.date(byAdding: .year, value: 5, to: date)!
        var result: [Date] = []
        while cursor < deadline && result.count < count {
            let c = calendar.dateComponents([.second, .minute, .hour, .day, .month, .weekday, .year], from: cursor)
            let weekday = c.weekday! - 1
            let weekMatch = weekdaysField.contains(weekday) || (weekday == 0 && weekdaysField.contains(7))
            let dayMatch = daysField.contains(c.day!)
            let dayAllowed: Bool = switch dayRule {
            case .quartzAny: true
            case .quartzDay:
                switch dayMatcher {
                case .last: CronScheduleMatch.isLastDayOfMonth(cursor, calendar: calendar)
                case .nearestWeekday(let day): CronScheduleMatch.isNearestWeekday(to: day, date: cursor, calendar: calendar)
                case .any: true
                case .values: dayMatch
                }
            case .quartzWeek:
                switch weekMatcher {
                case .last(let value): CronScheduleMatch.isLastWeekday(value, date: cursor, calendar: calendar)
                case .nth(let value, let nth): CronScheduleMatch.isNthWeekday(nth, weekday: value, date: cursor, calendar: calendar)
                case .any: true
                case .values: weekMatch
                }
            case .unixOr: dayMatch || weekMatch
            }
            let yearOK = yearsField == nil || yearsField!.contains(c.year!)
            let secondOK = flavor == .unix ? true : seconds.contains(c.second!)
            if yearOK && monthsField.contains(c.month!) && dayAllowed && hoursField.contains(c.hour!) && minutesField.contains(c.minute!) && secondOK {
                result.append(cursor)
            }
            cursor.addTimeInterval(step)
        }
        guard result.count == count else { throw ToolError(AppLocalization.string("cron.error.insufficientRuns", language: language)) }
        return result
    }
}

/// A small arithmetic parser, never an evaluator for shell, JavaScript or Objective-C.
public struct Calculator {
    private var tokens: [String]; private var position = 0
    private let language: AppLanguage
    public init(_ text: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        self.language = language
        let regex = try NSRegularExpression(pattern: #"(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?|[a-zA-Z]+|[+\-*/%^(),]"#)
        let stripped = text.filter { !$0.isWhitespace }
        let ns = stripped as NSString
        let matches = regex.matches(in: stripped, range: NSRange(location: 0, length: ns.length))
        guard matches.reduce(0, { $0 + $1.range.length }) == ns.length, ns.length <= 4096 else {
            throw ToolError(AppLocalization.string("calculator.error.unsupportedChars", language: language))
        }
        tokens = matches.map { ns.substring(with: $0.range) }
    }
    public mutating func evaluate() throws -> Double {
        let result = try expression()
        guard position == tokens.count, result.isFinite else { throw ToolError(AppLocalization.string("calculator.error.invalidExpr", language: language)) }
        return result
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
        if consume("(") { let value = try expression(); guard consume(")") else { throw ToolError(AppLocalization.string("calculator.error.missingParen", language: language)) }; return value }
        let token = peek; position += 1
        if let value = Double(token) { return value }
        if token == "pi" { return .pi }; if token == "e" { return exp(1) }
        guard ["sin", "cos", "tan", "sqrt", "abs", "ln", "log", "floor", "ceil", "round"].contains(token), consume("(") else {
            throw ToolError(String(format: AppLocalization.string("calculator.error.unknownToken", language: language), token))
        }
        let value = try expression(); guard consume(")") else { throw ToolError(AppLocalization.string("calculator.error.missingParen", language: language)) }
        switch token {
        case "sin": return sin(value); case "cos": return cos(value); case "tan": return tan(value)
        case "sqrt": return sqrt(value); case "abs": return abs(value); case "ln": return log(value)
        case "log": return log10(value); case "floor": return floor(value); case "ceil": return ceil(value)
        default: return value.rounded()
        }
    }
}
