const httpMethods = [
  'GET',
  'POST',
  'PUT',
  'PATCH',
  'DELETE',
  'HEAD',
  'OPTIONS',
];

class KeyValueEntry {
  KeyValueEntry(
      {String? id, this.name = '', this.value = '', this.enabled = true})
      : id = id ?? '${DateTime.now().microsecondsSinceEpoch}-$name';

  final String id;
  String name;
  String value;
  bool enabled;

  Map<String, Object?> toJson() =>
      {'id': id, 'name': name, 'value': value, 'enabled': enabled};

  factory KeyValueEntry.fromJson(Map<String, Object?> json) => KeyValueEntry(
        id: json['id'] as String?,
        name: json['name'] as String? ?? '',
        value: json['value'] as String? ?? '',
        enabled: json['enabled'] as bool? ?? true,
      );
}

class HttpCookieEntry extends KeyValueEntry {
  HttpCookieEntry(
      {super.id,
      super.name,
      super.value,
      super.enabled,
      this.domain = '',
      this.path = '/',
      this.expires = ''});

  String domain;
  String path;
  String expires;

  @override
  Map<String, Object?> toJson() => {
        ...super.toJson(),
        'domain': domain,
        'path': path,
        'expires': expires,
      };

  factory HttpCookieEntry.fromJson(Map<String, Object?> json) =>
      HttpCookieEntry(
        id: json['id'] as String?,
        name: json['name'] as String? ?? '',
        value: json['value'] as String? ?? '',
        enabled: json['enabled'] as bool? ?? true,
        domain: json['domain'] as String? ?? '',
        path: json['path'] as String? ?? '/',
        expires: json['expires'] as String? ?? '',
      );
}

class HttpRequestDraft {
  HttpRequestDraft({
    this.id,
    this.name = 'Untitled',
    this.method = 'GET',
    this.url = '',
    List<KeyValueEntry>? params,
    List<KeyValueEntry>? headers,
    List<HttpCookieEntry>? cookies,
    this.body = '',
    this.bodyType = 'application/json',
  })  : params = params ?? [],
        headers = headers ?? [],
        cookies = cookies ?? [];

  String? id;
  String name;
  String method;
  String url;
  List<KeyValueEntry> params;
  List<KeyValueEntry> headers;
  List<HttpCookieEntry> cookies;
  String body;
  String bodyType;

  Map<String, Object?> toJson() => {
        'id': id,
        'name': name,
        'method': method,
        'url': url,
        'params': [for (final item in params) item.toJson()],
        'headers': [for (final item in headers) item.toJson()],
        'cookies': [for (final item in cookies) item.toJson()],
        'body': body,
        'bodyType': bodyType,
      };

  factory HttpRequestDraft.fromJson(Map<String, Object?> json) =>
      HttpRequestDraft(
        id: json['id'] as String?,
        name: json['name'] as String? ?? 'Untitled',
        method: json['method'] as String? ?? 'GET',
        url: json['url'] as String? ?? '',
        params: [
          for (final item in json['params'] as List? ?? [])
            KeyValueEntry.fromJson(Map<String, Object?>.from(item as Map))
        ],
        headers: [
          for (final item in json['headers'] as List? ?? [])
            KeyValueEntry.fromJson(Map<String, Object?>.from(item as Map))
        ],
        cookies: [
          for (final item in json['cookies'] as List? ?? [])
            HttpCookieEntry.fromJson(Map<String, Object?>.from(item as Map))
        ],
        body: json['body'] as String? ?? '',
        bodyType: json['bodyType'] as String? ?? 'application/json',
      );

  HttpRequestDraft copy() => HttpRequestDraft.fromJson(toJson());
}

class SavedHttpRequest {
  SavedHttpRequest({
    required this.request,
    this.responseBody = '',
    this.responseHeaders = '',
    this.responseCookies = '',
    DateTime? created,
    DateTime? modified,
  })  : created = created ?? DateTime.now(),
        modified = modified ?? DateTime.now();

  HttpRequestDraft request;
  String responseBody;
  String responseHeaders;
  String responseCookies;
  DateTime created;
  DateTime modified;

  Map<String, Object?> toJson() => {
        'request': request.toJson(),
        'responseBody': responseBody,
        'responseHeaders': responseHeaders,
        'responseCookies': responseCookies,
        'created': created.toIso8601String(),
        'modified': modified.toIso8601String(),
      };

  factory SavedHttpRequest.fromJson(Map<String, Object?> json) =>
      SavedHttpRequest(
        request: HttpRequestDraft.fromJson(
            Map<String, Object?>.from(json['request'] as Map? ?? {})),
        responseBody: json['responseBody'] as String? ?? '',
        responseHeaders: json['responseHeaders'] as String? ?? '',
        responseCookies: json['responseCookies'] as String? ?? '',
        created: DateTime.tryParse(json['created'] as String? ?? '') ??
            DateTime.now(),
        modified: DateTime.tryParse(json['modified'] as String? ?? '') ??
            DateTime.now(),
      );
}

class HttpResponseResult {
  HttpResponseResult({
    required this.requestId,
    required this.ok,
    required this.status,
    required this.statusText,
    required this.url,
    required this.durationMs,
    this.body = '',
    this.headers = '',
    this.cookies = '',
    this.errorCode,
  });

  final String requestId;
  final bool ok;
  final int status;
  final String statusText;
  final String url;
  final int durationMs;
  final String body;
  final String headers;
  final String cookies;
  final String? errorCode;
}

const maxHttpResponseBytes = 10 * 1024 * 1024;
