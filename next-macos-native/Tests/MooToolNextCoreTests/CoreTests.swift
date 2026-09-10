#if !STANDALONE_CHECK
import XCTest
#endif
import Foundation
import CoreGraphics
import ImageIO
@testable import MooToolNextCore

final class CoreTests: XCTestCase {

    private func noteImageData() throws -> Data {
        let context = CGContext(data: nil, width: 16, height: 8, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.setFillColor(CGColor(red: 0.1, green: 0.5, blue: 0.8, alpha: 1)); context.fill(CGRect(x: 0, y: 0, width: 16, height: 8))
        let output = NSMutableData(), image = context.makeImage()!
        let destination = CGImageDestinationCreateWithData(output, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { throw ToolError("Fixture image failed") }; return output as Data
    }
    func testNoteAttachmentInsertionMatchesElectron() throws {
        let markdown = "![image](attachments/pixel.png)"
        let first = NoteImageInsertion(content: "beforeafter", selection: NSRange(location: 6, length: 0), markdown: markdown)
        XCTAssertEqual(first.text, "\n" + markdown + "\n"); XCTAssertEqual(first.caret, 6 + markdown.utf16.count + 2)
        XCTAssertEqual(NoteImageInsertion(content: "before\nafter", selection: NSRange(location: 7, length: 0), markdown: markdown).text, markdown + "\n")
        XCTAssertEqual(NoteImageInsertion(content: "", selection: NSRange(location: 20, length: 40), markdown: markdown).range, NSRange(location: 0, length: 0))
        let selection = NoteImageInsertion(content: "😀 selected end", selection: NSRange(location: 3, length: 8), markdown: markdown)
        XCTAssertEqual(selection.applying(to: "😀 selected end"), "😀 \n" + markdown + "\n end")
        let consecutive = NoteImageInsertion(content: first.applying(to: "beforeafter"), selection: NSRange(location: first.caret, length: 0), markdown: markdown)
        XCTAssertEqual(consecutive.applying(to: first.applying(to: "beforeafter")), "before\n" + markdown + "\n" + markdown + "\nafter")
    }
    func testNoteImageValidationAndIndependentFiles() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let repository = NoteAttachmentRepository(workspace: root)
        let data = try noteImageData(), payload = try NoteImagePayload(data: data)
        XCTAssertEqual(payload.attachment.width, 16); XCTAssertEqual(payload.attachment.height, 8)
        XCTAssertTrue(NoteAttachment.isManagedPath(payload.attachment.path))
        XCTAssertFalse(NoteAttachment.isManagedPath(payload.attachment.path + "\n"))
        XCTAssertFalse(NoteAttachment.isManagedPath("attachments/../../image.png"))
        XCTAssertThrowsError(try NoteImagePayload(data: Data("not an image".utf8)))
        XCTAssertThrowsError(try NoteImagePayload(data: Data(count: NoteImagePayload.maximumBytes + 1)))
        try repository.write(payload); try repository.write(payload)
        let argumentDirectory = NoteAttachmentRepository(workspace: URL(fileURLWithPath: root.path))
        XCTAssertEqual(try argumentDirectory.read(payload.attachment), data)
        XCTAssertEqual(try repository.read(payload.attachment), data)
        XCTAssertEqual(try repository.thumbnail(payload.attachment).width, 16)
        XCTAssertEqual(try FileManager.default.contentsOfDirectory(atPath: root.appendingPathComponent("attachments").path).count, 1)
        let file = root.appendingPathComponent(payload.attachment.path)
        XCTAssertEqual((try FileManager.default.attributesOfItem(atPath: file.path)[.posixPermissions] as? NSNumber)?.intValue, 0o600)
        try Data("corrupt".utf8).write(to: file)
        XCTAssertThrowsError(try repository.read(payload.attachment))
        try FileManager.default.removeItem(at: file)
        let external = root.appendingPathComponent("outside.png"); try data.write(to: external)
        try FileManager.default.createSymbolicLink(at: file, withDestinationURL: external)
        XCTAssertThrowsError(try repository.read(payload.attachment)); XCTAssertThrowsError(try repository.write(payload))
        XCTAssertEqual(try Data(contentsOf: external), data)
    }
    func testNoteAttachmentBackupRestoreAndFailureAtomicity() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let source = WorkspaceRepository(directory: root.appendingPathComponent("source")), target = WorkspaceRepository(directory: root.appendingPathComponent("target"))
        let image = try NoteImagePayload(data: noteImageData()); try source.attachmentRepository.write(image)
        var snapshot = WorkspaceSnapshot(); snapshot.noteAttachments = [image.attachment]
        snapshot.documents = [SavedDocument(toolID: "quickNote", title: "image.md", content: image.attachment.markdown)]
        try source.save(snapshot)
        XCTAssertNil(try source.load().attachmentData)
        let backup = try WorkspaceRepository.decode(source.backup(snapshot))
        XCTAssertEqual(backup.attachmentData?[image.attachment.path], image.data)
        let restored = try target.installBackup(backup)
        XCTAssertEqual(restored, snapshot); XCTAssertEqual(try target.load(), snapshot)
        XCTAssertEqual(try target.attachmentRepository.read(image.attachment), image.data)
        var broken = backup; broken.attachmentData?[image.attachment.path] = Data("corrupt".utf8)
        XCTAssertThrowsError(try target.installBackup(broken)); XCTAssertEqual(try target.load(), snapshot)
        broken = backup; broken.attachmentData = nil
        XCTAssertThrowsError(try target.installBackup(broken)); XCTAssertEqual(try target.load(), snapshot)
        var legacy = WorkspaceSnapshot(); legacy.documents = [SavedDocument(toolID: "quickNote", title: "old.md", content: "old")]
        XCTAssertEqual(try target.installBackup(legacy), legacy)
        // Restoring an older workspace retains image files required by a later undo/backup restore.
        XCTAssertEqual(try target.attachmentRepository.read(image.attachment), image.data)
        XCTAssertEqual(try target.installBackup(backup), snapshot)
    }
    func testNoteAttachmentExportCopiesAndMissingReferences() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let source = NoteAttachmentRepository(workspace: root.appendingPathComponent("source")), destination = root.appendingPathComponent("exports")
        try FileManager.default.createDirectory(at: destination, withIntermediateDirectories: true)
        let payload = try NoteImagePayload(data: noteImageData()); try source.write(payload)
        var vault = DocumentVault(documents: [SavedDocument(toolID: "quickNote", title: "图片.md", content: payload.attachment.markdown)])
        let copy = try vault.duplicate(vault.documents[0].id); _ = vault.delete(vault.documents[0].id)
        let content = vault.documents.first { $0.id == copy }!.content
        let exported = try source.exportDocument(name: "图片.md", content: content, attachments: [payload.attachment], to: destination)
        XCTAssertEqual(try String(contentsOf: exported.appendingPathComponent("图片.md"), encoding: .utf8), content)
        XCTAssertEqual(try Data(contentsOf: exported.appendingPathComponent(payload.attachment.path)), payload.data)
        let duplicate = try source.exportDocument(name: "图片.md", content: content, attachments: [payload.attachment], to: destination)
        XCTAssertNotEqual(exported, duplicate)
        XCTAssertThrowsError(try source.exportDocument(name: "missing.md", content: "![missing](attachments/missing.png)", attachments: [], to: destination))
        XCTAssertEqual(try FileManager.default.contentsOfDirectory(atPath: destination.path).count, 2)
        let outside = root.appendingPathComponent("outside"); try FileManager.default.createDirectory(at: outside, withIntermediateDirectories: true)
        let unsafe = root.appendingPathComponent("unsafe"); try FileManager.default.createDirectory(at: unsafe, withIntermediateDirectories: true)
        try FileManager.default.createSymbolicLink(at: unsafe.appendingPathComponent("attachments"), withDestinationURL: outside)
        XCTAssertThrowsError(try NoteAttachmentRepository(workspace: unsafe).write(payload))
        XCTAssertTrue(try FileManager.default.contentsOfDirectory(atPath: outside.path).isEmpty)
    }
    func testMarkdownImageBlocksAndLiteralCode() throws {
        let path = "attachments/" + String(repeating: "a", count: 64) + ".png"
        let text = "before ![caption](" + path + ") after\n\n```md\n![literal](" + path + ")\n```"
        let blocks = try MarkdownDocument(text).blocks
        XCTAssertEqual(blocks.map(\.kind), ["paragraph", "image", "paragraph", "code"])
        XCTAssertEqual(blocks[1].text, "caption"); XCTAssertEqual(blocks[1].destination, path)
        XCTAssertEqual(MarkdownImageReference.parse("![a](<" + path + "> \"title\")").first?.path, path)
        XCTAssertEqual(MarkdownImageReference.managedPaths(in: text), [path])
        XCTAssertTrue(MarkdownImageReference.managedPaths(in: "```md\n![literal](attachments/example.png)\n```\n`![inline](attachments/inline.png)`\n\\![escaped](attachments/escaped.png)").isEmpty)
        XCTAssertEqual(try MarkdownDocument("`![inline](attachments/example.png)`").blocks.map(\.kind), ["paragraph"])
        XCTAssertThrowsError(try MarkdownDocument(String(repeating: "![a](x) ", count: 1025)))
        XCTAssertTrue(try MarkdownDocument("![remote](https://example.com/image.png)").blocks.contains { $0.kind == "image" })
    }
    private func jsonOperation(_ action: String, _ input: String, configure: (inout JSONEngineRequest) -> Void = { _ in }) throws -> JSONEngineReply {
        var request = JSONEngineRequest(action, input: input); configure(&request)
        return try JSONEngine.evaluateLocally(request)
    }
    func testQuickNoteAllQuickReplaceActionsAndSelection() throws {
        let cases: [(QuickNoteAction, String, String)] = [
            (.trim, " a \r\n b ", "a\nb"), (.removeBlankLines, " a \n\n b ", " a \n b "), (.removeTabs, "a\tb", "ab"),
            (.scientificToNormal, "1.25e3 -2.5e-2", "1250 -0.025"), (.normalToScientific, "1250", "1.25e+3"),
            (.thousandsToNormal, "1,234,567.5", "1234567.5"), (.normalToThousands, "1234567.5", "1,234,567.5"),
            (.underscoreToCamel, "hello_world", "helloWorld"), (.camelToUnderscore, "helloWorld", "hello_world"),
            (.uppercase, "aBc", "ABC"), (.lowercase, "aBc", "abc"), (.linesToComma, "a\n\nb", "a,b"),
            (.linesToSingleQuoted, "a\nb", "'a','b'"), (.linesToDoubleQuoted, "a\nb", "\"a\",\"b\""),
            (.commaToLines, "\"a\", b", "a\nb"), (.tabsToLines, "a\tb", "a\nb"), (.clearNewlines, "a\r\nb", "ab"),
            (.deduplicateLines, "a\na\nb", "a\nb"), (.deduplicateWithCount, "a\na\nb", "a\t2\nb\t1"),
            (.escape, "a\nb", "a\\nb"), (.unescape, "a\\nb", "a\nb"), (.reverseLines, "a\nb", "b\na"),
            (.sortAscending, "b\na", "a\nb"), (.sortDescending, "a\nb", "b\na")
        ]
        XCTAssertEqual(cases.count, QuickNoteAction.allCases.count)
        for (action, source, expected) in cases {
            let reply = try jsonOperation("quickReplace", source) { $0.path = action.rawValue }
            XCTAssertEqual(reply.value, expected, action.rawValue)
        }
        let selected = try jsonOperation("quickReplace", "😀 keep abc tail") { $0.path = "uppercase"; $0.selectionStart = 8; $0.selectionEnd = 11 }
        XCTAssertEqual(selected.value, "😀 keep ABC tail"); XCTAssertEqual(selected.match?.range, NSRange(location: 8, length: 3))
        XCTAssertThrowsError(try jsonOperation("quickReplace", "1e-100000000") { $0.path = "scientificToNormal" })
        XCTAssertThrowsError(try jsonOperation("quickReplace", "bad\\q") { $0.path = "unescape" })
    }
    func testQuickNoteListPrefixAndTextSemantics() throws {
        XCTAssertEqual(try jsonOperation("noteBullet", "one\ntwo\nthree") { $0.selectionStart = 1; $0.selectionEnd = 6 }.value, "- one\n- two\nthree")
        XCTAssertEqual(try jsonOperation("noteNumbered", "one\ntwo\nthree") { $0.selectionStart = 4; $0.selectionEnd = 9 }.value, "one\n1. two\n2. three")
        XCTAssertEqual(try jsonOperation("noteBullet", "") .value, "- ")
        XCTAssertEqual(try jsonOperation("formatXML", "<tool><name>Native</name></tool>").value?.contains("<name>Native</name>"), true)
    }
    func testQuickNoteOptionsDocumentCopiesAndLegacyWorkspace() throws {
        var legacy = WorkspaceSnapshot(); legacy.documents = [SavedDocument(toolID: "quickNote", title: "legacy.md", content: "# Hello")]
        legacy.documents[0].noteOptions = nil
        let old = try WorkspaceRepository.decode(WorkspaceRepository.encode(legacy))
        XCTAssertNil(old.documents[0].noteOptions); XCTAssertEqual(QuickNoteOptions().syntax, .markdown)
        var options = QuickNoteOptions(); options.fontName = "Monaco"; options.fontSize = 22; options.lineSpacing = 1.6; options.lineWrap = false; options.color = .purple; options.syntax = .python
        var vault = DocumentVault(documents: legacy.documents); vault.documents[0].noteOptions = options
        let copy = try vault.duplicate(vault.documents[0].id)
        XCTAssertEqual(vault.documents.first { $0.id == copy }?.noteOptions, options)
        var snapshot = WorkspaceSnapshot(), draft = DraftRecord(), workspace = QuickNoteWorkspaceOptions()
        workspace.findOpen = true; workspace.quickReplaceOpen = true; workspace.findQuery = "x"; workspace.regex = true
        draft.noteOptions = options; draft.noteWorkspace = workspace; snapshot.drafts["quickNote"] = draft; snapshot.documents = vault.documents
        XCTAssertEqual(try WorkspaceRepository.decode(WorkspaceRepository.encode(snapshot)), snapshot)
        snapshot.documents[0].noteOptions?.fontSize = 200
        XCTAssertThrowsError(try snapshot.validated())
        snapshot.documents = []; snapshot.drafts["quickNote"]?.noteOptions?.lineSpacing = 0
        XCTAssertThrowsError(try snapshot.validated())
        XCTAssertEqual(QuickNoteOptions.forDocument("notes.txt").syntax, .plain)
        XCTAssertEqual(QuickNoteOptions.forDocument("notes.MD").syntax, .markdown)
    }
    func testMarkdownTablesListsFencesAndBounds() throws {
        let source = "# Heading\n\n| Name | Count | Code |\n| :--- | ---: | :---: |\n| **A** | 2 | `a|b` |\n| escaped\\|pipe | 3 | test |\n\n1. First\n- [x] Done\n  - [ ] Next\n> Quote\n\n~~~swift\n| not | a table |\n~~~\n\n[Link](https://example.com)"
        let blocks = try MarkdownDocument(source).blocks
        let table = blocks.first { $0.kind == "table" }!
        XCTAssertEqual(table.alignments, ["left", "right", "center"])
        XCTAssertEqual(table.cells[1], ["**A**", "2", "`a|b`"])
        XCTAssertEqual(table.cells[2][0], "escaped|pipe")
        XCTAssertEqual(blocks.filter { $0.kind == "list" }.map(\.marker), ["1.", "•", "•"])
        XCTAssertEqual(blocks.filter { $0.checked != nil }.map(\.checked), [true, false])
        XCTAssertEqual(blocks.first { $0.kind == "code" }?.language, "swift")
        XCTAssertEqual(blocks.first { $0.kind == "code" }?.text, "| not | a table |")
        XCTAssertEqual(try MarkdownDocument("Title\n===\n\n---").blocks.map(\.kind), ["heading", "divider"])
        XCTAssertEqual(try MarkdownDocument("a | b\nwrong | divider").blocks.first?.kind, "paragraph")
        XCTAssertThrowsError(try MarkdownDocument(String(repeating: "a", count: 2 * 1024 * 1024 + 1)))
        XCTAssertThrowsError(try MarkdownDocument(String(repeating: "\n", count: 20_001)))
        XCTAssertThrowsError(try MarkdownDocument(String(repeating: "a|", count: 10_001) + "\n" + String(repeating: "---|", count: 10_001)))
    }
    func testJSONNativeFormattingOrderAndDuplicateKeys() throws {
        let input = #"{"z":{"B":1,"a":2},"A":0}"#
        XCTAssertEqual(try jsonOperation("compress", input).value, input)
        XCTAssertEqual(try jsonOperation("format", #"{"name":"MooTool","items":[1,2]}"#).value,
                       "{\n  \"name\": \"MooTool\",\n  \"items\": [\n    1,\n    2\n  ]\n}")
        XCTAssertEqual(try jsonOperation("advanced", input) { $0.sortKeys = true; $0.ignoreCase = true }.value,
                       "{\n  \"A\": 0,\n  \"z\": {\n    \"a\": 2,\n    \"B\": 1\n  }\n}")
        XCTAssertEqual(try jsonOperation("duplicates", #"{"a":1,"A":2,"child":{"x":1,"\u0078":2}}"#) { $0.ignoreCase = true }.value,
                       #"["$.A","$.child.x"]"#)
        XCTAssertThrowsError(try jsonOperation("advanced", #"{"a":1,"a":2}"#))
        XCTAssertEqual(try jsonOperation("advanced", #"{"a":1,"a":2}"#) { $0.checkDuplicateKeys = false }.value, "{\n  \"a\": 2\n}")
        XCTAssertThrowsError(try jsonOperation("format", "[1,]"))
        XCTAssertThrowsError(try jsonOperation("format", String(repeating: "[", count: 130) + "0" + String(repeating: "]", count: 130)))
    }
    func testJSONNativePathsMatchElectronQueries() throws {
        let input = #"{"store":{"books":[{"title":"One","price":8},{"title":"Two","price":12},{"title":"Three","price":5}]},"a/b":{"~":false},"empty":[],"nil":null}"#
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.store.books[1].title" }.value, #""Two""#)
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.store.books[*].title" }.value, "[\n  \"One\",\n  \"Two\",\n  \"Three\"\n]")
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$..price" }.value, "[\n  8,\n  12,\n  5\n]")
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.store.books[?(@.price < 10 && @.title !== 'Three')].title" }.value, "[\n  \"One\"\n]")
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.store.books[0:2].title" }.count, 2)
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.store.books[0,2].title" }.count, 2)
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "/a~1b/~0" }.value, "false")
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.nil" }.value, "null")
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.empty" }.value, "[]")
        XCTAssertEqual(try jsonOperation("query", input) { $0.path = "$.missing" }.value, "undefined")
        XCTAssertThrowsError(try jsonOperation("query", input) { $0.path = "/bad~2pointer" })
        XCTAssertThrowsError(try jsonOperation("query", input) { $0.path = "$.store.books[?(@.constructor.constructor('return process')())]" })
    }
    func testJSONNativeXMLAndJavaConversions() throws {
        let xml = try jsonOperation("jsonToXml", #"{"name":"MooTool & <native>","enabled":true}"#).value!
        XCTAssertTrue(xml.contains("<name>MooTool &amp; &lt;native&gt;</name>"))
        let value = try jsonOperation("xmlToJson", "<tool enabled=\"true\"><name>MooTool</name><item>1</item><item>2</item></tool>").value!
        let parsed = try JSONSerialization.jsonObject(with: Data(value.utf8)) as! [String: Any]
        let tool = parsed["tool"] as! [String: Any]
        XCTAssertEqual(tool["@_enabled"] as? Bool, true); XCTAssertEqual(tool["item"] as? [Int], [1,2])
        XCTAssertThrowsError(try jsonOperation("xmlToJson", "<a><b></a>"))
        XCTAssertThrowsError(try jsonOperation("xmlToJson", "<!DOCTYPE x [<!ENTITY test SYSTEM 'file:///etc/hosts'>]><x>&test;</x>"))
        XCTAssertEqual(try jsonOperation("beanToJson", "public class User { private String name; private int age; private List<String> tags; }").value,
                       "{\n  \"name\": \"\",\n  \"age\": 0,\n  \"tags\": []\n}")
        let java = try jsonOperation("jsonToBean", #"{"class":1,"a-b":"one","a_b":"two","profile":{"active":true},"profiles":[{"id":1}],"mixed":[1,"a"]}"#) { $0.className = "ToolConfig" }.value!
        XCTAssertTrue(java.contains("import java.util.List;")); XCTAssertTrue(java.contains("private Long classValue;"))
        XCTAssertTrue(java.contains("private String a_b2;")); XCTAssertTrue(java.contains("public static class Profile2"))
        XCTAssertTrue(java.contains("List<Object> mixed")); XCTAssertTrue(java.hasSuffix("}"))
        let reserved = try jsonOperation("jsonToBean", #"{"string":{"text":"ok"},"list":[1,2],"object":{"list":{"id":1}}}"#) { $0.className = "List" }.value!
        XCTAssertTrue(reserved.contains("public class List2")); XCTAssertTrue(reserved.contains("public static class String2"))
        XCTAssertTrue(reserved.contains("private List<Long> list;")); XCTAssertTrue(reserved.contains("public static class List3"))
        XCTAssertThrowsError(try jsonOperation("jsonToBean", "[]"))
    }
    func testJSONNativeEscapesAndKeyValueSwap() throws {
        let input = "hello\n\t\"你好😀\"\\"
        let escaped = try jsonOperation("escape", input).value!
        XCTAssertEqual(try jsonOperation("unescape", escaped).value, input)
        let text = try jsonOperation("escapeText", "one\ntwo\t\\").value!
        XCTAssertEqual(try jsonOperation("unescapeText", text).value, "one\ntwo\t\\")
        XCTAssertThrowsError(try jsonOperation("unescape", "{}"))
        let result = try jsonOperation("swap", #"{"first":"one","second":2,"nested":{"x":"y"},"safe":"__proto__"}"#).value!
        let value = try JSONSerialization.jsonObject(with: Data(result.utf8)) as! [String: Any]
        XCTAssertEqual(value["one"] as? String, "first"); XCTAssertEqual(value["__proto__"] as? String, "safe")
    }
    func testJSONNativeFindReplaceUnicodeAndFlags() throws {
        let input = "😀 One one stone\nTWO 123"
        let reply = try jsonOperation("find", input) { $0.query = "one"; $0.wholeWord = true }
        XCTAssertEqual(reply.count, 2); XCTAssertEqual(reply.matches?.first?.range, NSRange(location: 3, length: 3))
        XCTAssertEqual(try jsonOperation("find", input) { $0.query = "one"; $0.matchCase = true; $0.wholeWord = true }.count, 1)
        XCTAssertEqual(try jsonOperation("replaceAll", input) { $0.query = "one"; $0.wholeWord = true; $0.replacement = "new" }.value, "😀 new new stone\nTWO 123")
        XCTAssertEqual(try jsonOperation("replaceAll", "item12 item34") { $0.query = #"item(\d+)"#; $0.regex = true; $0.replacement = #"$1\n"# }.value, "12\n 34\n")
        XCTAssertEqual(try jsonOperation("find", "abc") { $0.query = "(?=a)"; $0.regex = true }.count, 0)
        XCTAssertThrowsError(try jsonOperation("find", input) { $0.query = "["; $0.regex = true })
        XCTAssertEqual(try jsonOperation("find", "a b a") { $0.query = "a"; $0.selectionStart = 0; $0.selectionEnd = 0; $0.forward = false }.match?.start, 4)
    }
    func testJSONNativeOptionsLegacyAndBackup() throws {
        let legacy = try JSONDecoder().decode(JSONOptions.self, from: Data(#"{"indent":4,"sortKeys":true,"showsTree":true}"#.utf8))
        XCTAssertEqual(legacy.indent, 4); XCTAssertTrue(legacy.sortKeys); XCTAssertTrue(legacy.checkDuplicateKeys); XCTAssertNil(legacy.inspectorOpen)
        var snapshot = WorkspaceSnapshot(), draft = DraftRecord(), options = JSONOptions()
        options.inspectorOpen = true; options.wrapLines = false; options.findQuery = "name"; options.replacement = "title"; options.regex = true; options.ignoreCase = true
        draft.json = options; snapshot.drafts["json"] = draft
        XCTAssertEqual(try WorkspaceRepository.decode(WorkspaceRepository.encode(snapshot)), snapshot)
        options.className = String(repeating: "a", count: 241); draft.json = options; snapshot.drafts["json"] = draft
        XCTAssertThrowsError(try snapshot.validated())
    }
    func testJSONWorkerTimeoutCancellationAndIsolation() async throws {
        var request = JSONEngineRequest("find", input: String(repeating: "a", count: 20000) + "!")
        request.regex = true; request.query = "(a+)+$"
        do { _ = try await JSONEngine.execute(request, timeout: 0.15); XCTFail("Pathological regular expression did not stop") } catch {}
        let task = Task { try await JSONEngine.execute(request) }
        try await Task.sleep(for: .milliseconds(40)); task.cancel()
        do { _ = try await task.value; XCTFail("Worker did not cancel") } catch is CancellationError {} catch { XCTFail("Cancellation did not propagate: \(error)") }
        let normal = try await JSONEngine.execute(JSONEngineRequest("compress", input: #"{ "name": "MooTool" }"#))
        XCTAssertEqual(normal.value, #"{"name":"MooTool"}"#)
    }
    func testVaultHierarchyMutationsAndIdentity() throws {
        var vault = DocumentVault()
        let folder = try vault.createFolder(toolID: "json", name: "API")
        let nested = try vault.createFolder(toolID: "json", name: "Responses", parent: folder)
        let file = try vault.createDocument(toolID: "json", name: "data.json", content: "{\"a\":1}", parent: nested)
        XCTAssertEqual(vault.path(of: file), "API/Responses/data.json")
        XCTAssertEqual(vault.ancestors(of: file), [folder, nested])
        try vault.rename(folder, to: "接口")
        XCTAssertEqual(vault.path(of: file), "接口/Responses/data.json")
        try vault.move(nested, to: nil)
        XCTAssertEqual(vault.path(of: file), "Responses/data.json")
        let duplicate = try vault.duplicate(file)
        XCTAssertNotEqual(file, duplicate)
        XCTAssertEqual(vault.name(of: duplicate), "data (2).json")
        XCTAssertEqual(vault.documents.first { $0.id == duplicate }?.content, "{\"a\":1}")
        let removed = vault.delete(nested)
        XCTAssertEqual(removed, Set([nested, file, duplicate]))
        XCTAssertEqual(vault.folders.map(\.id), [folder]); XCTAssertTrue(vault.documents.isEmpty)
        try vault.validate()
    }
    func testVaultRejectsCyclesConflictsAndForeignParents() throws {
        var vault = DocumentVault()
        let root = try vault.createFolder(toolID: "json", name: "root")
        let child = try vault.createFolder(toolID: "json", name: "child", parent: root)
        let note = try vault.createFolder(toolID: "quickNote", name: "notes")
        let file = try vault.createDocument(toolID: "json", name: "Data.json", parent: root)
        let before = vault.folders
        XCTAssertThrowsError(try vault.move(root, to: child))
        XCTAssertThrowsError(try vault.move(root, to: root))
        XCTAssertEqual(vault.folders, before)
        XCTAssertThrowsError(try vault.move(file, to: note))
        XCTAssertThrowsError(try vault.createDocument(toolID: "json", name: "data.JSON", parent: root))
        XCTAssertThrowsError(try vault.createFolder(toolID: "json", name: "Data.json", parent: root))
        XCTAssertThrowsError(try vault.rename(file, to: "child"))
        for name in ["", "..", "../escape", "a/b", "a\\b", "a:b", "a\nname", "\u{0}"] { XCTAssertThrowsError(try DocumentVault.validName(name)) }
        var corrupt = vault; corrupt.folders[0].parentID = child; XCTAssertThrowsError(try corrupt.validate())
        corrupt = vault; corrupt.documents[0].parentID = UUID(); XCTAssertThrowsError(try corrupt.validate())
        corrupt = vault; corrupt.folders[0].id = file; XCTAssertThrowsError(try corrupt.validate())
        var deep = DocumentVault(); var parent: UUID?
        for i in 0..<64 { parent = try deep.createFolder(toolID: "json", name: "level\(i)", parent: parent) }
        XCTAssertThrowsError(try deep.createFolder(toolID: "json", name: "too-deep", parent: parent))
    }
    func testVaultSearchSortAndExpansion() throws {
        var vault = DocumentVault()
        let folder = try vault.createFolder(toolID: "json", name: "API")
        let first = try vault.createDocument(toolID: "json", name: "item2.json", content: "中文 needle", parent: folder)
        let second = try vault.createDocument(toolID: "json", name: "item10.json", content: "other", parent: folder)
        _ = try vault.createDocument(toolID: "quickNote", name: "needle.md", content: "private")
        var preferences = VaultPreferences()
        XCTAssertEqual(vault.rows(toolID: "json", preferences: preferences).map(\.id), [folder])
        preferences.expanded = [folder]
        XCTAssertEqual(vault.rows(toolID: "json", preferences: preferences).map(\.id), [folder, first, second])
        preferences.query = "NEEDLE"; preferences.expanded = []
        XCTAssertEqual(vault.rows(toolID: "json", preferences: preferences).map(\.id), [folder, first])
        preferences.includeContent = false
        XCTAssertTrue(vault.tree(toolID: "json", preferences: preferences).isEmpty)
        preferences.query = "API"
        XCTAssertEqual(vault.rows(toolID: "json", preferences: preferences).map(\.id), [folder, first, second])
        preferences.query = " "
        XCTAssertEqual(vault.rows(toolID: "json", preferences: preferences).map(\.id), [folder])
        preferences.query = ""; preferences.sort = .modified; preferences.expanded = [folder]
        vault.documents[0].modified = Date(timeIntervalSince1970: 1); vault.documents[1].modified = Date(timeIntervalSince1970: 2)
        XCTAssertEqual(vault.rows(toolID: "json", preferences: preferences).map(\.id), [folder, second, first])
    }
    func testVaultBatchImportIsAtomicAndPreservesPaths() throws {
        var vault = DocumentVault()
        let items = [DocumentImportItem(relativePath: "folder/a.json", content: "{}"), DocumentImportItem(relativePath: "folder/a.json", content: "[1]"), DocumentImportItem(relativePath: "folder/sub/b.json", content: "invalid draft is retained")]
        let ids = try vault.importDocuments(items, toolID: "json")
        XCTAssertEqual(ids.count, 3)
        XCTAssertEqual(vault.path(of: ids[0]), "folder/a.json")
        XCTAssertEqual(vault.path(of: ids[1]), "folder/a (2).json")
        XCTAssertEqual(vault.path(of: ids[2]), "folder/sub/b.json")
        let beforeFiles = vault.documents, beforeFolders = vault.folders
        XCTAssertThrowsError(try vault.importDocuments([DocumentImportItem(relativePath: "okay.json", content: "{}"), DocumentImportItem(relativePath: "../escape.json", content: "{}")] , toolID: "json"))
        XCTAssertEqual(vault.documents, beforeFiles); XCTAssertEqual(vault.folders, beforeFolders)
        XCTAssertThrowsError(try vault.importDocuments([DocumentImportItem(relativePath: "/absolute.json", content: "{}")], toolID: "json"))
        XCTAssertThrowsError(try vault.importDocuments(Array(repeating: DocumentImportItem(relativePath: "a.json", content: ""), count: 501), toolID: "json"))
    }
    func testVaultImportReaderUsesOnlySelectedUTF8Files() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let nested = directory.appendingPathComponent("选中的文档/sub")
        try FileManager.default.createDirectory(at: nested, withIntermediateDirectories: true)
        try Data("\u{FEFF}{\"中文\":true}".utf8).write(to: nested.appendingPathComponent("data.json"))
        try Data("# Note".utf8).write(to: nested.appendingPathComponent("note.md"))
        try Data("hidden".utf8).write(to: nested.appendingPathComponent(".hidden.json"))
        let outside = directory.appendingPathComponent("outside.json"); try Data("outside".utf8).write(to: outside)
        try FileManager.default.createSymbolicLink(at: nested.appendingPathComponent("link.json"), withDestinationURL: outside)
        let items = try DocumentImportReader.read([nested.deletingLastPathComponent()], toolID: "json")
        XCTAssertEqual(items.count, 1); XCTAssertEqual(items[0].relativePath, "选中的文档/sub/data.json")
        XCTAssertEqual(items[0].content, "{\"中文\":true}")
        XCTAssertThrowsError(try DocumentImportReader.read([nested.appendingPathComponent("link.json")], toolID: "json"))
        try Data([0xff, 0xfe, 0xff]).write(to: nested.appendingPathComponent("bad.json"))
        XCTAssertThrowsError(try DocumentImportReader.read([nested], toolID: "json"))
        let notes = try DocumentImportReader.read([nested], toolID: "quickNote")
        XCTAssertEqual(notes.count, 1); XCTAssertEqual(notes[0].content, "# Note")
    }
    func testEditorStateClampingAndVaultBackupCompatibility() throws {
        let clamped = EditorViewState(location: Int.max, length: Int.max, scrollX: -.infinity, scrollY: -1).clamped(toUTF16Length: 6)
        XCTAssertEqual(clamped, EditorViewState(location: 6))
        XCTAssertEqual(EditorViewState(location: -2, length: 8).clamped(toUTF16Length: 3), EditorViewState(location: 0, length: 3))
        var legacy = WorkspaceSnapshot(); legacy.documents = [SavedDocument(toolID: "json", title: "Legacy", content: "{}")]
        let oldData = try WorkspaceRepository.encode(legacy)
        XCTAssertFalse(String(decoding: oldData, as: UTF8.self).contains("vaultPreferences"))
        XCTAssertEqual(try WorkspaceRepository.decode(oldData), legacy)
        let folder = DocumentFolder(toolID: "json", name: "Folder")
        legacy.folders = [folder]; legacy.documents[0].parentID = folder.id
        legacy.documents[0].inputEditor = EditorViewState(location: 1, scrollY: 400)
        var preference = VaultPreferences(); preference.selectedEntryID = legacy.documents[0].id; preference.expanded = [folder.id]
        legacy.vaultPreferences = ["json": preference]
        legacy.scratchDrafts = ["json": DraftRecord()]
        XCTAssertEqual(try WorkspaceRepository.decode(WorkspaceRepository.encode(legacy)), legacy)
        legacy.documents[0].parentID = UUID(); XCTAssertThrowsError(try legacy.validated())
    }
    func testCatalogMatchesElectronToolIDs() {
        let ids = ["mootool", "quickNote", "textDiff", "reformat", "json", "java", "ymlProperties", "protobuf", "variables", "http", "host", "net", "uaParse", "encode", "crypto", "regex", "cron", "qrCode", "timeConvert", "messageBoard", "translation", "calculator", "colorBoard", "image", "pdf", "hardware"]
        XCTAssertEqual(Catalog.tools.map(\.id), ids)
        XCTAssertEqual(Set(Catalog.tools.map(\.id)).count, 26)
        XCTAssertTrue(Catalog.tool("json").matches("JSONPATH"))
        XCTAssertEqual(Catalog.tool("missing").id, "mootool")
        XCTAssertTrue(Product.dataDirectory.path.hasSuffix(Product.bundleID))
        XCTAssertFalse(Product.bundleID.contains("electron"))
    }
    func testJSONScalarsAndEscapedStrings() throws {
        for input in ["null", "true", "42", "\"hello\"", "[]", "{}"] { XCTAssertEqual(try TextServices.json(input, pretty: false), input) }
        XCTAssertThrowsError(try TextServices.json("{bad"))
        XCTAssertEqual(try TextServices.json("{\"z\":2,\"a\":\"你好\"}", pretty: false), "{\"a\":\"你好\",\"z\":2}")
    }
    func testJSONPathAndPointer() throws {
        let input = #"{"users":[{"name":"周"}],"a/b":{"~key":null}}"#
        XCTAssertEqual(try TextServices.jsonPath(input, path: "$.users[0].name"), "\"周\"")
        XCTAssertEqual(try TextServices.jsonPath(input, path: "/a~1b/~0key"), "null")
        XCTAssertThrowsError(try TextServices.jsonPath(input, path: "$.users[2]"))
        XCTAssertThrowsError(try TextServices.jsonPath(input, path: "$..name"))
        XCTAssertThrowsError(try TextServices.jsonPath(input, path: "/users/00"))
        XCTAssertThrowsError(try TextServices.jsonPath(input, path: "/a~2b"))
        XCTAssertEqual(try TextServices.jsonPath(input, path: ""), try TextServices.json(input))
    }
    func testJSONStructureTypesPathsAndLimits() throws {
        let input = #"{"a/b":{"~key":true,"":1},"array":[null,"中文",{}],"number":0}"#
        let tree = try JSONStructure(input)
        XCTAssertEqual(tree.root.kind, "Object"); XCTAssertEqual(tree.nodeCount, 9); XCTAssertEqual(tree.maxDepth, 2)
        func nodes(_ node: JSONTreeNode) -> [JSONTreeNode] { [node] + (node.children ?? []).flatMap(nodes) }
        let all = nodes(tree.root)
        XCTAssertEqual(Set(all.map(\.id)).count, 9)
        XCTAssertEqual(all.first { $0.pointer == "/a~1b/~0key" }?.kind, "Boolean")
        XCTAssertEqual(all.first { $0.pointer == "/a~1b/" }?.kind, "Number")
        XCTAssertEqual(all.first { $0.pointer == "/array/0" }?.kind, "Null")
        for node in all { _ = try TextServices.jsonPath(input, path: node.pointer) }
        let special = #"{"0":{"a/b":true,"quote\"'key":1,"bracket[0]":2,"line\nkey":3},"array":[false]}"#
        let specialNodes = nodes(try JSONStructure(special).root)
        XCTAssertEqual(specialNodes.first { $0.pointer == "/0" }?.queryPath, #"$["0"]"#)
        XCTAssertEqual(specialNodes.first { $0.pointer == "/array/0" }?.queryPath, "$.array[0]")
        for node in specialNodes {
            var request = JSONEngineRequest("query", input: special); request.path = node.queryPath
            let value = try JSONEngine.evaluateLocally(request).value!
            XCTAssertEqual(try TextServices.json(value), try TextServices.jsonPath(special, path: node.pointer))
        }
        XCTAssertEqual(try JSONStructure("false").root.summary, "false")
        XCTAssertEqual(try JSONStructure("1").root.kind, "Number")
        XCTAssertThrowsError(try JSONStructure(input, nodeLimit: 3))
        XCTAssertThrowsError(try JSONStructure(input, depthLimit: 1))
        XCTAssertThrowsError(try JSONStructure("bad JSON"))
    }
    func testJSONFormattingOptionsPreserveStringContent() throws {
        let input = #"{"z":{"line":"    spaces\n  next"},"a":true}"#
        let result = try TextServices.json(input, sorted: true, indent: 4)
        XCTAssertTrue(result.contains("\n    \"a\"")); XCTAssertTrue(result.contains("\n        \"line\""))
        XCTAssertEqual(try TextServices.json(result, pretty: false), try TextServices.json(input, pretty: false))
        XCTAssertThrowsError(try TextServices.json(input, indent: 3))
    }
    func testEncodingRoundTrips() throws {
        let text = "你好 🌍 &<>\"' /?=+#\n"
        for format in ["Base64", "URL", "Hex", "Unicode", "HTML"] {
            XCTAssertEqual(try TextServices.encode(TextServices.encode(text, format: format, decode: false), format: format, decode: true), text, format)
        }
        XCTAssertEqual(try TextServices.encode("&#x1F600;&#39;", format: "HTML", decode: true), "😀'")
        XCTAssertThrowsError(try TextServices.encode("zz", format: "Hex", decode: true))
        XCTAssertThrowsError(try TextServices.encode("a", format: "Hex", decode: true))
        XCTAssertThrowsError(try TextServices.encode("%xx", format: "URL", decode: true))
        XCTAssertThrowsError(try TextServices.encode("%%%", format: "Base64", decode: true))
    }
    func testKnownDigests() throws {
        XCTAssertEqual(try TextServices.digest("abc", algorithm: "SHA-256"), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        XCTAssertEqual(try TextServices.digest("abc", algorithm: "MD5"), "900150983cd24fb0d6963f7d28e17f72")
        XCTAssertEqual(try TextServices.digest("The quick brown fox jumps over the lazy dog", algorithm: "HMAC-SHA256", key: "key"), "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8")
    }
    func testAESGCMAuthenticatedRoundTrip() throws {
        let key = String(repeating: "01", count: 32)
        let sealed = try TextServices.digest("中文 secret", algorithm: "AES-GCM 加密", key: key)
        XCTAssertEqual(try TextServices.digest(sealed, algorithm: "AES-GCM 解密", key: key), "中文 secret")
        XCTAssertThrowsError(try TextServices.digest(sealed, algorithm: "AES-GCM 解密", key: String(repeating: "02", count: 32)))
        XCTAssertThrowsError(try TextServices.digest("hello", algorithm: "AES-GCM 加密", key: "01"))
        XCTAssertNotEqual(sealed, try TextServices.digest("中文 secret", algorithm: "AES-GCM 加密", key: key))
    }
    func testRegexUnicodeGroupsReplacementAndFlags() throws {
        let result = try TextServices.regex("🌍 hello@world.dev", pattern: #"(\w+)@([\w.]+)"#)
        XCTAssertTrue(result.contains("world.dev")); XCTAssertTrue(result.contains("utf16Offset"))
        XCTAssertEqual(try TextServices.regex("Hello HELLO", pattern: "hello", replacement: "hi", flags: "i"), "hi hi")
        XCTAssertEqual(try TextServices.regex("ab", pattern: "(a)(b)", replacement: "$2$1"), "ba")
        XCTAssertThrowsError(try TextServices.regex("text", pattern: "["))
    }
    func testConfigConversionsAndConflicts() throws {
        XCTAssertEqual(try TextServices.config("app:\n  name: MooTool\n  enabled: true", from: "YAML", to: "JSON"), try TextServices.json(#"{"app":{"name":"MooTool","enabled":true}}"#))
        XCTAssertTrue(try TextServices.config("app.name=MooTool\napp.port=8080", from: "Properties", to: "YAML").contains("MooTool"))
        XCTAssertEqual(try TextServices.config(#"{"a":{"b":"c"}}"#, from: "JSON", to: "Properties"), "a.b=c")
        XCTAssertEqual(try TextServices.config(#"{"enabled":true,"disabled":false,"count":1}"#, from: "JSON", to: "Properties"), "count=1\ndisabled=false\nenabled=true")
        XCTAssertThrowsError(try TextServices.config(#"{"value":" trailing "}"#, from: "JSON", to: "Properties"))
        XCTAssertThrowsError(try TextServices.config(#"path=C:\temp"#, from: "Properties", to: "JSON"))
        XCTAssertThrowsError(try TextServices.config("a=1\na.b=2", from: "Properties", to: "JSON"))
        XCTAssertThrowsError(try TextServices.config("a=1\na=2", from: "Properties", to: "JSON"))
        XCTAssertThrowsError(try TextServices.config(#"{"array":[1,2]}"#, from: "JSON", to: "Properties"))
        XCTAssertThrowsError(try TextServices.config(#"{"a.b":1}"#, from: "JSON", to: "Properties"))
    }
    func testXMLDoesNotReadExternalEntities() throws {
        let xml = try TextServices.formatXML("<root><value>hello</value></root>")
        XCTAssertTrue(xml.contains("hello"))
        let external = try TextServices.formatXML("<!DOCTYPE foo [<!ENTITY xxe SYSTEM 'file:///etc/hosts'>]><foo>&xxe;</foo>")
        XCTAssertFalse(external.contains("localhost"))
    }
    func testDiff() {
        let diff = TextServices.diff("a\nb\nc", "a\nx\nc\nd")
        XCTAssertTrue(diff.contains("− b")); XCTAssertTrue(diff.contains("+ x")); XCTAssertTrue(diff.contains("+ d")); XCTAssertTrue(diff.contains("  c"))
        XCTAssertTrue(TextServices.diff("same", "same").contains("新增 0 行"))
    }
    func testTimestampFractionsNegativeAndZones() throws {
        XCTAssertTrue(try DeveloperServices.timestamp("-1", zone: "UTC").contains("1969-12-31 23:59:59"))
        XCTAssertTrue(try DeveloperServices.timestamp("1700000000123", zone: "UTC").contains(".123"))
        XCTAssertTrue(try DeveloperServices.timestamp("1970-01-01 08:00:00", zone: "Asia/Shanghai").contains("秒      0"))
        XCTAssertThrowsError(try DeveloperServices.timestamp("NaN", zone: "UTC"))
        XCTAssertThrowsError(try DeveloperServices.timestamp("1e30", zone: "UTC"))
        XCTAssertThrowsError(try DeveloperServices.timestamp("0", zone: "invalid-zone"))
    }
    func testCalculatorPrecedenceAndFunctions() throws {
        for (input, expected) in [("2 + 3 * 4", 14.0), ("2^3^2", 512), ("-2^2", -4), ("2^-2", 0.25), ("sqrt(144)+sin(pi/2)", 13), ("1.2e3/2", 600)] {
            var calc = try Calculator(input); XCTAssertEqual(try calc.evaluate(), expected, accuracy: 0.000001)
        }
        for text in ["1/0", "sqrt(-1)", "2+", "(1", "", "pow(2,3)"] { var calc = try Calculator(text); XCTAssertThrowsError(try calc.evaluate(), text) }
        XCTAssertThrowsError(try Calculator("print('hello')"))
    }
    func testCronTimesAndValidation() throws {
        let zone = TimeZone(secondsFromGMT: 0)!
        let start = ISO8601DateFormatter().date(from: "2026-09-05T12:01:30Z")!
        let result = try CronExpression("*/15 * * * *").next(after: start, count: 3, timeZone: zone)
        XCTAssertEqual(ISO8601DateFormatter().string(from: result[0]), "2026-09-05T12:15:00Z")
        XCTAssertEqual(result[1].timeIntervalSince(result[0]), 900)
        XCTAssertThrowsError(try CronExpression("60 * * * *"))
        XCTAssertThrowsError(try CronExpression("*/0 * * * *"))
        XCTAssertThrowsError(try CronExpression("0 0 0 * * *"))
        XCTAssertThrowsError(try CronExpression("0 0 31 2 *").next(after: start, count: 1))
        // Traditional cron ORs restricted day-of-month and day-of-week fields.
        let sunday = try CronExpression("0 0 1 * 0").next(after: start, count: 1, timeZone: zone)[0]
        XCTAssertEqual(ISO8601DateFormatter().string(from: sunday), "2026-09-06T00:00:00Z")
    }
    func testProtobufBoundsAndTypes() throws {
        let decoded = try DeveloperServices.protobuf("08 96 01 12 07 4d 6f 6f 54 6f 6f 6c", base64: false)
        XCTAssertTrue(decoded.contains("150")); XCTAssertTrue(decoded.contains("MooTool"))
        XCTAssertThrowsError(try DeveloperServices.protobuf("12 ff ff", base64: false))
        XCTAssertThrowsError(try DeveloperServices.protobuf("00", base64: false))
        XCTAssertThrowsError(try DeveloperServices.protobuf("08 ffffffffffffffffffff", base64: false))
        XCTAssertThrowsError(try DeveloperServices.protobuf("0a05aabb", base64: false))
    }
    func testPDFPageRanges() throws {
        XCTAssertEqual(try DeveloperServices.pageIndices("1-3, 5", count: 6), [0, 1, 2, 4])
        XCTAssertEqual(try DeveloperServices.pageIndices("", count: 3), [0, 1, 2])
        XCTAssertEqual(try DeveloperServices.pageIndices("3,1,3", count: 3), [2, 0, 2])
        for input in ["0", "4", "3-1", "1,,2", "-1", "a"] { XCTAssertThrowsError(try DeveloperServices.pageIndices(input, count: 3)) }
    }
    func testHTTPRequestValidation() throws {
        let request = try NetworkServices.request(method: "POST", url: "https://example.com/api", headers: "Content-Type: application/json\nX-Token: a:b", body: "{}")
        XCTAssertEqual(request.httpBody, Data("{}".utf8)); XCTAssertEqual(request.value(forHTTPHeaderField: "X-Token"), "a:b")
        XCTAssertThrowsError(try NetworkServices.request(method: "GET", url: "file:///etc/hosts", headers: "", body: ""))
        XCTAssertThrowsError(try NetworkServices.request(method: "GET", url: "https://example.com", headers: "bad key: value", body: ""))
        XCTAssertNil(try NetworkServices.request(method: "GET", url: "https://example.com", headers: "", body: "ignored").httpBody)
    }
    func testHTTPParametersCookiesAndBodyModes() throws {
        var draft = DraftRecord(); draft.mode = "POST"; draft.option = "https://example.com/api?keep=a%2Bb#section"; draft.input = "raw"
        var options = HTTPOptions(); options.timeout = 5; options.bodyKind = .form
        options.params = [HTTPField("q", "你好 +&"), HTTPField("q", "two"), HTTPField("ignored", "x", enabled: false)]
        options.form = [HTTPField("a b", "x+y &"), HTTPField("empty", "")]
        options.cookies = [HTTPField("session", "abc=123"), HTTPField("off", "yes", enabled: false)]
        draft.http = options; draft.secondary = "X-Trace: one\r\nX-Trace: two\r\nCookie: original=yes"
        let request = try NetworkServices.request(draft)
        XCTAssertEqual(request.url?.absoluteString, "https://example.com/api?keep=a%2Bb&q=%E4%BD%A0%E5%A5%BD%20%2B%26&q=two#section")
        XCTAssertEqual(request.httpBody, Data("a%20b=x%2By%20%26&empty=".utf8))
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/x-www-form-urlencoded")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Cookie"), "original=yes; session=abc=123")
        XCTAssertEqual(request.value(forHTTPHeaderField: "X-Trace"), "one,two")
        XCTAssertEqual(request.timeoutInterval, 5)
        draft.http?.bodyKind = .json
        XCTAssertEqual(try NetworkServices.request(draft).value(forHTTPHeaderField: "Content-Type"), "application/json")
        draft.secondary += "\ncontent-type: application/custom"
        XCTAssertEqual(try NetworkServices.request(draft).value(forHTTPHeaderField: "Content-Type"), "application/custom")
        draft.http?.bodyKind = .none; XCTAssertNil(try NetworkServices.request(draft).httpBody)
        draft.http?.cookies.append(HTTPField("bad", "value;inject=1")); XCTAssertThrowsError(try NetworkServices.request(draft))
        draft.http?.cookies = []; draft.http?.timeout = .infinity; XCTAssertThrowsError(try NetworkServices.request(draft))
        XCTAssertThrowsError(try HTTPFields.headerLines("A: value\rB: injected"))
        XCTAssertThrowsError(try HTTPFields.headerLines("A: value\u{0}"))
        XCTAssertThrowsError(try HTTPFields.appendingQuery("x=%zz", to: "https://example.com"))
        XCTAssertThrowsError(try HTTPFields.appendingQuery("x=unencoded space", to: "https://example.com"))
    }
    func testCurlImportExportPreservesRequest() throws {
        let body = "{\"name\":\"O'Reilly\",\"literal\":\"$(echo nope)\",\"text\":\"你好\"}\n"
        let command = "curl --location -XPOST 'https://example.com/api?a=1&a=2' \\\n -H 'Content-Type: application/json' -H \"X-Trace: a:b\" -b 'session=abc; theme=dark' --data-raw " + CurlCommand.quote(body)
        let imported = try CurlCommand.parse(command)
        XCTAssertEqual(imported.input, body); XCTAssertEqual(imported.mode, "POST"); XCTAssertEqual(imported.http?.followRedirects, true)
        XCTAssertEqual(imported.http?.cookies.count, 2)
        let original = try NetworkServices.request(imported)
        let exported = try CurlCommand.export(imported)
        let restored = try NetworkServices.request(CurlCommand.parse(exported))
        XCTAssertEqual(restored.url, original.url); XCTAssertEqual(restored.httpMethod, original.httpMethod)
        XCTAssertEqual(restored.httpBody, original.httpBody)
        XCTAssertEqual(restored.allHTTPHeaderFields, original.allHTTPHeaderFields)
        let json = try CurlCommand.parse("curl https://example.com --json '{\"a\":' --json '1}'")
        XCTAssertEqual(json.input, "{\"a\":1}"); XCTAssertEqual(json.mode, "POST")
        XCTAssertEqual(try NetworkServices.request(json).value(forHTTPHeaderField: "Accept"), "application/json")
        let basic = try CurlCommand.parse("curl --url=https://example.com -u 'user:pass' -m 7 -I")
        XCTAssertEqual(basic.mode, "HEAD"); XCTAssertEqual(basic.http?.timeout, 7)
        XCTAssertEqual(try NetworkServices.request(basic).value(forHTTPHeaderField: "Authorization"), "Basic dXNlcjpwYXNz")
    }
    func testCurlQueryEncodingAndUnsupportedSyntax() throws {
        let query = try CurlCommand.parse("curl -G 'https://example.com/?old=1' --data-urlencode 'q=你好 +&' -d 'page=2'")
        XCTAssertEqual(query.mode, "GET"); XCTAssertEqual(query.input, "")
        XCTAssertEqual(query.option, "https://example.com/?old=1&q=%E4%BD%A0%E5%A5%BD%20%2B%26&page=2")
        XCTAssertEqual(try CurlCommand.parse("curl https://example.com -d ''").mode, "POST")
        XCTAssertEqual(try CurlCommand.parse("curl https://example.com --data-raw '@literal'").input, "@literal")
        XCTAssertThrowsError(try CurlCommand.parse("curl https://example.com -A 'agent\nX-Injected: value'"))
        for command in ["echo curl https://example.com", "curl 'https://example.com", "curl https://example.com --unknown", "curl https://example.com --data-binary '@/etc/hosts'", "curl https://example.com -b cookies.txt", "curl https://example.com | sh", "curl \"https://example.com/$TOKEN\"", "curl https://example.com --data-urlencode 'q@file'", "curl -G https://example.com -d 'a=%zz'", "curl -XGET https://example.com -d body", "curl https://example.com https://other.example.com", "curl https://example.com -k", "curl https://example.com --max-time 121"] {
            XCTAssertThrowsError(try CurlCommand.parse(command), command)
        }
    }
    func testExtendedWorkspaceAndLegacyCompatibility() throws {
        var state = WorkspaceSnapshot()
        let legacy = try WorkspaceRepository.encode(state)
        XCTAssertFalse(String(decoding: legacy, as: UTF8.self).contains("httpRequests"))
        XCTAssertEqual(try WorkspaceRepository.decode(legacy), state)
        var draft = try CurlCommand.parse("curl https://example.com --json '{\"ok\":true}'")
        draft.httpResult = HTTPResultMetadata(status: 201, headers: "X-Test: yes", cookies: "", url: draft.option, elapsed: 0.2, bytes: 10)
        draft.json = JSONOptions(); draft.json?.indent = 4; draft.json?.showsTree = true
        state.drafts["http"] = draft; state.httpRequests = [SavedHTTPRequest(name: "测试请求", collection: "本地 API", draft: draft)]
        state.history = [HistoryRecord(toolID: "http", draft: draft)]
        XCTAssertEqual(try WorkspaceRepository.decode(WorkspaceRepository.encode(state)), state)
        var invalid = state; invalid.httpRequests?.append(state.httpRequests![0]); XCTAssertThrowsError(try invalid.validated())
        invalid = state; invalid.drafts["http"]?.http?.timeout = 0; XCTAssertThrowsError(try invalid.validated())
        invalid = state; let field = HTTPField("a", "b"); invalid.drafts["http"]?.http?.params = [field, field]; XCTAssertThrowsError(try invalid.validated())
    }
    func testHTTPRedirectPolicyAndSessionIsolation() async throws {
        let fixture = try HTTPFixture()
        defer { close(fixture.listener) }
        let server = Task.detached { try fixture.serve(count: 5) }
        var draft = DraftRecord(); draft.mode = "GET"; draft.option = fixture.url + "/redirect"
        let stopped = try await NetworkServices.send(NetworkServices.request(draft), followRedirects: false)
        XCTAssertEqual(stopped.status, 302); XCTAssertTrue(stopped.cookies.contains("session=native"))
        let followed = try await NetworkServices.send(NetworkServices.request(draft), followRedirects: true)
        XCTAssertEqual(followed.status, 200); XCTAssertEqual(followed.url, fixture.url + "/echo")
        XCTAssertTrue(followed.body.lowercased().contains("cookie: session=native"))
        draft.option = fixture.url + "/echo"
        let fresh = try await NetworkServices.send(NetworkServices.request(draft))
        XCTAssertFalse(fresh.body.lowercased().contains("cookie:"))
        draft.mode = "POST"; draft.http = HTTPOptions(); draft.http?.bodyKind = .form
        draft.http?.form = [HTTPField("q", "中文 +")]; draft.http?.params = [HTTPField("a", "1"), HTTPField("a", "2")]
        let post = try await NetworkServices.send(NetworkServices.request(draft))
        XCTAssertTrue(post.body.hasPrefix("POST /echo?a=1&a=2 HTTP/1.1"))
        XCTAssertTrue(post.body.hasSuffix("q=%E4%B8%AD%E6%96%87%20%2B"))
        let requests = try await server.value; XCTAssertEqual(requests.count, 5)
    }
    func testLocalHTTPResponse() async throws {
        let listener = socket(AF_INET, SOCK_STREAM, 0)
        guard listener >= 0 else { throw ToolError("Cannot create local HTTP fixture") }
        defer { close(listener) }
        var address = sockaddr_in(); address.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        address.sin_family = sa_family_t(AF_INET); address.sin_addr.s_addr = inet_addr("127.0.0.1")
        let bound = withUnsafePointer(to: &address) { pointer in
            pointer.withMemoryRebound(to: sockaddr.self, capacity: 1) { bind(listener, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) }
        }
        guard bound == 0, listen(listener, 1) == 0 else { throw ToolError("Cannot bind local fixture") }
        var size = socklen_t(MemoryLayout<sockaddr_in>.size)
        _ = withUnsafeMutablePointer(to: &address) { pointer in pointer.withMemoryRebound(to: sockaddr.self, capacity: 1) { getsockname(listener, $0, &size) } }
        let port = UInt16(bigEndian: address.sin_port)
        let server = Task.detached {
            let client = accept(listener, nil, nil); guard client >= 0 else { return }
            defer { close(client) }
            var buffer = [UInt8](repeating: 0, count: 4096); _ = read(client, &buffer, buffer.count)
            let body = Data("{\"message\":\"你好\"}".utf8)
            let header = Data("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: \(body.count)\r\nConnection: close\r\n\r\n".utf8)
            let payload = header + body
            payload.withUnsafeBytes { bytes in _ = write(client, bytes.baseAddress, bytes.count) }
        }
        let request = try NetworkServices.request(method: "GET", url: "http://127.0.0.1:\(port)/echo", headers: "", body: "")
        let response = try await NetworkServices.send(request)
        await server.value
        XCTAssertEqual(response.status, 200); XCTAssertTrue(response.body.contains("你好"))
        XCTAssertEqual(response.bytes, Data(response.body.utf8).count)
        XCTAssertTrue(response.headers.lowercased().contains("application/json"))
    }
    func testWorkspaceRoundTripAndProductIsolation() throws {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: dir) }
        let repository = WorkspaceRepository(directory: dir)
        XCTAssertEqual(try repository.load(), WorkspaceSnapshot())
        var state = WorkspaceSnapshot(); state.documents.append(SavedDocument(toolID: "quickNote", title: "中文", content: "hello"))
        try repository.save(state); XCTAssertEqual(try repository.load(), state)
        let permissions = try FileManager.default.attributesOfItem(atPath: repository.file.path)[.posixPermissions] as? Int
        XCTAssertEqual(permissions, 0o600)
        var foreign = state; foreign.product = "next-electron"
        XCTAssertThrowsError(try repository.save(foreign)); XCTAssertEqual(try repository.load(), state)
        foreign = state; foreign.schema = 2
        XCTAssertThrowsError(try WorkspaceRepository.decode(WorkspaceRepository.encode(foreign)))
        var duplicate = state; duplicate.documents.append(state.documents[0])
        XCTAssertThrowsError(try repository.save(duplicate))
        try Data("broken".utf8).write(to: repository.file)
        XCTAssertThrowsError(try repository.load())
        XCTAssertEqual(try String(contentsOf: repository.file, encoding: .utf8), "broken")
    }
    func testProcessCapturesOutputAndTimeout() async throws {
        let text = try await ProcessRunner.run(executable: "/usr/bin/printf", arguments: ["hello %s", "MooTool"])
        XCTAssertTrue(text.contains("hello MooTool")); XCTAssertTrue(text.contains("退出码：0"))
        let cwd = try await ProcessRunner.run(executable: "/bin/pwd", arguments: [])
        XCTAssertTrue(cwd.contains(Product.id + "-process-"))
        let argument = "$(echo must-not-run); literal"
        let literal = try await ProcessRunner.run(executable: "/usr/bin/printf", arguments: ["%s", argument])
        XCTAssertTrue(literal.contains(argument))
        do { _ = try await ProcessRunner.run(executable: "/bin/sleep", arguments: ["5"], timeout: 0.1); XCTFail("Expected timeout") }
        catch { XCTAssertTrue(error.localizedDescription.contains("已停止")) }
    }
}

/// Loopback-only fixture; bounded accept/read operations keep a failing test from hanging.
private struct HTTPFixture: @unchecked Sendable {
    let listener: Int32
    let url: String
    init() throws {
        let descriptor = socket(AF_INET, SOCK_STREAM, 0)
        guard descriptor >= 0 else { throw ToolError("Cannot create fixture socket") }
        var address = sockaddr_in(); address.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        address.sin_family = sa_family_t(AF_INET); address.sin_addr.s_addr = inet_addr("127.0.0.1")
        let bound = withUnsafePointer(to: &address) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { bind(descriptor, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) } }
        guard bound == 0, listen(descriptor, 4) == 0 else { close(descriptor); throw ToolError("Cannot bind fixture") }
        var size = socklen_t(MemoryLayout<sockaddr_in>.size)
        _ = withUnsafeMutablePointer(to: &address) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { getsockname(descriptor, $0, &size) } }
        listener = descriptor; url = "http://127.0.0.1:\(UInt16(bigEndian: address.sin_port))"
    }
    func serve(count: Int) throws -> [String] {
        var requests: [String] = []
        for _ in 0..<count {
            var descriptor = pollfd(fd: listener, events: Int16(POLLIN), revents: 0)
            guard poll(&descriptor, 1, 5000) > 0 else { throw ToolError("Fixture accept timed out") }
            let client = accept(listener, nil, nil); guard client >= 0 else { throw ToolError("Fixture accept failed") }
            defer { close(client) }
            var timeout = timeval(tv_sec: 3, tv_usec: 0); var noSignal: Int32 = 1
            setsockopt(client, SOL_SOCKET, SO_RCVTIMEO, &timeout, socklen_t(MemoryLayout<timeval>.size))
            setsockopt(client, SOL_SOCKET, SO_NOSIGPIPE, &noSignal, socklen_t(MemoryLayout<Int32>.size))
            var data = Data(), buffer = [UInt8](repeating: 0, count: 4096)
            while data.count < 1024 * 1024 {
                let received = read(client, &buffer, buffer.count)
                guard received > 0 else { throw ToolError("Fixture read failed") }
                data.append(contentsOf: buffer.prefix(received))
                if let separator = data.range(of: Data("\r\n\r\n".utf8)) {
                    let headers = String(decoding: data[..<separator.lowerBound], as: UTF8.self)
                    let length = headers.components(separatedBy: "\r\n").first { $0.lowercased().hasPrefix("content-length:") }.flatMap { Int($0.dropFirst(15).trimmingCharacters(in: .whitespaces)) } ?? 0
                    if data.count - separator.upperBound >= length { break }
                }
            }
            let request = String(decoding: data, as: UTF8.self); requests.append(request)
            let redirect = request.hasPrefix("GET /redirect ")
            let body = redirect ? Data() : data
            let header = "HTTP/1.1 \(redirect ? "302 Found" : "200 OK")\r\nContent-Type: text/plain\r\nContent-Length: \(body.count)\r\nConnection: close\r\n" + (redirect ? "Location: /echo\r\nSet-Cookie: session=native; Path=/; HttpOnly\r\n" : "") + "\r\n"
            let payload = Data(header.utf8) + body
            try payload.withUnsafeBytes { bytes in
                var sent = 0
                while sent < bytes.count { let n = write(client, bytes.baseAddress!.advanced(by: sent), bytes.count - sent); guard n > 0 else { throw ToolError("Fixture write failed") }; sent += n }
            }
        }
        return requests
    }
}
