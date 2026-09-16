import Foundation

/// Mirrors the in-app document vault to on-disk folders aligned with Electron defaults (`json-vault`, `quick-notes`).
public enum VaultFilesystemSync {
    public static func root(toolID: String, workspace: URL) -> URL {
        switch toolID {
        case "json": return workspace.appendingPathComponent("json-vault", isDirectory: true)
        case "quickNote": return workspace.appendingPathComponent("quick-notes", isDirectory: true)
        default: fatalError("Unsupported vault tool: \(toolID)")
        }
    }

    public static func sync(vault: DocumentVault, workspace: URL, noteAttachments: [NoteAttachment] = [], readNoteAttachment: ((NoteAttachment) throws -> Data)? = nil) throws {
        for toolID in ["json", "quickNote"] {
            let expected = try materializeTool(toolID: toolID, vault: vault, workspace: workspace)
            try pruneUnexpected(under: root(toolID: toolID, workspace: workspace), keeping: expected)
        }
        if let read = readNoteAttachment {
            _ = try mirrorQuickNoteAttachments(vault: vault, workspace: workspace, attachments: noteAttachments, read: read)
        }
    }

    public struct FileChange: Equatable, Sendable {
        public enum Kind: Equatable, Sendable { case updated, imported, deleted }
        public var kind: Kind
        public var relativePath: String
        public var documentID: UUID?
        public var content: String?
        public init(kind: Kind, relativePath: String, documentID: UUID? = nil, content: String? = nil) {
            self.kind = kind; self.relativePath = relativePath; self.documentID = documentID; self.content = content
        }
    }

    public static func documentID(forRelativePath path: String, toolID: String, vault: DocumentVault) -> UUID? {
        vault.documents.first { $0.toolID == toolID && vault.path(of: $0.id) == path }?.id
    }

    public static func baseline(for toolID: String, vault: DocumentVault) -> [String: String] {
        Dictionary(uniqueKeysWithValues: vault.documents.filter { $0.toolID == toolID }.map { (vault.path(of: $0.id), $0.content) })
    }

    /// Compares on-disk files with the last mirrored baseline. `baseline` maps vault-relative paths to UTF-8 content written by the app.
    public static func detectChanges(toolID: String, vault: DocumentVault, workspace: URL, baseline: [String: String]) throws -> [FileChange] {
        let base = root(toolID: toolID, workspace: workspace)
        guard FileManager.default.fileExists(atPath: base.path) else { return [] }
        var changes: [FileChange] = []
        var seen = Set<String>()
        let files = try listFiles(under: base)
        for (relative, url) in files {
            seen.insert(relative)
            if toolID == "quickNote" && (relative == "attachments" || relative.hasPrefix("attachments/")) { continue }
            let content = try String(contentsOf: url, encoding: .utf8)
            guard content.utf8.count <= 10 * 1024 * 1024 else { continue }
            if baseline[relative] == content { continue }
            if let id = documentID(forRelativePath: relative, toolID: toolID, vault: vault) {
                changes.append(FileChange(kind: .updated, relativePath: relative, documentID: id, content: content))
            } else {
                changes.append(FileChange(kind: .imported, relativePath: relative, content: content))
            }
        }
        for (path, _) in baseline where !seen.contains(path) {
            if let id = documentID(forRelativePath: path, toolID: toolID, vault: vault) {
                changes.append(FileChange(kind: .deleted, relativePath: path, documentID: id))
            }
        }
        return changes
    }

    public static func entryURL(entryID: UUID, vault: DocumentVault, workspace: URL) -> URL? {
        guard let toolID = vault.tool(of: entryID) else { return nil }
        let base = root(toolID: toolID, workspace: workspace)
        if vault.folders.contains(where: { $0.id == entryID }) {
            let path = vault.path(of: entryID)
            return base.appendingPathComponent(path, isDirectory: true)
        }
        guard let document = vault.documents.first(where: { $0.id == entryID }) else { return nil }
        return base.appendingPathComponent(vault.path(of: document.id))
    }

    private static func materializeTool(toolID: String, vault: DocumentVault, workspace: URL) throws -> Set<String> {
        let base = root(toolID: toolID, workspace: workspace)
        try FileManager.default.createDirectory(at: base, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        var expected = Set<String>()
        for folder in vault.folders where folder.toolID == toolID {
            let relative = vault.path(of: folder.id)
            expected.insert(relative)
            try FileManager.default.createDirectory(at: base.appendingPathComponent(relative, isDirectory: true),
                                                    withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        }
        for document in vault.documents where document.toolID == toolID {
            let relative = vault.path(of: document.id)
            expected.insert(relative)
            let file = base.appendingPathComponent(relative)
            try FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
            try Data(document.content.utf8).write(to: file, options: .atomic)
            try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: file.path)
        }
        return expected
    }

    static func mirrorQuickNoteAttachments(
        vault: DocumentVault,
        workspace: URL,
        attachments: [NoteAttachment],
        read: (NoteAttachment) throws -> Data
    ) throws -> Set<String> {
        let base = root(toolID: "quickNote", workspace: workspace)
        let noteBodies = vault.documents.filter { $0.toolID == "quickNote" }.map(\.content)
        let referenced = noteBodies.reduce(into: Set<String>()) { $0.formUnion(MarkdownImageReference.managedPaths(in: $1)) }
        var expected = Set<String>()
        for attachment in attachments where noteBodies.contains(where: { $0.contains(attachment.path) }) || referenced.contains(attachment.path) {
            let path = attachment.path
            expected.insert(path)
            let file = base
                .appendingPathComponent("attachments", isDirectory: true)
                .appendingPathComponent((path as NSString).lastPathComponent, isDirectory: false)
            try FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
            let data = try read(attachment)
            try data.write(to: file, options: .atomic)
            try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: file.path)
        }
        let attachRoot = base.appendingPathComponent("attachments", isDirectory: true)
        if FileManager.default.fileExists(atPath: attachRoot.path) {
            for name in try FileManager.default.contentsOfDirectory(atPath: attachRoot.path) {
                let relative = "attachments/\(name)"
                if !expected.contains(relative) { try? FileManager.default.removeItem(at: attachRoot.appendingPathComponent(name)) }
            }
        }
        return expected
    }

    private static func listFiles(under root: URL) throws -> [(String, URL)] {
        let fm = FileManager.default
        guard let enumerator = fm.enumerator(at: root, includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles]) else { return [] }
        var files: [(String, URL)] = []
        for case let url as URL in enumerator {
            let values = try url.resourceValues(forKeys: [.isDirectoryKey])
            if values.isDirectory == true { continue }
            let relative = url.path.replacingOccurrences(of: root.path + "/", with: "")
            if relative.hasPrefix(".git/") || relative == ".gitignore" { continue }
            files.append((relative, url))
        }
        return files
    }

    private static func pruneUnexpected(under root: URL, keeping expected: Set<String>) throws {
        let fm = FileManager.default
        guard let enumerator = fm.enumerator(at: root, includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles]) else { return }
        var files: [URL] = []
        for case let url as URL in enumerator {
            let values = try url.resourceValues(forKeys: [.isDirectoryKey])
            if values.isDirectory == true { continue }
            let relative = url.path.replacingOccurrences(of: root.path + "/", with: "")
            if relative.hasPrefix(".git/") || relative == ".gitignore" { continue }
            files.append(url)
        }
        for file in files {
            let relative = file.path.replacingOccurrences(of: root.path + "/", with: "")
            if !expected.contains(relative) { try? fm.removeItem(at: file) }
        }
    }
}
