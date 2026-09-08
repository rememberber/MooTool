import 'package:flutter/material.dart';

import '../app/settings.dart';

@immutable
class MooTokens extends ThemeExtension<MooTokens> {
  const MooTokens({
    required this.workspace,
    required this.surface,
    required this.sidebar,
    required this.toolbar,
    required this.border,
    required this.textPrimary,
    required this.textSecondary,
    required this.accent,
    required this.onAccent,
    required this.focus,
  });

  final Color workspace;
  final Color surface;
  final Color sidebar;
  final Color toolbar;
  final Color border;
  final Color textPrimary;
  final Color textSecondary;
  final Color accent;
  final Color onAccent;
  final Color focus;

  static const light = MooTokens(
    workspace: Color(0xFFFAFBFC),
    surface: Color(0xFFFFFFFF),
    sidebar: Color(0xFFF1F3F6),
    toolbar: Color(0xFFF6F7F9),
    border: Color(0xFFE1E5EB),
    textPrimary: Color(0xFF20242C),
    textSecondary: Color(0xFF586174),
    accent: Color(0xFF356CB8),
    onAccent: Color(0xFFFFFFFF),
    focus: Color(0xFF2468C8),
  );

  static const dark = MooTokens(
    workspace: Color(0xFF17191D),
    surface: Color(0xFF1F2228),
    sidebar: Color(0xFF1B1E23),
    toolbar: Color(0xFF24272E),
    border: Color(0xFF363B45),
    textPrimary: Color(0xFFE8EBF1),
    textSecondary: Color(0xFFADB6C6),
    accent: Color(0xFF8AB8F8),
    onAccent: Color(0xFF101B2B),
    focus: Color(0xFF8AB8F8),
  );

  @override
  MooTokens copyWith({
    Color? workspace,
    Color? surface,
    Color? sidebar,
    Color? toolbar,
    Color? border,
    Color? textPrimary,
    Color? textSecondary,
    Color? accent,
    Color? onAccent,
    Color? focus,
  }) {
    return MooTokens(
      workspace: workspace ?? this.workspace,
      surface: surface ?? this.surface,
      sidebar: sidebar ?? this.sidebar,
      toolbar: toolbar ?? this.toolbar,
      border: border ?? this.border,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      accent: accent ?? this.accent,
      onAccent: onAccent ?? this.onAccent,
      focus: focus ?? this.focus,
    );
  }

  @override
  MooTokens lerp(ThemeExtension<MooTokens>? other, double t) {
    if (other is! MooTokens) return this;
    return MooTokens(
      workspace: Color.lerp(workspace, other.workspace, t)!,
      surface: Color.lerp(surface, other.surface, t)!,
      sidebar: Color.lerp(sidebar, other.sidebar, t)!,
      toolbar: Color.lerp(toolbar, other.toolbar, t)!,
      border: Color.lerp(border, other.border, t)!,
      textPrimary: Color.lerp(textPrimary, other.textPrimary, t)!,
      textSecondary: Color.lerp(textSecondary, other.textSecondary, t)!,
      accent: Color.lerp(accent, other.accent, t)!,
      onAccent: Color.lerp(onAccent, other.onAccent, t)!,
      focus: Color.lerp(focus, other.focus, t)!,
    );
  }
}

MooTokens tokensFor(InterfaceStyle style, Brightness brightness) {
  final dark = brightness == Brightness.dark;
  return switch (style) {
    InterfaceStyle.modern => dark ? MooTokens.dark : MooTokens.light,
    InterfaceStyle.quiet => dark
        ? const MooTokens(
            workspace: Color(0xFF141414),
            surface: Color(0xFF1C1C1C),
            sidebar: Color(0xFF181818),
            toolbar: Color(0xFF202020),
            border: Color(0xFF2E2E2E),
            textPrimary: Color(0xFFE6E6E6),
            textSecondary: Color(0xFF9A9A9A),
            accent: Color(0xFF8F8F8F),
            onAccent: Color(0xFF111111),
            focus: Color(0xFFB0B0B0),
          )
        : const MooTokens(
            workspace: Color(0xFFF7F7F5),
            surface: Color(0xFFFFFFFF),
            sidebar: Color(0xFFEEEEEC),
            toolbar: Color(0xFFF3F3F1),
            border: Color(0xFFDDDDD8),
            textPrimary: Color(0xFF2A2A28),
            textSecondary: Color(0xFF6F6F6A),
            accent: Color(0xFF5C5C56),
            onAccent: Color(0xFFFFFFFF),
            focus: Color(0xFF44443F),
          ),
    InterfaceStyle.hero => dark
        ? const MooTokens(
            workspace: Color(0xFF101826),
            surface: Color(0xFF182033),
            sidebar: Color(0xFF0C1422),
            toolbar: Color(0xFF1C2740),
            border: Color(0xFF2C3B5A),
            textPrimary: Color(0xFFE8EEF8),
            textSecondary: Color(0xFF9AABC4),
            accent: Color(0xFF5B8DEF),
            onAccent: Color(0xFF081018),
            focus: Color(0xFF7AA4F5),
          )
        : const MooTokens(
            workspace: Color(0xFFF3F6FB),
            surface: Color(0xFFFFFFFF),
            sidebar: Color(0xFFD9E4F2),
            toolbar: Color(0xFFE7EEF8),
            border: Color(0xFFC5D4E8),
            textPrimary: Color(0xFF1A2740),
            textSecondary: Color(0xFF4D5F7A),
            accent: Color(0xFF1F5FBF),
            onAccent: Color(0xFFFFFFFF),
            focus: Color(0xFF164A9A),
          ),
    InterfaceStyle.smartisan => dark
        ? const MooTokens(
            workspace: Color(0xFF1C1612),
            surface: Color(0xFF261E18),
            sidebar: Color(0xFF211A15),
            toolbar: Color(0xFF2B221C),
            border: Color(0xFF3D3228),
            textPrimary: Color(0xFFF0E6DC),
            textSecondary: Color(0xFFB7A394),
            accent: Color(0xFFE08A5A),
            onAccent: Color(0xFF1A120C),
            focus: Color(0xFFF0A070),
          )
        : const MooTokens(
            workspace: Color(0xFFF6EFE6),
            surface: Color(0xFFFFFBF6),
            sidebar: Color(0xFFEDE3D4),
            toolbar: Color(0xFFF3EADF),
            border: Color(0xFFDCCBB6),
            textPrimary: Color(0xFF3A2A1C),
            textSecondary: Color(0xFF7A6452),
            accent: Color(0xFFC45C26),
            onAccent: Color(0xFFFFFFFF),
            focus: Color(0xFFA34818),
          ),
    InterfaceStyle.miuiV5 => dark
        ? const MooTokens(
            workspace: Color(0xFF101418),
            surface: Color(0xFF171C22),
            sidebar: Color(0xFF14181D),
            toolbar: Color(0xFF1C232B),
            border: Color(0xFF2C3640),
            textPrimary: Color(0xFFE7EDF3),
            textSecondary: Color(0xFF9AA8B5),
            accent: Color(0xFF4DA3FF),
            onAccent: Color(0xFF071018),
            focus: Color(0xFF7CBCFF),
          )
        : const MooTokens(
            workspace: Color(0xFFF0F3F7),
            surface: Color(0xFFFFFFFF),
            sidebar: Color(0xFFE4EAF0),
            toolbar: Color(0xFFEAEEF3),
            border: Color(0xFFCDD6E0),
            textPrimary: Color(0xFF1C242C),
            textSecondary: Color(0xFF5A6874),
            accent: Color(0xFF0D84FF),
            onAccent: Color(0xFFFFFFFF),
            focus: Color(0xFF0A6AD1),
          ),
    InterfaceStyle.claude => dark
        ? const MooTokens(
            workspace: Color(0xFF1A1714),
            surface: Color(0xFF221E1A),
            sidebar: Color(0xFF1E1A16),
            toolbar: Color(0xFF28231E),
            border: Color(0xFF3A342C),
            textPrimary: Color(0xFFF2EBE3),
            textSecondary: Color(0xFFB4A89A),
            accent: Color(0xFFE09A7A),
            onAccent: Color(0xFF1A1410),
            focus: Color(0xFFF0B090),
          )
        : const MooTokens(
            workspace: Color(0xFFF7F4EF),
            surface: Color(0xFFFFFCF8),
            sidebar: Color(0xFFEFE8DC),
            toolbar: Color(0xFFF4EFE7),
            border: Color(0xFFDED4C4),
            textPrimary: Color(0xFF2C2620),
            textSecondary: Color(0xFF6E655A),
            accent: Color(0xFFD97757),
            onAccent: Color(0xFFFFFFFF),
            focus: Color(0xFFC45F40),
          ),
  };
}

ThemeData buildTheme(
    {required Brightness brightness,
    required double uiFontSize,
    Color? accent,
    InterfaceStyle style = InterfaceStyle.modern}) {
  final tokens = tokensFor(style, brightness);
  final scheme = ColorScheme.fromSeed(
    seedColor: accent ?? tokens.accent,
    brightness: brightness,
    surface: tokens.surface,
  );
  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: scheme,
    scaffoldBackgroundColor: tokens.workspace,
    fontFamily: null,
    textTheme: ThemeData(brightness: brightness).textTheme.apply(
          fontSizeFactor: uiFontSize / 13,
          bodyColor: tokens.textPrimary,
          displayColor: tokens.textPrimary,
        ),
    extensions: [tokens.copyWith(accent: accent ?? tokens.accent)],
  );
}

MooTokens tokensOf(BuildContext context) =>
    Theme.of(context).extension<MooTokens>() ?? MooTokens.light;
