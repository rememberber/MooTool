import Foundation

public enum HTTPRequestBuilderError: Error, Equatable, Sendable {
    case invalidURL
    case invalidHeader
}

public enum HTTPRequestBuilder {
    public static func build(method: String, urlText: String, headersText: String, bodyText: String) throws -> URLRequest {
        guard let url = URL(string: urlText.trimmingCharacters(in: .whitespacesAndNewlines)),
              let scheme = url.scheme,
              ["http", "https"].contains(scheme.lowercased()),
              url.host != nil else {
            throw HTTPRequestBuilderError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method.uppercased()

        for header in try parseHeaders(headersText) {
            request.setValue(header.value, forHTTPHeaderField: header.name)
        }

        if !bodyText.isEmpty {
            request.httpBody = Data(bodyText.utf8)
        }

        return request
    }

    public static func parseHeaders(_ headersText: String) throws -> [(name: String, value: String)] {
        try headersText
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .map { line in
                guard let separatorIndex = line.firstIndex(of: ":") else {
                    throw HTTPRequestBuilderError.invalidHeader
                }

                let name = String(line[..<separatorIndex]).trimmingCharacters(in: .whitespacesAndNewlines)
                let value = String(line[line.index(after: separatorIndex)...]).trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else {
                    throw HTTPRequestBuilderError.invalidHeader
                }
                return (name: name, value: value)
            }
    }
}
