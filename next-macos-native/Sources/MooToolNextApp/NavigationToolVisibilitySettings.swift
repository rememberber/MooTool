import SwiftUI
import MooToolNextCore

struct NavigationToolVisibilitySettings: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    @State private var hidden: Set<String> = []
    private var navigationTools: [Tool] { Catalog.localizedTools(language).filter { $0.id != "mootool" } }

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(loc("settings.navTools.title")).font(.title2)
                Spacer()
                Button(loc("common.done")) { store.hiddenNavigationToolIds = Array(hidden).sorted(); store.scheduleSave(); dismiss() }.keyboardShortcut(.defaultAction)
                Button(loc("common.cancel"), role: .cancel) { dismiss() }.keyboardShortcut(.cancelAction)
            }.padding()
            Text(loc("settings.navTools.hint")).font(.caption).foregroundStyle(.secondary).padding(.horizontal)
            HStack {
                Button(loc("settings.navTools.showAll")) { hidden.removeAll() }.disabled(hidden.isEmpty)
                Button(loc("settings.navTools.hideAll")) { hidden = Set(navigationTools.map(\.id)) }.disabled(hidden.count == navigationTools.count)
                Spacer()
            }.padding(.horizontal).padding(.top, 8)
            List {
                ForEach(Catalog.groups, id: \.self) { group in
                    let tools = navigationTools.filter { $0.group == group }
                    if !tools.isEmpty {
                        Section(AppLocalization.groupTitle(group, language: language)) {
                            ForEach(tools) { tool in
                                Toggle(isOn: Binding(
                                    get: { !hidden.contains(tool.id) },
                                    set: { visible in setVisible(tool.id, visible: visible) }
                                )) {
                                    Label(tool.title, systemImage: tool.symbol)
                                }
                            }
                        }
                    }
                }
            }.listStyle(.inset)
        }.frame(width: 480, height: 520)
        .onAppear { hidden = Set(store.hiddenNavigationToolIds) }
    }

    private func setVisible(_ id: String, visible: Bool) {
        if visible { hidden.remove(id) }
        else { hidden.insert(id) }
    }
}
