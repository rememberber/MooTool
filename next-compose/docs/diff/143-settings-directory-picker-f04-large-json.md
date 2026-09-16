# DIFF-143：设置目录选择与 F04 大 JSON / 重复键单测

对照 Electron `DirectorySetting` + `settings.chooseDirectory`；`feature-parity.md` F04 验收（重复 key、大输入）。

## 范围

- `DesktopFileDialogs.chooseDirectory` + `AppContainer.chooseDirectory`。
- 设置页随手记/JSON Vault、默认导出目录：`DirectorySettingRow`（文本提交 + 选目录按钮，路径仍走绝对路径校验）。
- `JsonEngineTest`：`formatAdvanced` 对原文重复键失败；约 1.5 MiB 字符串字段格式化不丢内容。

## 文件

- `DesktopFileDialogs.kt`、`AppContainer.kt`、`SettingsScreen.kt`、`Translator.kt`
- `JsonEngineTest.kt`

## 未测

- 设置页选目录按钮的系统文件对话框手势（需产品窗手工）。
