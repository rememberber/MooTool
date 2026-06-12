import MooToolNativeCore
import SwiftUI

struct ContentView: View {
    @State private var selection: MooToolModule? = MooToolModuleCatalog.defaultSelection

    var body: some View {
        NavigationSplitView {
            SidebarView(selection: $selection)
        } detail: {
            ModuleDetailView(module: selection)
        }
        .frame(minWidth: 960, minHeight: 620)
    }
}
