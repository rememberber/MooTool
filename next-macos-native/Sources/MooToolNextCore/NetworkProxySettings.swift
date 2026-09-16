import Foundation
import CFNetwork

public struct NetworkProxySettings: Equatable {
    public var enabled = false
    public var host = ""
    public var port = 8080
    public var username = ""
    public var password = ""

    public init(enabled: Bool = false, host: String = "", port: Int = 8080, username: String = "", password: String = "") {
        self.enabled = enabled
        self.host = host
        self.port = port
        self.username = username
        self.password = password
    }

    public static func current(defaults: UserDefaults = UserDefaults(suiteName: Product.bundleID) ?? .standard) -> NetworkProxySettings {
        NetworkProxySettings(
            enabled: defaults.bool(forKey: Keys.enabled),
            host: defaults.string(forKey: Keys.host) ?? "",
            port: defaults.object(forKey: Keys.port) as? Int ?? Int(defaults.string(forKey: Keys.port) ?? "") ?? 8080,
            username: defaults.string(forKey: Keys.username) ?? "",
            password: defaults.string(forKey: Keys.password) ?? ""
        )
    }

    public func connectionProxyDictionary() -> [AnyHashable: Any]? {
        guard enabled else { return nil }
        let trimmed = host.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, (1...65_535).contains(port) else { return nil }
        var dictionary: [AnyHashable: Any] = [
            kCFNetworkProxiesHTTPEnable as String: 1,
            kCFNetworkProxiesHTTPProxy as String: trimmed,
            kCFNetworkProxiesHTTPPort as String: port,
            kCFNetworkProxiesHTTPSEnable as String: 1,
            kCFNetworkProxiesHTTPSProxy as String: trimmed,
            kCFNetworkProxiesHTTPSPort as String: port,
        ]
        if !username.isEmpty {
            dictionary[kCFProxyUsernameKey as String] = username
            dictionary[kCFProxyPasswordKey as String] = password
        }
        return dictionary
    }

    private enum Keys {
        static let enabled = "network.proxyEnabled"
        static let host = "network.proxyHost"
        static let port = "network.proxyPort"
        static let username = "network.proxyUsername"
        static let password = "network.proxyPassword"
    }
}
