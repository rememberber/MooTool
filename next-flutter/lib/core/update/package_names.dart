class PackageSpec {
  const PackageSpec({
    required this.platform,
    required this.architecture,
    required this.packageType,
    required this.fileName,
  });

  final String platform;
  final String architecture;
  final String packageType;
  final String fileName;
}

class PackageNames {
  static const productSlug = 'MooTool-Next-Flutter';

  static String artifact({
    required String version,
    required String platform,
    required String architecture,
    required String packageType,
  }) {
    final spec = specFor(
      version: version,
      platform: platform,
      architecture: architecture,
      packageType: packageType,
    );
    return spec.fileName;
  }

  static PackageSpec specFor({
    required String version,
    required String platform,
    required String architecture,
    required String packageType,
  }) {
    final arch = normalizeArchitecture(architecture);
    final type = packageType.trim().toLowerCase();
    switch (platform.trim().toLowerCase()) {
      case 'darwin':
      case 'macos':
        if (type != 'dmg') {
          throw FormatException('macOS packages must be dmg, not $type');
        }
        return PackageSpec(
          platform: 'darwin',
          architecture: arch,
          packageType: 'dmg',
          fileName: '$productSlug-$version-mac-$arch.dmg',
        );
      case 'win32':
      case 'windows':
        if (type == 'nsis' || type == 'setup') {
          return PackageSpec(
            platform: 'win32',
            architecture: 'x64',
            packageType: 'nsis',
            fileName: '$productSlug-$version-win-x64-setup.exe',
          );
        }
        if (type == 'portable' || type == 'zip') {
          return PackageSpec(
            platform: 'win32',
            architecture: 'x64',
            packageType: 'portable',
            fileName: '$productSlug-$version-win-x64-portable.zip',
          );
        }
        throw FormatException('Windows package type must be nsis or portable');
      case 'linux':
        if (type == 'appimage') {
          return PackageSpec(
            platform: 'linux',
            architecture: 'x64',
            packageType: 'appimage',
            fileName: '$productSlug-$version-linux-x64.AppImage',
          );
        }
        if (type == 'deb') {
          return PackageSpec(
            platform: 'linux',
            architecture: 'x64',
            packageType: 'deb',
            fileName: '$productSlug-$version-linux-x64.deb',
          );
        }
        throw FormatException('Linux package type must be appimage or deb');
      default:
        throw FormatException('Unknown packaging platform: $platform');
    }
  }

  static String normalizeArchitecture(String value) {
    final normalized = value.trim().toLowerCase();
    if (normalized == 'aarch64') return 'arm64';
    if (normalized == 'amd64' || normalized == 'x86_64') return 'x64';
    return normalized;
  }
}
