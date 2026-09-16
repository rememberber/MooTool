import SwiftUI
import MooToolNextCore

private struct AppLanguageKey: EnvironmentKey {
    static let defaultValue = AppLanguage.zhCN
}

extension EnvironmentValues {
    var appLanguage: AppLanguage {
        get { self[AppLanguageKey.self] }
        set { self[AppLanguageKey.self] = newValue }
    }
}
