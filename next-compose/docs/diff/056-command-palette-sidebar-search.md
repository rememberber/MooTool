# DIFF-056：侧栏搜索入口与命令盘 chrome

- 编号：DIFF-056
- 影响：A02 命令搜索；ui-spec 壳「折叠 / 搜索 / 分组」；导航悬停
- 日期：2026-09-15

## 原行为（Electron）

侧栏 `sidebar-actions` 依次为折叠、搜索（tooltip `搜索 · ⌘K`）、管理分组。`CommandPalette` 为 620px 弹层：搜索行带关闭、结果行显示工具名与分组、选中用 `--control-active`、空结果居中；输入框内 ↑↓/Enter/Esc。导航 `.tool-button` 始终 1px 边框，悬停/选中有 raised 底。

## 本产品行为

- 侧栏标题栏顺序改为折叠、搜索 `⌕`、分组 `+`；搜索 tooltip 在 macOS 为 `搜索 · ⌘K`，其他为 `Ctrl+K`。搜索与折叠/分组同一套焦点环。
- 命令盘宽 620dp、描边与阴影；结果显示本地化名称与分组名（首页无分组）；选中 `selected` 底；空文案居中；关闭按钮 `app.search.close`。打开时焦点进输入框，方向键在输入框内也能换行选择。
- 导航行 `hoverable`：悬停/选中 1dp 边框；smartisan/hero/miui-v5 悬停用 raised 渐变。
- 分组弹层只在打开时快照草稿，编辑过程中不再被设置流重置。

## 理由

ui-spec 壳图与 Electron 都把搜索做成侧栏可见入口；原先只有快捷键。输入框吞掉方向键时命令盘无法键盘走查。

## 证据

`CommandPaletteKeysTest`、`ToolRegistry` 空查询返回 26 项。`desktopTest` **240/240**。主窗见 `71-home-search-button.png`、`72-command-palette.png`、`73-group-manager.png`。

## 受影响范围

- 仍非 Electron 六套 CSS 逐选择器皮肤。
- 产品主窗 Tab/IME 手工、托盘 TCC、三平台安装未测。
