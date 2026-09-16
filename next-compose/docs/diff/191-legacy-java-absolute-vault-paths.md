# DIFF-191：Java 绝对 Vault 根目录迁入设置

## 背景

Java `func.quickNote.quickNoteVaultPath` / `func.jsonBeauty.jsonBeautyVaultPath` 可为相对安装目录或绝对/`~` 路径。Compose 自定义文档库根须绝对路径（[DIFF-139](139-vault-path-absolute-validation.md)、[DIFF-141](141-vault-path-load-fallback.md)）；相对路径在加载时清除，迁移时也不写入设置。

## 行为

`LegacyJavaSettings.applyPatch`：仅当配置值为绝对路径（含 `~` 展开）且通过 `VaultPathConfig.normalizedCustomRoot` 时写入 `vault.quickNotePath` / `vault.jsonPath`。相对路径忽略（数据迁入仍由 `LegacyJavaDataPaths` + `CrossProductImporter` 按源根解析）。

## 证据

- `LegacyJavaSettingsTest.applyPatchMapsAbsoluteVaultPathsAndIgnoresRelative`
