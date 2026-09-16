# DIFF-186：Java Vault 列表排序与树展开迁入

## 背景

Java `config.setting` 在 `func.quickNote` / `func.jsonBeauty` 保存文档库列表排序（`MODIFIED_TIME` / `CREATE_TIME` / `NAME`）与树展开（`EXPAND_ALL` / `COLLAPSE_ALL`）。Compose 分别使用会话 `vaultSort`（`VaultSort`）与设置 `vault.quickNoteTreeExpandMode` / `vault.jsonTreeExpandMode`（`smart` / `expandAll` / `collapseAll`）。

## 行为

- `applyPatch`：`quickNoteTreeExpandMode` / `jsonBeautyTreeExpandMode` → 对应 `VaultSettings` 树展开（Java 仅两档，映射为 `expandAll` / `collapseAll`）。
- `applySessionPatches`：`quickNoteListSortMode` / `jsonBeautyListSortMode` → `QuickNoteSession` / `JsonSession` 的 `vaultSort`（经 `VaultSort.normalize`）。

## 证据

- `LegacyJavaSettingsTest.applyPatchMapsVaultTreeExpandModes`
- `LegacyJavaSettingsTest.applySessionPatchesMapsVaultListSortModes`
