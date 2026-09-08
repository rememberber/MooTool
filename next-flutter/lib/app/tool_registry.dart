enum ToolGroupId { home, text, dev, network, encode, daily, system }

enum ToolStatus { notStarted, inProgress, review, complete }

class ToolDefinition {
  const ToolDefinition({
    required this.id,
    required this.groupId,
    required this.titleKey,
    required this.icon,
    required this.keywords,
    this.supportsHistory = true,
    this.supportsFavorites = false,
    this.status = ToolStatus.notStarted,
  });

  final String id;
  final ToolGroupId groupId;
  final String titleKey;
  final String icon;
  final List<String> keywords;
  final bool supportsHistory;
  final bool supportsFavorites;
  final ToolStatus status;

  bool matches(String query) {
    if (query.trim().isEmpty) return true;
    final haystack = ([id, titleKey, ...keywords].join(' ')).toLowerCase();
    return query.toLowerCase().split(RegExp(r'\s+')).every(haystack.contains);
  }
}

class ToolGroupDefinition {
  const ToolGroupDefinition(
      {required this.id, required this.titleKey, required this.toolIds});

  final ToolGroupId id;
  final String titleKey;
  final List<String> toolIds;
}

const toolRegistry = <ToolDefinition>[
  ToolDefinition(
      id: 'mootool',
      groupId: ToolGroupId.home,
      titleKey: 'app.nav.home',
      icon: 'home',
      keywords: ['home', 'about', '首页', '主页', '关于', 'ホーム'],
      supportsHistory: false,
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'quickNote',
      groupId: ToolGroupId.text,
      titleKey: 'app.nav.quickNote',
      icon: 'note',
      keywords: ['note', 'memo', 'markdown', '随手记', '笔记', '記'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'textDiff',
      groupId: ToolGroupId.text,
      titleKey: 'app.nav.textDiff',
      icon: 'diff',
      keywords: ['diff', 'compare', 'text', '对比', '比較'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'reformat',
      groupId: ToolGroupId.text,
      titleKey: 'app.nav.reformat',
      icon: 'format',
      keywords: ['format', 'java', 'xml', 'html', 'nginx', '格式化'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'json',
      groupId: ToolGroupId.dev,
      titleKey: 'app.nav.json',
      icon: 'json',
      keywords: ['json', 'xml', 'javabean', 'jsonpath', '格式化'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'java',
      groupId: ToolGroupId.dev,
      titleKey: 'app.nav.java',
      icon: 'runtime',
      keywords: ['java', 'groovy', 'python', 'node', 'console', '运行', 'コード']),
  ToolDefinition(
      id: 'ymlProperties',
      groupId: ToolGroupId.dev,
      titleKey: 'app.nav.ymlProperties',
      icon: 'config',
      keywords: ['yaml', 'yml', 'properties', 'config', '配置', '変換'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'protobuf',
      groupId: ToolGroupId.dev,
      titleKey: 'app.nav.protobuf',
      icon: 'proto',
      keywords: ['protobuf', 'proto', 'wire', '序列化'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'variables',
      groupId: ToolGroupId.dev,
      titleKey: 'app.nav.variables',
      icon: 'env',
      keywords: ['env', 'environment', 'variable', '环境变量', '環境']),
  ToolDefinition(
      id: 'http',
      groupId: ToolGroupId.network,
      titleKey: 'app.nav.http',
      icon: 'http',
      keywords: ['http', 'curl', 'api', 'request', '请求'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'host',
      groupId: ToolGroupId.network,
      titleKey: 'app.nav.host',
      icon: 'host',
      keywords: ['host', 'dns', '域名'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'net',
      groupId: ToolGroupId.network,
      titleKey: 'app.nav.net',
      icon: 'net',
      keywords: ['network', 'ip', 'ping', 'whois', '网络', 'ネットワーク'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'uaParse',
      groupId: ToolGroupId.network,
      titleKey: 'app.nav.uaParse',
      icon: 'ua',
      keywords: ['ua', 'user-agent', 'browser', '浏览器', '分析'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'encode',
      groupId: ToolGroupId.encode,
      titleKey: 'app.nav.encode',
      icon: 'encode',
      keywords: ['encode', 'decode', 'base64', 'url', '编码', '解码'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'crypto',
      groupId: ToolGroupId.encode,
      titleKey: 'app.nav.crypto',
      icon: 'crypto',
      keywords: ['crypto', 'hash', 'md5', 'sha', 'random', '加密', '随机'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'regex',
      groupId: ToolGroupId.encode,
      titleKey: 'app.nav.regex',
      icon: 'regex',
      keywords: ['regex', 'regexp', 'regular', '正则', '正規'],
      supportsFavorites: true,
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'cron',
      groupId: ToolGroupId.encode,
      titleKey: 'app.nav.cron',
      icon: 'cron',
      keywords: ['cron', 'schedule', '定时', '表达式'],
      supportsFavorites: true,
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'qrCode',
      groupId: ToolGroupId.encode,
      titleKey: 'app.nav.qrCode',
      icon: 'qr',
      keywords: ['qr', 'qrcode', '二维码', 'バーコード'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'timeConvert',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.timeConvert',
      icon: 'time',
      keywords: ['time', 'timestamp', 'clock', '时间', '时区', '時間'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'messageBoard',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.messageBoard',
      icon: 'board',
      keywords: ['message', 'sign', 'notice', 'board', '留言', '告示'],
      supportsHistory: false),
  ToolDefinition(
      id: 'translation',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.translation',
      icon: 'translate',
      keywords: ['translate', 'translation', 'word', '翻译', '翻訳']),
  ToolDefinition(
      id: 'calculator',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.calculator',
      icon: 'calc',
      keywords: ['calculator', 'calc', 'math', '计算', '計算'],
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'colorBoard',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.colorBoard',
      icon: 'color',
      keywords: ['color', 'palette', 'hex', 'rgb', '调色', '色'],
      supportsFavorites: true,
      status: ToolStatus.inProgress),
  ToolDefinition(
      id: 'image',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.image',
      icon: 'image',
      keywords: ['image', 'watermark', 'compress', 'svg', '图片', '画像']),
  ToolDefinition(
      id: 'pdf',
      groupId: ToolGroupId.daily,
      titleKey: 'app.nav.pdf',
      icon: 'pdf',
      keywords: ['pdf', 'merge', 'split', '合并', '拆分']),
  ToolDefinition(
      id: 'hardware',
      groupId: ToolGroupId.system,
      titleKey: 'app.nav.hardware',
      icon: 'cpu',
      keywords: ['hardware', 'system', 'cpu', 'memory', '系统', 'ハードウェア'],
      supportsHistory: false),
];

final toolGroups = [
  ToolGroupDefinition(
      id: ToolGroupId.text,
      titleKey: 'app.group.text',
      toolIds: _ids(ToolGroupId.text)),
  ToolGroupDefinition(
      id: ToolGroupId.dev,
      titleKey: 'app.group.dev',
      toolIds: _ids(ToolGroupId.dev)),
  ToolGroupDefinition(
      id: ToolGroupId.network,
      titleKey: 'app.group.network',
      toolIds: _ids(ToolGroupId.network)),
  ToolGroupDefinition(
      id: ToolGroupId.encode,
      titleKey: 'app.group.encode',
      toolIds: _ids(ToolGroupId.encode)),
  ToolGroupDefinition(
      id: ToolGroupId.daily,
      titleKey: 'app.group.daily',
      toolIds: _ids(ToolGroupId.daily)),
  ToolGroupDefinition(
      id: ToolGroupId.system,
      titleKey: 'app.group.system',
      toolIds: _ids(ToolGroupId.system)),
];

final toolById = {for (final tool in toolRegistry) tool.id: tool};

List<String> _ids(ToolGroupId group) => [
      for (final tool in toolRegistry)
        if (tool.groupId == group) tool.id
    ];

bool isToolId(String value) => toolById.containsKey(value);

List<ToolDefinition> searchTools(String query,
    {Iterable<String> hiddenIds = const []}) {
  final hidden = hiddenIds.toSet();
  return [
    for (final tool in toolRegistry)
      if (tool.matches(query) &&
          (query.trim().isNotEmpty || !hidden.contains(tool.id)))
        tool,
  ];
}
