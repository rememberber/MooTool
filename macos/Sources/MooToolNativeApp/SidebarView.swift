import MooToolNativeCore
import SwiftUI

struct SidebarView: View {
    @Binding var selection: MooToolModule?

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                if let logoImage = NativeAppAssets.logoImage {
                    Image(nsImage: logoImage)
                        .resizable()
                        .frame(width: 34, height: 34)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("MooTool")
                        .font(.headline)
                    Text("Native Preview")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)

            Divider()

            List(MooToolModuleCatalog.previewModules, selection: $selection) { module in
                Label(module.title, systemImage: module.symbolName)
                    .tag(module)
            }
            .listStyle(.sidebar)
        }
        .navigationTitle("MooTool")
        .frame(minWidth: 220)
    }
}
