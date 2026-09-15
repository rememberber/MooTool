# DIFF-034：系统托盘闭环、软换行默认、工具栏密度与 960 折叠策略

- 编号：DIFF-034
- 影响：A01 general/editor、A03 托盘、P1 壳密度、F01/F04 新会话换行、F02/其余工具工具栏
- 日期：2026-09-15

## 原行为（Electron）

`settings.general.trayEnabled` 会创建系统托盘：打开、设置、取色、截图、翻译、Host 方案、退出。关闭到托盘时窗口隐藏，托盘点击恢复。编辑器软换行设置影响适用页面。工具栏高度与侧栏顶栏对齐。窗口内容宽 < 960 时折叠导航。

## 本产品行为

- `AppTray` 使用 AWT `SystemTray`。开启且平台支持时安装托盘图标与菜单；不支持时设置页显示原因，不再只翻转布尔值。
- 托盘：打开、设置、屏幕取色、区域截图、翻译、Host 方案（只打开 Host 工具并载入方案，**不**静默写入系统 hosts）、退出。取色/截图走已有 `ScreenColorPicker` / `ScreenRegionPicker`。
- 关闭行为 `hide` 仍隐藏主窗；托盘可用时点击图标恢复。
- JSON / 随手记**新会话**默认 `editor.softWrap`；已有会话快照与文档 frontmatter 仍优先。代码运行继续读取该设置。
- 工具栏/侧栏顶栏使用 `MooTheme.dimens.toolbar` 与 `toolbarBrush()`；状态栏使用 `statusBar`。
- `LayoutPolicy.collapseNavigation` 抽出 < 960 dp / 手动折叠 / 隐藏标题的判定。

## 理由

规格禁止假开关；托盘、软换行和密度必须改变运行结果。Host 从托盘静默写系统文件会跳过 diff/备份，因此只打开工具。

## 证据

`AppTrayTest`、`LayoutPolicyTest`、`CompactShellCaptureTest`（Compose 场景 PNG，不是真实窗口截图）。`desktopTest` **188/188**，见 `docs/evidence/2026-09-15-tray-density/`。IME 手工手势、三平台安装、逐选择器 CSS 仍未测。

## 受影响范围

- 不支持 SystemTray 的环境（含部分 headless CI）托盘菜单不会出现，设置页有说明。
- Host 托盘项不会调用 `HostEngine.apply`。
- 已保存的 JSON/随手记 wrap 不被设置覆盖。
