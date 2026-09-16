import Foundation
import Yams

public struct ElectronVaultFolderImportPreview: Equatable, Sendable {
    public var quickNoteRoot: String
    public var jsonRoot: String
    public var quickNoteFileCount: Int
    public var jsonFileCount: Int
    public var attachmentFileCount: Int
    public var warnings: [String]
}

public struct ElectronVaultFolderQuickNote: Equatable, Sendable {
    public var relativePath: String
    public var content: String
    public var options: QuickNoteOptions
    public var modified: Date?
    public init(relativePath: String, content: String, options: QuickNoteOptions, modified: Date? = nil) {
        self.relativePath = relativePath
        self.content = content
        self.options = options
        self.modified = modified
    }
}

public enum ElectronVaultFolderImport {
    private static let quickNoteExtensions: Set<String> = ["md", "markdown", "txt", "text", "log", "json", "java", "js", "ts", "py", "xml", "yaml", "yml", "sql"]
    private static let jsonExtensions: Set<String> = ["json"]
    private static let attachmentExtensions: Set<String> = ["png", "jpg", "jpeg", "gif", "bmp", "webp"]
    private static let maxDepth = 64
    private static let maxFileBytes = 10 * 1024 * 1024
    private static let maxAttachmentBytes = 20 * 1024 * 1024

    public static func preview(quickNoteRoot: URL, jsonRoot: URL) throws -> ElectronVaultFolderImportPreview {
        var warnings: [String] = []
        let notes = try collectQuickNotes(at: quickNoteRoot, warnings: &warnings)
        let json = try collectJson(at: jsonRoot, warnings: &warnings)
        let attachments = try countAttachments(at: quickNoteRoot)
        if notes.isEmpty && json.isEmpty && attachments == 0 {
            warnings.append("未在 Electron 文档库目录中找到可导入的文本或图片文件。")
        }
        return ElectronVaultFolderImportPreview(
            quickNoteRoot: quickNoteRoot.path,
            jsonRoot: jsonRoot.path,
            quickNoteFileCount: notes.count,
            jsonFileCount: json.count,
            attachmentFileCount: attachments,
            warnings: warnings)
    }

    public static func loadQuickNotes(at root: URL) throws -> [ElectronVaultFolderQuickNote] {
        var warnings: [String] = []
        return try collectQuickNotes(at: root, warnings: &warnings)
    }

    public static func loadJsonItems(at root: URL) throws -> [DocumentImportItem] {
        var warnings: [String] = []
        return try collectJson(at: root, warnings: &warnings).map { DocumentImportItem(relativePath: $0.relativePath, content: $0.content) }
    }

    public static func filterNewQuickNotes(_ items: [ElectronVaultFolderQuickNote], existing: [SavedDocument]) -> [ElectronVaultFolderQuickNote] {
        items.filter { item in
            let title = (item.relativePath as NSString).lastPathComponent
            return !existing.contains { $0.toolID == "quickNote" && titlesMatch($0.title, title) && $0.content == item.content }
        }
    }

    public static func filterNewJson(_ items: [DocumentImportItem], existing: [SavedDocument]) -> [DocumentImportItem] {
        items.filter { item in
            let title = (item.relativePath as NSString).lastPathComponent
            return !existing.contains { $0.toolID == "json" && titlesMatch($0.title, title) && $0.content == item.content }
        }
    }

    public static func documentImportItems(from notes: [ElectronVaultFolderQuickNote]) -> [DocumentImportItem] {
        notes.map { DocumentImportItem(relativePath: $0.relativePath, content: $0.content) }
    }

    /// Rewrites Electron `attachments/*` references to native managed paths.
    public static func rewriteElectronAttachments(
        in content: String,
        quickNoteRoot: URL,
        existingManifest: [NoteAttachment]
    ) throws -> (content: String, payloads: [NoteImagePayload]) {
        var replacements: [(NSRange, String)] = []
        var payloads: [NoteImagePayload] = []
        var manifest = existingManifest
        for reference in MarkdownImageReference.parse(content) {
            guard reference.path.hasPrefix("attachments/") else { continue }
            let fileURL = quickNoteRoot.appendingPathComponent(reference.path)
            guard FileManager.default.fileExists(atPath: fileURL.path) else { continue }
            let payload = try NoteImagePayload(file: fileURL)
            let attachment: NoteAttachment
            if let existing = manifest.first(where: { $0.sha256 == payload.attachment.sha256 }) {
                attachment = existing
            } else {
                payloads.append(payload)
                manifest.append(payload.attachment)
                attachment = payload.attachment
            }
            let markdown = "![\(reference.alt)](\(attachment.path))"
            replacements.append((reference.range, markdown))
        }
        guard !replacements.isEmpty else { return (content, payloads) }
        var result = content
        for entry in replacements.sorted(by: { $0.0.location > $1.0.location }) {
            result = (result as NSString).replacingCharacters(in: entry.0, with: entry.1)
        }
        return (result, payloads)
    }

    private struct CollectedFile: Equatable {
        var relativePath: String
        var content: String
        var modified: Date?
    }

    private static func titlesMatch(_ saved: String, _ imported: String) -> Bool {
        if saved == imported { return true }
        let savedBase = (saved as NSString).deletingPathExtension
        let importedBase = (imported as NSString).deletingPathExtension
        return savedBase.caseInsensitiveCompare(importedBase) == .orderedSame
    }

    private static func collectQuickNotes(at root: URL, warnings: inout [String]) throws -> [ElectronVaultFolderQuickNote] {
        guard FileManager.default.fileExists(atPath: root.path) else { return [] }
        let files = try collectTextFiles(at: root, allowedExtensions: quickNoteExtensions, skipDirectoryNames: ["attachments"])
        var totalBytes = 0
        var result: [ElectronVaultFolderQuickNote] = []
        for file in files {
            let bytes = file.content.utf8.count
            guard result.count < 500, bytes <= maxFileBytes, totalBytes + bytes <= 32 * 1024 * 1024 else {
                warnings.append("随手记磁盘导入达到 500 份或 32 MB 上限，其余文件已跳过。")
                break
            }
            totalBytes += bytes
            let parsed = parseElectronNote(file.content, fallbackTitle: (file.relativePath as NSString).deletingPathExtension, modified: file.modified)
            let options = parsed.options ?? QuickNoteOptions.forDocument(file.relativePath)
            result.append(ElectronVaultFolderQuickNote(relativePath: file.relativePath, content: parsed.body, options: options, modified: file.modified))
        }
        return result.sorted { $0.relativePath.localizedStandardCompare($1.relativePath) == .orderedAscending }
    }

    private static func collectJson(at root: URL, warnings: inout [String]) throws -> [CollectedFile] {
        guard FileManager.default.fileExists(atPath: root.path) else { return [] }
        let files = try collectTextFiles(at: root, allowedExtensions: jsonExtensions, skipDirectoryNames: [])
        var totalBytes = 0
        var result: [CollectedFile] = []
        for file in files {
            let bytes = file.content.utf8.count
            guard result.count < 500, bytes <= maxFileBytes, totalBytes + bytes <= 32 * 1024 * 1024 else {
                warnings.append("JSON 磁盘导入达到 500 份或 32 MB 上限，其余文件已跳过。")
                break
            }
            totalBytes += bytes
            result.append(file)
        }
        return result.sorted { $0.relativePath.localizedStandardCompare($1.relativePath) == .orderedAscending }
    }

    private static func countAttachments(at root: URL) throws -> Int {
        let directory = root.appendingPathComponent("attachments", isDirectory: true)
        guard FileManager.default.fileExists(atPath: directory.path) else { return 0 }
        var count = 0
        let keys: Set<URLResourceKey> = [.isRegularFileKey, .isSymbolicLinkKey, .fileSizeKey]
        guard let enumerator = FileManager.default.enumerator(at: directory, includingPropertiesForKeys: Array(keys), options: [.skipsHiddenFiles, .skipsPackageDescendants]) else { return 0 }
        for case let file as URL in enumerator {
            let values = try file.resourceValues(forKeys: keys)
            guard values.isSymbolicLink != true, values.isRegularFile == true else { continue }
            let ext = file.pathExtension.lowercased()
            guard attachmentExtensions.contains(ext), (values.fileSize ?? 0) <= maxAttachmentBytes else { continue }
            count += 1
        }
        return count
    }

    private static func collectTextFiles(at root: URL, allowedExtensions: Set<String>, skipDirectoryNames: Set<String>) throws -> [CollectedFile] {
        let keys: Set<URLResourceKey> = [.isDirectoryKey, .isRegularFileKey, .isSymbolicLinkKey, .fileSizeKey, .contentModificationDateKey]
        var result: [CollectedFile] = []
        func visit(_ directory: URL, relativePrefix: String, depth: Int) throws {
            guard depth <= maxDepth else { return }
            let entries = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: Array(keys), options: [.skipsHiddenFiles])
            for entry in entries.sorted(by: { $0.lastPathComponent.localizedStandardCompare($1.lastPathComponent) == .orderedAscending }) {
                let values = try entry.resourceValues(forKeys: keys)
                if values.isSymbolicLink == true { continue }
                let name = entry.lastPathComponent
                if values.isDirectory == true {
                    if skipDirectoryNames.contains(name) { continue }
                    let next = relativePrefix.isEmpty ? name : relativePrefix + "/" + name
                    try visit(entry, relativePrefix: next, depth: depth + 1)
                    continue
                }
                guard values.isRegularFile == true, allowedExtensions.contains(entry.pathExtension.lowercased()) else { continue }
                guard let size = values.fileSize, size <= maxFileBytes else { continue }
                let relative = relativePrefix.isEmpty ? name : relativePrefix + "/" + name
                let data = try Data(contentsOf: entry)
                guard data.count <= maxFileBytes, let text = String(data: data, encoding: .utf8) else { continue }
                let content = text.hasPrefix("\u{FEFF}") ? String(text.dropFirst()) : text
                result.append(CollectedFile(relativePath: relative, content: content, modified: values.contentModificationDate))
            }
        }
        try visit(root.standardizedFileURL, relativePrefix: "", depth: 0)
        return result
    }

    private static func parseElectronNote(_ raw: String, fallbackTitle: String, modified: Date?) -> (body: String, options: QuickNoteOptions?) {
        let pattern = #"^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n)?"#
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: raw, range: NSRange(location: 0, length: (raw as NSString).length)),
              match.numberOfRanges >= 2 else {
            return (raw, nil)
        }
        let yamlRange = match.range(at: 1)
        let yaml = (raw as NSString).substring(with: yamlRange)
        let body = (raw as NSString).substring(from: match.range.location + match.range.length)
        var options = QuickNoteOptions.forDocument(fallbackTitle)
        if let values = try? Yams.load(yaml: yaml) as? [String: Any] {
            if let title = values["title"] as? String, !title.isEmpty { /* title from path */ }
            if let syntax = values["syntax"] as? String { options.syntax = mapSyntax(syntax) }
            if let color = values["color"] as? String { options.color = mapColor(color) }
            if let font = values["font_name"] as? String { options.fontName = String(font.prefix(256)) }
            if let size = values["font_size"] as? String, let number = Double(size) { options.fontSize = min(max(number, 8), 48) }
            else if let size = values["font_size"] as? Double { options.fontSize = min(max(size, 8), 48) }
            if let spacing = values["line_spacing"] as? String, let number = Double(spacing), QuickNoteOptions.lineSpacings.contains(number) { options.lineSpacing = number }
            if let wrap = values["line_wrap"] as? String { options.lineWrap = wrap != "0" }
            if let wrap = values["line_wrap"] as? Bool { options.lineWrap = wrap }
        }
        return (body, options)
    }

    private static func mapSyntax(_ value: String) -> NoteSyntax {
        switch value.lowercased() {
        case "text/markdown": return .markdown
        case "text/plain": return .plain
        case "application/json": return .json
        case "text/java": return .java
        case "text/javascript": return .javascript
        case "text/typescript": return .typescript
        case "text/python": return .python
        case "text/xml": return .xml
        case "text/yaml": return .yaml
        case "text/sql": return .sql
        default: return .plain
        }
    }

    private static func mapColor(_ value: String) -> NoteColor {
        switch value.lowercased() {
        case "coral": return .coral
        case "yellow": return .yellow
        case "green": return .green
        case "blue": return .blue
        case "purple": return .purple
        case "red": return .red
        default: return .default
        }
    }
}
