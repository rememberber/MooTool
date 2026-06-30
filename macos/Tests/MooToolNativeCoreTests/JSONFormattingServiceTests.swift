import XCTest
@testable import MooToolNativeCore

final class JSONFormattingServiceTests: XCTestCase {
    func testFormatsJSONObjectWithStableKeyOrder() throws {
        let formatted = try JSONFormattingService.format(#"{"b":1,"a":[true,null]}"#)

        XCTAssertEqual(formatted, """
        {
          "a" : [
            true,
            null
          ],
          "b" : 1
        }
        """)
    }

    func testMinifiesJSON() throws {
        let minified = try JSONFormattingService.minify("""
        {
          "name": "MooTool",
          "native": true
        }
        """)

        XCTAssertEqual(minified, #"{"name":"MooTool","native":true}"#)
    }

    func testRejectsInvalidJSON() {
        XCTAssertThrowsError(try JSONFormattingService.format("{broken")) { error in
            XCTAssertEqual(error as? JSONFormattingError, .invalidJSON)
        }
    }
}
