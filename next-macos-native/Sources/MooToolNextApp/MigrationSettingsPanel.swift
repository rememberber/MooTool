import SwiftUI
import UniformTypeIdentifiers
import MooToolNextCore

struct MigrationSettingsPanel: View {
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @AppStorage("appearance", store: nativeDefaults) private var appearance = "system"
    @AppStorage("editorSize", store: nativeDefaults) private var editorSize = 13.0
    @AppStorage("wrapLines", store: nativeDefaults) private var wrapLines = true
    @AppStorage("network.proxyEnabled", store: nativeDefaults) private var proxyEnabled = false
    @AppStorage("network.proxyHost", store: nativeDefaults) private var proxyHost = ""
    @AppStorage("network.proxyPort", store: nativeDefaults) private var proxyPort = 8080
    @AppStorage("network.proxyUsername", store: nativeDefaults) private var proxyUsername = ""
    @AppStorage("network.proxyPassword", store: nativeDefaults) private var proxyPassword = ""
    @AppStorage("vaultAutoCommit", store: nativeDefaults) private var vaultAutoCommit = true
    @AppStorage("vaultAutoCommitIdleSeconds", store: nativeDefaults) private var vaultAutoCommitIdleSeconds = 30
    @AppStorage("vaultAutoCommitInactiveSeconds", store: nativeDefaults) private var vaultAutoCommitInactiveSeconds = 120
    @AppStorage("vaultAutoPullMinutes", store: nativeDefaults) private var vaultAutoPullMinutes = 0
    @AppStorage("general.trayEnabled", store: nativeDefaults) private var trayEnabled = true
    @AppStorage("general.closeBehavior", store: nativeDefaults) private var closeBehavior = "ask"
    @AppStorage("general.autoDownloadUpdates", store: nativeDefaults) private var autoDownloadUpdates = true
    @AppStorage("general.autoCheckUpdates", store: nativeDefaults) private var autoCheckUpdates = true
    @AppStorage("general.language", store: nativeDefaults) private var languageCode = "zh-CN"
    @State private var storePath = ElectronStoreImport.defaultStoreURL()?.path ?? ""
    @State private var preview: ElectronImportPreview?
    @State private var busy = false
    @State private var error: String?
    @State private var notice: String?
    @State private var confirmImport = false
    @State private var httpDatabasePath = ElectronDataPaths.defaultDatabaseURL.path
    @State private var httpPreview: ElectronHttpImportPreview?
    @State private var hostProfileCount = 0
    @State private var translationWordCount = 0
    @State private var translationHistoryCount = 0
    @State private var favoriteColorCount = 0
    @State private var favoriteRegexCount = 0
    @State private var favoriteCronCount = 0
    @State private var funcHistoryCount = 0
    @State private var funcContentCount = 0
    @State private var vaultQuickNoteCount = 0
    @State private var vaultJsonCount = 0
    @State private var vaultImportWarnings: [String] = []
    @State private var vaultFolderPreview: ElectronVaultFolderImportPreview?
    @State private var confirmHttpImport = false
    @State private var confirmVaultFolderImport = false
    @State private var javaVaultFolderPreview: ElectronVaultFolderImportPreview?
    @State private var confirmJavaVaultFolderImport = false
    private var knownToolIDs: Set<String> { Set(Catalog.tools.map(\.id)) }
    private var importableSqliteCount: Int {
        (httpPreview?.requestCount ?? 0) + (httpPreview?.historyCount ?? 0) + hostProfileCount + translationWordCount + translationHistoryCount + favoriteColorCount + favoriteRegexCount + favoriteCronCount + funcHistoryCount + funcContentCount + vaultQuickNoteCount + vaultJsonCount
    }

    var body: some View {
        Form {
            Text("从 next Electron 的 `mootool-next.json` 合并工作台布局、HTTP 代理、编辑器与文档库 Git 设置；并可从 Electron 磁盘目录 `quick-notes` / `json-vault` 导入文档与 `attachments/` 图片。SQLite 可合并 HTTP/Host/翻译/历史/草稿及表内文档正文；不含加密密钥。")
                .font(.caption).foregroundStyle(.secondary).padding(.vertical, 4)
            HStack {
                TextField(AppLocalization.string("migration.electronStorePath", language: language), text: $storePath)
                Button(AppLocalization.string("migration.choose", language: language)) { chooseFile() }
            }
            HStack {
                Button(AppLocalization.string("migration.scan", language: language)) { scan() }.disabled(busy || storePath.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Button(AppLocalization.string("migration.importLayout", language: language)) { confirmImport = true }
                    .disabled(busy || preview == nil)
                Button(AppLocalization.string("migration.importVaultDisk", language: language)) { confirmVaultFolderImport = true }
                    .disabled(busy || vaultFolderPreview == nil || vaultFolderImportableCount == 0)
            }
            if busy { ProgressView().controlSize(.small) }
            if let error { Text(error).foregroundStyle(.red).font(.caption) }
            if let notice { Text(notice).foregroundStyle(.secondary).font(.caption) }
            if let preview {
                LabeledContent("自定义分组", value: "\(preview.customGroupCount)")
                LabeledContent("隐藏侧栏工具", value: "\(preview.hiddenToolCount)")
                LabeledContent("分栏键", value: "\(preview.paneKeyCount)")
                ForEach(preview.warnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
            }
            if let vaultFolderPreview {
                LabeledContent("磁盘随手记", value: "\(vaultFolderPreview.quickNoteFileCount)")
                LabeledContent("磁盘 JSON", value: "\(vaultFolderPreview.jsonFileCount)")
                LabeledContent("磁盘图片", value: "\(vaultFolderPreview.attachmentFileCount)")
                ForEach(vaultFolderPreview.warnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
            }
            Divider()
            Section(AppLocalization.string("migration.section.java", language: language)) {
                let root = LegacyJavaDataPaths.defaultDirectory
                LabeledContent("默认目录", value: root.path)
                if LegacyJavaDataPaths.hasLegacyInstall(at: root) {
                    Text("可从 `quick-notes` / `json-beauty` 磁盘目录或 SQLite 合并数据；完整遗留迁移服务仍以 Compose/Electron 为准。").font(.caption).foregroundStyle(.secondary)
                } else {
                    Text("未在默认目录找到 Java 版数据库。若数据在其他位置，请用 Compose 迁移或手动复制文件到文档库。").font(.caption).foregroundStyle(.secondary)
                }
                HStack {
                    Button(AppLocalization.string("migration.scanJavaVaultDisk", language: language)) { scanJavaVaultFolders() }
                    Button(AppLocalization.string("migration.importJavaVaultDisk", language: language)) { confirmJavaVaultFolderImport = true }
                        .disabled(busy || javaVaultImportableCount == 0)
                }
                if let javaVaultFolderPreview {
                    LabeledContent("Java 磁盘随手记", value: "\(javaVaultFolderPreview.quickNoteFileCount)")
                    LabeledContent("Java 磁盘 JSON", value: "\(javaVaultFolderPreview.jsonFileCount)")
                    LabeledContent("Java 磁盘图片", value: "\(javaVaultFolderPreview.attachmentFileCount)")
                    ForEach(javaVaultFolderPreview.warnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
                }
                Button(AppLocalization.string("migration.openFinder", language: language)) { NSWorkspace.shared.open(root) }
                if let javaDB = LegacyJavaDataPaths.legacyDatabaseURL(in: root) {
                    Divider()
                    Button(AppLocalization.string("migration.importJavaHttp", language: language)) { importHttp(from: javaDB) }
                }
            }
            Divider()
            Section(AppLocalization.string("migration.section.sqlite", language: language)) {
                Text("从 Electron/Java SQLite 合并 HTTP、Host、翻译、历史、草稿与文档库正文（`t_quick_note` / `t_json_beauty`）；重复标题+正文会跳过。").font(.caption).foregroundStyle(.secondary)
                HStack {
                    TextField(AppLocalization.string("migration.sqlitePath", language: language), text: $httpDatabasePath)
                    Button(AppLocalization.string("migration.choose", language: language)) { chooseDatabase() }
                }
                HStack {
                    Button(AppLocalization.string("migration.scan", language: language)) { scanHttp() }.disabled(busy || httpDatabasePath.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Button(AppLocalization.string("migration.importMerge", language: language)) { confirmHttpImport = true }
                        .disabled(busy || importableSqliteCount == 0)
                }
                if let httpPreview {
                    LabeledContent("可导入请求", value: "\(httpPreview.requestCount)")
                    LabeledContent("可导入历史", value: "\(httpPreview.historyCount)")
                    LabeledContent("可导入 Host", value: "\(hostProfileCount)")
                    LabeledContent("可导入翻译词条", value: "\(translationWordCount)")
                    LabeledContent("可导入翻译历史", value: "\(translationHistoryCount)")
                    LabeledContent("可导入颜色/正则/Cron 收藏", value: "\(favoriteColorCount)/\(favoriteRegexCount)/\(favoriteCronCount)")
                    LabeledContent("可导入通用工具历史", value: "\(funcHistoryCount)")
                    LabeledContent("可导入工具草稿", value: "\(funcContentCount)")
                    LabeledContent("可导入随手记文档", value: "\(vaultQuickNoteCount)")
                    LabeledContent("可导入 JSON 文档", value: "\(vaultJsonCount)")
                    ForEach(httpPreview.warnings + vaultImportWarnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
                }
            }
        }
        .formStyle(.grouped).frame(width: 560, height: 600)
        .onAppear {
            if storePath.isEmpty { storePath = ElectronStoreImport.defaultStoreURL()?.path ?? "" }
            if httpDatabasePath.isEmpty || !FileManager.default.fileExists(atPath: httpDatabasePath) {
                httpDatabasePath = ElectronDataPaths.defaultDatabaseURL.path
            }
            scanJavaVaultFolders()
        }
        .confirmationDialog("导入 Electron 设置？", isPresented: $confirmImport) {
            Button(AppLocalization.string("migration.mergeIntoWorkspace", language: language)) { importSettings() }
        } message: {
            Text("将覆盖匹配的工作台字段，并写入 HTTP 代理与编辑器偏好。导入前会自动保存当前工作区。")
        }
        .confirmationDialog("导入 HTTP 请求集合？", isPresented: $confirmHttpImport) {
            Button(AppLocalization.string("migration.mergeIntoWorkspace", language: language)) { importHttp(from: URL(fileURLWithPath: httpDatabasePath)) }
        } message: {
            Text("将合并 HTTP 集合、各工具历史（含 t_func_history）与 Host/翻译/收藏；重复项会跳过。")
        }
        .confirmationDialog("导入 Electron 磁盘文档库？", isPresented: $confirmVaultFolderImport) {
            Button(AppLocalization.string("migration.mergeIntoWorkspace", language: language)) { importVaultFolders() }
        } message: {
            Text("从 Electron 的 quick-notes 与 json-vault 目录复制文本文件；随手记正文中的 attachments/ 图片会转为原生附件。重复标题+正文会跳过。")
        }
        .confirmationDialog("导入 Java 磁盘文档库？", isPresented: $confirmJavaVaultFolderImport) {
            Button(AppLocalization.string("migration.mergeIntoWorkspace", language: language)) { importJavaVaultFolders() }
        } message: {
            Text("从 Java 版 `quick-notes` 与 `json-beauty` 目录合并文本与 attachments/ 图片；重复标题+正文会跳过。")
        }
    }

    private var vaultFolderImportableCount: Int {
        (vaultFolderPreview?.quickNoteFileCount ?? 0) + (vaultFolderPreview?.jsonFileCount ?? 0)
    }

    private var javaVaultImportableCount: Int {
        (javaVaultFolderPreview?.quickNoteFileCount ?? 0) + (javaVaultFolderPreview?.jsonFileCount ?? 0)
    }

    private func chooseDatabase() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.allowedContentTypes = [UTType(filenameExtension: "db") ?? .data, .database]
        panel.prompt = "选择"
        if !httpDatabasePath.isEmpty { panel.directoryURL = URL(fileURLWithPath: httpDatabasePath).deletingLastPathComponent() }
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            httpDatabasePath = url.path
            httpPreview = nil
        }
    }

    private func httpCollection(for url: URL) -> String {
        url.path.contains(".MooTool") ? "Java 导入" : ElectronHttpImport.defaultCollection
    }

    private func scanHttp() {
        busy = true
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: httpDatabasePath.trimmingCharacters(in: .whitespacesAndNewlines))
            httpPreview = try ElectronHttpImport.preview(at: url, collection: httpCollection(for: url))
            hostProfileCount = try ElectronHostImport.preview(at: url).profileCount
            let translation = try ElectronTranslationImport.preview(at: url)
            translationWordCount = translation.wordCount
            translationHistoryCount = translation.historyCount
            let favorites = try ElectronFavoriteImport.preview(at: url)
            favoriteColorCount = favorites.colorCount
            favoriteRegexCount = favorites.regexCount
            favoriteCronCount = favorites.cronCount
            funcHistoryCount = try ElectronFuncHistoryImport.preview(at: url).count
            funcContentCount = try ElectronFuncContentImport.preview(at: url).count
            let vaultPreview = try ElectronVaultSqliteImport.preview(at: url)
            vaultQuickNoteCount = vaultPreview.quickNoteCount
            vaultJsonCount = vaultPreview.jsonCount
            vaultImportWarnings = vaultPreview.warnings
            error = nil
        } catch {
            httpPreview = nil
            hostProfileCount = 0
            translationWordCount = 0
            translationHistoryCount = 0
            favoriteColorCount = 0
            favoriteRegexCount = 0
            favoriteCronCount = 0
            funcHistoryCount = 0
            funcContentCount = 0
            vaultQuickNoteCount = 0
            vaultJsonCount = 0
            vaultImportWarnings = []
            self.error = error.localizedDescription
        }
    }

    private func importHttp(from url: URL) {
        busy = true
        defer { busy = false }
        do {
            let collection = httpCollection(for: url)
            let items = try ElectronHttpImport.loadRequests(at: url, collection: collection)
            let result = ElectronHttpImport.merge(importing: items, into: store.httpRequests)
            store.httpRequests = result.merged
            let historyItems = try ElectronHttpImport.loadHttpHistory(at: url)
            let historyResult = ElectronHttpImport.mergeHistory(importing: historyItems, into: store.history)
            var mergedHistory = historyResult.merged
            let hostItems = try ElectronHostImport.loadProfiles(at: url)
            let hostResult = ElectronHostImport.merge(importing: hostItems, into: store.hostProfiles)
            store.hostProfiles = hostResult.merged
            let translationWords = try ElectronTranslationImport.loadWords(at: url)
            let wordResult = ElectronTranslationImport.mergeWords(importing: translationWords, into: store.translationWords)
            store.translationWords = wordResult.merged
            let translationHistory = try ElectronTranslationImport.loadHistory(at: url)
            let translationHistoryResult = ElectronHttpImport.mergeHistory(importing: translationHistory, into: mergedHistory)
            mergedHistory = translationHistoryResult.merged
            let funcHistory = try ElectronFuncHistoryImport.load(at: url)
            let funcHistoryResult = ElectronFuncHistoryImport.merge(importing: funcHistory, into: mergedHistory)
            mergedHistory = funcHistoryResult.merged
            let contentRows = try ElectronFuncContentImport.load(at: url)
            let draftsBeforeImport = store.snapshot().drafts
            let draftApply = LegacyToolDraftApplier.apply(rows: contentRows, merging: draftsBeforeImport, existingHistory: mergedHistory)
            mergedHistory = draftApply.history
            for (toolID, record) in draftApply.drafts where draftsBeforeImport[toolID] != record {
                store.draft(toolID).apply(record)
            }
            let favorites = mergedHistory.filter(\.favorite)
            let ordinary = Array(mergedHistory.filter { !$0.favorite }.prefix(100))
            store.history = (favorites + ordinary).sorted { $0.date > $1.date }
            let favoriteItems = try ElectronFavoriteImport.loadFavorites(at: url)
            let favoriteResult = ElectronFavoriteImport.merge(importing: favoriteItems, into: store.toolFavorites)
            store.toolFavorites = favoriteResult.merged
            var vaultNoteAdded = 0, vaultJsonAdded = 0
            let noteRows = ElectronVaultSqliteImport.filterNewQuickNotes(try ElectronVaultSqliteImport.loadQuickNotes(at: url), existing: store.documents)
            if !noteRows.isEmpty {
                let ids = try store.importVaultDocuments(ElectronVaultSqliteImport.documentImportItems(from: noteRows), toolID: "quickNote", parent: nil)
                vaultNoteAdded = ids.count
                for (id, row) in zip(ids, noteRows) {
                    if let index = store.documents.firstIndex(where: { $0.id == id }) {
                        store.documents[index].noteOptions = row.options
                        if let modified = row.modified { store.documents[index].modified = modified }
                    }
                }
            }
            let jsonRows = ElectronVaultSqliteImport.filterNewJson(try ElectronVaultSqliteImport.loadJsonDocuments(at: url), existing: store.documents)
            if !jsonRows.isEmpty {
                vaultJsonAdded = try store.importVaultDocuments(ElectronVaultSqliteImport.documentImportItems(from: jsonRows), toolID: "json", parent: nil).count
            }
            store.saveNow()
            NotificationCenter.default.post(name: .menuBarTrayRefresh, object: nil)
            notice = "请求 +\(result.added)；HTTP 历史 +\(historyResult.added)；工具历史 +\(funcHistoryResult.added)；草稿回写 \(draftApply.applied)；随手记 +\(vaultNoteAdded)；JSON +\(vaultJsonAdded)；Host +\(hostResult.added)；词条 +\(wordResult.added)；翻译历史 +\(translationHistoryResult.added)；收藏 +\(favoriteResult.added)。"
            httpPreview = try ElectronHttpImport.preview(at: url, collection: collection)
            error = nil
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func chooseFile() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.allowedContentTypes = [.json]
        panel.prompt = "选择"
        if !storePath.isEmpty { panel.directoryURL = URL(fileURLWithPath: storePath).deletingLastPathComponent() }
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            storePath = url.path
            preview = nil
            error = nil
            notice = nil
        }
    }

    private func scan() {
        busy = true
        error = nil
        notice = nil
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: storePath.trimmingCharacters(in: .whitespacesAndNewlines))
            preview = try ElectronStoreImport.preview(at: url, knownToolIDs: knownToolIDs, currentPaneSizes: store.layoutPaneSizes)
            let roots = try ElectronStoreImport.vaultDirectoryRoots(at: url)
            vaultFolderPreview = try ElectronVaultFolderImport.preview(quickNoteRoot: roots.quickNote, jsonRoot: roots.json)
        } catch {
            preview = nil
            vaultFolderPreview = nil
            self.error = error.localizedDescription
        }
    }

    private func scanJavaVaultFolders() {
        do {
            let roots = try LegacyJavaVaultPaths.vaultDirectoryRoots()
            javaVaultFolderPreview = try ElectronVaultFolderImport.preview(quickNoteRoot: roots.quickNote, jsonRoot: roots.json)
        } catch {
            javaVaultFolderPreview = nil
        }
    }

    private func importJavaVaultFolders() {
        busy = true
        defer { busy = false }
        do {
            let roots = try LegacyJavaVaultPaths.vaultDirectoryRoots()
            try performVaultFolderImport(quickNoteRoot: roots.quickNote, jsonRoot: roots.json, label: "Java")
            scanJavaVaultFolders()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func importVaultFolders() {
        busy = true
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: storePath.trimmingCharacters(in: .whitespacesAndNewlines))
            let roots = try ElectronStoreImport.vaultDirectoryRoots(at: url)
            try performVaultFolderImport(quickNoteRoot: roots.quickNote, jsonRoot: roots.json, label: "Electron")
            vaultFolderPreview = try ElectronVaultFolderImport.preview(quickNoteRoot: roots.quickNote, jsonRoot: roots.json)
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func performVaultFolderImport(quickNoteRoot: URL, jsonRoot: URL, label: String) throws {
        let noteRows = ElectronVaultFolderImport.filterNewQuickNotes(try ElectronVaultFolderImport.loadQuickNotes(at: quickNoteRoot), existing: store.documents)
        var preparedNotes: [ElectronVaultFolderQuickNote] = []
        var attachmentPayloads: [NoteImagePayload] = []
        var manifest = store.noteAttachments
        for row in noteRows {
            let rewritten = try ElectronVaultFolderImport.rewriteElectronAttachments(in: row.content, quickNoteRoot: quickNoteRoot, existingManifest: manifest)
            manifest.append(contentsOf: rewritten.payloads.map(\.attachment))
            attachmentPayloads.append(contentsOf: rewritten.payloads)
            preparedNotes.append(ElectronVaultFolderQuickNote(relativePath: row.relativePath, content: rewritten.content, options: row.options, modified: row.modified))
        }
        try store.mergeNoteAttachmentPayloads(attachmentPayloads)
        var vaultNoteAdded = 0
        if !preparedNotes.isEmpty {
            let ids = try store.importVaultDocuments(ElectronVaultFolderImport.documentImportItems(from: preparedNotes), toolID: "quickNote", parent: nil)
            vaultNoteAdded = ids.count
            for (id, row) in zip(ids, preparedNotes) {
                if let index = store.documents.firstIndex(where: { $0.id == id }) {
                    store.documents[index].noteOptions = row.options
                    store.documents[index].content = row.content
                    if let modified = row.modified { store.documents[index].modified = modified }
                }
            }
        }
        let jsonItems = ElectronVaultFolderImport.filterNewJson(try ElectronVaultFolderImport.loadJsonItems(at: jsonRoot), existing: store.documents)
        let vaultJsonAdded = jsonItems.isEmpty ? 0 : try store.importVaultDocuments(jsonItems, toolID: "json", parent: nil).count
        store.saveNow()
        notice = "\(label) 磁盘文档库：随手记 +\(vaultNoteAdded)，JSON +\(vaultJsonAdded)，图片附件 +\(attachmentPayloads.count)。"
        error = nil
    }

    private func importSettings() {
        busy = true
        error = nil
        notice = nil
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: storePath.trimmingCharacters(in: .whitespacesAndNewlines))
            let patch = try ElectronStoreImport.loadPatch(at: url, knownToolIDs: knownToolIDs, merging: store.layoutPaneSizes)
            var snapshot = store.snapshot()
            try ElectronStoreImport.apply(patch, to: &snapshot)
            store.restore(snapshot)
            if let value = patch.proxyEnabled { proxyEnabled = value }
            if let value = patch.proxyHost { proxyHost = value }
            if let value = patch.proxyPort { proxyPort = value }
            if let value = patch.proxyUsername { proxyUsername = value }
            if let value = patch.proxyPassword { proxyPassword = value }
            if let value = patch.editorFontSize { editorSize = value.clamped(to: 11...22) }
            if let value = patch.wrapLines { wrapLines = value }
            if let value = patch.vaultAutoCommit { vaultAutoCommit = value }
            if let value = patch.vaultAutoCommitIdleSeconds { vaultAutoCommitIdleSeconds = value }
            if let value = patch.vaultAutoCommitInactiveSeconds { vaultAutoCommitInactiveSeconds = value }
            if let value = patch.vaultAutoPullMinutes { vaultAutoPullMinutes = value }
            if let value = patch.trayEnabled { trayEnabled = value }
            if let value = patch.closeBehavior { closeBehavior = value }
            if let value = patch.autoDownloadUpdates { autoDownloadUpdates = value }
            if let value = patch.autoCheckUpdates { autoCheckUpdates = value }
            if let value = patch.language { languageCode = value }
            if let value = patch.shortcutSearch { nativeDefaults.set(value, forKey: "shortcuts.search") }
            if let value = patch.shortcutSettings { nativeDefaults.set(value, forKey: "shortcuts.settings") }
            store.saveNow()
            NotificationCenter.default.post(name: .menuBarTrayRefresh, object: nil)
            notice = "已合并 Electron 工作台与网络/编辑器偏好。"
            preview = try ElectronStoreImport.preview(at: url, knownToolIDs: knownToolIDs, currentPaneSizes: store.layoutPaneSizes)
        } catch {
            self.error = error.localizedDescription
        }
    }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double { min(max(self, range.lowerBound), range.upperBound) }
}
