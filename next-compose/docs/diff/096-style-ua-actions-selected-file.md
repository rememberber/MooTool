# DIFF-096：UA 操作条 P5 与 selected-file-name 组件

- 编号：DIFF-096
- 影响：F12 UA；F14 加密摘要；`SelectedFileName`
- 日期：2026-09-15

## 原行为（Electron）

- `.ua-actions`：预设占 `1fr`，粘贴/清空/解析为 `panel-command` / `primary-command`（P5 工具约 30px 高）
- `.selected-file-name`：11px faint、省略（加密摘要工具栏在选文件后显示）

## 本产品行为

- 新增 `SelectedFileName`；摘要 Tab 工具栏改用该组件
- UA：预设按钮 `weight(1f)` + 粘贴/清空/解析/结果复制均 `p5Toolbar`

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

UA 预设改原生 `select` 栅格、file-drop 拖放、系统 IME、产品窗 Tab 帧、三平台安装仍未测。
