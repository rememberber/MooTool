import Foundation

public struct UpdateCheckResult: Equatable, Sendable {
    public enum Status: Equatable, Sendable { case upToDate, available, failed }
    public var status: Status
    public var currentVersion: String
    public var latestVersion: String?
    public var releaseURL: URL?
    public var message: String?

    public init(status: Status, currentVersion: String, latestVersion: String? = nil, releaseURL: URL? = nil, message: String? = nil) {
        self.status = status
        self.currentVersion = currentVersion
        self.latestVersion = latestVersion
        self.releaseURL = releaseURL
        self.message = message
    }
}

public enum GitHubUpdateService {
    public static let releasesPage = URL(string: "https://github.com/rememberber/MooTool/releases")!
    private static let latestAPI = URL(string: "https://api.github.com/repos/rememberber/MooTool/releases/latest")!

    public static func compare(_ lhs: String, _ rhs: String) -> ComparisonResult {
        let left = parseVersion(lhs)
        let right = parseVersion(rhs)
        let count = max(left.count, right.count)
        for index in 0..<count {
            let l = index < left.count ? left[index] : 0
            let r = index < right.count ? right[index] : 0
            if l < r { return .orderedAscending }
            if l > r { return .orderedDescending }
        }
        return .orderedSame
    }

    public static func check(currentVersion: String) async -> UpdateCheckResult {
        let trimmed = currentVersion.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed != "Development" else {
            return UpdateCheckResult(status: .failed, currentVersion: trimmed, message: "Development build")
        }
        do {
            var request = URLRequest(url: latestAPI)
            request.timeoutInterval = 15
            request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
            request.setValue("MooTool-Next-Native", forHTTPHeaderField: "User-Agent")
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                return UpdateCheckResult(status: .failed, currentVersion: trimmed, message: "HTTP \((response as? HTTPURLResponse)?.statusCode ?? 0)")
            }
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let tag = json["tag_name"] as? String else {
                return UpdateCheckResult(status: .failed, currentVersion: trimmed, message: "Invalid release payload")
            }
            let latest = normalizeTag(tag)
            let html = json["html_url"] as? String
            let url = html.flatMap(URL.init(string:)) ?? releasesPage
            if compare(trimmed, latest) == .orderedAscending {
                return UpdateCheckResult(status: .available, currentVersion: trimmed, latestVersion: latest, releaseURL: url)
            }
            return UpdateCheckResult(status: .upToDate, currentVersion: trimmed, latestVersion: latest, releaseURL: url)
        } catch {
            return UpdateCheckResult(status: .failed, currentVersion: trimmed, message: error.localizedDescription)
        }
    }

    private static func normalizeTag(_ tag: String) -> String {
        var value = tag.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.hasPrefix("v") || value.hasPrefix("V") { value.removeFirst() }
        return value
    }

    private static func parseVersion(_ value: String) -> [Int] {
        normalizeTag(value)
            .split(whereSeparator: { $0 == "." || $0 == "-" || $0 == "_" })
            .map { part in
                let digits = part.prefix { $0.isNumber }
                return Int(digits) ?? 0
            }
    }
}
