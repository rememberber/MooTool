import SwiftUI
import MooToolNextCore

struct TranslationWordBook: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var selectedID: UUID?
    @State private var search = ""
    @State private var sourceText = ""
    @State private var targetText = ""
    @State private var sourceLang = "auto"
    @State private var targetLang = "zh-Hans"
    @State private var remark = ""

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }

    private var filtered: [SavedTranslationWord] {
        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return store.translationWords }
        return store.translationWords.filter {
            $0.sourceText.localizedCaseInsensitiveContains(query) ||
            $0.targetText.localizedCaseInsensitiveContains(query) ||
            $0.remark.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        PersistedHSplit(toolID: "translation-words", defaultLeading: 220, minLeading: 170, maxLeading: 360) {
            VStack(alignment: .leading, spacing: 8) {
                TextField(loc("translation.searchWords"), text: $search).textFieldStyle(.roundedBorder)
                List {
                    ForEach(filtered) { word in
                        Button {
                            selectedID = word.id
                            load(word)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(word.sourceText).lineLimit(1)
                                Text(word.targetText).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                            }
                        }.buttonStyle(.plain)
                    }
                }.listStyle(.inset)
            }
        } trailing: {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    TextField(loc("translation.sourceLanguage"), text: $sourceLang).textFieldStyle(.roundedBorder).frame(width: 100)
                    TextField(loc("translation.targetLanguage"), text: $targetLang).textFieldStyle(.roundedBorder).frame(width: 100)
                    Spacer()
                    Button(AppLocalization.string("tool.new", language: language)) { createWord() }
                    Button(AppLocalization.string("tool.save", language: language)) { saveWord() }.disabled(selectedID == nil && sourceText.isEmpty)
                    Button(AppLocalization.string("tool.delete", language: language)) { deleteWord() }.disabled(selectedID == nil)
                    Button(AppLocalization.string("translation.applyToTranslate", language: language)) { applyToDraft() }.disabled(sourceText.isEmpty)
                }.font(.caption)
                EditorPane(title: AppLocalization.string("translation.source", language: language), text: $sourceText)
                EditorPane(title: AppLocalization.string("translation.target", language: language), text: $targetText)
                TextField(loc("translation.remark"), text: $remark).textFieldStyle(.roundedBorder)
            }
        }
        .onAppear { bootstrap() }
    }

    private func bootstrap() {
        if selectedID == nil { selectedID = store.translationWords.first?.id }
        if let id = selectedID, let word = store.translationWords.first(where: { $0.id == id }) { load(word) }
    }

    private func load(_ word: SavedTranslationWord) {
        sourceText = word.sourceText
        targetText = word.targetText
        sourceLang = word.sourceLang
        targetLang = word.targetLang
        remark = word.remark
    }

    private func createWord() {
        let word = SavedTranslationWord(sourceText: "", targetText: "", sourceLang: sourceLang, targetLang: targetLang)
        store.translationWords.append(word)
        selectedID = word.id
        load(word)
        store.scheduleSave()
    }

    private func saveWord() {
        do {
            var word = SavedTranslationWord(sourceText: sourceText, targetText: targetText, sourceLang: sourceLang, targetLang: targetLang, remark: remark)
            if let id = selectedID, let index = store.translationWords.firstIndex(where: { $0.id == id }) {
                word.id = id
                word.modified = Date()
                try word.validate(language: language)
                store.translationWords[index] = word
            } else {
                try word.validate(language: language)
                store.translationWords.append(word)
                selectedID = word.id
            }
            store.scheduleSave()
        } catch {
            draft.error = error.localizedDescription
        }
    }

    private func deleteWord() {
        guard let id = selectedID else { return }
        store.translationWords.removeAll { $0.id == id }
        selectedID = store.translationWords.first?.id
        if let first = store.translationWords.first { load(first) }
        else { sourceText = ""; targetText = ""; remark = "" }
        store.scheduleSave()
    }

    private func applyToDraft() {
        draft.input = sourceText
        draft.output = targetText
        draft.mode = targetLang
        draft.option = sourceLang
        draft.status = loc("translation.status.fromWordBook")
    }
}

struct TranslationHistoryPane: View {
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @Bindable var draft: ToolDraft
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private var items: [HistoryRecord] { store.history.filter { $0.toolID == "translation" } }

    var body: some View {
        List(items) { item in
            Button {
                draft.input = item.draft.input
                draft.output = item.draft.output
                draft.mode = item.draft.mode
                draft.option = item.draft.option
                draft.status = loc("translation.status.fromHistory")
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.draft.input).lineLimit(2)
                    Text(item.draft.output).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                    Text(item.date.formatted(date: .abbreviated, time: .shortened)).font(.caption2).foregroundStyle(.tertiary)
                }
            }.buttonStyle(.plain)
        }.overlay {
            if items.isEmpty {
                ContentUnavailableView(loc("translation.history.emptyTitle"), systemImage: "clock", description: Text(loc("translation.history.emptyDescription")))
            }
        }
    }
}
