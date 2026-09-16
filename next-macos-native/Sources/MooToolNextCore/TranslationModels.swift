import Foundation

public struct SavedTranslationWord: Codable, Equatable, Identifiable {
    public var id = UUID()
    public var sourceText: String
    public var targetText: String
    public var sourceLang: String
    public var targetLang: String
    public var remark: String
    public var modified = Date()

    public init(sourceText: String, targetText: String, sourceLang: String = "auto", targetLang: String = "zh-Hans", remark: String = "") {
        self.sourceText = sourceText
        self.targetText = targetText
        self.sourceLang = sourceLang
        self.targetLang = targetLang
        self.remark = remark
    }

    public func validate() throws {
        guard !sourceText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw ToolError("词条原文不能为空。") }
        guard sourceText.utf8.count <= 16_384, targetText.utf8.count <= 16_384, remark.utf8.count <= 1024 else { throw ToolError("词条内容超过限制。") }
        guard sourceLang.utf8.count <= 32, targetLang.utf8.count <= 32 else { throw ToolError("语言代码无效。") }
    }
}
