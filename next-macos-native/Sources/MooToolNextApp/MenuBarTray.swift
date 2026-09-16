import AppKit
import SwiftUI
import MooToolNextCore

extension Notification.Name {
    static let menuBarTrayRefresh = Notification.Name("mootool.native.tray.refresh")
}

@MainActor
final class MenuBarTray {
    private var statusItem: NSStatusItem?
    private weak var store: AppStore?

    func attach(store: AppStore) {
        self.store = store
        refresh()
        NotificationCenter.default.addObserver(self, selector: #selector(refreshFromDefaults), name: UserDefaults.didChangeNotification, object: nativeDefaults)
        NotificationCenter.default.addObserver(self, selector: #selector(refreshFromDefaults), name: .menuBarTrayRefresh, object: nil)
    }

    @objc private func refreshFromDefaults() { refresh() }

    func refresh() {
        let enabled = nativeDefaults.object(forKey: "general.trayEnabled") as? Bool ?? true
        guard enabled else {
            if let statusItem { NSStatusBar.system.removeStatusItem(statusItem) }
            statusItem = nil
            return
        }
        let item = statusItem ?? NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        statusItem = item
        item.length = NSStatusItem.squareLength
        if let url = AppResources.bundle.url(forResource: "Brand", withExtension: "png"),
           let image = NSImage(contentsOf: url) {
            image.size = NSSize(width: 18, height: 18)
            image.isTemplate = true
            item.button?.image = image
        } else {
            item.button?.title = "M"
        }
        item.button?.toolTip = Product.name
        item.menu = buildMenu()
    }

    private func buildMenu() -> NSMenu {
        let menu = NSMenu()
        menu.addItem(actionItem("打开 MooTool", #selector(openMain)))
        menu.addItem(actionItem("设置…", #selector(openSettings)))
        menu.addItem(.separator())
        menu.addItem(actionItem("屏幕取色", #selector(pickColor)))
        menu.addItem(actionItem("区域截图", #selector(captureScreenshot)))
        menu.addItem(actionItem("翻译", #selector(openTranslation)))
        if let store, !store.hostProfiles.isEmpty {
            menu.addItem(.separator())
            for profile in store.hostProfiles {
                let item = NSMenuItem(title: profile.name, action: #selector(selectHost(_:)), keyEquivalent: "")
                item.target = self
                item.representedObject = profile.id
                item.state = store.draft("host").option == profile.id.uuidString ? .on : .off
                menu.addItem(item)
            }
        }
        menu.addItem(.separator())
        menu.addItem(actionItem("退出 MooTool", #selector(quit)))
        return menu
    }

    private func actionItem(_ title: String, _ action: Selector) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: action, keyEquivalent: "")
        item.target = self
        return item
    }

    @objc private func openMain() {
        activateMain()
    }

    @objc private func openSettings() {
        activateMain()
        NSApp.sendAction(Selector(("showSettingsWindow:")), to: nil, from: nil)
    }

    @objc private func pickColor() {
        activateMain()
        store?.select("colorBoard")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self] in
            guard self?.store != nil else { return }
            NSColorSampler().show { color in
                guard let color, let store = self?.store else { return }
                let draft = store.draft("colorBoard")
                guard let rgb = color.usingColorSpace(.sRGB) else { return }
                draft.input = String(format: "#%02X%02X%02X", Int(rgb.redComponent * 255), Int(rgb.greenComponent * 255), Int(rgb.blueComponent * 255))
                draft.status = "已从托盘取色"
                draft.error = nil
            }
        }
    }

    @objc private func captureScreenshot() {
        activateMain()
        store?.select("image")
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("mootool-tray-capture-\(UUID().uuidString).png")
        Task { @MainActor [weak self] in
            do {
                _ = try await ProcessRunner.run(executable: "/usr/sbin/screencapture", arguments: ["-i", "-x", url.path], timeout: 120)
                guard FileManager.default.fileExists(atPath: url.path), let store = self?.store else { return }
                let draft = store.draft("image")
                draft.media = draft.media ?? MediaWorkspaceState()
                draft.media?.filePaths = [url.path]
                draft.status = "已导入截图"
                draft.error = nil
            } catch {
                self?.store?.draft("image").error = error.localizedDescription
            }
        }
    }

    @objc private func openTranslation() {
        activateMain()
        store?.select("translation")
    }

    @objc private func selectHost(_ sender: NSMenuItem) {
        guard let id = sender.representedObject as? UUID, let store, let profile = store.hostProfiles.first(where: { $0.id == id }) else { return }
        activateMain()
        let draft = store.draft("host")
        draft.input = profile.content
        draft.option = profile.id.uuidString
        draft.status = "已切换配置：\(profile.name)"
        store.select("host")
        refresh()
    }

    @objc private func quit() {
        NSApp.terminate(nil)
    }

    private func activateMain() {
        NSApp.activate(ignoringOtherApps: true)
        if let window = NSApp.windows.first(where: { $0.canBecomeMain && $0.frame.width > 500 }) {
            window.makeKeyAndOrderFront(nil)
        }
    }
}
