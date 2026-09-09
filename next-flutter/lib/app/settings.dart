import '../app/product.dart';

enum AppLanguage { zhCN, enUS, jaJP }

enum ThemePreference { system, light, dark }

enum InterfaceStyle { modern, quiet, hero, smartisan, miuiV5, claude }

enum CloseBehavior { ask, hide, quit }

enum NavigationStyle { classic, card, grouped }

const accentColorPresets = {
  'yellow': '#e0b22b',
  'coral': '#de8f7d',
  'blue': '#4f83cc',
  'green': '#4e9275',
  'red': '#c96761',
  'purple': '#8a72b5',
};

String resolveAccentHex(String value) {
  if (accentColorPresets.containsKey(value)) return accentColorPresets[value]!;
  if (RegExp(r'^#[0-9a-fA-F]{6}$').hasMatch(value)) return value;
  return accentColorPresets['blue']!;
}

class CustomToolGroup {
  CustomToolGroup(
      {required this.id, required this.name, required this.toolIds});

  String id;
  String name;
  List<String> toolIds;

  Map<String, Object?> toJson() => {'id': id, 'name': name, 'toolIds': toolIds};

  factory CustomToolGroup.fromJson(Map<String, Object?> json) =>
      CustomToolGroup(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        toolIds: [...?json['toolIds'] as List?],
      );
}

class AppSettings {
  AppSettings({
    this.schemaVersion = Product.schemaVersion,
    this.language = AppLanguage.zhCN,
    this.theme = ThemePreference.system,
    this.interfaceStyle = InterfaceStyle.modern,
    this.accentColor = 'blue',
    this.uiFontSize = 13,
    this.editorFontSize = 14,
    this.softWrap = true,
    this.showRecent = false,
    this.compactNavigation = false,
    this.hideNavigationTitles = false,
    this.navigationStyle = NavigationStyle.classic,
    this.closeBehavior = CloseBehavior.ask,
    this.trayEnabled = true,
    this.autoCheckUpdates = false,
    this.autoDownloadUpdates = false,
    this.startMaximized = false,
    this.showSidebarDivider = true,
    this.sqlDialect = 'mysql',
    this.jsonFontName = 'monospace',
    this.noteFontName = 'monospace',
    this.noteFontSize = 14,
    this.sidebarWidth = 248,
    this.proxyEnabled = false,
    this.proxyHost = '',
    this.proxyPort = '7890',
    this.proxyUsername = '',
    this.proxyPassword = '',
    this.httpTimeoutMs = 30000,
    this.translationTimeoutMs = 15000,
    this.javaPath = '',
    this.groovyPath = '',
    this.pythonPath = '',
    this.nodePath = '',
    this.qrSize = 300,
    this.qrLevel = 'M',
    this.randomLength = 16,
    this.exportDirectory = '',
    this.defaultTranslator = 'google',
    this.defaultSourceLang = 'auto',
    this.defaultTargetLang = 'zh-CN',
    this.gitRemote = '',
    this.gitUser = '',
    this.gitToken = '',
    this.gitAutoCommit = false,
    this.gitAutoPull = false,
    this.vaultExpand = 'remember',
    List<CustomToolGroup>? customGroups,
    List<String>? hiddenNavigationToolIds,
  })  : customGroups = customGroups ?? [],
        hiddenNavigationToolIds = hiddenNavigationToolIds ?? [];

  int schemaVersion;
  AppLanguage language;
  ThemePreference theme;
  InterfaceStyle interfaceStyle;
  String accentColor;
  double uiFontSize;
  double editorFontSize;
  bool softWrap;
  bool showRecent;
  bool compactNavigation;
  bool hideNavigationTitles;
  NavigationStyle navigationStyle;
  CloseBehavior closeBehavior;
  bool trayEnabled;
  bool autoCheckUpdates;
  bool autoDownloadUpdates;
  bool startMaximized;
  bool showSidebarDivider;
  String sqlDialect;
  String jsonFontName;
  String noteFontName;
  double noteFontSize;
  double sidebarWidth;
  bool proxyEnabled;
  String proxyHost;
  String proxyPort;
  String proxyUsername;
  String proxyPassword;
  int httpTimeoutMs;
  int translationTimeoutMs;
  String javaPath;
  String groovyPath;
  String pythonPath;
  String nodePath;
  int qrSize;
  String qrLevel;
  int randomLength;
  String exportDirectory;
  String defaultTranslator;
  String defaultSourceLang;
  String defaultTargetLang;
  String gitRemote;
  String gitUser;
  String gitToken;
  bool gitAutoCommit;
  bool gitAutoPull;
  String vaultExpand;
  List<CustomToolGroup> customGroups;
  List<String> hiddenNavigationToolIds;

  AppSettings copy() => AppSettings(
        schemaVersion: schemaVersion,
        language: language,
        theme: theme,
        interfaceStyle: interfaceStyle,
        accentColor: accentColor,
        uiFontSize: uiFontSize,
        editorFontSize: editorFontSize,
        softWrap: softWrap,
        showRecent: showRecent,
        compactNavigation: compactNavigation,
        hideNavigationTitles: hideNavigationTitles,
        navigationStyle: navigationStyle,
        closeBehavior: closeBehavior,
        trayEnabled: trayEnabled,
        autoCheckUpdates: autoCheckUpdates,
        autoDownloadUpdates: autoDownloadUpdates,
        startMaximized: startMaximized,
        showSidebarDivider: showSidebarDivider,
        sqlDialect: sqlDialect,
        jsonFontName: jsonFontName,
        noteFontName: noteFontName,
        noteFontSize: noteFontSize,
        sidebarWidth: sidebarWidth,
        proxyEnabled: proxyEnabled,
        proxyHost: proxyHost,
        proxyPort: proxyPort,
        proxyUsername: proxyUsername,
        proxyPassword: proxyPassword,
        httpTimeoutMs: httpTimeoutMs,
        translationTimeoutMs: translationTimeoutMs,
        javaPath: javaPath,
        groovyPath: groovyPath,
        pythonPath: pythonPath,
        nodePath: nodePath,
        qrSize: qrSize,
        qrLevel: qrLevel,
        randomLength: randomLength,
        exportDirectory: exportDirectory,
        defaultTranslator: defaultTranslator,
        defaultSourceLang: defaultSourceLang,
        defaultTargetLang: defaultTargetLang,
        gitRemote: gitRemote,
        gitUser: gitUser,
        gitToken: gitToken,
        gitAutoCommit: gitAutoCommit,
        gitAutoPull: gitAutoPull,
        vaultExpand: vaultExpand,
        customGroups: [
          for (final group in customGroups)
            CustomToolGroup(
                id: group.id, name: group.name, toolIds: [...group.toolIds])
        ],
        hiddenNavigationToolIds: [...hiddenNavigationToolIds],
      );

  Map<String, Object?> toJson() => {
        'schemaVersion': schemaVersion,
        'productId': Product.id,
        'language': language.name,
        'theme': theme.name,
        'interfaceStyle': interfaceStyle.name,
        'accentColor': accentColor,
        'uiFontSize': uiFontSize,
        'editorFontSize': editorFontSize,
        'softWrap': softWrap,
        'showRecent': showRecent,
        'compactNavigation': compactNavigation,
        'hideNavigationTitles': hideNavigationTitles,
        'navigationStyle': navigationStyle.name,
        'closeBehavior': closeBehavior.name,
        'trayEnabled': trayEnabled,
        'autoCheckUpdates': autoCheckUpdates,
        'autoDownloadUpdates': autoDownloadUpdates,
        'startMaximized': startMaximized,
        'showSidebarDivider': showSidebarDivider,
        'sqlDialect': sqlDialect,
        'customGroups': [for (final group in customGroups) group.toJson()],
        'hiddenNavigationToolIds': hiddenNavigationToolIds,
        'jsonFontName': jsonFontName,
        'noteFontName': noteFontName,
        'noteFontSize': noteFontSize,
        'sidebarWidth': sidebarWidth,
        'proxyEnabled': proxyEnabled,
        'proxyHost': proxyHost,
        'proxyPort': proxyPort,
        'proxyUsername': proxyUsername,
        'httpTimeoutMs': httpTimeoutMs,
        'translationTimeoutMs': translationTimeoutMs,
        'javaPath': javaPath,
        'groovyPath': groovyPath,
        'pythonPath': pythonPath,
        'nodePath': nodePath,
        'qrSize': qrSize,
        'qrLevel': qrLevel,
        'randomLength': randomLength,
        'exportDirectory': exportDirectory,
        'defaultTranslator': defaultTranslator,
        'defaultSourceLang': defaultSourceLang,
        'defaultTargetLang': defaultTargetLang,
        'gitRemote': gitRemote,
        'gitUser': gitUser,
        'gitAutoCommit': gitAutoCommit,
        'gitAutoPull': gitAutoPull,
        'vaultExpand': vaultExpand,
      };

  static AppSettings fromJson(Map<String, Object?> json) {
    final settings = AppSettings();
    settings.schemaVersion =
        json['schemaVersion'] as int? ?? Product.schemaVersion;
    settings.language =
        _enum(AppLanguage.values, json['language'], AppLanguage.zhCN);
    settings.theme =
        _enum(ThemePreference.values, json['theme'], ThemePreference.system);
    settings.interfaceStyle = _enum(
        InterfaceStyle.values, json['interfaceStyle'], InterfaceStyle.modern);
    settings.accentColor =
        json['accentColor'] as String? ?? settings.accentColor;
    settings.uiFontSize = (json['uiFontSize'] as num?)?.toDouble() ?? 13;
    settings.editorFontSize =
        (json['editorFontSize'] as num?)?.toDouble() ?? 14;
    settings.softWrap = json['softWrap'] as bool? ?? true;
    settings.showRecent = json['showRecent'] as bool? ?? false;
    settings.compactNavigation = json['compactNavigation'] as bool? ?? false;
    settings.hideNavigationTitles =
        json['hideNavigationTitles'] as bool? ?? false;
    settings.navigationStyle = _enum(NavigationStyle.values,
        json['navigationStyle'], NavigationStyle.classic);
    settings.closeBehavior =
        _enum(CloseBehavior.values, json['closeBehavior'], CloseBehavior.ask);
    settings.trayEnabled = json['trayEnabled'] as bool? ?? true;
    settings.autoCheckUpdates = json['autoCheckUpdates'] as bool? ?? false;
    settings.autoDownloadUpdates =
        json['autoDownloadUpdates'] as bool? ?? false;
    settings.startMaximized = json['startMaximized'] as bool? ?? false;
    settings.showSidebarDivider = json['showSidebarDivider'] as bool? ?? true;
    settings.sqlDialect = json['sqlDialect'] as String? ?? 'mysql';
    settings.customGroups = [
      for (final item in json['customGroups'] as List? ?? const [])
        if (item is Map)
          CustomToolGroup.fromJson(Map<String, Object?>.from(item)),
    ];
    settings.hiddenNavigationToolIds = [
      ...?json['hiddenNavigationToolIds'] as List?
    ];
    settings.jsonFontName = json['jsonFontName'] as String? ?? 'monospace';
    settings.noteFontName = json['noteFontName'] as String? ?? 'monospace';
    settings.noteFontSize = (json['noteFontSize'] as num?)?.toDouble() ?? 14;
    settings.sidebarWidth = (json['sidebarWidth'] as num?)?.toDouble() ?? 248;
    settings.proxyEnabled = json['proxyEnabled'] as bool? ?? false;
    settings.proxyHost = json['proxyHost'] as String? ?? '';
    settings.proxyPort = '${json['proxyPort'] ?? '7890'}';
    settings.proxyUsername = json['proxyUsername'] as String? ?? '';
    settings.proxyPassword = json['proxyPassword'] as String? ?? '';
    settings.httpTimeoutMs = (json['httpTimeoutMs'] as num?)?.toInt() ?? 30000;
    settings.translationTimeoutMs =
        (json['translationTimeoutMs'] as num?)?.toInt() ?? 15000;
    settings.javaPath = json['javaPath'] as String? ?? '';
    settings.groovyPath = json['groovyPath'] as String? ?? '';
    settings.pythonPath = json['pythonPath'] as String? ?? '';
    settings.nodePath = json['nodePath'] as String? ?? '';
    settings.qrSize = (json['qrSize'] as num?)?.toInt() ?? 300;
    settings.qrLevel = json['qrLevel'] as String? ?? 'M';
    settings.randomLength = (json['randomLength'] as num?)?.toInt() ?? 16;
    settings.exportDirectory = json['exportDirectory'] as String? ?? '';
    settings.defaultTranslator =
        json['defaultTranslator'] as String? ?? 'google';
    settings.defaultSourceLang = json['defaultSourceLang'] as String? ?? 'auto';
    settings.defaultTargetLang =
        json['defaultTargetLang'] as String? ?? 'zh-CN';
    settings.gitRemote = json['gitRemote'] as String? ?? '';
    settings.gitUser = json['gitUser'] as String? ?? '';
    settings.gitToken = json['gitToken'] as String? ?? '';
    settings.gitAutoCommit = json['gitAutoCommit'] as bool? ?? false;
    settings.gitAutoPull = json['gitAutoPull'] as bool? ?? false;
    settings.vaultExpand = json['vaultExpand'] as String? ?? 'remember';
    return settings;
  }
}

T _enum<T extends Enum>(List<T> values, Object? name, T fallback) {
  return values
      .cast<T?>()
      .firstWhere((value) => value?.name == name, orElse: () => fallback) as T;
}
