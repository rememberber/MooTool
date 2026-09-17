# DIFF-495：设置界面字体加载规范化（对齐 Electron 迁入）

## 背景

DIFF-482 已覆盖 `editor.*FontName` 修剪；DIFF-490 仍列 **A01 设置** `appearance.fontFamily` 未入加载链。Electron 默认 `system-ui`，Compose 设置下拉使用 `system`/`sans-serif`/`serif`/`monospace`；迁入或手工编辑 `settings.json` 可能保留首尾空白或 `system-ui`，导致 `MooTheme` 与设置页选中态不一致。

## 行为

- `SettingsLayoutNormalize.normalizeUiFontFamily`：trim、最长 120、空值回退默认 `system`；`system-ui`→`system`；四档预设统一小写。
- `SettingsLayoutNormalize.apply` / `SettingsRepository.sanitizeLoadedSettings` / `ElectronNextSettingsImport` 合并外观时统一校正。

## 验证

- `SettingsLayoutNormalizeTest.normalizeUiFontFamily_trimsAndMapsSystemUiAlias`
- `SettingsVaultPathSanitizeTest.loadNormalizesUiFontFamily`
- `ElectronNextSettingsImportTest.merge_normalizesUiFontFamily`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品窗界面字体走查帧、六套 CSS 逐选择器皮肤。
