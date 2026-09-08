import '../reformat/reformat_engine.dart';

const runtimeIds = ['java', 'groovy', 'python', 'node'];
const maxRuntimeCodeBytes = 1024 * 1024;
const maxRuntimeOutputBytes = 2 * 1024 * 1024;

const runtimeSamples = {
  'java': '''public class Main {
    public static void main(String[] args) {
        System.out.println(40 + 2);
    }
}''',
  'groovy': '''println 40 + 2''',
  'python': '''print(40 + 2)''',
  'node': '''console.log(40 + 2)''',
};

String runtimeDisplayName(String runtime) {
  if (runtime == 'node') return 'Node.js';
  if (runtime.isEmpty) return runtime;
  return '${runtime[0].toUpperCase()}${runtime.substring(1)}';
}

List<String> parseRuntimeArguments(String value) {
  final result = <String>[];
  var current = '';
  var quote = '';
  var escaped = false;
  void push() {
    if (current.isNotEmpty) result.add(current);
    current = '';
  }

  for (final character in value.trim().split('')) {
    if (escaped) {
      current += character;
      escaped = false;
    } else if (character == r'\' && quote != "'") {
      escaped = true;
    } else if (quote.isNotEmpty) {
      if (character == quote) {
        quote = '';
      } else {
        current += character;
      }
    } else if (character == '"' || character == "'") {
      quote = character;
    } else if (RegExp(r'\s').hasMatch(character)) {
      push();
    } else {
      current += character;
    }
  }
  if (escaped || quote.isNotEmpty) {
    throw const FormatException('Unterminated runtime argument');
  }
  push();
  if (result.length > 40 || result.any((item) => item.length > 1000)) {
    throw const FormatException('Too many runtime arguments');
  }
  return result;
}

String formatRuntimeSource(String code, String runtime) {
  if (code.trim().isEmpty) return '';
  if (runtime == 'java') return ReformatEngine().formatCode(code, 'java');
  return code
      .replaceAll('\t', '    ')
      .split(RegExp(r'\r?\n'))
      .map((line) => line.trimRight())
      .join('\n')
      .trimRight();
}

class RuntimeStatus {
  RuntimeStatus(
      {required this.id,
      required this.available,
      required this.command,
      this.version = ''});
  final String id;
  final bool available;
  final String command;
  final String version;
}

class RuntimeExecutionResult {
  RuntimeExecutionResult({
    required this.requestId,
    required this.runtime,
    required this.command,
    required this.stdout,
    required this.stderr,
    required this.exitCode,
    required this.durationMs,
    this.timedOut = false,
    this.cancelled = false,
    this.truncated = false,
  });

  final String requestId;
  final String runtime;
  final String command;
  final String stdout;
  final String stderr;
  final int? exitCode;
  final int durationMs;
  final bool timedOut;
  final bool cancelled;
  final bool truncated;
}

class RuntimeSession {
  RuntimeSession()
      : codes = Map<String, String>.from(runtimeSamples),
        arguments = {for (final id in runtimeIds) id: ''},
        workingDirectories = {for (final id in runtimeIds) id: ''};

  String tab = 'java';
  String javaMode = 'java';
  final Map<String, String> codes;
  final Map<String, String> arguments;
  final Map<String, String> workingDirectories;
  String stdout = '';
  String stderr = '';
  String notice = '';
  bool running = false;
  String requestId = '';
  RuntimeExecutionResult? result;
  List<RuntimeStatus> statuses = [];

  String get runtime => tab == 'java' ? javaMode : tab;
  String get code => codes[runtime] ?? '';
  set code(String value) => codes[runtime] = value;

  Map<String, Object?> toJson() => {
        'tab': tab,
        'javaMode': javaMode,
        'codes': codes,
        'arguments': arguments,
        'workingDirectories': workingDirectories,
        'stdout': stdout,
        'stderr': stderr,
      };

  void restore(Map<String, Object?> json) {
    tab = json['tab'] as String? ?? 'java';
    javaMode = json['javaMode'] as String? ?? 'java';
    final storedCodes = json['codes'];
    if (storedCodes is Map) {
      storedCodes.forEach((key, value) {
        codes['$key'] = '$value';
      });
    }
    final storedArgs = json['arguments'];
    if (storedArgs is Map) {
      storedArgs.forEach((key, value) {
        arguments['$key'] = '$value';
      });
    }
    final storedDirs = json['workingDirectories'];
    if (storedDirs is Map) {
      storedDirs.forEach((key, value) {
        workingDirectories['$key'] = '$value';
      });
    }
    stdout = json['stdout'] as String? ?? '';
    stderr = json['stderr'] as String? ?? '';
  }
}
