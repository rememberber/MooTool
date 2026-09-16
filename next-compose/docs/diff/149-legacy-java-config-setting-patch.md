# DIFF-149：Java `config.setting` 设置补丁迁移

## 背景

Electron `legacySettingsPatch` 从 Java 版 `config/config.setting`（INI 分组）读取语言、外观、布局、编辑器、代理、Vault Git、工具项等，并在迁移后合并进本产品设置。next-compose 此前只导入 Electron `mootool-next.json` 自定义分组，不处理 Java 配置文件。

## 行为

- `LegacyJavaSettings`：解析 `config/config.setting`，生成与 Electron 对齐的 `applyPatch`（不含代理密码 / Vault token）。
- `CrossProductImporter.inspect`：检测 Java 配置、写入指纹、发出 `legacy:differentVaultRemotes` / `legacy:secretsSkipped` 警告码。
- 确认导入后：`MigrationSettingsPanel` 在合并自定义分组的同时 `updateSettings { applyPatch }`。
- 设置页预览显示 Java 配置是否找到，并将警告码翻译为中文/英文说明。

## 证据

- `./gradlew :composeApp:desktopTest --offline`（`LegacyJavaSettingsTest`、`CrossProductImporterTest` 含 `config.setting`）。

## 未覆盖

- Electron Next 自身 `mootool-next.json` 全量设置合并（仍仅 customGroups）。
- 相对路径导出目录仍按 DIFF-142 在加载时清除，仅导入绝对路径。
