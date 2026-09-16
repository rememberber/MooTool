import Foundation

public enum ElectronDataPaths {
    public static var applicationSupportDirectory: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/Application Support/MooTool", isDirectory: true)
    }

    public static var defaultDatabaseURL: URL {
        applicationSupportDirectory.appendingPathComponent("MooToolNext.db", isDirectory: false)
    }

    public static func databaseExists(at url: URL = defaultDatabaseURL) -> Bool {
        FileManager.default.fileExists(atPath: url.path)
    }
}
