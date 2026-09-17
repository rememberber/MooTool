# DIFF-482：设置加载规范化编辑器字体名（对齐 Electron `mergeSettings`）

## 背景

Electron `normalizeSettings` 经 `normalizeFontName` 修剪 `editor.jsonFontName` / `editor.quickNoteFontName` 首尾空白，空字符串回退默认 `ui-monospace`（`next/src/shared/contracts/settings.test.ts` `normalizes editor font family names`）。Compose 设置页与 JSON 工具栏虽写入用户选择，但 `settings.json` 与 Electron 迁入曾保留带空格或空随手记字体，导致 RSTA/`DocumentFormatEngine` 解析与 Electron 不一致。

## 行为

- `EditorFontSettings.normalizeFontName` / `normalizeEditorSettings`：trim、最长 120、空值用默认字体。
- `SettingsRepository.sanitizeLoadedSettings`：加载与 `save()` 写盘前规范化 `editor` 字体字段。
- `ElectronNextSettingsImport.sanitize` 与 `mergeInto`：迁入/合并同样规范化。

## 验证

- `EditorFontSettingsTest.normalizeFontName_matchesElectronSettingsContract`
- `SettingsVaultPathSanitizeTest.loadNormalizesEditorFontNames`
- `ElectronNextSettingsImportTest.merge_normalizesEditorFontNames`
- `./gradlew :composeApp:desktopTest --offline`
