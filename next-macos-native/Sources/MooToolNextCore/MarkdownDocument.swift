import Foundation

public struct MarkdownBlock: Identifiable, Equatable, Sendable {
    public let id: Int
    public var kind: String
    public var text = ""
    public var level = 0
    public var marker = ""
    public var checked: Bool?
    public var language = ""
    public var destination = ""
    public var cells: [[String]] = []
    public var alignments: [String] = []
}
public struct MarkdownDocument: Sendable {
    public let blocks: [MarkdownBlock]
    public init(_ source: String) throws {
        guard source.utf8.count <= 2 * 1024 * 1024 else { throw ToolError("Markdown 预览最多读取 2 MB；正文可继续编辑和导出。") }
        let lines = source.replacingOccurrences(of: "\r\n", with: "\n").replacingOccurrences(of: "\r", with: "\n").components(separatedBy: "\n")
        guard lines.count <= 20_000 else { throw ToolError("Markdown 预览最多读取 2 万行。") }
        var result: [MarkdownBlock] = [], paragraph: [String] = [], index = 0, cellCount = 0, imageCount = 0
        func append(_ kind: String, _ text: String = "", level: Int = 0, marker: String = "", checked: Bool? = nil, language: String = "", destination: String = "", cells: [[String]] = [], alignments: [String] = []) {
            result.append(MarkdownBlock(id: result.count, kind: kind, text: text, level: level, marker: marker, checked: checked, language: language, destination: destination, cells: cells, alignments: alignments))
        }
        func flush() throws {
            guard !paragraph.isEmpty else { return }
            let text = paragraph.joined(separator: "\n"), source = text as NSString
            var offset = 0
            for image in MarkdownImageReference.parse(text) {
                let before = source.substring(with: NSRange(location: offset, length: image.range.location - offset)).trimmingCharacters(in: .whitespacesAndNewlines)
                if !before.isEmpty { append("paragraph", before) }
                imageCount += 1
                guard imageCount <= 1024 else { throw ToolError("Markdown 图片最多预览 1024 处引用。") }
                append("image", image.alt, destination: image.path); offset = NSMaxRange(image.range)
            }
            let after = source.substring(from: offset).trimmingCharacters(in: .whitespacesAndNewlines)
            if !after.isEmpty { append("paragraph", after) }; paragraph = []
        }
        while index < lines.count {
            if index.isMultiple(of: 128) { try Task.checkCancellation() }
            let line = lines[index], trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty { try flush(); index += 1; continue }
            if let fence = Self.groups(#"^ {0,3}(`{3,}|~{3,})(.*)$"#, line) {
                try flush(); let token = fence[0], language = fence[1].trimmingCharacters(in: .whitespaces); var code: [String] = []; index += 1
                while index < lines.count {
                    if index.isMultiple(of: 128) { try Task.checkCancellation() }
                    let close = lines[index].trimmingCharacters(in: .whitespaces)
                    if close.count >= token.count, close.allSatisfy({ $0 == token.first! }) { index += 1; break }
                    code.append(lines[index]); index += 1
                }
                append("code", code.joined(separator: "\n"), language: language); continue
            }
            if index + 1 < lines.count, line.contains("|") {
                let header = Self.tableCells(line), divider = Self.tableCells(lines[index + 1])
                if !header.isEmpty, header.count == divider.count, divider.allSatisfy({ $0.range(of: #"^:?-{3,}:?$"#, options: .regularExpression) != nil }) {
                    try flush(); var rows = [header]; index += 2; cellCount += header.count
                    guard cellCount <= 10_000 else { throw ToolError("Markdown 表格最多预览 1 万个单元格。") }
                    while index < lines.count, !lines[index].trimmingCharacters(in: .whitespaces).isEmpty, lines[index].contains("|") {
                        if index.isMultiple(of: 128) { try Task.checkCancellation() }
                        let cells = Self.tableCells(lines[index]); rows.append(Array((cells + Array(repeating: "", count: header.count)).prefix(header.count))); cellCount += header.count; index += 1
                        guard cellCount <= 10_000 else { throw ToolError("Markdown 表格最多预览 1 万个单元格。") }
                    }
                    append("table", cells: rows, alignments: divider.map { $0.hasPrefix(":") && $0.hasSuffix(":") ? "center" : $0.hasSuffix(":") ? "right" : "left" }); continue
                }
            }
            if let heading = Self.groups(#"^ {0,3}(#{1,6})\s+(.*)$"#, line) { try flush(); append("heading", heading[1], level: heading[0].count); index += 1; continue }
            if index + 1 < lines.count, let next = Self.groups(#"^ {0,3}(=+|-+)\s*$"#, lines[index + 1]) { try flush(); append("heading", trimmed, level: next[0].first == "=" ? 1 : 2); index += 2; continue }
            if trimmed.range(of: #"^(?:\*\s*){3,}$|^(?:-\s*){3,}$|^(?:_\s*){3,}$"#, options: .regularExpression) != nil { try flush(); append("divider"); index += 1; continue }
            if let quote = Self.groups(#"^\s*>\s?(.*)$"#, line) { try flush(); append("quote", quote[0]); index += 1; continue }
            if let list = Self.groups(#"^(\s*)([-+*]|\d+[.)])\s+(.*)$"#, line) {
                try flush(); var value = list[2], checked: Bool?
                if let task = Self.groups(#"^\[([ xX])\]\s+(.*)$"#, value) { checked = task[0] != " "; value = task[1] }
                append("list", value, level: min(12, list[0].count / 2), marker: list[1].count == 1 ? "•" : list[1], checked: checked); index += 1; continue
            }
            paragraph.append(line); index += 1
        }
        try flush(); blocks = result
    }
    private static func groups(_ pattern: String, _ text: String) -> [String]? {
        guard let regex = try? NSRegularExpression(pattern: pattern), let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: (text as NSString).length)) else { return nil }
        return (1..<match.numberOfRanges).map { (text as NSString).substring(with: match.range(at: $0)) }
    }
    public static func tableCells(_ line: String) -> [String] {
        var value = line.trimmingCharacters(in: .whitespaces)
        if value.hasPrefix("|") { value.removeFirst() }
        if value.hasSuffix("|"), !value.hasSuffix("\\|") { value.removeLast() }
        var cells: [String] = [], current = "", escaped = false, codeTicks = 0
        let chars = Array(value); var index = 0
        while index < chars.count {
            let char = chars[index]
            if escaped { if char != "|" { current.append("\\") }; current.append(char); escaped = false; index += 1; continue }
            if char == "\\" { escaped = true; index += 1; continue }
            if char == "`" {
                var count = 1; while index + count < chars.count && chars[index + count] == "`" { count += 1 }
                if codeTicks == 0 { codeTicks = count } else if codeTicks == count { codeTicks = 0 }
                current += String(repeating: "`", count: count); index += count; continue
            }
            if char == "|", codeTicks == 0 { cells.append(current.trimmingCharacters(in: .whitespaces)); current = "" } else { current.append(char) }
            index += 1
        }
        if escaped { current.append("\\") }; cells.append(current.trimmingCharacters(in: .whitespaces)); return cells
    }
}
