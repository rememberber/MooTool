import SwiftUI
import MooToolNextCore

struct ToolFavoritesSheet: View {
    let kind: ToolFavoriteKind
    let currentValue: String
    let onApply: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    @Environment(AppStore.self) private var store
    @State private var search = ""
    @State private var saveName = ""

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }

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
                Button(loc("common.done")) { dismiss() }
            }
            TextField(loc("favorites.search"), text: $search).textFieldStyle(.roundedBorder)
            HStack {
                TextField(loc("favorites.saveName"), text: $saveName).textFieldStyle(.roundedBorder)
                Button(loc("favorites.saveCurrent")) { saveCurrent() }.disabled(currentValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
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
                        Button(loc("favorites.use")) { onApply(item.value); dismiss() }
                        Button(role: .destructive) { delete(item) } label: { Image(systemName: "trash") }
                    }.padding(.vertical, 4)
                }
            }
            if items.isEmpty {
                Text(loc("favorites.empty")).font(.caption).foregroundStyle(.secondary)
            }
        }.padding(20).frame(width: 560, height: 480)
        .onAppear { if saveName.isEmpty { saveName = defaultName } }
    }

    private var favoriteTitle: String {
        switch kind {
        case .color: return loc("favorites.color")
        case .regex: return loc("favorites.regex")
        case .cron: return loc("favorites.cron")
        }
    }

    private var defaultName: String {
        let trimmed = currentValue.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return locf("favorites.numberedName", store.toolFavorites.filter { $0.kind == kind }.count + 1)
        }
        return String(trimmed.prefix(40))
    }

    private func saveCurrent() {
        do {
            let value = currentValue.trimmingCharacters(in: .whitespacesAndNewlines)
            let name = saveName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? defaultName : saveName
            var item = SavedToolFavorite(kind: kind, folder: loc("favorites.defaultFolder"), name: name, value: value)
            try item.validate(language: language)
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
