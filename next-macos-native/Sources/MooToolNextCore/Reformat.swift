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
    public func validate() throws {
        guard (2...6).contains(indent), fileName.utf8.count <= 1024,
              fileSource.utf8.count <= 10 * 1024 * 1024, fileResult.utf8.count <= 16 * 1024 * 1024 else { throw ToolError("格式化工作区设置无效或内容超过大小限制。") }
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
    public static func format(_ input: String, type: ReformatType, indent: Int = 4) async throws -> String {
        guard input.utf8.count <= maximumInputBytes else { throw ToolError("格式化输入超过 2 MB，正文保持原样。") }
        var request = JSONEngineRequest("reformat", input: input); request.path = type.rawValue; request.indent = indent
        return try await JSONEngine.execute(request).value ?? ""
    }
    public static func readFile(_ file: URL) throws -> String {
        let values = try file.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey])
        guard values.isRegularFile == true, (values.fileSize ?? Int.max) <= maximumInputBytes else { throw ToolError("请选择 2 MB 以内的 UTF-8 文本文件。") }
        let handle = try FileHandle(forReadingFrom: file); defer { try? handle.close() }
        let data = try handle.read(upToCount: maximumInputBytes + 1) ?? Data()
        guard data.count <= maximumInputBytes, let value = String(data: data, encoding: .utf8) else { throw ToolError("文件不是 UTF-8 文本或超过 2 MB。") }
        return value
    }
    /// Called only by the bounded helper and deterministic tests; no file/network APIs are exposed to JS.
    static func evaluate(_ request: JSONEngineRequest) throws -> JSONEngineReply {
        guard request.input.utf8.count <= maximumInputBytes, (2...6).contains(request.indent), ReformatType(rawValue: request.path) != nil else { throw ToolError("格式化类型、缩进无效或输入超过 2 MB。") }
        guard let context = JSContext(), let url = JSONEngine.resources.url(forResource: "ReformatTools", withExtension: "js") else { throw ToolError("缺少原生格式化解析器，请重新构建或安装应用。") }
        context.evaluateScript(bootstrap)
        context.evaluateScript(try String(contentsOf: url, encoding: .utf8))
        if let exception = context.exception { throw ToolError(exception.toString() ?? "格式化解析器初始化失败。") }
        let payload = String(decoding: try JSONEncoder().encode(request), as: UTF8.self)
        context.objectForKeyedSubscript("startNativeReformat")?.call(withArguments: [payload])
        let deadline = Date().addingTimeInterval(2.8)
        while context.objectForKeyedSubscript("nativeReformatReply")?.isUndefined != false {
            if let exception = context.exception { throw ToolError(exception.toString() ?? "格式化失败。") }
            guard Date() < deadline, !Task.isCancelled else { throw ToolError("格式化超时，正文保持原样。") }
            RunLoop.current.run(until: Date().addingTimeInterval(0.005))
            context.evaluateScript("void 0")
        }
        guard let output = context.objectForKeyedSubscript("nativeReformatReply")?.toString(), output.utf8.count <= 16 * 1024 * 1024 else { throw ToolError("格式化结果超过 16 MB。") }
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
