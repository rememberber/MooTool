import MooToolNextCore

enum HTTPRequestTab: String, CaseIterable, Identifiable {
    case params, headers, cookies, body
    var id: String { rawValue }
    func title(language: AppLanguage) -> String {
        switch self {
        case .params: return AppLocalization.string("http.tab.params", language: language)
        case .headers: return AppLocalization.string("http.tab.headers", language: language)
        case .cookies: return AppLocalization.string("http.tab.cookies", language: language)
        case .body: return AppLocalization.string("http.tab.body", language: language)
        }
    }
}

enum HTTPResponseTab: String, CaseIterable, Identifiable {
    case body, headers, cookies
    var id: String { rawValue }
    func title(language: AppLanguage) -> String {
        switch self {
        case .body: return AppLocalization.string("http.tab.body", language: language)
        case .headers: return AppLocalization.string("http.tab.headers", language: language)
        case .cookies: return AppLocalization.string("http.tab.cookies", language: language)
        }
    }
}
