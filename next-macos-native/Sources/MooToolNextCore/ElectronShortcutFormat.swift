import Foundation

public enum ElectronShortcutFormat {
    /// Formats Electron accelerator strings (e.g. `CommandOrControl+K`) for macOS display.
    public static func display(_ accelerator: String) -> String {
        let trimmed = accelerator.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "" }
        var parts = trimmed.split(separator: "+", omittingEmptySubsequences: false).map(String.init)
        guard !parts.isEmpty else { return trimmed }
        let key = parts.removeLast()
        var prefix = ""
        for part in parts {
            switch part.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
            case "commandorcontrol", "command", "cmd": prefix += "⌘"
            case "control", "ctrl": prefix += "⌃"
            case "shift": prefix += "⇧"
            case "alt", "option": prefix += "⌥"
            default: prefix += part
            }
        }
        return prefix + displayKey(key)
    }

    private static func displayKey(_ key: String) -> String {
        let normalized = key.trimmingCharacters(in: .whitespacesAndNewlines)
        if normalized.count == 1 { return normalized.uppercased() }
        if normalized.hasPrefix("Key"), normalized.count == 4 { return String(normalized.dropFirst(3)).uppercased() }
        if normalized == "Comma" { return "," }
        if normalized == "Period" { return "." }
        if normalized == "Slash" { return "/" }
        if normalized == "Backslash" { return "\\" }
        return normalized
    }
}
