import 'dart:convert';
import 'dart:io';

class ProductSecrets {
  ProductSecrets({this.gitToken = '', this.proxyPassword = ''});
  String gitToken;
  String proxyPassword;
  bool get isEmpty => gitToken.isEmpty && proxyPassword.isEmpty;
}

class SecretStore {
  SecretStore(this.file);
  final File file;

  Future<ProductSecrets> load() async {
    if (!await file.exists()) return ProductSecrets();
    try {
      final decoded = jsonDecode(await file.readAsString());
      if (decoded is! Map) return ProductSecrets();
      final map = Map<String, Object?>.from(decoded);
      return ProductSecrets(
        gitToken: map['gitToken'] as String? ?? '',
        proxyPassword: map['proxyPassword'] as String? ?? '',
      );
    } catch (_) {
      return ProductSecrets();
    }
  }

  Future<void> save(ProductSecrets secrets) async {
    await file.parent.create(recursive: true);
    if (secrets.isEmpty) {
      if (await file.exists()) await file.delete();
      return;
    }
    await file.writeAsString(
        const JsonEncoder.withIndent('  ').convert({
          'gitToken': secrets.gitToken,
          'proxyPassword': secrets.proxyPassword,
        }),
        flush: true);
    if (!Platform.isWindows) {
      await Process.run('chmod', ['600', file.path]);
    }
  }

  static ProductSecrets takeFromSettingsJson(Map<String, Object?> json) {
    final secrets = ProductSecrets(
      gitToken: json['gitToken'] as String? ?? '',
      proxyPassword: json['proxyPassword'] as String? ?? '',
    );
    json.remove('gitToken');
    json.remove('proxyPassword');
    return secrets;
  }
}
