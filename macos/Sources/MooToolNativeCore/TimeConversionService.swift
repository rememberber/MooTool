import Foundation

public enum TimeConversionError: Error, Equatable, Sendable {
    case emptyInput
    case invalidTimestamp
}

public struct TimestampPair: Equatable, Sendable {
    public let seconds: Int64
    public let milliseconds: Int64
}

public struct TimeConversionResult: Equatable, Sendable {
    public let seconds: Int64
    public let milliseconds: Int64
    public let formattedText: String
}

public enum TimeConversionService {
    public static func convertTimestamp(_ rawValue: String, timeZone: TimeZone = .current) throws -> TimeConversionResult {
        let value = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else {
            throw TimeConversionError.emptyInput
        }

        guard let parsedValue = Int64(value), isIntegerLiteral(value) else {
            throw TimeConversionError.invalidTimestamp
        }

        let digitCount = value.trimmingPrefix("-").count
        let milliseconds: Int64
        let seconds: Int64

        if digitCount > 10 {
            milliseconds = parsedValue
            seconds = parsedValue / 1_000
        } else {
            let multiplied = parsedValue.multipliedReportingOverflow(by: 1_000)
            guard !multiplied.overflow else {
                throw TimeConversionError.invalidTimestamp
            }
            seconds = parsedValue
            milliseconds = multiplied.partialValue
        }

        let date = Date(timeIntervalSince1970: Double(seconds))
        return TimeConversionResult(
            seconds: seconds,
            milliseconds: milliseconds,
            formattedText: format(date: date, milliseconds: millisecondComponent(from: milliseconds), timeZone: timeZone)
        )
    }

    public static func currentTimestamp(now: Date = Date()) -> TimestampPair {
        let milliseconds = Int64((now.timeIntervalSince1970 * 1_000).rounded())
        return TimestampPair(seconds: milliseconds / 1_000, milliseconds: milliseconds)
    }

    private static func isIntegerLiteral(_ value: String) -> Bool {
        let startIndex = value.first == "-" ? value.index(after: value.startIndex) : value.startIndex
        guard startIndex < value.endIndex else {
            return false
        }
        return value[startIndex...].allSatisfy(\.isNumber)
    }

    private static func millisecondComponent(from milliseconds: Int64) -> Int64 {
        let component = milliseconds % 1_000
        return component >= 0 ? component : -component
    }

    private static func format(date: Date, milliseconds: Int64, timeZone: TimeZone) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone

        let components = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: date
        )
        let zoneText = timeZone.secondsFromGMT(for: date) == 0 ? "UTC" : (timeZone.abbreviation(for: date) ?? timeZone.identifier)

        return String(
            format: "%04d-%02d-%02d %02d:%02d:%02d.%03d %@",
            components.year ?? 0,
            components.month ?? 0,
            components.day ?? 0,
            components.hour ?? 0,
            components.minute ?? 0,
            components.second ?? 0,
            milliseconds,
            zoneText
        )
    }
}
