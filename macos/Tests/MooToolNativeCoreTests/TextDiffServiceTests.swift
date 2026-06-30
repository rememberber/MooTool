import XCTest
@testable import MooToolNativeCore

final class TextDiffServiceTests: XCTestCase {
    func testBuildsLineDiffWithSummary() {
        let result = TextDiffService.diff(
            original: "alpha\nbeta\ngamma",
            revised: "alpha\nbeta changed\ngamma\ndelta"
        )

        XCTAssertEqual(result.summary, TextDiffSummary(unchanged: 2, added: 2, removed: 1))
        XCTAssertEqual(result.lines.map(\.kind), [.unchanged, .removed, .added, .unchanged, .added])
        XCTAssertEqual(result.lines.map(\.text), ["alpha", "beta", "beta changed", "gamma", "delta"])
    }

    func testEmptyTextsHaveNoDiffLines() {
        let result = TextDiffService.diff(original: "", revised: "")

        XCTAssertTrue(result.lines.isEmpty)
        XCTAssertEqual(result.summary, TextDiffSummary(unchanged: 0, added: 0, removed: 0))
    }
}
