# DIFF-139：文档库自定义路径必须为绝对路径

对照 `settings.vault.hint` 与 Electron 目录设置语义。

## 范围

- `VaultPathConfig`：空字符串表示默认 `data/vaults/*`；非空须为绝对路径。
- `NoteVault` / `JsonVault`：`resolveCustomRoot` 解析配置。
- 设置页随手记/JSON 目录：提交时校验，失败 toast `settings.vault.absoluteRequired`。
- 单测：`VaultPathConfigTest`。

## 文件

- `VaultPathConfig.kt`、`NoteVault.kt`、`JsonVault.kt`、`SettingsScreen.kt`、`Translator.kt`
- `VaultPathConfigTest.kt`

## 备注

- 使用 `java.nio.file.Path.isAbsolute`；勿误导入 `kotlin.io.path.isAbsolute`（会导致 desktop 编译失败）。
