import SwiftUI
import MooToolNextCore

struct CustomGroupsSettings: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    @State private var groups: [CustomToolGroup] = []
    @State private var message: String?
    private var selectableTools: [Tool] { Catalog.localizedTools(language).filter { $0.id != "mootool" } }

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(loc("settings.customGroups")).font(.title2)
                Spacer()
                Button(loc("common.done")) { saveAndClose() }.keyboardShortcut(.defaultAction)
                Button(loc("common.cancel"), role: .cancel) { dismiss() }.keyboardShortcut(.cancelAction)
            }.padding()
            if let message { Text(message).font(.caption).foregroundStyle(.red).padding(.horizontal) }
            Text(loc("settings.customGroups.hint")).font(.caption).foregroundStyle(.secondary).padding(.horizontal)
            List {
                ForEach($groups) { $group in
                    DisclosureGroup {
                        ForEach(selectableTools) { tool in
                            Toggle(tool.title, isOn: Binding(
                                get: { group.toolIds.contains(tool.id) },
                                set: { on in
                                    if on { if !group.toolIds.contains(tool.id) { group.toolIds.append(tool.id) } }
                                    else { group.toolIds.removeAll { $0 == tool.id } }
                                }))
                        }
                    } label: {
                        TextField(loc("settings.customGroups.namePlaceholder"), text: $group.name).textFieldStyle(.roundedBorder)
                    }
                }.onDelete { groups.remove(atOffsets: $0) }
                .onMove { groups.move(fromOffsets: $0, toOffset: $1) }
            }.listStyle(.inset)
            HStack {
                Button(loc("settings.customGroups.add")) {
                    groups.append(CustomToolGroup(name: loc("settings.customGroups.newGroup"), toolIds: []))
                }
                Spacer()
                Toggle(AppLocalization.string("settings.showGroupSeparators", language: language), isOn: Binding(
                    get: { store.showNavigationSeparators },
                    set: { store.showNavigationSeparators = $0; store.scheduleSave() }))
            }.padding()
        }.frame(width: 520, height: 520)
        .onAppear { groups = store.customGroups }
    }
    private func saveAndClose() {
        do {
            let next = CustomToolGroupRules.sanitized(groups)
            try CustomToolGroupRules.validate(next, knownToolIDs: Set(Catalog.tools.map(\.id)))
            store.customGroups = next
            store.scheduleSave()
            dismiss()
        } catch { message = error.localizedDescription }
    }
}
