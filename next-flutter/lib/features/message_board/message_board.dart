const maxMessageLength = 80;
const messageBoardThemes = [
  'sunbeam',
  'coral',
  'cobalt',
  'forest',
  'paper',
  'midnight'
];
const messageBoardPresets = [
  ('away', 'sunbeam'),
  ('closed', 'coral'),
  ('rest', 'paper'),
  ('busy', 'cobalt'),
  ('meeting', 'forest'),
  ('quiet', 'midnight'),
  ('maintenance', 'cobalt'),
  ('call', 'forest'),
];

const messageBoardColors = {
  'sunbeam': (0xFFF4CE57, 0xFF183832),
  'coral': (0xFFF36B55, 0xFFFFF7EC),
  'cobalt': (0xFF3459D4, 0xFFF3F5FF),
  'forest': (0xFF0F4A3A, 0xFFE8F0C2),
  'paper': (0xFFEFE9DC, 0xFF29241F),
  'midnight': (0xFF151821, 0xFFE8F0FF),
};

class MessageBoardSession {
  String message = '';
  String theme = 'sunbeam';
  String alignment = 'center';
  int size = 100;
  bool presenting = false;
  bool displayAwake = false;

  String clampMessage(String value) => value.length <= maxMessageLength
      ? value
      : value.substring(0, maxMessageLength);

  void applyPreset(String id, String text) {
    message = clampMessage(text);
    for (final preset in messageBoardPresets) {
      if (preset.$1 == id) {
        theme = preset.$2;
        break;
      }
    }
  }

  Map<String, Object?> toJson() => {
        'message': message,
        'theme': theme,
        'alignment': alignment,
        'size': size,
      };

  void restore(Map<String, Object?> json) {
    message = clampMessage(json['message'] as String? ?? '');
    final storedTheme = json['theme'] as String? ?? 'sunbeam';
    theme = messageBoardThemes.contains(storedTheme) ? storedTheme : 'sunbeam';
    alignment = json['alignment'] == 'left' ? 'left' : 'center';
    final storedSize = json['size'];
    final parsed = storedSize is int
        ? storedSize
        : storedSize is num
            ? storedSize.round()
            : 100;
    size = parsed.clamp(70, 130);
  }
}
