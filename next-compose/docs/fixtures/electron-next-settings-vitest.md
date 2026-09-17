# Electron `settings.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| numeric-boundaries | `normalizes numeric settings at their supported boundaries` | `SettingsNumericBoundsTest`（[DIFF-485](../diff/485-settings-numeric-bounds-normalize.md)） |
| tools-editor-numeric-commit | `SettingsWindow` tools `NumberSetting` + editor font size 11–24 | `SettingsToolsEditorNumericNormalizeTest`（[DIFF-521](../diff/521-settings-tools-editor-numeric-commit.md)） |
| json-soft-wrap-live | （Electron JSON 初始 wrap 来自 settings；Compose 设置切换即时同步） | `EditorSettingsLiveApplyTest` + `JsonScreen`（[DIFF-528](../diff/528-http-multipart-editor-tray-git-slice.md)） |
| editor-wrap-by-tool | Runtime `wrap={false}`；HTTP 读 settings；随手记新建 `lineWrap` | `EditorSettingsLiveApplyTest` + `CodeRunScreen`/`HttpScreen`/`QuickNoteScreen`（[DIFF-529](../diff/529-editor-runtime-git-inspector-slice.md)） |
| translation-languages | `migrates legacy localized translation language names` | `SettingsTranslationLanguageTest` / load sanitize（[DIFF-481](../diff/481-settings-translation-language-normalize.md)） |
| editor-font-names | `normalizes editor font family names` | `EditorFontSettingsTest`（[DIFF-482](../diff/482-settings-editor-font-normalize.md)） |
| editor-sql-dialect | `SettingsWindow` SQL dialect select presets | `EditorFontSettingsTest.normalizeSqlDialect_*`（[DIFF-530](../diff/530-editor-sql-dialect-http-font-slice.md)） |
| json-editor-font-size-live | JSON/HTTP/CodeRun `EditorHost` 读 `jsonFontSize` | `EditorSettingsLiveApplyTest` + `HttpScreen` 响应区（[DIFF-530](../diff/530-editor-sql-dialect-http-font-slice.md)） |
| tools-defaults-live | QR 尺寸/纠错、随机串默认长度 | `ToolsSettingsLiveApplyTest` + `QrCodeScreen`/`CryptoScreen`（[DIFF-531](../diff/531-tools-defaults-live-git-keywords-slice.md)） |
| vault-tree-expand | `normalizes unknown vault tree expand modes` | `SettingsVaultPathSanitizeTest`（[DIFF-480](../diff/480-vault-tree-expand-mode-normalize.md)） |
| custom-groups | `normalizes custom navigation groups` | `SettingsLayoutNormalizeTest`（[DIFF-486](../diff/486-settings-layout-interface-custom-groups.md)） |
| hidden-nav | `normalizes hidden navigation tools` | `SettingsVaultPathSanitizeTest`（[DIFF-177](../diff/177-settings-hidden-nav-normalize.md)） |
| pane-sizes | `normalizes persisted workspace pane sizes` | `SettingsLayoutNormalizeTest.sanitizePaneSizes_*`（DIFF-486） |
| interface-style | `normalizes unknown interface styles` | `SettingsLayoutNormalizeTest` / load sanitize（DIFF-486） |
| accent-color | `accentColorPresets` in `normalizeSettings` | `SettingsLayoutNormalizeTest.normalizeAccentColor_*`（[DIFF-490](../diff/490-settings-accent-color-normalize.md)） |
