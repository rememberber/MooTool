# Electron `settings.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| numeric-boundaries | `normalizes numeric settings at their supported boundaries` | `SettingsNumericBoundsTest`（[DIFF-485](../diff/485-settings-numeric-bounds-normalize.md)） |
| translation-languages | `migrates legacy localized translation language names` | `SettingsTranslationLanguageTest` / load sanitize（[DIFF-481](../diff/481-settings-translation-language-normalize.md)） |
| editor-font-names | `normalizes editor font family names` | `EditorFontSettingsTest`（[DIFF-482](../diff/482-settings-editor-font-normalize.md)） |
| vault-tree-expand | `normalizes unknown vault tree expand modes` | `SettingsVaultPathSanitizeTest`（[DIFF-480](../diff/480-vault-tree-expand-mode-normalize.md)） |
| custom-groups | `normalizes custom navigation groups` | `SettingsLayoutNormalizeTest`（[DIFF-486](../diff/486-settings-layout-interface-custom-groups.md)） |
| hidden-nav | `normalizes hidden navigation tools` | `SettingsVaultPathSanitizeTest`（[DIFF-177](../diff/177-settings-hidden-nav-normalize.md)） |
| pane-sizes | `normalizes persisted workspace pane sizes` | `SettingsLayoutNormalizeTest.sanitizePaneSizes_*`（DIFF-486） |
| interface-style | `normalizes unknown interface styles` | `SettingsLayoutNormalizeTest` / load sanitize（DIFF-486） |
| accent-color | `accentColorPresets` in `normalizeSettings` | `SettingsLayoutNormalizeTest.normalizeAccentColor_*`（[DIFF-490](../diff/490-settings-accent-color-normalize.md)） |
