import Foundation

public struct ElectronImportPreview: Equatable, Sendable {
    public var storePath: String
    public var customGroupCount: Int
    public var hiddenToolCount: Int
    public var paneKeyCount: Int
    public var hasEncryptedSecrets: Bool
    public var warnings: [String]
}

public struct ElectronImportPatch: Equatable, Sendable {
    public var showRecent: Bool?
    public var hideNavigationTitles: Bool?
    public var showNavigationSeparators: Bool?
    public var hiddenNavigationToolIds: [String]?
    public var customGroups: [CustomToolGroup]?
    public var layoutPaneSizes: [String: [Double]]?
    public var proxyEnabled: Bool?
    public var proxyHost: String?
    public var proxyPort: Int?
    public var proxyUsername: String?
    public var proxyPassword: String?
    public var editorFontSize: Double?
    public var wrapLines: Bool?
    public var vaultAutoCommit: Bool?
    public var vaultAutoCommitIdleSeconds: Int?
    public var vaultAutoCommitInactiveSeconds: Int?
    public var vaultAutoPullMinutes: Int?
}

public enum ElectronStoreImport {
    public static func defaultStoreURL() -> URL? {
        let url = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/Application Support/MooTool/mootool-next.json")
        return FileManager.default.fileExists(atPath: url.path) ? url : nil
    }

    public static func preview(at url: URL, knownToolIDs: Set<String>, currentPaneSizes: [String: [Double]] = [:]) throws -> ElectronImportPreview {
        let loaded = try parseStore(at: url, knownToolIDs: knownToolIDs, currentPaneSizes: currentPaneSizes)
        var warnings: [String] = []
        if loaded.hasEncryptedSecrets { warnings.append("Electron 中存在 safeStorage 加密密钥，代理密码等不会导入。") }
        if loaded.patch.customGroups == nil && loaded.patch.hiddenNavigationToolIds == nil && loaded.patch.layoutPaneSizes == nil {
            warnings.append("未找到可合并的布局或分组数据。")
        }
        return ElectronImportPreview(
            storePath: url.path,
            customGroupCount: loaded.patch.customGroups?.count ?? 0,
            hiddenToolCount: loaded.patch.hiddenNavigationToolIds?.count ?? 0,
            paneKeyCount: loaded.patch.layoutPaneSizes?.count ?? 0,
            hasEncryptedSecrets: loaded.hasEncryptedSecrets,
            warnings: warnings)
    }

    public static func loadPatch(at url: URL, knownToolIDs: Set<String>, merging currentPaneSizes: [String: [Double]] = [:]) throws -> ElectronImportPatch {
        try parseStore(at: url, knownToolIDs: knownToolIDs, currentPaneSizes: currentPaneSizes).patch
    }

    public static func apply(_ patch: ElectronImportPatch, to snapshot: inout WorkspaceSnapshot) throws {
        if let value = patch.showRecent { snapshot.showRecent = value }
        if let value = patch.hideNavigationTitles { snapshot.hideNavigationTitles = value }
        if let value = patch.showNavigationSeparators { snapshot.showNavigationSeparators = value }
        if let value = patch.hiddenNavigationToolIds { snapshot.hiddenNavigationToolIds = value }
        if let value = patch.customGroups { snapshot.customGroups = CustomToolGroupRules.sanitized(value) }
        if let value = patch.layoutPaneSizes { snapshot.layoutPaneSizes = value }
        _ = try snapshot.validated()
    }

    private struct LoadedStore {
        var patch: ElectronImportPatch
        var hasEncryptedSecrets: Bool
    }

    private static func parseStore(at url: URL, knownToolIDs: Set<String>, currentPaneSizes: [String: [Double]]) throws -> LoadedStore {
        let root = try readJSONObject(at: url)
        let hasEncryptedSecrets = encryptedSecrets(in: root)
        guard let settings = root["settings"] as? [String: Any] else { throw ToolError("不是有效的 Electron 工作区文件（缺少 settings）。") }
        let layout = settings["layout"] as? [String: Any] ?? [:]
        let network = settings["network"] as? [String: Any] ?? [:]
        let editor = settings["editor"] as? [String: Any] ?? [:]
        let vault = settings["vault"] as? [String: Any] ?? [:]

        let electronPanes = parsePaneSizes(layout["paneSizes"])
        let mergedPanes = electronPanes.isEmpty ? nil : ElectronPaneSizeImport.mergeElectronIntoNative(electronPanes, current: currentPaneSizes)

        let groups = parseCustomGroups(layout["customGroups"], knownToolIDs: knownToolIDs)
        let hidden = parseStringArray(layout["hiddenNavigationToolIds"]).filter { knownToolIDs.contains($0) && $0 != "mootool" }
        let uniqueHidden = Array(Set(hidden)).sorted()

        var patch = ElectronImportPatch()
        if let value = layout["showRecent"] as? Bool { patch.showRecent = value }
        if let value = layout["hideNavigationTitles"] as? Bool { patch.hideNavigationTitles = value }
        if let value = layout["showSeparators"] as? Bool { patch.showNavigationSeparators = value }
        if !uniqueHidden.isEmpty { patch.hiddenNavigationToolIds = uniqueHidden }
        if !groups.isEmpty { patch.customGroups = groups }
        if let mergedPanes, !mergedPanes.isEmpty { patch.layoutPaneSizes = mergedPanes }
        if let value = network["proxyEnabled"] as? Bool { patch.proxyEnabled = value }
        if let value = network["proxyHost"] as? String, !value.isEmpty { patch.proxyHost = value }
        if let port = parseProxyPort(network["proxyPort"]) { patch.proxyPort = port }
        if let value = network["proxyUsername"] as? String, !value.isEmpty { patch.proxyUsername = value }
        if !hasEncryptedSecrets, let value = network["proxyPassword"] as? String, !value.isEmpty { patch.proxyPassword = value }
        if let size = editor["jsonFontSize"] as? Double { patch.editorFontSize = size }
        else if let size = editor["jsonFontSize"] as? Int { patch.editorFontSize = Double(size) }
        if let value = editor["softWrap"] as? Bool { patch.wrapLines = value }
        if let value = vault["autoCommit"] as? Bool { patch.vaultAutoCommit = value }
        if let idle = vault["autoCommitIdleSeconds"] as? Int { patch.vaultAutoCommitIdleSeconds = max(5, idle) }
        if let inactive = vault["autoCommitInactiveSeconds"] as? Int { patch.vaultAutoCommitInactiveSeconds = max(5, inactive) }
        if let pull = vault["autoPullMinutes"] as? Int { patch.vaultAutoPullMinutes = max(0, pull) }
        return LoadedStore(patch: patch, hasEncryptedSecrets: hasEncryptedSecrets)
    }

    private static func readJSONObject(at url: URL) throws -> [String: Any] {
        let data = try Data(contentsOf: url)
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ToolError("无法解析 Electron 工作区 JSON。")
        }
        return root
    }

    private static func encryptedSecrets(in root: [String: Any]) -> Bool {
        guard let secrets = root["secrets"] as? [String: Any] else { return false }
        return secrets.values.contains { value in
            guard let text = value as? String else { return false }
            return !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    private static func parseStringArray(_ value: Any?) -> [String] {
        guard let array = value as? [Any] else { return [] }
        return array.compactMap { $0 as? String }
    }

    private static func parseProxyPort(_ value: Any?) -> Int? {
        if let number = value as? Int { return (1...65535).contains(number) ? number : nil }
        if let text = value as? String, let number = Int(text.trimmingCharacters(in: .whitespacesAndNewlines)) {
            return (1...65535).contains(number) ? number : nil
        }
        return nil
    }

    private static func parsePaneSizes(_ value: Any?) -> [String: [Double]] {
        guard let map = value as? [String: Any] else { return [:] }
        var result: [String: [Double]] = [:]
        for (key, raw) in map {
            guard let array = raw as? [Any] else { continue }
            let numbers = array.compactMap { element -> Double? in
                if let number = element as? Double { return number }
                if let number = element as? Int { return Double(number) }
                return nil
            }
            if !numbers.isEmpty { result[key] = numbers }
        }
        return result
    }

    private static func parseCustomGroups(_ value: Any?, knownToolIDs: Set<String>) -> [CustomToolGroup] {
        guard let array = value as? [[String: Any]] else { return [] }
        return array.compactMap { item in
            let name = (item["name"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let id = (item["id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? UUID().uuidString
            let toolIds = parseStringArray(item["toolIds"]).filter { knownToolIDs.contains($0) }
            guard !name.isEmpty, !toolIds.isEmpty else { return nil }
            return CustomToolGroup(id: id, name: name, toolIds: toolIds)
        }
    }
}

private extension String {
    var nonEmpty: String? { isEmpty ? nil : self }
}
