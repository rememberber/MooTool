import MooToolNativeCore
import SwiftUI

struct RegexTesterView: View {
    @State private var pattern = #"(\w+)@([\w.]+)"#
    @State private var input = "dev@mootool.app admin@example.com"
    @State private var result = RegexEvaluationResult(matches: [])
    @State private var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("表达式")
                .font(.headline)

            TextField("输入正则表达式", text: $pattern)
                .textFieldStyle(.roundedBorder)
                .font(.system(.body, design: .monospaced))

            Text("测试文本")
                .font(.headline)

            TextEditor(text: $input)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 140)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }

            HStack(spacing: 10) {
                Button("匹配") {
                    runRegex()
                }
                Button {
                    copy(result.matches.map(\.text).joined(separator: "\n"))
                } label: {
                    Label("复制匹配", systemImage: "doc.on.doc")
                }
                .disabled(result.matches.isEmpty)
            }

            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle")
                    .foregroundStyle(.red)
            }

            Text("匹配结果")
                .font(.headline)

            if result.matches.isEmpty {
                ContentUnavailableView("暂无匹配", systemImage: "text.magnifyingglass")
                    .frame(maxWidth: .infinity, minHeight: 160)
            } else {
                List(result.matches.indices, id: \.self) { index in
                    let match = result.matches[index]
                    VStack(alignment: .leading, spacing: 6) {
                        Text(match.text)
                            .font(.system(.body, design: .monospaced))
                            .textSelection(.enabled)
                        Text("range: \(match.range.location), \(match.range.length)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        if !match.captureGroups.isEmpty {
                            Text("groups: \(match.captureGroups.joined(separator: ", "))")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .textSelection(.enabled)
                        }
                    }
                    .padding(.vertical, 4)
                }
                .frame(minHeight: 180)
            }
        }
        .onAppear {
            runRegex()
        }
    }

    private func runRegex() {
        do {
            result = try RegexMatchService.evaluate(pattern: pattern, input: input)
            errorMessage = nil
        } catch {
            result = RegexEvaluationResult(matches: [])
            errorMessage = "正则表达式无法解析"
        }
    }

    private func copy(_ value: String) {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(value, forType: .string)
    }
}
