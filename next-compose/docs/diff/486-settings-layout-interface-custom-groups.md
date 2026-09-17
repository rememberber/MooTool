# DIFF-486：设置外观/导航布局加载规范化（对齐 Electron `normalizeSettings`）

## 背景

Electron `mergeSettings` / `normalizeSettings` 会校正未知 `interfaceStyle`、自定义侧栏分组 `customGroups`（去重 ID、剔除首页与非法工具、空名丢弃）以及 `paneSizes` 键名/向量合法性（见 `settings.test.ts` `normalizes unknown interface styles`、`normalizes custom navigation groups`、`normalizes persisted workspace pane sizes`）。Compose 仅在 Electron 迁入路径零散处理，且 `SettingsRepository.sanitizeLoadedSettings` 未覆盖外观/布局，非法 `interfaceStyle` 可能传入 `MooTheme`。

## 行为

- 新增 `SettingsLayoutNormalize`：`normalizeInterfaceStyle`、`normalizeTheme`、`normalizeNavigationStyle`、`normalizeCustomGroups`、`sanitizePaneSizes`（Compose 仍以 dp 存分栏；剔除非法键与非正长度向量，比例→dp 仍走 `ElectronPaneSizeImport`）。
- `SettingsRepository` / `ElectronNextSettingsImport.sanitize` 在数值边界校正前统一 `SettingsLayoutNormalize.apply`。
- 修复 `sanitizeLoadedSettings` 仅以数值比较决定是否回写、导致路径/布局校正未持久化的问题。

## 验证

- `SettingsLayoutNormalizeTest`
- `SettingsVaultPathSanitizeTest.loadNormalizesUnknownInterfaceStyleAndCustomGroups`
- `./gradlew :composeApp:desktopTest --offline`
