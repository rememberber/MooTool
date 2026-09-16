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
                    Section("常用") { ForEach(store.pinned.filter { navigationVisible($0) && Catalog.tool($0).matches(query) }, id: \.self) { row(Catalog.tool($0)) } }
                }
                ForEach(store.customGroups) { custom in
                    let tools = custom.toolIds.compactMap { id in Catalog.tools.first { $0.id == id } }.filter { navigationVisible($0.id) && $0.matches(query) }
                    if !tools.isEmpty {
                        Section(custom.name) { ForEach(tools) { row($0) } }
                            .listSectionSeparator(store.showNavigationSeparators ? .visible : .hidden)
                    }
                }
                ForEach(Catalog.groups, id: \.self) { group in
                    let tools = Catalog.tools.filter { $0.group == group && navigationVisible($0.id) && $0.matches(query) }
                    if !tools.isEmpty { Section(group) { ForEach(tools) { row($0) } } }
                }
                if store.showRecent && !store.recent.isEmpty && query.isEmpty {
                    Section("最近使用") { ForEach(store.recent.filter { navigationVisible($0) }.prefix(5), id: \.self) { row(Catalog.tool($0)) } }
                }
            }.listStyle(.sidebar)
                .navigationSplitViewColumnWidth(
                    min: store.hideNavigationTitles ? 58 : 185,
                    ideal: store.hideNavigationTitles ? 64 : store.sidebarWidth,
                    max: store.hideNavigationTitles ? 88 : 300)
                .overlay(alignment: .trailing) {
                    if !store.hideNavigationTitles {
                        PaneResizeDivider(
                            vertical: true,
                            current: CGFloat(store.sidebarWidth),
                            min: 185,
                            max: 300,
                            onResize: { store.sidebarWidth = Double($0); store.scheduleSave() },
                            onReset: { store.sidebarWidth = 215; store.scheduleSave() })
                    }
                }
                .searchable(text: $query, placement: .sidebar, prompt: "搜索工具")
                .safeAreaInset(edge: .bottom) {
                    HStack(spacing: 9) {
                        Image(nsImage: NSImage(contentsOf: AppResources.bundle.url(forResource: "Brand", withExtension: "png")!)!).resizable().frame(width: 24, height: 24)
                        VStack(alignment: .leading, spacing: 2) { Text("MooTool").font(.system(size: 12, weight: .semibold)); Text("Next Native").font(.system(size: 10)).foregroundStyle(.secondary) }
                        Spacer()
                        Button {
                            store.hideNavigationTitles.toggle(); store.scheduleSave()
                        } label: {
                            Image(systemName: store.hideNavigationTitles ? "sidebar.left" : "sidebar.right")
                        }.buttonStyle(.plain).help(store.hideNavigationTitles ? "展开导航标题" : "仅显示图标")
                        SettingsLink { Image(systemName: "gearshape") }.buttonStyle(.plain).help("设置 · ⌘,")
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
    private func navigationVisible(_ id: String) -> Bool {
        id == "mootool" || !store.hiddenNavigationToolIds.contains(id)
    }
    private func row(_ tool: Tool) -> some View {
        Group {
            if store.hideNavigationTitles {
                Label(tool.title, systemImage: tool.symbol).labelStyle(.iconOnly)
            } else {
                Label(tool.title, systemImage: tool.symbol)
            }
        }
        .font(.system(size: 12.5)).tag(tool.id)
        .help(store.hideNavigationTitles ? tool.title : "")
            .contextMenu {
                Button(store.pinned.contains(tool.id) ? "移出常用" : "添加到常用") { store.togglePin(tool.id) }
                Button("在独立窗口中打开") { openWindow(id: "tool", value: tool.id) }
            }
    }
}

private struct HomePerson: Identifiable {
    let id = UUID()
    let title: String
    let url: String
}

struct HomeView: View {
    @Environment(AppStore.self) private var store
    private let contributors = [
        HomePerson(title: "CassianFlorin", url: "https://github.com/CassianFlorin"),
        HomePerson(title: "felixcn", url: "https://github.com/felixcn"),
        HomePerson(title: "felixnan168", url: "https://gitee.com/felixnan168"),
        HomePerson(title: "Lyp", url: "https://gitee.com/L1yp"),
        HomePerson(title: "sunsence", url: "https://github.com/sunsence"),
        HomePerson(title: "rememberber", url: "https://github.com/rememberber")
    ]
    private let works: [(String, String, String)] = [
        ("WePush", "微信消息推送与定时提醒", "https://github.com/rememberber/WePush"),
        ("MooInfo", "系统与硬件信息速览", "https://github.com/rememberber/MooInfo")
    ]
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 30) {
                HStack(spacing: 22) {
                    Image(nsImage: NSImage(contentsOf: AppResources.bundle.url(forResource: "Brand", withExtension: "png")!)!).resizable().scaledToFit().frame(width: 86, height: 86)
                    VStack(alignment: .leading, spacing: 7) {
                        HStack(alignment: .firstTextBaseline) {
                            Text("MooTool").font(.system(size: 34, weight: .bold))
                            Text("Next Native").font(.caption).padding(.horizontal, 9).padding(.vertical, 4).background(.quaternary, in: Capsule())
                            Text("v\(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.8.0")").font(.caption).foregroundStyle(.tertiary)
                        }
                        Text("开发与日常，得心应手。").font(.system(size: 15)).foregroundStyle(.secondary)
                        Text("由 Zhou Bo 创作").font(.caption).foregroundStyle(.tertiary)
                        Link("mootool.luoboduner.com ↗", destination: URL(string: "https://mootool.luoboduner.com")!).font(.caption)
                    }
                }
                homeSection("关于") {
                    Text("MooTool 是面向开发者与日常效率的桌面工具箱。原生版保留 26 个工具入口与工作区，数据独立保存在本机。").font(.system(size: 13)).foregroundStyle(.secondary).lineSpacing(4)
                }
                homeSection("贡献者") {
                    FlowLayout(spacing: 10) {
                        ForEach(contributors) { person in
                            Link(person.title, destination: URL(string: person.url)!).font(.system(size: 12, weight: .medium))
                        }
                    }
                    Text("感谢每一位为 MooTool 生态贡献想法、代码与反馈的朋友。").font(.caption).foregroundStyle(.tertiary).padding(.top, 6)
                }
                homeSection("赞赏") {
                    Text("如果 MooTool 对你有帮助，欢迎请作者喝杯咖啡。").font(.system(size: 13)).foregroundStyle(.secondary)
                    if let sponsor = AppResources.bundle.url(forResource: "wx-zanshang", withExtension: "jpg") {
                        Image(nsImage: NSImage(contentsOf: sponsor)!).resizable().scaledToFit().frame(width: 139).clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                    Text("微信赞赏码").font(.caption).foregroundStyle(.tertiary)
                }
                homeSection("源码与反馈") {
                    VStack(alignment: .leading, spacing: 6) {
                        Link("GitHub ↗", destination: URL(string: "https://github.com/rememberber/MooTool")!)
                        Link("Gitee ↗", destination: URL(string: "https://gitee.com/zhoubochina/MooTool")!)
                        Link("提交问题 ↗", destination: URL(string: "https://github.com/rememberber/MooTool/issues")!)
                    }
                }
                homeSection("其他作品") {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(works, id: \.0) { work in
                            Link("\(work.0) ↗", destination: URL(string: work.2)!)
                            Text(work.1).font(.caption).foregroundStyle(.secondary)
                        }
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
                Text("MIT License").font(.caption).foregroundStyle(.tertiary)
            }.frame(maxWidth: 820).padding(44).frame(maxWidth: .infinity)
        }.background(Color(nsColor: .windowBackgroundColor))
    }
    @ViewBuilder private func homeSection(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.headline)
            content()
        }
    }
}

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(proposal: proposal, subviews: subviews)
        let height = rows.map(\.height).reduce(0, +) + CGFloat(max(0, rows.count - 1)) * spacing
        return CGSize(width: proposal.width ?? 0, height: height)
    }
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(proposal: proposal, subviews: subviews) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }
    private struct Row { var indices: [Int]; var height: CGFloat }
    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> [Row] {
        let width = proposal.width ?? 640
        var rows: [Row] = []
        var current = Row(indices: [], height: 0)
        var x: CGFloat = 0
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            if x + size.width > width && !current.indices.isEmpty {
                rows.append(current); current = Row(indices: [], height: 0); x = 0
            }
            current.indices.append(index)
            current.height = max(current.height, size.height)
            x += size.width + spacing
        }
        if !current.indices.isEmpty { rows.append(current) }
        return rows
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
                        VStack(alignment: .leading, spacing: 5) {
                            if toolID == "textDiff" {
                                Text(item.draft.input).lineLimit(1).font(.system(.body, design: .monospaced))
                                Text("→ " + item.draft.secondary).lineLimit(1).font(.system(.caption, design: .monospaced)).foregroundStyle(.secondary)
                                Text(item.date.formatted()).font(.caption).foregroundStyle(.secondary)
                            } else if toolID == "reformat" {
                                let options = ReformatOptions.migrating(item.draft)
                                Text(options.tab == .file ? (options.fileSource.isEmpty ? item.draft.output : options.fileSource) : (item.draft.input.isEmpty ? item.draft.output : item.draft.input))
                                    .lineLimit(2).font(.system(.body, design: .monospaced))
                                Text("\(options.type.title) · \(options.tab == .file ? "文件" : "文本") · \(item.date.formatted())")
                                    .font(.caption).foregroundStyle(.secondary)
                            } else {
                                Text(item.draft.input.isEmpty ? item.draft.output : item.draft.input).lineLimit(2).font(.system(.body, design: .monospaced))
                                Text(item.date.formatted()).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button { if let index = store.history.firstIndex(where: { $0.id == item.id }) { store.history[index].favorite.toggle(); store.scheduleSave() } } label: { Image(systemName: item.favorite ? "star.fill" : "star") }
                        Button("恢复") { store.restoreDraft(toolID, record: item.draft); dismiss() }.disabled(store.draft(toolID).busy)
                    }
                }
            }.overlay { if !store.history.contains(where: { $0.toolID == toolID }) { ContentUnavailableView("暂无历史记录", systemImage: "clock", description: Text("运行工具后，结果会保存在这里。")) } }
        }.frame(width: 680, height: 470)
    }
}
