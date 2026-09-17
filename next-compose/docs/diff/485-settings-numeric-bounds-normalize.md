# DIFF-485：设置数值边界规范化（对齐 Electron `mergeSettings`）

## 背景

Electron `normalizeSettings` 在加载/合并时对字号、超时、Vault 自动提交间隔、二维码尺寸、随机串长度等字段做 `clampNumber`（见 `next/src/shared/contracts/settings.test.ts` `normalizes numeric settings at their supported boundaries`）。Compose 仅在 Electron 迁入路径零散 `coerceIn`，且 `SettingsRepository` 加载 `settings.json` 时未统一校正，非法值会影响 HTTP/翻译超时、加解密随机长度与 JSON 编辑器字号。

## 行为

- 新增 `SettingsNumericBounds.normalize`：对齐 Electron 各字段 min/max（含 `translationTimeoutMs` 上限 120_000、`randomStringLength` 1–4096、Vault 自动提交 5–3600 等）。
- `SettingsRepository.sanitizeLoadedSettings` / `save()`：在路径/字体/语言等规范化后再写回数值边界。
- `ElectronNextSettingsImport.sanitize` / `mergeInto`：迁入合并复用同一 helper，修正此前翻译超时 60s 上限与随机串 256 上限偏差。

## 验证

- `SettingsNumericBoundsTest.normalize_matchesElectronSettingsContract`
- `SettingsVaultPathSanitizeTest.loadNormalizesNumericSettingBoundaries`
- `./gradlew :composeApp:desktopTest --offline`
