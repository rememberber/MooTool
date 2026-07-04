import XCTest
@testable import MooToolNativeCore

final class QRCodePayloadServiceTests: XCTestCase {
    func testTrimsPayloadAndReportsByteCount() throws {
        let payload = try QRCodePayloadService.prepare("  https://mootool.app  ")

        XCTAssertEqual(payload.text, "https://mootool.app")
        XCTAssertEqual(payload.byteCount, 19)
    }

    func testRejectsEmptyPayload() {
        XCTAssertThrowsError(try QRCodePayloadService.prepare("  ")) { error in
            XCTAssertEqual(error as? QRCodePayloadError, .emptyPayload)
        }
    }
}
