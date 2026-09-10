class HostProfile {
  HostProfile(
      {required this.id,
      required this.name,
      this.content = '',
      DateTime? created,
      DateTime? modified})
      : created = created ?? DateTime.now(),
        modified = modified ?? DateTime.now();

  final String id;
  String name;
  String content;
  DateTime created;
  DateTime modified;

  Map<String, Object?> toJson() => {
        'id': id,
        'name': name,
        'content': content,
        'created': created.toIso8601String(),
        'modified': modified.toIso8601String(),
      };

  factory HostProfile.fromJson(Map<String, Object?> json) => HostProfile(
        id: json['id'] as String? ?? '${DateTime.now().microsecondsSinceEpoch}',
        name: json['name'] as String? ?? 'Untitled',
        content: json['content'] as String? ?? '',
        created: DateTime.tryParse(json['created'] as String? ?? '') ??
            DateTime.now(),
        modified: DateTime.tryParse(json['modified'] as String? ?? '') ??
            DateTime.now(),
      );
}

class HostSession {
  final List<HostProfile> profiles = [];
  String? selectedId;
  String notice = '';

  HostProfile? get selected {
    for (final item in profiles) {
      if (item.id == selectedId) return item;
    }
    return null;
  }

  Map<String, Object?> toJson() => {
        'profiles': [for (final item in profiles) item.toJson()],
        'selectedId': selectedId,
        'notice': notice,
      };

  void restore(Map<String, Object?> json) {
    profiles
      ..clear()
      ..addAll([
        for (final item in json['profiles'] as List? ?? [])
          HostProfile.fromJson(Map<String, Object?>.from(item as Map))
      ]);
    selectedId = json['selectedId'] as String?;
    notice = json['notice'] as String? ?? '';
  }
}
