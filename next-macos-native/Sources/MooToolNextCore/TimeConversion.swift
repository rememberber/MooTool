import Foundation

public enum TimeConversion {
    public enum TimestampUnit: String, CaseIterable {
        case second
        case millisecond
    }

    public static let commonTimezones: [String] = [
        "UTC",
        "Asia/Shanghai",
        "Asia/Tokyo",
        "Asia/Seoul",
        "Asia/Singapore",
        "Asia/Hong_Kong",
        "Asia/Kolkata",
        "Asia/Dubai",
        "Europe/London",
        "Europe/Paris",
        "Europe/Berlin",
        "Europe/Moscow",
        "America/New_York",
        "America/Chicago",
        "America/Denver",
        "America/Los_Angeles",
        "Australia/Sydney",
        "Pacific/Auckland",
    ]

    public static let quickTimezones: [(label: String, zone: String)] = [
        ("UTC", "UTC"),
        ("+8", "Asia/Shanghai"),
        ("+9", "Asia/Tokyo"),
        ("-5", "America/New_York"),
        ("-8", "America/Los_Angeles"),
        ("+1", "Europe/Paris"),
        ("+3", "Europe/Moscow"),
    ]

    public static func timestampToLocal(
        _ input: String,
        unit: TimestampUnit,
        zone: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> (localTime: String, unit: TimestampUnit, milliseconds: Int64) {
        let normalized = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalized.range(of: #"^-?\d+$"#, options: .regularExpression) != nil else {
            throw ToolError(AppLocalization.string("timeConvert.error.invalidTimestampNumber", language: language))
        }
        let digitCount = normalized.replacingOccurrences(of: "-", with: "").count
        let detected = digitCount >= 13 ? TimestampUnit.millisecond : unit
        guard let value = Int64(normalized) else {
            throw ToolError(AppLocalization.string("timeConvert.error.timestampOutOfRange", language: language))
        }
        let milliseconds = detected == .second ? value * 1000 : value
        let date = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
        guard abs(date.timeIntervalSince1970) < 253_402_300_800 else {
            throw ToolError(AppLocalization.string("timeConvert.error.dateOutOfRange", language: language))
        }
        return (formatLocalTime(milliseconds: milliseconds, zone: zone), detected, milliseconds)
    }

    public static func localToTimestamp(
        _ input: String,
        unit: TimestampUnit,
        zone: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        guard TimeZone(identifier: zone) != nil else {
            throw ToolError(AppLocalization.format("timeConvert.error.invalidTimezone", language: language, replacements: ["zone": zone]))
        }
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: zone)
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        formatter.isLenient = false
        guard let date = formatter.date(from: trimmed), formatter.string(from: date) == trimmed else {
            throw ToolError(AppLocalization.string("timeConvert.error.localTimeFormat", language: language))
        }
        let milliseconds = Int64((date.timeIntervalSince1970 * 1000).rounded())
        return unit == .second ? String(milliseconds / 1000) : String(milliseconds)
    }

    public static func formatLocalTime(milliseconds: Int64, zone: String) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
        return formatLocalTime(date: date, zone: zone)
    }

    public static func formatLocalTime(date: Date, zone: String) -> String {
        guard let timezone = TimeZone(identifier: zone) else { return "" }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timezone
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: date)
    }

    public static func formatTimezoneLabel(zone: String, date: Date = Date()) -> String {
        guard let timezone = TimeZone(identifier: zone) else { return zone }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timezone
        formatter.dateFormat = "ZZZZZ"
        return "\(zone) (\(formatter.string(from: date)))"
    }

    public static func timezones(including system: String) -> [String] {
        Array(Set([system] + commonTimezones)).sorted()
    }
}
