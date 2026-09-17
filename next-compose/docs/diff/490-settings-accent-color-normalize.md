# DIFF-490：设置强调色加载规范化（对齐 Electron `normalizeSettings`）

## 背景

Electron `mergeSettings` / `normalizeSettings` 仅保留 `accentColorPresets` 中的 id，未知值回退默认 `blue`（见 `next/src/shared/contracts/settings.ts`）。Compose 主题运行时经 `AccentPresets.normalize` 兜底，但 `settings.json` 与 Electron 迁入仍可能保留 `bogus` 或旧版 `orange`/`teal` 别名，导致设置页色板选中态与磁盘不一致。

parity-gap 优先补齐 **A01 设置** 加载规范化链（DIFF-486 已覆盖 interfaceStyle/customGroups/paneSizes，本条补 `accentColor`）。

## 行为

- `SettingsLayoutNormalize.normalizeAccentColor`：六色预设 + `orange`→`yellow`、`teal`→`green`，未知→默认 `blue`。
- `SettingsLayoutNormalize.apply` / `SettingsRepository.sanitizeLoadedSettings` / `ElectronNextSettingsImport` 统一校正。
- `AccentPresets.normalize` 委托同一函数，避免主题与持久化分叉。

## 验证

- `SettingsLayoutNormalizeTest.normalizeAccentColor_matchesElectronAccentPresets`
- `SettingsVaultPathSanitizeTest.loadNormalizesUnknownAccentColorAndLegacyAliases`
- `./gradlew :composeApp:desktopTest --offline`

## 未做

`fontFamily` 修剪、产品窗强调色走查帧、六套 CSS 逐选择器皮肤。
