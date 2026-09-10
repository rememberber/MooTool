class LocalSession {
  LocalSession({
    this.tab = '',
    this.left = '',
    this.right = '',
    this.notice = '',
    this.historyOpen = false,
    Map<String, String>? options,
    Map<String, Map<String, String>>? pairs,
  })  : options = options ?? {},
        pairs = pairs ?? {};

  String tab;
  String left;
  String right;
  String notice;
  bool historyOpen;
  final Map<String, String> options;
  final Map<String, Map<String, String>> pairs;

  Map<String, Object?> toJson() => {
        'tab': tab,
        'left': left,
        'right': right,
        'notice': notice,
        'options': options,
        'pairs': pairs,
      };

  factory LocalSession.fromJson(Map<String, Object?> json) {
    final options = <String, String>{};
    final rawOptions = json['options'];
    if (rawOptions is Map) {
      rawOptions.forEach((key, value) {
        options['$key'] = '$value';
      });
    }
    final pairs = <String, Map<String, String>>{};
    final rawPairs = json['pairs'];
    if (rawPairs is Map) {
      rawPairs.forEach((key, value) {
        if (value is Map) {
          pairs['$key'] = {
            for (final entry in value.entries) '${entry.key}': '${entry.value}'
          };
        }
      });
    }
    return LocalSession(
      tab: json['tab'] as String? ?? '',
      left: json['left'] as String? ?? json['text'] as String? ?? '',
      right: json['right'] as String? ?? '',
      notice: json['notice'] as String? ?? '',
      options: options,
      pairs: pairs,
    );
  }
}

class FavoriteRecord {
  FavoriteRecord({
    required this.id,
    required this.toolId,
    required this.name,
    required this.value,
  });

  final String id;
  final String toolId;
  final String name;
  final String value;

  Map<String, Object?> toJson() => {
        'id': id,
        'toolId': toolId,
        'name': name,
        'value': value,
      };

  factory FavoriteRecord.fromJson(Map<String, Object?> json) => FavoriteRecord(
        id: json['id'] as String,
        toolId: json['toolId'] as String,
        name: json['name'] as String? ?? '',
        value: json['value'] as String? ?? '',
      );
}
