import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../local/io_workspace.dart';
import 'net_engine.dart';

class NetToolPage extends StatelessWidget {
  const NetToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.localFor('net');
    if (session.tab.isEmpty) session.tab = 'ipv4';
    return IoWorkspace(
      controller: controller,
      toolId: 'net',
      leftLabel: controller.t('tool.io.input'),
      rightLabel: controller.t('tool.io.output'),
      tabs: [
        ('ipv4', controller.t('net.tab.ipv4')),
        ('lookup', controller.t('net.tab.lookup')),
        ('ping', controller.t('net.tab.ping')),
        ('whois', controller.t('net.tab.whois')),
        ('local', controller.t('net.tab.local')),
      ],
      actions: [
        ToolActionButton(
            label: controller.t('net.run'),
            icon: Icons.play_arrow,
            onPressed: () {
              unawaited(_run());
            }),
      ],
    );
  }

  Future<void> _run() async {
    final session = controller.localFor('net');
    try {
      switch (session.tab) {
        case 'lookup':
          final results = await InternetAddress.lookup(session.left.trim());
          controller.runLocal('net', (current) {
            current.right = [
              for (final item in results) item.address
            ].join('\n');
            if (current.right.isEmpty) current.right = controller.t('net.empty');
          });
        case 'ping':
          final host = session.left.trim();
          if (!RegExp(r'^[A-Za-z0-9.:_-]+$').hasMatch(host)) {
            throw const FormatException('INVALID_TARGET');
          }
          final args = Platform.isWindows
              ? ['-n', '1', host]
              : ['-c', '1', host];
          final result = await Process.run('ping', args);
          controller.runLocal('net', (current) {
            current.right = '${result.stdout}\n${result.stderr}'.trim();
            if (result.exitCode != 0 && current.right.isEmpty) {
              current.right = 'ping exit ${result.exitCode}';
            }
          });
        case 'whois':
          final output = await queryWhois(session.left);
          controller.runLocal('net', (current) => current.right = output);
        case 'local':
          final output = await localAddresses();
          controller.runLocal('net', (current) => current.right = output);
        default:
          controller.runLocal('net', (current) {
            final input = current.left.trim();
            current.right = input.contains('.')
                ? '${ipv4ToLong(input)}'
                : longToIpv4(input);
          });
      }
    } catch (error) {
      controller.runLocal('net', (current) {
        current.right = '';
        current.notice = error.toString();
      });
    }
  }
}
