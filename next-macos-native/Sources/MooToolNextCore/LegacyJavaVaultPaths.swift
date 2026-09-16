import Foundation

/// Resolves Java desktop edition on-disk vault folders under `~/.MooTool` (or custom root).
public enum LegacyJavaVaultPaths {
    public static func vaultDirectoryRoots(at root: URL = LegacyJavaDataPaths.defaultDirectory) throws -> (quickNote: URL, json: URL) {
        let configPath = root.appendingPathComponent("config/config.setting")
        let config = FileManager.default.fileExists(atPath: configPath.path)
            ? try parseLegacySetting(String(contentsOf: configPath, encoding: .utf8))
            : [:]
        let quick = resolveConfiguredPath(
            root: root,
            configured: settingValue(config, group: "func.quickNote", key: "quickNoteVaultPath"),
            fallback: "quick-notes")
        let json = resolveConfiguredPath(
            root: root,
            configured: settingValue(config, group: "func.jsonBeauty", key: "jsonBeautyVaultPath"),
            fallback: "json-beauty")
        return (quick, json)
    }

    public static func parseLegacySetting(_ raw: String) throws -> [String: [String: String]] {
        var result: [String: [String: String]] = [:]
        var group = ""
        for rawLine in raw.replacingOccurrences(of: "\u{FEFF}", with: "").components(separatedBy: .newlines) {
            let line = rawLine.trimmingCharacters(in: .whitespacesAndNewlines)
            if line.isEmpty || line.hasPrefix("#") || line.hasPrefix(";") { continue }
            if line.hasPrefix("["), line.hasSuffix("]"), line.count >= 2 {
                group = String(line.dropFirst().dropLast()).trimmingCharacters(in: .whitespacesAndNewlines)
                result[group] = result[group] ?? [:]
                continue
            }
            guard let separator = line.firstIndex(of: "=") else { continue }
            let key = String(line[..<separator]).trimmingCharacters(in: .whitespacesAndNewlines)
            let value = unquote(String(line[line.index(after: separator)...]).trimmingCharacters(in: .whitespacesAndNewlines))
            guard !key.isEmpty else { continue }
            result[group, default: [:]][key] = value
        }
        return result
    }

    private static func settingValue(_ config: [String: [String: String]], group: String, key: String) -> String {
        config[group]?[key]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    private static func resolveConfiguredPath(root: URL, configured: String, fallback: String) -> URL {
        let trimmed = configured.trimmingCharacters(in: .whitespacesAndNewlines)
        let relative = trimmed.isEmpty ? fallback : trimmed
        let expanded = NSString(string: relative).expandingTildeInPath
        if (expanded as NSString).isAbsolutePath {
            return URL(fileURLWithPath: expanded, isDirectory: true).standardizedFileURL
        }
        return root.appendingPathComponent(expanded, isDirectory: true).standardizedFileURL
    }

    private static func unquote(_ value: String) -> String {
        if value.count >= 2,
           (value.hasPrefix("\"") && value.hasSuffix("\"")) || (value.hasPrefix("'") && value.hasSuffix("'")) {
            return String(value.dropFirst().dropLast())
        }
        return value
    }
}
