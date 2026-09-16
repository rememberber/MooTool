# DIFF-150：Electron `mootool-next.json` 设置合并导入

## 背景

跨产品导入此前只从 Electron 存储读取 `layout.customGroups`。Electron Next 的 `settings` 对象包含语言、外观、布局、编辑器、网络、Vault、运行环境路径与工具项等，应在迁移时合并进本产品 `AppSettings`（与 Electron `mergeSettings` 同类，但保留 compose 的 `workspace` 与侧栏宽度）。

## 行为

- `ElectronNextSettingsImport`：从 `mootool-next.json` 的 `settings` 解码并 `mergeInto` 当前设置。
- 不导入 `proxyPassword`、`gitToken`；Vault/导出路径经 `VaultPathConfig` 清除非法相对路径。
- `customGroups` 按 `id` 追加，不覆盖已有分组。
- 确认导入时先合并 Electron 设置，再应用 Java `config.setting` 补丁（DIFF-149）。
- `ImportPreview` 携带 `electronSettings`；`totalItems` 在存在 Electron 设置时 +1。

## 证据

- `./gradlew :composeApp:desktopTest --offline`（`ElectronNextSettingsImportTest`、`CrossProductImporterTest`）。

## 未覆盖

- Electron 密文凭据仓（safeStorage）单独迁移。
- `runtime.drafts/options` 迁入代码运行会话见 [DIFF-151](151-electron-runtime-drafts-import.md)（不写入 `AppSettings.RuntimeSettings`）。
