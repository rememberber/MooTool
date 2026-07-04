import AppKit

enum NativeAppAssets {
    static var appIconImage: NSImage? {
        image(named: "AppIcon", extension: "icns")
    }

    static var logoImage: NSImage? {
        image(named: "logo-128", extension: "png")
    }

    private static func image(named name: String, extension fileExtension: String) -> NSImage? {
        guard let url = Bundle.module.url(forResource: name, withExtension: fileExtension) else {
            return nil
        }
        return NSImage(contentsOf: url)
    }
}
