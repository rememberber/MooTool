import '../../app/product.dart';

const defaultUpdateFeedUrl =
    'https://raw.githubusercontent.com/rememberber/MooTool/master/update-manifest.json';
const defaultReleaseUrl = 'https://github.com/rememberber/MooTool/releases';

class UpdateClientIdentity {
  const UpdateClientIdentity({
    this.productId = Product.id,
    required this.platform,
    required this.architecture,
    this.packageType,
  });

  final String productId;
  final String platform;
  final String architecture;
  final String? packageType;
}

enum UpdateCheckStatus { unpublished, latest, available }

enum UpdateDownloadStatus { idle, available, downloading, ready, error }

class UpdateDownload {
  const UpdateDownload({
    required this.fileName,
    required this.packageType,
    required this.url,
    required this.sha512,
    required this.size,
  });

  final String fileName;
  final String packageType;
  final String url;
  final String sha512;
  final int size;
}

class UpdateCheckResult {
  const UpdateCheckResult({
    required this.status,
    required this.productId,
    required this.productName,
    required this.currentVersion,
    required this.latestVersion,
    required this.releaseUrl,
    required this.releaseNotes,
    required this.platform,
    required this.architecture,
    required this.checkedAt,
    this.download,
    this.message = '',
  });

  final UpdateCheckStatus status;
  final String productId;
  final String productName;
  final String currentVersion;
  final String latestVersion;
  final String releaseUrl;
  final String releaseNotes;
  final String platform;
  final String architecture;
  final DateTime checkedAt;
  final UpdateDownload? download;
  final String message;
}

class UpdateAsset {
  const UpdateAsset({
    required this.platform,
    required this.architecture,
    required this.packageType,
    required this.fileName,
    required this.url,
    required this.sha512,
    required this.size,
    required this.priority,
  });

  final String platform;
  final String architecture;
  final String packageType;
  final String fileName;
  final String url;
  final String sha512;
  final int size;
  final int priority;

  UpdateDownload toDownload() => UpdateDownload(
        fileName: fileName,
        packageType: packageType,
        url: url,
        sha512: sha512,
        size: size,
      );
}

class ProductRelease {
  const ProductRelease({
    required this.version,
    required this.title,
    required this.notes,
    required this.prerelease,
    required this.releaseUrl,
    required this.assets,
  });

  final String version;
  final String title;
  final String notes;
  final bool prerelease;
  final String releaseUrl;
  final List<UpdateAsset> assets;
}
