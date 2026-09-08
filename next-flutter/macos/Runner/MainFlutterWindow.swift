import Cocoa
import FlutterMacOS
import IOKit.pwr_mgt

final class DesktopRuntime {
  static let shared = DesktopRuntime()

  var closeBehavior = "ask"
  var trayEnabled = true
  var windowHidden = false
  weak var window: MainFlutterWindow?
  var channel: FlutterMethodChannel?
  var statusItem: NSStatusItem?

  var trayAvailable: Bool { statusItem != nil }

  var effectiveClose: String {
    if closeBehavior == "hide" && !(trayEnabled && trayAvailable) {
      return "ask"
    }
    return closeBehavior
  }

  var shouldQuitWhenLastWindowCloses: Bool {
    effectiveClose == "quit" && !windowHidden
  }

  func apply(closeBehavior: String, trayEnabled: Bool, startMaximized: Bool, window: MainFlutterWindow) {
    self.closeBehavior = closeBehavior
    self.trayEnabled = trayEnabled
    self.window = window
    setTrayEnabled(trayEnabled)
    if startMaximized, let screen = window.screen ?? NSScreen.main {
      window.setFrame(screen.visibleFrame, display: true)
    }
  }

  @discardableResult
  func setTrayEnabled(_ enabled: Bool) -> Bool {
    if !enabled {
      if let item = statusItem {
        NSStatusBar.system.removeStatusItem(item)
        statusItem = nil
      }
      return true
    }
    if statusItem == nil {
      let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
      item.button?.title = "M"
      item.button?.toolTip = "MooTool Next Flutter"
      let menu = NSMenu()
      let show = NSMenuItem(title: "Show", action: #selector(MainFlutterWindow.showFromTray), keyEquivalent: "")
      show.target = window
      let quit = NSMenuItem(title: "Quit", action: #selector(MainFlutterWindow.quitFromTray), keyEquivalent: "q")
      quit.target = window
      menu.addItem(show)
      menu.addItem(quit)
      item.menu = menu
      statusItem = item
    }
    return statusItem != nil
  }
}

class MainFlutterWindow: NSWindow, NSWindowDelegate {
  private var sleepAssertion: IOPMAssertionID = 0

  override func awakeFromNib() {
    let flutterViewController = FlutterViewController()
    let windowFrame = self.frame
    self.contentViewController = flutterViewController
    self.setFrame(windowFrame, display: true)
    self.delegate = self

    RegisterGeneratedPlugins(registry: flutterViewController)
    registerDesktopChannel(flutterViewController)
    DesktopRuntime.shared.window = self

    super.awakeFromNib()
  }

  private func registerDesktopChannel(_ controller: FlutterViewController) {
    let channel = FlutterMethodChannel(
      name: "com.rememberber.mootool.next.flutter/desktop",
      binaryMessenger: controller.engine.binaryMessenger)
    DesktopRuntime.shared.channel = channel
    channel.setMethodCallHandler { [weak self] call, result in
      guard let self else {
        result(FlutterError(code: "UNAVAILABLE", message: "Window gone", details: nil))
        return
      }
      switch call.method {
      case "capabilities":
        result([
          "preventDisplaySleep": true,
          "screenshot": false,
          "screenColor": false,
          "tray": DesktopRuntime.shared.trayAvailable,
          "clipboardImage": true
        ])
      case "setPreventDisplaySleep":
        result(self.setPreventDisplaySleep(call.arguments as? Bool ?? false))
      case "readClipboardImage":
        if let data = self.readClipboardImage() {
          result(FlutterStandardTypedData(bytes: data))
        } else {
          result(nil)
        }
      case "applyWindowPolicy":
        let args = call.arguments as? [String: Any] ?? [:]
        DesktopRuntime.shared.apply(
          closeBehavior: args["closeBehavior"] as? String ?? "ask",
          trayEnabled: args["trayEnabled"] as? Bool ?? true,
          startMaximized: args["startMaximized"] as? Bool ?? false,
          window: self)
        result(true)
      case "performCloseAction":
        result(self.performCloseAction(call.arguments as? String ?? "cancel"))
      case "captureScreenRegion", "pickScreenColor":
        result(FlutterError(
          code: "UNIMPLEMENTED",
          message: "Screen capture/color pick is not wired on this host yet",
          details: nil))
      default:
        result(FlutterMethodNotImplemented)
      }
    }
  }

  func windowShouldClose(_ sender: NSWindow) -> Bool {
    switch DesktopRuntime.shared.effectiveClose {
    case "quit":
      return true
    case "hide":
      orderOut(nil)
      DesktopRuntime.shared.windowHidden = true
      return false
    default:
      DesktopRuntime.shared.channel?.invokeMethod("closeRequested", arguments: nil)
      return false
    }
  }

  @objc func showFromTray() {
    DesktopRuntime.shared.windowHidden = false
    makeKeyAndOrderFront(nil)
    NSApp.activate(ignoringOtherApps: true)
  }

  @objc func quitFromTray() {
    NSApp.terminate(nil)
  }

  private func performCloseAction(_ action: String) -> Bool {
    switch action {
    case "hide":
      orderOut(nil)
      DesktopRuntime.shared.windowHidden = true
      return true
    case "quit":
      NSApp.terminate(nil)
      return true
    default:
      DesktopRuntime.shared.windowHidden = false
      makeKeyAndOrderFront(nil)
      return true
    }
  }

  private func readClipboardImage() -> Data? {
    let pasteboard = NSPasteboard.general
    if let png = pasteboard.data(forType: .png) {
      return png
    }
    if let tiff = pasteboard.data(forType: .tiff),
       let image = NSImage(data: tiff),
       let tiffRep = image.tiffRepresentation,
       let bitmap = NSBitmapImageRep(data: tiffRep),
       let png = bitmap.representation(using: .png, properties: [:]) {
      return png
    }
    return nil
  }

  private func setPreventDisplaySleep(_ enabled: Bool) -> Bool {
    if sleepAssertion != 0 {
      IOPMAssertionRelease(sleepAssertion)
      sleepAssertion = 0
    }
    if !enabled {
      return true
    }
    let status = IOPMAssertionCreateWithName(
      kIOPMAssertionTypeNoDisplaySleep as CFString,
      IOPMAssertionLevel(kIOPMAssertionLevelOn),
      "MooTool Next Flutter message board" as CFString,
      &sleepAssertion)
    if status != kIOReturnSuccess {
      sleepAssertion = 0
      return false
    }
    return true
  }
}
