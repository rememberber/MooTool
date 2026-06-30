import XCTest
@testable import MooToolNativeCore

final class QuickNoteServiceTests: XCTestCase {
    func testBuildsNoteSummary() {
        let note = QuickNoteDraft(title: "Release Plan", content: "Ship macOS native preview\nKeep Java packages")

        let summary = QuickNoteService.summary(for: note)

        XCTAssertEqual(summary.title, "Release Plan")
        XCTAssertEqual(summary.lineCount, 2)
        XCTAssertEqual(summary.characterCount, 44)
        XCTAssertEqual(summary.preview, "Ship macOS native preview")
    }

    func testUsesUntitledNameForEmptyTitle() {
        let note = QuickNoteDraft(title: "  ", content: "")

        let summary = QuickNoteService.summary(for: note)

        XCTAssertEqual(summary.title, "未命名随手记")
        XCTAssertEqual(summary.lineCount, 0)
        XCTAssertEqual(summary.characterCount, 0)
        XCTAssertEqual(summary.preview, "暂无内容")
    }
}
