import SwiftUI
import MooToolNextCore

struct HostWorkspace: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var selectedID: UUID?
    @State private var profileName = ""
    @State private var search = ""

    private var filtered: [SavedHostProfile] {
        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return store.hostProfiles }
        return store.hostProfiles.filter {
            $0.name.localizedCaseInsensitiveContains(query) || $0.content.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        ToolPage(tool: Catalog.tool("host"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.newProfile", language: language), symbol: "plus") { createProfile() }
            PrimaryButton(title: AppLocalization.string("tool.saveProfile", language: language), symbol: "square.and.arrow.down") { saveProfile() }.disabled(selectedID == nil)
            Button("删除配置", systemImage: "trash") { deleteProfile() }.disabled(selectedID == nil)
            Button("读取系统 Hosts", systemImage: "arrow.clockwise") { readSystemHosts() }
            Button("检查配置") { validateHosts() }
            Button("导出 hosts") { FilePanels.saveText(draft.input, name: "hosts") }
        } content: {
            PersistedHSplit(toolID: "host", defaultLeading: 220, minLeading: 170, maxLeading: 360) {
                VStack(alignment: .leading, spacing: 8) {
                    TextField("搜索配置", text: $search).textFieldStyle(.roundedBorder)
                    List {
                        ForEach(filtered) { profile in
                            Button {
                                selectedID = profile.id
                                loadSelection()
                            } label: {
                                HStack {
                                    Text(profile.name)
                                    Spacer()
                                    if profile.id == selectedID { Image(systemName: "checkmark").foregroundStyle(.secondary) }
                                }
                            }.buttonStyle(.plain)
                        }
                    }.listStyle(.inset)
                    if store.hostProfiles.isEmpty {
                        Text("暂无保存的配置，可新建或从设置迁移导入。").font(.caption).foregroundStyle(.secondary).padding(.horizontal, 8)
                    }
                }
            } trailing: {
                VStack(alignment: .leading, spacing: 10) {
                    TextField("配置名称", text: $profileName).textFieldStyle(.roundedBorder)
                    EditorPane(title: "Hosts 内容", text: $draft.input)
                }
            }
        }
        .onAppear { bootstrapSelection() }
        .onChange(of: selectedID) { loadSelection() }
    }

    private func bootstrapSelection() {
        if let id = UUID(uuidString: draft.option), store.hostProfiles.contains(where: { $0.id == id }) {
            selectedID = id
        } else if let first = store.hostProfiles.first {
            selectedID = first.id
        } else if draft.input.isEmpty {
            readSystemHosts()
        }
        loadSelection()
    }

    private func loadSelection() {
        guard let id = selectedID, let index = store.hostProfiles.firstIndex(where: { $0.id == id }) else {
            draft.option = ""
            return
        }
        let profile = store.hostProfiles[index]
        profileName = profile.name
        draft.input = profile.content
        draft.option = profile.id.uuidString
        draft.error = nil
    }

    private func createProfile() {
        let profile = SavedHostProfile(name: "新配置", content: "127.0.0.1 localhost\n")
        store.hostProfiles.append(profile)
        selectedID = profile.id
        profileName = profile.name
        draft.input = profile.content
        draft.option = profile.id.uuidString
        store.scheduleSave()
    }

    private func saveProfile() {
        guard let id = selectedID, let index = store.hostProfiles.firstIndex(where: { $0.id == id }) else { return }
        do {
            var profile = store.hostProfiles[index]
            profile.name = profileName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "未命名" : profileName
            profile.content = draft.input
            profile.modified = Date()
            try profile.validate()
            store.hostProfiles[index] = profile
            profileName = profile.name
            draft.status = "已保存配置"
            store.scheduleSave()
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func readSystemHosts() {
        do {
            draft.input = try String(contentsOfFile: "/etc/hosts", encoding: .utf8)
            draft.status = "已读取 /etc/hosts，可编辑后导出或保存为配置。"
            draft.error = nil
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func validateHosts() {
        store.run("host") { d in try HostWorkspace.validateContent(d.input) }
    }

    static func validateContent(_ text: String) throws -> String {
        try DeveloperServices.validateHostsContent(text)
    }

    private func deleteProfile() {
        guard let id = selectedID else { return }
        store.hostProfiles.removeAll { $0.id == id }
        selectedID = store.hostProfiles.first?.id
        loadSelection()
        store.scheduleSave()
    }
}
