import MooToolNativeCore
import SwiftUI

struct TimeConverterView: View {
    @State private var timestampInput = String(TimeConversionService.currentTimestamp().seconds)
    @State private var errorMessage: String?

    private var conversionResult: TimeConversionResult? {
        do {
            return try TimeConversionService.convertTimestamp(timestampInput)
        } catch {
            return nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Unix 时间戳")
                    .font(.headline)
                HStack(spacing: 10) {
                    TextField("输入秒或毫秒时间戳", text: $timestampInput)
                        .textFieldStyle(.roundedBorder)
                        .font(.system(.body, design: .monospaced))
                        .onChange(of: timestampInput) {
                            errorMessage = nil
                        }

                    Button("当前时间") {
                        let current = TimeConversionService.currentTimestamp()
                        timestampInput = String(current.seconds)
                        errorMessage = nil
                    }
                }
            }

            if let conversionResult {
                resultGrid(conversionResult)
            } else {
                ContentUnavailableView("无法解析时间戳", systemImage: "exclamationmark.triangle", description: Text(errorMessage ?? "请输入 Unix 秒或毫秒时间戳。"))
                    .frame(maxWidth: .infinity, minHeight: 220)
            }

            Spacer(minLength: 0)
        }
    }

    private func resultGrid(_ result: TimeConversionResult) -> some View {
        Grid(alignment: .leading, horizontalSpacing: 18, verticalSpacing: 14) {
            resultRow(title: "本地时间", value: result.formattedText)
            resultRow(title: "秒", value: String(result.seconds))
            resultRow(title: "毫秒", value: String(result.milliseconds))
        }
        .padding(16)
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 8))
    }

    private func resultRow(title: String, value: String) -> some View {
        GridRow {
            Text(title)
                .foregroundStyle(.secondary)
                .frame(width: 72, alignment: .leading)

            Text(value)
                .font(.system(.body, design: .monospaced))
                .textSelection(.enabled)

            Button {
                NSPasteboard.general.clearContents()
                NSPasteboard.general.setString(value, forType: .string)
            } label: {
                Image(systemName: "doc.on.doc")
            }
            .buttonStyle(.borderless)
            .help("复制")
        }
    }
}
