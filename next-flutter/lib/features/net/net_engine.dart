int ipv4ToLong(String value) {
  final parts = value.trim().split('.');
  if (parts.length != 4 ||
      parts.any((part) => !RegExp(r'^\d{1,3}$').hasMatch(part) || int.parse(part) > 255)) {
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
