import MooToolNativeCore
import SwiftUI

struct JSONFormatterView: View {
    @State private var input = #"{"name":"MooTool","native":true}"#
    @State private var output = ""
    @State private var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("JSON 输入")
                .font(.headline)

            TextEditor(text: $input)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 150)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }

            HStack(spacing: 10) {
                Button("格式化") {
                    runJSONAction(JSONFormattingService.format)
                }
                Button("压缩") {
                    runJSONAction(JSONFormattingService.minify)
                }
                Button {
                    copy(output)
                } label: {
                    Label("复制结果", systemImage: "doc.on.doc")
                }
                .disabled(output.isEmpty)
            }

            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle")
                    .foregroundStyle(.red)
            }

            Text("结果")
                .font(.headline)

            TextEditor(text: $output)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 190)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }
        }
    }

    private func runJSONAction(_ action: (String) throws -> String) {
        do {
            output = try action(input)
            errorMessage = nil
        } catch {
            output = ""
            errorMessage = "JSON 无法解析"
        }
    }

    private func copy(_ value: String) {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(value, forType: .string)
    }
}
