import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'curl_command.dart';
import 'http_models.dart';

class HttpToolPage extends StatelessWidget {
  const HttpToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.http;
    final tokens = tokensOf(context);
    final compact = MediaQuery.sizeOf(context).width < 1080;
    return Column(
      children: [
        _toolbar(tokens),
        Expanded(
          child: Row(
            children: [
              if (!compact)
                SizedBox(
                    width: session.collectionWidth.clamp(180.0, 320.0),
                    child: _collection(tokens)),
              Expanded(
                child: Column(
                  children: [
                    _urlBar(tokens),
                    Expanded(child: _requestPane(tokens)),
                    Expanded(child: _responsePane(tokens)),
                  ],
                ),
              ),
            ],
          ),
        ),
        _status(tokens),
      ],
    );
  }

  Widget _toolbar(MooTokens tokens) {
    final session = controller.http;
    return ColoredBox(
      color: tokens.toolbar,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Wrap(
          spacing: 6,
          runSpacing: 6,
          children: [
            CompactButton(
                label: controller.t('http.save'),
                onPressed: controller.saveHttpDraft),
            CompactButton(
                label: controller.t('http.importCurl'),
                onPressed: () {
                  session.curlOpen = !session.curlOpen;
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('http.exportCurl'),
                onPressed: () async {
                  await Clipboard.setData(
                      ClipboardData(text: toCurlCommand(session.draft)));
                  controller.toast = controller.t('json.action.copied');
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('http.history'),
                onPressed: () {
                  session.historyOpen = !session.historyOpen;
                  controller.refresh();
                }),
          ],
        ),
      ),
    );
  }

  Widget _urlBar(MooTokens tokens) {
    final session = controller.http;
    return ColoredBox(
      color: tokens.toolbar,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
        child: Row(
          children: [
            DropdownButton<String>(
              value: httpMethods.contains(session.draft.method)
                  ? session.draft.method
                  : 'GET',
              items: [
                for (final method in httpMethods)
                  DropdownMenuItem(value: method, child: Text(method))
              ],
              onChanged: (value) {
                if (value == null) return;
                session.draft.method = value;
                controller.refresh();
              },
            ),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: TextEditingController(text: session.draft.url)
                  ..selection = TextSelection.collapsed(
                      offset: session.draft.url.length),
                decoration: InputDecoration(
                    isDense: true, hintText: controller.t('http.url')),
                onChanged: (value) => session.draft.url = value,
              ),
            ),
            const SizedBox(width: 8),
            SizedBox(
              width: 88,
              child: TextField(
                decoration: InputDecoration(
                    isDense: true, labelText: controller.t('http.timeout')),
                controller:
                    TextEditingController(text: '${session.timeoutMs}'),
                onSubmitted: (value) {
                  session.timeoutMs =
                      (int.tryParse(value) ?? 30000).clamp(1000, 120000);
                  controller.refresh();
                },
              ),
            ),
            CompactButton(
                label: session.sending
                    ? controller.t('http.cancel')
                    : controller.t('http.send'),
                primary: !session.sending,
                onPressed: session.sending
                    ? controller.cancelHttp
                    : () => controller.sendHttp()),
          ],
        ),
      ),
    );
  }

  Widget _collection(MooTokens tokens) {
    final session = controller.http;
    final items = [
      for (final item in session.collection)
        if (session.search.isEmpty ||
            item.request.name.toLowerCase().contains(session.search.toLowerCase()) ||
            item.request.url.toLowerCase().contains(session.search.toLowerCase()))
          item
    ];
    return ColoredBox(
      color: tokens.sidebar,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(8),
            child: TextField(
              decoration: InputDecoration(
                  isDense: true, hintText: controller.t('http.search')),
              onChanged: (value) {
                session.search = value;
                controller.refresh();
              },
            ),
          ),
          CompactButton(
              label: controller.t('http.new'),
              onPressed: controller.newHttpDraft),
          Expanded(
            child: items.isEmpty
                ? Center(
                    child: Text(controller.t('http.empty'),
                        style: TextStyle(
                            color: tokens.textSecondary, fontSize: 12)))
                : ListView(
                    children: [
                      for (final item in items)
                        ListTile(
                          dense: true,
                          title: Text(item.request.name,
                              overflow: TextOverflow.ellipsis),
                          subtitle: Text(
                              '${item.request.method} ${item.request.url}',
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(fontSize: 11)),
                          onTap: () => controller.openHttpSaved(item),
                          onLongPress: () =>
                              controller.deleteHttpSaved(item.request.id),
                        ),
                    ],
                  ),
          ),
        ],
      ),
    );
  }

  Widget _requestPane(MooTokens tokens) {
    final session = controller.http;
    return ColoredBox(
      color: tokens.surface,
      child: Column(
        children: [
          Wrap(spacing: 4, children: [
            for (final tab in ['params', 'headers', 'cookies', 'body'])
              CompactButton(
                label: controller.t('http.tab.$tab'),
                primary: session.requestTab == tab,
                onPressed: () {
                  session.requestTab = tab;
                  controller.refresh();
                },
              ),
          ]),
          if (session.curlOpen)
            Padding(
              padding: const EdgeInsets.all(8),
              child: TextField(
                maxLines: 4,
                decoration: InputDecoration(
                    hintText: controller.t('http.curlPrompt')),
                onChanged: (value) => session.curlText = value,
                onSubmitted: (value) {
                  try {
                    session.draft = parseCurlCommand(value);
                    session.curlOpen = false;
                    session.notice = '';
                  } catch (error) {
                    session.notice = error.toString();
                  }
                  controller.refresh();
                },
              ),
            ),
          Expanded(child: _requestEditor(tokens)),
        ],
      ),
    );
  }

  Widget _requestEditor(MooTokens tokens) {
    final session = controller.http;
    if (session.requestTab == 'body') {
      return TextField(
        maxLines: null,
        expands: true,
        controller: TextEditingController(text: session.draft.body)
          ..selection =
              TextSelection.collapsed(offset: session.draft.body.length),
        style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
        decoration: const InputDecoration(
            border: InputBorder.none, contentPadding: EdgeInsets.all(8)),
        onChanged: (value) => session.draft.body = value,
      );
    }
    final rows = switch (session.requestTab) {
      'headers' => session.draft.headers,
      'cookies' => session.draft.cookies,
      _ => session.draft.params,
    };
    return Column(
      children: [
        CompactButton(
            label: controller.t('http.addRow'),
            onPressed: () {
              if (session.requestTab == 'cookies') {
                session.draft.cookies.add(HttpCookieEntry());
              } else if (session.requestTab == 'headers') {
                session.draft.headers.add(KeyValueEntry());
              } else {
                session.draft.params.add(KeyValueEntry());
              }
              controller.refresh();
            }),
        Expanded(
          child: ListView.builder(
            itemCount: rows.length,
            itemBuilder: (context, index) {
              final row = rows[index];
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                child: Row(
                  children: [
                    Checkbox(
                        value: row.enabled,
                        onChanged: (value) {
                          row.enabled = value ?? true;
                          controller.refresh();
                        }),
                    Expanded(
                        child: TextField(
                      controller: TextEditingController(text: row.name),
                      decoration: const InputDecoration(
                          isDense: true, hintText: 'name'),
                      onChanged: (value) => row.name = value,
                    )),
                    const SizedBox(width: 6),
                    Expanded(
                        child: TextField(
                      controller: TextEditingController(text: row.value),
                      decoration: const InputDecoration(
                          isDense: true, hintText: 'value'),
                      onChanged: (value) => row.value = value,
                    )),
                    IconButton(
                        icon: const Icon(Icons.close, size: 16),
                        onPressed: () {
                          rows.removeAt(index);
                          controller.refresh();
                        }),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _responsePane(MooTokens tokens) {
    final session = controller.http;
    final result = session.response;
    final text = switch (session.responseTab) {
      'headers' => result?.headers ?? '',
      'cookies' => result?.cookies ?? '',
      _ => result?.body ?? '',
    };
    return ColoredBox(
      color: tokens.workspace,
      child: Column(
        children: [
          Wrap(spacing: 4, children: [
            for (final tab in ['body', 'headers', 'cookies'])
              CompactButton(
                label: controller.t('http.tab.$tab'),
                primary: session.responseTab == tab,
                onPressed: () {
                  session.responseTab = tab;
                  controller.refresh();
                },
              ),
            if (result != null)
              Text('${result.status} ${result.statusText}  ${result.durationMs}ms',
                  style: const TextStyle(fontSize: 12)),
          ]),
          Expanded(
            child: SelectableText(text,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 13)),
          ),
        ],
      ),
    );
  }

  Widget _status(MooTokens tokens) {
    final session = controller.http;
    return ColoredBox(
      color: tokens.toolbar,
      child: SizedBox(
        height: 26,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Text(
              session.notice.isEmpty
                  ? (session.response?.url ?? '')
                  : session.notice,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 11)),
        ),
      ),
    );
  }
}
