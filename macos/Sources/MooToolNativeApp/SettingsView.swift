import SwiftUI

struct SettingsView: View {
    @Environment(NativeAppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings

        Form {
            Toggle("跟随系统外观", isOn: $settings.followsSystemAppearance)
            Toggle("首次启动导入旧数据", isOn: $settings.importsLegacyDataOnFirstLaunch)
        }
        .formStyle(.grouped)
        .padding(24)
        .frame(width: 420)
    }
}
