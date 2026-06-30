import XCTest
@testable import MooToolNativeCore

final class EncodingConversionServiceTests: XCTestCase {
    func testBase64RoundTripUsesUTF8() throws {
        let encoded = EncodingConversionService.base64Encode("MooTool 原生版")
        let decoded = try EncodingConversionService.base64Decode(encoded)

        XCTAssertEqual(encoded, "TW9vVG9vbCDljp/nlJ/niYg=")
        XCTAssertEqual(decoded, "MooTool 原生版")
    }

    func testRejectsInvalidBase64() {
        XCTAssertThrowsError(try EncodingConversionService.base64Decode("not base64")) { error in
            XCTAssertEqual(error as? EncodingConversionError, .invalidBase64)
        }
    }

    func testURLEncodesAndDecodesQueryText() throws {
        let encoded = EncodingConversionService.urlEncode("q=Moo Tool&lang=中文")
        let decoded = try EncodingConversionService.urlDecode(encoded)

        XCTAssertEqual(encoded, "q%3DMoo%20Tool%26lang%3D%E4%B8%AD%E6%96%87")
        XCTAssertEqual(decoded, "q=Moo Tool&lang=中文")
    }

    func testRejectsInvalidPercentEncoding() {
        XCTAssertThrowsError(try EncodingConversionService.urlDecode("%E4%B8%AD%")) { error in
            XCTAssertEqual(error as? EncodingConversionError, .invalidPercentEncoding)
        }
    }
}
