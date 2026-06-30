import MooToolNativeCore
import SwiftUI

struct EncodingConverterView: View {
    @State private var mode: EncodingMode = .base64
    @State private var operation: EncodingOperation = .encode
    @State private var input = "MooTool 原生版"
    @State private var output = ""
    @State private var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Picker("类型", selection: $mode) {
                    ForEach(EncodingMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 220)

                Picker("操作", selection: $operation) {
                    ForEach(EncodingOperation.allCases) { operation in
                        Text(operation.title).tag(operation)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 180)
            }

            Text("输入")
                .font(.headline)

            TextEditor(text: $input)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 120)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }

            HStack(spacing: 10) {
                Button(operation.title) {
                    runConversion()
                }
                Button("交换") {
                    input = output
                    output = ""
                    errorMessage = nil
                }
                .disabled(output.isEmpty)
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
                .frame(minHeight: 150)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }
        }
    }

    private func runConversion() {
        do {
            switch (mode, operation) {
            case (.base64, .encode):
                output = EncodingConversionService.base64Encode(input)
            case (.base64, .decode):
                output = try EncodingConversionService.base64Decode(input)
            case (.url, .encode):
                output = EncodingConversionService.urlEncode(input)
            case (.url, .decode):
                output = try EncodingConversionService.urlDecode(input)
            }
            errorMessage = nil
        } catch EncodingConversionError.invalidBase64 {
            output = ""
            errorMessage = "Base64 内容无法解码"
        } catch EncodingConversionError.invalidPercentEncoding {
            output = ""
            errorMessage = "URL 百分号编码不完整"
        } catch {
            output = ""
            errorMessage = "无法完成转换"
        }
    }

    private func copy(_ value: String) {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(value, forType: .string)
    }
}

private enum EncodingMode: String, CaseIterable, Identifiable {
    case base64
    case url

    var id: String { rawValue }

    var title: String {
        switch self {
        case .base64:
            "Base64"
        case .url:
            "URL"
        }
    }
}

private enum EncodingOperation: String, CaseIterable, Identifiable {
    case encode
    case decode

    var id: String { rawValue }

    var title: String {
        switch self {
        case .encode:
            "编码"
        case .decode:
            "解码"
        }
    }
}
