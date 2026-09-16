import Foundation

public struct MediaWorkspaceState: Codable, Equatable {
    public var filePaths: [String] = []
    public var exportWidth: String = ""
    public var jpegQuality: Double = 0.85
    public var watermark: String = ""
    public var pdfShowText = false

    public init() {}

    public func validate(language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        func err(_ key: String) -> ToolError { ToolError(AppLocalization.string(key, language: language)) }
        guard filePaths.count <= 32 else { throw err("media.error.tooManyFiles") }
        for path in filePaths {
            guard !path.isEmpty, path.utf8.count <= 4096 else { throw err("media.error.invalidPath") }
        }
        guard jpegQuality.isFinite, (0.05...1).contains(jpegQuality) else { throw err("media.error.jpegQuality") }
        guard exportWidth.utf8.count <= 16, watermark.utf8.count <= 1024 else { throw err("media.error.exportSettings") }
    }
}
