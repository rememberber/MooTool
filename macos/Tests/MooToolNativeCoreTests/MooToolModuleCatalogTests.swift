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
        XCTAssertEqual(modules.first?.status, .preview)
    }

    func testFirstPreviewModuleIsDefaultSelection() {
        XCTAssertEqual(MooToolModuleCatalog.defaultSelection?.id, "quick-note")
    }

    func testTimeModuleIsMarkedAsPreview() {
        let timeModule = MooToolModuleCatalog.previewModules.first { $0.id == "time" }

        XCTAssertEqual(timeModule?.status, .preview)
    }

    func testBasicToolModulesAreMarkedAsPreview() {
        let moduleStatusByID = Dictionary(
            uniqueKeysWithValues: MooToolModuleCatalog.previewModules.map { ($0.id, $0.status) }
        )

        XCTAssertEqual(moduleStatusByID["quick-note"], .preview)
        XCTAssertEqual(moduleStatusByID["json"], .preview)
        XCTAssertEqual(moduleStatusByID["encoding"], .preview)
        XCTAssertEqual(moduleStatusByID["qr-code"], .preview)
        XCTAssertEqual(moduleStatusByID["http"], .preview)
        XCTAssertEqual(moduleStatusByID["host"], .preview)
        XCTAssertEqual(moduleStatusByID["regex"], .preview)
        XCTAssertEqual(moduleStatusByID["text-diff"], .preview)
    }
}
