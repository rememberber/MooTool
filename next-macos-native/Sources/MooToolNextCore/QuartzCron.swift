import Foundation

extension CronExpression {
    public static func describe(_ input: String) -> String? {
        guard let cron = try? CronExpression(input) else { return nil }
        if let summary = cron.naturalSummary() {
            return summary + " · " + cron.describe()
        }
        return cron.describe()
    }
}

extension CronExpression {
    /// Short Chinese schedule hint for common Quartz patterns (cronstrue-style, not a full parser).
    func naturalSummary() -> String? {
        guard flavor == .quartz, seconds == [0], minutesField.count == 1, hoursField.count == 1 else { return nil }
        let minute = minutesField.sorted().first!
        let hour = hoursField.sorted().first!
        let time = String(format: "%02d:%02d", hour, minute)
        let weekdays = Set(weekdaysField.map { $0 == 7 ? 0 : $0 })
        let workdays = Set([1, 2, 3, 4, 5])
        if dayRule == .quartzWeek, weekMatcher == .values, weekdays == workdays {
            return "工作日每天 \(time)"
        }
        if dayRule == .quartzAny, monthsField.count == 12, daysField.count == 31 {
            return "每天 \(time)"
        }
        if dayRule == .quartzDay, dayMatcher == .last {
            return "每月最后一天 \(time)"
        }
        if dayRule == .quartzWeek, case .nth(let weekday, let nth) = weekMatcher {
            return "每月第 \(nth) 个\(weekdayName(weekday)) \(time)"
        }
        if dayRule == .quartzWeek, case .last(let weekday) = weekMatcher {
            return "每月最后一个\(weekdayName(weekday)) \(time)"
        }
        return nil
    }

    func describe() -> String {
        switch flavor {
        case .unix:
            return "Unix 五段 Cron（分 时 日 月 周）"
        case .quartz:
            var parts: [String] = []
            if seconds != [0] { parts.append("秒：\(label(seconds, in: 0...59))") }
            parts.append("分：\(label(minutesField, in: 0...59))")
            parts.append("时：\(label(hoursField, in: 0...23))")
            switch dayRule {
            case .unixOr: parts.append("日/周：按 Unix 规则（日或周匹配）")
            case .quartzDay:
                switch dayMatcher {
                case .last: parts.append("日：每月最后一天")
                case .nearestWeekday(let day): parts.append("日：\(day) 日最近工作日")
                case .any: parts.append("日：任意")
                case .values: parts.append("日：\(label(daysField, in: 1...31))")
                }
            case .quartzWeek:
                switch weekMatcher {
                case .last(let weekday): parts.append("周：每月最后一个\(weekdayName(weekday))")
                case .nth(let weekday, let nth): parts.append("周：每月第 \(nth) 个\(weekdayName(weekday))")
                case .any: parts.append("周：任意")
                case .values: parts.append("周：\(label(weekdaysField, in: 0...7))")
                }
            case .quartzAny: parts.append("每日")
            }
            parts.append("月：\(label(monthsField, in: 1...12))")
            if let yearsField, yearsField != Set(1970...2199) { parts.append("年：\(label(yearsField, in: 1970...2199))") }
            return parts.joined(separator: " · ")
        }
    }

    private func label(_ values: Set<Int>, in range: ClosedRange<Int>) -> String {
        if values.count == range.count { return "每\(range.lowerBound == 0 ? "个" : "月")" }
        if values.count <= 4 { return values.sorted().map(String.init).joined(separator: ",") }
        return "\(values.count) 个取值"
    }

    private func weekdayName(_ weekday: Int) -> String {
        ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][max(0, min(6, weekday))]
    }
}

enum CronFlavor { case unix, quartz }
enum CronDayRule { case unixOr, quartzDay, quartzWeek, quartzAny }

enum CronDayMatcher: Equatable {
    case values
    case any
    case last
    case nearestWeekday(Int)
}

enum CronWeekMatcher: Equatable {
    case values
    case any
    case last(Int)
    case nth(Int, Int)
}

struct QuartzCronParts {
    var seconds: Set<Int>
    var minutes: Set<Int>
    var hours: Set<Int>
    var days: Set<Int>
    var months: Set<Int>
    var weekdays: Set<Int>
    var years: Set<Int>?
    var dayRule: CronDayRule
    var flavor: CronFlavor
    var dayMatcher: CronDayMatcher = .values
    var weekMatcher: CronWeekMatcher = .values
}

enum CronScheduleMatch {
    static func weekdayIndex(_ date: Date, calendar: Calendar) -> Int { calendar.component(.weekday, from: date) - 1 }

    static func isLastDayOfMonth(_ date: Date, calendar: Calendar) -> Bool {
        let day = calendar.component(.day, from: date)
        guard let range = calendar.range(of: .day, in: .month, for: date) else { return false }
        return day == range.count
    }

    static func isNearestWeekday(to day: Int, date: Date, calendar: Calendar) -> Bool {
        let year = calendar.component(.year, from: date)
        let month = calendar.component(.month, from: date)
        guard let anchor = calendar.date(from: DateComponents(year: year, month: month, day: day)) else { return false }
        let weekday = calendar.component(.weekday, from: anchor)
        var nearest = anchor
        if weekday == 7 { nearest = calendar.date(byAdding: .day, value: -1, to: anchor)! }
        else if weekday == 1 { nearest = calendar.date(byAdding: .day, value: 1, to: anchor)! }
        if calendar.component(.month, from: nearest) != month {
            if weekday == 7, let forward = calendar.date(byAdding: .day, value: 2, to: anchor), calendar.component(.month, from: forward) == month { nearest = forward }
            else if weekday == 1, let backward = calendar.date(byAdding: .day, value: -2, to: anchor), calendar.component(.month, from: backward) == month { nearest = backward }
        }
        return calendar.isDate(date, inSameDayAs: nearest)
    }

    static func isLastWeekday(_ weekday: Int, date: Date, calendar: Calendar) -> Bool {
        guard weekdayIndex(date, calendar: calendar) == weekday else { return false }
        guard let range = calendar.range(of: .day, in: .month, for: date) else { return false }
        let day = calendar.component(.day, from: date)
        for next in (day + 1)...range.count {
            guard let candidate = calendar.date(byAdding: .day, value: next - day, to: date) else { continue }
            if weekdayIndex(candidate, calendar: calendar) == weekday { return false }
        }
        return true
    }

    static func isNthWeekday(_ nth: Int, weekday: Int, date: Date, calendar: Calendar) -> Bool {
        guard weekdayIndex(date, calendar: calendar) == weekday else { return false }
        let year = calendar.component(.year, from: date)
        let month = calendar.component(.month, from: date)
        guard let range = calendar.range(of: .day, in: .month, for: date) else { return false }
        var seen = 0
        for day in range {
            guard let candidate = calendar.date(from: DateComponents(year: year, month: month, day: day)) else { continue }
            if weekdayIndex(candidate, calendar: calendar) == weekday {
                seen += 1
                if seen == nth { return calendar.isDate(candidate, inSameDayAs: date) }
            }
        }
        return false
    }
}

enum QuartzCronParser {
    static func parse(_ input: String) throws -> QuartzCronParts {
        let parts = input.split(whereSeparator: \.isWhitespace).map(String.init)
        switch parts.count {
        case 5: return try parseUnix(parts)
        case 6, 7: return try parseQuartz(parts)
        default: throw ToolError("使用五段 Unix Cron 或六/七段 Quartz Cron（秒 分 时 日 月 周 [年]）。支持 *、?、L、W、#、范围、列表、/ 步长与 MON–SUN。")
        }
    }

    private static func parseUnix(_ parts: [String]) throws -> QuartzCronParts {
        let anyDay = parts[2].hasPrefix("*")
        let anyWeekday = parts[4].hasPrefix("*")
        let dayRule: CronDayRule
        if anyDay && anyWeekday { dayRule = .quartzAny }
        else if anyWeekday && !anyDay { dayRule = .quartzDay }
        else if anyDay && !anyWeekday { dayRule = .quartzWeek }
        else { dayRule = .unixOr }
        return QuartzCronParts(
            seconds: [0],
            minutes: try parseField(parts[0], range: 0...59),
            hours: try parseField(parts[1], range: 0...23),
            days: try parseField(parts[2], range: 1...31),
            months: try parseField(parts[3], range: 1...12),
            weekdays: try parseWeekField(parts[4]),
            years: nil,
            dayRule: dayRule,
            flavor: .unix
        )
    }

    private static func parseQuartz(_ parts: [String]) throws -> QuartzCronParts {
        let dayToken = parts[3]
        let weekToken = parts[5]
        if dayToken == "?" && weekToken == "?" { throw ToolError("Quartz Cron 的「日」与「周」不能同时为 ?。") }
        let parsedDay = try parseDayToken(dayToken)
        let parsedWeek = try parseWeekToken(weekToken)
        let dayRule: CronDayRule
        if dayToken == "?" { dayRule = .quartzWeek }
        else if weekToken == "?" { dayRule = .quartzDay }
        else if dayToken == "*" && (weekToken == "*" || weekToken == "?") { dayRule = .quartzAny }
        else { dayRule = .unixOr }
        let years = parts.count == 7 && !parts[6].isEmpty ? try parseField(parts[6], range: 1970...2199) : nil
        return QuartzCronParts(
            seconds: try parseField(parts[0], range: 0...59),
            minutes: try parseField(parts[1], range: 0...59),
            hours: try parseField(parts[2], range: 0...23),
            days: parsedDay.values,
            months: try parseField(parts[4], range: 1...12),
            weekdays: parsedWeek.values,
            years: years,
            dayRule: dayRule,
            flavor: .quartz,
            dayMatcher: parsedDay.matcher,
            weekMatcher: parsedWeek.matcher
        )
    }

    private static func parseDayToken(_ value: String) throws -> (values: Set<Int>, matcher: CronDayMatcher) {
        if value == "?" { return (Set(1...31), .any) }
        if value == "L" { return (Set(1...31), .last) }
        if value.hasSuffix("W"), value.count > 1, let number = Int(value.dropLast()) {
            guard (1...31).contains(number) else { throw ToolError("日字段 W 需在 1–31 之间。") }
            return (Set(1...31), .nearestWeekday(number))
        }
        if value.contains("L") || value.contains("W") { throw ToolError("暂不支持的日字段：\(value)") }
        return (try parseField(value, range: 1...31), .values)
    }

    private static func parseWeekToken(_ value: String) throws -> (values: Set<Int>, matcher: CronWeekMatcher) {
        if value == "?" || value == "*" { return (Set(0...7), .any) }
        if !value.contains(",") {
            let hash = value.split(separator: "#", omittingEmptySubsequences: false)
            if hash.count == 2 {
                let weekday = try weekdayNumber(String(hash[0]))
                guard let nth = Int(hash[1]), (1...5).contains(nth) else { throw ToolError("周字段 # 次数需在 1–5 之间。") }
                return (Set(0...7), .nth(weekday, nth))
            }
            if value.count > 1, value.last?.uppercased() == "L" {
                let weekday = try weekdayNumber(String(value.dropLast()))
                return (Set(0...7), .last(weekday))
            }
        }
        if value.contains("#") || value.uppercased().contains("L") { throw ToolError("暂不支持的周字段：\(value)") }
        return (try parseWeekField(value), .values)
    }

    private static func weekdayNumber(_ token: String) throws -> Int {
        let normalized = token.uppercased()
        let map = ["SUN": 0, "MON": 1, "TUE": 2, "WED": 3, "THU": 4, "FRI": 5, "SAT": 6]
        if let mapped = map[normalized] { return mapped }
        guard let number = Int(normalized), (0...7).contains(number) else { throw ToolError("无效周字段：\(token)") }
        return number == 7 ? 0 : number
    }

    static func parseField(_ value: String, range: ClosedRange<Int>) throws -> Set<Int> {
        if value == "?" { return Set(range) }
        var output = Set<Int>()
        for item in value.split(separator: ",", omittingEmptySubsequences: false) {
            let pair = item.split(separator: "/", omittingEmptySubsequences: false)
            guard (1...2).contains(pair.count), let step = pair.count == 2 ? Int(pair[1]) : 1, step > 0 else { throw ToolError("无效 Cron 步长。") }
            let bounds = pair[0].split(separator: "-", omittingEmptySubsequences: false)
            let lower: Int; let upper: Int
            if pair[0] == "*" { lower = range.lowerBound; upper = range.upperBound }
            else if bounds.count == 1, let number = Int(bounds[0]) { lower = number; upper = pair.count == 2 ? range.upperBound : number }
            else if bounds.count == 2, let a = Int(bounds[0]), let b = Int(bounds[1]) { lower = a; upper = b }
            else { throw ToolError("无效 Cron 字段：\(item)") }
            guard range.contains(lower), range.contains(upper), lower <= upper else { throw ToolError("Cron 字段越界：\(item)") }
            output.formUnion(stride(from: lower, through: upper, by: step))
        }
        return output
    }

    static func parseWeekField(_ value: String) throws -> Set<Int> {
        if value == "?" { return Set(0...7) }
        let normalized = value
            .replacingOccurrences(of: "SUN", with: "0", options: .caseInsensitive)
            .replacingOccurrences(of: "MON", with: "1", options: .caseInsensitive)
            .replacingOccurrences(of: "TUE", with: "2", options: .caseInsensitive)
            .replacingOccurrences(of: "WED", with: "3", options: .caseInsensitive)
            .replacingOccurrences(of: "THU", with: "4", options: .caseInsensitive)
            .replacingOccurrences(of: "FRI", with: "5", options: .caseInsensitive)
            .replacingOccurrences(of: "SAT", with: "6", options: .caseInsensitive)
        return try parseField(normalized, range: 0...7)
    }
}

public struct CronFieldDraft: Equatable {
    public var second = "0"
    public var minute = "*"
    public var hour = "*"
    public var day = "*"
    public var month = "*"
    public var week = "?"
    public var year = ""
    public init() {}
    public init(second: String, minute: String, hour: String, day: String, month: String, week: String, year: String) {
        self.second = second; self.minute = minute; self.hour = hour; self.day = day; self.month = month; self.week = week; self.year = year
    }
    public func build() throws -> String {
        let core = [second, minute, hour, day, month, week].map { $0.trimmingCharacters(in: .whitespaces) }
        guard core.allSatisfy({ !$0.isEmpty }) else { throw ToolError("请填写全部 Quartz 字段。") }
        let trimmedYear = year.trimmingCharacters(in: .whitespaces)
        return ([second, minute, hour, day, month, week] + (trimmedYear.isEmpty ? [] : [trimmedYear])).joined(separator: " ")
    }
    public static func split(_ expression: String) throws -> CronFieldDraft {
        let parts = expression.split(whereSeparator: \.isWhitespace).map(String.init)
        guard parts.count == 6 || parts.count == 7 else { throw ToolError("Quartz 表达式需为 6 或 7 段。") }
        return CronFieldDraft(second: parts[0], minute: parts[1], hour: parts[2], day: parts[3], month: parts[4], week: parts[5], year: parts.count == 7 ? parts[6] : "")
    }
    public static let presets: [(String, String)] = [
        ("每分钟", "0 * * * * ?"),
        ("每小时", "0 0 * * * ?"),
        ("每天零点", "0 0 0 * * ?"),
        ("工作日 9:00", "0 0 9 ? * MON-FRI"),
        ("每月最后一天", "0 0 0 L * ?"),
        ("每月第一个周五 9:00", "0 0 9 ? * FRI#1")
    ]
}
