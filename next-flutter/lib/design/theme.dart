import 'package:flutter/material.dart';

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

ThemeData buildTheme(
    {required Brightness brightness,
    required double uiFontSize,
    Color? accent}) {
  final tokens =
      brightness == Brightness.dark ? MooTokens.dark : MooTokens.light;
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
