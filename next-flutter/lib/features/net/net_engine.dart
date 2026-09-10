import 'dart:io';

int ipv4ToLong(String value) {
  final parts = value.trim().split('.');
  if (parts.length != 4 ||
      parts.any((part) =>
          !RegExp(r'^\d{1,3}$').hasMatch(part) || int.parse(part) > 255)) {
    throw const FormatException('Invalid IPv4 address');
  }
  var result = 0;
  for (final part in parts) {
    result = result * 256 + int.parse(part);
  }
  return result;
}

String longToIpv4(Object value) {
  final number = value is String ? int.tryParse(value.trim()) : value as int?;
  if (number == null || number < 0 || number > 0xffffffff) {
    throw const FormatException('Invalid IPv4 number');
  }
  return [
    (number >> 24) & 0xff,
    (number >> 16) & 0xff,
    (number >> 8) & 0xff,
    number & 0xff,
  ].join('.');
}

String normalizeWhoisTarget(String value) {
  final target = value.trim().toLowerCase();
  if (target.isEmpty ||
      target.length > 253 ||
      !RegExp(r'^[a-z0-9.-]+$').hasMatch(target)) {
    throw const FormatException('INVALID_TARGET');
  }
  return target;
}

typedef WhoisQuery = Future<String> Function(String server, String target);

Future<String> queryWhois(String rawTarget, {WhoisQuery? query}) async {
  final target = normalizeWhoisTarget(rawTarget);
  final lookup = query ?? whoisServerQuery;
  final first = await lookup('whois.iana.org', target);
  final referral = RegExp(r'^(?:refer|whois):\s*(\S+)',
          multiLine: true, caseSensitive: false)
      .firstMatch(first)?[1];
  if (referral == null || referral == 'whois.iana.org') return first;
  return lookup(referral, target);
}

Future<String> whoisServerQuery(String server, String target) async {
  final socket =
      await Socket.connect(server, 43, timeout: const Duration(seconds: 15));
  try {
    socket.add('$target\r\n'.codeUnits);
    final chunks = <int>[];
    await for (final chunk in socket.timeout(const Duration(seconds: 15))) {
      chunks.addAll(chunk);
      if (chunks.length > 2 * 1024 * 1024) {
        throw const FormatException('WHOIS response exceeds 2 MB');
      }
    }
    return String.fromCharCodes(chunks).trim();
  } finally {
    socket.destroy();
  }
}

Future<String> localAddresses() async {
  final interfaces = await NetworkInterface.list(
      includeLoopback: true, type: InternetAddressType.any);
  final lines = <String>[];
  for (final iface in interfaces) {
    for (final address in iface.addresses) {
      lines.add('${iface.name}\t${address.type.name}\t${address.address}');
    }
  }
  if (lines.isEmpty) throw const FormatException('No local addresses');
  return lines.join('\n');
}
