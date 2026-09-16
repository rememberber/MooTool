import SwiftUI
import MooToolNextCore

struct ToolFavoritesSheet: View {
    let kind: ToolFavoriteKind
    let currentValue: String
    let onApply: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(AppStore.self) private var store
    @State private var search = ""
    @State private var saveName = ""

    private var items: [SavedToolFavorite] {
        let all = store.toolFavorites.filter { $0.kind == kind }
        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return all }
        return all.filter {
            $0.name.localizedCaseInsensitiveContains(query) ||
            $0.value.localizedCaseInsensitiveContains(query) ||
            $0.folder.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(favoriteTitle).font(.title2)
                Spacer()
                Button("完成") { dismiss() }
            }
            TextField("搜索收藏", text: $search).textFieldStyle(.roundedBorder)
            HStack {
                TextField("保存名称", text: $saveName).textFieldStyle(.roundedBorder)
                Button("保存当前") { saveCurrent() }.disabled(currentValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            List {
                ForEach(items) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.name).font(.headline)
                            Text(item.folder).font(.caption).foregroundStyle(.secondary)
                            Text(item.value).font(.caption.monospaced()).lineLimit(2)
                        }
                        Spacer()
                        Button("使用") { onApply(item.value); dismiss() }
                        Button(role: .destructive) { delete(item) } label: { Image(systemName: "trash") }
                    }.padding(.vertical, 4)
                }
            }
            if items.isEmpty {
                Text("暂无收藏，可保存当前内容或从迁移导入。").font(.caption).foregroundStyle(.secondary)
            }
        }.padding(20).frame(width: 560, height: 480)
        .onAppear { if saveName.isEmpty { saveName = defaultName } }
    }

    private var favoriteTitle: String {
        switch kind {
        case .color: return "颜色收藏"
        case .regex: return "正则收藏"
        case .cron: return "Cron 收藏"
        }
    }

    private var defaultName: String {
        let trimmed = currentValue.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "收藏 \(store.toolFavorites.filter { $0.kind == kind }.count + 1)" }
        return String(trimmed.prefix(40))
    }

    private func saveCurrent() {
        do {
            let value = currentValue.trimmingCharacters(in: .whitespacesAndNewlines)
            let name = saveName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? defaultName : saveName
            var item = SavedToolFavorite(kind: kind, folder: "默认收藏夹", name: name, value: value)
            try item.validate()
            store.toolFavorites.append(item)
            store.scheduleSave()
        } catch {
            store.error = error.localizedDescription
        }
    }

    private func delete(_ item: SavedToolFavorite) {
        store.toolFavorites.removeAll { $0.id == item.id }
        store.scheduleSave()
    }
}
