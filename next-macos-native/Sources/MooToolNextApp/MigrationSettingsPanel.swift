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

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }

    var body: some View {
        Form {
            Text(loc("migration.intro"))
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
                LabeledContent(loc("migration.preview.customGroups"), value: "\(preview.customGroupCount)")
                LabeledContent(loc("migration.preview.hiddenTools"), value: "\(preview.hiddenToolCount)")
                LabeledContent(loc("migration.preview.paneKeys"), value: "\(preview.paneKeyCount)")
                ForEach(preview.warnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
            }
            if let vaultFolderPreview {
                LabeledContent(loc("migration.preview.diskQuickNotes"), value: "\(vaultFolderPreview.quickNoteFileCount)")
                LabeledContent(loc("migration.preview.diskJson"), value: "\(vaultFolderPreview.jsonFileCount)")
                LabeledContent(loc("migration.preview.diskImages"), value: "\(vaultFolderPreview.attachmentFileCount)")
                ForEach(vaultFolderPreview.warnings, id: \.self) { warning in Text("· \(warning)").font(.caption).foregroundStyle(.secondary) }
            }
            Divider()
            Section(AppLocalization.string("migration.section.java", language: language)) {
                let root = LegacyJavaDataPaths.defaultDirectory
                LabeledContent(loc("migration.java.defaultDir"), value: root.path)
                if LegacyJavaDataPaths.hasLegacyInstall(at: root) {
                    Text(loc("migration.java.hintFound")).font(.caption).foregroundStyle(.secondary)
                } else {
                    Text(loc("migration.java.hintMissing")).font(.caption).foregroundStyle(.secondary)
                }
                HStack {
                    Button(AppLocalization.string("migration.scanJavaVaultDisk", language: language)) { scanJavaVaultFolders() }
                    Button(AppLocalization.string("migration.importJavaVaultDisk", language: language)) { confirmJavaVaultFolderImport = true }
                        .disabled(busy || javaVaultImportableCount == 0)
                }
                if let javaVaultFolderPreview {
                    LabeledContent(loc("migration.preview.javaQuickNotes"), value: "\(javaVaultFolderPreview.quickNoteFileCount)")
                    LabeledContent(loc("migration.preview.javaJson"), value: "\(javaVaultFolderPreview.jsonFileCount)")
                    LabeledContent(loc("migration.preview.javaImages"), value: "\(javaVaultFolderPreview.attachmentFileCount)")
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
                Text(loc("migration.sqlite.hint")).font(.caption).foregroundStyle(.secondary)
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
                    LabeledContent(loc("migration.count.requests"), value: "\(httpPreview.requestCount)")
                    LabeledContent(loc("migration.count.httpHistory"), value: "\(httpPreview.historyCount)")
                    LabeledContent(loc("migration.count.host"), value: "\(hostProfileCount)")
                    LabeledContent(loc("migration.count.translationWords"), value: "\(translationWordCount)")
                    LabeledContent(loc("migration.count.translationHistory"), value: "\(translationHistoryCount)")
                    LabeledContent(loc("migration.count.favorites"), value: "\(favoriteColorCount)/\(favoriteRegexCount)/\(favoriteCronCount)")
                    LabeledContent(loc("migration.count.funcHistory"), value: "\(funcHistoryCount)")
                    LabeledContent(loc("migration.count.drafts"), value: "\(funcContentCount)")
                    LabeledContent(loc("migration.count.quickNotes"), value: "\(vaultQuickNoteCount)")
                    LabeledContent(loc("migration.count.jsonDocs"), value: "\(vaultJsonCount)")
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
        .confirmationDialog(loc("migration.confirm.settings.title"), isPresented: $confirmImport) {
            Button(loc("migration.mergeIntoWorkspace")) { importSettings() }
        } message: {
            Text(loc("migration.confirm.settings.message"))
        }
        .confirmationDialog(loc("migration.confirm.http.title"), isPresented: $confirmHttpImport) {
            Button(loc("migration.mergeIntoWorkspace")) { importHttp(from: URL(fileURLWithPath: httpDatabasePath)) }
        } message: {
            Text(loc("migration.confirm.http.message"))
        }
        .confirmationDialog(loc("migration.confirm.electronVault.title"), isPresented: $confirmVaultFolderImport) {
            Button(loc("migration.mergeIntoWorkspace")) { importVaultFolders() }
        } message: {
            Text(loc("migration.confirm.electronVault.message"))
        }
        .confirmationDialog(loc("migration.confirm.javaVault.title"), isPresented: $confirmJavaVaultFolderImport) {
            Button(loc("migration.mergeIntoWorkspace")) { importJavaVaultFolders() }
        } message: {
            Text(loc("migration.confirm.javaVault.message"))
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
        panel.prompt = loc("migration.panel.promptChoose")
        if !httpDatabasePath.isEmpty { panel.directoryURL = URL(fileURLWithPath: httpDatabasePath).deletingLastPathComponent() }
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            httpDatabasePath = url.path
            httpPreview = nil
        }
    }

    private func httpCollection(for url: URL) -> String {
        url.path.contains(".MooTool") ? loc("migration.collection.java") : ElectronHttpImport.defaultCollection
    }

    private func scanHttp() {
        busy = true
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: httpDatabasePath.trimmingCharacters(in: .whitespacesAndNewlines))
            httpPreview = try ElectronHttpImport.preview(at: url, collection: httpCollection(for: url), language: language)
            hostProfileCount = try ElectronHostImport.preview(at: url, language: language).profileCount
            let translation = try ElectronTranslationImport.preview(at: url, language: language)
            translationWordCount = translation.wordCount
            translationHistoryCount = translation.historyCount
            let favorites = try ElectronFavoriteImport.preview(at: url, language: language)
            favoriteColorCount = favorites.colorCount
            favoriteRegexCount = favorites.regexCount
            favoriteCronCount = favorites.cronCount
            funcHistoryCount = try ElectronFuncHistoryImport.preview(at: url, language: language).count
            funcContentCount = try ElectronFuncContentImport.preview(at: url, language: language).count
            let vaultPreview = try ElectronVaultSqliteImport.preview(at: url, language: language)
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
            let items = try ElectronHttpImport.loadRequests(at: url, collection: collection, language: language)
            let result = ElectronHttpImport.merge(importing: items, into: store.httpRequests)
            store.httpRequests = result.merged
            let historyItems = try ElectronHttpImport.loadHttpHistory(at: url, language: language)
            let historyResult = ElectronHttpImport.mergeHistory(importing: historyItems, into: store.history)
            var mergedHistory = historyResult.merged
            let hostItems = try ElectronHostImport.loadProfiles(at: url, language: language)
            let hostResult = ElectronHostImport.merge(importing: hostItems, into: store.hostProfiles)
            store.hostProfiles = hostResult.merged
            let translationWords = try ElectronTranslationImport.loadWords(at: url, language: language)
            let wordResult = ElectronTranslationImport.mergeWords(importing: translationWords, into: store.translationWords)
            store.translationWords = wordResult.merged
            let translationHistory = try ElectronTranslationImport.loadHistory(at: url, language: language)
            let translationHistoryResult = ElectronHttpImport.mergeHistory(importing: translationHistory, into: mergedHistory)
            mergedHistory = translationHistoryResult.merged
            let funcHistory = try ElectronFuncHistoryImport.load(at: url, language: language)
            let funcHistoryResult = ElectronFuncHistoryImport.merge(importing: funcHistory, into: mergedHistory)
            mergedHistory = funcHistoryResult.merged
            let contentRows = try ElectronFuncContentImport.load(at: url, language: language)
            let draftsBeforeImport = store.snapshot().drafts
            let draftApply = LegacyToolDraftApplier.apply(rows: contentRows, merging: draftsBeforeImport, existingHistory: mergedHistory)
            mergedHistory = draftApply.history
            for (toolID, record) in draftApply.drafts where draftsBeforeImport[toolID] != record {
                store.draft(toolID).apply(record)
            }
            let favorites = mergedHistory.filter(\.favorite)
            let ordinary = Array(mergedHistory.filter { !$0.favorite }.prefix(100))
            store.history = (favorites + ordinary).sorted { $0.date > $1.date }
            let favoriteItems = try ElectronFavoriteImport.loadFavorites(at: url, language: language)
            let favoriteResult = ElectronFavoriteImport.merge(importing: favoriteItems, into: store.toolFavorites)
            store.toolFavorites = favoriteResult.merged
            var vaultNoteAdded = 0, vaultJsonAdded = 0
            let noteRows = ElectronVaultSqliteImport.filterNewQuickNotes(
                try ElectronVaultSqliteImport.loadQuickNotes(at: url, language: language),
                existing: store.documents)
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
            let jsonRows = ElectronVaultSqliteImport.filterNewJson(
                try ElectronVaultSqliteImport.loadJsonDocuments(at: url, language: language),
                existing: store.documents)
            if !jsonRows.isEmpty {
                vaultJsonAdded = try store.importVaultDocuments(ElectronVaultSqliteImport.documentImportItems(from: jsonRows), toolID: "json", parent: nil).count
            }
            store.saveNow()
            NotificationCenter.default.post(name: .menuBarTrayRefresh, object: nil)
            notice = locf(
                "migration.notice.sqliteImport",
                result.added, historyResult.added, funcHistoryResult.added, draftApply.applied,
                vaultNoteAdded, vaultJsonAdded, hostResult.added, wordResult.added,
                translationHistoryResult.added, favoriteResult.added
            )
            httpPreview = try ElectronHttpImport.preview(at: url, collection: collection, language: language)
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
        panel.prompt = loc("migration.panel.promptChoose")
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
            preview = try ElectronStoreImport.preview(
                at: url, knownToolIDs: knownToolIDs, currentPaneSizes: store.layoutPaneSizes, language: language)
            let roots = try ElectronStoreImport.vaultDirectoryRoots(at: url, language: language)
            vaultFolderPreview = try ElectronVaultFolderImport.preview(quickNoteRoot: roots.quickNote, jsonRoot: roots.json, language: language)
        } catch {
            preview = nil
            vaultFolderPreview = nil
            self.error = error.localizedDescription
        }
    }

    private func scanJavaVaultFolders() {
        do {
            let roots = try LegacyJavaVaultPaths.vaultDirectoryRoots()
            javaVaultFolderPreview = try ElectronVaultFolderImport.preview(quickNoteRoot: roots.quickNote, jsonRoot: roots.json, language: language)
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
            let roots = try ElectronStoreImport.vaultDirectoryRoots(at: url, language: language)
            try performVaultFolderImport(quickNoteRoot: roots.quickNote, jsonRoot: roots.json, label: "Electron")
            vaultFolderPreview = try ElectronVaultFolderImport.preview(quickNoteRoot: roots.quickNote, jsonRoot: roots.json, language: language)
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
        notice = locf("migration.notice.vaultImport", label, vaultNoteAdded, vaultJsonAdded, attachmentPayloads.count)
        error = nil
    }

    private func importSettings() {
        busy = true
        error = nil
        notice = nil
        defer { busy = false }
        do {
            let url = URL(fileURLWithPath: storePath.trimmingCharacters(in: .whitespacesAndNewlines))
            let patch = try ElectronStoreImport.loadPatch(
                at: url, knownToolIDs: knownToolIDs, merging: store.layoutPaneSizes, language: language)
            var snapshot = store.snapshot()
            try ElectronStoreImport.apply(patch, to: &snapshot, language: language)
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
            notice = loc("migration.notice.settingsMerged")
            preview = try ElectronStoreImport.preview(
                at: url, knownToolIDs: knownToolIDs, currentPaneSizes: store.layoutPaneSizes, language: language)
        } catch {
            self.error = error.localizedDescription
        }
    }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double { min(max(self, range.lowerBound), range.upperBound) }
}
