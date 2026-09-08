import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'http_models.dart';

class HttpProxyConfig {
  HttpProxyConfig(
      {this.enabled = false,
      this.host = '',
      this.port = '',
      this.username = '',
      this.password = ''});
  final bool enabled;
  final String host;
  final String port;
  final String username;
  final String password;
}

class HttpSender {
  final Map<String, HttpClient> _clients = {};
  final Set<String> _cancelled = {};

  Future<HttpResponseResult> send({
    required String requestId,
    required HttpRequestDraft request,
    int timeoutMs = 30000,
    HttpProxyConfig? proxy,
  }) async {
    final started = DateTime.now();
    if (request.url.trim().isEmpty) {
      return HttpResponseResult(
        requestId: requestId,
        ok: false,
        status: 0,
        statusText: 'URL is required',
        url: request.url,
        durationMs: 0,
        errorCode: 'INVALID_REQUEST',
      );
    }
    cancel(requestId);
    _cancelled.remove(requestId);
    final client = HttpClient();
    _clients[requestId] = client;
    client.autoUncompress = true;
    client.connectionTimeout =
        Duration(milliseconds: timeoutMs.clamp(1000, 120000));
    final directive = httpFindProxy(proxy);
    if (directive != null) {
      client.findProxy = (_) => directive;
      final proxyPort = int.parse(proxy!.port);
      if (proxy.username.isNotEmpty) {
        client.addProxyCredentials(
            proxy.host.trim(),
            proxyPort,
            '',
            HttpClientBasicCredentials(proxy.username, proxy.password));
      }
    }
    try {
      final uri = Uri.parse(buildRequestUrl(
          request.url, request.method, request.params));
      final httpRequest =
          await client.openUrl(request.method, uri).timeout(client.connectionTimeout!);
      httpRequest.followRedirects = true;
      httpRequest.maxRedirects = 20;
      _applyHeaders(httpRequest, request);
      final body = buildRequestBody(
          request.method, request.body, request.bodyType, request.params);
      if (body != null) {
        if (!_hasHeader(httpRequest.headers, 'content-type')) {
          httpRequest.headers.contentType = ContentType.parse(
              request.body.isNotEmpty
                  ? (request.bodyType.isEmpty
                      ? 'text/plain'
                      : request.bodyType)
                  : 'application/x-www-form-urlencoded');
        }
        final bytes = utf8.encode(body);
        httpRequest.contentLength = bytes.length;
        httpRequest.add(bytes);
      }
      final response = await httpRequest
          .close()
          .timeout(Duration(milliseconds: timeoutMs.clamp(1000, 120000)));
      final bytes = await _readLimited(response);
      final contentType = response.headers.contentType;
      final text = _decodeBody(bytes, contentType);
      return HttpResponseResult(
        requestId: requestId,
        ok: response.statusCode >= 200 && response.statusCode < 400,
        status: response.statusCode,
        statusText: response.reasonPhrase,
        url: response.redirects.isEmpty
            ? uri.toString()
            : response.redirects.last.location.toString(),
        durationMs: DateTime.now().difference(started).inMilliseconds,
        body: text,
        headers: _formatHeaders(response.headers),
        cookies: response.cookies
            .map((cookie) => cookie.toString())
            .join('\n'),
      );
    } on TimeoutException {
      return HttpResponseResult(
        requestId: requestId,
        ok: false,
        status: 0,
        statusText: 'TIMEOUT',
        url: request.url,
        durationMs: DateTime.now().difference(started).inMilliseconds,
        errorCode: 'TIMEOUT',
      );
    } on HttpException catch (error) {
      return HttpResponseResult(
        requestId: requestId,
        ok: false,
        status: 0,
        statusText: error.message,
        url: request.url,
        durationMs: DateTime.now().difference(started).inMilliseconds,
        errorCode: _cancelled.contains(requestId) ? 'ABORTED' : 'NETWORK',
      );
    } on SocketException catch (error) {
      return HttpResponseResult(
        requestId: requestId,
        ok: false,
        status: 0,
        statusText: error.message,
        url: request.url,
        durationMs: DateTime.now().difference(started).inMilliseconds,
        errorCode: _cancelled.contains(requestId) ? 'ABORTED' : 'NETWORK',
      );
    } on FormatException catch (error) {
      return HttpResponseResult(
        requestId: requestId,
        ok: false,
        status: 0,
        statusText: error.message,
        url: request.url,
        durationMs: DateTime.now().difference(started).inMilliseconds,
        errorCode: error.message == 'RESPONSE_TOO_LARGE'
            ? 'RESPONSE_TOO_LARGE'
            : 'INVALID_REQUEST',
      );
    } finally {
      client.close(force: true);
      _clients.remove(requestId);
    }
  }

  bool cancel(String requestId) {
    _cancelled.add(requestId);
    final client = _clients.remove(requestId);
    if (client == null) return false;
    client.close(force: true);
    return true;
  }

  void _applyHeaders(HttpClientRequest httpRequest, HttpRequestDraft request) {
    for (final header in request.headers.where(
        (item) => item.enabled && item.name.trim().isNotEmpty)) {
      httpRequest.headers.add(header.name, header.value);
    }
    final cookie = request.cookies
        .where((item) => item.enabled && item.name.trim().isNotEmpty)
        .map((item) => '${item.name}=${item.value}')
        .join('; ');
    if (cookie.isNotEmpty) httpRequest.headers.set('Cookie', cookie);
  }

  bool _hasHeader(HttpHeaders headers, String name) {
    var found = false;
    headers.forEach((headerName, values) {
      if (headerName.toLowerCase() == name) found = true;
    });
    return found;
  }
}

String? httpFindProxy(HttpProxyConfig? proxy) {
  final proxyPort = int.tryParse(proxy?.port ?? '');
  if (proxy == null ||
      !proxy.enabled ||
      proxy.host.trim().isEmpty ||
      proxyPort == null ||
      proxyPort <= 0) {
    return null;
  }
  return 'PROXY ${proxy.host.trim()}:$proxyPort';
}

String buildRequestUrl(
    String value, String method, List<KeyValueEntry> params) {
  final trimmed = value.trim();
  final normalized = RegExp(r'^https?://', caseSensitive: false).hasMatch(trimmed)
      ? trimmed
      : 'http://$trimmed';
  if (method != 'GET' && method != 'HEAD' && method != 'OPTIONS') {
    return normalized;
  }
  final extra = [
    for (final entry in params.where(
        (item) => item.enabled && item.name.trim().isNotEmpty))
      '${Uri.encodeQueryComponent(entry.name)}=${Uri.encodeQueryComponent(entry.value)}'
  ];
  if (extra.isEmpty) return normalized;
  final separator = normalized.contains('?') ? '&' : '?';
  return '$normalized$separator${extra.join('&')}';
}

String? buildRequestBody(String method, String body, String bodyType,
    List<KeyValueEntry> params) {
  if (method == 'GET' || method == 'HEAD' || method == 'OPTIONS') return null;
  if (body.isNotEmpty) return body;
  final form = [
    for (final entry in params.where(
        (item) => item.enabled && item.name.trim().isNotEmpty))
      '${Uri.encodeQueryComponent(entry.name)}=${Uri.encodeQueryComponent(entry.value)}'
  ];
  if (form.isEmpty) return null;
  return form.join('&');
}

Future<Uint8List> _readLimited(HttpClientResponse response) async {
  final declared = response.contentLength;
  if (declared > maxHttpResponseBytes) {
    throw const FormatException('RESPONSE_TOO_LARGE');
  }
  final builder = BytesBuilder(copy: false);
  await for (final chunk in response) {
    builder.add(chunk);
    if (builder.length > maxHttpResponseBytes) {
      throw const FormatException('RESPONSE_TOO_LARGE');
    }
  }
  return builder.takeBytes();
}

String _decodeBody(Uint8List bytes, ContentType? type) {
  final mime = type?.mimeType ?? '';
  final binary = mime.startsWith('image/') ||
      mime == 'application/octet-stream' ||
      bytes.contains(0);
  if (binary) {
    final preview = bytes.take(64).map((b) => b.toRadixString(16).padLeft(2, '0')).join(' ');
    return '[binary ${bytes.length} bytes]\n$preview';
  }
  final charset = type?.charset ?? 'utf-8';
  final text = charset.toLowerCase() == 'utf-8' || charset.toLowerCase() == 'utf8'
      ? utf8.decode(bytes, allowMalformed: true)
      : latin1.decode(bytes);
  return _prettyJson(text);
}

String _prettyJson(String value) {
  try {
    return const JsonEncoder.withIndent('  ').convert(jsonDecode(value));
  } catch (_) {
    return value;
  }
}

String _formatHeaders(HttpHeaders headers) {
  final lines = <String>[];
  headers.forEach((name, values) {
    for (final value in values) {
      lines.add('$name: $value');
    }
  });
  return lines.join('\n');
}
