import XCTest
@testable import MooToolNativeCore

final class RegexMatchServiceTests: XCTestCase {
    func testFindsMatchesAndCaptureGroups() throws {
        let result = try RegexMatchService.evaluate(
            pattern: #"(\w+)@([\w.]+)"#,
            input: "dev@mootool.app admin@example.com"
        )

        XCTAssertEqual(result.matches.map(\.text), ["dev@mootool.app", "admin@example.com"])
        XCTAssertEqual(result.matches.first?.captureGroups, ["dev", "mootool.app"])
        XCTAssertEqual(result.matches.first?.range.location, 0)
        XCTAssertEqual(result.matches.first?.range.length, 15)
    }

    func testEmptyPatternReturnsNoMatches() throws {
        let result = try RegexMatchService.evaluate(pattern: "", input: "abc")

        XCTAssertTrue(result.matches.isEmpty)
    }

    func testRejectsInvalidPattern() {
        XCTAssertThrowsError(try RegexMatchService.evaluate(pattern: "[", input: "abc")) { error in
            XCTAssertEqual(error as? RegexMatchError, .invalidPattern)
        }
    }
}
