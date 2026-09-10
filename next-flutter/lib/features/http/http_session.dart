import 'http_models.dart';

class HttpSession {
  HttpSession() : draft = emptyDraft();

  HttpRequestDraft draft;
  HttpResponseResult? response;
  final List<SavedHttpRequest> collection = [];
  String search = '';
  String requestTab = 'params';
  String responseTab = 'body';
  bool sending = false;
  bool historyOpen = false;
  bool curlOpen = false;
  String curlText = '';
  String notice = '';
  String activeRequestId = '';
  int timeoutMs = 30000;
  double collectionWidth = 240;

  static HttpRequestDraft emptyDraft() => HttpRequestDraft(name: 'Untitled');

  Map<String, Object?> toJson() => {
        'draft': draft.toJson(),
        'collection': [for (final item in collection) item.toJson()],
        'search': search,
        'requestTab': requestTab,
        'responseTab': responseTab,
        'timeoutMs': timeoutMs,
        'collectionWidth': collectionWidth,
        'notice': notice,
      };

  void restore(Map<String, Object?> json) {
    draft = HttpRequestDraft.fromJson(
        Map<String, Object?>.from(json['draft'] as Map? ?? {}));
    collection
      ..clear()
      ..addAll([
        for (final item in json['collection'] as List? ?? [])
          SavedHttpRequest.fromJson(Map<String, Object?>.from(item as Map))
      ]);
    search = json['search'] as String? ?? '';
    requestTab = json['requestTab'] as String? ?? 'params';
    responseTab = json['responseTab'] as String? ?? 'body';
    timeoutMs = json['timeoutMs'] as int? ?? 30000;
    collectionWidth = (json['collectionWidth'] as num?)?.toDouble() ?? 240;
    notice = json['notice'] as String? ?? '';
  }
}

HttpRequestDraft emptyDraft() => HttpSession.emptyDraft();
