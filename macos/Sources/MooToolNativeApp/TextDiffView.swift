import MooToolNativeCore
import SwiftUI

struct TextDiffView: View {
    @State private var original = "alpha\nbeta\ngamma"
    @State private var revised = "alpha\nbeta changed\ngamma\ndelta"

    private var diff: TextDiffResult {
        TextDiffService.diff(original: original, revised: revised)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                diffEditor(title: "原文", text: $original)
                diffEditor(title: "新文本", text: $revised)
            }

            HStack(spacing: 14) {
                Label("\(diff.summary.unchanged)", systemImage: "equal")
                    .foregroundStyle(.secondary)
                Label("+\(diff.summary.added)", systemImage: "plus")
                    .foregroundStyle(.green)
                Label("-\(diff.summary.removed)", systemImage: "minus")
                    .foregroundStyle(.red)
            }

            List(diff.lines) { line in
                HStack(alignment: .top, spacing: 10) {
                    Text(prefix(for: line.kind))
                        .font(.system(.body, design: .monospaced))
                        .foregroundStyle(color(for: line.kind))
                        .frame(width: 18, alignment: .leading)
                    Text(line.text)
                        .font(.system(.body, design: .monospaced))
                        .textSelection(.enabled)
                    Spacer(minLength: 0)
                }
                .padding(.vertical, 2)
            }
            .frame(minHeight: 230)
        }
    }

    private func diffEditor(title: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
            TextEditor(text: text)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 150)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }
        }
    }

    private func prefix(for kind: TextDiffLineKind) -> String {
        switch kind {
        case .unchanged:
            " "
        case .added:
            "+"
        case .removed:
            "-"
        }
    }

    private func color(for kind: TextDiffLineKind) -> Color {
        switch kind {
        case .unchanged:
            .secondary
        case .added:
            .green
        case .removed:
            .red
        }
    }
}
