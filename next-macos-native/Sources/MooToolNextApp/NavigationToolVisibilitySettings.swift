import SwiftUI
import MooToolNextCore

struct NavigationToolVisibilitySettings: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var hidden: Set<String> = []
    private var navigationTools: [Tool] { Catalog.tools.filter { $0.id != "mootool" } }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("侧栏工具显示").font(.title2)
                Spacer()
                Button("完成") { store.hiddenNavigationToolIds = Array(hidden).sorted(); store.scheduleSave(); dismiss() }.keyboardShortcut(.defaultAction)
                Button("取消", role: .cancel) { dismiss() }.keyboardShortcut(.cancelAction)
            }.padding()
            Text("与 Electron 相同：取消勾选的工具不会出现在侧边栏，仍可通过 ⌘K 搜索打开。").font(.caption).foregroundStyle(.secondary).padding(.horizontal)
            HStack {
                Button("全部显示") { hidden.removeAll() }.disabled(hidden.isEmpty)
                Button("全部隐藏") { hidden = Set(navigationTools.map(\.id)) }.disabled(hidden.count == navigationTools.count)
                Spacer()
            }.padding(.horizontal).padding(.top, 8)
            List {
                ForEach(Catalog.groups, id: \.self) { group in
                    let tools = navigationTools.filter { $0.group == group }
                    if !tools.isEmpty {
                        Section(group) {
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
