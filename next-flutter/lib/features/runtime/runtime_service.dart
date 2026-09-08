import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;

import 'runtime_tools.dart';

class RuntimeExecutionService {
  RuntimeExecutionService(this.tempRoot);
  final Directory tempRoot;
  final Map<String, Process> _active = {};
  final Set<String> _cancelled = {};
  final Set<String> _timedOut = {};
  final Set<String> _truncated = {};

  Future<List<RuntimeStatus>> detect({
    String javaPath = '',
    String groovyPath = '',
    String pythonPath = '',
    String nodePath = '',
  }) async {
    final python = pythonPath.isEmpty
        ? (Platform.isWindows ? 'python' : 'python3')
        : pythonPath;
    return [
      await _detect('java', javaPath.isEmpty ? 'java' : javaPath, ['-version']),
      await _detect(
          'groovy', groovyPath.isEmpty ? 'groovy' : groovyPath, ['--version']),
      await _detect('python', python, ['--version']),
      await _detect('node', nodePath.isEmpty ? 'node' : nodePath, ['--version']),
    ];
  }

  Future<RuntimeStatus> _detect(
      String id, String command, List<String> args) async {
    try {
      final result = await Process.run(command, args,
          runInShell: false).timeout(const Duration(seconds: 3));
      final output = '${result.stdout}\n${result.stderr}'.trim().split('\n').first;
      return RuntimeStatus(
          id: id,
          available: result.exitCode == 0 || output.isNotEmpty,
          command: command,
          version: output);
    } catch (_) {
      return RuntimeStatus(id: id, available: false, command: command);
    }
  }

  Future<RuntimeExecutionResult> run({
    required String requestId,
    required String runtime,
    required String code,
    List<String> arguments = const [],
    String workingDirectory = '',
    int timeoutMs = 30000,
    String javaPath = '',
    String groovyPath = '',
    String pythonPath = '',
    String nodePath = '',
    void Function(String stream, String text)? onOutput,
  }) async {
    if (!RegExp(r'^[A-Za-z0-9_-]{8,80}$').hasMatch(requestId)) {
      throw const FormatException('Invalid runtime request id');
    }
    if (!runtimeIds.contains(runtime)) {
      throw FormatException('Unsupported runtime $runtime');
    }
    if (utf8.encode(code).length > maxRuntimeCodeBytes) {
      throw const FormatException('Code exceeds 1 MB limit');
    }
    if (arguments.length > 40 ||
        arguments.any((item) => item.length > 1000 || item.contains('\u0000'))) {
      throw const FormatException('Invalid runtime arguments');
    }
    await tempRoot.create(recursive: true);
    final directory =
        await Directory(p.join(tempRoot.path, 'run-$requestId')).create();
    final definition = _definition(runtime, directory.path, code,
        javaPath: javaPath,
        groovyPath: groovyPath,
        pythonPath: pythonPath,
        nodePath: nodePath);
    await File(definition.file).writeAsString(code);
    final cwd = workingDirectory.trim().isEmpty
        ? directory.path
        : Directory(workingDirectory.trim()).resolveSymbolicLinksSync();
    if (!Directory(cwd).existsSync()) {
      throw const FormatException('Runtime working directory is not a directory');
    }
    final args = [...definition.args, ...arguments];
    final started = DateTime.now();
    _cancelled.remove(requestId);
    _timedOut.remove(requestId);
    _truncated.remove(requestId);
    var stdout = '';
    var stderr = '';
    var outputBytes = 0;
    late Process process;
    try {
      process = await Process.start(
        definition.command,
        args,
        workingDirectory: cwd,
        environment: _runtimeEnvironment(),
        includeParentEnvironment: false,
        runInShell: false,
      );
      _active[requestId] = process;
      final timeout = Timer(Duration(milliseconds: timeoutMs.clamp(1000, 120000)),
          () {
        _timedOut.add(requestId);
        unawaited(cancel(requestId));
      });
      void append(String stream, List<int> chunk) {
        if (_truncated.contains(requestId)) return;
        final remaining = maxRuntimeOutputBytes - outputBytes;
        if (remaining <= 0) {
          _truncated.add(requestId);
          unawaited(cancel(requestId));
          return;
        }
        final used = chunk.length > remaining ? chunk.sublist(0, remaining) : chunk;
        final text = utf8.decode(used, allowMalformed: true);
        outputBytes += used.length;
        if (stream == 'stdout') {
          stdout += text;
        } else {
          stderr += text;
        }
        onOutput?.call(stream, text);
        if (chunk.length > remaining) {
          _truncated.add(requestId);
          unawaited(cancel(requestId));
        }
      }

      final stdoutDone = Completer<void>();
      final stderrDone = Completer<void>();
      process.stdout.listen((chunk) => append('stdout', chunk),
          onDone: stdoutDone.complete,
          onError: (_) => stdoutDone.complete());
      process.stderr.listen((chunk) => append('stderr', chunk),
          onDone: stderrDone.complete,
          onError: (_) => stderrDone.complete());
      process.stdin.close();
      final exitCode = await process.exitCode;
      timeout.cancel();
      await Future.wait([stdoutDone.future, stderrDone.future]);
      return RuntimeExecutionResult(
        requestId: requestId,
        runtime: runtime,
        command: [definition.command, ...args].join(' '),
        stdout: stdout,
        stderr: stderr,
        exitCode: exitCode,
        durationMs: DateTime.now().difference(started).inMilliseconds,
        timedOut: _timedOut.contains(requestId),
        cancelled: _cancelled.contains(requestId),
        truncated: _truncated.contains(requestId),
      );
    } on ProcessException catch (error) {
      throw FormatException('Unable to start $runtime: ${error.message}');
    } finally {
      _active.remove(requestId);
      if (await directory.exists()) {
        await directory.delete(recursive: true);
      }
    }
  }

  Future<bool> cancel(String requestId) async {
    _cancelled.add(requestId);
    final process = _active.remove(requestId);
    if (process == null) return false;
    process.kill(ProcessSignal.sigterm);
    Future<void>.delayed(const Duration(milliseconds: 1200), () {
      process.kill(ProcessSignal.sigkill);
    });
    return true;
  }

  ({String command, List<String> args, String file}) _definition(
      String runtime, String directory, String code,
      {String javaPath = '',
      String groovyPath = '',
      String pythonPath = '',
      String nodePath = ''}) {
    if (runtime == 'java') {
      final match = RegExp(
              r'\bpublic\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*(?:class|record|interface|enum)\s+([A-Za-z_$][\w$]*)')
          .firstMatch(code);
      final name = match?[1] ?? 'Main';
      final file = p.join(directory, '$name.java');
      return (
        command: javaPath.isEmpty ? 'java' : javaPath,
        args: [file],
        file: file
      );
    }
    if (runtime == 'groovy') {
      final file = p.join(directory, 'main.groovy');
      return (
        command: groovyPath.isEmpty ? 'groovy' : groovyPath,
        args: [file],
        file: file
      );
    }
    if (runtime == 'python') {
      final file = p.join(directory, 'main.py');
      return (
        command: pythonPath.isEmpty
            ? (Platform.isWindows ? 'python' : 'python3')
            : pythonPath,
        args: ['-u', file],
        file: file
      );
    }
    final file = p.join(directory, 'main.mjs');
    return (
      command: nodePath.isEmpty ? 'node' : nodePath,
      args: [file],
      file: file
    );
  }

  Map<String, String> _runtimeEnvironment() {
    const allowed = [
      'PATH',
      'HOME',
      'USERPROFILE',
      'TMPDIR',
      'TMP',
      'TEMP',
      'SystemRoot',
      'WINDIR',
      'LANG',
      'LC_ALL',
      'JAVA_HOME',
      'GROOVY_HOME',
    ];
    return {
      for (final key in allowed)
        if (Platform.environment[key] != null) key: Platform.environment[key]!
    };
  }
}
