import Foundation

/// Default Java desktop edition data root (`~/.MooTool`), used for migration hints only.
public enum LegacyJavaDataPaths {
    public static var defaultDirectory: URL {
        FileManager.default.homeDirectoryForCurrentUser.appendingPathComponent(".MooTool", isDirectory: true)
    }

    public static func legacyDatabaseURL(in root: URL = defaultDirectory) -> URL? {
        let candidates = [
            root.appendingPathComponent("MooTool.db"),
            root.appendingPathComponent("database/MooTool.db")
        ]
        return candidates.first { FileManager.default.fileExists(atPath: $0.path) }
    }

    public static func hasLegacyInstall(at root: URL = defaultDirectory) -> Bool {
        legacyDatabaseURL(in: root) != nil
    }
}
