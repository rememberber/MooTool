# DIFF-145：设置页运行环境检测与可执行文件选择

对照 Electron `RuntimeSettings`（`detectRuntimes`、版本状态、路径输入）。

## 范围

- `RuntimeSettingsPanel`：进入/路径变更时 `CodeRunEngine.detect`；「重新检测」按钮；每行显示可用性与版本/未找到。
- 路径可编辑；「选择…」通过 `DesktopFileDialogs.chooseExecutable` / `AppContainer.chooseExecutable`。
- 文案 `settings.runtime.detect` / `notFound` / `path` / `browse`。

## 文件

- `RuntimeSettingsPanel.kt`、`SettingsScreen.kt`、`DesktopFileDialogs.kt`、`AppContainer.kt`、`Translator.kt`

## 未测

- 设置页运行环境检测与文件选择对话框的产品窗手工走查。
