import XCTest
@testable import MooToolNativeCore

final class HTTPRequestBuilderTests: XCTestCase {
    func testBuildsRequestWithHeadersAndBody() throws {
        let request = try HTTPRequestBuilder.build(
            method: "POST",
            urlText: "https://example.com/api",
            headersText: "Content-Type: application/json\nX-Trace: abc",
            bodyText: #"{"ok":true}"#
        )

        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.url?.absoluteString, "https://example.com/api")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
        XCTAssertEqual(request.value(forHTTPHeaderField: "X-Trace"), "abc")
        XCTAssertEqual(String(data: request.httpBody ?? Data(), encoding: .utf8), #"{"ok":true}"#)
    }

    func testRejectsInvalidURL() {
        XCTAssertThrowsError(try HTTPRequestBuilder.build(method: "GET", urlText: "not a url", headersText: "", bodyText: "")) { error in
            XCTAssertEqual(error as? HTTPRequestBuilderError, .invalidURL)
        }
    }

    func testRejectsInvalidHeaderLine() {
        XCTAssertThrowsError(try HTTPRequestBuilder.build(method: "GET", urlText: "https://example.com", headersText: "Broken", bodyText: "")) { error in
            XCTAssertEqual(error as? HTTPRequestBuilderError, .invalidHeader)
        }
    }
}
