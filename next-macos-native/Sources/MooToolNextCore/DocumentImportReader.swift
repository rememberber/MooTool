import Foundation

public enum DocumentImportReader {
    public static func read(_ urls: [URL], toolID: String) throws -> [DocumentImportItem] {
        guard ["json", "quickNote"].contains(toolID) else { throw ToolError("未知文档库。") }
        let extensions: Set<String> = toolID == "json" ? ["json"] : ["md", "markdown", "txt", "text", "log"]
        let keys: Set<URLResourceKey> = [.isDirectoryKey, .isRegularFileKey, .isSymbolicLinkKey, .fileSizeKey]
        var items: [DocumentImportItem] = [], totalBytes = 0
        func append(_ file: URL, path: String) throws {
            let values = try file.resourceValues(forKeys: keys)
            guard values.isSymbolicLink != true, values.isRegularFile == true else { throw ToolError("只能导入普通文本文件，不支持符号链接。") }
            guard items.count < 500, let size = values.fileSize, size <= 10 * 1024 * 1024, totalBytes + size <= 32 * 1024 * 1024 else { throw ToolError("一次最多导入 500 份文档、32 MB 内容，单个文件不超过 10 MB。") }
            let data = try Data(contentsOf: file)
            guard data.count <= 10 * 1024 * 1024, totalBytes + data.count <= 32 * 1024 * 1024,
                  let text = String(data: data, encoding: .utf8) else { throw ToolError("\(file.lastPathComponent) 不是 UTF-8 文本或超过大小限制。") }
            totalBytes += data.count
            items.append(DocumentImportItem(relativePath: path, content: text.hasPrefix("\u{FEFF}") ? String(text.dropFirst()) : text))
        }
        for url in urls {
            guard url.isFileURL else { throw ToolError("请选择本地文件。") }
            let values = try url.resourceValues(forKeys: keys)
            guard values.isSymbolicLink != true else { throw ToolError("不导入符号链接，请选择实际文件或文件夹。") }
            if values.isDirectory == true {
                let root = url.resolvingSymlinksInPath().standardizedFileURL
                var traversalError: Error?
                guard let enumerator = FileManager.default.enumerator(at: root, includingPropertiesForKeys: Array(keys), options: [.skipsHiddenFiles, .skipsPackageDescendants], errorHandler: { _, error in traversalError = error; return false }) else { throw ToolError("无法读取文件夹。") }
                for case let file as URL in enumerator {
                    let child = try file.resourceValues(forKeys: keys)
                    if child.isSymbolicLink == true { enumerator.skipDescendants(); continue }
                    if child.isRegularFile == true && extensions.contains(file.pathExtension.lowercased()) {
                        let components = file.standardizedFileURL.pathComponents
                        guard components.starts(with: root.pathComponents) else { throw ToolError("导入文件超出了所选文件夹。") }
                        let relative = components.dropFirst(root.pathComponents.count).joined(separator: "/")
                        try append(file, path: url.lastPathComponent + "/" + relative)
                    }
                }
                if let traversalError { throw traversalError }
            } else {
                guard extensions.contains(url.pathExtension.lowercased()) else { throw ToolError(toolID == "json" ? "JSON 文档库支持 .json 文件。" : "随手记支持 Markdown、TXT、TEXT 和 LOG 文件。") }
                try append(url, path: url.lastPathComponent)
            }
        }
        guard !items.isEmpty else { throw ToolError("没有找到可导入的文档。") }
        return items.sorted { $0.relativePath.localizedStandardCompare($1.relativePath) == .orderedAscending }
    }
}
