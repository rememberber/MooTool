import MooToolNativeCore
import SwiftUI

struct HostEditorView: View {
    @State private var hostsText = """
    127.0.0.1 localhost
    # 0.0.0.0 ads.example.com
    192.168.1.2 api.local dev.local
    """

    private var entries: [HostEntry] {
        HostFileService.parse(hostsText)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Hosts 内容")
                    .font(.headline)
                Spacer()
                Button {
                    copy(HostFileService.render(entries))
                } label: {
                    Label("复制规范化内容", systemImage: "doc.on.doc")
                }
            }

            TextEditor(text: $hostsText)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 170)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }

            Text("解析结果：\(entries.count) 条")
                .font(.headline)

            List(entries) { entry in
                HStack(spacing: 10) {
                    Image(systemName: entry.isEnabled ? "checkmark.circle" : "pause.circle")
                        .foregroundStyle(entry.isEnabled ? .green : .secondary)
                    Text(entry.address)
                        .font(.system(.body, design: .monospaced))
                        .frame(width: 120, alignment: .leading)
                    Text(entry.hosts.joined(separator: ", "))
                        .textSelection(.enabled)
                    Spacer()
                }
            }
            .frame(minHeight: 170)
        }
    }

    private func copy(_ value: String) {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(value, forType: .string)
    }
}
