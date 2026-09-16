import Foundation

enum MigrationImportErrors {
    static func electronMissingSettings(_ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string("migration.error.electronMissingSettings", language: language))
    }

    static func electronParseJson(_ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string("migration.error.electronParseJson", language: language))
    }

    static func sqliteNotFound(_ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string("migration.error.sqliteNotFound", language: language))
    }

    static func sqliteOpen(_ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string("migration.error.sqliteOpen", language: language))
    }

    static func warning(_ key: String, language: AppLanguage) -> String {
        AppLocalization.string(key, language: language)
    }
}
