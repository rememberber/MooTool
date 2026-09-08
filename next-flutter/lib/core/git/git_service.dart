import 'dart:io';

class GitException implements Exception {
  GitException(this.message);
  final String message;
  @override
  String toString() => message;
}

class GitStatus {
  GitStatus({
    required this.available,
    this.repository = false,
    this.branch = '',
    this.ahead = 0,
    this.behind = 0,
    this.changes = const [],
    this.conflicted = const [],
    this.remote = '',
  });

  final bool available;
  final bool repository;
  final String branch;
  final int ahead;
  final int behind;
  final List<String> changes;
  final List<String> conflicted;
  final String remote;
}

class GitService {
  Future<bool> available() async {
    final result =
        await _run(Directory.current, ['--version'], allowFail: true);
    return result.exitCode == 0;
  }

  Future<GitStatus> status(Directory vault) async {
    if (!await available()) return GitStatus(available: false);
    if (!await Directory('${vault.path}/.git').exists())
      return GitStatus(available: true);
    final branch = (await _run(vault, ['rev-parse', '--abbrev-ref', 'HEAD']))
        .stdout
        .toString()
        .trim();
    final porcelain =
        (await _run(vault, ['status', '--porcelain'])).stdout.toString();
    final changes = [
      for (final line in porcelain.split('\n'))
        if (line.trim().isNotEmpty) line.substring(3).trim()
    ];
    final conflicted = [
      for (final line in porcelain.split('\n'))
        if (line.startsWith('UU') ||
            line.startsWith('AA') ||
            line.startsWith('DD'))
          line.substring(3).trim()
    ];
    var remote = '';
    final remoteResult =
        await _run(vault, ['remote', 'get-url', 'origin'], allowFail: true);
    if (remoteResult.exitCode == 0)
      remote = remoteResult.stdout.toString().trim();
    return GitStatus(
        available: true,
        repository: true,
        branch: branch,
        changes: changes,
        conflicted: conflicted,
        remote: remote);
  }

  Future<void> init(Directory vault) async {
    await vault.create(recursive: true);
    await _run(vault, ['init']);
  }

  Future<void> addRemote(Directory vault, String url) async {
    _assertSafeRemote(url);
    final current = await _run(vault, ['remote'], allowFail: true);
    if (current.stdout.toString().contains('origin')) {
      await _run(vault, ['remote', 'set-url', 'origin', url]);
    } else {
      await _run(vault, ['remote', 'add', 'origin', url]);
    }
  }

  Future<void> commitAll(Directory vault, String message) async {
    if (message.trim().isEmpty) throw GitException('提交说明不能为空。');
    await _run(vault, ['add', '-A']);
    await _run(vault, ['commit', '-m', message]);
  }

  Future<void> fetch(Directory vault) => _run(vault, ['fetch']);
  Future<void> pull(Directory vault) => _run(vault, ['pull', '--ff-only']);
  Future<void> push(Directory vault) => _run(vault, ['push']);
  Future<void> abort(Directory vault) async {
    await _run(vault, ['merge', '--abort'], allowFail: true);
    await _run(vault, ['rebase', '--abort'], allowFail: true);
  }

  Future<void> checkoutOurs(Directory vault, String path) =>
      _run(vault, ['checkout', '--ours', '--', path]);
  Future<void> checkoutTheirs(Directory vault, String path) =>
      _run(vault, ['checkout', '--theirs', '--', path]);
  Future<void> discard(Directory vault, String path) =>
      _run(vault, ['checkout', '--', path]);

  Future<List<String>> log(Directory vault) async {
    final result =
        await _run(vault, ['log', '-n', '20', '--pretty=format:%h %s']);
    return [
      for (final line in result.stdout.toString().split('\n'))
        if (line.trim().isNotEmpty) line
    ];
  }

  void _assertSafeRemote(String url) {
    if (url.contains(RegExp(r'[\n\r;|&`$]'))) throw GitException('远程地址包含非法字符。');
  }

  Future<ProcessResult> _run(Directory vault, List<String> args,
      {bool allowFail = false}) async {
    final result = await Process.run('git', args,
        workingDirectory: vault.path, environment: const {'LC_ALL': 'C'});
    if (!allowFail && result.exitCode != 0) {
      throw GitException((result.stderr.toString().trim().isEmpty
              ? result.stdout
              : result.stderr)
          .toString()
          .trim());
    }
    return result;
  }
}
