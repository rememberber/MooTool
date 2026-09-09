import Foundation
import CoreFoundation

public struct JSONOptions: Codable, Equatable, Sendable {
    public var indent = 2
    public var sortKeys = false
    public var showsTree = false
    public var inspectorOpen: Bool?
    public var wrapLines: Bool?
    public var fontName = "Menlo"
    public var ignoreCase = false
    public var checkDuplicateKeys = true
    public var className = "Root"
    public var findOpen = false
    public var findQuery = ""
    public var replacement = ""
    public var matchCase = false
    public var wholeWord = false
    public var regex = false
    public init() {}
    private enum CodingKeys: String, CodingKey { case indent, sortKeys, showsTree, inspectorOpen, wrapLines, fontName, ignoreCase, checkDuplicateKeys, className, findOpen, findQuery, replacement, matchCase, wholeWord, regex }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        indent = try c.decodeIfPresent(Int.self, forKey: .indent) ?? 2
        sortKeys = try c.decodeIfPresent(Bool.self, forKey: .sortKeys) ?? false
        showsTree = try c.decodeIfPresent(Bool.self, forKey: .showsTree) ?? false
        inspectorOpen = try c.decodeIfPresent(Bool.self, forKey: .inspectorOpen)
        wrapLines = try c.decodeIfPresent(Bool.self, forKey: .wrapLines)
        fontName = try c.decodeIfPresent(String.self, forKey: .fontName) ?? "Menlo"
        ignoreCase = try c.decodeIfPresent(Bool.self, forKey: .ignoreCase) ?? false
        checkDuplicateKeys = try c.decodeIfPresent(Bool.self, forKey: .checkDuplicateKeys) ?? true
        className = try c.decodeIfPresent(String.self, forKey: .className) ?? "Root"
        findOpen = try c.decodeIfPresent(Bool.self, forKey: .findOpen) ?? false
        findQuery = try c.decodeIfPresent(String.self, forKey: .findQuery) ?? ""
        replacement = try c.decodeIfPresent(String.self, forKey: .replacement) ?? ""
        matchCase = try c.decodeIfPresent(Bool.self, forKey: .matchCase) ?? false
        wholeWord = try c.decodeIfPresent(Bool.self, forKey: .wholeWord) ?? false
        regex = try c.decodeIfPresent(Bool.self, forKey: .regex) ?? false
    }
}

public struct JSONTreeNode: Identifiable, Equatable, Sendable {
    public var id: String { pointer }
    public let pointer: String
    public let queryPath: String
    public let name: String
    public let kind: String
    public let summary: String
    public let children: [JSONTreeNode]?
}

public struct JSONStructure: Sendable {
    public let root: JSONTreeNode
    public let nodeCount: Int
    public let maxDepth: Int
    public init(_ text: String, nodeLimit: Int = 20_000, depthLimit: Int = 128) throws {
        guard text.utf8.count <= 10 * 1024 * 1024 else { throw ToolError("结构树最多读取 10 MB JSON。") }
        let object = try JSONSerialization.jsonObject(with: Data(text.utf8), options: [.fragmentsAllowed])
        var count = 0, deepest = 0
        func build(_ value: Any, name: String, pointer: String, queryPath: String, depth: Int) throws -> JSONTreeNode {
            count += 1; deepest = max(deepest, depth)
            guard count <= nodeLimit, depth <= depthLimit else { throw ToolError("JSON 结构超过 \(nodeLimit) 个节点或 \(depthLimit) 层，请先查询需要的部分。") }
            let kind: String, summary: String, children: [JSONTreeNode]?
            if let dictionary = value as? [String: Any] {
                kind = "Object"; summary = "\(dictionary.count) 个属性"
                children = dictionary.isEmpty ? nil : try dictionary.keys.sorted().map { key in
                    try build(dictionary[key]!, name: key, pointer: pointer + "/" + Self.escapePointer(key), queryPath: queryPath + Self.pathComponent(key), depth: depth + 1)
                }
            } else if let array = value as? [Any] {
                kind = "Array"; summary = "\(array.count) 个元素"
                children = array.isEmpty ? nil : try array.enumerated().map { try build($0.element, name: "[\($0.offset)]", pointer: pointer + "/\($0.offset)", queryPath: queryPath + "[\($0.offset)]", depth: depth + 1) }
            } else if let string = value as? String {
                kind = "String"; summary = try TextServices.serialize(String(string.prefix(150)), pretty: false) + (string.count > 150 ? "…" : "")
                children = nil
            } else {
                kind = value is NSNull ? "Null" : ((value as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } == true ? "Boolean" : "Number")
                summary = try TextServices.serialize(value, pretty: false); children = nil
            }
            return JSONTreeNode(pointer: pointer, queryPath: queryPath, name: name, kind: kind, summary: summary, children: children)
        }
        root = try build(object, name: "$", pointer: "", queryPath: "$", depth: 0)
        nodeCount = count; maxDepth = deepest
    }
    public static func escapePointer(_ key: String) -> String { key.replacingOccurrences(of: "~", with: "~0").replacingOccurrences(of: "/", with: "~1") }
    private static func pathComponent(_ key: String) throws -> String {
        if key.range(of: #"^[a-zA-Z_$][a-zA-Z0-9_$]*$"#, options: .regularExpression) != nil { return "." + key }
        let quoted = try JSONSerialization.data(withJSONObject: key, options: [.fragmentsAllowed, .withoutEscapingSlashes])
        return "[" + String(decoding: quoted, as: UTF8.self) + "]"
    }
}
