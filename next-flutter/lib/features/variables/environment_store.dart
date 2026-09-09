import 'dart:convert';
import 'dart:io';

import '../../app/product.dart';

class EnvironmentEntry {
  EnvironmentEntry(
      {required this.key, required this.value, this.scope = 'process'});
  final String key;
  String value;
  final String scope;
}

class EnvironmentStore {
  EnvironmentStore(this.file);
  final File file;

  Future<Map<String, String>> readUser() async {
    if (!await file.exists()) return {};
    final json = jsonDecode(await file.readAsString());
    if (json is! Map) return {};
    return {for (final entry in json.entries) '${entry.key}': '${entry.value}'};
  }

  Future<void> writeUser(Map<String, String> values) async {
    await file.parent.create(recursive: true);
    final encoded = const JsonEncoder.withIndent('  ').convert(values);
    await file.writeAsString(encoded);
  }

  Future<List<EnvironmentEntry>> processEntries() async {
    final entries = [
      for (final entry in Platform.environment.entries)
        EnvironmentEntry(key: entry.key, value: entry.value)
    ]..sort((a, b) => a.key.compareTo(b.key));
    return entries;
  }

  List<EnvironmentEntry> runtimeEntries() {
    return [
      EnvironmentEntry(key: 'product.id', value: Product.id, scope: 'runtime'),
      EnvironmentEntry(
          key: 'product.version', value: Product.version, scope: 'runtime'),
      EnvironmentEntry(
          key: 'product.applicationId',
          value: Product.applicationId,
          scope: 'runtime'),
      EnvironmentEntry(
          key: 'dart.version', value: Platform.version, scope: 'runtime'),
      EnvironmentEntry(
          key: 'os.name', value: Platform.operatingSystem, scope: 'runtime'),
      EnvironmentEntry(
          key: 'os.version',
          value: Platform.operatingSystemVersion,
          scope: 'runtime'),
      EnvironmentEntry(
          key: 'os.locale', value: Platform.localeName, scope: 'runtime'),
      EnvironmentEntry(
          key: 'host.name', value: Platform.localHostname, scope: 'runtime'),
      EnvironmentEntry(
          key: 'processors',
          value: '${Platform.numberOfProcessors}',
          scope: 'runtime'),
      EnvironmentEntry(
          key: 'executable',
          value: Platform.resolvedExecutable,
          scope: 'runtime'),
    ];
  }
}
