import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app/product.dart';
import '../../design/theme.dart';
import '../../l10n/strings.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key, required this.l10n});

  final L10n l10n;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    return Material(
      color: tokens.workspace,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: ListView(
            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 28),
            children: [
              Row(
                children: [
                  InkWell(
                    onTap: () => _open('https://mootool.luoboduner.com'),
                    child: Image.asset('assets/brand/mootool-logo.png',
                        width: 104,
                        height: 104,
                        filterQuality: FilterQuality.medium),
                  ),
                  const SizedBox(width: 20),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        TextButton(
                          onPressed: () =>
                              _open('https://mootool.luoboduner.com'),
                          child: Text(
                              'mootool.luoboduner.com · ${l10n.t('app.home.website')}'),
                        ),
                        Row(children: [
                          Text(Product.shortName,
                              style: const TextStyle(
                                  fontSize: 28, fontWeight: FontWeight.w600)),
                          const SizedBox(width: 10),
                          Text('v${Product.version}',
                              style: TextStyle(color: tokens.textSecondary)),
                        ]),
                        Text(l10n.t('app.home.tagline')),
                        Text(l10n.t('app.home.author'),
                            style: TextStyle(
                                color: tokens.textSecondary, fontSize: 12)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 28),
              _section(l10n.t('app.home.about.title'), [
                l10n.t('app.home.about.line1'),
                l10n.t('app.home.about.lineDaily'),
                l10n.t('app.home.about.line2'),
                l10n.t('app.home.about.line2Note'),
                l10n.t('app.home.about.line3'),
                l10n.t('app.home.about.line4'),
                l10n.t('app.home.about.line5'),
              ]),
              _chips(l10n.t('app.home.contributors.title'), const [
                'CassianFlorin',
                'felixcn',
                'felixnan168',
                'Lyp',
                'sunsence',
                'rememberber'
              ]),
              Text(l10n.t('app.home.contributors.thanks'),
                  style: TextStyle(color: tokens.textSecondary)),
              const SizedBox(height: 20),
              Text(l10n.t('app.home.sponsor.title'),
                  style: const TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              Text(l10n.t('app.home.sponsor.prompt')),
              const SizedBox(height: 8),
              Image.asset('assets/brand/wx-zanshang.jpg',
                  width: 180, filterQuality: FilterQuality.medium),
              Text(l10n.t('app.home.sponsor.tip'),
                  style: TextStyle(color: tokens.textSecondary, fontSize: 12)),
              const SizedBox(height: 20),
              Text(l10n.t('app.home.source.title'),
                  style: const TextStyle(fontWeight: FontWeight.w600)),
              Wrap(spacing: 8, children: [
                TextButton(
                    onPressed: () =>
                        _open('https://github.com/rememberber/MooTool'),
                    child: const Text('GitHub')),
                TextButton(
                    onPressed: () =>
                        _open('https://gitee.com/zhoubochina/MooTool'),
                    child: const Text('Gitee')),
              ]),
              TextButton(
                onPressed: () =>
                    _open('https://github.com/rememberber/MooTool/issues'),
                child: Text(l10n.t('app.home.help.issue')),
              ),
              const SizedBox(height: 12),
              Text(l10n.t('app.home.otherWorks.title'),
                  style: const TextStyle(fontWeight: FontWeight.w600)),
              ListTile(
                dense: true,
                title: const Text('WePush'),
                subtitle: Text(l10n.t('app.home.wePush.desc')),
                onTap: () => _open('https://github.com/rememberber/WePush'),
              ),
              ListTile(
                dense: true,
                title: const Text('MooInfo'),
                subtitle: Text(l10n.t('app.home.mooInfo.desc')),
                onTap: () => _open('https://github.com/rememberber/MooInfo'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _section(String title, List<String> lines) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        for (final line in lines)
          Padding(padding: const EdgeInsets.only(bottom: 6), child: Text(line)),
      ]),
    );
  }

  Widget _chips(String title, List<String> names) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        Wrap(spacing: 8, runSpacing: 8, children: [
          for (final name in names)
            ActionChip(
                label: Text(name),
                onPressed: () => _open('https://github.com/$name')),
        ]),
      ]),
    );
  }

  Future<void> _open(String url) async {
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri))
      await launchUrl(uri, mode: LaunchMode.externalApplication);
  }
}
