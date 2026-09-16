# DIFF-097：Tab 焦点 Compose 证据帧与二维码识别 file-drop

- 编号：DIFF-097
- 影响：A01 焦点环；F17 二维码识别；`ToolbarFocusCaptureTest`
- 日期：2026-09-15

## 原行为（Electron）

- 命令盘搜索、JSON 工具栏复制等可键盘 Tab 并显示焦点样式
- 二维码识别区选图 + 文件名省略（同类 file-drop/selected-file）

## 本产品行为

- `ToolbarFocusCaptureTest` 在 Compose 场景生成 `127-compose-command-search-tab-focus.png`、`128-compose-json-copy-tab-focus.png`（modern 焦点环像素断言）；**不能代替**产品主窗 AX 帧，与 `116`/`120`/`123` 互补
- 二维码「识别」Tab 选图行改用 `FileDropRow`，去掉预览下重复 12sp 文件名

## 证据

`desktopTest` 含新用例；PNG 写入 `docs/evidence/2026-09-15-inspector-screencapture/windows/`。

## 未做

其余工具页产品窗 Tab 走查、系统 IME、三平台安装仍未测。
