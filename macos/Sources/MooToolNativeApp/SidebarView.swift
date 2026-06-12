import MooToolNativeCore
import SwiftUI

struct SidebarView: View {
    @Binding var selection: MooToolModule?

    var body: some View {
        List(MooToolModuleCatalog.previewModules, selection: $selection) { module in
            Label(module.title, systemImage: module.symbolName)
                .tag(module)
        }
        .navigationTitle("MooTool")
        .listStyle(.sidebar)
        .frame(minWidth: 220)
    }
}
