import '../../app/settings.dart';

enum CloseDecision { cancel, hide, quit }

class WindowPolicy {
  static CloseBehavior effectiveClose({
    required CloseBehavior requested,
    required bool trayEnabled,
    required bool trayAvailable,
  }) {
    if (requested == CloseBehavior.hide && !(trayEnabled && trayAvailable)) {
      return CloseBehavior.ask;
    }
    return requested;
  }

  static CloseDecision resolve({
    required CloseBehavior effective,
    CloseDecision? asked,
  }) {
    return switch (effective) {
      CloseBehavior.quit => CloseDecision.quit,
      CloseBehavior.hide => CloseDecision.hide,
      CloseBehavior.ask => asked ?? CloseDecision.cancel,
    };
  }
}

String clipboardImageFileName(List<int> bytes) {
  if (bytes.length >= 3 &&
      bytes[0] == 0xFF &&
      bytes[1] == 0xD8 &&
      bytes[2] == 0xFF) {
    return 'clipboard.jpg';
  }
  if (bytes.length >= 6 &&
      bytes[0] == 0x47 &&
      bytes[1] == 0x49 &&
      bytes[2] == 0x46) {
    return 'clipboard.gif';
  }
  if (bytes.length >= 12 &&
      bytes[0] == 0x52 &&
      bytes[8] == 0x57 &&
      bytes[9] == 0x45 &&
      bytes[10] == 0x42 &&
      bytes[11] == 0x50) {
    return 'clipboard.webp';
  }
  return 'clipboard.png';
}
