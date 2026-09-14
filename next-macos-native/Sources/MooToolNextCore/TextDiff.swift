import Foundation

public enum TextDiffDisplay: String, Codable, Sendable { case side, unified }
public enum TextDiffHighlight: String, Codable, Sendable { case both, characters, lines }
public struct TextDiffOptions: Codable, Equatable, Sendable {
    public var display: TextDiffDisplay = .side
    public var highlight: TextDiffHighlight = .both
    public var ignoreWhitespace = false
    public var leftEditor = EditorViewState()
    public var rightEditor = EditorViewState()
    public var unifiedEditor = EditorViewState()
    public init() {}
}
public enum TextDiffKind: String, Equatable, Sendable { case insert, delete, change }
public struct TextDiffSegment: Equatable, Sendable {
    public var kind: TextDiffKind
    public var left: NSRange
    public var right: NSRange
    public var wholeLine: Bool
    public init(_ kind: TextDiffKind, left: NSRange, right: NSRange, wholeLine: Bool) {
        self.kind = kind; self.left = left; self.right = right; self.wholeLine = wholeLine
    }
}
public enum TextDiffSpanKind: String, Sendable { case added, removed, changed, header, hunk }
public struct TextDiffSpan: Sendable {
    public var range: NSRange
    public var kind: TextDiffSpanKind
    public init(_ range: NSRange, _ kind: TextDiffSpanKind) { self.range = range; self.kind = kind }
}
public struct TextDiffResult: Sendable {
    public var left: String
    public var right: String
    public var segments: [TextDiffSegment]
    public var unified: String
    public var unifiedLines: [TextDiffSpan]
    public var unifiedCharacters: [TextDiffSpan]
    public var added: Int
    public var removed: Int
    public var changed: Int
    public static let empty = Self(left: "", right: "", segments: [], unified: "", unifiedLines: [], unifiedCharacters: [], added: 0, removed: 0, changed: 0)
}

/// Independent Swift implementation of the Electron diff page's line patch and UTF-16 character ranges.
public enum TextDiffEngine {
    private static let maximumCharacterPair = 4_000
    private struct Delta<T> {
        var sourcePosition: Int
        var targetPosition: Int
        var source: [T]
        var target: [T]
        var kind: TextDiffKind { source.isEmpty ? .insert : target.isEmpty ? .delete : .change }
    }
    public static func compare(_ left: String, _ right: String, ignoreWhitespace: Bool = false) throws -> TextDiffResult {
        guard left.utf16.count + right.utf16.count <= 500_000 else { throw ToolError("对比文本总长度请控制在 50 万字符以内。") }
        let leftLines = splitLines(left), rightLines = splitLines(right)
        guard leftLines.count * rightLines.count <= 10_000_000 else { throw ToolError("对比行数过多，请缩小输入。") }
        let deltas = patch(leftLines, rightLines)
        let leftStarts = lineStarts(left), rightStarts = lineStarts(right)
        var segments: [TextDiffSegment] = []
        var added = 0, removed = 0, changed = 0
        for delta in deltas {
            if delta.kind == .insert { added += delta.target.count }
            else if delta.kind == .delete { removed += delta.source.count }
            else {
                let paired = min(delta.source.count, delta.target.count)
                changed += paired; removed += delta.source.count - paired; added += delta.target.count - paired
            }
            if delta.kind == .delete {
                for (index, line) in delta.source.enumerated() where !ignoreWhitespace || !isWhitespace(line) {
                    segments.append(TextDiffSegment(.delete, left: NSRange(location: start(leftStarts, delta.sourcePosition + index), length: line.utf16.count), right: invalidRange, wholeLine: true))
                }
            } else if delta.kind == .insert {
                for (index, line) in delta.target.enumerated() where !ignoreWhitespace || !isWhitespace(line) {
                    segments.append(TextDiffSegment(.insert, left: invalidRange, right: NSRange(location: start(rightStarts, delta.targetPosition + index), length: line.utf16.count), wholeLine: true))
                }
            } else {
                let paired = min(delta.source.count, delta.target.count)
                for index in 0..<paired {
                    let before = delta.source[index], after = delta.target[index]
                    let beforeStart = start(leftStarts, delta.sourcePosition + index), afterStart = start(rightStarts, delta.targetPosition + index)
                    if before.utf16.count + after.utf16.count > maximumCharacterPair {
                        segments.append(TextDiffSegment(.change,
                                                        left: NSRange(location: beforeStart, length: before.utf16.count),
                                                        right: NSRange(location: afterStart, length: after.utf16.count), wholeLine: true))
                        continue
                    }
                    for change in patch(Array(before.utf16), Array(after.utf16)) {
                        let beforeValue = String(decoding: change.source, as: UTF16.self), afterValue = String(decoding: change.target, as: UTF16.self)
                        if ignoreWhitespace && (change.kind == .delete && isWhitespace(beforeValue) || change.kind == .insert && isWhitespace(afterValue) || change.kind == .change && equalIgnoringWhitespace(beforeValue, afterValue)) { continue }
                        let leftRange = change.source.isEmpty ? invalidRange : NSRange(location: beforeStart + change.sourcePosition, length: change.source.count)
                        let rightRange = change.target.isEmpty ? invalidRange : NSRange(location: afterStart + change.targetPosition, length: change.target.count)
                        segments.append(TextDiffSegment(change.kind, left: leftRange, right: rightRange, wholeLine: false))
                    }
                }
                if delta.source.count > paired {
                    for index in paired..<delta.source.count where !ignoreWhitespace || !isWhitespace(delta.source[index]) {
                        segments.append(TextDiffSegment(.delete, left: NSRange(location: start(leftStarts, delta.sourcePosition + index), length: delta.source[index].utf16.count), right: invalidRange, wholeLine: true))
                    }
                }
                if delta.target.count > paired {
                    for index in paired..<delta.target.count where !ignoreWhitespace || !isWhitespace(delta.target[index]) {
                        segments.append(TextDiffSegment(.insert, left: invalidRange, right: NSRange(location: start(rightStarts, delta.targetPosition + index), length: delta.target[index].utf16.count), wholeLine: true))
                    }
                }
            }
        }
        let unifiedLines = unified(leftLines, deltas)
        let text = unifiedLines.joined(separator: "\n")
        var lineSpans: [TextDiffSpan] = [], characterSpans: [TextDiffSpan] = []
        var offset = 0, deletes: [(Int, String)] = [], inserts: [(Int, String)] = []
        func flush() {
            for index in 0..<min(deletes.count, inserts.count) {
                let before = deletes[index], after = inserts[index]
                if before.1.utf16.count + after.1.utf16.count > maximumCharacterPair { continue }
                for change in patch(Array(before.1.utf16), Array(after.1.utf16)) {
                    let old = String(decoding: change.source, as: UTF16.self), new = String(decoding: change.target, as: UTF16.self)
                    if ignoreWhitespace && (change.kind == .delete && isWhitespace(old) || change.kind == .insert && isWhitespace(new) || change.kind == .change && equalIgnoringWhitespace(old, new)) { continue }
                    if !change.source.isEmpty { characterSpans.append(TextDiffSpan(NSRange(location: before.0 + 1 + change.sourcePosition, length: change.source.count), change.kind == .change ? .changed : .removed)) }
                    if !change.target.isEmpty { characterSpans.append(TextDiffSpan(NSRange(location: after.0 + 1 + change.targetPosition, length: change.target.count), change.kind == .change ? .changed : .added)) }
                }
            }
            deletes.removeAll(); inserts.removeAll()
        }
        for (index, line) in unifiedLines.enumerated() {
            let length = line.utf16.count, range = NSRange(location: offset, length: length)
            if line.hasPrefix("@@") { flush(); lineSpans.append(TextDiffSpan(range, .hunk)) }
            else if line.hasPrefix("---") || line.hasPrefix("+++") { lineSpans.append(TextDiffSpan(range, .header)) }
            else if line.hasPrefix("-") {
                let value = String(line.dropFirst()); if !ignoreWhitespace || !isWhitespace(value) { lineSpans.append(TextDiffSpan(range, .removed)) }
                deletes.append((offset, value))
            } else if line.hasPrefix("+") {
                let value = String(line.dropFirst()); if !ignoreWhitespace || !isWhitespace(value) { lineSpans.append(TextDiffSpan(range, .added)) }
                inserts.append((offset, value))
            }
            offset += length + (index == unifiedLines.count - 1 ? 0 : 1)
        }
        flush()
        return TextDiffResult(left: left, right: right, segments: segments, unified: text, unifiedLines: lineSpans, unifiedCharacters: characterSpans, added: added, removed: removed, changed: changed)
    }
    private static let invalidRange = NSRange(location: NSNotFound, length: 0)
    private static func patch<T: Equatable>(_ source: [T], _ target: [T]) -> [Delta<T>] {
        let difference = target.difference(from: source)
        func offset(_ change: CollectionDifference<T>.Change) -> Int {
            switch change { case .remove(let value, _, _), .insert(let value, _, _): return value }
        }
        let removed = Set(difference.removals.map(offset)), inserted = Set(difference.insertions.map(offset))
        var result: [Delta<T>] = [], i = 0, j = 0
        while i < source.count || j < target.count {
            if removed.contains(i) || inserted.contains(j) {
                let startI = i, startJ = j; var old: [T] = [], new: [T] = []
                while removed.contains(i) || inserted.contains(j) {
                    if removed.contains(i) { old.append(source[i]); i += 1 }
                    if inserted.contains(j) { new.append(target[j]); j += 1 }
                }
                result.append(Delta(sourcePosition: startI, targetPosition: startJ, source: old, target: new))
            } else { i += 1; j += 1 }
        }
        return result
    }
    private static func splitLines(_ text: String) -> [String] {
        let source = text as NSString
        var lines: [String] = [], location = 0, index = 0
        while index < source.length {
            let code = source.character(at: index)
            if [10, 11, 12, 13, 133, 8232, 8233].contains(code) {
                lines.append(source.substring(with: NSRange(location: location, length: index - location)))
                index += code == 13 && index + 1 < source.length && source.character(at: index + 1) == 10 ? 2 : 1
                location = index
            } else { index += 1 }
        }
        lines.append(source.substring(from: location)); return lines
    }
    private static func lineStarts(_ text: String) -> [Int] {
        var result = [0]
        for (index, code) in text.utf16.enumerated() where code == 10 { result.append(index + 1) }
        return result
    }
    private static func start(_ starts: [Int], _ index: Int) -> Int { starts[min(max(index, 0), starts.count - 1)] }
    private static func isWhitespace(_ text: String) -> Bool { text.utf16.allSatisfy(isJavaWhitespace) }
    private static func equalIgnoringWhitespace(_ a: String, _ b: String) -> Bool { a.utf16.filter { !isJavaWhitespace($0) } == b.utf16.filter { !isJavaWhitespace($0) } }
    private static func isJavaWhitespace(_ code: UInt16) -> Bool {
        (9...13).contains(code) || (28...32).contains(code) || code == 0x1680 || (0x2000...0x2006).contains(code) || (0x2008...0x200A).contains(code) || code == 0x2028 || code == 0x2029 || code == 0x205F || code == 0x3000
    }
    private static func unified(_ source: [String], _ deltas: [Delta<String>]) -> [String] {
        guard !deltas.isEmpty else { return [] }
        var lines = ["--- old", "+++ new"], group = [deltas[0]], previous = deltas[0]
        for next in deltas.dropFirst() {
            if previous.sourcePosition + previous.source.count + 3 >= next.sourcePosition - 3 { group.append(next) }
            else { lines += unifiedGroup(source, group); group = [next] }
            previous = next
        }
        lines += unifiedGroup(source, group); return lines
    }
    private static func unifiedGroup(_ source: [String], _ deltas: [Delta<String>]) -> [String] {
        let first = deltas[0]
        let oldStart = max(1, first.sourcePosition + 1 - 3), newStart = max(1, first.targetPosition + 1 - 3)
        var lines: [String] = [], oldTotal = 0, newTotal = 0
        for index in max(0, first.sourcePosition - 3)..<first.sourcePosition { lines.append(" " + source[index]); oldTotal += 1; newTotal += 1 }
        var previous = first
        for (offset, delta) in deltas.enumerated() {
            if offset > 0 {
                let start = previous.sourcePosition + previous.source.count
                for index in start..<delta.sourcePosition { lines.append(" " + source[index]); oldTotal += 1; newTotal += 1 }
            }
            lines += delta.source.map { "-" + $0 }; lines += delta.target.map { "+" + $0 }
            oldTotal += delta.source.count; newTotal += delta.target.count; previous = delta
        }
        let tail = previous.sourcePosition + previous.source.count
        for index in tail..<min(source.count, tail + 3) { lines.append(" " + source[index]); oldTotal += 1; newTotal += 1 }
        lines.insert("@@ -\(oldStart),\(oldTotal) +\(newStart),\(newTotal) @@", at: 0)
        return lines
    }
}
