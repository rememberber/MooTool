# DIFF-193：相对 Vault 路径迁移后写入本产品根目录

## 背景

Java 常用相对 `quickNoteVaultPath` / `jsonBeautyVaultPath`（相对安装目录）。`CrossProductImporter` 将文件写入本产品默认/当前 Vault 根，但 [DIFF-191](191-legacy-java-absolute-vault-paths.md) 不会把相对路径写入设置，导致设置页仍显示「默认」而 Git/路径与真实落盘目录不一致。

## 行为

迁移确认且 `CrossProductImporter` 完成后，在 `applyPatch` 之后调用 `LegacyJavaSettings.applyVaultRootsAfterRelativeJavaConfig`：

- 配置中存在对应键、路径非绝对、且该侧 `importedNotes` / `importedJson` > 0 时，将 `vault.quickNotePath` / `vault.jsonPath` 设为当前 `NoteVault` / `JsonVault` 的 `root()` 绝对路径。
- 已迁入的绝对 Java 路径不被覆盖。

## 证据

- `LegacyJavaSettingsTest.applyVaultRootsAfterRelativeJavaConfigUsesComposeRoots`
- `LegacyJavaSettingsTest.applyVaultRootsAfterRelativeSkipsWhenAbsolutePathAlreadyPatched`
