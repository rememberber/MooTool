import XCTest
@testable import MooToolNativeCore

final class TimeConversionServiceTests: XCTestCase {
    func testConvertsUnixSecondsTimestamp() throws {
        let result = try TimeConversionService.convertTimestamp("1704067200", timeZone: utcTimeZone)

        XCTAssertEqual(result.seconds, 1_704_067_200)
        XCTAssertEqual(result.milliseconds, 1_704_067_200_000)
        XCTAssertEqual(result.formattedText, "2024-01-01 00:00:00.000 UTC")
    }

    func testConvertsUnixMillisecondsTimestamp() throws {
        let result = try TimeConversionService.convertTimestamp("1704067200123", timeZone: utcTimeZone)

        XCTAssertEqual(result.seconds, 1_704_067_200)
        XCTAssertEqual(result.milliseconds, 1_704_067_200_123)
        XCTAssertEqual(result.formattedText, "2024-01-01 00:00:00.123 UTC")
    }

    func testCurrentTimestampUsesProvidedDate() {
        let timestamp = TimeConversionService.currentTimestamp(
            now: Date(timeIntervalSince1970: 1_704_067_200.123)
        )

        XCTAssertEqual(timestamp.seconds, 1_704_067_200)
        XCTAssertEqual(timestamp.milliseconds, 1_704_067_200_123)
    }

    func testRejectsEmptyTimestamp() {
        XCTAssertThrowsError(try TimeConversionService.convertTimestamp("   ", timeZone: utcTimeZone)) { error in
            XCTAssertEqual(error as? TimeConversionError, .emptyInput)
        }
    }

    func testRejectsInvalidTimestamp() {
        XCTAssertThrowsError(try TimeConversionService.convertTimestamp("not-a-time", timeZone: utcTimeZone)) { error in
            XCTAssertEqual(error as? TimeConversionError, .invalidTimestamp)
        }
    }

    private var utcTimeZone: TimeZone {
        TimeZone(secondsFromGMT: 0)!
    }
}
