import '../time/time_engine.dart';
import 'package:timezone/timezone.dart' as tz;

class CronFields {
  const CronFields({
    required this.second,
    required this.minute,
    required this.hour,
    required this.day,
    required this.month,
    required this.week,
    this.year = '',
  });
  final String second;
  final String minute;
  final String hour;
  final String day;
  final String month;
  final String week;
  final String year;

  static const defaults = CronFields(
    second: '0',
    minute: '*',
    hour: '*',
    day: '*',
    month: '*',
    week: '?',
  );
}

const cronPresets = [
  ('minute', '0 * * * * ?'),
  ('hour', '0 0 * * * ?'),
  ('day', '0 0 0 * * ?'),
  ('weekdays', '0 0 9 ? * MON-FRI'),
];

const _weekNames = {
  'SUN': 0,
  'MON': 1,
  'TUE': 2,
  'WED': 3,
  'THU': 4,
  'FRI': 5,
  'SAT': 6,
};

const _monthNames = {
  'JAN': 1,
  'FEB': 2,
  'MAR': 3,
  'APR': 4,
  'MAY': 5,
  'JUN': 6,
  'JUL': 7,
  'AUG': 8,
  'SEP': 9,
  'OCT': 10,
  'NOV': 11,
  'DEC': 12,
};

class CronEngine {
  CronEngine() : _time = TimeEngine();
  final TimeEngine _time;

  String buildCron(CronFields fields) {
    final values = [
      fields.second,
      fields.minute,
      fields.hour,
      fields.day,
      fields.month,
      fields.week
    ];
    if (values.any((value) => value.trim().isEmpty)) {
      throw const FormatException('All Cron fields are required');
    }
    final withYear = [...values, fields.year.trim()];
    return withYear.where((value) => value.isNotEmpty).join(' ');
  }

  CronFields splitCron(String expression) {
    final parts = expression.trim().split(RegExp(r'\s+'));
    if (parts.length != 6 && parts.length != 7) {
      throw const FormatException('Cron requires 6 or 7 fields');
    }
    return CronFields(
      second: parts[0],
      minute: parts[1],
      hour: parts[2],
      day: parts[3],
      month: parts[4],
      week: parts[5],
      year: parts.length == 7 ? parts[6] : '',
    );
  }

  List<String> nextCronRuns(String expression, String timeZone,
      {int count = 10, DateTime? currentDate}) {
    final fields = splitCron(expression);
    final second = _CronField.parse(fields.second, 0, 59);
    final minute = _CronField.parse(fields.minute, 0, 59);
    final hour = _CronField.parse(fields.hour, 0, 23);
    final day = _CronField.parse(fields.day == '?' ? '*' : fields.day, 1, 31);
    final month = _CronField.parse(fields.month, 1, 12, names: _monthNames);
    final week = _CronField.parse(
        fields.week == '?' ? '*' : _normalizeWeek(fields.week), 0, 6);
    final location = tz.getLocation(timeZone);
    var cursor =
        tz.TZDateTime.from(currentDate ?? DateTime.now().toUtc(), location)
            .add(const Duration(seconds: 1));
    cursor = tz.TZDateTime(location, cursor.year, cursor.month, cursor.day,
        cursor.hour, cursor.minute, cursor.second);
    final runs = <String>[];
    var guard = 0;
    while (runs.length < count && guard < 2000000) {
      guard += 1;
      if (!_matchesYear(cursor.year, fields.year)) {
        cursor = _jumpYear(cursor, location, fields.year);
        continue;
      }
      if (!month.matches(cursor.month)) {
        cursor = _nextMonth(cursor, location);
        continue;
      }
      if (!day.matches(cursor.day) ||
          !week.matches(cursor.weekday == 7 ? 0 : cursor.weekday)) {
        cursor = _nextDay(cursor, location);
        continue;
      }
      if (!hour.matches(cursor.hour)) {
        cursor = _nextHour(cursor, location);
        continue;
      }
      if (!minute.matches(cursor.minute)) {
        cursor = _nextMinute(cursor, location);
        continue;
      }
      if (!second.matches(cursor.second)) {
        cursor = cursor.add(const Duration(seconds: 1));
        continue;
      }
      runs.add(
          '${_time.formatLocalTime(cursor.millisecondsSinceEpoch, timeZone)} ${_offsetLabel(cursor)}');
      cursor = cursor.add(const Duration(seconds: 1));
    }
    if (runs.length < count) {
      throw const FormatException(
          'No matching run time in the supported year range');
    }
    return runs;
  }

  String describeCron(String expression, String language) {
    final fields = splitCron(expression);
    final hour = fields.hour.padLeft(2, '0');
    final minute = fields.minute == '*' ? '00' : fields.minute.padLeft(2, '0');
    final clock = '$hour:$minute';
    final week = fields.week;
    if (language.startsWith('zh')) {
      if (week.contains('MON-FRI')) return '工作日 $clock';
      return '于 $clock';
    }
    if (language.startsWith('ja')) {
      if (week.contains('MON-FRI')) return '平日 $clock';
      return clock;
    }
    if (week.contains('MON-FRI')) return 'At $clock, Monday through Friday';
    return 'At $clock';
  }

  String _normalizeWeek(String value) {
    return value.replaceAllMapped(
        RegExp(r'SUN|MON|TUE|WED|THU|FRI|SAT', caseSensitive: false),
        (match) => '${_weekNames[match.group(0)!.toUpperCase()]}');
  }

  bool _matchesYear(int year, String expression) {
    if (expression.isEmpty || expression == '*') return true;
    return expression.split(',').any((part) {
      final stepMatch = RegExp(r'^(\*|\d{4}-\d{4})/(\d+)$').firstMatch(part);
      if (stepMatch != null) {
        final bounds = stepMatch.group(1) == '*'
            ? [1970, 2199]
            : stepMatch.group(1)!.split('-').map(int.parse).toList();
        return year >= bounds[0] &&
            year <= bounds[1] &&
            (year - bounds[0]) % int.parse(stepMatch.group(2)!) == 0;
      }
      final range = RegExp(r'^(\d{4})-(\d{4})$').firstMatch(part);
      if (range != null) {
        return year >= int.parse(range.group(1)!) &&
            year <= int.parse(range.group(2)!);
      }
      return int.tryParse(part) == year;
    });
  }

  tz.TZDateTime _jumpYear(
      tz.TZDateTime cursor, tz.Location location, String yearField) {
    var year = cursor.year + 1;
    while (year <= 2199 && !_matchesYear(year, yearField)) {
      year += 1;
    }
    return tz.TZDateTime(location, year, 1, 1, 0, 0, 0);
  }

  tz.TZDateTime _nextMonth(tz.TZDateTime cursor, tz.Location location) {
    return cursor.month == 12
        ? tz.TZDateTime(location, cursor.year + 1, 1, 1)
        : tz.TZDateTime(location, cursor.year, cursor.month + 1, 1);
  }

  tz.TZDateTime _nextDay(tz.TZDateTime cursor, tz.Location location) {
    return tz.TZDateTime(location, cursor.year, cursor.month, cursor.day)
        .add(const Duration(days: 1));
  }

  tz.TZDateTime _nextHour(tz.TZDateTime cursor, tz.Location location) {
    return tz.TZDateTime(
            location, cursor.year, cursor.month, cursor.day, cursor.hour)
        .add(const Duration(hours: 1));
  }

  tz.TZDateTime _nextMinute(tz.TZDateTime cursor, tz.Location location) {
    return tz.TZDateTime(location, cursor.year, cursor.month, cursor.day,
            cursor.hour, cursor.minute)
        .add(const Duration(minutes: 1));
  }

  String _offsetLabel(tz.TZDateTime value) {
    final offset = value.timeZoneOffset;
    final sign = offset.isNegative ? '-' : '+';
    final abs = offset.abs();
    final hours = abs.inHours.toString().padLeft(2, '0');
    final minutes = (abs.inMinutes % 60).toString().padLeft(2, '0');
    return 'GMT$sign$hours:$minutes';
  }
}

class _CronField {
  _CronField(this.values);
  final Set<int> values;
  bool matches(int value) => values.contains(value);

  factory _CronField.parse(String expression, int min, int max,
      {Map<String, int>? names}) {
    final values = <int>{};
    for (final part in expression.split(',')) {
      values.addAll(_expand(part.trim(), min, max, names));
    }
    if (values.isEmpty)
      throw FormatException('Invalid Cron field: $expression');
    return _CronField(values);
  }

  static Iterable<int> _expand(
      String part, int min, int max, Map<String, int>? names) {
    if (part == '*' || part == '?') return [for (var i = min; i <= max; i++) i];
    final stepMatch = RegExp(r'^(.+)/(\d+)$').firstMatch(part);
    final step = stepMatch == null ? 1 : int.parse(stepMatch.group(2)!);
    final rangePart = stepMatch?.group(1) ?? part;
    int resolve(String token) {
      final named = names?[token.toUpperCase()];
      if (named != null) return named;
      if (token == '*') return min;
      return int.parse(token);
    }

    if (rangePart == '*') {
      return [for (var i = min; i <= max; i += step) i];
    }
    if (rangePart.contains('-')) {
      final bounds = rangePart.split('-');
      var start = resolve(bounds[0]);
      final end = resolve(bounds[1]);
      final output = <int>[];
      for (var i = start; i <= end; i += step) {
        if (i >= min && i <= max) output.add(i);
      }
      return output;
    }
    final value = resolve(rangePart);
    if (stepMatch != null) {
      return [for (var i = value; i <= max; i += step) i];
    }
    return [value];
  }
}
