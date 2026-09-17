# DIFF-481：设置加载规范化翻译语言（对齐 Electron `mergeSettings`）

## 背景

Electron `normalizeSettings` 经 `normalizeTranslationLanguagePair` 将旧版本地化语言名（如 `English`、`英语`）映射为语言代码，并在源/目标相同时把源语言改为 `auto`（`next/src/shared/contracts/settings.test.ts` `migrates legacy localized translation language names`）。Compose 已在 `TranslationEngine` / 翻译存储 / 跨产品导入中使用同一逻辑，但 `settings.json` 与 Electron 设置迁入曾原样保留显示名，导致 F20 默认语言与 API 参数不一致。

## 行为

- `SettingsRepository.sanitizeLoadedSettings`：`tools.translationSourceLang` / `translationTargetLang` 经 `TranslationEngine.normalizeLanguagePair` 写回。
- `ElectronNextSettingsImport.sanitize`：`normalizeTranslationTools` 同步规范化并保留导出目录绝对路径处理。

## 验证

- `SettingsVaultPathSanitizeTest.loadNormalizesLegacyTranslationLanguageNames`
- `ElectronNextSettingsImportTest.sanitize_normalizesLegacyTranslationLanguageNames`
- `./gradlew :composeApp:desktopTest --offline`
