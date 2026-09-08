import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class DesktopCapabilities {
  DesktopCapabilities(
      {this.preventDisplaySleep = false,
      this.screenshot = false,
      this.screenColor = false,
      this.tray = false,
      this.clipboardImage = false});
  final bool preventDisplaySleep;
  final bool screenshot;
  final bool screenColor;
  final bool tray;
  final bool clipboardImage;
}

abstract class DesktopHost {
  void Function()? onCloseRequested;

  Future<DesktopCapabilities> capabilities();
  Future<bool> setPreventDisplaySleep(bool enabled);
  Future<Uint8List?> captureScreenRegion();
  Future<String?> pickScreenColor();
  Future<Uint8List?> readClipboardImage();
  Future<bool> applyWindowPolicy({
    required String closeBehavior,
    required bool trayEnabled,
    required bool startMaximized,
  });
  Future<bool> performCloseAction(String action);
}

class ChannelDesktopHost implements DesktopHost {
  ChannelDesktopHost() {
    _listenIfPossible();
  }

  static const channel =
      MethodChannel('com.rememberber.mootool.next.flutter/desktop');

  bool _listening = false;

  @override
  void Function()? onCloseRequested;

  void _listenIfPossible() {
    if (_listening) return;
    if (BindingBase.debugBindingType() == null) return;
    _listening = true;
    channel.setMethodCallHandler((call) async {
      if (call.method == 'closeRequested') {
        onCloseRequested?.call();
      }
    });
  }

  bool get _channelReady => BindingBase.debugBindingType() != null;

  Future<T?> _invoke<T>(String method, [dynamic arguments]) async {
    if (!_channelReady) return null;
    try {
      return await channel.invokeMethod<T>(method, arguments);
    } on MissingPluginException {
      return null;
    } on PlatformException {
      return null;
    }
  }

  @override
  Future<DesktopCapabilities> capabilities() async {
    final raw = await _invoke<Map<Object?, Object?>>('capabilities');
    if (raw == null) return DesktopCapabilities();
    return DesktopCapabilities(
      preventDisplaySleep: raw['preventDisplaySleep'] == true,
      screenshot: raw['screenshot'] == true,
      screenColor: raw['screenColor'] == true,
      tray: raw['tray'] == true,
      clipboardImage: raw['clipboardImage'] == true,
    );
  }

  @override
  Future<bool> setPreventDisplaySleep(bool enabled) async {
    return await _invoke<bool>('setPreventDisplaySleep', enabled) ?? false;
  }

  @override
  Future<Uint8List?> captureScreenRegion() async {
    return _invoke<Uint8List>('captureScreenRegion');
  }

  @override
  Future<String?> pickScreenColor() async {
    return _invoke<String>('pickScreenColor');
  }

  @override
  Future<Uint8List?> readClipboardImage() async {
    return _invoke<Uint8List>('readClipboardImage');
  }

  @override
  Future<bool> applyWindowPolicy({
    required String closeBehavior,
    required bool trayEnabled,
    required bool startMaximized,
  }) async {
    _listenIfPossible();
    return await _invoke<bool>('applyWindowPolicy', {
          'closeBehavior': closeBehavior,
          'trayEnabled': trayEnabled,
          'startMaximized': startMaximized,
        }) ??
        false;
  }

  @override
  Future<bool> performCloseAction(String action) async {
    return await _invoke<bool>('performCloseAction', action) ?? false;
  }
}

class MemoryDesktopHost implements DesktopHost {
  MemoryDesktopHost(
      {this.sleepWorks = true,
      this.screenshotBytes,
      this.pickedColor,
      this.clipboardBytes,
      this.trayWorks = true,
      DesktopCapabilities? caps})
      : caps = caps ??
            DesktopCapabilities(
                preventDisplaySleep: true,
                screenshot: true,
                screenColor: true,
                tray: true,
                clipboardImage: true);

  bool sleepWorks;
  bool sleepHeld = false;
  Uint8List? screenshotBytes;
  String? pickedColor;
  Uint8List? clipboardBytes;
  bool trayWorks;
  DesktopCapabilities caps;
  String closeBehavior = 'ask';
  bool trayEnabled = true;
  bool startMaximized = false;
  bool windowHidden = false;
  bool windowMaximized = false;
  String? lastCloseAction;

  @override
  void Function()? onCloseRequested;

  @override
  Future<DesktopCapabilities> capabilities() async => DesktopCapabilities(
        preventDisplaySleep: caps.preventDisplaySleep,
        screenshot: caps.screenshot,
        screenColor: caps.screenColor,
        tray: trayEnabled && trayWorks,
        clipboardImage: caps.clipboardImage,
      );

  @override
  Future<bool> setPreventDisplaySleep(bool enabled) async {
    if (!sleepWorks) return false;
    sleepHeld = enabled;
    return true;
  }

  @override
  Future<Uint8List?> captureScreenRegion() async => screenshotBytes;

  @override
  Future<String?> pickScreenColor() async => pickedColor;

  @override
  Future<Uint8List?> readClipboardImage() async => clipboardBytes;

  @override
  Future<bool> applyWindowPolicy({
    required String closeBehavior,
    required bool trayEnabled,
    required bool startMaximized,
  }) async {
    this.closeBehavior = closeBehavior;
    this.trayEnabled = trayEnabled && trayWorks;
    this.startMaximized = startMaximized;
    if (startMaximized) windowMaximized = true;
    return true;
  }

  @override
  Future<bool> performCloseAction(String action) async {
    lastCloseAction = action;
    windowHidden = action == 'hide';
    return action != 'cancel';
  }

  void simulateCloseRequested() => onCloseRequested?.call();
}
