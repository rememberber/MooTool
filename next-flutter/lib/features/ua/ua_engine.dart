class UaResult {
  const UaResult({
    required this.browser,
    required this.browserVersion,
    required this.engine,
    required this.engineVersion,
    required this.os,
    required this.osVersion,
    required this.deviceType,
    required this.deviceBrand,
    required this.deviceModel,
    required this.mobile,
    required this.bot,
  });

  final String browser;
  final String browserVersion;
  final String engine;
  final String engineVersion;
  final String os;
  final String osVersion;
  final String deviceType;
  final String deviceBrand;
  final String deviceModel;
  final bool mobile;
  final bool bot;

  Map<String, Object?> toJson() => {
        'browser': browser,
        'browserVersion': browserVersion,
        'engine': engine,
        'engineVersion': engineVersion,
        'os': os,
        'osVersion': osVersion,
        'deviceType': deviceType,
        'deviceBrand': deviceBrand,
        'deviceModel': deviceModel,
        'mobile': mobile,
        'bot': bot,
      };
}

const uaPresets = [
  (
    'Chrome (Windows)',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
  ),
  (
    'Chrome (macOS)',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
  ),
  (
    'Firefox (Windows)',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0'
  ),
  (
    'Safari (iPhone)',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1'
  ),
  (
    'Chrome (Android)',
    'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36'
  ),
  ('curl', 'curl/8.7.1'),
];

class UaEngine {
  UaResult parseUserAgent(String value) {
    final source = value.trim();
    if (source.isEmpty) throw const FormatException('User-Agent is required');
    final bot = RegExp(
            r'bot|crawler|spider|slurp|bingpreview|headless|facebookexternalhit',
            caseSensitive: false)
        .hasMatch(source);
    final browser = _pair(source, [
      (r'Edg(?:e|A|iOS)?/([\d.]+)', 'Edge'),
      (r'OPR/([\d.]+)', 'Opera'),
      (r'Firefox/([\d.]+)', 'Firefox'),
      (r'Chrome/([\d.]+)', 'Chrome'),
      (r'Version/([\d.]+).*Safari/', 'Safari'),
      (r'Safari/([\d.]+)', 'Safari'),
      (r'curl/([\d.]+)', 'curl'),
    ]);
    final engine = _pair(source, [
      (r'AppleWebKit/([\d.]+)', 'WebKit'),
      (r'Gecko/([\d.]+)', 'Gecko'),
      (r'Trident/([\d.]+)', 'Trident'),
    ]);
    final os = _os(source);
    final device = _device(source);
    final mobile = device.$1 == 'mobile' ||
        device.$1 == 'tablet' ||
        RegExp(r'mobile|android|iphone|ipad', caseSensitive: false)
            .hasMatch(source);
    return UaResult(
      browser: browser.$1,
      browserVersion: browser.$2,
      engine: engine.$1,
      engineVersion: engine.$2,
      os: os.$1,
      osVersion: os.$2,
      deviceType: bot
          ? 'bot'
          : (device.$1 == 'Unknown'
              ? (mobile ? 'mobile' : 'desktop')
              : device.$1),
      deviceBrand: device.$2,
      deviceModel: device.$3,
      mobile: mobile,
      bot: bot,
    );
  }

  (String, String) _pair(String source, List<(String, String)> rules) {
    for (final rule in rules) {
      final match = RegExp(rule.$1).firstMatch(source);
      if (match != null) {
        return (rule.$2, match.groupCount >= 1 ? match.group(1)! : 'Unknown');
      }
    }
    return ('Unknown', 'Unknown');
  }

  (String, String) _os(String source) {
    final android = RegExp(r'Android ([\d.]+)').firstMatch(source);
    if (android != null) return ('Android', android.group(1)!);
    final ios = RegExp(r'(?:iPhone OS|CPU OS) ([\d_]+)').firstMatch(source);
    if (ios != null) return ('iOS', ios.group(1)!.replaceAll('_', '.'));
    final mac = RegExp(r'Mac OS X ([\d_]+)').firstMatch(source);
    if (mac != null) return ('Mac OS', mac.group(1)!.replaceAll('_', '.'));
    if (source.contains('Windows')) {
      final win = RegExp(r'Windows NT ([\d.]+)').firstMatch(source);
      return ('Windows', win?.group(1) ?? 'Unknown');
    }
    if (source.contains('Linux')) return ('Linux', 'Unknown');
    return ('Unknown', 'Unknown');
  }

  (String, String, String) _device(String source) {
    if (RegExp(r'iPhone').hasMatch(source)) {
      return ('mobile', 'Apple', 'iPhone');
    }
    if (RegExp(r'iPad').hasMatch(source)) {
      return ('tablet', 'Apple', 'iPad');
    }
    final pixel = RegExp(r'Android .*?; ([^)]+)').firstMatch(source);
    if (source.contains('Android')) {
      return (
        'mobile',
        'Unknown',
        pixel?.group(1)?.trim() ?? 'Unknown',
      );
    }
    return ('desktop', 'Unknown', 'Unknown');
  }
}
