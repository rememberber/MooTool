import Foundation

public struct MediaWorkspaceState: Codable, Equatable {
    public var filePaths: [String] = []
    public var exportWidth: String = ""
    public var jpegQuality: Double = 0.85
    public var watermark: String = ""
    public var pdfShowText = false

    public init() {}

    public func validate() throws {
        guard filePaths.count <= 32 else { throw ToolError("媒体文件列表过长。") }
        for path in filePaths {
            guard !path.isEmpty, path.utf8.count <= 4096 else { throw ToolError("媒体文件路径无效。") }
        }
        guard jpegQuality.isFinite, (0.05...1).contains(jpegQuality) else { throw ToolError("JPEG 质量无效。") }
        guard exportWidth.utf8.count <= 16, watermark.utf8.count <= 1024 else { throw ToolError("图片导出设置无效。") }
    }
}
