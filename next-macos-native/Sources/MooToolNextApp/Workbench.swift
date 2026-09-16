import SwiftUI
import MooToolNextCore

struct Workbench: View {
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @Environment(\.openWindow) private var openWindow
    @State private var visibility: NavigationSplitViewVisibility = .all
    @State private var query = ""
    var body: some View {
        @Bindable var store = store
        NavigationSplitView(columnVisibility: $visibility) {
            List(selection: Binding(get: { store.selected }, set: { store.select($0) })) {
                row(Catalog.localizedTool(Catalog.tools[0].id, language: language))
                if !store.pinned.isEmpty {
                    Section(AppLocalization.string("workbench.pinned", language: language)) { ForEach(store.pinned.filter { navigationVisible($0) && Catalog.tool($0).matches(query) }, id: \.self) { row(Catalog.localizedTool($0, language: language)) } }
                }
                ForEach(store.customGroups) { custom in
                    let tools = custom.toolIds.compactMap { id in Catalog.tools.first { $0.id == id } }.filter { navigationVisible($0.id) && $0.matches(query) }.map { $0.localized(in: language) }
                    if !tools.isEmpty {
                        Section(custom.name) { ForEach(tools) { row($0) } }
                            .listSectionSeparator(store.showNavigationSeparators ? .visible : .hidden)
                    }
                }
                ForEach(Catalog.groups, id: \.self) { group in
                    let tools = Catalog.tools.filter { $0.group == group && navigationVisible($0.id) && $0.matches(query) }.map { $0.localized(in: language) }
                    if !tools.isEmpty { Section(AppLocalization.groupTitle(group, language: language)) { ForEach(tools) { row($0) } } }
                }
                if store.showRecent && !store.recent.isEmpty && query.isEmpty {
                    Section(AppLocalization.string("app.nav.recent", language: language)) { ForEach(store.recent.filter { navigationVisible($0) }.prefix(5), id: \.self) { row(Catalog.localizedTool($0, language: language)) } }
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
                .searchable(text: $query, placement: .sidebar, prompt: AppLocalization.string("app.search.placeholder", language: language))
                .safeAreaInset(edge: .bottom) {
                    HStack(spacing: 9) {
                        Image(nsImage: NSImage(contentsOf: AppResources.bundle.url(forResource: "Brand", withExtension: "png")!)!).resizable().frame(width: 24, height: 24)
                        VStack(alignment: .leading, spacing: 2) { Text("MooTool").font(.system(size: 12, weight: .semibold)); Text("Next Native").font(.system(size: 10)).foregroundStyle(.secondary) }
                        Spacer()
                        Button {
                            store.hideNavigationTitles.toggle(); store.scheduleSave()
                        } label: {
                            Image(systemName: store.hideNavigationTitles ? "sidebar.left" : "sidebar.right")
                        }.buttonStyle(.plain).help(store.hideNavigationTitles ? AppLocalization.string("workbench.sidebar.expand", language: language) : AppLocalization.string("workbench.sidebar.iconsOnly", language: language))
                        SettingsLink { Image(systemName: "gearshape") }.buttonStyle(.plain).help(AppLocalization.string("workbench.settingsHelp", language: language))
                    }.padding(14).background(.bar)
                }
        } detail: {
            ToolRouter(id: store.selected)
                .navigationTitle(Catalog.localizedTool(store.selected, language: language).title)
                .toolbar {
                    ToolbarItemGroup {
                        Button { store.searchPresented = true } label: { Label(AppLocalization.string("workbench.search", language: language), systemImage: "magnifyingglass") }.help("\(AppLocalization.string("workbench.search", language: language)) · ⌘K")
                        if store.selected != "mootool" {
                            Button { store.togglePin(store.selected) } label: { Label(AppLocalization.string("workbench.pinned", language: language), systemImage: store.pinned.contains(store.selected) ? "star.fill" : "star") }.help(AppLocalization.string("workbench.pinHelp", language: language))
                            Button { store.historyPresented = true } label: { Label(AppLocalization.string("workbench.history", language: language), systemImage: "clock.arrow.circlepath") }
                            Button { openWindow(id: "tool", value: store.selected) } label: { Label(AppLocalization.string("workbench.detach", language: language), systemImage: "rectangle.on.rectangle") }
                        }
                    }
                }
        }.frame(minWidth: 940, minHeight: 630)
            .sheet(isPresented: $store.searchPresented) { CommandPalette().environment(store).environment(\.appLanguage, language) }
            .sheet(isPresented: $store.historyPresented) { HistoryView(toolID: store.selected).environment(store).environment(\.appLanguage, language) }
            .alert(AppLocalization.string("workbench.alert.title", language: language), isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) { Button(AppLocalization.string("workbench.alert.ok", language: language)) { store.error = nil } } message: { Text(store.error ?? "") }
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
    @Environment(\.appLanguage) private var language
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
                        Text(AppLocalization.string("home.tagline", language: language)).font(.system(size: 15)).foregroundStyle(.secondary)
                        Text(AppLocalization.string("home.byAuthor", language: language)).font(.caption).foregroundStyle(.tertiary)
                        Link("mootool.luoboduner.com ↗", destination: URL(string: "https://mootool.luoboduner.com")!).font(.caption)
                    }
                }
                homeSection(AppLocalization.string("home.section.about", language: language)) {
                    Text(AppLocalization.string("home.aboutBody", language: language)).font(.system(size: 13)).foregroundStyle(.secondary).lineSpacing(4)
                }
                homeSection(AppLocalization.string("home.section.contributors", language: language)) {
                    FlowLayout(spacing: 10) {
                        ForEach(contributors) { person in
                            Link(person.title, destination: URL(string: person.url)!).font(.system(size: 12, weight: .medium))
                        }
                    }
                    Text(AppLocalization.string("home.contributorsThanks", language: language)).font(.caption).foregroundStyle(.tertiary).padding(.top, 6)
                }
                homeSection(AppLocalization.string("home.section.sponsor", language: language)) {
                    Text(AppLocalization.string("home.sponsorText", language: language)).font(.system(size: 13)).foregroundStyle(.secondary)
                    if let sponsor = AppResources.bundle.url(forResource: "wx-zanshang", withExtension: "jpg") {
                        Image(nsImage: NSImage(contentsOf: sponsor)!).resizable().scaledToFit().frame(width: 139).clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                    Text(AppLocalization.string("home.sponsorCaption", language: language)).font(.caption).foregroundStyle(.tertiary)
                }
                homeSection(AppLocalization.string("home.section.source", language: language)) {
                    VStack(alignment: .leading, spacing: 6) {
                        Link("GitHub ↗", destination: URL(string: "https://github.com/rememberber/MooTool")!)
                        Link("Gitee ↗", destination: URL(string: "https://gitee.com/zhoubochina/MooTool")!)
                        Link("提交问题 ↗", destination: URL(string: "https://github.com/rememberber/MooTool/issues")!)
                    }
                }
                homeSection(AppLocalization.string("home.section.otherWorks", language: language)) {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(works, id: \.0) { work in
                            Link("\(work.0) ↗", destination: URL(string: work.2)!)
                            Text(work.1).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                Button { store.searchPresented = true } label: {
                    HStack { Image(systemName: "magnifyingglass"); Text(AppLocalization.string("home.searchTools", language: language)); Spacer(); Text("⌘ K").font(.system(size: 11, design: .monospaced)).padding(5).background(.quaternary.opacity(0.6), in: RoundedRectangle(cornerRadius: 5)) }
                        .foregroundStyle(.secondary).padding(16).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(.quaternary))
                }.buttonStyle(.plain)
                VStack(alignment: .leading, spacing: 13) {
                    Text(AppLocalization.string("workbench.pinned", language: language)).font(.headline)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 195), spacing: 12)], spacing: 12) {
                        ForEach(homePinnedTools) { tool in
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
                    Text(AppLocalization.string("home.forMac.title", language: language)).font(.headline)
                    Text(AppLocalization.string("home.forMac.body", language: language)).font(.system(size: 13)).foregroundStyle(.secondary).lineSpacing(5)
                    HStack(spacing: 18) {
                        Label(AppLocalization.string("home.localSave", language: language), systemImage: "internaldrive")
                        Label(AppLocalization.string("home.standaloneInstall", language: language), systemImage: "square.stack.3d.up")
                        Label(AppLocalization.string("home.nativeShortcuts", language: language), systemImage: "command")
                    }.font(.caption).foregroundStyle(.secondary)
                }
                Text("MIT License").font(.caption).foregroundStyle(.tertiary)
            }.frame(maxWidth: 820).padding(44).frame(maxWidth: .infinity)
        }.background(Color(nsColor: .windowBackgroundColor))
    }
    private var homePinnedTools: [Tool] {
        if store.pinned.isEmpty {
            return Array(Catalog.tools.dropFirst().prefix(6)).map { $0.localized(in: language) }
        }
        return store.pinned.map { Catalog.localizedTool($0, language: language) }
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
    @Environment(\.appLanguage) private var language
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var selection: String?
    @FocusState private var focused: Bool
    var results: [Tool] { Catalog.localizedTools(language).filter { $0.matches(query) } }
    var body: some View {
        VStack(spacing: 0) {
            HStack { Image(systemName: "magnifyingglass").foregroundStyle(.secondary); TextField(AppLocalization.string("app.search.placeholder", language: language), text: $query).textFieldStyle(.plain).font(.title3).focused($focused).onSubmit(open); Button(AppLocalization.string("palette.cancel", language: language)) { dismiss() }.keyboardShortcut(.cancelAction) }.padding(20)
            Divider()
            List(results, selection: $selection) { tool in
                HStack(spacing: 13) { Image(systemName: tool.symbol).frame(width: 25).foregroundStyle(.tint); VStack(alignment: .leading, spacing: 3) { Text(tool.title); Text(tool.subtitle).font(.caption).foregroundStyle(.secondary) }; Spacer(); Text(tool.group).font(.caption).foregroundStyle(.tertiary) }.padding(.vertical, 5).tag(tool.id)
                    .contentShape(Rectangle()).onTapGesture { store.select(tool.id); dismiss() }
            }.listStyle(.inset).onSubmit(open)
            HStack { Text(AppLocalization.string("palette.hint", language: language)); Spacer(); Text("\(results.count) \(AppLocalization.string("palette.count", language: language))") }.font(.caption).foregroundStyle(.secondary).padding(12)
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
    @Environment(\.appLanguage) private var language
    @Environment(\.dismiss) private var dismiss
    private var toolTitle: String { Catalog.localizedTool(toolID, language: language).title }
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Text("\(toolTitle) · \(AppLocalization.string("workbench.history", language: language))").font(.title2)
                Spacer()
                Button(AppLocalization.string("common.done", language: language)) { dismiss() }.keyboardShortcut(.cancelAction)
            }.padding()
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
                            } else if toolID == "timeConvert" {
                                Text(item.draft.input).lineLimit(1).font(.system(.body, design: .monospaced))
                                let unitKey = item.draft.mode == "millisecond" ? "timeConvert.unitMillisecond" : "timeConvert.unitSecond"
                                let unit = AppLocalization.string(unitKey, language: language)
                                Text("\(item.draft.secondary) · \(unit) · \(item.date.formatted())")
                                    .font(.caption).foregroundStyle(.secondary)
                            } else {
                                Text(item.draft.input.isEmpty ? item.draft.output : item.draft.input).lineLimit(2).font(.system(.body, design: .monospaced))
                                Text(item.date.formatted()).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button { if let index = store.history.firstIndex(where: { $0.id == item.id }) { store.history[index].favorite.toggle(); store.scheduleSave() } } label: { Image(systemName: item.favorite ? "star.fill" : "star") }
                        Button(AppLocalization.string("history.restore", language: language)) { store.restoreDraft(toolID, record: item.draft); dismiss() }.disabled(store.draft(toolID).busy)
                    }
                }
            }.overlay {
                if !store.history.contains(where: { $0.toolID == toolID }) {
                    ContentUnavailableView(
                        AppLocalization.string("history.empty.title", language: language),
                        systemImage: "clock",
                        description: Text(AppLocalization.string("history.empty.description", language: language)))
                }
            }
        }.frame(width: 680, height: 470)
    }
}
