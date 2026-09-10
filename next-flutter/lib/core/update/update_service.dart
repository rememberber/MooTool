import 'dart:convert';
import 'dart:ffi';
import 'dart:io';

import '../../app/product.dart';
import 'package_names.dart';
import 'semver.dart';
import 'update_models.dart';

typedef UpdateTextFetcher = Future<UpdateTextResponse> Function(Uri url);

class UpdateTextResponse {
  const UpdateTextResponse({required this.statusCode, required this.body});
  final int statusCode;
  final String body;
  bool get ok => statusCode >= 200 && statusCode < 300;
}

class UpdateService {
  UpdateService({
    this.feedUrl = defaultUpdateFeedUrl,
    UpdateClientIdentity? identity,
    UpdateTextFetcher? fetcher,
  })  : identity = identity ?? detectUpdateIdentity(),
        fetcher = fetcher ?? defaultUpdateFetcher;

  final String feedUrl;
  final UpdateClientIdentity identity;
  final UpdateTextFetcher fetcher;

  Future<UpdateCheckResult> check(String currentVersion) async {
    if (identity.productId != Product.id) {
      throw StateError('Update productId is compiled to ${Product.id}');
    }
    final uri = Uri.parse(feedUrl);
    if (uri.scheme != 'https') {
      throw FormatException('Update feed must use HTTPS');
    }
    final response = await fetcher(uri);
    if (!response.ok) {
      throw FormatException(
          'Update server returned HTTP ${response.statusCode}');
    }
    if (response.body.isEmpty || response.body.length > 2 * 1024 * 1024) {
      throw FormatException('Invalid update response');
    }
    final normalizedCurrent = normalizeVersion(currentVersion);
    final product = _parseManifest(response.body, identity.productId);
    if (product == null) {
      return _unpublished(normalizedCurrent, Product.displayName);
    }
    final allowPrerelease =
        parseVersion(normalizedCurrent).prerelease.isNotEmpty;
    final eligible = [
      for (final release in product.releases)
        if (allowPrerelease || !release.prerelease) release
    ];
    if (eligible.isEmpty) {
      return _unpublished(normalizedCurrent, product.displayName);
    }
    var latest = eligible.first;
    for (final release in eligible.skip(1)) {
      if (compareVersions(release.version, latest.version) > 0) {
        latest = release;
      }
    }
    final available = compareVersions(latest.version, normalizedCurrent) > 0;
    return UpdateCheckResult(
      status:
          available ? UpdateCheckStatus.available : UpdateCheckStatus.latest,
      productId: Product.id,
      productName: product.displayName,
      currentVersion: normalizedCurrent,
      latestVersion: latest.version,
      releaseUrl: latest.releaseUrl,
      releaseNotes:
          available ? releaseNotesAfter(eligible, normalizedCurrent) : '',
      platform: identity.platform,
      architecture: PackageNames.normalizeArchitecture(identity.architecture),
      checkedAt: DateTime.now().toUtc(),
      download: available ? selectUpdateAsset(latest.assets, identity) : null,
    );
  }

  UpdateCheckResult _unpublished(String current, String name) {
    return UpdateCheckResult(
      status: UpdateCheckStatus.unpublished,
      productId: Product.id,
      productName: name,
      currentVersion: current,
      latestVersion: current,
      releaseUrl: defaultReleaseUrl,
      releaseNotes: '',
      platform: identity.platform,
      architecture: PackageNames.normalizeArchitecture(identity.architecture),
      checkedAt: DateTime.now().toUtc(),
      message: 'unpublished',
    );
  }
}

class _ParsedProduct {
  const _ParsedProduct(this.displayName, this.releases);
  final String displayName;
  final List<ProductRelease> releases;
}

_ParsedProduct? _parseManifest(String raw, String productId) {
  late final Object? decoded;
  try {
    decoded = jsonDecode(raw);
  } on FormatException {
    throw FormatException('Update response is not valid JSON');
  }
  if (decoded is! Map) {
    throw FormatException('Unsupported update manifest');
  }
  final map = Map<String, Object?>.from(decoded);
  if (map['schemaVersion'] != 1 || map['products'] is! Map) {
    throw FormatException('Unsupported update manifest');
  }
  final products = Map<String, Object?>.from(map['products'] as Map);
  final productValue = products[productId];
  if (productValue == null) return null;
  if (productValue is! Map) {
    throw FormatException('Update product is not registered: $productId');
  }
  final product = Map<String, Object?>.from(productValue);
  final status = product['status'] as String? ?? '';
  if (status != 'active') return null;
  if (product['displayName'] is! String || product['releases'] is! List) {
    throw FormatException('Invalid update product: $productId');
  }
  final releases = [
    for (final item in product['releases'] as List) parseRelease(item)
  ];
  if ({for (final release in releases) release.version}.length !=
      releases.length) {
    throw FormatException(
        'Update product has duplicate release versions: $productId');
  }
  return _ParsedProduct(
    _clip(product['displayName'] as String, 120),
    releases,
  );
}

ProductRelease parseRelease(Object? value) {
  if (value is! Map) {
    throw FormatException('Invalid release in update manifest');
  }
  final map = Map<String, Object?>.from(value);
  if (map['version'] is! String || map['releaseUrl'] is! String) {
    throw FormatException('Invalid release in update manifest');
  }
  final version = normalizeVersion(map['version'] as String);
  final prerelease = parseVersion(version).prerelease.isNotEmpty;
  if (map['prerelease'] != null && map['prerelease'] is! bool) {
    throw FormatException('Invalid prerelease flag for version $version');
  }
  if (map['prerelease'] is bool && map['prerelease'] != prerelease) {
    throw FormatException('Prerelease flag does not match version $version');
  }
  return ProductRelease(
    version: version,
    title: map['title'] is String ? _clip(map['title'] as String, 300) : '',
    notes: map['notes'] is String ? _clip(map['notes'] as String, 5000) : '',
    prerelease: prerelease,
    releaseUrl: normalizeHttpsUrl(map['releaseUrl'] as String, 'release'),
    assets: [
      for (final item in map['assets'] as List? ?? const []) parseAsset(item)
    ],
  );
}

UpdateAsset parseAsset(Object? value) {
  if (value is! Map) throw FormatException('Invalid asset in update manifest');
  final map = Map<String, Object?>.from(value);
  if (map['platform'] is! String ||
      map['architecture'] is! String ||
      map['packageType'] is! String ||
      map['fileName'] is! String ||
      map['url'] is! String ||
      map['sha512'] is! String ||
      map['size'] is! num) {
    throw FormatException('Invalid asset in update manifest');
  }
  final fileName = (map['fileName'] as String).trim();
  if (fileName.isEmpty ||
      fileName.length > 240 ||
      fileName.contains('/') ||
      fileName.contains('\\')) {
    throw FormatException('Invalid update asset file name');
  }
  final sha512 = (map['sha512'] as String).trim();
  if (!RegExp(r'^[A-Za-z0-9+/]{86}==$').hasMatch(sha512)) {
    throw FormatException('Invalid update asset SHA-512');
  }
  final size = (map['size'] as num).toInt();
  if (size <= 0) throw FormatException('Invalid update asset size');
  return UpdateAsset(
    platform: (map['platform'] as String).trim().toLowerCase(),
    architecture:
        PackageNames.normalizeArchitecture(map['architecture'] as String),
    packageType: (map['packageType'] as String).trim().toLowerCase(),
    fileName: fileName,
    url: normalizeHttpsUrl(map['url'] as String, 'asset'),
    sha512: sha512,
    size: size,
    priority: map['priority'] is num ? (map['priority'] as num).toInt() : 100,
  );
}

UpdateDownload? selectUpdateAsset(
    List<UpdateAsset> assets, UpdateClientIdentity target) {
  final platform = target.platform.trim().toLowerCase();
  final architecture = PackageNames.normalizeArchitecture(target.architecture);
  final packageType = target.packageType?.trim().toLowerCase();
  final matches = [
    for (final asset in assets)
      if (asset.platform == platform &&
          (asset.architecture == architecture ||
              asset.architecture == 'universal') &&
          (platform != 'darwin' || asset.packageType == 'dmg'))
        asset
  ]..sort((left, right) {
      final architectureOrder = (left.architecture != architecture ? 1 : 0) -
          (right.architecture != architecture ? 1 : 0);
      if (architectureOrder != 0) return architectureOrder;
      if (packageType != null && packageType.isNotEmpty) {
        final packageOrder = (left.packageType != packageType ? 1 : 0) -
            (right.packageType != packageType ? 1 : 0);
        if (packageOrder != 0) return packageOrder;
      }
      if (left.priority != right.priority) {
        return left.priority - right.priority;
      }
      return packagePreference(platform, left.packageType) -
          packagePreference(platform, right.packageType);
    });
  return matches.isEmpty ? null : matches.first.toDownload();
}

int packagePreference(String platform, String packageType) {
  const preferences = {
    'darwin': ['dmg', 'pkg'],
    'win32': ['nsis', 'msi', 'portable', 'zip'],
    'linux': ['appimage', 'deb', 'rpm', 'tar.gz'],
  };
  final index =
      (preferences[platform] ?? const <String>[]).indexOf(packageType);
  return index < 0 ? 100 : index;
}

String releaseNotesAfter(List<ProductRelease> releases, String currentVersion) {
  final newer = [
    for (final release in releases)
      if (compareVersions(release.version, currentVersion) > 0) release
  ]..sort((left, right) => compareVersions(right.version, left.version));
  final selected = <String>[];
  var length = 0;
  for (final release in newer) {
    final heading = release.title.contains(release.version)
        ? release.title
        : [release.version, release.title]
            .where((part) => part.isNotEmpty)
            .join(' — ');
    final section =
        [heading, release.notes].where((part) => part.isNotEmpty).join('\n');
    final clipped =
        section.length > 12000 ? section.substring(0, 12000) : section;
    final added = clipped.length + (selected.isNotEmpty ? 2 : 0);
    if (selected.isNotEmpty && length + added > 12000) break;
    selected.insert(0, clipped);
    length += added;
  }
  return selected.join('\n\n');
}

String normalizeHttpsUrl(String value, String kind) {
  final url = Uri.tryParse(value.trim());
  if (url == null || url.scheme != 'https' || url.host.isEmpty) {
    throw FormatException('Update $kind URL must use HTTPS');
  }
  return url.toString();
}

UpdateClientIdentity detectUpdateIdentity() {
  final platform = Platform.isMacOS
      ? 'darwin'
      : Platform.isWindows
          ? 'win32'
          : 'linux';
  return UpdateClientIdentity(
    platform: platform,
    architecture: currentArchitecture(),
    packageType: detectPackageType(platform),
  );
}

String currentArchitecture() {
  final abi = Abi.current().toString().toLowerCase();
  if (abi.contains('arm64') || abi.contains('aarch64')) return 'arm64';
  if (abi.contains('ia32') || abi.contains('x86')) return 'x86';
  return 'x64';
}

String? detectPackageType(String platform) {
  if (platform == 'darwin') return 'dmg';
  if (platform == 'win32') return 'nsis';
  if (Platform.environment.containsKey('APPIMAGE')) return 'appimage';
  return 'deb';
}

Future<UpdateTextResponse> defaultUpdateFetcher(Uri url) async {
  final client = HttpClient();
  try {
    client.connectionTimeout = const Duration(seconds: 10);
    final request = await client.getUrl(url);
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    request.headers.set(HttpHeaders.userAgentHeader, 'MooTool-Next-Flutter');
    final response = await request.close().timeout(const Duration(seconds: 10));
    final body = await utf8.decodeStream(response);
    return UpdateTextResponse(statusCode: response.statusCode, body: body);
  } finally {
    client.close(force: true);
  }
}

String _clip(String value, int max) =>
    value.length <= max ? value : value.substring(0, max);
