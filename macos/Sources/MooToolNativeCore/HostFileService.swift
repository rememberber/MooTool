import Foundation

public struct HostEntry: Equatable, Identifiable, Sendable {
    public let id: String
    public let address: String
    public let hosts: [String]
    public let isEnabled: Bool

    public init(address: String, hosts: [String], isEnabled: Bool) {
        self.address = address
        self.hosts = hosts
        self.isEnabled = isEnabled
        self.id = "\(isEnabled)-\(address)-\(hosts.joined(separator: ","))"
    }
}

public enum HostFileService {
    public static func parse(_ content: String) -> [HostEntry] {
        content
            .components(separatedBy: .newlines)
            .compactMap(parseLine)
    }

    public static func render(_ entries: [HostEntry]) -> String {
        entries
            .map { entry in
                let line = "\(entry.address) \(entry.hosts.joined(separator: " "))"
                return entry.isEnabled ? line : "# \(line)"
            }
            .joined(separator: "\n")
    }

    private static func parseLine(_ rawLine: String) -> HostEntry? {
        let trimmed = rawLine.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return nil
        }

        let isEnabled = !trimmed.hasPrefix("#")
        let uncommented = isEnabled
            ? trimmed
            : trimmed.dropFirst().trimmingCharacters(in: .whitespacesAndNewlines)
        let parts = uncommented
            .split(whereSeparator: \.isWhitespace)
            .map(String.init)

        guard parts.count >= 2, isHostAddress(parts[0]) else {
            return nil
        }

        return HostEntry(address: parts[0], hosts: Array(parts.dropFirst()), isEnabled: isEnabled)
    }

    private static func isHostAddress(_ value: String) -> Bool {
        value.contains(".") || value.contains(":")
    }
}
