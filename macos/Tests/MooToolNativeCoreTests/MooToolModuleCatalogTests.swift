import XCTest
@testable import MooToolNativeCore

final class MooToolModuleCatalogTests: XCTestCase {
    func testPreviewCatalogExposesStableTopLevelModules() {
        let modules = MooToolModuleCatalog.previewModules

        XCTAssertEqual(modules.map(\.id), [
            "quick-note",
            "json",
            "time",
            "encoding",
            "qr-code",
            "http",
            "host",
            "regex",
            "text-diff"
        ])
        XCTAssertEqual(modules.first?.title, "随手记")
        XCTAssertEqual(modules.first?.status, .planned)
    }

    func testFirstPreviewModuleIsDefaultSelection() {
        XCTAssertEqual(MooToolModuleCatalog.defaultSelection?.id, "quick-note")
    }

    func testTimeModuleIsMarkedAsPreview() {
        let timeModule = MooToolModuleCatalog.previewModules.first { $0.id == "time" }

        XCTAssertEqual(timeModule?.status, .preview)
    }
}
