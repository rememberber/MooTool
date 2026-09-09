import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/features/http/curl_command.dart';
import 'package:mootool_next_flutter/features/http/http_client.dart';
import 'package:mootool_next_flutter/features/http/http_models.dart';
import 'package:mootool_next_flutter/features/net/net_engine.dart';

void main() {
  test('parses curl method headers cookies and JSON body', () {
    final request = parseCurlCommand(
        "curl 'https://example.com/api?q=1' -X POST -H 'Content-Type: application/json' -H 'X-Test: yes' -b 'sid=abc; mode=dark' --data-raw '{\"ok\":true}'");
    expect(request.method, 'POST');
    expect(request.url, 'https://example.com/api?q=1');
    expect(request.bodyType, 'application/json');
    expect(request.body, '{"ok":true}');
    expect(request.headers, hasLength(2));
    expect(request.cookies.map((item) => item.name).toList(), ['sid', 'mode']);
  });

  test('round trips important curl fields', () {
    final source = parseCurlCommand(
        "curl https://example.com -H 'Accept: application/json' --data-raw 'hello world'");
    final parsed = parseCurlCommand(toCurlCommand(source));
    expect(parsed.method, 'POST');
    expect(parsed.url, 'https://example.com');
    expect(parsed.body, 'hello world');
    expect(parsed.headers.first.name, 'Accept');
    expect(parsed.headers.first.value, 'application/json');
  });

  test('rejects curl authentication instead of dropping it', () {
    expect(() => parseCurlCommand("curl https://example.com -u user:pass"),
        throwsA(isA<FormatException>()));
  });

  test('GET keeps duplicate query parameter order', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    server.listen((request) async {
      request.response.write(request.uri.query);
      await request.response.close();
    });
    final result = await HttpSender().send(
      requestId: 'dup',
      request: HttpRequestDraft(
        url: 'http://127.0.0.1:${server.port}/x',
        params: [
          KeyValueEntry(name: 'q', value: '1'),
          KeyValueEntry(name: 'q', value: '2'),
        ],
      ),
    );
    expect(result.ok, isTrue);
    expect(result.status, 200);
    expect(result.body, 'q=1&q=2');
  });

  test('POST sends body and does not report a fake 200 on failure', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    server.listen((request) async {
      final body = await utf8.decoder.bind(request).join();
      request.response.statusCode = 201;
      request.response.write('got:$body');
      await request.response.close();
    });
    final result = await HttpSender().send(
      requestId: 'post',
      request: HttpRequestDraft(
        method: 'POST',
        url: 'http://127.0.0.1:${server.port}/echo',
        body: '{"ok":true}',
        bodyType: 'application/json',
      ),
    );
    expect(result.status, 201);
    expect(result.body, 'got:{"ok":true}');
    expect(result.ok, isTrue);

    final missing = await HttpSender().send(
      requestId: 'miss',
      request: HttpRequestDraft(url: 'http://127.0.0.1:1/nope'),
      timeoutMs: 1000,
    );
    expect(missing.ok, isFalse);
    expect(missing.status, isNot(200));
    expect(missing.errorCode, isNotNull);
  });

  test('timeout is an explicit error', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    server.listen((request) async {
      await Future<void>.delayed(const Duration(seconds: 5));
      request.response.write('late');
      await request.response.close();
    });
    final timed = await HttpSender().send(
      requestId: 'slow',
      request: HttpRequestDraft(url: 'http://127.0.0.1:${server.port}/slow'),
      timeoutMs: 1000,
    );
    expect(timed.ok, isFalse);
    expect(timed.errorCode, 'TIMEOUT');
  });

  test('oversized responses are explicit errors', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    server.listen((request) async {
      final chunk = List<int>.filled(256 * 1024, 1);
      var sent = 0;
      try {
        while (sent <= maxHttpResponseBytes) {
          request.response.add(chunk);
          sent += chunk.length;
        }
        await request.response.close();
      } catch (_) {}
    });
    final large = await HttpSender().send(
      requestId: 'large',
      request: HttpRequestDraft(url: 'http://127.0.0.1:${server.port}/big'),
      timeoutMs: 30000,
    );
    expect(large.ok, isFalse);
    expect(large.errorCode, 'RESPONSE_TOO_LARGE');
  });

  test('ipv4 long conversion matches Electron fixtures', () {
    expect(ipv4ToLong('127.0.0.1'), 2130706433);
    expect(longToIpv4('2130706433'), '127.0.0.1');
    expect(() => ipv4ToLong('127.0.0.256'), throwsA(isA<FormatException>()));
    expect(() => longToIpv4('-1'), throwsA(isA<FormatException>()));
  });

  test('http collection and host profiles survive persist', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-p5-');
    addTearDown(() => root.deleteSync(recursive: true));
    final first = AppController(AppPaths(root));
    addTearDown(first.dispose);
    await first.load();
    first.http.draft.url = 'http://example.com';
    first.http.draft.method = 'GET';
    first.saveHttpDraft();
    first.createHostProfile();
    first.host.selected!.name = 'Local';
    first.host.selected!.content = '127.0.0.1 localhost\n';
    await first.persist();

    final second = AppController(AppPaths(root));
    addTearDown(second.dispose);
    await second.load();
    expect(second.http.collection, isNotEmpty);
    expect(second.http.collection.single.request.url, 'http://example.com');
    expect(second.host.profiles.single.name, 'Local');
  });
}
