import SwiftUI
import MooToolNextCore

struct Workbench: View {
    @Environment(AppStore.self) private var store
    @Environment(\.openWindow) private var openWindow
    @State private var visibility: NavigationSplitViewVisibility = .all
    @State private var query = ""
    var body: some View {
        @Bindable var store = store
        NavigationSplitView(columnVisibility: $visibility) {
            List(selection: Binding(get: { store.selected }, set: { store.select($0) })) {
                row(Catalog.tools[0])
                if !store.pinned.isEmpty {
                    Section("常用") { ForEach(store.pinned.filter { Catalog.tool($0).matches(query) }, id: \.self) { row(Catalog.tool($0)) } }
                }
                ForEach(Catalog.groups, id: \.self) { group in
                    let tools = Catalog.tools.filter { $0.group == group && $0.matches(query) }
                    if !tools.isEmpty { Section(group) { ForEach(tools) { row($0) } } }
                }
                if !store.recent.isEmpty && query.isEmpty {
                    Section("最近使用") { ForEach(store.recent.prefix(5), id: \.self) { row(Catalog.tool($0)) } }
                }
            }.listStyle(.sidebar).navigationSplitViewColumnWidth(min: 185, ideal: 215, max: 280)
                .searchable(text: $query, placement: .sidebar, prompt: "搜索工具")
                .safeAreaInset(edge: .bottom) {
                    HStack(spacing: 9) {
                        Image(nsImage: NSImage(contentsOf: AppResources.bundle.url(forResource: "Brand", withExtension: "png")!)!).resizable().frame(width: 24, height: 24)
                        VStack(alignment: .leading, spacing: 2) { Text("MooTool").font(.system(size: 12, weight: .semibold)); Text("Next Native").font(.system(size: 10)).foregroundStyle(.secondary) }
                        Spacer(); SettingsLink { Image(systemName: "gearshape") }.buttonStyle(.plain).help("设置 · ⌘,")
                    }.padding(14).background(.bar)
                }
        } detail: {
            ToolRouter(id: store.selected)
                .navigationTitle(Catalog.tool(store.selected).title)
                .toolbar {
                    ToolbarItemGroup {
                        Button { store.searchPresented = true } label: { Label("搜索工具", systemImage: "magnifyingglass") }.help("搜索工具 · ⌘K")
                        if store.selected != "mootool" {
                            Button { store.togglePin(store.selected) } label: { Label("常用", systemImage: store.pinned.contains(store.selected) ? "star.fill" : "star") }.help("添加或移出常用工具")
                            Button { store.historyPresented = true } label: { Label("历史记录", systemImage: "clock.arrow.circlepath") }
                            Button { openWindow(id: "tool", value: store.selected) } label: { Label("独立窗口", systemImage: "rectangle.on.rectangle") }
                        }
                    }
                }
        }.frame(minWidth: 940, minHeight: 630)
            .sheet(isPresented: $store.searchPresented) { CommandPalette().environment(store) }
            .sheet(isPresented: $store.historyPresented) { HistoryView(toolID: store.selected).environment(store) }
            .alert("工作区提示", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) { Button("好") { store.error = nil } } message: { Text(store.error ?? "") }
    }
    private func row(_ tool: Tool) -> some View {
        Label(tool.title, systemImage: tool.symbol).font(.system(size: 12.5)).tag(tool.id)
            .contextMenu {
                Button(store.pinned.contains(tool.id) ? "移出常用" : "添加到常用") { store.togglePin(tool.id) }
                Button("在独立窗口中打开") { openWindow(id: "tool", value: tool.id) }
            }
    }
}

struct HomeView: View {
    @Environment(AppStore.self) private var store
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 30) {
                HStack(spacing: 22) {
                    Image(nsImage: NSImage(contentsOf: AppResources.bundle.url(forResource: "Brand", withExtension: "png")!)!).resizable().scaledToFit().frame(width: 86, height: 86)
                    VStack(alignment: .leading, spacing: 7) {
                        HStack(alignment: .firstTextBaseline) { Text("MooTool").font(.system(size: 34, weight: .bold)); Text("Next Native").font(.caption).padding(.horizontal, 9).padding(.vertical, 4).background(.quaternary, in: Capsule()) }
                        Text("开发与日常，得心应手。").font(.system(size: 15)).foregroundStyle(.secondary)
                        Link("mootool.luoboduner.com ↗", destination: URL(string: "https://mootool.luoboduner.com")!).font(.caption)
                    }
                }
                Button { store.searchPresented = true } label: {
                    HStack { Image(systemName: "magnifyingglass"); Text("查找你需要的工具"); Spacer(); Text("⌘ K").font(.system(size: 11, design: .monospaced)).padding(5).background(.quaternary.opacity(0.6), in: RoundedRectangle(cornerRadius: 5)) }
                        .foregroundStyle(.secondary).padding(16).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(.quaternary))
                }.buttonStyle(.plain)
                VStack(alignment: .leading, spacing: 13) {
                    Text("常用工具").font(.headline)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 195), spacing: 12)], spacing: 12) {
                        ForEach(store.pinned.isEmpty ? Array(Catalog.tools.dropFirst().prefix(6)) : store.pinned.map(Catalog.tool)) { tool in
                            Button { store.select(tool.id) } label: {
                                VStack(alignment: .leading, spacing: 12) {
                                    Image(systemName: tool.symbol).font(.system(size: 22, weight: .light)).foregroundStyle(.tint)
                                    Text(tool.title).font(.system(size: 13, weight: .semibold)).foregroundStyle(.primary)
                                    Text(tool.subtitle).font(.system(size: 11)).foregroundStyle(.secondary).lineLimit(2).frame(height: 30, alignment: .top)
                                }.frame(maxWidth: .infinity, alignment: .leading).padding(17)
                                    .background(.quaternary.opacity(0.3), in: RoundedRectangle(cornerRadius: 12))
                            }.buttonStyle(.plain)
                        }
                    }
                }
                Divider()
                VStack(alignment: .leading, spacing: 12) {
                    Text("为 Mac 而生").font(.headline)
                    Text("熟悉的 MooTool 工作方式，原生的 macOS 体验。通过侧边栏探索文本、开发、网络、编码和日常工具，也可以把工具单独打开，让工作区更从容。").font(.system(size: 13)).foregroundStyle(.secondary).lineSpacing(5)
                    HStack(spacing: 18) {
                        Label("本地保存", systemImage: "internaldrive"); Label("独立安装", systemImage: "square.stack.3d.up"); Label("原生快捷键", systemImage: "command")
                    }.font(.caption).foregroundStyle(.secondary)
                }
                HStack {
                    Text("由 Zhou Bo 创作 · MIT License").foregroundStyle(.tertiary); Spacer()
                    Link("GitHub ↗", destination: URL(string: "https://github.com/rememberber/MooTool")!)
                }.font(.caption)
            }.frame(maxWidth: 820).padding(44).frame(maxWidth: .infinity)
        }.background(Color(nsColor: .windowBackgroundColor))
    }
}

struct CommandPalette: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var selection: String?
    @FocusState private var focused: Bool
    var results: [Tool] { Catalog.tools.filter { $0.matches(query) } }
    var body: some View {
        VStack(spacing: 0) {
            HStack { Image(systemName: "magnifyingglass").foregroundStyle(.secondary); TextField("搜索工具、名称或关键词", text: $query).textFieldStyle(.plain).font(.title3).focused($focused).onSubmit(open); Button("取消") { dismiss() }.keyboardShortcut(.cancelAction) }.padding(20)
            Divider()
            List(results, selection: $selection) { tool in
                HStack(spacing: 13) { Image(systemName: tool.symbol).frame(width: 25).foregroundStyle(.tint); VStack(alignment: .leading, spacing: 3) { Text(tool.title); Text(tool.subtitle).font(.caption).foregroundStyle(.secondary) }; Spacer(); Text(tool.group).font(.caption).foregroundStyle(.tertiary) }.padding(.vertical, 5).tag(tool.id)
                    .contentShape(Rectangle()).onTapGesture { store.select(tool.id); dismiss() }
            }.listStyle(.inset).onSubmit(open)
            HStack { Text("↑ ↓ 选择 · ↩ 打开 · Esc 关闭"); Spacer(); Text("\(results.count) 个工具") }.font(.caption).foregroundStyle(.secondary).padding(12)
        }.frame(width: 560, height: 430).onAppear { focused = true; selection = results.first?.id }
            .onChange(of: query) { selection = results.first?.id }
            .onMoveCommand { direction in
                let index = results.firstIndex { $0.id == selection } ?? 0
                if direction == .down, index + 1 < results.count { selection = results[index + 1].id }
                if direction == .up, index > 0 { selection = results[index - 1].id }
            }
    }
    private func open() { if let id = selection ?? results.first?.id { store.select(id); dismiss() } }
}

struct HistoryView: View {
    let toolID: String
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(alignment: .leading) {
            HStack { Text("\(Catalog.tool(toolID).title) · 历史记录").font(.title2); Spacer(); Button("完成") { dismiss() }.keyboardShortcut(.cancelAction) }.padding()
            List {
                ForEach(store.history.filter { $0.toolID == toolID }) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 5) { Text(item.draft.input.isEmpty ? item.draft.output : item.draft.input).lineLimit(2).font(.system(.body, design: .monospaced)); Text(item.date.formatted()).font(.caption).foregroundStyle(.secondary) }
                        Spacer()
                        Button { if let index = store.history.firstIndex(where: { $0.id == item.id }) { store.history[index].favorite.toggle(); store.scheduleSave() } } label: { Image(systemName: item.favorite ? "star.fill" : "star") }
                        Button("恢复") { store.restoreDraft(toolID, record: item.draft); dismiss() }.disabled(store.draft(toolID).busy)
                    }
                }
            }.overlay { if !store.history.contains(where: { $0.toolID == toolID }) { ContentUnavailableView("暂无历史记录", systemImage: "clock", description: Text("运行工具后，结果会保存在这里。")) } }
        }.frame(width: 680, height: 470)
    }
}
