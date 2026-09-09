import SwiftUI
import UniformTypeIdentifiers
import MooToolNextCore

struct DocumentsTool: View {
    let id: String
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @State private var documentPicker = false
    @State private var action: VaultAction?
    @State private var importing = false
    @State private var exporting = false
    private var preferences: VaultPreferences { store.vaultPreference(id) }
    private var selectedID: UUID? { preferences.selectedEntryID }
    private var selectedParent: UUID? {
        guard let selectedID else { return nil }
        return store.folders.contains { $0.id == selectedID } ? selectedID : store.vault.parent(of: selectedID)
    }
    private var activeDocument: SavedDocument? { store.documents.first { $0.id == draft.documentID && $0.toolID == id } }
    private func preference<Value>(_ path: WritableKeyPath<VaultPreferences, Value>) -> Binding<Value> {
        Binding(get: { preferences[keyPath: path] }, set: { value in store.updateVaultPreference(id) { $0[keyPath: path] = value } })
    }
    var body: some View {
        GeometryReader { geometry in
            HSplitView {
                if preferences.treeVisible && geometry.size.width >= 800 { library.frame(minWidth: id == "json" ? 190 : 220, idealWidth: id == "json" ? 210 : 235, maxWidth: id == "json" ? 260 : 330) }
                VStack(spacing: 0) {
                    editorToolbar(compact: geometry.size.width < 800)
                    Divider()
                    if id == "json" { JSONWorkspace(draft: draft) }
                    else { QuickNoteWorkspace(draft: draft, onSave: save, onDelete: {
                        if let documentID = draft.documentID { action = VaultAction(kind: .delete, entryID: documentID) }
                    }) }
                }.frame(minWidth: id == "json" ? 360 : 430)
            }
        }
        .popover(isPresented: $documentPicker) { library.frame(width: 270, height: 480) }
        .sheet(item: $action) { VaultActionSheet(toolID: id, action: $0).environment(store) }
    }
    private var library: some View {
        VStack(spacing: 0) {
            HStack(spacing: 7) {
                Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
                TextField(id == "json" ? "搜索 JSON 文档" : "搜索笔记", text: preference(\.query)).textFieldStyle(.plain)
                if !preferences.query.isEmpty { Button { store.updateVaultPreference(id) { $0.query = "" } } label: { Image(systemName: "xmark.circle.fill") }.buttonStyle(.borderless).help("清空搜索") }
            }.padding(8).background(.quaternary.opacity(0.3), in: RoundedRectangle(cornerRadius: 8)).padding(.horizontal, 11).padding(.top, 12)
            HStack(spacing: 6) {
                Toggle("搜索正文", isOn: preference(\.includeContent)).toggleStyle(.checkbox).fixedSize()
                Spacer(minLength: 0)
                Picker("排序", selection: preference(\.sort)) {
                    ForEach(VaultSort.allCases.filter { id == "quickNote" || $0 != .created }, id: \.self) { Text($0.title).tag($0) }
                }.labelsHidden().frame(width: 82)
                moreMenu
            }.font(.system(size: 11)).controlSize(.small).padding(.horizontal, 12).padding(.vertical, 9)
            HStack(spacing: 14) {
                iconButton("新建文档", "doc.badge.plus") { begin(.file) }
                iconButton("新建文件夹", "folder.badge.plus") { begin(.folder) }
                iconButton(allExpanded ? "全部折叠" : "全部展开", allExpanded ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right") {
                    store.updateVaultPreference(id) { $0.expanded = allExpanded ? [] : Set(store.folders.filter { $0.toolID == id }.map(\.id)) }
                }
                if id == "json" { iconButton("保存", "square.and.arrow.down", action: save); iconButton("删除", "trash") { begin(.delete) }.disabled(selectedID == nil) }
                Spacer(minLength: 0)
                if importing { ProgressView().controlSize(.mini) }
            }.buttonStyle(.borderless).padding(.horizontal, 15).padding(.bottom, 12)
            Divider()
            tree
            Divider()
            HStack {
                Text(selectedID.map { store.vault.path(of: $0) } ?? "\(store.documents.filter { $0.toolID == id }.count) 份文档").lineLimit(1).truncationMode(.middle)
                Spacer(minLength: 0)
                if draft.documentID == nil { Text("草稿").foregroundStyle(.tertiary) }
            }.font(.system(size: 10)).foregroundStyle(.secondary).padding(12).help(selectedID.map { store.vault.path(of: $0) } ?? "文档库根目录")
                .dropDestination(for: String.self) { values, _ in drop(values, into: nil) }
        }.background(Color(nsColor: .controlBackgroundColor))
    }
    private var tree: some View {
        let rows = store.vault.rows(toolID: id, preferences: preferences)
        return ScrollViewReader { proxy in
            List(selection: Binding(get: { selectedID }, set: { value in if let value { select(value) } })) {
                ForEach(rows) { row in
                    HStack(spacing: 5) {
                        if row.node.isFolder {
                            Image(systemName: preferences.expanded.contains(row.id) || !preferences.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "chevron.down" : "chevron.right").font(.system(size: 9, weight: .semibold)).frame(width: 12)
                        } else { Color.clear.frame(width: 12, height: 1) }
                        Image(systemName: row.node.isFolder ? "folder" : id == "json" ? "curlybraces" : "doc.text").foregroundStyle(row.node.isFolder ? Color.accentColor : id == "quickNote" ? NativeNoteStyle.color(store.documents.first { $0.id == row.id }?.noteOptions?.color ?? .default) : .secondary).frame(width: 15)
                        Text(row.node.title).lineLimit(1).truncationMode(.middle)
                        Spacer(minLength: 0)
                        if row.id == draft.documentID { Circle().fill(Color.accentColor).frame(width: 5, height: 5) }
                    }.font(.system(size: 12)).padding(.leading, CGFloat(row.depth) * 13).padding(.vertical, 3).contentShape(Rectangle()).tag(row.id).id(row.id)
                    .onTapGesture { select(row.id); if row.node.isFolder { toggleFolder(row.id) } }
                    .accessibilityAction { select(row.id); if row.node.isFolder { toggleFolder(row.id) } }
                    .contextMenu { entryMenu(row.node) }
                    .draggable("\(Product.id):\(id):\(row.id.uuidString)") { Label(row.node.title, systemImage: row.node.isFolder ? "folder" : "doc.text").padding(8) }
                    .dropDestination(for: String.self) { values, _ in row.node.isFolder && drop(values, into: row.id) }
                    .help(row.node.path)
                }
            }.listStyle(.sidebar)
            .overlay { if rows.isEmpty { Text(preferences.query.isEmpty ? "新建文档或导入文件\n开始整理你的工作区" : "没有匹配的文档").font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center).padding() } }
            .dropDestination(for: String.self) { values, _ in drop(values, into: nil) }
            .onAppear { if let selectedID { proxy.scrollTo(selectedID, anchor: .center) } }
            .onChange(of: draft.documentID) { _, value in if let value { proxy.scrollTo(value) } }
        }
    }
    private func editorToolbar(compact: Bool) -> some View {
        HStack(spacing: 10) {
            iconButton("显示或隐藏文档库", "sidebar.left") { if compact { documentPicker.toggle() } else { store.updateVaultPreference(id) { $0.treeVisible.toggle() } } }
            Text(activeDocument?.title ?? "未命名草稿").font(.system(size: 12, weight: .medium)).lineLimit(1).help(activeDocument.map { store.vault.path(of: $0.id) } ?? "草稿")
            Spacer(minLength: 0)
            Text(store.persistenceBlocked ? "保存已暂停" : store.savePending ? "保存中…" : "已保存").font(.system(size: 10)).foregroundStyle(store.persistenceBlocked ? Color.red : .secondary)
            if id == "json" { iconButton("保存", "square.and.arrow.down", action: save).keyboardShortcut("s", modifiers: .command) }
        }.buttonStyle(.borderless).padding(.horizontal, 16).frame(height: 43).background(.bar)
    }
    private var moreMenu: some View {
        Menu {
            Button("重命名…") { begin(.rename) }.disabled(selectedID == nil)
            Button("移动…") { begin(.move) }.disabled(selectedID == nil)
            Button("创建副本") { if let selectedID { perform { try store.duplicateVaultDocument(selectedID) } } }.disabled(!store.documents.contains { $0.id == selectedID })
            Divider()
            Button("导入文件…") { importFiles(folder: false) }.disabled(importing)
            Button("导入文件夹…") { importFiles(folder: true) }.disabled(importing)
            Button("导出当前文档…", action: exportCurrent)
            if id == "quickNote" { Button("导出文档及附件…") { exportWithAttachments(name: activeDocument?.title ?? "未命名笔记", content: draft.input) }.disabled(exporting) }
            Divider()
            Button("打开草稿") { store.openScratch(id) }
            Button("另存为文档…") { begin(.file, saveAs: true) }
            Button("删除…") { begin(.delete) }.disabled(selectedID == nil)
        } label: { Image(systemName: "ellipsis") }.menuStyle(.borderlessButton).menuIndicator(.hidden).fixedSize().frame(width: 20).help("更多文档操作")
    }
    @ViewBuilder private func entryMenu(_ node: VaultNode) -> some View {
        if node.isFolder { Button("新建文档…") { action = VaultAction(kind: .file, parentID: node.id, content: id == "json" ? draft.input : "") }; Button("新建文件夹…") { action = VaultAction(kind: .folder, parentID: node.id) }; Divider() }
        Button("重命名…") { action = VaultAction(kind: .rename, entryID: node.id) }
        Button("移动…") { action = VaultAction(kind: .move, entryID: node.id) }
        if !node.isFolder {
            Button("创建副本") { perform { try store.duplicateVaultDocument(node.id) } }
            Button("导出…") { if let file = store.documents.first(where: { $0.id == node.id }) { FilePanels.saveText(file.content, name: exportName(file.title)) } }
            if id == "quickNote" { Button("导出文档及附件…") { if let file = store.documents.first(where: { $0.id == node.id }) { exportWithAttachments(name: file.title, content: file.content) } }.disabled(exporting) }
        }
        Button("复制路径") { FilePanels.copy(node.path) }
        Divider()
        Button("删除…", role: .destructive) { action = VaultAction(kind: .delete, entryID: node.id) }
    }
    private var allExpanded: Bool { let folders = Set(store.folders.filter { $0.toolID == id }.map(\.id)); return !folders.isEmpty && folders.isSubset(of: preferences.expanded) }
    private func select(_ entry: UUID) {
        if store.documents.contains(where: { $0.id == entry && $0.toolID == id }) { perform { try store.openDocument(entry) }; documentPicker = false }
        else { store.updateVaultPreference(id) { $0.selectedEntryID = entry } }
    }
    private func toggleFolder(_ entry: UUID) { store.updateVaultPreference(id) { if $0.expanded.contains(entry) { $0.expanded.remove(entry) } else { $0.expanded.insert(entry) } } }
    private func begin(_ kind: VaultAction.Kind, saveAs: Bool = false) {
        action = VaultAction(kind: kind, entryID: selectedID, parentID: selectedParent, content: kind == .file && (id == "json" || saveAs) ? draft.input : "")
    }
    private func save() {
        if draft.documentID == nil { begin(.file, saveAs: true) }
        else { store.synchronizeDocument(id); store.saveNow(); if store.error == nil { draft.status = "已保存" } }
    }
    private func exportName(_ name: String) -> String { name.contains(".") ? name.replacingOccurrences(of: "/", with: "_") : name + (id == "json" ? ".json" : ".md") }
    private func exportCurrent() { FilePanels.saveText(draft.input, name: exportName(activeDocument?.title ?? "未命名文档")) }
    private func exportWithAttachments(name: String, content: String) {
        let panel = NSOpenPanel(); panel.canChooseFiles = false; panel.canChooseDirectories = true; panel.canCreateDirectories = true
        panel.prompt = "导出到此处"; panel.message = "将在所选位置新建文件夹，包含 Markdown 笔记和图片附件。"
        let repository = store.repository.attachmentRepository, manifest = store.noteAttachments
        panel.begin { response in
            guard response == .OK, let parent = panel.url else { return }; exporting = true
            Task {
                defer { exporting = false }
                do {
                    let target = try await Task.detached { try repository.exportDocument(name: name, content: content, attachments: manifest, to: parent) }.value
                    draft.status = "已导出：" + target.lastPathComponent
                } catch { FilePanels.error(error) }
            }
        }
    }
    private func iconButton(_ title: String, _ symbol: String, action: @escaping () -> Void) -> some View { Button(action: action) { Image(systemName: symbol) }.help(title).accessibilityLabel(title) }
    private func perform(_ operation: () throws -> Void) { do { try operation(); draft.error = nil } catch { draft.error = error.localizedDescription; FilePanels.error(error) } }
    private func drop(_ values: [String], into parent: UUID?) -> Bool {
        guard values.count == 1, values[0].hasPrefix("\(Product.id):\(id):"), let entry = UUID(uuidString: String(values[0].split(separator: ":").last ?? "")), store.vault.tool(of: entry) == id else { return false }
        do { try store.moveVaultEntry(entry, to: parent); return true } catch { draft.error = error.localizedDescription; return true }
    }
    private func importFiles(folder: Bool) {
        let panel = NSOpenPanel(); panel.canChooseDirectories = folder; panel.canChooseFiles = !folder; panel.allowsMultipleSelection = !folder
        if !folder { panel.allowedContentTypes = id == "json" ? [.json] : [.plainText, .text] }
        panel.prompt = "导入"; let parent = selectedParent
        panel.begin { response in
            guard response == .OK else { return }; let urls = panel.urls; importing = true
            Task {
                defer { importing = false }
                do {
                    let items = try await Task.detached { try DocumentImportReader.read(urls, toolID: id) }.value
                    let ids = try store.importVaultDocuments(items, toolID: id, parent: parent)
                    draft.status = "已导入 \(ids.count) 份文档"; draft.error = nil
                } catch { draft.error = error.localizedDescription; FilePanels.error(error) }
            }
        }
    }
}

struct VaultAction: Identifiable {
    enum Kind { case file, folder, rename, move, delete }
    let id = UUID()
    var kind: Kind
    var entryID: UUID?
    var parentID: UUID?
    var content = ""
    var title: String { switch kind { case .file: return "新建文档"; case .folder: return "新建文件夹"; case .rename: return "重命名"; case .move: return "移动"; case .delete: return "删除" } }
}

private struct VaultActionSheet: View {
    let toolID: String
    let action: VaultAction
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var target: UUID?
    @State private var error: String?
    private var destinations: [DocumentFolder] {
        let excluded = action.entryID.map { store.vault.descendants(of: $0) } ?? []
        return store.folders.filter { $0.toolID == toolID && !excluded.contains($0.id) }.sorted { store.vault.path(of: $0.id).localizedStandardCompare(store.vault.path(of: $1.id)) == .orderedAscending }
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(action.title).font(.title2.bold())
            if let id = action.entryID, [.rename, .move, .delete].contains(action.kind) { Text(store.vault.path(of: id)).font(.caption).foregroundStyle(.secondary).textSelection(.enabled) }
            if action.kind == .delete {
                let ids = action.entryID.map { store.vault.descendants(of: $0) } ?? []
                Text("将删除 \(store.documents.filter { ids.contains($0.id) }.count) 份文档和 \(store.folders.filter { ids.contains($0.id) }.count) 个文件夹。正在编辑的内容会保留为草稿。")
            } else if action.kind == .move {
                Picker("目标文件夹", selection: $target) { Text("文档库根目录").tag(nil as UUID?); ForEach(destinations) { Text(store.vault.path(of: $0.id)).tag(Optional($0.id)) } }
            } else {
                TextField(action.kind == .folder ? "文件夹名称" : "名称", text: $name).textFieldStyle(.roundedBorder).onSubmit(submit)
                if action.kind == .file || action.kind == .folder { Text("位置：" + (action.parentID.map { store.vault.path(of: $0) } ?? "文档库根目录")).font(.caption).foregroundStyle(.secondary) }
            }
            if let error { Text(error).font(.caption).foregroundStyle(.red).textSelection(.enabled) }
            HStack { Button("取消") { dismiss() }.keyboardShortcut(.cancelAction); Spacer(); Button(action.kind == .delete ? "删除" : action.kind == .move ? "移动" : "保存", role: action.kind == .delete ? .destructive : nil, action: submit).buttonStyle(.borderedProminent).keyboardShortcut(.defaultAction) }
        }.padding(24).frame(width: 430)
        .onAppear {
            target = action.entryID.flatMap { store.vault.parent(of: $0) }
            if action.kind == .rename, let id = action.entryID { name = store.vault.name(of: id) ?? "" }
            else { name = store.vault.uniqueName(action.kind == .folder ? "新建文件夹" : toolID == "json" ? "未命名.json" : "未命名笔记.md", toolID: toolID, parent: action.parentID) }
        }
    }
    private func submit() {
        do {
            switch action.kind {
            case .file: try store.createVaultDocument(toolID, name: name, parent: action.parentID, content: action.content.isEmpty && toolID == "json" ? "{\n\n}" : action.content)
            case .folder: try store.createVaultFolder(toolID, name: name, parent: action.parentID)
            case .rename: if let id = action.entryID { try store.renameVaultEntry(id, name: name) }
            case .move: if let id = action.entryID { try store.moveVaultEntry(id, to: target) }
            case .delete: if let id = action.entryID { try store.deleteVaultEntry(id) }
            }
            dismiss()
        } catch { self.error = error.localizedDescription }
    }
}
