# DIFF-500：设置 general 语言/关闭行为加载规范化

## 背景

Electron `normalizeSettings` 将 `general.language` 限制为 `zh-CN` / `en-US` / `ja-JP`，非法 `closeBehavior` 回退 `ask`（见 `next/src/shared/contracts/settings.ts`）。Compose 仅在 Electron 迁入时校正；`SettingsRepository.load()` 仍可能保留手工编辑的无效值，导致设置页下拉与磁盘不一致，关闭主窗行为不可预期。

parity-gap **A01 设置** 加载链在 DIFF-490/495 已补外观字段，本条补 **general** 切片。

## 行为

- **`SettingsGeneralNormalize`**：`normalizeLanguage` / `normalizeCloseBehavior` / `apply`。
- **`SettingsLayoutNormalize.apply`** 先走 general 规范化，再校正 appearance/layout（与 Electron 单次 `normalizeSettings` 等价）。
- **`ElectronNextSettingsImport`** 合并 general 时复用同一函数，去掉重复私有实现。

## 验证

- `SettingsGeneralNormalizeTest`
- `SettingsVaultPathSanitizeTest.loadNormalizesUnknownGeneralLanguageAndCloseBehavior`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品窗设置语言/关闭行为走查帧、六套 CSS 逐选择器皮肤、P7 三平台安装验收。
