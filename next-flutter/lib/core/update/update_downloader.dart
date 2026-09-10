import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:path/path.dart' as p;

import 'update_models.dart';

typedef UpdateBytesFetcher = Future<UpdateBytesResponse> Function(Uri url);

class UpdateBytesResponse {
  const UpdateBytesResponse({required this.statusCode, required this.body});
  final int statusCode;
  final Uint8List body;
  bool get ok => statusCode >= 200 && statusCode < 300;
}

class DownloadProgress {
  const DownloadProgress({
    required this.percent,
    required this.transferred,
    required this.total,
  });
  final double percent;
  final int transferred;
  final int total;
}

class UpdateDownloader {
  UpdateDownloader({
    required this.directory,
    this.fetcher = defaultUpdateBytesFetcher,
    this.opener = openLocalPath,
  });

  final Directory directory;
  final UpdateBytesFetcher fetcher;
  final Future<bool> Function(String path) opener;
  bool _cancelled = false;

  void cancel() => _cancelled = true;

  Future<File> download(
    UpdateDownload download, {
    void Function(DownloadProgress progress)? onProgress,
  }) async {
    _cancelled = false;
    if (!download.url.startsWith('https://')) {
      throw FormatException('Update download must use HTTPS');
    }
    await directory.create(recursive: true);
    final destination = File(p.join(directory.path, download.fileName));
    final temporary = File(p.join(
        directory.path, '.${download.fileName}.${pidOrZero()}.download'));
    onProgress?.call(
        DownloadProgress(percent: 0, transferred: 0, total: download.size));
    final response = await fetcher(Uri.parse(download.url));
    if (_cancelled) {
      throw const UpdateCancelled();
    }
    if (!response.ok) {
      throw FormatException(
          'Update download returned HTTP ${response.statusCode}');
    }
    if (response.body.length != download.size) {
      throw FormatException(
          'Update download size mismatch: expected ${download.size}, received ${response.body.length}');
    }
    final actual = base64Encode(sha512.convert(response.body).bytes);
    if (actual != download.sha512) {
      throw FormatException('Update download checksum mismatch');
    }
    await temporary.writeAsBytes(response.body, flush: true);
    if (await destination.exists()) await destination.delete();
    await temporary.rename(destination.path);
    onProgress?.call(DownloadProgress(
        percent: 100, transferred: download.size, total: download.size));
    return destination;
  }

  Future<bool> openInstaller(File file) async {
    if (!await file.exists()) {
      throw FormatException('Downloaded update is missing');
    }
    return opener(file.path);
  }
}

class UpdateCancelled implements Exception {
  const UpdateCancelled();
  @override
  String toString() => 'Update download cancelled';
}

int pidOrZero() {
  try {
    return pid;
  } catch (_) {
    return 0;
  }
}

Future<UpdateBytesResponse> defaultUpdateBytesFetcher(Uri url) async {
  final client = HttpClient();
  try {
    client.connectionTimeout = const Duration(minutes: 2);
    final request = await client.getUrl(url);
    request.headers.set(HttpHeaders.acceptHeader, 'application/octet-stream');
    request.headers.set(HttpHeaders.userAgentHeader, 'MooTool-Next-Flutter');
    final response = await request.close().timeout(const Duration(minutes: 30));
    final builder = BytesBuilder(copy: false);
    await for (final chunk in response) {
      builder.add(chunk);
    }
    return UpdateBytesResponse(
      statusCode: response.statusCode,
      body: builder.takeBytes(),
    );
  } finally {
    client.close(force: true);
  }
}

Future<bool> openLocalPath(String path) async {
  if (Platform.isMacOS) {
    final result = await Process.run('open', [path]);
    return result.exitCode == 0;
  }
  if (Platform.isWindows) {
    final result = await Process.run('cmd', ['/c', 'start', '', path]);
    return result.exitCode == 0;
  }
  final result = await Process.run('xdg-open', [path]);
  return result.exitCode == 0;
}
