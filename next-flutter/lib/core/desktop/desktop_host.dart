import 'package:flutter/services.dart';

class DesktopCapabilities {
  DesktopCapabilities(
      {this.preventDisplaySleep = false,
      this.screenshot = false,
      this.screenColor = false,
      this.tray = false});
  final bool preventDisplaySleep;
  final bool screenshot;
  final bool screenColor;
  final bool tray;
}

abstract class DesktopHost {
  Future<DesktopCapabilities> capabilities();
  Future<bool> setPreventDisplaySleep(bool enabled);
  Future<Uint8List?> captureScreenRegion();
  Future<String?> pickScreenColor();
}

class ChannelDesktopHost implements DesktopHost {
  static const channel =
      MethodChannel('com.rememberber.mootool.next.flutter/desktop');

  @override
  Future<DesktopCapabilities> capabilities() async {
    try {
      final raw = await channel.invokeMethod<Map<Object?, Object?>>('capabilities');
      return DesktopCapabilities(
        preventDisplaySleep: raw?['preventDisplaySleep'] == true,
        screenshot: raw?['screenshot'] == true,
        screenColor: raw?['screenColor'] == true,
        tray: raw?['tray'] == true,
      );
    } on MissingPluginException {
      return DesktopCapabilities();
    }
  }

  @override
  Future<bool> setPreventDisplaySleep(bool enabled) async {
    try {
      return await channel.invokeMethod<bool>(
              'setPreventDisplaySleep', enabled) ??
          false;
    } on MissingPluginException {
      return false;
    } on PlatformException {
      return false;
    }
  }

  @override
  Future<Uint8List?> captureScreenRegion() async {
    try {
      final bytes = await channel.invokeMethod<Uint8List>('captureScreenRegion');
      return bytes;
    } on MissingPluginException {
      return null;
    } on PlatformException {
      return null;
    }
  }

  @override
  Future<String?> pickScreenColor() async {
    try {
      return await channel.invokeMethod<String>('pickScreenColor');
    } on MissingPluginException {
      return null;
    } on PlatformException {
      return null;
    }
  }
}

class MemoryDesktopHost implements DesktopHost {
  MemoryDesktopHost(
      {this.sleepWorks = true,
      this.screenshotBytes,
      this.pickedColor,
      DesktopCapabilities? caps})
      : caps = caps ??
            DesktopCapabilities(
                preventDisplaySleep: true,
                screenshot: true,
                screenColor: true);

  bool sleepWorks;
  bool sleepHeld = false;
  Uint8List? screenshotBytes;
  String? pickedColor;
  DesktopCapabilities caps;

  @override
  Future<DesktopCapabilities> capabilities() async => caps;

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
}
