import Foundation
import JavaScriptCore

public enum ReformatType: String, Codable, CaseIterable, Sendable {
    case nginx, java, xml, html, json
    public var title: String { self == .nginx ? "Nginx" : rawValue.uppercased() }
    public var fileExtension: String { self == .nginx ? "conf" : rawValue }
    public var sample: String {
        switch self {
        case .nginx: return "server { listen 80; location / { proxy_pass http://127.0.0.1:3000; } }"
        case .java: return "class Demo{public static void main(String[] args){System.out.println(\"MooTool\");}}"
        case .xml: return "<root><tool id=\"mootool\"><name>MooTool</name></tool></root>"
        case .html: return "<main><h1>MooTool</h1><p>Desktop toolbox</p></main>"
        case .json: return "{\"name\":\"MooTool\",\"native\":true}"
        }
    }
    public var editorSyntax: NoteSyntax { switch self { case .java: return .java; case .xml, .html: return .xml; case .json: return .json; case .nginx: return .plain } }
}
public enum ReformatTab: String, Codable, Sendable { case text, file }
public struct ReformatOptions: Codable, Equatable, Sendable {
    public var type: ReformatType = .nginx
    public var tab: ReformatTab = .text
    public var indent = 4
    public var fileName = ""
    public var fileSource = ""
    public var fileResult = ""
    public var fileIdentity = UUID()
    public var fileEditor = EditorViewState()
    public var resultEditor = EditorViewState()
    public init() {}
    public static func migrating(_ record: DraftRecord) -> Self {
        if let value = record.reformat { return value }
        var value = Self()
        if record.mode == "JSON" { value.type = .json }
        else if record.mode == "XML / XHTML" { value.type = .xml }
        if !record.output.isEmpty { value.fileSource = record.input; value.fileResult = record.output }
        return value
    }
    public var exportName: String {
        let name = (fileName as NSString).lastPathComponent
        let base = (name as NSString).deletingPathExtension
        return (base.isEmpty || base == "." || base == ".." ? "formatted" : String(base.prefix(100))) + "." + type.fileExtension
    }
    public func validate(language: AppLanguage = AppLocalization.preferredLanguage()) throws {
        guard (2...6).contains(indent), fileName.utf8.count <= 1024,
              fileSource.utf8.count <= 10 * 1024 * 1024, fileResult.utf8.count <= 16 * 1024 * 1024 else {
            throw ToolError(AppLocalization.string("reformat.error.workspaceInvalid", language: language))
        }
    }
    public static func restoringHistory(_ record: DraftRecord) -> DraftRecord {
        var value = record; var options = migrating(record)
        if options.tab == .text, !record.output.isEmpty { value.input = record.output }
        if options.tab == .file, !record.output.isEmpty { options.fileResult = record.output }
        value.output = ""; value.reformat = options; return value
    }
}

public enum ReformatEngine {
    public static let maximumInputBytes = 2 * 1024 * 1024
    public static func format(_ input: String, type: ReformatType, indent: Int = 4, language: AppLanguage = AppLocalization.preferredLanguage()) async throws -> String {
        guard input.utf8.count <= maximumInputBytes else { throw ToolError(AppLocalization.string("reformat.error.inputTooLarge", language: language)) }
        var request = JSONEngineRequest("reformat", input: input)
        request.path = type.rawValue; request.indent = indent
        request.language = language.rawValue
        return try await JSONEngine.execute(request).value ?? ""
    }
    public static func readFile(_ file: URL, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let values = try file.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey])
        guard values.isRegularFile == true, (values.fileSize ?? Int.max) <= maximumInputBytes else {
            throw ToolError(AppLocalization.string("reformat.error.filePickTooLarge", language: language))
        }
        let handle = try FileHandle(forReadingFrom: file); defer { try? handle.close() }
        let data = try handle.read(upToCount: maximumInputBytes + 1) ?? Data()
        guard data.count <= maximumInputBytes, let value = String(data: data, encoding: .utf8) else {
            throw ToolError(AppLocalization.string("reformat.error.fileNotUtf8", language: language))
        }
        return value
    }
    /// Called only by the bounded helper and deterministic tests; no file/network APIs are exposed to JS.
    static func evaluate(_ request: JSONEngineRequest) throws -> JSONEngineReply {
        let language = AppLanguage(rawValue: request.language) ?? AppLocalization.preferredLanguage()
        func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
        guard request.input.utf8.count <= maximumInputBytes, (2...6).contains(request.indent), ReformatType(rawValue: request.path) != nil else {
            throw ToolError(loc("reformat.error.invalidRequest"))
        }
        guard let context = JSContext(), let url = JSONEngine.resources.url(forResource: "ReformatTools", withExtension: "js") else {
            throw ToolError(loc("reformat.error.missingParser"))
        }
        context.evaluateScript(bootstrap)
        context.evaluateScript(try String(contentsOf: url, encoding: .utf8))
        if let exception = context.exception { throw ToolError(exception.toString() ?? loc("reformat.error.parserInit")) }
        let payload = String(decoding: try JSONEncoder().encode(request), as: UTF8.self)
        context.objectForKeyedSubscript("startNativeReformat")?.call(withArguments: [payload])
        let deadline = Date().addingTimeInterval(2.8)
        while context.objectForKeyedSubscript("nativeReformatReply")?.isUndefined != false {
            if let exception = context.exception { throw ToolError(exception.toString() ?? loc("reformat.error.failed")) }
            guard Date() < deadline, !Task.isCancelled else { throw ToolError(loc("reformat.error.timeout")) }
            RunLoop.current.run(until: Date().addingTimeInterval(0.005))
            context.evaluateScript("void 0")
        }
        guard let output = context.objectForKeyedSubscript("nativeReformatReply")?.toString(), output.utf8.count <= 16 * 1024 * 1024 else {
            throw ToolError(loc("reformat.error.outputTooLarge"))
        }
        let reply = try JSONDecoder().decode(JSONEngineReply.self, from: Data(output.utf8))
        if let error = reply.error { throw ToolError(error) }; return reply
    }
    private static let bootstrap = #"""
    globalThis.console = { log(){}, warn(){}, error(){} };
    globalThis.performance = { now: () => Date.now() };
    globalThis.atob = function(input) {
      const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
      let bits = 0, value = 0; const output = [];
      for (const char of input) {
        if (char === '=') break;
        const digit = alphabet.indexOf(char); if (digit < 0) continue;
        value = (value << 6) | digit; bits += 6;
        if (bits >= 8) { bits -= 8; output.push(String.fromCharCode((value >> bits) & 255)); }
      }
      return output.join('');
    };
    """#
}
