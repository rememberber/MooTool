import 'package:timezone/data/latest.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

typedef TimestampUnit = String;

class TimeConvertResult {
  TimeConvertResult(
      {required this.localTime,
      required this.unit,
      required this.milliseconds});
  final String localTime;
  final String unit;
  final int milliseconds;
}

const commonTimezones = [
  'UTC',
  'Asia/Shanghai',
  'Asia/Tokyo',
  'Asia/Seoul',
  'Asia/Singapore',
  'Asia/Hong_Kong',
  'Asia/Kolkata',
  'Asia/Dubai',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Moscow',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'Australia/Sydney',
  'Pacific/Auckland',
];

const quickTimezones = [
  ('UTC', 'UTC'),
  ('+8', 'Asia/Shanghai'),
  ('+9', 'Asia/Tokyo'),
  ('-5', 'America/New_York'),
  ('-8', 'America/Los_Angeles'),
  ('+1', 'Europe/Paris'),
  ('+3', 'Europe/Moscow'),
];

class TimeEngine {
  TimeEngine() {
    _ensureTimezones();
  }

  static var _initialized = false;
  static void _ensureTimezones() {
    if (_initialized) return;
    tzdata.initializeTimeZones();
    _initialized = true;
  }

  TimeConvertResult timestampToLocal(String input, String unit, String zone) {
    final normalized = input.trim();
    if (!RegExp(r'^-?\d+$').hasMatch(normalized)) {
      throw const FormatException('invalid-timestamp');
    }
    final detectedUnit =
        normalized.replaceAll('-', '').length >= 13 ? 'millisecond' : unit;
    final value = int.parse(normalized);
    final milliseconds = detectedUnit == 'second' ? value * 1000 : value;
    final dateTime = _fromMillis(milliseconds, zone);
    return TimeConvertResult(
      localTime: _formatLocal(dateTime),
      unit: detectedUnit,
      milliseconds: milliseconds,
    );
  }

  String localToTimestamp(String input, String unit, String zone) {
    final trimmed = input.trim();
    final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$')
        .firstMatch(trimmed);
    if (match == null) throw const FormatException('invalid-local-time');
    final location = tz.getLocation(zone);
    final year = int.parse(match.group(1)!);
    final month = int.parse(match.group(2)!);
    final day = int.parse(match.group(3)!);
    final hour = int.parse(match.group(4)!);
    final minute = int.parse(match.group(5)!);
    final second = int.parse(match.group(6)!);
    tz.TZDateTime dateTime;
    try {
      dateTime =
          tz.TZDateTime(location, year, month, day, hour, minute, second);
    } catch (_) {
      throw const FormatException('invalid-local-time');
    }
    if (_formatLocal(dateTime) != trimmed) {
      throw const FormatException('invalid-local-time');
    }
    final milliseconds = dateTime.millisecondsSinceEpoch;
    return unit == 'second' ? '${milliseconds ~/ 1000}' : '$milliseconds';
  }

  String formatLocalTime(int milliseconds, String zone) {
    return _formatLocal(_fromMillis(milliseconds, zone));
  }

  String formatTimezoneLabel(String zone, [int? now]) {
    final dateTime =
        _fromMillis(now ?? DateTime.now().millisecondsSinceEpoch, zone);
    return '$zone (GMT${_formatOffset(dateTime.timeZoneOffset)})';
  }

  tz.TZDateTime _fromMillis(int milliseconds, String zone) {
    return tz.TZDateTime.fromMillisecondsSinceEpoch(
        tz.getLocation(zone), milliseconds);
  }

  String _formatLocal(tz.TZDateTime dateTime) {
    String pad(int value) => value.toString().padLeft(2, '0');
    return '${dateTime.year.toString().padLeft(4, '0')}-'
        '${pad(dateTime.month)}-${pad(dateTime.day)} '
        '${pad(dateTime.hour)}:${pad(dateTime.minute)}:${pad(dateTime.second)}';
  }

  String _formatOffset(Duration offset) {
    final sign = offset.isNegative ? '-' : '+';
    final abs = offset.abs();
    final hours = abs.inHours.toString().padLeft(2, '0');
    final minutes = (abs.inMinutes % 60).toString().padLeft(2, '0');
    return '$sign$hours:$minutes';
  }
}
