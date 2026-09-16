import Foundation

public enum DocumentImportReader {
    public static func read(_ urls: [URL], toolID: String, language: AppLanguage = .zhCN) throws -> [DocumentImportItem] {
        guard ["json", "quickNote"].contains(toolID) else { throw ToolError(loc("vault.import.error.unknownVault", language: language)) }
        let extensions: Set<String> = toolID == "json" ? ["json"] : ["md", "markdown", "txt", "text", "log"]
        let keys: Set<URLResourceKey> = [.isDirectoryKey, .isRegularFileKey, .isSymbolicLinkKey, .fileSizeKey]
        var items: [DocumentImportItem] = [], totalBytes = 0
        func append(_ file: URL, path: String) throws {
            let values = try file.resourceValues(forKeys: keys)
            guard values.isSymbolicLink != true, values.isRegularFile == true else {
                throw ToolError(loc("vault.import.error.symlinkFile", language: language))
            }
            guard items.count < 500, let size = values.fileSize, size <= 10 * 1024 * 1024, totalBytes + size <= 32 * 1024 * 1024 else {
                throw ToolError(loc("vault.import.error.limits", language: language))
            }
            let data = try Data(contentsOf: file)
            guard data.count <= 10 * 1024 * 1024, totalBytes + data.count <= 32 * 1024 * 1024,
                  let text = String(data: data, encoding: .utf8) else {
                throw ToolError(loc("vault.import.error.notUtf8", language: language, replacements: ["name": file.lastPathComponent]))
            }
            totalBytes += data.count
            items.append(DocumentImportItem(relativePath: path, content: text.hasPrefix("\u{FEFF}") ? String(text.dropFirst()) : text))
        }
        for url in urls {
            guard url.isFileURL else { throw ToolError(loc("vault.import.error.localFileOnly", language: language)) }
            let values = try url.resourceValues(forKeys: keys)
            guard values.isSymbolicLink != true else { throw ToolError(loc("vault.import.error.noSymlinks", language: language)) }
            if values.isDirectory == true {
                let root = url.resolvingSymlinksInPath().standardizedFileURL
                var traversalError: Error?
                guard let enumerator = FileManager.default.enumerator(at: root, includingPropertiesForKeys: Array(keys), options: [.skipsHiddenFiles, .skipsPackageDescendants], errorHandler: { _, error in traversalError = error; return false }) else {
                    throw ToolError(loc("vault.import.error.unreadableFolder", language: language))
                }
                for case let file as URL in enumerator {
                    let child = try file.resourceValues(forKeys: keys)
                    if child.isSymbolicLink == true { enumerator.skipDescendants(); continue }
                    if child.isRegularFile == true && extensions.contains(file.pathExtension.lowercased()) {
                        let components = file.standardizedFileURL.pathComponents
                        guard components.starts(with: root.pathComponents) else {
                            throw ToolError(loc("vault.import.error.outsideFolder", language: language))
                        }
                        let relative = components.dropFirst(root.pathComponents.count).joined(separator: "/")
                        try append(file, path: url.lastPathComponent + "/" + relative)
                    }
                }
                if let traversalError { throw traversalError }
            } else {
                guard extensions.contains(url.pathExtension.lowercased()) else {
                    throw ToolError(loc(toolID == "json" ? "vault.import.error.jsonExtensions" : "vault.import.error.noteExtensions", language: language))
                }
                try append(url, path: url.lastPathComponent)
            }
        }
        guard !items.isEmpty else { throw ToolError(loc("vault.error.noImportableDocuments", language: language)) }
        return items.sorted { $0.relativePath.localizedStandardCompare($1.relativePath) == .orderedAscending }
    }

    private static func loc(_ key: String, language: AppLanguage, replacements: [String: String] = [:]) -> String {
        var text = AppLocalization.string(key, language: language)
        for (placeholder, value) in replacements {
            text = text.replacingOccurrences(of: "{\(placeholder)}", with: value)
        }
        return text
    }
}
