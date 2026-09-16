# DIFF-142：默认导出目录绝对路径校验

对照 Electron `DirectorySetting` 与 Vault 目录语义：留空表示默认行为，非空须为绝对路径。

## 范围

- 设置「默认导出目录」提交时复用 `commitVaultPath` / `VaultPathConfig` 校验与 toast。
- `SettingsRepository.load` 清除已保存的相对导出路径（与 DIFF-141 Vault 一致）。
- 图片/PDF 默认保存路径解析使用 `effectiveCustomRoot`。
- 设置页提示文案 `settings.tools.exportDirHint` / `exportDirDefault`。

## 文件

- `SettingsRepository.kt`、`SettingsScreen.kt`、`ImageScreen.kt`、`PdfScreen.kt`、`Translator.kt`
- `SettingsVaultPathSanitizeTest.kt`
