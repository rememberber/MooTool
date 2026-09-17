import Foundation

public enum HTTPMultipartBuilder {
    private static let maximumPartBytes = 10 * 1024 * 1024
    private static let maximumTotalBytes = 10 * 1024 * 1024

    public static func active(_ parts: [HTTPMultipartPart]) -> [HTTPMultipartPart] {
        parts.filter { $0.enabled && !$0.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    public static func encode(_ parts: [HTTPMultipartPart], language: AppLanguage = AppLocalization.preferredLanguage()) throws -> (Data, String) {
        let items = active(parts)
        guard !items.isEmpty else { throw err("http.multipart.needField", language) }
        let boundary = "MooToolNative-" + UUID().uuidString.replacingOccurrences(of: "-", with: "")
        var body = Data()
        for part in items {
            let name = try validName(part.name, language: language)
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            if part.isFile {
                let path = part.filePath.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !path.isEmpty else { throw err("http.multipart.selectFile", language) }
                let url = URL(fileURLWithPath: path)
                guard url.isFileURL, FileManager.default.fileExists(atPath: url.path) else {
                    throw ToolError(String(format: AppLocalization.string("http.multipart.fileMissing", language: language), url.lastPathComponent))
                }
                let fileData = try Data(contentsOf: url, options: [.mappedIfSafe])
                guard !fileData.isEmpty, fileData.count <= maximumPartBytes else { throw err("http.multipart.fileTooLarge", language) }
                let filename = url.lastPathComponent
                body.append("Content-Disposition: form-data; name=\"\(name)\"; filename=\"\(escapeQuoted(filename))\"\r\n".data(using: .utf8)!)
                body.append("Content-Type: application/octet-stream\r\n\r\n".data(using: .utf8)!)
                body.append(fileData)
            } else {
                body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".data(using: .utf8)!)
                body.append(Data(part.value.utf8))
            }
            body.append("\r\n".data(using: .utf8)!)
            guard body.count <= maximumTotalBytes else { throw err("http.multipart.bodyTooLarge", language) }
        }
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        return (body, "multipart/form-data; boundary=\(boundary)")
    }

    private static func validName(_ value: String, language: AppLanguage) throws -> String {
        let name = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty, name.count <= 256, !name.contains("\""), !name.contains("\r"), !name.contains("\n") else {
            throw err("http.multipart.invalidFieldName", language)
        }
        return name
    }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }

    private static func escapeQuoted(_ value: String) -> String {
        value.replacingOccurrences(of: "\"", with: "_").replacingOccurrences(of: "\r", with: "_").replacingOccurrences(of: "\n", with: "_")
    }
}
