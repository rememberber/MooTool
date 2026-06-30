import MooToolNativeCore
import SwiftUI

struct ModuleDetailView: View {
    let module: MooToolModule?

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            if let module {
                Label(module.title, systemImage: module.symbolName)
                    .font(.title2)
                    .symbolRenderingMode(.hierarchical)

                Text(statusText(for: module.status))
                    .foregroundStyle(.secondary)

                Divider()

                detailContent(for: module)
            } else {
                Text("请选择模块")
                    .foregroundStyle(.secondary)
            }
        }
        .padding(28)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func statusText(for status: MooToolModuleStatus) -> String {
        switch status {
        case .planned:
            "计划中"
        case .preview:
            "预览"
        }
    }

    @ViewBuilder
    private func detailContent(for module: MooToolModule) -> some View {
        switch module.id {
        case "json":
            JSONFormatterView()
        case "time":
            TimeConverterView()
        case "encoding":
            EncodingConverterView()
        case "regex":
            RegexTesterView()
        default:
            Text("原生版预览骨架")
                .font(.headline)

            Text("该模块将在骨架通过后分阶段迁移。")
                .foregroundStyle(.secondary)
        }
    }
}
