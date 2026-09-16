import SwiftUI
import MooToolNextCore

struct HostWorkspace: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var selectedID: UUID?
    @State private var profileName = ""
    @State private var search = ""

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }

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
            Button(AppLocalization.string("tool.delete", language: language), systemImage: "trash") { deleteProfile() }.disabled(selectedID == nil)
            Button(AppLocalization.string("host.readSystem", language: language), systemImage: "arrow.clockwise") { readSystemHosts() }
            Button(AppLocalization.string("host.validate", language: language)) { validateHosts() }
            Button(AppLocalization.string("host.export", language: language)) { FilePanels.saveText(draft.input, name: "hosts") }
        } content: {
            PersistedHSplit(toolID: "host", defaultLeading: 220, minLeading: 170, maxLeading: 360) {
                VStack(alignment: .leading, spacing: 8) {
                    TextField(loc("host.search"), text: $search).textFieldStyle(.roundedBorder)
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
                        Text(loc("host.emptyHint")).font(.caption).foregroundStyle(.secondary).padding(.horizontal, 8)
                    }
                }
            } trailing: {
                VStack(alignment: .leading, spacing: 10) {
                    TextField(loc("host.profileName"), text: $profileName).textFieldStyle(.roundedBorder)
                    EditorPane(title: loc("host.content"), text: $draft.input)
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
        let profile = SavedHostProfile(name: loc("host.defaultProfileName"), content: "127.0.0.1 localhost\n")
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
            profile.name = profileName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? loc("host.unnamedProfile") : profileName
            profile.content = draft.input
            profile.modified = Date()
            try profile.validate(language: language)
            store.hostProfiles[index] = profile
            profileName = profile.name
            draft.status = loc("host.status.saved")
            store.scheduleSave()
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func readSystemHosts() {
        do {
            draft.input = try String(contentsOfFile: "/etc/hosts", encoding: .utf8)
            draft.status = loc("host.status.readSystem")
            draft.error = nil
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func validateHosts() {
        let lang = language
        store.run("host") { d in try HostWorkspace.validateContent(d.input, language: lang) }
    }

    static func validateContent(_ text: String, language: AppLanguage) throws -> String {
        try DeveloperServices.validateHostsContent(text, language: language)
    }

    private func deleteProfile() {
        guard let id = selectedID else { return }
        store.hostProfiles.removeAll { $0.id == id }
        selectedID = store.hostProfiles.first?.id
        loadSelection()
        store.scheduleSave()
    }
}
