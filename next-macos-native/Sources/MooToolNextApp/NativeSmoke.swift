import SwiftUI
import MooToolNextCore

/// An opt-in acceptance harness that renders only this application's views in an isolated workspace.
@MainActor enum NativeSmoke {
    static func run(store: AppStore) async {
        guard let directory = ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_SCREENSHOTS"],
              let _ = ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_TEST_DATA"] else {
            fputs("Smoke testing requires isolated data and screenshot directories.\n", stderr); exit(2)
        }
        do {
            let output = URL(fileURLWithPath: directory)
            try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
            store.draft("json").input = "{\"name\":\"MooTool\",\"native\":true,\"tools\":[\"JSON\",\"HTTP\",\"随手记\"]}"
            store.draft("json").input = try await JSONEngine.execute(JSONEngineRequest("format", input: store.draft("json").input)).value ?? store.draft("json").input
            store.draft("json").output = try TextServices.json(store.draft("json").input)
            store.draft("quickNote").input = "# 今天的想法\n\n让开发与日常，得心应手。\n\n- 整理 API 文档\n- 检查 JSON 响应\n- 记录一个好想法\n\nHello, **MooTool**.\n\n| 工具 | 状态 |\n| :--- | ---: |\n| JSON | 已对齐 |\n| HTTP | 可用 |\n\n- [x] 核对布局\n- [ ] 整理笔记\n\n```swift\nlet tool = \"MooTool\"\n```"
            var request = try CurlCommand.parse("curl --location 'https://example.com/api' --json '{\"name\":\"MooTool\",\"native\":true}' -b 'theme=dark' -H 'X-Client: MooTool Native'")
            request.http?.params = [HTTPField("locale", "zh-CN"), HTTPField("limit", "20"), HTTPField("debug", "true", enabled: false)]
            request.output = "{\"data\":{\"name\":\"MooTool\",\"platform\":\"macOS\"},\"success\":true}"
            request.httpResult = HTTPResultMetadata(status: 200, headers: "Content-Type: application/json\nX-Request-Id: native-demo", cookies: "theme=dark\n  Domain: example.com · Path: /", url: request.option, elapsed: 0.128, bytes: request.output.utf8.count)
            store.draft("http").apply(request)
            store.httpRequests = [SavedHTTPRequest(name: "创建应用", collection: "开发 API", draft: request)]
            store.draft("qrCode").input = "https://mootool.luoboduner.com"
            store.documents = [SavedDocument(toolID: "json", title: "API 响应", content: store.draft("json").input), SavedDocument(toolID: "quickNote", title: "今天的想法", content: store.draft("quickNote").input)]
            store.draft("json").documentID = store.documents[0].id
            store.draft("quickNote").documentID = store.documents[1].id
            var library = store.vault
            let api = try library.createFolder(toolID: "json", name: "接口示例")
            let users = try library.createFolder(toolID: "json", name: "用户", parent: api)
            let notes = try library.createFolder(toolID: "quickNote", name: "工作笔记")
            library.documents[0].parentID = users; library.documents[1].parentID = notes
            _ = try library.createDocument(toolID: "json", name: "配置.json", content: "{\"theme\":\"system\"}", parent: api)
            _ = try library.createDocument(toolID: "quickNote", name: "发布清单.md", content: "# 发布清单\n\n- 检查功能\n- 检查布局\n- 构建原生安装包", parent: notes)
            try store.commitVault(library)
            store.updateVaultPreference("json") { $0.expanded = [api, users]; $0.selectedEntryID = store.documents[0].id }
            store.updateVaultPreference("quickNote") { $0.expanded = [notes]; $0.selectedEntryID = store.documents[1].id }
            var reports: [[String: Any]] = []
            guard let window = NSApp.windows.first(where: { $0.isVisible && $0.frame.width > 700 }), let hosting = window.contentView else { throw ToolError("找不到主窗口。") }
            window.center()
            let noteLayoutsOnly = CommandLine.arguments.contains("--note-layouts-only")
            if !noteLayoutsOnly {
                try await NativeAttachmentAcceptance.run(store: store, window: window)
                try await NativeNoteAcceptance.run(store: store, window: window)
            }
            if CommandLine.arguments.contains("--notes-only") {
                let images = try NativeAttachmentAcceptance.seed(store)
                store.draft("quickNote").input += "\n\n" + images[0].markdown
                store.saveNow()
                try WorkspaceRepository.encode(store.snapshot()).write(to: store.repository.directory.appendingPathComponent("restart-expectation.json"), options: .atomic)
                print("PASS: focused native note and attachment acceptance"); NSApp.terminate(nil); return
            }
            if !noteLayoutsOnly {
                try await NativeJSONAcceptance.run(store: store, window: window)
                try await NativeVaultAcceptance.run(store: store, window: window)
            }
            let previewAttachments = try NativeAttachmentAcceptance.seed(store)
            store.editorRestoreGeneration += 1
            store.draft("json").inputEditor = EditorViewState(); store.draft("json").editorRevision += 1
            if let index = store.documents.firstIndex(where: { $0.id == store.draft("json").documentID }) { store.documents[index].inputEditor = EditorViewState() }
            store.draft("quickNote").noteOptions = QuickNoteOptions()
            store.draft("quickNote").noteWorkspace = QuickNoteWorkspaceOptions()
            store.draft("quickNote").inputEditor = EditorViewState(); store.draft("quickNote").editorRevision += 1
            for scheme in [ColorScheme.light, .dark] {
                for tool in Catalog.tools where !noteLayoutsOnly {
                    store.selected = tool.id
                    nativeDefaults.set(scheme == .light ? "light" : "dark", forKey: "appearance")
                    window.setContentSize(NSSize(width: 1200, height: 800)); NSApp.activate(ignoringOtherApps: true); window.makeKeyAndOrderFront(nil)
                    hosting.layoutSubtreeIfNeeded(); window.displayIfNeeded()
                    try await Task.sleep(for: .milliseconds(["http", "json"].contains(tool.id) ? 1100 : 300))
                    hosting.layoutSubtreeIfNeeded()
                    guard let bitmap = hosting.bitmapImageRepForCachingDisplay(in: hosting.bounds) else { throw ToolError("无法捕获 \(tool.id)") }
                    hosting.cacheDisplay(in: hosting.bounds, to: bitmap)
                    let name = "\(tool.id)-\(scheme == .light ? "light" : "dark").png"
                    guard let data = bitmap.representation(using: .png, properties: [:]) else { throw ToolError("无法编码截图") }
                    let destination = output.appendingPathComponent(name)
                    try data.write(to: destination)
                    if CommandLine.arguments.contains("--window-capture") {
                        let capture = Process(); capture.executableURL = URL(fileURLWithPath: "/usr/sbin/screencapture")
                        capture.arguments = ["-x", "-o", "-l", String((window.attachedSheet ?? window).windowNumber), destination.path]
                        try capture.run(); capture.waitUntilExit()
                        guard capture.terminationStatus == 0 else { throw ToolError("系统窗口截图不可用。") }
                    }
                    reports.append(["tool": tool.id, "appearance": scheme == .light ? "light" : "dark", "width": bitmap.pixelsWide, "height": bitmap.pixelsHigh, "screenshot": name])
                }
            }
            for variant in ["json-tree-light", "json-tree-dark", "json-compact", "http-compact", "quickNote-split-light", "quickNote-split-dark", "quickNote-compact", "json-search", "json-inspector-light", "json-inspector-dark", "json-find-light", "json-find-dark", "quickNote-replace-light", "quickNote-replace-dark", "quickNote-preview-light", "quickNote-preview-dark", "quickNote-toolbar-light", "quickNote-toolbar-dark", "quickNote-find-compact", "quickNote-images-light", "quickNote-images-dark", "quickNote-images-compact", "quickNote-image-missing-light", "quickNote-image-missing-dark"] {
                if noteLayoutsOnly && !variant.hasPrefix("quickNote") { continue }
                let id = variant.hasPrefix("http") ? "http" : variant.hasPrefix("quickNote") ? "quickNote" : "json"
                store.selected = id
                var options = JSONOptions(); options.showsTree = variant.contains("tree"); options.indent = 4
                options.inspectorOpen = variant.contains("inspector") || variant.contains("find")
                options.findOpen = variant.contains("find"); options.findQuery = "name|tools"; options.regex = true
                store.draft("json").json = options
                store.updateVaultPreference("quickNote") { $0.noteViewMode = variant.contains("split") ? .split : variant.contains("preview") ? .preview : .editor }
                if variant.contains("image") {
                    let missing = variant.contains("missing")
                    store.draft("quickNote").input = missing ? "# 图片附件\n\n正文和引用保留，缺失图片会显示提示。\n\n![图片暂不可用](\(NativeAttachmentAcceptance.missingPath))" : "# 图片附件\n\n选图、粘贴或拖入，随笔记一起保存。\n\n\(previewAttachments[0].markdown)\n\n## 长图\n\n\(previewAttachments[1].markdown)"
                    store.updateVaultPreference("quickNote") { $0.noteViewMode = missing || variant.contains("compact") ? .preview : .split }
                    store.draft("quickNote").inputEditor = EditorViewState(); store.draft("quickNote").editorRevision += 1
                }
                var noteWorkspace = QuickNoteWorkspaceOptions(); noteWorkspace.quickReplaceOpen = variant.contains("replace")
                noteWorkspace.findOpen = variant.contains("replace") || variant == "quickNote-find-compact"; noteWorkspace.findQuery = "JSON|HTTP"; noteWorkspace.regex = true
                store.draft("quickNote").noteWorkspace = noteWorkspace
                store.updateVaultPreference("json") { $0.query = variant == "json-search" ? "MooTool" : "" }
                nativeDefaults.set(variant.hasSuffix("dark") ? "dark" : "light", forKey: "appearance")
                window.setContentSize(variant.contains("compact") ? NSSize(width: 940, height: 630) : NSSize(width: variant.hasPrefix("quickNote") ? 1600 : variant.contains("inspector") || variant.contains("find") ? 1440 : 1200, height: 800))
                NSApp.activate(ignoringOtherApps: true); window.makeKeyAndOrderFront(nil)
                hosting.layoutSubtreeIfNeeded(); window.displayIfNeeded()
                try await Task.sleep(for: .milliseconds(1000)); hosting.layoutSubtreeIfNeeded()
                if variant.contains("tree") { try await NativeJSONAcceptance.waitForPathPreview(window, source: store.draft("json").input); hosting.layoutSubtreeIfNeeded() }
                if id == "quickNote" && (variant.contains("split") || variant.contains("preview")) { try await NativeNoteAcceptance.waitForPreview(window); hosting.layoutSubtreeIfNeeded() }
                if id == "quickNote" && noteWorkspace.findOpen { try await NativeNoteAcceptance.waitForFind(window, count: 3); hosting.layoutSubtreeIfNeeded() }
                if variant.contains("image") { try await NativeAttachmentAcceptance.waitForImage(window, path: variant.contains("missing") ? NativeAttachmentAcceptance.missingPath : previewAttachments[0].path, missing: variant.contains("missing")); hosting.layoutSubtreeIfNeeded() }
                if variant == "quickNote-images-light" || variant == "quickNote-images-dark" { try NativeAttachmentAcceptance.verifySplitLayout(window) }
                guard let bitmap = hosting.bitmapImageRepForCachingDisplay(in: hosting.bounds) else { throw ToolError("无法捕获 \(variant)") }
                hosting.cacheDisplay(in: hosting.bounds, to: bitmap)
                let name = variant + ".png", destination = output.appendingPathComponent(variant + ".png")
                try bitmap.representation(using: .png, properties: [:])?.write(to: destination)
                if CommandLine.arguments.contains("--window-capture") {
                    let capture = Process(); capture.executableURL = URL(fileURLWithPath: "/usr/sbin/screencapture")
                    capture.arguments = ["-x", "-o", "-l", String((window.attachedSheet ?? window).windowNumber), destination.path]
                    try capture.run(); capture.waitUntilExit()
                    guard capture.terminationStatus == 0 else { throw ToolError("\(variant) 窗口截图不可用。") }
                }
                reports.append(["tool": id, "variant": variant, "width": bitmap.pixelsWide, "height": bitmap.pixelsHigh, "screenshot": name])
            }
            store.selected = "mootool"
            try await Task.sleep(for: .milliseconds(200))
            store.saveNow()
            guard try store.repository.load() == store.snapshot() else { throw ToolError("工作区持久化验证失败。") }
            let restored = AppStore(directory: store.repository.directory)
            guard restored.httpRequests == store.httpRequests, restored.draft("http").record == store.draft("http").record,
                  restored.draft("json").record == store.draft("json").record, restored.draft("quickNote").record == store.draft("quickNote").record else { throw ToolError("HTTP / JSON 工作区恢复验证失败。") }
            try JSONSerialization.data(withJSONObject: reports, options: [.prettyPrinted, .sortedKeys]).write(to: output.appendingPathComponent("report.json"))
            try WorkspaceRepository.encode(store.snapshot()).write(to: store.repository.directory.appendingPathComponent("restart-expectation.json"), options: .atomic)
            if noteLayoutsOnly { print("PASS: \(reports.count) note layout captures, image split widths and persistence round trip.") }
            else { print("PASS: \(reports.count) view captures, document vault interactions, JSON tree, compact layouts, extended persistence round trip.") }
            NSApp.terminate(nil)
        } catch { fputs("Smoke test failed: \(error)\n", stderr); exit(1) }
    }
    static func verifyRestart(store: AppStore) {
        guard ProcessInfo.processInfo.environment["MOOTOOL_NATIVE_TEST_DATA"] != nil else { fputs("Restart check requires isolated data.\n", stderr); exit(2) }
        do {
            let expected = try WorkspaceRepository.readSnapshot(at: store.repository.directory.appendingPathComponent("restart-expectation.json"))
            guard store.snapshot() == expected else { throw ToolError("重新启动后的工作区与保存内容不同。") }
            for attachment in store.noteAttachments { _ = try store.repository.attachmentRepository.thumbnail(attachment) }
            print("PASS: fresh application launch restores documents, folders, selections and editor state"); NSApp.terminate(nil)
        } catch { fputs("Restart check failed: \(error)\n", stderr); exit(1) }
    }
}
