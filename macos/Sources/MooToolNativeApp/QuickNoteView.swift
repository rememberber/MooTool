import MooToolNativeCore
import SwiftUI

struct QuickNoteView: View {
    @AppStorage("quickNote.title") private var title = "随手记"
    @AppStorage("quickNote.content") private var content = "记录 macOS 原生版迁移事项"

    private var summary: QuickNoteSummary {
        QuickNoteService.summary(for: QuickNoteDraft(title: title, content: content))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TextField("标题", text: $title)
                .textFieldStyle(.roundedBorder)
                .font(.title3)

            TextEditor(text: $content)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 300)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }

            Grid(alignment: .leading, horizontalSpacing: 18, verticalSpacing: 8) {
                GridRow {
                    Text("标题")
                        .foregroundStyle(.secondary)
                    Text(summary.title)
                }
                GridRow {
                    Text("行数")
                        .foregroundStyle(.secondary)
                    Text("\(summary.lineCount)")
                }
                GridRow {
                    Text("字符")
                        .foregroundStyle(.secondary)
                    Text("\(summary.characterCount)")
                }
                GridRow {
                    Text("预览")
                        .foregroundStyle(.secondary)
                    Text(summary.preview)
                }
            }
            .padding(14)
            .background(.background.secondary, in: RoundedRectangle(cornerRadius: 8))
        }
    }
}
