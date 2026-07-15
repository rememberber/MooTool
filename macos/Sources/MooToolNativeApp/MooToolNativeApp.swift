import SwiftUI

@main
struct MooToolNativeApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var settings = NativeAppSettings()

    var body: some Scene {
        WindowGroup("MooTool Native") {
            ContentView()
                .environment(settings)
        }
        .commands {
            CommandGroup(replacing: .appInfo) {
                Button("关于 MooTool Native") {
                    NSApplication.shared.orderFrontStandardAboutPanel(options: [
                        .applicationName: "MooTool Native",
                        .applicationVersion: "Preview"
                    ])
                }
            }
        }

        Settings {
            SettingsView()
                .environment(settings)
        }
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        if let appIconImage = NativeAppAssets.appIconImage {
            NSApplication.shared.applicationIconImage = appIconImage
        }
        NSApplication.shared.setActivationPolicy(.regular)
        NSApplication.shared.activate()
    }
}
