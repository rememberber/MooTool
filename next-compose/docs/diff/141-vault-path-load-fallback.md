# DIFF-141：Vault 路径加载回退与运行时容错

对照 Electron：用户目录须为绝对路径；非法配置不应导致产品无法打开 Vault。

## 范围

- `VaultPathConfig.effectiveCustomRoot`：非空但相对/非法路径回退为默认（空串 → `data/vaults/*`）。
- `NoteVault` / `JsonVault`：`root()` 使用 `effectiveCustomRoot`，避免 `resolveCustomRoot` 在运行期抛错。
- `SettingsRepository.load`：清除已持久化的相对路径并写回设置文件。
- 单测：`SettingsVaultPathSanitizeTest`、`VaultPathConfigTest`；`JsonEngineTest` 增补 Java 转义/文本还原（对齐 `jsonTools.test.ts` 语义）。

## 文件

- `VaultPathConfig.kt`、`NoteVault.kt`、`JsonVault.kt`、`SettingsRepository.kt`
- `SettingsVaultPathSanitizeTest.kt`、`VaultPathConfigTest.kt`、`JsonEngineTest.kt`
