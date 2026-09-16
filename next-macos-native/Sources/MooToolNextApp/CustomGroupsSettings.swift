import SwiftUI
import MooToolNextCore

struct CustomGroupsSettings: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var groups: [CustomToolGroup] = []
    @State private var message: String?
    private var selectableTools: [Tool] { Catalog.tools.filter { $0.id != "mootool" } }
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("自定义分组").font(.title2)
                Spacer()
                Button("完成") { saveAndClose() }.keyboardShortcut(.defaultAction)
                Button("取消", role: .cancel) { dismiss() }.keyboardShortcut(.cancelAction)
            }.padding()
            if let message { Text(message).font(.caption).foregroundStyle(.red).padding(.horizontal) }
            Text("与 Electron 相同：在侧边栏显示自定义工具分组，顺序与工具列表一致。").font(.caption).foregroundStyle(.secondary).padding(.horizontal)
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
                        TextField("分组名称", text: $group.name).textFieldStyle(.roundedBorder)
                    }
                }.onDelete { groups.remove(atOffsets: $0) }
                .onMove { groups.move(fromOffsets: $0, toOffset: $1) }
            }.listStyle(.inset)
            HStack {
                Button("添加分组") {
                    groups.append(CustomToolGroup(name: "新分组", toolIds: []))
                }
                Spacer()
                Toggle("显示分组分隔线", isOn: Binding(
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
