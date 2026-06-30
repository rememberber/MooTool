import XCTest
@testable import MooToolNativeCore

final class HostFileServiceTests: XCTestCase {
    func testParsesActiveAndDisabledHostEntries() {
        let entries = HostFileService.parse("""
        127.0.0.1 localhost
        # 0.0.0.0 ads.example.com
        # comment only
        192.168.1.2 api.local dev.local
        """)

        XCTAssertEqual(entries.count, 3)
        XCTAssertEqual(entries[0], HostEntry(address: "127.0.0.1", hosts: ["localhost"], isEnabled: true))
        XCTAssertEqual(entries[1], HostEntry(address: "0.0.0.0", hosts: ["ads.example.com"], isEnabled: false))
        XCTAssertEqual(entries[2], HostEntry(address: "192.168.1.2", hosts: ["api.local", "dev.local"], isEnabled: true))
    }

    func testRendersHostEntries() {
        let rendered = HostFileService.render([
            HostEntry(address: "127.0.0.1", hosts: ["localhost"], isEnabled: true),
            HostEntry(address: "0.0.0.0", hosts: ["ads.example.com"], isEnabled: false)
        ])

        XCTAssertEqual(rendered, """
        127.0.0.1 localhost
        # 0.0.0.0 ads.example.com
        """)
    }
}
