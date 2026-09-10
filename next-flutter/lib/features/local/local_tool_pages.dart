import 'dart:convert';

import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/widgets.dart';
import '../calculator/calculator_engine.dart';
import '../color/color_engine.dart';
import '../config/config_engine.dart';
import '../cron/cron_engine.dart';
import '../crypto/crypto_engine.dart';
import '../diff/diff_engine.dart';
import '../encode/encode_engine.dart';
import '../protobuf/protobuf_engine.dart';
import '../qr/qr_engine.dart';
import '../reformat/reformat_engine.dart';
import '../regex/regex_engine.dart';
import '../time/time_engine.dart';
import '../ua/ua_engine.dart';
import 'io_workspace.dart';

class EncodeToolPage extends StatelessWidget {
  const EncodeToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.localFor('encode');
    if (session.tab.isEmpty) session.tab = 'unicode';
    final engine = EncodeEngine();
    return IoWorkspace(
      controller: controller,
      toolId: 'encode',
      leftLabel: controller.t('tool.io.input'),
      rightLabel: controller.t('tool.io.output'),
      tabs: [
        ('unicode', controller.t('encode.tab.unicode')),
        ('url', controller.t('encode.tab.url')),
        ('hex', controller.t('encode.tab.hex')),
        ('ascii', controller.t('encode.tab.ascii')),
      ],
      actions: [
        ToolActionButton(
            label: controller.t('encode.forward'),
            icon: Icons.arrow_forward,
            onPressed: () => _convert(engine, true)),
        ToolActionButton(
            label: controller.t('encode.reverse'),
            icon: Icons.arrow_back,
            onPressed: () => _convert(engine, false)),
        if (session.tab == 'url')
          _option(session, 'charset', ['utf-8', 'gb2312']),
        if (session.tab == 'ascii')
          _option(session, 'asciiFormat', ['decimal', 'hex']),
      ],
    );
  }

  Widget _option(session, String key, List<String> values) {
    final current = session.options[key] ?? values.first;
    return DropdownButton<String>(
      value: values.contains(current) ? current : values.first,
      isExpanded: true,
      items: [
        for (final value in values)
          DropdownMenuItem(
              value: value,
              child: Text(value, style: const TextStyle(fontSize: 12)))
      ],
      onChanged: (value) {
        if (value == null) return;
        session.options[key] = value;
        controller.refresh();
      },
    );
  }

  void _convert(EncodeEngine engine, bool forward) {
    controller.runLocal('encode', (session) {
      final input = forward ? session.left : session.right;
      final output = switch (session.tab) {
        'url' => forward
            ? engine.urlEncode(
                input,
                session.options['charset'] == 'gb2312'
                    ? UrlCharset.gb2312
                    : UrlCharset.utf8)
            : engine.urlDecode(
                input,
                session.options['charset'] == 'gb2312'
                    ? UrlCharset.gb2312
                    : UrlCharset.utf8),
        'hex' => forward ? engine.textToHex(input) : engine.hexToText(input),
        'ascii' => forward
            ? engine.textToAscii(
                input,
                session.options['asciiFormat'] == 'hex'
                    ? AsciiFormat.hex
                    : AsciiFormat.decimal)
            : engine.asciiToText(input),
        _ => forward ? engine.toUnicode(input) : engine.fromUnicode(input),
      };
      if (forward) {
        session.right = output;
      } else {
        session.left = output;
      }
      controller.recordToolHistory(
          toolId: 'encode', title: session.tab, input: input, output: output);
    });
  }
}

class CalculatorToolPage extends StatelessWidget {
  const CalculatorToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = CalculatorEngine();
    return IoWorkspace(
      controller: controller,
      toolId: 'calculator',
      leftLabel: controller.t('calculator.expression'),
      rightLabel: controller.t('tool.io.output'),
      actions: [
        ToolActionButton(
            label: '=',
            onPressed: () => controller.runLocal('calculator', (session) {
                  session.right = engine.evaluateExpression(session.left);
                  controller.recordToolHistory(
                      toolId: 'calculator',
                      title: '=',
                      input: session.left,
                      output: session.right);
                })),
        ToolActionButton(
            label: controller.t('calculator.base'),
            onPressed: () => controller.runLocal('calculator', (session) {
                  session.right = engine.convertBase(
                      session.left,
                      int.parse(session.options['from'] ?? '10'),
                      int.parse(session.options['to'] ?? '16'));
                })),
        _field(controller, 'calculator', 'from', '10'),
        _field(controller, 'calculator', 'to', '16'),
      ],
    );
  }
}

class TimeConvertToolPage extends StatelessWidget {
  const TimeConvertToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = TimeEngine();
    final session = controller.localFor('timeConvert');
    session.options.putIfAbsent('zone', () => 'Asia/Shanghai');
    session.options.putIfAbsent('unit', () => 'second');
    return IoWorkspace(
      controller: controller,
      toolId: 'timeConvert',
      leftLabel: controller.t('time.timestamp'),
      rightLabel: controller.t('time.local'),
      actions: [
        ToolActionButton(
            label: controller.t('time.toLocal'),
            onPressed: () => controller.runLocal('timeConvert', (session) {
                  final result = engine.timestampToLocal(
                      session.left,
                      session.options['unit'] ?? 'second',
                      session.options['zone'] ?? 'Asia/Shanghai');
                  session.right = result.localTime;
                  session.options['detectedUnit'] = result.unit;
                  controller.recordToolHistory(
                      toolId: 'timeConvert',
                      title: 'toLocal',
                      input: session.left,
                      output: session.right);
                })),
        ToolActionButton(
            label: controller.t('time.toTimestamp'),
            onPressed: () => controller.runLocal('timeConvert', (session) {
                  session.left = engine.localToTimestamp(
                      session.right,
                      session.options['unit'] ?? 'second',
                      session.options['zone'] ?? 'Asia/Shanghai');
                })),
        ToolActionButton(
            label: controller.t('time.now'),
            onPressed: () => controller.runLocal('timeConvert', (session) {
                  final now = DateTime.now().millisecondsSinceEpoch;
                  session.left = '${now ~/ 1000}';
                  session.right = engine.formatLocalTime(
                      now, session.options['zone'] ?? 'Asia/Shanghai');
                })),
        DropdownButton<String>(
          value: session.options['zone'],
          isExpanded: true,
          items: [
            for (final zone in commonTimezones)
              DropdownMenuItem(
                  value: zone,
                  child: Text(zone, style: const TextStyle(fontSize: 11)))
          ],
          onChanged: (value) {
            if (value == null) return;
            session.options['zone'] = value;
            controller.refresh();
          },
        ),
      ],
    );
  }
}

class RegexToolPage extends StatelessWidget {
  const RegexToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = RegexEngine();
    final session = controller.localFor('regex');
    return IoWorkspace(
      controller: controller,
      toolId: 'regex',
      leftLabel: controller.t('regex.source'),
      rightLabel: controller.t('regex.matches'),
      headerActions: [
        CompactButton(
          label: controller.t('tool.favorite'),
          onPressed: () => controller.addFavorite(
              'regex', session.options['pattern'] ?? '', session.left),
        ),
      ],
      actions: [
        _field(controller, 'regex', 'pattern', r'(moo)(\d+)'),
        ToolActionButton(
            label: controller.t('regex.match'),
            onPressed: () => controller.runLocal('regex', (session) {
                  final hits = engine.matchRegex(
                      session.options['pattern'] ?? '',
                      session.left,
                      const RegexOptions(global: true));
                  session.right = [
                    for (final hit in hits)
                      '${hit.index}: ${hit.value} ${hit.groups}'
                  ].join('\n');
                  controller.recordToolHistory(
                      toolId: 'regex',
                      title: session.options['pattern'] ?? '',
                      input: session.left,
                      output: session.right);
                })),
        Wrap(
          spacing: 4,
          children: [
            for (final item in commonRegexes.take(8))
              CompactButton(
                label: item.$1,
                onPressed: () {
                  session.options['pattern'] = item.$2;
                  controller.refresh();
                },
              ),
          ],
        ),
      ],
    );
  }
}

class CronToolPage extends StatelessWidget {
  const CronToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = CronEngine();
    return IoWorkspace(
      controller: controller,
      toolId: 'cron',
      leftLabel: controller.t('cron.expression'),
      rightLabel: controller.t('cron.next'),
      headerActions: [
        CompactButton(
          label: controller.t('tool.favorite'),
          onPressed: () {
            final session = controller.localFor('cron');
            controller.addFavorite('cron', session.left, session.left);
          },
        ),
      ],
      actions: [
        ToolActionButton(
            label: controller.t('cron.nextRuns'),
            onPressed: () => controller.runLocal('cron', (session) {
                  final zone = session.options['zone'] ?? 'Asia/Shanghai';
                  final runs =
                      engine.nextCronRuns(session.left, zone, count: 10);
                  session.right =
                      '${engine.describeCron(session.left, controller.settings.language.name)}\n${runs.join('\n')}';
                  controller.recordToolHistory(
                      toolId: 'cron',
                      title: session.left,
                      input: session.left,
                      output: session.right);
                })),
        for (final preset in cronPresets)
          CompactButton(
            label: preset.$1,
            onPressed: () {
              controller.localFor('cron').left = preset.$2;
              controller.refresh();
            },
          ),
      ],
    );
  }
}

class UaParseToolPage extends StatelessWidget {
  const UaParseToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = UaEngine();
    return IoWorkspace(
      controller: controller,
      toolId: 'uaParse',
      leftLabel: 'User-Agent',
      rightLabel: controller.t('tool.io.output'),
      actions: [
        ToolActionButton(
            label: controller.t('ua.parse'),
            onPressed: () => controller.runLocal('uaParse', (session) {
                  final result = engine.parseUserAgent(session.left);
                  session.right = const JsonEncoder.withIndent('  ')
                      .convert(result.toJson());
                  controller.recordToolHistory(
                      toolId: 'uaParse',
                      title: result.browser,
                      input: session.left,
                      output: session.right);
                })),
        for (final preset in uaPresets.take(4))
          CompactButton(
            label: preset.$1,
            onPressed: () {
              controller.localFor('uaParse').left = preset.$2;
              controller.refresh();
            },
          ),
      ],
    );
  }
}

class ConfigConvertToolPage extends StatelessWidget {
  const ConfigConvertToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = ConfigEngine();
    return IoWorkspace(
      controller: controller,
      toolId: 'ymlProperties',
      leftLabel: controller.t('tool.io.input'),
      rightLabel: controller.t('tool.io.output'),
      actions: [
        ToolActionButton(
            label: 'Properties → YAML',
            onPressed: () => controller.runLocal('ymlProperties', (session) {
                  session.right = engine.propertiesToYaml(session.left);
                  controller.recordToolHistory(
                      toolId: 'ymlProperties',
                      title: 'propertiesToYaml',
                      input: session.left,
                      output: session.right);
                })),
        ToolActionButton(
            label: 'YAML → Properties',
            onPressed: () => controller.runLocal('ymlProperties', (session) {
                  session.right = engine.yamlToProperties(session.left);
                })),
        ToolActionButton(
            label: controller.t('config.format'),
            onPressed: () => controller.runLocal('ymlProperties', (session) {
                  session.right = engine.formatYaml(session.left);
                })),
        ToolActionButton(
            label: controller.t('config.validate'),
            onPressed: () => controller.runLocal('ymlProperties', (session) {
                  final result = engine.validateYaml(session.left);
                  session.right = result.valid
                      ? controller.t('config.valid')
                      : result.message;
                })),
      ],
    );
  }
}

class CryptoToolPage extends StatelessWidget {
  const CryptoToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = CryptoEngine();
    final session = controller.localFor('crypto');
    if (session.tab.isEmpty) session.tab = 'symmetric';
    return IoWorkspace(
      controller: controller,
      toolId: 'crypto',
      leftLabel: controller.t('tool.io.input'),
      rightLabel: controller.t('tool.io.output'),
      tabs: [
        ('symmetric', controller.t('crypto.tab.symmetric')),
        ('digest', controller.t('crypto.tab.digest')),
        ('base', controller.t('crypto.tab.base')),
        ('random', controller.t('crypto.tab.random')),
        ('asymmetric', controller.t('crypto.tab.asymmetric')),
      ],
      actions: [
        if (session.tab == 'symmetric') ...[
          _field(controller, 'crypto', 'algorithm', 'AES'),
          _field(controller, 'crypto', 'key', '1234567890abcdef'),
          ToolActionButton(
              label: controller.t('crypto.encrypt'),
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.right = engine.symmetricEncrypt(
                        session.options['algorithm'] ?? 'AES',
                        session.left,
                        session.options['key'] ?? '');
                    controller.recordToolHistory(
                        toolId: 'crypto',
                        title: 'encrypt',
                        input: session.left,
                        output: session.right);
                  })),
          ToolActionButton(
              label: controller.t('crypto.decrypt'),
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.left = engine.symmetricDecrypt(
                        session.options['algorithm'] ?? 'AES',
                        session.right,
                        session.options['key'] ?? '');
                  })),
        ],
        if (session.tab == 'digest') ...[
          _field(controller, 'crypto', 'digest', 'SHA-256'),
          ToolActionButton(
              label: controller.t('crypto.digest'),
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.right = engine.digestText(
                        session.options['digest'] ?? 'SHA-256', session.left);
                  })),
        ],
        if (session.tab == 'base') ...[
          ToolActionButton(
              label: 'Base64',
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.right = engine.encodeBase('Base64', session.left);
                  })),
          ToolActionButton(
              label: 'Base32',
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.right = engine.encodeBase('Base32', session.left);
                  })),
          ToolActionButton(
              label: controller.t('encode.reverse'),
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.left = engine.decodeBase(
                        session.options['base'] ?? 'Base64', session.right);
                  })),
        ],
        if (session.tab == 'random') ...[
          ToolActionButton(
              label: 'UUID',
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.right = engine.randomUuid();
                  })),
          ToolActionButton(
              label: controller.t('crypto.password'),
              onPressed: () => controller.runLocal('crypto', (session) {
                    session.right = engine.randomPassword(
                        int.tryParse(session.options['length'] ?? '24') ?? 24);
                  })),
        ],
        if (session.tab == 'asymmetric')
          Text(controller.t('crypto.asymmetricPending'),
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}

class ColorBoardToolPage extends StatelessWidget {
  const ColorBoardToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = ColorEngine();
    final session = controller.localFor('colorBoard');
    return IoWorkspace(
      controller: controller,
      toolId: 'colorBoard',
      leftLabel: controller.t('color.primary'),
      rightLabel: controller.t('color.result'),
      headerActions: [
        CompactButton(
          label: controller.t('tool.favorite'),
          onPressed: () =>
              controller.addFavorite('colorBoard', session.left, session.right),
        ),
        CompactButton(
          label: controller.t('color.pick'),
          onPressed: () => controller.pickScreenColorInto('colorBoard'),
        ),
      ],
      actions: [
        ToolActionButton(
            label: controller.t('color.parse'),
            onPressed: () => controller.runLocal('colorBoard', (session) {
                  final color = engine.parseColor(session.left);
                  session.right = engine.formatColor(color, 'HEX_UPPER');
                })),
        ToolActionButton(
            label: controller.t('color.invert'),
            onPressed: () => controller.runLocal('colorBoard', (session) {
                  final color = engine.applyColorOperation(
                      'invert',
                      engine.parseColor(session.left),
                      const RgbColor(r: 0, g: 0, b: 0));
                  session.right = engine.formatColor(color, 'HEX_UPPER');
                })),
        Wrap(
          spacing: 4,
          runSpacing: 4,
          children: [
            for (final color in standardColors)
              GestureDetector(
                onTap: () {
                  session.left = color;
                  controller.refresh();
                },
                child:
                    Container(width: 18, height: 18, color: _parseHex(color)),
              ),
          ],
        ),
      ],
    );
  }

  Color _parseHex(String value) {
    final hex = value.replaceFirst('#', '');
    return Color(int.parse('FF$hex', radix: 16));
  }
}

class TextDiffToolPage extends StatelessWidget {
  const TextDiffToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = DiffEngine();
    return IoWorkspace(
      controller: controller,
      toolId: 'textDiff',
      leftLabel: controller.t('diff.left'),
      rightLabel: controller.t('diff.right'),
      actions: [
        ToolActionButton(
            label: controller.t('diff.compare'),
            onPressed: () => controller.runLocal('textDiff', (session) {
                  final result =
                      engine.compareText(session.left, session.right, false);
                  session.notice =
                      '+${result.added} -${result.removed} ~${result.changed}';
                  session.options['unified'] = result.unified;
                  controller.recordToolHistory(
                      toolId: 'textDiff',
                      title: 'diff',
                      input: session.left,
                      output: result.unified);
                })),
        ToolActionButton(
            label: controller.t('diff.unified'),
            onPressed: () => controller.runLocal('textDiff', (session) {
                  session.notice = session.options['unified'] ?? '';
                })),
      ],
    );
  }
}

class QrCodeToolPage extends StatelessWidget {
  const QrCodeToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = QrEngine();
    final session = controller.localFor('qrCode');
    return IoWorkspace(
      controller: controller,
      toolId: 'qrCode',
      leftLabel: controller.t('qr.content'),
      rightLabel: controller.t('qr.note'),
      actions: [
        ToolActionButton(
            label: controller.t('qr.generate'),
            onPressed: () => controller.runLocal('qrCode', (session) {
                  final png = engine.generatePng(session.left,
                      errorCorrectionLevel: session.options['level'] ?? 'M');
                  session.right = 'png:${base64.encode(png)}';
                  session.notice = controller.t('qr.generated');
                  controller.recordToolHistory(
                      toolId: 'qrCode',
                      title: 'generate',
                      input: session.left,
                      output: '${png.length} bytes');
                })),
        if (session.right.startsWith('png:'))
          SizedBox(
              width: 120,
              height: 120,
              child: Image.memory(base64.decode(session.right.substring(4)),
                  fit: BoxFit.contain)),
        Text(controller.t('qr.recognizePending'),
            textAlign: TextAlign.center, style: const TextStyle(fontSize: 11)),
      ],
    );
  }
}

class ReformatToolPage extends StatelessWidget {
  const ReformatToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = ReformatEngine();
    final session = controller.localFor('reformat');
    if (session.tab.isEmpty) session.tab = 'nginx';
    return IoWorkspace(
      controller: controller,
      toolId: 'reformat',
      leftLabel: controller.t('tool.io.input'),
      rightLabel: controller.t('tool.io.output'),
      tabs: [
        ('nginx', 'Nginx'),
        ('xml', 'XML'),
        ('html', 'HTML'),
        ('java', 'Java'),
      ],
      actions: [
        ToolActionButton(
            label: controller.t('reformat.run'),
            onPressed: () => controller.runLocal('reformat', (session) {
                  session.right =
                      engine.formatCode(session.left, session.tab, 2);
                  controller.recordToolHistory(
                      toolId: 'reformat',
                      title: session.tab,
                      input: session.left,
                      output: session.right);
                })),
      ],
    );
  }
}

class ProtobufToolPage extends StatelessWidget {
  const ProtobufToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final engine = ProtobufEngine();
    final session = controller.localFor('protobuf');
    session.options.putIfAbsent('message', () => 'Person');
    session.options.putIfAbsent(
        'proto',
        () =>
            'syntax = "proto3"; message Person { string name = 1; int32 age = 2; repeated string tags = 3; }');
    return IoWorkspace(
      controller: controller,
      toolId: 'protobuf',
      leftLabel: 'JSON',
      rightLabel: 'Hex / Wire',
      actions: [
        ToolActionButton(
            label: 'JSON → Hex',
            onPressed: () => controller.runLocal('protobuf', (session) {
                  session.right = engine.jsonToProtobuf(
                      session.options['proto'] ?? '',
                      session.options['message'] ?? 'Person',
                      session.left,
                      'Hex');
                })),
        ToolActionButton(
            label: 'Hex → JSON',
            onPressed: () => controller.runLocal('protobuf', (session) {
                  session.left = engine.protobufToJson(
                      session.options['proto'] ?? '',
                      session.options['message'] ?? 'Person',
                      session.right,
                      'Hex');
                })),
        ToolActionButton(
            label: 'Wire',
            onPressed: () => controller.runLocal('protobuf', (session) {
                  session.notice = engine.decodeWire(session.right, 'Hex');
                })),
      ],
    );
  }
}

Widget _field(
    AppController controller, String toolId, String key, String fallback) {
  final session = controller.localFor(toolId);
  return TextField(
    controller: TextEditingController(text: session.options[key] ?? fallback)
      ..selection = TextSelection.collapsed(
          offset: (session.options[key] ?? fallback).length),
    style: const TextStyle(fontSize: 12, fontFamily: 'monospace'),
    decoration: InputDecoration(labelText: key, isDense: true),
    onChanged: (value) {
      session.options[key] = value;
      controller.scheduleSave();
    },
  );
}
