import 'http_models.dart';

const _supportedFlags = {
  '-X',
  '--request',
  '-H',
  '--header',
  '-d',
  '--data',
  '--data-raw',
  '--data-binary',
  '--data-urlencode',
  '-b',
  '--cookie',
  '--url',
};

HttpRequestDraft emptyHttpRequest([String name = 'Untitled']) =>
    HttpRequestDraft(name: name);

const _rejectedFlags = {
  '-u',
  '--user',
  '-x',
  '--proxy',
  '--proxy-user',
  '--proxy-header',
  '--oauth2-bearer',
};

HttpRequestDraft parseCurlCommand(String command) {
  final tokens = tokenizeCurl(command.trim());
  final curlIndex =
      tokens.indexWhere((token) => token == 'curl' || token.endsWith('/curl'));
  if (curlIndex < 0) throw const FormatException('A curl command is required');
  var method = 'GET';
  var url = '';
  var body = '';
  var bodyType = 'application/json';
  final headers = <KeyValueEntry>[];
  final cookies = <HttpCookieEntry>[];
  for (var index = curlIndex + 1; index < tokens.length; index += 1) {
    final token = tokens[index];
    final next = index + 1 < tokens.length ? tokens[index + 1] : null;
    if (token.startsWith('-') && _rejectedFlags.contains(token)) {
      throw FormatException(
          'Unsupported curl option: $token. Authentication/proxy is not imported; add headers instead.');
    }
    if (token.startsWith('-') && !_supportedFlags.contains(token)) {
      if (next != null && !next.startsWith('-')) index += 1;
      continue;
    }
    if (['-X', '--request'].contains(token) && next != null) {
      method = next.toUpperCase();
      index += 1;
      continue;
    }
    if (['-H', '--header'].contains(token) && next != null) {
      final separator = next.indexOf(':');
      final name =
          separator < 0 ? next.trim() : next.substring(0, separator).trim();
      final value = separator < 0 ? '' : next.substring(separator + 1).trim();
      headers.add(KeyValueEntry(name: name, value: value));
      if (name.toLowerCase() == 'content-type') bodyType = value;
      index += 1;
      continue;
    }
    if (['-d', '--data', '--data-raw', '--data-binary', '--data-urlencode']
            .contains(token) &&
        next != null) {
      body = next;
      if (method == 'GET') method = 'POST';
      index += 1;
      continue;
    }
    if (['-b', '--cookie'].contains(token) && next != null) {
      for (final item in next.split(';')) {
        final pieces = item.trim().split('=');
        if (pieces.first.isNotEmpty) {
          cookies.add(HttpCookieEntry(
              name: pieces.first, value: pieces.skip(1).join('=')));
        }
      }
      index += 1;
      continue;
    }
    if (token == '--url' && next != null) {
      url = next;
      index += 1;
      continue;
    }
    if (!token.startsWith('-') && url.isEmpty) url = token;
  }
  if (url.isEmpty) throw const FormatException('The curl command has no URL');
  if (!httpMethods.contains(method)) {
    throw FormatException('Unsupported HTTP method: $method');
  }
  return HttpRequestDraft(
    name: 'Imported cURL',
    method: method,
    url: url,
    headers: headers,
    cookies: cookies,
    body: body,
    bodyType: bodyType,
  );
}

String toCurlCommand(HttpRequestDraft request) {
  final parts = ['curl', '-X', request.method, shellQuote(request.url)];
  for (final header in request.headers.where(_active)) {
    parts.addAll(['-H', shellQuote('${header.name}: ${header.value}')]);
  }
  final cookieValue = request.cookies
      .where(_active)
      .map((item) => '${item.name}=${item.value}')
      .join('; ');
  if (cookieValue.isNotEmpty) parts.addAll(['-b', shellQuote(cookieValue)]);
  if (request.body.isNotEmpty) {
    parts.addAll(['--data-raw', shellQuote(request.body)]);
  }
  return parts.join(' ');
}

List<String> tokenizeCurl(String value) {
  final tokens = <String>[];
  var token = '';
  String? quote;
  var escaped = false;
  for (final character in value.split('')) {
    if (escaped) {
      token += character;
      escaped = false;
      continue;
    }
    if (character == r'\' && quote != "'") {
      escaped = true;
      continue;
    }
    if (character == "'" && quote != '"') {
      quote = quote == "'" ? null : "'";
      continue;
    }
    if (character == '"' && quote != "'") {
      quote = quote == '"' ? null : '"';
      continue;
    }
    if (RegExp(r'\s').hasMatch(character) && quote == null) {
      if (token.isNotEmpty) {
        tokens.add(token);
        token = '';
      }
      continue;
    }
    token += character;
  }
  if (escaped || quote != null) {
    throw const FormatException('Unterminated curl argument');
  }
  if (token.isNotEmpty) tokens.add(token);
  return tokens;
}

String shellQuote(String value) => "'${value.replaceAll("'", "'\"'\"'")}'";

bool _active(KeyValueEntry value) =>
    value.enabled && value.name.trim().isNotEmpty;
