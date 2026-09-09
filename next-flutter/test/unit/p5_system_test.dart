import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/product.dart';
import 'package:mootool_next_flutter/features/hardware/system_info.dart';
import 'package:mootool_next_flutter/features/net/net_engine.dart';
import 'package:mootool_next_flutter/features/runtime/runtime_service.dart';
import 'package:mootool_next_flutter/features/runtime/runtime_tools.dart';
import 'package:mootool_next_flutter/features/translation/translation_client.dart';
import 'package:mootool_next_flutter/features/variables/environment_store.dart';

Future<bool> _commandAvailable(String command, List<String> args) async {
  try {
    final result = await Process.run(command, args, runInShell: false)
        .timeout(const Duration(seconds: 3));
    return result.exitCode == 0 ||
        '${result.stdout}${result.stderr}'.trim().isNotEmpty;
  } catch (_) {
    return false;
  }
}

void main() {
  test('parseRuntimeArguments matches Electron fixtures', () {
    expect(parseRuntimeArguments('--name "Moo Tool" --count 2'),
        ['--name', 'Moo Tool', '--count', '2']);
    expect(parseRuntimeArguments("'single value' escaped\\ value"),
        ['single value', 'escaped value']);
    expect(() => parseRuntimeArguments('"unfinished'),
        throwsA(isA<FormatException>()));
    expect(runtimeDisplayName('node'), 'Node.js');
    expect(runtimeDisplayName('groovy'), 'Groovy');
  });

  test('python format only expands tabs and is not Prettier', () {
    expect(
        formatRuntimeSource('\tprint("moo")  ', 'python'), '    print("moo")');
    expect(formatRuntimeSource('const x={a:1};console.log(x)', 'node'),
        'const x={a:1};console.log(x)');
  });

  test('user environment writes product file and not process env', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-env-');
    addTearDown(() => root.deleteSync(recursive: true));
    const marker = 'MOOTOOL_FLUTTER_USER_ONLY';
    expect(Platform.environment.containsKey(marker), isFalse);
    final store = EnvironmentStore(File('${root.path}/user.json'));
    await store.writeUser({marker: 'isolated'});
    expect(await store.readUser(), {marker: 'isolated'});
    expect(Platform.environment.containsKey(marker), isFalse);
    expect(
        store.runtimeEntries().any((item) => item.key == 'product.id'), isTrue);
    expect(
        store
            .runtimeEntries()
            .singleWhere((item) => item.key == 'product.id')
            .value,
        Product.id);
  });

  test('user variables persist through AppController data dir', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-p5s-');
    addTearDown(() => root.deleteSync(recursive: true));
    final first = AppController(AppPaths(root));
    addTearDown(first.dispose);
    await first.load();
    first.localFor('variables').options['userKey'] = 'FLUTTER_P5';
    first.localFor('variables').options['userValue'] = 'ok';
    await first.saveUserVariable();
    first.runtime.tab = 'python';
    first.runtime.code = 'print(40 + 2)';
    first.translation.source = 'hello';
    first.translation.targetLang = 'zh-CN';
    await first.persist();

    final second = AppController(AppPaths(root));
    addTearDown(second.dispose);
    await second.load();
    await second.refreshEnvironment();
    expect(
        second.environmentUser
            .any((item) => item.key == 'FLUTTER_P5' && item.value == 'ok'),
        isTrue);
    expect(second.runtime.tab, 'python');
    expect(second.runtime.code, 'print(40 + 2)');
    expect(second.translation.source, 'hello');
    expect(File('${root.path}/environment/user.json').readAsStringSync(),
        contains('FLUTTER_P5'));
  });

  test('splitTranslationText and googleLanguage match Electron helpers', () {
    expect(
        () => splitTranslationText('text', 1), throwsA(isA<FormatException>()));
    final text = '${'a' * 8}\n${'b' * 8}';
    expect(splitTranslationText(text, 10).join(), text);
    expect(splitTranslationText(text, 10).every((chunk) => chunk.length <= 10),
        isTrue);
    final words = 'hello ' * 10;
    final wordChunks = splitTranslationText(words, 20);
    expect(wordChunks.join(), words);
    expect(
        wordChunks
            .sublist(0, wordChunks.length - 1)
            .every((chunk) => RegExp(r'\s$').hasMatch(chunk)),
        isTrue);
    final emoji = '${'a' * 9}😀${'b' * 9}';
    final emojiChunks = splitTranslationText(emoji, 10);
    expect(emojiChunks.join(), emoji);
    expect(emojiChunks.every((chunk) => chunk.length <= 10), isTrue);
    expect(emojiChunks.first, 'a' * 9);
    expect(emojiChunks[1].startsWith('😀'), isTrue);
    expect(googleLanguage('jp'), 'ja');
    expect(googleLanguage('cht'), 'zh-TW');
    expect(googleLanguage('wyw'), 'lzh');
  });

  test('translation client parses Google-shaped JSON and can abort', () async {
    final client = TranslationClient(fetch: (uri, timeoutMs) async {
      expect(uri.host, 'translate.googleapis.com');
      expect(uri.queryParameters['sl'], 'en');
      expect(uri.queryParameters['tl'], 'zh-CN');
      expect(uri.queryParameters['q'], 'hello');
      return jsonEncode([
        [
          ['你好', 'hello', null, null, 10]
        ]
      ]);
    });
    final translated = await client.translate(
      requestId: 'trans-ok1',
      text: 'hello',
      sourceLang: 'en',
      targetLang: 'zh-CN',
    );
    expect(translated.text, '你好');
    expect(translated.provider, 'google');

    var calls = 0;
    late final TranslationClient aborting;
    aborting = TranslationClient(fetch: (uri, timeoutMs) async {
      calls += 1;
      aborting.cancel('trans-ab1');
      return jsonEncode([
        [
          ['x', 'y']
        ]
      ]);
    });
    await expectLater(
        aborting.translate(
          requestId: 'trans-ab1',
          text: '${'a' * 1800} hello',
          sourceLang: 'auto',
          targetLang: 'zh-CN',
        ),
        throwsA(predicate((error) =>
            error is FormatException && '$error'.contains('ABORTED'))));
    expect(calls, 1);
  });

  test('WHOIS rejects invalid targets and follows IANA referral', () async {
    expect(() => normalizeWhoisTarget(''), throwsA(isA<FormatException>()));
    expect(() => normalizeWhoisTarget('bad host'),
        throwsA(isA<FormatException>()));
    final calls = <String>[];
    final result =
        await queryWhois('Example.COM', query: (server, target) async {
      calls.add('$server:$target');
      if (server == 'whois.iana.org') return 'refer: whois.example.net\n';
      return 'Domain Name: EXAMPLE.COM';
    });
    expect(
        calls, ['whois.iana.org:example.com', 'whois.example.net:example.com']);
    expect(result, contains('EXAMPLE.COM'));
  });

  test('system info reports the current platform and does not invent CPU %',
      () async {
    final snapshot = await collectSystemInfo();
    final system = snapshot.sections['system']!.expand((group) => group.items);
    expect(
        system.any((item) =>
            item.label == 'Platform' && item.value == Platform.operatingSystem),
        isTrue);
    expect(snapshot.sections['cpu']!.first.items.first.label, 'Logical cores');
    expect(snapshot.sections['cpu']!.first.items.first.value,
        '${Platform.numberOfProcessors}');
    expect(snapshot.sections.containsKey('gpu'), isFalse);
  });

  test('python runtime prints 40 + 2 and cancel stops a loop', () async {
    final hasPython = await _commandAvailable('python3', ['--version']);
    if (!hasPython) {
      markTestSkipped('python3 is not available on this machine');
      return;
    }
    final root = Directory.systemTemp.createTempSync('mootool-flutter-rt-');
    addTearDown(() => root.deleteSync(recursive: true));
    final service = RuntimeExecutionService(root);
    final added = await service.run(
      requestId: 'python-add',
      runtime: 'python',
      code: 'print(40 + 2)',
      timeoutMs: 15000,
    );
    expect(added.exitCode, 0);
    expect(added.stdout.trim(), '42');
    expect(added.cancelled, isFalse);

    final loop = service.run(
      requestId: 'python-loop',
      runtime: 'python',
      code: 'import time\nwhile True:\n    time.sleep(0.2)\n',
      timeoutMs: 30000,
    );
    await Future<void>.delayed(const Duration(milliseconds: 400));
    expect(await service.cancel('python-loop'), isTrue);
    final stopped = await loop;
    expect(stopped.cancelled || stopped.exitCode != 0, isTrue);
    expect(stopped.stdout.contains('42'), isFalse);
  }, timeout: const Timeout(Duration(seconds: 40)));
}
