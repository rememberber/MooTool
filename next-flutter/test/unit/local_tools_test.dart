import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/features/calculator/calculator_engine.dart';
import 'package:mootool_next_flutter/features/color/color_engine.dart';
import 'package:mootool_next_flutter/features/config/config_engine.dart';
import 'package:mootool_next_flutter/features/cron/cron_engine.dart';
import 'package:mootool_next_flutter/features/crypto/crypto_engine.dart';
import 'package:mootool_next_flutter/features/diff/diff_engine.dart';
import 'package:mootool_next_flutter/features/encode/encode_engine.dart';
import 'package:mootool_next_flutter/features/protobuf/protobuf_engine.dart';
import 'package:mootool_next_flutter/features/qr/qr_engine.dart';
import 'package:mootool_next_flutter/features/reformat/reformat_engine.dart';
import 'package:mootool_next_flutter/features/regex/regex_engine.dart';
import 'package:mootool_next_flutter/features/time/time_engine.dart';
import 'package:mootool_next_flutter/features/ua/ua_engine.dart';

void main() {
  test('encode unicode hex ascii and url round trips', () {
    final engine = EncodeEngine();
    expect(engine.fromUnicode(engine.toUnicode('Moo 工具 🚀')), 'Moo 工具 🚀');
    expect(engine.hexToText(engine.textToHex('你好 Moo')), '你好 Moo');
    expect(
        engine.urlDecode(
            engine.urlEncode('你好 a/b', UrlCharset.utf8), UrlCharset.utf8),
        '你好 a/b');
    expect(
        engine.urlDecode(
            engine.urlEncode('编码测试', UrlCharset.gb2312), UrlCharset.gb2312),
        '编码测试');
    expect(engine.textToAscii('A中', AsciiFormat.decimal), '65 20013');
    expect(engine.textToAscii('A中', AsciiFormat.hex), '41 4E2D');
    expect(engine.asciiToText('65 4E2D'), 'A中');
  });

  test('calculator expression base gcd and combinations', () {
    final engine = CalculatorEngine();
    expect(engine.evaluateExpression('2 * (3 + 4)='), '14');
    expect(engine.evaluateExpression('-3 + 10 / 2'), '2');
    expect(engine.evaluateExpression('.5 * 8'), '4');
    expect(() => engine.evaluateExpression('x = 2'), throwsFormatException);
    expect(() => engine.evaluateExpression('1 / 0'), throwsFormatException);
    expect(engine.convertBase('255', 10, 16), 'ff');
    expect(engine.convertBase('11111111', 2, 10), '255');
    expect(engine.gcd('54', '24'), '6');
    expect(engine.lcm('6', '8'), '24');
    expect(engine.permutation('5', '2'), '20');
    expect(engine.combination('5', '2'), '10');
  });

  test('time conversion uses IANA zones and rejects invalid dates', () {
    final engine = TimeEngine();
    expect(engine.timestampToLocal('0', 'second', 'Asia/Shanghai').localTime,
        '1970-01-01 08:00:00');
    expect(engine.timestampToLocal('1704067200000', 'second', 'UTC').localTime,
        '2024-01-01 00:00:00');
    expect(
        engine.localToTimestamp(
            '1970-01-01 08:00:00', 'second', 'Asia/Shanghai'),
        '0');
    expect(engine.localToTimestamp('2024-01-01 00:00:00', 'millisecond', 'UTC'),
        '1704067200000');
    expect(
        () => engine.localToTimestamp('2024-02-31 00:00:00', 'second', 'UTC'),
        throwsFormatException);
    expect(engine.formatTimezoneLabel('UTC', 0), 'UTC (GMT+00:00)');
  });

  test('regex matches groups and zero-width without looping', () {
    final engine = RegexEngine();
    expect(
        engine
            .matchRegex(
                '(moo)(\\d+)', 'moo1 moo22', const RegexOptions(global: true))
            .map((hit) =>
                {'index': hit.index, 'value': hit.value, 'groups': hit.groups})
            .toList(),
        [
          {
            'index': 0,
            'value': 'moo1',
            'groups': ['moo', '1']
          },
          {
            'index': 5,
            'value': 'moo22',
            'groups': ['moo', '22']
          }
        ]);
    expect(engine.matchRegex('(?=a)', 'aa', const RegexOptions(global: true)),
        hasLength(2));
    expect(commonRegexes, hasLength(21));
  });

  test('cron builds splits and next weekday runs', () {
    final engine = CronEngine();
    expect(engine.buildCron(CronFields.defaults), '0 * * * * ?');
    expect(engine.splitCron('0 15 10 ? * MON-FRI 2027').year, '2027');
    final runs = engine.nextCronRuns('0 0 9 ? * MON-FRI', 'Asia/Shanghai',
        count: 2, currentDate: DateTime.utc(2026, 7, 17, 2));
    expect(runs, hasLength(2));
    expect(runs.first, contains('2026-07-20 09:00:00'));
    final yearRun = engine.nextCronRuns('0 0 0 1 1 ? 2028', 'UTC',
        count: 1, currentDate: DateTime.utc(2026, 1, 1));
    expect(yearRun.first, contains('2028-01-01 00:00:00'));
    expect(
        engine.describeCron('0 0 9 ? * MON-FRI', 'en-US'), contains('09:00'));
    expect(
        engine.describeCron('0 0 9 ? * MON-FRI', 'zh-CN'), contains('09:00'));
  });

  test('ua parser reads presets and bots', () {
    final engine = UaEngine();
    final chrome = engine.parseUserAgent(uaPresets[0].$2);
    expect(chrome.browser, 'Chrome');
    expect(chrome.os, 'Windows');
    expect(chrome.mobile, isFalse);
    expect(engine.parseUserAgent(uaPresets[3].$2).mobile, isTrue);
    expect(engine.parseUserAgent('Mozilla/5.0 Googlebot/2.1').bot, isTrue);
  });

  test('config properties yaml round trip', () {
    final engine = ConfigEngine();
    final yaml = engine.propertiesToYaml(
        'server.port=8080\nusers[0].name=Ada\nusers[1].name=Lin');
    expect(yaml, contains('server:'));
    expect(yaml, contains('users:'));
    expect(yaml, contains('name: Ada'));
    expect(engine.yamlToProperties('server:\n  port: 8080\ntags: [a, b]\n'),
        'server.port=8080\ntags=a,b');
    expect(engine.validateYaml('a: [1, 2]').valid, isTrue);
    expect(engine.validateYaml('a: [1,').valid, isFalse);
    expect(engine.formatYaml('a: {b: 1}'), contains('a:'));
  });

  test('crypto AES ECB PKCS7 matches Electron fixture and hashes', () {
    final engine = CryptoEngine();
    expect(engine.symmetricEncrypt('AES', 'MooTool 加密', '1234567890abcdef'),
        '504a3eb1fee7af3af9561f37a6f12fa8');
    expect(
        engine.symmetricDecrypt(
            'AES', '504a3eb1fee7af3af9561f37a6f12fa8', '1234567890abcdef'),
        'MooTool 加密');
    expect(
        engine.symmetricDecrypt(
            'DES',
            engine.symmetricEncrypt('DES', 'MooTool 加密', '12345678'),
            '12345678'),
        'MooTool 加密');
    expect(engine.digestText('MD5', 'abc'), '900150983cd24fb0d6963f7d28e17f72');
    expect(engine.digestText('SHA-256', 'abc'),
        'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad');
    expect(engine.digestText('SM3', 'abc'),
        '66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0');
    expect(engine.decodeBase('Base32', engine.encodeBase('Base32', 'Moo 工具')),
        'Moo 工具');
    expect(engine.encodeBase('Base32', 'Moo 工具'), 'JVXW6IHFW6S6LBNX');
    expect(engine.randomDigits(32), matches(RegExp(r'^\d{32}$')));
    expect(engine.randomPassword(24), hasLength(24));
  });

  test('color parse operations and frozen theme catalog', () {
    final engine = ColorEngine();
    expect(engine.parseColor('#0f8'), const RgbColor(r: 0, g: 255, b: 136));
    expect(
        engine.parseColor('12, 34, 56'), const RgbColor(r: 12, g: 34, b: 56));
    expect(
        engine.formatColor(const RgbColor(r: 10, g: 187, b: 204), 'HEX_UPPER'),
        '#0ABBCC');
    final a = const RgbColor(r: 100, g: 150, b: 200);
    final b = const RgbColor(r: 50, g: 200, b: 100);
    expect(engine.applyColorOperation('invert', a, b),
        const RgbColor(r: 155, g: 105, b: 55));
    expect(engine.applyColorOperation('intersect', a, b),
        const RgbColor(r: 19, g: 117, b: 78));
    expect(
        sha256
            .convert(utf8.encode(jsonEncode(engine.colorThemesJsonValue())))
            .toString(),
        '72e2218c6dcffce0def99f1317075f2041127035730a0379875f5e875e893575');
    expect(sha256.convert(utf8.encode(jsonEncode(standardColors))).toString(),
        '1994345359abd00b901c22614eeef6b400775120cebc04083e05bab9875b535c');
  });

  test('text diff ranges and unified patch', () {
    final result =
        DiffEngine().compareText('one\ntwo\n', 'one\nthree\nplus\n', false);
    expect(result.changed, 1);
    expect(result.added, 1);
    expect(result.removed, 0);
    expect(result.segments, [
      const DiffSegment(
          type: 'change',
          leftStart: 5,
          leftEnd: 7,
          rightStart: 5,
          rightEnd: 9,
          wholeLine: false),
      const DiffSegment(
          type: 'insert',
          leftStart: -1,
          leftEnd: -1,
          rightStart: 10,
          rightEnd: 14,
          wholeLine: true),
    ]);
    expect(
        result.unified,
        [
          '--- old',
          '+++ new',
          '@@ -1,3 +1,4 @@',
          ' one',
          '-two',
          '+three',
          '+plus',
          ' '
        ].join('\n'));
  });

  test('qr png generation and size clamp', () {
    final engine = QrEngine();
    final png = engine.generatePng('https://github.com/rememberber/MooTool',
        size: 240, errorCorrectionLevel: 'M');
    expect(png.take(8), [137, 80, 78, 71, 13, 10, 26, 10]);
    expect(engine.normalizeQrSize(20), 120);
    expect(engine.normalizeQrSize(360.4), 360);
    expect(engine.normalizeQrSize(9999), 2000);
  });

  test('reformat nginx xml html java', () {
    final engine = ReformatEngine();
    expect(
        engine.formatNginx(
            'server { listen 80; location / { proxy_pass http://app; } }', 2),
        'server {\n  listen 80;\n  location / {\n    proxy_pass http://app;\n  }\n}');
    expect(engine.formatCode('<root><item id="1">moo</item></root>', 'xml', 2),
        contains('\n  <item'));
    expect(engine.formatCode('<main><strong>Moo</strong></main>', 'html', 2),
        contains('<strong>Moo</strong>'));
    final java = engine.formatCode(
        'class Demo{public static void main(String[] args){System.out.println("moo");}}',
        'java',
        2);
    expect(java, contains('class Demo'));
    expect(java, contains('System.out.println("moo");'));
  });

  test('protobuf json hex wire round trip from pasted schema', () {
    const proto =
        'syntax = "proto3"; message Person { string name = 1; int32 age = 2; repeated string tags = 3; }';
    final engine = ProtobufEngine();
    const json = '{"name":"Moo","age":25,"tags":["desktop","tool"]}';
    final hex = engine.jsonToProtobuf(proto, 'Person', json, 'Hex');
    expect(hex, matches(RegExp(r'^[0-9a-f]+$')));
    expect(jsonDecode(engine.protobufToJson(proto, 'Person', hex, 'Hex')), {
      'name': 'Moo',
      'age': 25,
      'tags': ['desktop', 'tool']
    });
    final wire = engine.decodeWire(hex, 'Hex');
    expect(wire, contains('field=1'));
    expect(wire, contains('value="Moo"'));
    expect(engine.formatProtoDefinition(proto),
        contains('message Person {\n  string name = 1;'));
  });
}
