# DIFF-065：工具面板/编辑器对齐 Electron editor-shell 与 vault-panel

- 编号：DIFF-065
- 影响：F01/F04/F05/F09/F10/F20/F23 布局；ui-spec 工作区面板
- 日期：2026-09-15

## 原行为（Electron）

`.editor-shell` / `.vault-panel` / `.quick-replace-panel` / `.runtime-shell` 有圆角、细分隔和 `shadow-soft`。hero/smartisan/claude 阴影更重，raised 风格用 `--border-control`。

## 本产品行为

- `mooToolShell`：Vault、检查器、快速替换、HTTP 集合、Host 列表、图片库、翻译侧栏、运行输出、文本对比、编码、格式化、配置、正则结果、Protobuf。
- `mooEditorFrame`：JSON/随手记/HTTP/运行台/Host 编辑器外框（不 clip Swing，避免圆角裁切 RSTA）。
- 随手记/JSON/Host/HTTP/运行台/翻译/环境变量弹层改用 `mooDialogSurface`。

## 理由

工具工作区此前是平铺色块，和 Electron 面板层级不一致。不能给 Swing 编辑器套 clip 圆角，所以编辑器只用边与阴影。

## 证据

实现与 `desktopTest`。窗口截图待拍。仍不是六套 CSS 逐选择器移植。

## 未做

- 系统 IME 窗口手势、托盘 TCC、三平台安装仍未测。
