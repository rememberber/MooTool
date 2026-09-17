# DIFF-480：Vault 树展开模式加载规范化（对齐 Electron 设置）

## 背景

Electron `mergeSettings` 仅接受 `expandAll`/`collapseAll`，未知 `vault.jsonTreeExpandMode` / `quickNoteTreeExpandMode` 回退默认 `expandAll`（`next/src/shared/contracts/settings.test.ts`）。Compose 支持 Java 迁入的 `smart`，但磁盘或迁入 JSON 中的非法值曾原样保留，Vault 树 `expandMode` 走 `else` 分支仅展开根目录，与 Electron 及设置 UI 不一致。

## 行为

- `normalizeVaultTreeExpandMode`：`smart`/`expandAll`/`collapseAll` 保留，其它 → `expandAll`。
- `SettingsRepository.sanitizeLoadedSettings` 与 `ElectronNextSettingsImport.sanitize` 写回规范化结果。

## 验证

- `VaultTreeTest.normalizeVaultTreeExpandMode_matchesElectronSettingsContract`
- `SettingsVaultPathSanitizeTest.loadNormalizesUnknownVaultTreeExpandModes`
- `./gradlew :composeApp:desktopTest --offline`
