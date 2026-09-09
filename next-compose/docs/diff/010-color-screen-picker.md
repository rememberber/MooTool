# DIFF-010：调色板屏幕取色使用 AWT Robot 冻结截图

- 编号：DIFF-010
- 影响：F22 调色板
- 日期：2026-09-09

## 原行为（Electron）

主进程 `desktopCapturer` 截取各屏后再由渲染进程叠层采样。遮罩本身不进入像素。收藏夹使用 SQLite 数字 `folderId`。

## 本产品行为

按 `GraphicsDevice` 用 AWT `Robot` 截各屏，拼到可含负坐标的虚拟桌面，再显示不透明冻结截图覆层采样。放大预览、Esc/右键取消。macOS 无录屏权限时 Robot 常返回全黑图，按网格抽样若整图近乎全黑则报权限错误，不把黑色当作取色成功。自由取色使用 `JColorChooser`。收藏为 `favorites/color.json`，文件夹 id 为 UUID 字符串。

## 理由

Compose Desktop 没有 `desktopCapturer`。先截后叠可保证覆层不影响像素。全黑启发式避免在无权限时假装取到 `#000000`。

## 证据

`ColorEngineTest`：解析/格式、五运算、主题与标准色 SHA-256、RGBA 采样夹紧。`ScreenColorSamplerTest`：负坐标原点与放大预览。真实多屏/录屏权限对话框未在本机窗口验收。

## 受影响范围

- HiDPI 下 AWT 逻辑像素与物理像素若不一致，取色可能偏移；需在目标机实测。
- 整屏确实全黑时会被当成权限失败。
- 收藏 JSON 不能直接导入 Electron 数字 folderId 数据库。
