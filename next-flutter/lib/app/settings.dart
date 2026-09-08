import '../app/product.dart';

enum AppLanguage { zhCN, enUS, jaJP }

enum ThemePreference { system, light, dark }

enum InterfaceStyle { modern, quiet, hero, smartisan, miuiV5, claude }

enum CloseBehavior { ask, hide, quit }

enum NavigationStyle { classic, card, grouped }

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
    this.accentColor = '#4f83cc',
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
    List<CustomToolGroup>? customGroups,
    List<String>? hiddenNavigationToolIds,
    this.jsonFontName = 'monospace',
    this.sidebarWidth = 248,
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
  List<CustomToolGroup> customGroups;
  List<String> hiddenNavigationToolIds;
  String jsonFontName;
  double sidebarWidth;

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
        customGroups: [
          for (final group in customGroups)
            CustomToolGroup(
                id: group.id, name: group.name, toolIds: [...group.toolIds])
        ],
        hiddenNavigationToolIds: [...hiddenNavigationToolIds],
        jsonFontName: jsonFontName,
        sidebarWidth: sidebarWidth,
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
        'customGroups': [for (final group in customGroups) group.toJson()],
        'hiddenNavigationToolIds': hiddenNavigationToolIds,
        'jsonFontName': jsonFontName,
        'sidebarWidth': sidebarWidth,
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
    settings.customGroups = [
      for (final item in json['customGroups'] as List? ?? const [])
        if (item is Map<String, Object?>) CustomToolGroup.fromJson(item),
    ];
    settings.hiddenNavigationToolIds = [
      ...?json['hiddenNavigationToolIds'] as List?
    ];
    settings.jsonFontName = json['jsonFontName'] as String? ?? 'monospace';
    settings.sidebarWidth = (json['sidebarWidth'] as num?)?.toDouble() ?? 248;
    return settings;
  }
}

T _enum<T extends Enum>(List<T> values, Object? name, T fallback) {
  return values
      .cast<T?>()
      .firstWhere((value) => value?.name == name, orElse: () => fallback) as T;
}
