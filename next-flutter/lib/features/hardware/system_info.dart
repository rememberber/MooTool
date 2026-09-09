import 'dart:ffi';
import 'dart:io';

class HardwareItem {
  HardwareItem(this.label, this.value);
  final String label;
  final String value;
}

class HardwareGroup {
  HardwareGroup(this.title, this.items);
  final String title;
  final List<HardwareItem> items;
}

class HardwareSnapshot {
  HardwareSnapshot({required this.collectedAt, required this.sections});
  final DateTime collectedAt;
  final Map<String, List<HardwareGroup>> sections;
}

Future<HardwareSnapshot> collectSystemInfo() async {
  final interfaces = await NetworkInterface.list(
      includeLoopback: true, type: InternetAddressType.any);
  final memory = await _memoryBytes();
  return HardwareSnapshot(
    collectedAt: DateTime.now(),
    sections: {
      'system': [
        HardwareGroup('Operating system', [
          HardwareItem('Platform', Platform.operatingSystem),
          HardwareItem('Version', Platform.operatingSystemVersion),
          HardwareItem('Architecture', Abi.current().toString()),
          HardwareItem('Host name', Platform.localHostname),
          HardwareItem('Locale', Platform.localeName),
          HardwareItem('Dart', Platform.version),
        ]),
      ],
      'cpu': [
        HardwareGroup('Processor', [
          HardwareItem('Logical cores', '${Platform.numberOfProcessors}'),
        ]),
      ],
      'memory': [
        HardwareGroup('Physical memory', [
          HardwareItem(
              'Total', memory == null ? 'unavailable' : _formatBytes(memory)),
          HardwareItem('Process RSS', _formatBytes(ProcessInfo.currentRss)),
        ]),
      ],
      'storage': [
        HardwareGroup('Working directory', [
          HardwareItem('Path', Directory.current.path),
        ]),
      ],
      'network': [
        for (final iface in interfaces)
          HardwareGroup(iface.name, [
            for (final address in iface.addresses)
              HardwareItem(
                  address.type == InternetAddressType.IPv6 ? 'IPv6' : 'IPv4',
                  address.address),
          ]),
      ],
    },
  );
}

Future<int?> _memoryBytes() async {
  try {
    if (Platform.isMacOS) {
      final result =
          await Process.run('/usr/sbin/sysctl', ['-n', 'hw.memsize']);
      return int.tryParse('${result.stdout}'.trim());
    }
    if (Platform.isLinux) {
      final lines = await File('/proc/meminfo').readAsLines();
      for (final line in lines) {
        if (line.startsWith('MemTotal:')) {
          final kb = int.tryParse(line.split(RegExp(r'\s+'))[1]);
          return kb == null ? null : kb * 1024;
        }
      }
    }
  } catch (_) {}
  return null;
}

String _formatBytes(int bytes) {
  if (bytes < 1024) return '$bytes B';
  if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KiB';
  if (bytes < 1024 * 1024 * 1024) {
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MiB';
  }
  return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GiB';
}
