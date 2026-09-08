import Cocoa
import FlutterMacOS
import IOKit.pwr_mgt

class MainFlutterWindow: NSWindow {
  private var sleepAssertion: IOPMAssertionID = 0

  override func awakeFromNib() {
    let flutterViewController = FlutterViewController()
    let windowFrame = self.frame
    self.contentViewController = flutterViewController
    self.setFrame(windowFrame, display: true)

    RegisterGeneratedPlugins(registry: flutterViewController)
    registerDesktopChannel(flutterViewController)

    super.awakeFromNib()
  }

  private func registerDesktopChannel(_ controller: FlutterViewController) {
    let channel = FlutterMethodChannel(
      name: "com.rememberber.mootool.next.flutter/desktop",
      binaryMessenger: controller.engine.binaryMessenger)
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
          "tray": false
        ])
      case "setPreventDisplaySleep":
        result(self.setPreventDisplaySleep(call.arguments as? Bool ?? false))
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
