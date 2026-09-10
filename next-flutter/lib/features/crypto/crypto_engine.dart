import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:pointycastle/export.dart';
import 'package:pointycastle/block/des_base.dart';

class CryptoEngine {
  String symmetricEncrypt(String algorithm, String content, String key) {
    final spec = _symmetric(algorithm);
    final cipher =
        PaddedBlockCipherImpl(PKCS7Padding(), ECBBlockCipher(spec.engine));
    cipher.init(
        true,
        PaddedBlockCipherParameters(
            KeyParameter(Uint8List.fromList(
                utf8.encode(_normalizeKey(key, spec.keyChars)))),
            null));
    return _toHex(cipher.process(Uint8List.fromList(utf8.encode(content))));
  }

  String symmetricDecrypt(String algorithm, String cipherHex, String key) {
    final spec = _symmetric(algorithm);
    final cipher =
        PaddedBlockCipherImpl(PKCS7Padding(), ECBBlockCipher(spec.engine));
    cipher.init(
        false,
        PaddedBlockCipherParameters(
            KeyParameter(Uint8List.fromList(
                utf8.encode(_normalizeKey(key, spec.keyChars)))),
            null));
    try {
      final output = utf8.decode(cipher.process(_fromHex(cipherHex)));
      if (output.isEmpty && cipherHex.trim().isNotEmpty) {
        throw const FormatException('Unable to decrypt with the supplied key');
      }
      return output;
    } catch (error) {
      throw const FormatException('Unable to decrypt with the supplied key');
    }
  }

  String digestText(String algorithm, String content) {
    final bytes = utf8.encode(content);
    switch (algorithm) {
      case 'MD5':
        return md5.convert(bytes).toString();
      case 'SHA-1':
        return sha1.convert(bytes).toString();
      case 'SHA-256':
        return sha256.convert(bytes).toString();
      case 'SHA-384':
        return sha384.convert(bytes).toString();
      case 'SHA-512':
        return sha512.convert(bytes).toString();
      case 'SM3':
        return _sm3(bytes);
      default:
        throw FormatException('Unsupported digest $algorithm');
    }
  }

  String encodeBase(String algorithm, String content) {
    if (algorithm == 'Base64') return base64.encode(utf8.encode(content));
    if (algorithm == 'Base32') return _base32Encode(utf8.encode(content));
    throw FormatException('Unsupported base algorithm $algorithm');
  }

  String decodeBase(String algorithm, String content) {
    final cleaned = content.replaceAll(RegExp(r'\s+'), '');
    if (algorithm == 'Base64') {
      try {
        final output = utf8.decode(base64.decode(cleaned));
        if (output.isEmpty && content.trim().isNotEmpty) {
          throw const FormatException('Invalid Base64 content');
        }
        return output;
      } catch (_) {
        throw const FormatException('Invalid Base64 content');
      }
    }
    if (algorithm == 'Base32') return utf8.decode(_base32Decode(cleaned));
    throw FormatException('Unsupported base algorithm $algorithm');
  }

  String randomUuid() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = _toHex(bytes);
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  String randomDigits(int length) => _randomFromAlphabet('0123456789', length);

  String randomString(int length) => _randomFromAlphabet(
      'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', length);

  String randomPassword(int length) {
    const categories = [
      'abcdefghijklmnopqrstuvwxyz',
      'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
      '0123456789',
      r"`~!@#$%^&*()_+-=[]{};':,./<>?",
    ];
    final size = _normalizeLength(length);
    final required = [
      for (var i = 0; i < size && i < categories.length; i++)
        _randomFromAlphabet(categories[i], 1)
    ];
    final rest = _randomFromAlphabet(
            categories.join(), (size - required.length).clamp(0, size))
        .split('');
    final values = [...required, ...rest];
    final random = Random.secure();
    for (var index = values.length - 1; index > 0; index--) {
      final target = random.nextInt(index + 1);
      final temp = values[index];
      values[index] = values[target];
      values[target] = temp;
    }
    return values.join();
  }

  String unsupportedAsymmetric(String algorithm) {
    throw FormatException(
        '$algorithm key generation / encrypt / sign is not implemented in this Flutter build yet');
  }

  ({BlockCipher engine, int keyChars}) _symmetric(String algorithm) {
    switch (algorithm) {
      case 'AES':
        return (engine: AESEngine(), keyChars: 16);
      case 'DES':
        return (engine: _DesEngine(), keyChars: 8);
      case 'SM4':
        throw const FormatException(
            'SM4 ECB+PKCS#7 is not implemented in this Flutter build yet');
      default:
        throw FormatException('Unsupported symmetric algorithm $algorithm');
    }
  }

  String _normalizeKey(String key, int length) {
    final chars = key.runes.toList();
    if (chars.length >= length) {
      return String.fromCharCodes(chars.take(length));
    }
    return (key + '0' * length).substring(0, length);
  }

  String _randomFromAlphabet(String alphabet, int length) {
    final size = _normalizeLength(length);
    final random = Random.secure();
    return [
      for (var i = 0; i < size; i++) alphabet[random.nextInt(alphabet.length)]
    ].join();
  }

  int _normalizeLength(int length) {
    if (length < 1 || length > 4096) {
      throw const FormatException('Length must be between 1 and 4096');
    }
    return length;
  }

  String _toHex(List<int> bytes) =>
      bytes.map((byte) => byte.toRadixString(16).padLeft(2, '0')).join();

  Uint8List _fromHex(String value) {
    final cleaned = value.replaceAll(RegExp(r'\s+'), '');
    if (!RegExp(r'^(?:[0-9a-fA-F]{2})*$').hasMatch(cleaned)) {
      throw const FormatException('Invalid hexadecimal content');
    }
    final bytes = Uint8List(cleaned.length ~/ 2);
    for (var i = 0; i < bytes.length; i++) {
      bytes[i] = int.parse(cleaned.substring(i * 2, i * 2 + 2), radix: 16);
    }
    return bytes;
  }

  String _base32Encode(List<int> bytes) {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    var buffer = 0;
    var bits = 0;
    final output = StringBuffer();
    for (final byte in bytes) {
      buffer = (buffer << 8) | byte;
      bits += 8;
      while (bits >= 5) {
        output.write(alphabet[(buffer >> (bits - 5)) & 31]);
        bits -= 5;
      }
    }
    if (bits > 0) output.write(alphabet[(buffer << (5 - bits)) & 31]);
    return output.toString();
  }

  List<int> _base32Decode(String value) {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    final cleaned = value.toUpperCase().replaceAll('=', '');
    var buffer = 0;
    var bits = 0;
    final output = <int>[];
    for (final rune in cleaned.runes) {
      final index = alphabet.indexOf(String.fromCharCode(rune));
      if (index < 0) throw const FormatException('Invalid Base32 content');
      buffer = (buffer << 5) | index;
      bits += 5;
      if (bits >= 8) {
        output.add((buffer >> (bits - 8)) & 0xff);
        bits -= 8;
      }
    }
    return output;
  }
}

String _sm3(List<int> message) {
  final padded = _sm3Pad(message);
  var iv = [
    0x7380166f,
    0x4914b2b9,
    0x172442d7,
    0xda8a0600,
    0xa96f30bc,
    0x163138aa,
    0xe38dee4d,
    0xb0fb0e4e
  ];
  for (var offset = 0; offset < padded.length; offset += 64) {
    iv = _sm3Compress(iv, padded.sublist(offset, offset + 64));
  }
  return iv.map((word) => word.toRadixString(16).padLeft(8, '0')).join();
}

Uint8List _sm3Pad(List<int> message) {
  final bitLength = message.length * 8;
  final paddedLength = ((message.length + 9 + 63) ~/ 64) * 64;
  final output = Uint8List(paddedLength);
  output.setAll(0, message);
  output[message.length] = 0x80;
  final view = ByteData.sublistView(output);
  view.setUint32(paddedLength - 4, bitLength & 0xffffffff, Endian.big);
  view.setUint32(paddedLength - 8,
      (bitLength / 0x100000000).floor() & 0xffffffff, Endian.big);
  return output;
}

List<int> _sm3Compress(List<int> iv, List<int> block) {
  final w = List<int>.filled(68, 0);
  final wPrime = List<int>.filled(64, 0);
  final view = ByteData.sublistView(Uint8List.fromList(block));
  for (var i = 0; i < 16; i++) {
    w[i] = view.getUint32(i * 4, Endian.big);
  }
  for (var j = 16; j < 68; j++) {
    w[j] = _rotl(
            _p1(w[j - 16] ^ w[j - 9] ^ _rotl(w[j - 3], 15)) ^
                _rotl(w[j - 13], 7) ^
                w[j - 6],
            0) &
        0xffffffff;
  }
  for (var j = 0; j < 64; j++) {
    wPrime[j] = (w[j] ^ w[j + 4]) & 0xffffffff;
  }
  var a = iv[0],
      b = iv[1],
      c = iv[2],
      d = iv[3],
      e = iv[4],
      f = iv[5],
      g = iv[6],
      h = iv[7];
  for (var j = 0; j < 64; j++) {
    final tj = j < 16 ? 0x79cc4519 : 0x7a879d8a;
    final ss1 = _rotl((_rotl(a, 12) + e + _rotl(tj, j)) & 0xffffffff, 7);
    final ss2 = ss1 ^ _rotl(a, 12);
    final tt1 = ((_ff(j, a, b, c) + d + ss2 + wPrime[j]) & 0xffffffff);
    final tt2 = ((_gg(j, e, f, g) + h + ss1 + w[j]) & 0xffffffff);
    d = c;
    c = _rotl(b, 9);
    b = a;
    a = tt1;
    h = g;
    g = _rotl(f, 19);
    f = e;
    e = _p0(tt2);
  }
  return [
    (a ^ iv[0]) & 0xffffffff,
    (b ^ iv[1]) & 0xffffffff,
    (c ^ iv[2]) & 0xffffffff,
    (d ^ iv[3]) & 0xffffffff,
    (e ^ iv[4]) & 0xffffffff,
    (f ^ iv[5]) & 0xffffffff,
    (g ^ iv[6]) & 0xffffffff,
    (h ^ iv[7]) & 0xffffffff,
  ];
}

int _rotl(int value, int n) {
  final shift = n % 32;
  final masked = value & 0xffffffff;
  if (shift == 0) return masked;
  return ((masked << shift) | (masked >> (32 - shift))) & 0xffffffff;
}

int _p0(int x) => (x ^ _rotl(x, 9) ^ _rotl(x, 17)) & 0xffffffff;
int _p1(int x) => (x ^ _rotl(x, 15) ^ _rotl(x, 23)) & 0xffffffff;
int _ff(int j, int x, int y, int z) => j < 16
    ? (x ^ y ^ z) & 0xffffffff
    : ((x & y) | (x & z) | (y & z)) & 0xffffffff;
int _gg(int j, int x, int y, int z) =>
    j < 16 ? (x ^ y ^ z) & 0xffffffff : ((x & y) | ((~x) & z)) & 0xffffffff;

class _DesEngine extends DesBase implements BlockCipher {
  static const _blockSize = 8;
  List<int>? _workingKey;

  @override
  String get algorithmName => 'DES';

  @override
  int get blockSize => _blockSize;

  @override
  void init(bool forEncryption, covariant CipherParameters? params) {
    if (params is! KeyParameter) {
      throw ArgumentError('DES requires a KeyParameter');
    }
    if (params.key.length != 8) {
      throw ArgumentError('DES key must be 8 bytes');
    }
    _workingKey = generateWorkingKey(forEncryption, params.key);
  }

  @override
  Uint8List process(Uint8List data) {
    final out = Uint8List(blockSize);
    processBlock(data, 0, out, 0);
    return out;
  }

  @override
  int processBlock(Uint8List inp, int inpOff, Uint8List out, int outOff) {
    desFunc(_workingKey!, inp, inpOff, out, outOff);
    return _blockSize;
  }

  @override
  void reset() {}
}
