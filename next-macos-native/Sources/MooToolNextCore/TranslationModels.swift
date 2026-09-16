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

    public func validate(language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        func err(_ key: String) -> ToolError { ToolError(AppLocalization.string(key, language: language)) }
        guard !sourceText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw err("translationWord.error.emptySource") }
        guard sourceText.utf8.count <= 16_384, targetText.utf8.count <= 16_384, remark.utf8.count <= 1024 else { throw err("translationWord.error.fieldLimit") }
        guard sourceLang.utf8.count <= 32, targetLang.utf8.count <= 32 else { throw err("translationWord.error.langCode") }
    }
}
