# DIFF-059：工具内 Dialog 改为应用内遮罩

- 编号：DIFF-059
- 影响：F01/F04/F08/F09/F10/F15/F16/F18/F20/F22/F23/F24 弹层；A02 历史叠层
- 日期：2026-09-15

## 原行为（Electron）

工具确认框、收藏夹、cURL、JSON 输入与路径选择都在应用内遮罩上，点空白关闭。没有独立半透明系统窗。

## 本产品行为

- JSON / 随手记 / HTTP / 图片 / 调色板 / Host / 环境变量 / 翻译 / 运行台 / PDF / 正则 / Cron / 时钟 的 Compose `Dialog` 改为 `MooOverlay`。
- 面板实底 + 1dp 边框；点遮罩关闭；Esc 关闭。
- 仍走 `ModalOverlayState`：弹层打开时卸下 `SwingPanel`，避免编辑器抢走点击。
- 系统 `FileDialog` / `JFileChooser` / `JColorChooser` 保持原生选择器。

## 理由

Compose `Dialog` 会另开半透明窗，主窗截图透出下层，点击也会和 Swing 编辑器抢层。工具弹层应与命令盘同一套遮罩。

## 证据

`desktopTest` **241/241**。产品主窗 Tab/IME、960 逐页、六套 CSS、托盘 TCC、三平台安装未测。

## 受影响范围

- 弹层覆盖的是工具内容区 `fillMaxSize`，不是整窗（命令盘仍在 Workbench 根上盖住侧栏）。
- 原生文件选择器不是 overlay。
