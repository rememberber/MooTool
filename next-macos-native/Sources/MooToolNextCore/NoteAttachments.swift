import Foundation
import CryptoKit
import ImageIO
import UniformTypeIdentifiers

public struct NoteAttachment: Codable, Equatable, Sendable {
    public let path: String
    public let sha256: String
    public let bytes: Int
    public let width: Int
    public let height: Int
    public var markdown: String { "![image](\(path))" }
    public func validate() throws {
        guard Self.isManagedPath(path), path == "attachments/" + sha256 + "." + (path as NSString).pathExtension,
              (1...NoteImagePayload.maximumBytes).contains(bytes), width > 0, height > 0,
              width <= 20_000, height <= 20_000, width * height <= 40_000_000 else { throw ToolError("图片附件记录无效。") }
    }
    public static func isManagedPath(_ path: String) -> Bool {
        path.range(of: #"^attachments/[a-f0-9]{64}\.(png|jpg|gif|bmp|webp)\z"#, options: .regularExpression) != nil
    }
}

public struct NoteImagePayload: Sendable {
    public static let maximumBytes = 20 * 1024 * 1024
    public let data: Data
    public let attachment: NoteAttachment
    public init(data original: Data) throws {
        guard !original.isEmpty, original.count <= Self.maximumBytes,
              let source = CGImageSourceCreateWithData(original as CFData, [kCGImageSourceShouldCache: false] as CFDictionary),
              let type = CGImageSourceGetType(source) as String?,
              let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = props[kCGImagePropertyPixelWidth] as? Int, let height = props[kCGImagePropertyPixelHeight] as? Int,
              width > 0, height > 0, width <= 20_000, height <= 20_000, width * height <= 40_000_000,
              CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceThumbnailMaxPixelSize: 32, kCGImageSourceShouldCache: false] as CFDictionary) != nil else {
            throw ToolError("图片无效、超过 20 MB 或 4000 万像素（单边最多 2 万像素）。")
        }
        let extensions = [UTType.png.identifier: "png", UTType.jpeg.identifier: "jpg", UTType.gif.identifier: "gif", UTType.bmp.identifier: "bmp", UTType.webP.identifier: "webp"]
        var data = original
        let ext: String
        if let known = extensions[type] { ext = known }
        else if type == UTType.tiff.identifier {
            guard let image = CGImageSourceCreateImageAtIndex(source, 0, [kCGImageSourceShouldCache: false] as CFDictionary) else { throw ToolError("无法读取剪贴板图片。") }
            let output = NSMutableData()
            guard let destination = CGImageDestinationCreateWithData(output, UTType.png.identifier as CFString, 1, nil) else { throw ToolError("无法转换剪贴板图片。") }
            CGImageDestinationAddImage(destination, image, nil)
            guard CGImageDestinationFinalize(destination) else { throw ToolError("无法转换剪贴板图片。") }
            data = output as Data; ext = "png"
        } else { throw ToolError("支持 PNG、JPEG、GIF、BMP、WebP 图片及剪贴板 TIFF。") }
        guard data.count <= Self.maximumBytes else { throw ToolError("转换后的图片超过 20 MB。") }
        let hash = SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
        self.data = data; attachment = NoteAttachment(path: "attachments/\(hash).\(ext)", sha256: hash, bytes: data.count, width: width, height: height)
    }
    public init(file: URL) throws { try self.init(data: Self.readBounded(file)) }
    public static func readBounded(_ file: URL) throws -> Data {
        let values = try file.resourceValues(forKeys: [.isRegularFileKey, .isSymbolicLinkKey, .fileSizeKey])
        guard values.isRegularFile == true, values.isSymbolicLink != true, (values.fileSize ?? Int.max) <= maximumBytes else { throw ToolError("附件必须是 20 MB 以内的普通图片文件。") }
        let handle = try FileHandle(forReadingFrom: file); defer { try? handle.close() }
        let data = try handle.read(upToCount: maximumBytes + 1) ?? Data()
        guard data.count <= maximumBytes else { throw ToolError("图片超过 20 MB。") }; return data
    }
}

public struct NoteImageInsertion: Equatable {
    public let range: NSRange
    public let text: String
    public var caret: Int { range.location + (text as NSString).length }
    public init(content: String, selection: NSRange, markdown: String) {
        let source = content as NSString, start = min(max(0, selection.location), source.length)
        let length = min(max(0, selection.length), source.length - start), end = start + length
        let leading = start > 0 && source.character(at: start - 1) != 10 ? "\n" : ""
        let trailing = end < source.length && source.character(at: end) != 10 ? "\n" : ""
        range = NSRange(location: start, length: length); text = leading + markdown + trailing
    }
    public func applying(to content: String) -> String { (content as NSString).replacingCharacters(in: range, with: text) }
}

public struct MarkdownImageReference: Equatable, Sendable {
    public let range: NSRange
    public let alt: String
    public let path: String
    public static func parse(_ text: String) -> [Self] {
        let pattern = #"!\[((?:\\.|[^\]\\])*)\]\(\s*(<[^>\n]+>|[^\s)]+)(?:\s+\"[^\"\n]*\")?\s*\)"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let source = text as NSString
        let literals = codeRanges(text)
        var literalIndex = 0
        return regex.matches(in: text, range: NSRange(location: 0, length: source.length)).filter { match in
            if Task.isCancelled { return false }
            while literalIndex < literals.count, NSMaxRange(literals[literalIndex]) <= match.range.location { literalIndex += 1 }
            var cursor = match.range.location, escapes = 0
            while cursor > 0, source.character(at: cursor - 1) == 92 { escapes += 1; cursor -= 1 }
            return escapes.isMultiple(of: 2) && (literalIndex == literals.count || NSIntersectionRange(literals[literalIndex], match.range).length == 0)
        }.map {
            let raw = source.substring(with: $0.range(at: 2))
            return Self(range: $0.range, alt: source.substring(with: $0.range(at: 1)), path: raw.hasPrefix("<") ? String(raw.dropFirst().dropLast()) : raw)
        }
    }
    public static func managedPaths(in text: String) -> Set<String> { Set(parse(text).map(\.path).filter { $0.hasPrefix("attachments/") }) }
    private static func codeRanges(_ text: String) -> [NSRange] {
        guard text.contains("`") || text.contains("~~~") else { return [] }
        let source = text as NSString
        var ranges: [NSRange] = [], offset = 0, fence: (marker: UInt16, count: Int, start: Int)?
        for line in text.components(separatedBy: "\n") {
            if Task.isCancelled { return [] }
            let value = line as NSString; var start = 0
            while start < value.length, start < 4, value.character(at: start) == 32 { start += 1 }
            if start <= 3, start < value.length {
                let marker = value.character(at: start); var end = start
                if marker == 96 || marker == 126 {
                    while end < value.length, value.character(at: end) == marker { end += 1 }
                    if let open = fence {
                        if marker == open.marker, end - start >= open.count, value.substring(from: end).trimmingCharacters(in: .whitespaces).isEmpty {
                            ranges.append(NSRange(location: open.start, length: offset + value.length - open.start)); fence = nil
                        }
                    } else if end - start >= 3 { fence = (marker, end - start, offset) }
                }
            }
            offset += value.length + 1
        }
        if let fence { ranges.append(NSRange(location: fence.start, length: source.length - fence.start)) }
        let blocks = ranges
        let runs = (try? NSRegularExpression(pattern: "`+").matches(in: text, range: NSRange(location: 0, length: source.length))) ?? []
        var index = 0, blockIndex = 0
        while index < runs.count {
            if Task.isCancelled { return [] }
            let opening = runs[index].range
            while blockIndex < blocks.count, NSMaxRange(blocks[blockIndex]) <= opening.location { blockIndex += 1 }
            if blockIndex < blocks.count, NSIntersectionRange(blocks[blockIndex], opening).length > 0 { index += 1; continue }
            var end = index + 1
            while end < runs.count, runs[end].range.length != opening.length { end += 1 }
            if end < runs.count, blockIndex == blocks.count || blocks[blockIndex].location >= NSMaxRange(runs[end].range) {
                ranges.append(NSRange(location: opening.location, length: NSMaxRange(runs[end].range) - opening.location)); index = end + 1
            } else { index += 1 }
        }
        return ranges.sorted { $0.location < $1.location }
    }
}

public struct NoteAttachmentRepository: Sendable {
    public let workspace: URL
    public init(workspace: URL) { self.workspace = workspace }
    private var directory: URL { workspace.appendingPathComponent("attachments", isDirectory: true) }
    private func checkedDirectory(create: Bool) throws -> URL {
        let fm = FileManager.default
        if create { try fm.createDirectory(at: workspace, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700]) }
        let root = try workspace.resourceValues(forKeys: [.isDirectoryKey, .isSymbolicLinkKey])
        guard root.isDirectory == true, root.isSymbolicLink != true else { throw ToolError("附件工作区目录无效。") }
        if create && !fm.fileExists(atPath: directory.path) { try fm.createDirectory(at: directory, withIntermediateDirectories: false, attributes: [.posixPermissions: 0o700]) }
        let values = try directory.resourceValues(forKeys: [.isDirectoryKey, .isSymbolicLinkKey])
        guard values.isDirectory == true, values.isSymbolicLink != true,
              directory.resolvingSymlinksInPath().deletingLastPathComponent().standardizedFileURL.path == workspace.resolvingSymlinksInPath().standardizedFileURL.path else { throw ToolError("附件目录不在原生版工作区内。") }
        return directory
    }
    public func write(_ payload: NoteImagePayload) throws {
        try payload.attachment.validate()
        let target = try checkedDirectory(create: true).appendingPathComponent((payload.attachment.path as NSString).lastPathComponent)
        if FileManager.default.fileExists(atPath: target.path) { _ = try read(payload.attachment); return }
        try payload.data.write(to: target, options: .atomic)
        try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: target.path)
    }
    public func read(_ attachment: NoteAttachment) throws -> Data {
        try attachment.validate()
        let file = try checkedDirectory(create: false).appendingPathComponent((attachment.path as NSString).lastPathComponent)
        let data = try NoteImagePayload.readBounded(file)
        guard data.count == attachment.bytes, SHA256.hash(data: data).map({ String(format: "%02x", $0) }).joined() == attachment.sha256 else { throw ToolError("图片附件已损坏：\(attachment.path)") }
        return data
    }
    public func thumbnail(_ attachment: NoteAttachment, maximumPixels: Int = 2048) throws -> CGImage {
        let data = try read(attachment)
        guard let source = CGImageSourceCreateWithData(data as CFData, [kCGImageSourceShouldCache: false] as CFDictionary),
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceThumbnailMaxPixelSize: maximumPixels, kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceShouldCache: false] as CFDictionary) else { throw ToolError("图片附件无法解码。") }
        return image
    }
    public static func validateManifest(_ attachments: [NoteAttachment]) throws {
        guard attachments.count <= 1024, Set(attachments.map(\.path)).count == attachments.count else { throw ToolError("附件重复或超过 1024 张。") }
        var total = 0
        for attachment in attachments { try attachment.validate(); total += attachment.bytes }
        guard total <= 64 * 1024 * 1024 else { throw ToolError("原生工作区附件总量超过 64 MB。") }
    }
    /// Create a new folder; never overwrite the user's existing export or source images.
    public func exportDocument(name: String, content: String, attachments: [NoteAttachment], to parent: URL) throws -> URL {
        let paths = MarkdownImageReference.managedPaths(in: content)
        let manifest = attachments.filter { paths.contains($0.path) }
        guard Set(manifest.map(\.path)) == paths else { throw ToolError("文档引用了未登记或丢失的附件，无法完整导出。") }
        let files = try manifest.map { ($0, try read($0)) }
        let fm = FileManager.default, stage = parent.appendingPathComponent(".mootool-export-" + UUID().uuidString, isDirectory: true)
        try fm.createDirectory(at: stage, withIntermediateDirectories: false, attributes: [.posixPermissions: 0o700])
        defer { try? fm.removeItem(at: stage) }
        var base = (name as NSString).deletingPathExtension.replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: ":", with: "_")
        if base.isEmpty || base == "." || base == ".." { base = "笔记" }; base = String(base.prefix(100))
        try Data(content.utf8).write(to: stage.appendingPathComponent(base + ".md"), options: .atomic)
        if !files.isEmpty { try fm.createDirectory(at: stage.appendingPathComponent("attachments"), withIntermediateDirectories: false, attributes: [.posixPermissions: 0o700]) }
        for (attachment, data) in files { try data.write(to: stage.appendingPathComponent(attachment.path), options: .atomic) }
        var target = parent.appendingPathComponent(base, isDirectory: true), suffix = 2
        while fm.fileExists(atPath: target.path) { target = parent.appendingPathComponent("\(base) \(suffix)", isDirectory: true); suffix += 1 }
        try fm.moveItem(at: stage, to: target); return target
    }
}

extension WorkspaceRepository {
    public var attachmentRepository: NoteAttachmentRepository { NoteAttachmentRepository(workspace: directory) }
    public func backup(_ snapshot: WorkspaceSnapshot) throws -> Data {
        var value = snapshot
        let attachments = value.noteAttachments ?? []
        if !attachments.isEmpty { value.attachmentData = try Dictionary(uniqueKeysWithValues: attachments.map { ($0.path, try attachmentRepository.read($0)) }) }
        return try Self.encode(value.validated())
    }
    public func installBackup(_ backup: WorkspaceSnapshot) throws -> WorkspaceSnapshot {
        var value = try backup.validated()
        let attachments = value.noteAttachments ?? []
        guard attachments.isEmpty || value.attachmentData != nil else { throw ToolError("备份缺少图片内容，请使用原生版“导出备份”生成完整备份。") }
        // Validate and decode every image before writing anything to this workspace.
        let images = try attachments.map { record -> NoteImagePayload in
            guard let data = value.attachmentData?[record.path] else { throw ToolError("备份缺少图片：\(record.path)") }
            let payload = try NoteImagePayload(data: data)
            guard payload.attachment == record else { throw ToolError("备份图片与记录不一致。") }; return payload
        }
        value.attachmentData = nil; _ = try Self.encode(value)
        for image in images { try attachmentRepository.write(image) }
        try save(value); return value
    }
}
